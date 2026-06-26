package world.service;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import world.dao.AnalysisHistoryDao;
import world.entity.AnalysisHistory;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class AnalysisHistoryService {

    @Resource
    private AnalysisHistoryDao analysisHistoryDao;

    // ==================== 原有方法（保持兼容） ====================

    /**
     * 保存分析记录（同步方式，旧接口兼容）
     */
    public void saveHistory(Integer userId, String fileName, String fileType,
                            String imageUrl, JSONObject analysisResult) {
        try {
            AnalysisHistory history = new AnalysisHistory();
            history.setUserId(userId);
            history.setFileName(fileName);
            history.setFileType(fileType);
            history.setImageUrl(imageUrl);
            history.setAnalysisResult(analysisResult.toJSONString());
            history.setPatientName(analysisResult.getString("patient_name"));

            if (analysisResult.containsKey("diagnosis")) {
                JSONObject diagnosis = analysisResult.getJSONObject("diagnosis");
                history.setPrimaryDiagnosis(diagnosis.getString("primary_diagnosis"));
            }

            history.setStatus("SUCCESS");
            history.setProgress(100);

            if (analysisResult.containsKey("confidence")) {
                history.setConfidence(analysisResult.getDouble("confidence"));
            }

            Date now = new Date();
            history.setCreateTime(now);
            history.setUpdateTime(now);

            analysisHistoryDao.insert(history);
            log.info("保存分析历史成功, userId: {}, fileName: {}, createTime: {}", userId, fileName, now);
        } catch (Exception e) {
            log.error("保存分析历史失败", e);
        }
    }

    public Page<AnalysisHistory> getUserHistory(Integer userId, Integer pageNum, Integer pageSize) {
        Page<AnalysisHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getUserId, userId)
                .orderByDesc(AnalysisHistory::getCreateTime);
        return analysisHistoryDao.selectPage(page, wrapper);
    }

    public List<AnalysisHistory> getUserHistoryList(Integer userId, Integer limit) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getUserId, userId)
                .orderByDesc(AnalysisHistory::getCreateTime)
                .last(limit != null ? "limit " + limit : "");
        return analysisHistoryDao.selectList(wrapper);
    }

    public AnalysisHistory getById(Integer id, Integer userId) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getId, id)
                .eq(AnalysisHistory::getUserId, userId);
        return analysisHistoryDao.selectOne(wrapper);
    }

    public boolean deleteById(Integer id, Integer userId) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getId, id)
                .eq(AnalysisHistory::getUserId, userId);
        return analysisHistoryDao.delete(wrapper) > 0;
    }

    public boolean clearUserHistory(Integer userId) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getUserId, userId);
        return analysisHistoryDao.delete(wrapper) > 0;
    }

    public Long getHistoryCount(Integer userId) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getUserId, userId);
        return analysisHistoryDao.selectCount(wrapper);
    }

    /**
     * 获取用户存储统计（记录总数 + 文件总大小）
     */
    public java.util.Map<String, Object> getStorageStats(Integer userId) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getUserId, userId)
                .isNotNull(AnalysisHistory::getFileSize)
                .ne(AnalysisHistory::getFileSize, 0);
        Long totalCount = analysisHistoryDao.selectCount(wrapper);
        // 使用 SQL sum 聚合查询
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AnalysisHistory> sumWrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        sumWrapper.select("IFNULL(SUM(file_size), 0) as total_size")
                .eq("user_id", userId);
        java.util.Map<String, Object> result = analysisHistoryDao.selectMaps(sumWrapper)
                .stream().findFirst().orElse(new java.util.HashMap<>());
        Long totalSize = result.get("total_size") instanceof Number
                ? ((Number) result.get("total_size")).longValue() : 0L;
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("totalSize", totalSize);
        stats.put("formattedSize", formatFileSize(totalSize));
        return stats;
    }

    /**
     * 批量删除历史记录（仅限当前用户）
     */
    public int batchDelete(java.util.List<Integer> ids, Integer userId) {
        if (ids == null || ids.isEmpty()) return 0;
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AnalysisHistory::getId, ids)
                .eq(AnalysisHistory::getUserId, userId);
        return analysisHistoryDao.delete(wrapper);
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // ==================== 异步任务管理（新增） ====================

    /** 任务状态常量 */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_WARNING = "WARNING";
    public static final String STATUS_FAILED = "FAILED";

    /**
     * 创建异步分析任务（状态=PENDING）
     */
    public AnalysisHistory createTask(Integer userId, String fileName, String fileType,
                                      Long fileSize, String imageUrl) {
        AnalysisHistory history = new AnalysisHistory();
        history.setUserId(userId);
        history.setFileName(fileName);
        history.setFileType(fileType);
        history.setFileSize(fileSize);
        history.setImageUrl(imageUrl);
        history.setStatus(STATUS_PENDING);
        history.setProgress(0);
        Date now = new Date();
        history.setCreateTime(now);
        history.setUpdateTime(now);
        analysisHistoryDao.insert(history);
        log.info("创建分析任务: id={}, userId={}, fileName={}, status=PENDING", history.getId(), userId, fileName);
        return history;
    }

    /**
     * 更新任务状态
     */
    public void updateTaskStatus(Integer taskId, String status, Integer progress) {
        AnalysisHistory record = new AnalysisHistory();
        record.setId(taskId);
        record.setStatus(status);
        record.setProgress(progress);
        record.setUpdateTime(new Date());
        analysisHistoryDao.updateById(record);
    }

    /**
     * 更新任务为处理中
     */
    public void markProcessing(Integer taskId) {
        updateTaskStatus(taskId, STATUS_PROCESSING, 30);
        log.info("任务开始处理: id={}, status=PROCESSING", taskId);
    }

    /**
     * 更新任务为成功
     */
    public void markSuccess(Integer taskId, JSONObject result) {
        AnalysisHistory record = new AnalysisHistory();
        record.setId(taskId);
        record.setStatus(STATUS_SUCCESS);
        record.setProgress(100);
        record.setAnalysisResult(result.toJSONString());
        record.setPatientName(result.getString("patient_name"));

        if (result.containsKey("diagnosis")) {
            JSONObject diagnosis = result.getJSONObject("diagnosis");
            if (diagnosis != null) {
                record.setPrimaryDiagnosis(diagnosis.getString("primary_diagnosis"));
            }
        }
        if (result.containsKey("confidence")) {
            record.setConfidence(result.getDouble("confidence"));
        }
        record.setUpdateTime(new Date());
        analysisHistoryDao.updateById(record);
        log.info("任务分析成功: id={}", taskId);
    }

    /**
     * 更新任务为警告（部分识别）
     */
    public void markWarning(Integer taskId, JSONObject partialResult, String warningMsg) {
        AnalysisHistory record = new AnalysisHistory();
        record.setId(taskId);
        record.setStatus(STATUS_WARNING);
        record.setProgress(100);
        record.setAnalysisResult(partialResult != null ? partialResult.toJSONString() : null);
        record.setPatientName(partialResult != null ? partialResult.getString("patient_name") : null);
        record.setErrorMessage(warningMsg);
        record.setUpdateTime(new Date());
        analysisHistoryDao.updateById(record);
        log.warn("任务部分识别: id={}, warning={}", taskId, warningMsg);
    }

    /**
     * 更新任务为失败
     */
    public void markFailed(Integer taskId, String errorMessage) {
        AnalysisHistory record = new AnalysisHistory();
        record.setId(taskId);
        record.setStatus(STATUS_FAILED);
        record.setProgress(0);
        record.setErrorMessage(truncate(errorMessage, 1000));
        record.setUpdateTime(new Date());
        analysisHistoryDao.updateById(record);
        log.error("任务分析失败: id={}, error={}", taskId, errorMessage);
    }

    /**
     * 查询任务状态（供前端轮询）
     */
    public AnalysisHistory getTaskStatus(Integer taskId, Integer userId) {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AnalysisHistory::getId, AnalysisHistory::getStatus,
                        AnalysisHistory::getProgress, AnalysisHistory::getErrorMessage,
                        AnalysisHistory::getCreateTime, AnalysisHistory::getUpdateTime)
                .eq(AnalysisHistory::getId, taskId)
                .eq(AnalysisHistory::getUserId, userId);
        return analysisHistoryDao.selectOne(wrapper);
    }

    /**
     * 重试失败的任务（重置为 PENDING）
     */
    public boolean retryTask(Integer taskId, Integer userId) {
        AnalysisHistory history = getById(taskId, userId);
        if (history == null) {
            return false;
        }
        if (!STATUS_FAILED.equals(history.getStatus()) && !STATUS_SUCCESS.equals(history.getStatus())) {
            return false;
        }
        LambdaUpdateWrapper<AnalysisHistory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(AnalysisHistory::getId, taskId)
                .set(AnalysisHistory::getStatus, STATUS_PENDING)
                .set(AnalysisHistory::getProgress, 0)
                .set(AnalysisHistory::getErrorMessage, null)
                .set(AnalysisHistory::getUpdateTime, new Date());
        analysisHistoryDao.update(null, wrapper);
        log.info("任务重置重试: id={}", taskId);
        return true;
    }

    /**
     * 获取待处理的任务（用于系统启动时恢复未完成的任务）
     */
    public List<AnalysisHistory> getPendingTasks() {
        LambdaQueryWrapper<AnalysisHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AnalysisHistory::getStatus, STATUS_PENDING)
                .or().eq(AnalysisHistory::getStatus, STATUS_PROCESSING);
        return analysisHistoryDao.selectList(wrapper);
    }

    private static String truncate(String str, int maxLen) {
        return str != null && str.length() > maxLen ? str.substring(0, maxLen) : str;
    }
}
