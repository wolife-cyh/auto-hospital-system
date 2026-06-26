# RAG Implementation Plan: Knowledge-Enhanced AI Chat

## Goal
Enable the AI chat agent to retrieve and use data from the `illness`, `medicine`, and `illness_kind` tables when answering user questions, implementing a RAG (Retrieval-Augmented Generation) pattern.

## Approach: Keyword-Based Retrieval (No Embeddings)

Since the data is in a structured MySQL database (not unstructured documents), we use **keyword matching** via SQL `LIKE` queries rather than vector embeddings. This is:
- Simpler to implement (no vector DB needed)
- Fast (direct SQL queries)
- Well-suited for structured medical data with clear field names

## Architecture Overview

```
User Query → MessageController
                ↓
         KnowledgeRetrievalService.search(query)
                ↓ (SQL LIKE on illness/medicine tables)
         Formatted knowledge context
                ↓
         ApiService (system prompt + knowledge + history + query)
                ↓
         Tongyi Qianwen LLM → Response
```

## Files to Create

### 1. `KnowledgeRetrievalService.java`
- **Path**: `src/main/java/world/service/KnowledgeRetrievalService.java`
- **Purpose**: Search database for relevant medical knowledge based on user query
- **Logic**:
  - Extract potential keywords from user query
  - Search `illness` table: `LIKE` match on `illness_name`, `include_reason`, `illness_symptom`, `special_symptom`
  - For each matched illness: fetch related `illness_kind` (category name) and `medicine` list via `illness_medicine` join
  - Also search `medicine` table: `LIKE` match on `medicine_name`, `keyword`, `medicine_effect`
  - Format results as structured text
  - Return knowledge context string (or empty string if nothing found)

## Files to Modify

### 2. `ApiService.java`
- Add method: `buildKnowledgeEnhancedMessages(String knowledgeContext, List<Message> historyMessages)`
  - Creates a system message containing: SYSTEM_PROMPT + "\n\n【参考知识库】\n" + knowledgeContext
  - Prepends this to the history messages list
- Keep existing methods for backward compatibility

### 3. `MessageController.java`
- Inject `KnowledgeRetrievalService`
- In both `query()` (sync) and `queryStream()` (streaming) methods:
  1. After saving user message, call `knowledgeRetrievalService.retrieveKnowledge(content)` 
  2. Build AI messages with knowledge context via `apiService.buildKnowledgeEnhancedMessages()`
  3. Send to AI as before

## Knowledge Context Format

The retrieved knowledge will be formatted as:

```
【疾病信息】
- 疾病名称: 病毒性感冒
  分类: 传染科
  诱因: 各种导致全身或呼吸道局部防御功能降低的原因...
  主要症状: 急性起病，患者主要表现为鼻塞、流涕...
  特殊症状: 急性起病，患者主要表现为鼻塞、流涕...
  相关药品: 阿莫西林胶囊、999感冒灵颗粒、布洛芬缓释胶囊

【药品信息】
- 药品名称: 999感冒灵颗粒
  功效: 解热镇痛功效，用于因感冒引起的头痛...
  用法用量: 开水冲服，一次1袋，一日3次
  禁忌: 忌烟，酒及辛辣，生冷，油腻食物...
```

## Data Flow (Sync Endpoint Example)

```
POST /message/query { content: "感冒了怎么办", conversationId: "xxx" }

1. Save user message to chat_history
2. knowledgeRetrievalService.retrieveKnowledge("感冒了怎么办")
   → SQL: SELECT * FROM illness WHERE illness_name LIKE '%感冒%' 
          OR include_reason LIKE '%感冒%' OR illness_symptom LIKE '%感冒%'...
   → Returns formatted context with matched illnesses + medicines
3. buildKnowledgeEnhancedMessages(knowledgeContext, historyMessages)
   → System message = SYSTEM_PROMPT + knowledge
   → Followed by conversation history
4. apiService.query(messages) → AI response with medical knowledge
5. Save AI response to chat_history
6. Return response
```

## Key Design Decisions

| Decision | Rationale |
|---|---|
| Keyword matching vs embeddings | Structured data with clear field names; no vector DB dependency |
| Retrieve per-query (not cached per conversation) | Ensures fresh results for each new question |
| Include medicines for matched illnesses | Users often ask about treatment/medication |
| Limit results to top 5 illnesses | Avoid overwhelming the context window |
| Format as structured Chinese text | Matches the existing Chinese medical data and system prompt |
