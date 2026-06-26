package world.controller;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import world.component.OssClient;
import world.dto.RespResult;
import world.entity.AnalysisHistory;
import world.entity.DocumentTask;
import world.entity.Medicine;
import world.entity.User;
import world.service.AnalysisHistoryService;
import world.service.MedicalRecordAnalysisService;
import world.service.MedicineService;
import world.service.TaskService;
import world.utils.PdfExportUtil;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class MedicalScanController {

    @Resource
    private MedicalRecordAnalysisService medicalRecordAnalysisService;

    @Resource
    private AnalysisHistoryService analysisHistoryService;

    @Resource
    private MedicineService medicineService;

    @Resource
    private TaskService taskService;

    @Resource
    private OssClient ossClient;

    /**
     * 页面 - 智能扫描
     */
    @GetMapping("/medical-scan")
    public String medicalScan(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loginUser") == null) {
            redirectAttributes.addFlashAttribute("needLogin", true);
            return "redirect:/index.html";
        }
        return "medical-scan";
    }

    @GetMapping("/analysis-history")
    public String analysisHistory(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("loginUser") == null) {
            redirectAttributes.addFlashAttribute("needLogin", true);
            return "redirect:/index.html";
        }
        return "analysis-history";
    }

    /**
     * 上传诊断书并触发异步分析
     */
    @ResponseBody
    @PostMapping("/api/medical-record/analyze")
    public RespResult analyzeMedicalRecord(@RequestParam("file") MultipartFile file,
                                           HttpSession session) {
        log.info("收到诊断书分析请求: {}", file.getOriginalFilename());

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            // 校验文件
            String filename = file.getOriginalFilename();
            String ext = getExtension(filename);
            if (!isAllowedType(ext)) {
                return RespResult.fail("不支持的文件类型，请上传 jpg/png/jpeg/pdf 格式");
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                return RespResult.fail("文件大小不能超过 10MB");
            }

            // 上传到 OSS（按用户隔离路径）
            String imageUrl = ossClient.upload(file, "medical_records", loginUser.getId());

            // 创建任务记录 (status=PENDING)
            AnalysisHistory history = analysisHistoryService.createTask(
                    loginUser.getId(),
                    filename,
                    file.getContentType(),
                    file.getSize(),
                    imageUrl
            );

            // 提交到线程池异步分析
            DocumentTask task = DocumentTask.builder()
                    .taskId(history.getId())
                    .userId(loginUser.getId())
                    .fileName(filename)
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .imageData(imageUrl)
                    .base64(false)
                    .build();

            taskService.submitAnalysis(task);

            log.info("分析任务已提交: taskId={}, fileName={}", history.getId(), filename);

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", history.getId());
            data.put("status", AnalysisHistoryService.STATUS_PENDING);
            data.put("estimatedWait", "约 10-30 秒完成");

            return RespResult.success("分析任务已提交", data);

        } catch (Exception e) {
            log.error("提交分析任务失败", e);
            return RespResult.fail("提交失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态（供前端轮询）
     */
    @ResponseBody
    @GetMapping("/api/analysis-history/status")
    public RespResult getTaskStatus(@RequestParam Integer id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            AnalysisHistory task = analysisHistoryService.getTaskStatus(id, loginUser.getId());
            if (task == null) {
                return RespResult.fail("任务不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", task.getId());
            data.put("status", task.getStatus());
            data.put("progress", task.getProgress());
            data.put("errorMessage", task.getErrorMessage());
            data.put("createTime", task.getCreateTime());
            data.put("updateTime", task.getUpdateTime());

            return RespResult.success("获取成功", data);
        } catch (Exception e) {
            log.error("查询任务状态失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 重新分析失败/成功的任务
     */
    @ResponseBody
    @PostMapping("/api/analysis-history/retry")
    public RespResult retryAnalysis(@RequestParam Integer id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            AnalysisHistory history = analysisHistoryService.getById(id, loginUser.getId());
            if (history == null) {
                return RespResult.fail("任务不存在");
            }
            if (AnalysisHistoryService.STATUS_PENDING.equals(history.getStatus())
                    || AnalysisHistoryService.STATUS_PROCESSING.equals(history.getStatus())) {
                return RespResult.fail("任务正在处理中，请稍后");
            }

            // 重置并重新提交
            taskService.retryAnalysis(id, loginUser.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("taskId", id);
            data.put("status", AnalysisHistoryService.STATUS_PENDING);

            return RespResult.success("已重新提交分析任务", data);
        } catch (Exception e) {
            log.error("重新分析失败", e);
            return RespResult.fail("操作失败: " + e.getMessage());
        }
    }

    /**
     * 获取分析历史列表（用于异步任务完成后的结果加载）
     */
    @ResponseBody
    @GetMapping("/api/analysis-history/list")
    public RespResult getHistoryList(@RequestParam(defaultValue = "1") Integer pageNum,
                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                     HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            Page<AnalysisHistory> page = analysisHistoryService.getUserHistory(loginUser.getId(), pageNum, pageSize);
            return RespResult.success("获取成功", page);
        } catch (Exception e) {
            log.error("获取历史记录失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 获取分析历史详情
     */
    @ResponseBody
    @GetMapping("/api/analysis-history/detail")
    public RespResult getHistoryDetail(@RequestParam Integer id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            AnalysisHistory history = analysisHistoryService.getById(id, loginUser.getId());
            if (history == null) {
                return RespResult.fail("记录不存在");
            }
            return RespResult.success("获取成功", history);
        } catch (Exception e) {
            log.error("获取历史详情失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    @ResponseBody
    @PostMapping("/api/analysis-history/delete")
    public RespResult deleteHistory(@RequestParam Integer id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            boolean success = analysisHistoryService.deleteById(id, loginUser.getId());
            if (success) {
                return RespResult.success("删除成功");
            } else {
                return RespResult.fail("删除失败");
            }
        } catch (Exception e) {
            log.error("删除历史记录失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 清空用户所有历史记录
     */
    @ResponseBody
    @PostMapping("/api/analysis-history/clear")
    public RespResult clearHistory(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            boolean success = analysisHistoryService.clearUserHistory(loginUser.getId());
            if (success) {
                return RespResult.success("清空成功");
            } else {
                return RespResult.fail("清空失败");
            }
        } catch (Exception e) {
            log.error("清空历史记录失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 根据药品名称查询药品
     */
    @ResponseBody
    @GetMapping("/api/medicine/search")
    public RespResult searchMedicine(@RequestParam String name) {
        try {
            Medicine medicine = medicineService.getByMedicineName(name);
            if (medicine == null) {
                return RespResult.fail("未找到该药品");
            }
            JSONObject result = new JSONObject();
            result.put("id", medicine.getId());
            result.put("name", medicine.getMedicineName());
            result.put("price", medicine.getMedicinePrice());
            result.put("imgPath", medicine.getImgPath());
            return RespResult.success("查询成功", result);
        } catch (Exception e) {
            log.error("查询药品失败", e);
            return RespResult.fail("查询失败");
        }
    }

    /**
     * 导出诊断报告 PDF
     */
    @ResponseBody
    @PostMapping("/api/medical-record/export-pdf")
    public void exportPdf(@RequestBody JSONObject requestData,
                          HttpServletResponse response) {
        try {
            JSONObject analysisData = requestData.getJSONObject("analysisData");
            String fileName = requestData.getString("fileName");

            if (analysisData == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("分析数据为空");
                return;
            }

            byte[] pdfBytes = PdfExportUtil.generateMedicalReport(analysisData, fileName);

            String timestamp = String.valueOf(System.currentTimeMillis());
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=medical_report_" + timestamp + ".pdf");
            response.setContentLength(pdfBytes.length);

            response.getOutputStream().write(pdfBytes);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.error("导出PDF失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("PDF生成失败: " + e.getMessage());
            } catch (Exception ex) {
                log.error("写入错误响应失败", ex);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private static boolean isAllowedType(String ext) {
        return "jpg".equals(ext) || "jpeg".equals(ext)
                || "png".equals(ext) || "pdf".equals(ext);
    }

    /**
     * 批量删除历史记录
     */
    @ResponseBody
    @PostMapping("/api/analysis-history/batch-delete")
    public RespResult batchDelete(@RequestBody java.util.Map<String, Object> params, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> ids = (java.util.List<Integer>) params.get("ids");
            if (ids == null || ids.isEmpty()) {
                return RespResult.fail("请选择要删除的记录");
            }
            int count = analysisHistoryService.batchDelete(ids, loginUser.getId());
            return RespResult.success("成功删除 " + count + " 条记录");
        } catch (Exception e) {
            log.error("批量删除失败", e);
            return RespResult.fail("删除失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户存储统计
     */
    @ResponseBody
    @GetMapping("/api/analysis-history/stats")
    public RespResult getStorageStats(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) {
            return RespResult.fail("请先登录");
        }

        try {
            java.util.Map<String, Object> stats = analysisHistoryService.getStorageStats(loginUser.getId());
            return RespResult.success("获取成功", stats);
        } catch (Exception e) {
            log.error("获取存储统计失败", e);
            return RespResult.fail(e.getMessage());
        }
    }
}
