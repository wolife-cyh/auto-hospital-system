package world.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 智能医生会话实体（对应 DeepSeek 等页面的边栏对话列表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("chat_conversation")
public class ChatConversation implements Serializable {

    /** 会话ID（UUID） */
    private String id;

    /** 用户ID */
    private Integer userId;

    /** 会话标题（自动取第一条用户消息的前 50 字） */
    private String title;

    /** 创建时间 */
    private Date createdAt;

    /** 最后活动时间 */
    private Date updatedAt;
}
