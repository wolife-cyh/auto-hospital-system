package world.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步分析任务结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTaskResult {

    /** 任务记录 ID */
    private Integer taskId;

    /** 最终状态：SUCCESS / WARNING / FAILED */
    private String status;

    /** 进度 100 */
    private Integer progress;

    /** 分析结果 JSON */
    private String analysisResult;

    /** 患者姓名 */
    private String patientName;

    /** 主要诊断 */
    private String primaryDiagnosis;

    /** 置信度 */
    private Double confidence;

    /** 错误信息 */
    private String errorMessage;
}
