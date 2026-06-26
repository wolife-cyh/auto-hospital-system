package world.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.Callable;

/**
 * 异步分析任务
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTask implements Callable<DocumentTaskResult> {

    /** 任务记录 ID（analysis_history 主键） */
    private Integer taskId;

    /** 用户 ID */
    private Integer userId;

    /** 文件名 */
    private String fileName;

    /** 文件类型 */
    private String fileType;

    /** 文件大小 */
    private Long fileSize;

    /** 图片数据（Base64 data URL 或 OSS URL） */
    private String imageData;

    /** 是否使用 Base64（true=Base64, false=OSS URL） */
    private boolean base64;

    @Override
    public DocumentTaskResult call() {
        throw new UnsupportedOperationException("Task must be executed by TaskService");
    }
}
