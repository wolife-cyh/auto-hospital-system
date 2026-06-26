package world.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import world.component.OssClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MedicalRecordAnalysisService {

    @Value("${ai-key}")
    private String apiKey;

    @Resource
    private OssClient ossClient;

    private static final String DASHSCOPE_API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";

    private final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public JSONObject uploadAndAnalyze(MultipartFile file) throws IOException {
        validateFileType(file);
        String imageUrl = ossClient.upload(file, "medical_records");
        log.info("诊断书上传成功: {}", imageUrl);
        JSONObject analysisResult = analyzeMedicalRecord(imageUrl);
        analysisResult.put("image_url", imageUrl);
        return analysisResult;
    }

    public JSONObject analyzeMedicalRecord(String imageUrl) {
        log.info("开始调用通义千问VL分析诊断书: {}", imageUrl);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key 未配置，请检查 application.yml 中的 ai-key 配置");
        }

        String urlPreview = imageUrl.length() > 200 ? imageUrl.substring(0, 200) + "..." : imageUrl;
        log.info("使用的图片URL: {}", urlPreview);

        try {
            Request testRequest = new Request.Builder()
                    .url(imageUrl)
                    .head()
                    .build();
            try (Response testResponse = okHttpClient.newCall(testRequest).execute()) {
                log.info("图片URL可访问性测试: 状态码 {}", testResponse.code());
                if (!testResponse.isSuccessful()) {
                    log.warn("图片URL可能无法被公网访问: {}", testResponse.code());
                }
            }
        } catch (Exception e) {
            log.warn("图片URL访问测试失败: {}", e.getMessage());
        }

        String encodedUrl = imageUrl;
        try {
            java.net.URI uri = new java.net.URI(imageUrl);
            encodedUrl = uri.toString();
            log.info("URL编码后: {}", encodedUrl);
        } catch (Exception e) {
            log.warn("URL编码失败，使用原URL: {}", e.getMessage());
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-vl-plus");

        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");

        JSONArray content = new JSONArray();

        JSONObject imageContent = new JSONObject();
        imageContent.put("image", encodedUrl);
        content.add(imageContent);

        JSONObject textContent = new JSONObject();
        textContent.put("text", buildMedicalPrompt());
        content.add(textContent);

        message.put("content", content);
        messages.add(message);
        input.put("messages", messages);
        requestBody.put("input", input);

        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");
        requestBody.put("parameters", parameters);

        log.debug("请求体: {}", requestBody.toJSONString());

        Request request = new Request.Builder()
                .url(DASHSCOPE_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse("application/json")
                ))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.info("响应状态码: {}", response.code());
            log.debug("响应内容: {}", responseBody);

            if (!response.isSuccessful()) {
                log.error("API调用失败: {}, 响应: {}", response.code(), responseBody);
                throw new RuntimeException("AI服务调用失败: " + response.code() + " - " + responseBody);
            }

            return parseResponse(responseBody);
        } catch (IOException e) {
            log.error("调用通义千问VL失败", e);
            throw new RuntimeException("诊断书分析失败: " + e.getMessage(), e);
        }
    }

    public JSONObject uploadAndAnalyzeWithBase64(MultipartFile file) throws IOException {
        validateFileType(file);

        byte[] bytes = file.getBytes();
        String base64Content = java.util.Base64.getEncoder().encodeToString(bytes);
        String contentType = file.getContentType();
        String dataUrl = "data:" + contentType + ";base64," + base64Content;

        log.info("图片已转为Base64，原始大小: {} bytes, Base64长度: {}", bytes.length, base64Content.length());

        return analyzeMedicalRecordWithBase64(dataUrl);
    }

    public JSONObject analyzeMedicalRecordWithBase64(String dataUrl) {
        log.info("开始调用通义千问VL分析诊断书（Base64方式）");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API Key 未配置");
        }

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-vl-plus");

        JSONObject input = new JSONObject();
        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");

        JSONArray content = new JSONArray();

        JSONObject imageContent = new JSONObject();
        imageContent.put("image", dataUrl);
        content.add(imageContent);

        JSONObject textContent = new JSONObject();
        textContent.put("text", buildMedicalPrompt());
        content.add(textContent);

        message.put("content", content);
        messages.add(message);
        input.put("messages", messages);
        requestBody.put("input", input);

        JSONObject parameters = new JSONObject();
        parameters.put("result_format", "message");
        requestBody.put("parameters", parameters);

        log.debug("请求体大小: {} 字符", requestBody.toJSONString().length());

        Request request = new Request.Builder()
                .url(DASHSCOPE_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        requestBody.toJSONString(),
                        MediaType.parse("application/json")
                ))
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.info("响应状态码: {}", response.code());

            if (!response.isSuccessful()) {
                log.error("API调用失败: {}, 响应: {}", response.code(), responseBody);
                throw new RuntimeException("AI服务调用失败: " + response.code() + " - " + responseBody);
            }

            return parseResponse(responseBody);
        } catch (IOException e) {
            log.error("调用通义千问VL失败", e);
            throw new RuntimeException("诊断书分析失败: " + e.getMessage(), e);
        }
    }

    private void validateFileType(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new RuntimeException("文件名为空");
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("pdf")) {
            throw new RuntimeException("不支持的文件类型，请上传图片（jpg、png、jpeg）或PDF文件");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("文件大小不能超过10MB");
        }
    }

    private String buildMedicalPrompt() {
        return "你是一位专业的医疗文档分析专家。请分析这张图片。\n\n" +
                "第一步：判断这张图片是否是医疗相关的文档（诊断书、检验报告、处方、出院小结、病历等）。\n\n" +
                "如果不是医疗文档，请返回：\n" +
                "{\n" +
                "  \"is_medical_document\": false,\n" +
                "  \"message\": \"请上传诊断书、检验报告等医疗文档\",\n" +
                "  \"detected_content_type\": \"识别到的文件类型\"\n" +
                "}\n\n" +
                "如果是医疗文档，请提取以下信息并以JSON格式返回：\n" +
                "{\n" +
                "  \"is_medical_document\": true,\n" +
                "  \"patient_name\": \"患者姓名\",\n" +
                "  \"patient_age\": \"年龄\",\n" +
                "  \"patient_gender\": \"性别\",\n" +
                "  \"diagnosis\": {\n" +
                "    \"primary_diagnosis\": \"主要诊断结论\",\n" +
                "    \"secondary_diagnosis\": \"次要诊断\",\n" +
                "    \"icd_code\": \"疾病编码\"\n" +
                "  },\n" +
                "  \"symptoms\": [\"症状1\", \"症状2\"],\n" +
                "  \"examinations\": [\n" +
                "    {\n" +
                "      \"item\": \"检查项目\",\n" +
                "      \"result\": \"结果\",\n" +
                "      \"reference\": \"参考范围\",\n" +
                "      \"status\": \"正常/偏高/偏低\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"medications\": [\n" +
                "    {\n" +
                "      \"name\": \"药品名称\",\n" +
                "      \"dosage\": \"用法用量\",\n" +
                "      \"frequency\": \"频率\",\n" +
                "      \"duration\": \"周期\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"recommendations\": \"医生建议\",\n" +
                "  \"report_date\": \"报告日期\",\n" +
                "  \"hospital_name\": \"医院名称\",\n" +
                "  \"doctor_name\": \"医生姓名\"\n" +
                "}\n\n" +
                "要求：只输出JSON，不要有其他文字。";
    }

    /**
     * 解析AI响应 - 修复非医疗文档处理逻辑
     */
    private JSONObject parseResponse(String responseBody) {
        JSONObject response = JSON.parseObject(responseBody);

        JSONObject output = response.getJSONObject("output");
        if (output == null) {
            log.error("响应格式异常: 缺少output字段, 响应内容: {}", responseBody);
            throw new RuntimeException("AI服务响应格式异常");
        }

        JSONArray choices = output.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            log.error("响应格式异常: 缺少choices字段");
            throw new RuntimeException("AI服务响应格式异常");
        }

        JSONObject choice = choices.getJSONObject(0);
        JSONObject message = choice.getJSONObject("message");
        if (message == null) {
            throw new RuntimeException("AI服务响应格式异常: 缺少message字段");
        }

        Object contentObj = message.get("content");
        String content = "";

        if (contentObj instanceof JSONArray) {
            JSONArray contentArray = (JSONArray) contentObj;
            if (!contentArray.isEmpty()) {
                JSONObject firstContent = contentArray.getJSONObject(0);
                content = firstContent.getString("text");
            }
        } else if (contentObj instanceof String) {
            content = (String) contentObj;
        }

        if (content == null || content.isEmpty()) {
            log.error("响应格式异常: 缺少content字段");
            throw new RuntimeException("AI服务响应格式异常: 缺少content字段");
        }

        log.info("模型返回的原始内容: {}", content);

        String cleanContent = content;
        if (cleanContent.contains("```json")) {
            cleanContent = cleanContent.replaceAll("```json\\n?", "");
            cleanContent = cleanContent.replaceAll("\\n?```", "");
        } else if (cleanContent.contains("```")) {
            cleanContent = cleanContent.replaceAll("```\\n?", "");
            cleanContent = cleanContent.replaceAll("\\n?```", "");
        }

        try {
            JSONObject result = JSON.parseObject(cleanContent);

            // ✅ 关键修复：非医疗文档不再抛出异常，返回带标识的JSON
            if (result.containsKey("is_medical_document") && !result.getBoolean("is_medical_document")) {
                String errorMsg = result.getString("message");
                String detectedType = result.getString("detected_content_type");
                log.warn("上传的文件不是医疗文档, 识别类型: {}", detectedType);

                // 返回包含错误信息的JSON，由调用方处理
                JSONObject errorResult = new JSONObject();
                errorResult.put("is_medical_document", false);
                errorResult.put("_error", true);
                errorResult.put("_error_type", "NOT_MEDICAL_DOCUMENT");
                errorResult.put("message", errorMsg != null ? errorMsg : "请上传诊断书、检验报告等医疗文档");
                errorResult.put("detected_content_type", detectedType);
                return errorResult;
            }

            ensureFieldExists(result);

            boolean hasValidData = checkHasValidData(result);
            if (!hasValidData) {
                result.put("_warning", "未能从图片中识别出医疗信息，请确保图片清晰且为医疗文档");
            }

            return result;

        } catch (Exception e) {
            // 解析异常，返回原始内容
            log.warn("模型返回的不是标准JSON，返回原始内容: {}", content);
            JSONObject fallback = new JSONObject();
            fallback.put("raw_content", content);
            fallback.put("_error", true);
            fallback.put("_error_type", "PARSE_ERROR");
            fallback.put("message", "AI返回格式异常，请确保上传的是清晰的医疗文档");
            fallback.put("_warning", "AI返回格式异常");
            fallback.put("confidence", 0.5);
            return fallback;
        }
    }

    private boolean checkHasValidData(JSONObject result) {
        if (result.containsKey("is_medical_document") && !result.getBoolean("is_medical_document")) {
            return false;
        }

        if (result.containsKey("patient_name") && result.getString("patient_name") != null
                && !result.getString("patient_name").equals("null") && !result.getString("patient_name").isEmpty()) {
            return true;
        }

        if (result.containsKey("diagnosis")) {
            JSONObject diagnosis = result.getJSONObject("diagnosis");
            if (diagnosis != null) {
                String primaryDiagnosis = diagnosis.getString("primary_diagnosis");
                if (primaryDiagnosis != null && !primaryDiagnosis.equals("null") && !primaryDiagnosis.isEmpty()) {
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

        if (result.containsKey("recommendations") && result.getString("recommendations") != null
                && !result.getString("recommendations").equals("null") && !result.getString("recommendations").isEmpty()) {
            return true;
        }

        return false;
    }

    private void ensureFieldExists(JSONObject result) {
        if (!result.containsKey("is_medical_document")) {
            result.put("is_medical_document", true);
        }
        if (!result.containsKey("patient_name")) result.put("patient_name", null);
        if (!result.containsKey("patient_age")) result.put("patient_age", null);
        if (!result.containsKey("patient_gender")) result.put("patient_gender", null);
        if (!result.containsKey("diagnosis")) {
            JSONObject defaultDiagnosis = new JSONObject();
            defaultDiagnosis.put("primary_diagnosis", null);
            defaultDiagnosis.put("secondary_diagnosis", null);
            defaultDiagnosis.put("icd_code", null);
            result.put("diagnosis", defaultDiagnosis);
        }
        if (!result.containsKey("symptoms")) result.put("symptoms", new JSONArray());
        if (!result.containsKey("examinations")) result.put("examinations", new JSONArray());
        if (!result.containsKey("medications")) result.put("medications", new JSONArray());
        if (!result.containsKey("recommendations")) result.put("recommendations", null);
        if (!result.containsKey("report_date")) result.put("report_date", null);
        if (!result.containsKey("hospital_name")) result.put("hospital_name", null);
        if (!result.containsKey("doctor_name")) result.put("doctor_name", null);
        if (!result.containsKey("confidence")) result.put("confidence", 0.85);
    }
}