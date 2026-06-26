package world.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import world.entity.ChatHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 基于数据库的 ChatMemoryStore 实现
 * <p>
 * 将 LangChain4j 的 ChatMemory 持久化到现有的 ChatHistory 表。
 * memoryId 使用 {@link MemoryKey} 组合键（userId + conversationId）。
 */
@Slf4j
@Component
public class DbChatMemoryStore implements ChatMemoryStore {

    @Resource
    private ChatHistoryService chatHistoryService;

    /**
     * ChatMemory 的组合键
     */
    public record MemoryKey(Integer userId, String conversationId) {
        public MemoryKey {
            Objects.requireNonNull(userId, "userId must not be null");
            Objects.requireNonNull(conversationId, "conversationId must not be null");
        }

        public static MemoryKey of(Integer userId, String conversationId) {
            return new MemoryKey(userId, conversationId);
        }
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        MemoryKey key = (MemoryKey) memoryId;
        List<ChatHistory> histories = chatHistoryService.getConversationMessages(
                key.userId(), key.conversationId());

        List<ChatMessage> messages = new ArrayList<>();
        for (ChatHistory h : histories) {
            String content = cleanRagBlock(h.getContent());
            if (content == null || content.isEmpty()) {
                continue;
            }
            switch (h.getRole().toLowerCase()) {
                case "user":
                    messages.add(UserMessage.from(content));
                    break;
                case "assistant":
                    messages.add(AiMessage.from(content));
                    break;
                case "system":
                    messages.add(SystemMessage.from(content));
                    break;
                default:
                    log.warn("未知消息角色: {}, 按 user 处理", h.getRole());
                    messages.add(UserMessage.from(content));
            }
        }
        return messages;
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        MemoryKey key = (MemoryKey) memoryId;

        // 清除旧消息
        chatHistoryService.clearConversationMessages(key.userId(), key.conversationId());

        // 写入新消息（跳过 system 消息，系统提示由 ApiService 动态注入）
        for (ChatMessage msg : messages) {
            String role;
            String content;

            if (msg instanceof UserMessage) {
                role = "user";
                content = ((UserMessage) msg).singleText();
            } else if (msg instanceof AiMessage) {
                role = "assistant";
                content = ((AiMessage) msg).text();
            } else if (msg instanceof SystemMessage) {
                // System 消息不持久化到 DB，由业务层动态注入
                continue;
            } else {
                role = "assistant";
                content = msg.toString();
            }

            if (content != null && !content.isEmpty()) {
                chatHistoryService.saveMessage(key.userId(), key.conversationId(), role, content);
            }
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        MemoryKey key = (MemoryKey) memoryId;
        chatHistoryService.clearConversationMessages(key.userId(), key.conversationId());
    }

    /**
     * 移除消息中可能存在的 RAG 状态块标记（HTML / Markdown 格式）
     * <p>
     * 避免 RAG 状态标记污染 AI 对话历史上下文。
     */
    private String cleanRagBlock(String content) {
        if (content == null) {
            return null;
        }
        // 移除 HTML RAG 块
        if (content.startsWith("<blockquote class=\"rag-")) {
            int idx = content.indexOf("</blockquote>");
            if (idx > 0) {
                content = content.substring(idx + "</blockquote>".length()).trim();
            }
        }
        // 移除 Markdown RAG 块
        if (content.startsWith("> 📋 **RAG 知识库")) {
            int idx = content.indexOf("\n\n");
            if (idx > 0) {
                content = content.substring(idx + 2).trim();
            }
        }
        return content;
    }
}
