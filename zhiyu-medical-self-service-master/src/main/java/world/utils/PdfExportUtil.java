package world.utils;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public class PdfExportUtil {

    /**
     * 生成诊断分析报告PDF（诊断书风格 - iText 7版本）
     */
    public static byte[] generateMedicalReport(com.alibaba.fastjson2.JSONObject analysisData, String fileName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(50, 50, 50, 50);

            // 创建中文字体（修复版 - iText 7 正确写法）
            PdfFont chineseFont = createChineseFont();

            // 添加页眉装饰线
            addHeaderDecoration(document);

            // 添加标题
            addTitle(document, chineseFont);

            // 添加报告基本信息卡片
            addReportInfoCard(document, fileName, chineseFont);

            // 患者基本信息卡片
            addPatientInfoCard(document, analysisData, chineseFont);

            // 诊断结论卡片
            addDiagnosisCard(document, analysisData, chineseFont);

            // 症状表现卡片
            addSymptomsCard(document, analysisData, chineseFont);

            // 检验检查项目表格
            addExaminationsTable(document, analysisData, chineseFont);

            // 用药建议表格
            addMedicationsTable(document, analysisData, chineseFont);

            // 医生建议卡片
            addRecommendationsCard(document, analysisData, chineseFont);

            // 医院信息栏
            addHospitalFooter(document, analysisData, chineseFont);

            // 添加免责声明
            addDisclaimer(document, chineseFont);

            document.close();
            pdfDoc.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("生成PDF失败", e);
            throw new RuntimeException("PDF生成失败: " + e.getMessage());
        }
    }

    /**
     * 创建中文字体（iText 7 兼容写法）
     */
    private static PdfFont createChineseFont() {
        try {
            // 方法1：使用系统字体（推荐，使用 Windows 自带宋体）
            String fontPath = "C:/Windows/Fonts/simsun.ttc,0";
            return PdfFontFactory.createFont(fontPath, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
        } catch (Exception e1) {
            try {
                // 方法2：使用 iText 自带的亚洲字体包
                return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception e2) {
                try {
                    // 方法3：使用默认字体（可能不支持中文，但不报错）
                    return PdfFontFactory.createFont();
                } catch (Exception e3) {
                    log.warn("无法加载中文字体，使用默认字体，中文可能显示异常");
                    return null;
                }
            }
        }
    }

    /**
     * 添加页眉装饰线
     */
    private static void addHeaderDecoration(Document document) {
        Table headerTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        headerTable.setMarginBottom(10);
        Cell headerCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(ColorConstants.BLUE)
                .setHeight(5);
        headerTable.addCell(headerCell);
        document.add(headerTable);
    }

    /**
     * 添加标题
     */
    private static void addTitle(Document document, PdfFont font) {
        Paragraph mainTitle = new Paragraph("智愈医疗 · 智能诊断分析报告");
        if (font != null) mainTitle.setFont(font);
        mainTitle.setFontSize(24)
                .setBold()
                .setFontColor(ColorConstants.BLUE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(8);
        document.add(mainTitle);

        Paragraph subTitle = new Paragraph("AI-Powered Medical Diagnosis Analysis Report");
        if (font != null) subTitle.setFont(font);
        subTitle.setFontSize(10)
                .setFontColor(ColorConstants.DARK_GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subTitle);
    }

    /**
     * 添加报告基本信息卡片
     */
    private static void addReportInfoCard(Document document, String fileName, PdfFont font) {
        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth();

        String reportNo = "RPT" + System.currentTimeMillis();
        String createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        addInfoRow(infoTable, "报告编号：", reportNo, font);
        addInfoRow(infoTable, "生成时间：", createTime, font);
        addInfoRow(infoTable, "原始文件：", fileName, font);
        addInfoRow(infoTable, "分析引擎：", "通义千问VL大模型 v1.0", font);

        contentCell.add(infoTable);
        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加患者基本信息卡片
     */
    private static void addPatientInfoCard(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell titleCell = new Cell()
                .add(new Paragraph("患者基本信息").setFont(font).setFontSize(13).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);
        card.addCell(titleCell);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{20, 80}))
                .useAllAvailableWidth();

        addInfoRow(infoTable, "姓名：", getValue(data, "patient_name", "—"), font);
        addInfoRow(infoTable, "年龄：", getValue(data, "patient_age", "—"), font);
        addInfoRow(infoTable, "性别：", getValue(data, "patient_gender", "—"), font);

        contentCell.add(infoTable);
        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加诊断结论卡片
     */
    private static void addDiagnosisCard(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell titleCell = new Cell()
                .add(new Paragraph("诊断结论").setFont(font).setFontSize(13).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);
        card.addCell(titleCell);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        com.alibaba.fastjson2.JSONObject diagnosis = data.getJSONObject("diagnosis");

        if (diagnosis != null && hasValidValue(diagnosis, "primary_diagnosis")) {
            Paragraph diagnosisPara = new Paragraph(getValue(diagnosis, "primary_diagnosis", "待查"));
            if (font != null) diagnosisPara.setFont(font);
            diagnosisPara.setFontSize(13)
                    .setBold()
                    .setFontColor(ColorConstants.BLUE)
                    .setMarginBottom(10);
            contentCell.add(diagnosisPara);

            String secondary = getValue(diagnosis, "secondary_diagnosis", null);
            if (secondary != null && !secondary.equals("null")) {
                Paragraph secondaryPara = new Paragraph("合并症：" + secondary);
                if (font != null) secondaryPara.setFont(font);
                secondaryPara.setFontSize(10).setMarginBottom(5);
                contentCell.add(secondaryPara);
            }

            String icdCode = getValue(diagnosis, "icd_code", null);
            if (icdCode != null && !icdCode.equals("null")) {
                Paragraph icdPara = new Paragraph("ICD-10编码：" + icdCode);
                if (font != null) icdPara.setFont(font);
                icdPara.setFontSize(9).setFontColor(ColorConstants.GRAY);
                contentCell.add(icdPara);
            }
        } else {
            Paragraph emptyPara = new Paragraph("暂无诊断信息");
            if (font != null) emptyPara.setFont(font);
            emptyPara.setFontSize(11).setFontColor(ColorConstants.DARK_GRAY);
            contentCell.add(emptyPara);
        }

        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加症状表现卡片
     */
    private static void addSymptomsCard(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        com.alibaba.fastjson2.JSONArray symptoms = data.getJSONArray("symptoms");
        if (symptoms == null || symptoms.isEmpty()) {
            return;
        }

        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell titleCell = new Cell()
                .add(new Paragraph("症状表现").setFont(font).setFontSize(13).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);
        card.addCell(titleCell);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        for (int i = 0; i < symptoms.size(); i++) {
            String symptom = symptoms.getString(i);
            if (symptom != null && !symptom.equals("null") && !symptom.isEmpty()) {
                Paragraph symptomPara = new Paragraph("•  " + symptom);
                if (font != null) symptomPara.setFont(font);
                symptomPara.setMarginBottom(5);
                contentCell.add(symptomPara);
            }
        }

        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加检验检查项目表格
     */
    private static void addExaminationsTable(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        com.alibaba.fastjson2.JSONArray examinations = data.getJSONArray("examinations");
        if (examinations == null || examinations.isEmpty()) {
            return;
        }

        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell titleCell = new Cell()
                .add(new Paragraph("检验检查结果").setFont(font).setFontSize(13).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);
        card.addCell(titleCell);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        float[] columnWidths = {30, 20, 25, 25};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        // 表头
        String[] headers = {"检查项目", "结果", "参考范围", "判定"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setFont(font).setFontSize(10).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(headerCell);
        }

        // 数据行
        for (int i = 0; i < examinations.size(); i++) {
            com.alibaba.fastjson2.JSONObject exam = examinations.getJSONObject(i);
            String item = getValue(exam, "item", "—");
            String result = getValue(exam, "result", "—");
            String reference = getValue(exam, "reference", "—");
            String status = getValue(exam, "status", "—");

            table.addCell(new Cell().add(new Paragraph(item).setFont(font).setFontSize(9)).setPadding(6));
            table.addCell(new Cell().add(new Paragraph(result).setFont(font).setFontSize(9)).setPadding(6));
            table.addCell(new Cell().add(new Paragraph(reference).setFont(font).setFontSize(9)).setPadding(6));

            Paragraph statusPara = new Paragraph(status).setFont(font).setFontSize(9);
            if ("偏高".equals(status) || "偏低".equals(status)) {
                statusPara.setFontColor(ColorConstants.RED).setBold();
            } else if ("正常".equals(status)) {
                statusPara.setFontColor(ColorConstants.GREEN).setBold();
            }
            table.addCell(new Cell().add(statusPara).setPadding(6));
        }

        contentCell.add(table);
        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加用药建议表格
     */
    private static void addMedicationsTable(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        com.alibaba.fastjson2.JSONArray medications = data.getJSONArray("medications");
        if (medications == null || medications.isEmpty()) {
            return;
        }

        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell titleCell = new Cell()
                .add(new Paragraph("用药建议").setFont(font).setFontSize(13).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);
        card.addCell(titleCell);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        float[] columnWidths = {30, 35, 35};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        String[] headers = {"药品名称", "用法用量", "用药周期"};
        for (String header : headers) {
            Cell headerCell = new Cell()
                    .add(new Paragraph(header).setFont(font).setFontSize(10).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(ColorConstants.BLUE)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(headerCell);
        }

        for (int i = 0; i < medications.size(); i++) {
            com.alibaba.fastjson2.JSONObject med = medications.getJSONObject(i);
            String name = getValue(med, "name", "—");
            String dosage = getValue(med, "dosage", "—");
            String frequency = getValue(med, "frequency", "—");
            String duration = getValue(med, "duration", "—");

            String usage = dosage;
            if (frequency != null && !frequency.equals("—")) {
                usage = dosage + "，" + frequency;
            }

            table.addCell(new Cell().add(new Paragraph(name).setFont(font).setFontSize(9)).setPadding(6));
            table.addCell(new Cell().add(new Paragraph(usage).setFont(font).setFontSize(9)).setPadding(6));
            table.addCell(new Cell().add(new Paragraph(duration).setFont(font).setFontSize(9)).setPadding(6));
        }

        contentCell.add(table);
        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加医生建议卡片
     */
    private static void addRecommendationsCard(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        String recommendations = getValue(data, "recommendations", null);
        if (recommendations == null || recommendations.equals("null") || recommendations.isEmpty()) {
            return;
        }

        Table card = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        card.setMarginBottom(15);

        Cell titleCell = new Cell()
                .add(new Paragraph("医嘱建议").setFont(font).setFontSize(13).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ColorConstants.BLUE)
                .setPadding(10)
                .setBorder(Border.NO_BORDER);
        card.addCell(titleCell);

        Cell contentCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(12);

        Paragraph advicePara = new Paragraph(recommendations);
        if (font != null) advicePara.setFont(font);
        advicePara.setFontSize(10).setMarginBottom(0);
        contentCell.add(advicePara);

        card.addCell(contentCell);
        document.add(card);
    }

    /**
     * 添加医院信息栏
     */
    private static void addHospitalFooter(Document document, com.alibaba.fastjson2.JSONObject data, PdfFont font) {
        Table footerTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        footerTable.setMarginTop(20);

        Cell footerCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(10);

        String hospitalName = getValue(data, "hospital_name", "智愈医疗合作医院");
        String doctorName = getValue(data, "doctor_name", "AI智能医生");
        String reportDate = getValue(data, "report_date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        double confidence = data.getDoubleValue("confidence");
        String confidenceStr = String.format("%.0f%%", confidence * 100);

        String infoText = "诊断医院：" + hospitalName +
                "    |    诊断医师：" + doctorName +
                "    |    报告日期：" + reportDate +
                "    |    置信度：" + confidenceStr;

        Paragraph info = new Paragraph(infoText);
        if (font != null) info.setFont(font);
        info.setFontSize(9).setTextAlignment(TextAlignment.CENTER);

        footerCell.add(info);
        footerTable.addCell(footerCell);
        document.add(footerTable);
    }

    /**
     * 添加免责声明
     */
    private static void addDisclaimer(Document document, PdfFont font) {
        Table disclaimerTable = new Table(UnitValue.createPercentArray(1)).useAllAvailableWidth();
        disclaimerTable.setMarginTop(15);

        Cell disclaimerCell = new Cell().setBorder(Border.NO_BORDER);

        String disclaimerText = "——————————————————————————————————————————————————\n" +
                "免责声明：本报告由「智愈」医疗自助服务系统AI智能生成，基于通义千问VL大模型分析结果。\n" +
                "报告内容仅供参考，不能替代专业医生的诊断和建议。如有身体不适，请及时就医。";

        Paragraph disclaimer = new Paragraph(disclaimerText);
        if (font != null) disclaimer.setFont(font);
        disclaimer.setTextAlignment(TextAlignment.CENTER)
                .setFontSize(7)
                .setFontColor(ColorConstants.GRAY);

        disclaimerCell.add(disclaimer);
        disclaimerTable.addCell(disclaimerCell);
        document.add(disclaimerTable);
    }

    // ==================== 辅助方法 ====================

    private static void addInfoRow(Table table, String label, String value, PdfFont font) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(10).setBold())
                .setBorder(Border.NO_BORDER)
                .setPadding(4);
        table.addCell(labelCell);

        Cell valueCell = new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(4);
        table.addCell(valueCell);
    }

    private static String getValue(com.alibaba.fastjson2.JSONObject obj, String key, String defaultValue) {
        if (obj == null) return defaultValue;
        Object value = obj.get(key);
        if (value == null) return defaultValue;
        String str = value.toString();
        if ("null".equals(str) || str.isEmpty()) return defaultValue;
        return str;
    }

    private static boolean hasValidValue(com.alibaba.fastjson2.JSONObject obj, String key) {
        String value = getValue(obj, key, null);
        return value != null && !value.equals("null") && !value.isEmpty();
    }
}
