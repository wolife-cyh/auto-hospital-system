package world.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@Data
@TableName("analysis_history")
public class AnalysisHistory {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String fileName;

    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    private String imageUrl;

    /** 任务状态：PENDING/PROCESSING/SUCCESS/WARNING/FAILED */
    private String status;

    /** 分析进度 0-100 */
    private Integer progress;

    /** 失败原因 */
    private String errorMessage;

    /** AI 置信度 */
    private Double confidence;

    private String analysisResult;

    private String patientName;

    private String primaryDiagnosis;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
