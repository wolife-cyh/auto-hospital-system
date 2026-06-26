package world.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import world.dao.ChatHistoryDao;
import world.entity.ChatHistory;

import java.util.Date;
import java.util.List;

/**
 * 智能医生对话消息服务
 */
@Slf4j
@Service
public class ChatHistoryService {

    @Resource
    private ChatHistoryDao chatHistoryDao;

    /**
     * 保存一条对话消息
     */
    public ChatHistory saveMessage(Integer userId, String conversationId, String role, String content) {
        ChatHistory record = ChatHistory.builder()
                .userId(userId)
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .createTime(new Date())
                .build();
        chatHistoryDao.insert(record);
        return record;
    }

    /**
     * 获取指定会话的所有消息（按时间正序）
     */
    public List<ChatHistory> getConversationMessages(Integer userId, String conversationId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, conversationId)
                .orderByAsc(ChatHistory::getCreateTime);
        return chatHistoryDao.selectList(wrapper);
    }

    /**
     * 获取指定会话中用户的第一条消息（用于生成标题）
     */
    public String getFirstUserMessage(Integer userId, String conversationId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, conversationId)
                .eq(ChatHistory::getRole, "user")
                .orderByAsc(ChatHistory::getCreateTime)
                .last("LIMIT 1");
        ChatHistory record = chatHistoryDao.selectOne(wrapper);
        return record != null ? record.getContent() : null;
    }

    /**
     * 清空指定会话的所有消息
     */
    public int clearConversationMessages(Integer userId, String conversationId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, conversationId);
        return chatHistoryDao.delete(wrapper);
    }

    /**
     * 清空用户所有对话消息
     */
    public int clearAll(Integer userId) {
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatHistory::getUserId, userId);
        return chatHistoryDao.delete(wrapper);
    }

    /**
     * 按 ID 列表删除消息（仅限当前用户 + 当前会话双重隔离）
     */
    public int deleteByIds(java.util.List<Integer> ids, Integer userId, String conversationId) {
        if (ids == null || ids.isEmpty()) return 0;
        LambdaQueryWrapper<ChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(ChatHistory::getId, ids)
                .eq(ChatHistory::getUserId, userId)
                .eq(ChatHistory::getConversationId, conversationId);
        return chatHistoryDao.delete(wrapper);
    }
}
