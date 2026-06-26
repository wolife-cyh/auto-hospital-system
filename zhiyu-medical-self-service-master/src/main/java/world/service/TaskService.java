package world.service;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import world.entity.AnalysisHistory;
import world.entity.DocumentTask;
import world.entity.DocumentTaskResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 异步任务调度服务 - 修复非医疗文档处理逻辑
 */
@Slf4j
@Service
public class TaskService {

    @Resource(name = "analysisExecutor")
    private ThreadPoolTaskExecutor executor;

    @Resource
    private AnalysisHistoryService analysisHistoryService;

    @Resource
    private MedicalRecordAnalysisService analysisService;

    /**
     * 系统启动时恢复未完成的任务
     */
    @PostConstruct
    public void recoverPendingTasks() {
        List<AnalysisHistory> pendingTasks = analysisHistoryService.getPendingTasks();
        if (!pendingTasks.isEmpty()) {
            log.info("恢复 {} 个未完成的分析任务", pendingTasks.size());
            for (AnalysisHistory task : pendingTasks) {
                analysisHistoryService.markFailed(task.getId(),
                        "系统重启，任务已重置，请重新分析");
            }
        }
    }

    /**
     * 提交异步分析任务
     *
     * @param task 任务信息
     * @return CompletableFuture 包含最终结果
     */
    public CompletableFuture<DocumentTaskResult> submitAnalysis(DocumentTask task) {
        log.info("提交分析任务: taskId={}, fileName={}", task.getTaskId(), task.getFileName());

        CompletableFuture<DocumentTaskResult> future = CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int retryCount = 0;
            int maxRetries = 2; // 减少重试次数
            Throwable lastError = null;

            while (retryCount <= maxRetries) {
                try {
                    // 标记为处理中
                    analysisHistoryService.markProcessing(task.getTaskId());

                    JSONObject result;
                    if (task.isBase64()) {
                        result = analysisService.analyzeMedicalRecordWithBase64(task.getImageData());
                    } else {
                        result = analysisService.analyzeMedicalRecord(task.getImageData());
                    }

                    long elapsed = System.currentTimeMillis() - startTime;
                    log.info("任务分析完成: taskId={}, 耗时={}ms, 重试次数={}",
                            task.getTaskId(), elapsed, retryCount);

                    // ========================================
                    // ✅ 关键修复：检查是否为错误结果（非医疗文档或解析错误）
                    // ========================================
                    if (result.containsKey("_error") && result.getBoolean("_error")) {
                        String errorMsg = result.getString("message");
                        String errorType = result.getString("_error_type");
                        log.warn("任务失败: taskId={}, errorType={}, message={}",
                                task.getTaskId(), errorType, errorMsg);

                        analysisHistoryService.markFailed(task.getTaskId(), errorMsg);
                        return DocumentTaskResult.builder()
                                .taskId(task.getTaskId())
                                .status(AnalysisHistoryService.STATUS_FAILED)
                                .progress(0)
                                .errorMessage(errorMsg)
                                .build();
                    }

                    // 检查是否是医疗文档（兼容旧逻辑）
                    if (result.containsKey("is_medical_document")
                            && !result.getBoolean("is_medical_document")) {
                        String errorMsg = result.getString("message");
                        analysisHistoryService.markFailed(task.getTaskId(), errorMsg);
                        return DocumentTaskResult.builder()
                                .taskId(task.getTaskId())
                                .status(AnalysisHistoryService.STATUS_FAILED)
                                .progress(0)
                                .errorMessage(errorMsg)
                                .build();
                    }

                    // 检查是否包含有效数据
                    boolean hasValidData = checkHasValidData(result);
                    if (hasValidData) {
                        analysisHistoryService.markSuccess(task.getTaskId(), result);
                        return DocumentTaskResult.builder()
                                .taskId(task.getTaskId())
                                .status(AnalysisHistoryService.STATUS_SUCCESS)
                                .progress(100)
                                .analysisResult(result.toJSONString())
                                .patientName(result.getString("patient_name"))
                                .confidence(result.containsKey("confidence")
                                        ? result.getDouble("confidence") : 0.85)
                                .build();
                    } else {
                        // 部分识别 → WARNING
                        String warning = "未能从图片中识别出完整的医疗信息，请确保图片清晰且为医疗文档";
                        analysisHistoryService.markWarning(task.getTaskId(), result, warning);
                        return DocumentTaskResult.builder()
                                .taskId(task.getTaskId())
                                .status(AnalysisHistoryService.STATUS_WARNING)
                                .progress(100)
                                .analysisResult(result.toJSONString())
                                .errorMessage(warning)
                                .build();
                    }

                } catch (Exception e) {
                    lastError = e;
                    retryCount++;
                    log.warn("任务分析异常: taskId={}, 重试 {}/{}: {}",
                            task.getTaskId(), retryCount, maxRetries, e.getMessage());

                    if (retryCount > maxRetries) {
                        // 超过最大重试次数
                        String errorMsg = "分析失败(" + maxRetries + "次重试): " + lastError.getMessage();
                        analysisHistoryService.markFailed(task.getTaskId(), errorMsg);
                        log.error("任务最终失败: taskId={}, error={}", task.getTaskId(), errorMsg);
                        return DocumentTaskResult.builder()
                                .taskId(task.getTaskId())
                                .status(AnalysisHistoryService.STATUS_FAILED)
                                .progress(0)
                                .errorMessage(errorMsg)
                                .build();
                    }

                    // 指数退避：5s → 10s → 20s
                    long backoff = 5000L * retryCount;
                    try {
                        Thread.sleep(Math.min(backoff, 30000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // 兜底
            analysisHistoryService.markFailed(task.getTaskId(),
                    lastError != null ? lastError.getMessage() : "未知错误");
            return DocumentTaskResult.builder()
                    .taskId(task.getTaskId())
                    .status(AnalysisHistoryService.STATUS_FAILED)
                    .progress(0)
                    .errorMessage(lastError != null ? lastError.getMessage() : "未知错误")
                    .build();
        }, executor);

        future.exceptionally(ex -> {
            log.error("任务执行异常: taskId={}", task.getTaskId(), ex);
            analysisHistoryService.markFailed(task.getTaskId(),
                    "系统异常: " + ex.getMessage());
            return DocumentTaskResult.builder()
                    .taskId(task.getTaskId())
                    .status(AnalysisHistoryService.STATUS_FAILED)
                    .progress(0)
                    .errorMessage("系统异常: " + ex.getMessage())
                    .build();
        });

        return future;
    }

    /**
     * 重新提交失败的任务
     */
    public CompletableFuture<DocumentTaskResult> retryAnalysis(Integer taskId, Integer userId) {
        AnalysisHistory history = analysisHistoryService.getById(taskId, userId);
        if (history == null) {
            return CompletableFuture.failedFuture(
                    new RuntimeException("任务不存在"));
        }

        // 重置状态
        analysisHistoryService.retryTask(taskId, userId);

        DocumentTask task = DocumentTask.builder()
                .taskId(taskId)
                .userId(userId)
                .fileName(history.getFileName())
                .fileType(history.getFileType())
                .imageData(history.getImageUrl())
                .base64(false)
                .build();

        return submitAnalysis(task);
    }

    private boolean checkHasValidData(JSONObject result) {
        if (result.containsKey("patient_name") && result.getString("patient_name") != null
                && !result.getString("patient_name").equals("null")
                && !result.getString("patient_name").isEmpty()) {
            return true;
        }
        if (result.containsKey("diagnosis")) {
            JSONObject diagnosis = result.getJSONObject("diagnosis");
            if (diagnosis != null) {
                String primary = diagnosis.getString("primary_diagnosis");
                if (primary != null && !primary.equals("null") && !primary.isEmpty()) {
                    return true;
                }
            }
        }
        if (result.containsKey("medications") && result.getJSONArray("medications") != null
                && !result.getJSONArray("medications").isEmpty()) {
            return true;
        }
        if (result.containsKey("examinations") && result.getJSONArray("examinations") != null
                && !result.getJSONArray("examinations").isEmpty()) {
            return true;
        }
        return false;
    }
}