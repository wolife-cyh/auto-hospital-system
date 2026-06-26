package world.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import world.dao.ChatConversationDao;
import world.entity.ChatConversation;

import java.util.Date;
import java.util.List;

/**
 * 智能医生会话管理服务
 */
@Slf4j
@Service
public class ChatConversationService {

    @Resource
    private ChatConversationDao chatConversationDao;

    /**
     * 创建新会话
     */
    public ChatConversation createConversation(Integer userId) {
        ChatConversation conversation = ChatConversation.builder()
                .id(IdUtil.simpleUUID())
                .userId(userId)
                .title("新对话")
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
        chatConversationDao.insert(conversation);
        return conversation;
    }

    /**
     * 更新会话标题（取第一条用户消息的前 50 字）
     */
    public void updateTitle(String conversationId, String firstMessage) {
        if (firstMessage == null || firstMessage.isEmpty()) return;
        String title = firstMessage.length() > 50
                ? firstMessage.substring(0, 50) + "…"
                : firstMessage;
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getId, conversationId)
                .set(ChatConversation::getTitle, title)
                .set(ChatConversation::getUpdatedAt, new Date());
        chatConversationDao.update(null, wrapper);
        log.debug("更新会话标题: conversationId={}, title={}", conversationId, title);
    }

    /**
     * 更新最后活动时间
     */
    public void touchConversation(String conversationId) {
        LambdaUpdateWrapper<ChatConversation> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ChatConversation::getId, conversationId)
                .set(ChatConversation::getUpdatedAt, new Date());
        chatConversationDao.update(null, wrapper);
    }

    /**
     * 获取用户的所有会话列表（按最后活动时间倒序）
     */
    public List<ChatConversation> getUserConversations(Integer userId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .orderByDesc(ChatConversation::getUpdatedAt);
        return chatConversationDao.selectList(wrapper);
    }

    /**
     * 根据 ID 获取会话
     */
    public ChatConversation getById(String conversationId) {
        return chatConversationDao.selectById(conversationId);
    }

    /**
     * 删除会话（仅限当前用户）
     */
    public int deleteById(String conversationId, Integer userId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getId, conversationId)
                .eq(ChatConversation::getUserId, userId);
        return chatConversationDao.delete(wrapper);
    }

    /**
     * 清空用户所有会话
     */
    public int clearAll(Integer userId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId);
        return chatConversationDao.delete(wrapper);
    }
}
