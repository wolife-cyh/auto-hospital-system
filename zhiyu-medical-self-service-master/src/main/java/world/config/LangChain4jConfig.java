package world.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.QwenChatModel;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LangChain4j 核心配置
 * <p>
 * 提供 ChatLanguageModel、StreamingChatLanguageModel、EmbeddingModel、
 * EmbeddingStore 等核心 Bean。
 * <p>
 * ChatMemory 为会话级别对象，由 MessageController 按 conversationId 动态创建。
 */
@Configuration
public class LangChain4jConfig {

    @Value("${ai-key}")
    private String apiKey;

    // ==================== Chat Models ====================

    @Bean
    public ChatLanguageModel chatLanguageModel(
            @Value("${langchain4j.dashscope.chat-model.model-name}") String modelName,
            @Value("${langchain4j.dashscope.chat-model.max-tokens}") Integer maxTokens,
            @Value("${langchain4j.dashscope.chat-model.temperature}") Double temperature) {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature.floatValue())
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel(
            @Value("${langchain4j.dashscope.streaming-chat-model.model-name}") String modelName,
            @Value("${langchain4j.dashscope.streaming-chat-model.max-tokens}") Integer maxTokens,
            @Value("${langchain4j.dashscope.streaming-chat-model.temperature}") Double temperature) {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature.floatValue())
                .build();
    }

    // ==================== Embedding Model & Store ====================

    @Bean
    public EmbeddingModel embeddingModel(
            @Value("${langchain4j.dashscope.embedding-model.model-name}") String modelName) {
        return QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    /**
     * 内存向量存储（存储疾病/药品的向量化数据）
     * <p>
     * 当前使用 InMemoryEmbeddingStore，重启后需要 KnowledgeIngestionService 重新摄入。
     * 如需持久化，可替换为 Chroma / Pinecone / PgVector 等实现。
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
