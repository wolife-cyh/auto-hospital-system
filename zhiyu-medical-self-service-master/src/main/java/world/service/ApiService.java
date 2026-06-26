package world.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * 智慧医生 AI 服务（LangChain4j 版本）
 * <p>
 * 使用 LangChain4j 抽象层对接通义千问，提供同步/流式两类查询接口。
 * 通过 LangChain4j 解耦模型供应商，未来可无缝切换到 OpenAI / Claude 等。
 */
@Slf4j
@Service
public class ApiService {

    public static final String SYSTEM_PROMPT = "你是智慧医疗系统的智能医生助手。你可以：\n" +
            "1. 分析诊断报告，解释医学术语\n" +
            "2. 提供用药建议和生活注意事项\n" +
            "3. 解答患者关于病情的疑问\n" +
            "4. 给出后续就医建议\n\n" +
            "请注意：你的回答仅供参考，不能替代真实医生的诊断。如症状严重，请建议患者及时就医。";

    public static final String KNOWLEDGE_PRIORITY_INSTRUCTION =
            "\n\n【⚠️ 重要指令——数据库知识优先】\n" +
            "系统已从数据库中检索到与用户问题相关的疾病和药品信息（见下方「参考知识库」）。\n" +
            "你必须**严格遵循以下优先级**回答：\n" +
            "1. **首选**：使用「参考知识库」中的数据回答，包括疾病名称、症状、诱因、所属科室、相关药品等\n" +
            "2. **补充**：在知识库数据基础上，结合你的医学知识进行补充说明\n" +
            "3. **禁止**：不得忽略知识库中已有的信息而凭空回答\n" +
            "4. 如果知识库中有相关药品，必须在回答中明确列出并说明用法\n" +
            "5. 回答时直接引用知识库中的疾病名称、症状描述等关键信息";

    @Resource
    private ChatLanguageModel chatLanguageModel;

    @Resource
    private StreamingChatLanguageModel streamingChatLanguageModel;

    /** 构建默认的系统 + 用户消息列表 */
    private List<ChatMessage> buildDefaultMessages(String queryMessage) {
        return Arrays.asList(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(queryMessage)
        );
    }

    /**
     * 构建融合知识库上下文的系统消息列表（RAG）
     * <p>
     * 将数据库检索到的知识作为系统提示的一部分，与对话历史一起组成完整的消息列表。
     *
     * @param knowledgeContext      知识检索服务返回的结构化知识文本（可为空）
     * @param conversationMessages  原有的对话消息列表（含 SYSTEM + 历史 + USER）
     * @return 增强后的消息列表，系统消息融合了知识库信息
     */
    public List<ChatMessage> buildKnowledgeEnhancedMessages(String knowledgeContext,
                                                             List<ChatMessage> conversationMessages) {
        List<ChatMessage> enhanced = new ArrayList<>();

        // 构建增强版系统提示（知识库数据 + 优先级指令）
        String systemContent = SYSTEM_PROMPT;
        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            systemContent = SYSTEM_PROMPT + KNOWLEDGE_PRIORITY_INSTRUCTION + "\n\n" + knowledgeContext;
        }

        // 检查原列表第一个消息是否是 SYSTEM，若是则替换内容，否则新增
        boolean systemReplaced = false;
        for (ChatMessage msg : conversationMessages) {
            if (!systemReplaced && msg instanceof SystemMessage) {
                enhanced.add(SystemMessage.from(systemContent));
                systemReplaced = true;
            } else {
                enhanced.add(msg);
            }
        }

        // 如果原列表没有 SYSTEM 消息，在最前面添加
        if (!systemReplaced) {
            enhanced.add(0, SystemMessage.from(systemContent));
        }

        return enhanced;
    }

    /** 同步查询（简版，无历史） */
    public String query(String queryMessage) {
        return query(buildDefaultMessages(queryMessage));
    }

    /** 同步查询（带完整消息列表，支持历史上下文） */
    public String query(List<ChatMessage> messages) {
        try {
            Response<AiMessage> response = chatLanguageModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("AI 同步查询失败", e);
            return "智能医生现在不在线，请稍后再试～";
        }
    }

    /** 流式查询（简版，无历史） */
    public void queryStream(String queryMessage,
                            Consumer<String> onToken,
                            Runnable onComplete,
                            Consumer<Exception> onError) {
        queryStream(buildDefaultMessages(queryMessage), onToken, onComplete, onError);
    }

    /** 流式查询（带完整消息列表，支持历史上下文） */
    public void queryStream(List<ChatMessage> messages,
                            Consumer<String> onToken,
                            Runnable onComplete,
                            Consumer<Exception> onError) {
        try {
            streamingChatLanguageModel.generate(messages, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    if (token != null && !token.isEmpty()) {
                        onToken.accept(token);
                    }
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    onComplete.run();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式AI调用失败", error);
                    onError.accept(new Exception(error));
                }
            });
        } catch (Exception e) {
            log.error("流式AI调用初始化失败", e);
            onError.accept(e);
        }
    }
}
