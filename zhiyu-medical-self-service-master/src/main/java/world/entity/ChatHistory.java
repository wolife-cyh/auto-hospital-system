package world.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能医生对话消息实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("chat_history")
public class ChatHistory implements Serializable {

    /** 主键ID */
    private Integer id;

    /** 用户ID */
    private Integer userId;

    /** 会话ID（关联 chat_conversation） */
    private String conversationId;

    /** 角色：user/assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间 */
    private Date createTime;
}
