package world.controller;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import world.dto.RespResult;
import world.entity.ChatConversation;
import world.entity.ChatHistory;
import world.entity.User;
import world.service.ApiService;
import world.service.ChatConversationService;
import world.service.ChatHistoryService;
import world.service.KnowledgeRetrievalService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能医生消息控制器（LangChain4j 版本）
 * <p>
 * 使用 LangChain4j 标准消息类型替代 DashScope 原生 Message，
 * 保持现有的 RAG + 流式/同步查询架构不变。
 */
@Slf4j
@RestController
@RequestMapping("/message")
public class MessageController extends BaseController<User> {

    @Resource(name = "analysisExecutor")
    private ThreadPoolTaskExecutor executor;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private ChatConversationService chatConversationService;

    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

    @Resource
    private ApiService apiService;

    // ==================== 认证辅助 ====================

    private User getLoginUser(HttpSession session) {
        return (User) session.getAttribute("loginUser");
    }

    private RespResult requireLogin() {
        return RespResult.fail("请先登录");
    }

    // ==================== 构建 AI 上下文(始终从数据库读取) ====================

    /**
     * 从数据库加载对话历史，构建 LangChain4j 消息列表
     */
    private List<ChatMessage> buildAIMessages(String conversationId, Integer userId, String newContent) {
        List<ChatMessage> messages = new ArrayList<>();
        // 系统消息（基础版，不含 RAG，RAG 由 buildKnowledgeEnhancedMessages 动态注入）
        messages.add(SystemMessage.from(ApiService.SYSTEM_PROMPT));

        // 从数据库加载该会话的全部历史（移除 RAG 状态块，避免污染 AI 上下文）
        List<ChatHistory> history = chatHistoryService.getConversationMessages(userId, conversationId);
        for (ChatHistory h : history) {
            String content = h.getContent();
            if (content != null) {
                content = stripRagBlock(content);
            }
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
            }
        }

        // 追加当前用户输入
        if (newContent != null && !newContent.isEmpty()) {
            messages.add(UserMessage.from(newContent));
        }
        return messages;
    }

    /**
     * 移除 RAG 状态块（HTML blockquote 或 Markdown 引用格式）
     */
    private String stripRagBlock(String content) {
        if (content == null) return null;
        if (content.startsWith("<blockquote class=\"rag-")) {
            int idx = content.indexOf("</blockquote>");
            if (idx > 0) {
                content = content.substring(idx + "</blockquote>".length()).trim();
            }
        } else if (content.startsWith("> 📋 **RAG 知识库")) {
            int idx = content.indexOf("\n\n");
            if (idx > 0) {
                content = content.substring(idx + 2).trim();
            }
        }
        return content;
    }

    private void updateTitleIfNeeded(String conversationId, Integer userId, String userMessage) {
        String existingTitle = chatConversationService.getById(conversationId).getTitle();
        if ("新对话".equals(existingTitle) || existingTitle == null) {
            chatConversationService.updateTitle(conversationId, userMessage);
        }
    }

    // ==================== 会话管理 API ====================

    /** 创建新会话 */
    @PostMapping("/conversation/new")
    public RespResult newConversation(HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return requireLogin();
        ChatConversation conversation = chatConversationService.createConversation(loginUser.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversation.getId());
        data.put("createdAt", conversation.getCreatedAt());
        log.info("创建新会话: conversationId={}, userId={}", conversation.getId(), loginUser.getId());
        return RespResult.success("创建成功", data);
    }

    /** 获取会话列表 */
    @GetMapping("/conversation/list")
    public RespResult listConversations(HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return requireLogin();
        List<ChatConversation> list = chatConversationService.getUserConversations(loginUser.getId());
        return RespResult.success("获取成功", list);
    }

    /** 获取指定会话的消息列表 */
    @GetMapping("/conversation/{id}/messages")
    public RespResult getConversationMessages(@PathVariable("id") String conversationId,
                                               HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return requireLogin();
        try {
            List<ChatHistory> messages = chatHistoryService.getConversationMessages(
                    loginUser.getId(), conversationId);
            return RespResult.success("获取成功", messages);
        } catch (Exception e) {
            log.error("获取会话消息失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    /** 删除指定会话 */
    @PostMapping("/conversation/{id}/delete")
    public RespResult deleteConversation(@PathVariable("id") String conversationId,
                                          HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return requireLogin();
        try {
            chatHistoryService.clearConversationMessages(loginUser.getId(), conversationId);
            chatConversationService.deleteById(conversationId, loginUser.getId());
            log.info("删除会话: conversationId={}", conversationId);
            return RespResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除会话失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    /**
     * 删除指定的用户-AI消息对（用于用户修改对话后清理旧数据）
     */
    @PostMapping("/history/delete-pair")
    public RespResult deleteMessagePair(@RequestBody java.util.Map<String, Object> params,
                                         HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return requireLogin();
        try {
            @SuppressWarnings("unchecked")
            java.util.List<Integer> ids = (java.util.List<Integer>) params.get("ids");
            String conversationId = (String) params.get("conversationId");
            if (ids == null || ids.isEmpty()) {
                return RespResult.fail("请指定要删除的消息");
            }
            if (conversationId == null) {
                return RespResult.fail("缺少会话标识");
            }
            int count = chatHistoryService.deleteByIds(ids, loginUser.getId(), conversationId);
            return RespResult.success("已删除 " + count + " 条消息");
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return RespResult.fail(e.getMessage());
        }
    }

    // ==================== RAG 状态块 ====================

    /**
     * 构建 RAG 知识库状态块（持久化到 DB，命中=绿色，未命中=灰色）
     */
    private String buildRagStatusBlock(String knowledge) {
        if (knowledge != null && !knowledge.isEmpty()) {
            StringBuilder summary = new StringBuilder();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(?:疾病名称|药品名称)[：:]\\s*(\\S+)")
                    .matcher(knowledge);
            while (m.find()) {
                if (summary.length() > 0) summary.append("、");
                summary.append(m.group(1));
            }
            java.util.regex.Matcher km = java.util.regex.Pattern
                    .compile("所属科室[：:]\\s*(\\S+)")
                    .matcher(knowledge);
            if (km.find()) {
                summary.append(" [").append(km.group(1)).append("]");
            }
            String hitInfo = summary.length() > 0 ? summary.toString() : "已检索到相关疾病/药品信息";
            return "<blockquote class=\"rag-hit\"><p>📋 <b>RAG 知识库 ✅命中</b> | " + hitInfo + "</p></blockquote>\n";
        } else {
            return "<blockquote class=\"rag-miss\"><p>📋 <b>RAG 知识库 ❌未命中</b> | 该问题未匹配到数据库中的疾病/药品信息，以下回答基于通用医学知识</p></blockquote>\n";
        }
    }

    // ==================== 同步查询(对话) ====================

    @PostMapping("/query")
    public RespResult query(@RequestParam String content,
                            @RequestParam String conversationId,
                            HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) return requireLogin();

        if (content == null || content.trim().isEmpty()) {
            return RespResult.fail("请输入咨询内容");
        }

        try {
            // 保存用户消息 + 更新标题 + 构建 AI 上下文
            chatHistoryService.saveMessage(loginUser.getId(), conversationId, "user", content);
            chatConversationService.touchConversation(conversationId);
            updateTitleIfNeeded(conversationId, loginUser.getId(), content);

            List<ChatMessage> messages = buildAIMessages(conversationId, loginUser.getId(), null);

            // RAG: 双路混合检索（语义 + 关键词）
            String knowledge = knowledgeRetrievalService.retrieveKnowledge(content);
            List<ChatMessage> enhancedMessages = apiService.buildKnowledgeEnhancedMessages(knowledge, messages);

            String result = apiService.query(enhancedMessages);

            // RAG 状态块（命中/未命中均持久化到 DB）
            String ragBlock = buildRagStatusBlock(knowledge);
            String displayResult = ragBlock + result;

            chatHistoryService.saveMessage(loginUser.getId(), conversationId, "assistant", displayResult);
            chatConversationService.touchConversation(conversationId);

            log.info("AI回复成功: conversationId={}, ragHit={}", conversationId, !knowledge.isEmpty());
            return RespResult.success(displayResult);

        } catch (Exception e) {
            log.error("智能医生调用失败", e);
            return RespResult.fail("AI服务调用失败: " + e.getMessage());
        }
    }

    // ==================== 流式查询(对话) ====================

    @GetMapping("/query/stream")
    public SseEmitter queryStream(@RequestParam String content,
                                   @RequestParam String conversationId,
                                   HttpSession session) {
        User loginUser = getLoginUser(session);
        if (loginUser == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error-event").data("请先登录"));
            } catch (IOException e) {}
            emitter.complete();
            return emitter;
        }

        // 先保存用户消息 + 更新标题
        chatHistoryService.saveMessage(loginUser.getId(), conversationId, "user", content);
        chatConversationService.touchConversation(conversationId);
        updateTitleIfNeeded(conversationId, loginUser.getId(), content);

        List<ChatMessage> messages = buildAIMessages(conversationId, loginUser.getId(), null);

        // RAG: 双路混合检索（语义 + 关键词）
        String knowledge = knowledgeRetrievalService.retrieveKnowledge(content);
        List<ChatMessage> enhancedMessages = apiService.buildKnowledgeEnhancedMessages(knowledge, messages);

        Integer userId = loginUser.getId();

        SseEmitter emitter = new SseEmitter(180000L);

        executor.execute(() -> {
            try {
                StringBuilder fullResponse = new StringBuilder();

                // RAG 状态块：先发送
                String ragBlock = buildRagStatusBlock(knowledge);
                fullResponse.append(ragBlock);
                try {
                    emitter.send(SseEmitter.event().data(ragBlock));
                } catch (IOException e) { /* client disconnected */ }

                apiService.queryStream(enhancedMessages,
                    token -> {
                        if (token == null) return;
                        fullResponse.append(token);
                        try {
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            // Client disconnected
                        }
                    },
                    () -> {
                        // 流结束，保存 AI 回复
                        String reply = fullResponse.toString();
                        chatHistoryService.saveMessage(userId, conversationId, "assistant", reply);
                        chatConversationService.touchConversation(conversationId);
                        try {
                            emitter.send(SseEmitter.event().name("done").data(""));
                        } catch (IOException e) {}
                        emitter.complete();
                    },
                    e -> {
                        log.error("流式AI调用失败", e);
                        try {
                            String errMsg = e.getMessage();
                            emitter.send(SseEmitter.event()
                                .name("error-event")
                                .data(errMsg != null ? errMsg : "AI服务异常"));
                        } catch (IOException ex) {}
                        emitter.completeWithError(e);
                    }
                );
            } catch (Exception e) {
                log.error("流式处理异常", e);
                try {
                    String errMsg = e.getMessage() != null ? e.getMessage() : "未知异常";
                    emitter.send(SseEmitter.event()
                        .name("error-event")
                        .data("AI服务异常: " + errMsg));
                } catch (IOException ex) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
