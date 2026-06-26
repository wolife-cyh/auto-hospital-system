package world.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import world.dao.IllnessDao;
import world.dao.IllnessKindDao;
import world.dao.IllnessMedicineDao;
import world.dao.MedicineDao;
import world.entity.Illness;
import world.entity.IllnessKind;
import world.entity.IllnessMedicine;
import world.entity.Medicine;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识检索服务（RAG — LangChain4j 增强版）
 * <p>
 * 采用双路混合检索策略：
 * <ol>
 *   <li><b>向量语义检索</b>：通过 EmbeddingModel 将用户问题向量化，在 EmbeddingStore 中做语义相似度搜索</li>
 *   <li><b>关键词精确检索</b>：通过 SQL LIKE 在疾病/药品表中做关键词匹配（适配中文医疗术语）</li>
 * </ol>
 * 两条路径的结果合并去重后，格式化为结构化文本作为 AI 的参考知识上下文。
 */
@Slf4j
@Service
public class KnowledgeRetrievalService {

    @Resource
    private IllnessDao illnessDao;

    @Resource
    private IllnessKindDao illnessKindDao;

    @Resource
    private IllnessMedicineDao illnessMedicineDao;

    @Resource
    private MedicineDao medicineDao;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /** 向量检索最多返回数 */
    @Value("${langchain4j.rag.embedding.max-results}")
    private int embeddingMaxResults;

    /** 向量检索最低相似度阈值 */
    @Value("${langchain4j.rag.embedding.min-score}")
    private double embeddingMinScore;

    /** 关键词检索最多返回的疾病数量 */
    private static final int MAX_ILLNESS = 5;

    /** 关键词检索最多返回的药品数量 */
    private static final int MAX_MEDICINE = 3;

    // ==================== 公开 API ====================

    /**
     * 根据用户查询检索相关知识（双路混合检索）
     *
     * @param userQuery 用户输入的问题
     * @return 格式化的知识上下文，未检索到则返回空字符串
     */
    public String retrieveKnowledge(String userQuery) {
        if (StrUtil.isBlank(userQuery)) {
            return "";
        }

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  RAG 双路检索 ── 开始                                  ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║  用户问题: {}", truncateLog(userQuery, 80));
        log.info("║  向量阈值: minScore={}, maxResults={}", embeddingMinScore, embeddingMaxResults);
        log.info("║  关键词限制: maxIllness={}, maxMedicine={}", MAX_ILLNESS, MAX_MEDICINE);
        log.info("╚══════════════════════════════════════════════════════════╝");

        // ====== 路径1：向量语义检索 ======
        Set<Integer> embeddingIllnessIds = new LinkedHashSet<>();
        Set<Integer> embeddingMedicineIds = new LinkedHashSet<>();
        try {
            searchByEmbedding(userQuery, embeddingIllnessIds, embeddingMedicineIds);
        } catch (Exception e) {
            log.warn("向量语义检索异常，回退到纯关键词检索: {}", e.getMessage());
        }

        // ====== 路径2：关键词精确检索 ======
        List<String> keywords = extractKeywords(userQuery);
        List<Illness> keywordIllnesses = Collections.emptyList();
        List<Medicine> keywordMedicines = Collections.emptyList();
        if (!keywords.isEmpty()) {
            keywordIllnesses = searchIllnesses(keywords);
            keywordMedicines = searchMedicines(keywords);
        }

        // ====== 结果合并去重（按 ID） ======
        // 语义检索命中的 ID 优先，再补充关键词命中的（去重）
        Set<Integer> mergedIllnessIds = new LinkedHashSet<>(embeddingIllnessIds);
        for (Illness i : keywordIllnesses) {
            if (mergedIllnessIds.size() >= MAX_ILLNESS) break;
            mergedIllnessIds.add(i.getId());
        }
        Set<Integer> mergedMedicineIds = new LinkedHashSet<>(embeddingMedicineIds);
        for (Medicine m : keywordMedicines) {
            if (mergedMedicineIds.size() >= MAX_MEDICINE) break;
            mergedMedicineIds.add(m.getId());
        }

        // 构建最终实体列表
        List<Illness> matchedIllnesses = loadIllnessesByIds(mergedIllnessIds);
        List<Medicine> matchedMedicines = loadMedicinesByIds(mergedMedicineIds);

        // 补充分类名称和关联药品
        Map<Integer, String> kindNameMap = buildKindNameMap(matchedIllnesses);
        Map<Integer, List<Medicine>> illnessMedicineMap = buildIllnessMedicineMap(matchedIllnesses);

        if (matchedIllnesses.isEmpty() && matchedMedicines.isEmpty()) {
            log.info("╔══════════════════════════════════════════════════════════╗");
            log.info("║  RAG 结果: ❌ 双路均未命中                             ║");
            log.info("╚══════════════════════════════════════════════════════════╝");
            return "";
        }

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║  RAG 合并结果 ──────────────────────────────────────── ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║  语义:  illness={} medicine={}                          ",
                embeddingIllnessIds.size(), embeddingMedicineIds.size());
        log.info("║  关键词: illness={} medicine={}                         ",
                keywordIllnesses.size(), keywordMedicines.size());
        log.info("║  合并后: illness={} medicine={}                         ",
                matchedIllnesses.size(), matchedMedicines.size());
        // 逐条输出
        for (Illness i : matchedIllnesses) {
            boolean fromEmb = embeddingIllnessIds.contains(i.getId());
            boolean fromKw = keywordIllnesses.stream().anyMatch(k -> k.getId().equals(i.getId()));
            log.info("║   疾病: \"{}\" (id={}) 来源: {}{}",
                    truncateLog(i.getIllnessName(), 30), i.getId(),
                    fromEmb ? "语义 " : "", fromKw ? "关键词" : "");
        }
        for (Medicine m : matchedMedicines) {
            boolean fromEmb = embeddingMedicineIds.contains(m.getId());
            boolean fromKw = keywordMedicines.stream().anyMatch(k -> k.getId().equals(m.getId()));
            log.info("║   药品: \"{}\" (id={}) 来源: {}{}",
                    truncateLog(m.getMedicineName(), 30), m.getId(),
                    fromEmb ? "语义 " : "", fromKw ? "关键词" : "");
        }
        log.info("╚══════════════════════════════════════════════════════════╝");

        String context = formatKnowledgeContext(matchedIllnesses, matchedMedicines, kindNameMap, illnessMedicineMap);
        log.info("RAG 上下文总长度: {} 字符 (约 {} tokens)", context.length(), context.length() / 2);
        return context;
    }

    // ==================== 向量语义检索 ====================

    /**
     * 通过 Embedding 向量相似度搜索疾病和药品
     */
    private void searchByEmbedding(String query, Set<Integer> outIllnessIds, Set<Integer> outMedicineIds) {
        log.info("  ┌─ 路径1: 向量语义检索 ──────────────────────────");

        // 1. 将查询向量化
        Response<Embedding> embedResponse = embeddingModel.embed(query);
        if (embedResponse == null || embedResponse.content() == null) {
            log.warn("  │  Embedding 返回为空，跳过语义检索");
            log.info("  └──────────────────────────────────────────────────");
            return;
        }
        log.info("  │  查询已向量化 (dim={})", embedResponse.content().vectorAsList().size());

        // 2. 搜索疾病
        EmbeddingSearchRequest illnessRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedResponse.content())
                .maxResults(embeddingMaxResults)
                .minScore(embeddingMinScore)
                .filter(new IsEqualTo("type", "illness"))
                .build();
        EmbeddingSearchResult<TextSegment> illnessResults = embeddingStore.search(illnessRequest);
        log.info("  │  语义检索 [疾病] 总命中: {} 条", illnessResults.matches().size());
        for (EmbeddingMatch<TextSegment> match : illnessResults.matches()) {
            String idStr = match.embedded().metadata().get("id");
            String name = match.embedded().metadata().get("name");
            String text = match.embedded().text();
            String scoreBar = scoreBar(match.score());
            log.info("  │    [语义] score={:.4f} {} | id={} name={} | \"{}\"",
                    match.score(), scoreBar, idStr, name, truncateLog(text, 60));
            if (idStr != null) {
                try { outIllnessIds.add(Integer.parseInt(idStr)); } catch (NumberFormatException ignored) {}
            }
            // 同时记录低于阈值被筛掉的
        }
        if (illnessResults.matches().isEmpty()) {
            log.info("  │    (无命中)");
        }

        // 3. 搜索药品
        EmbeddingSearchRequest medicineRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedResponse.content())
                .maxResults(embeddingMaxResults)
                .minScore(embeddingMinScore)
                .filter(new IsEqualTo("type", "medicine"))
                .build();
        EmbeddingSearchResult<TextSegment> medicineResults = embeddingStore.search(medicineRequest);
        log.info("  │  语义检索 [药品] 总命中: {} 条", medicineResults.matches().size());
        for (EmbeddingMatch<TextSegment> match : medicineResults.matches()) {
            String idStr = match.embedded().metadata().get("id");
            String name = match.embedded().metadata().get("name");
            String text = match.embedded().text();
            String scoreBar = scoreBar(match.score());
            log.info("  │    [语义] score={:.4f} {} | id={} name={} | \"{}\"",
                    match.score(), scoreBar, idStr, name, truncateLog(text, 60));
            if (idStr != null) {
                try { outMedicineIds.add(Integer.parseInt(idStr)); } catch (NumberFormatException ignored) {}
            }
        }
        if (medicineResults.matches().isEmpty()) {
            log.info("  │    (无命中)");
        }
        log.info("  └─ 语义路径汇总: illness={} medicine={} ──────────",
                outIllnessIds.size(), outMedicineIds.size());
    }

    /** 生成分数可视化条 ████░░░░ */
    private static String scoreBar(double score) {
        int filled = (int) Math.round(score * 10);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "█" : "░");
        }
        sb.append("]");
        return sb.toString();
    }

    /** 截断过长日志文本 */
    private static String truncateLog(String text, int maxLen) {
        if (text == null) return "null";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }

    // ==================== 关键词提取 ====================

    /** 需要从查询中移除的停用词/短语 */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "请问", "我想", "想知道", "知道", "什么", "怎么", "如何", "为什么", "怎么办","什么是",
            "应该", "可以", "需要", "是否", "能不能", "是什么",
            "得了", "我有", "我得了", "感觉", "最近", "一直", "还是",
            "这个", "那个", "一下", "一些", "有什么", "什么是",
            "的我", "的吗", "啊", "呢", "吧", "吗", "哦", "嗯",
            "该", "要", "会", "能", "有", "是", "的", "了", "在", "我",
            "吃什么", "该吃", "怎样", "什么样", "什么样", "介绍", "告诉我",
            "帮我", "请", "问一下", "问", "一个", "哪种", "哪些", "哪",
            "现在", "有点", "好", "很", "太", "比较", "非常"
    ));

    /**
     * 从用户查询中提取关键词（适配中文无空格特性）
     */
    private List<String> extractKeywords(String query) {
        String cleaned = query
                .replaceAll("[，。！？、；：（）\\[\\]【】\"'\\s\\p{Punct}]+", "")
                .trim();

        if (cleaned.length() < 2) {
            return Collections.emptyList();
        }

        List<String> sortedStopWords = new ArrayList<>(STOP_WORDS);
        sortedStopWords.sort((a, b) -> Integer.compare(b.length(), a.length()));
        for (String stop : sortedStopWords) {
            cleaned = cleaned.replace(stop, "");
        }

        if (cleaned.length() < 2) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();
        keywords.add(cleaned);

        if (cleaned.length() >= 4) {
            for (int len = 2; len <= 3; len++) {
                for (int i = 0; i + len <= cleaned.length(); i++) {
                    String sub = cleaned.substring(i, i + len);
                    if (!STOP_WORDS.contains(sub) && !keywords.contains(sub)) {
                        keywords.add(sub);
                    }
                }
            }
        }

        log.info("  ┌─ 关键词提取 ────────────────────────────────────");
        log.info("  │  原始文本: \"{}\"", truncateLog(query, 100));
        log.info("  │  清理后:   \"{}\"", truncateLog(cleaned, 100));
        log.info("  │  提取关键词({}个): {}", keywords.size(), keywords);
        log.info("  └──────────────────────────────────────────────────");
        return keywords;
    }

    // ==================== 数据库检索 ====================

    private List<Illness> searchIllnesses(List<String> keywords) {
        log.info("  ┌─ 路径2: 关键词检索 [疾病] ───────────────────────");
        log.info("  │  搜索字段: illness_name, include_reason, illness_symptom, special_symptom");
        for (String kw : keywords) {
            log.info("  │  关键词: \"{}\"", kw);
        }

        QueryWrapper<Illness> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            for (int i = 0; i < keywords.size(); i++) {
                String like = "%" + keywords.get(i) + "%";
                if (i == 0) {
                    w.like("illness_name", like)
                     .or().like("include_reason", like)
                     .or().like("illness_symptom", like)
                     .or().like("special_symptom", like);
                } else {
                    w.or(sub -> sub
                            .like("illness_name", like)
                            .or().like("include_reason", like)
                            .or().like("illness_symptom", like)
                            .or().like("special_symptom", like));
                }
            }
        });
        wrapper.last("LIMIT " + MAX_ILLNESS);

        List<Illness> results = illnessDao.selectList(wrapper);
        for (Illness i : results) {
            // 找出是哪个关键词命中了哪个字段
            List<String> matchedFields = new ArrayList<>();
            for (String kw : keywords) {
                if (i.getIllnessName() != null && i.getIllnessName().contains(kw))
                    matchedFields.add("名称匹配:" + kw);
                if (i.getIncludeReason() != null && i.getIncludeReason().contains(kw))
                    matchedFields.add("诱因匹配:" + kw);
                if (i.getIllnessSymptom() != null && i.getIllnessSymptom().contains(kw))
                    matchedFields.add("症状匹配:" + kw);
                if (i.getSpecialSymptom() != null && i.getSpecialSymptom().contains(kw))
                    matchedFields.add("特殊症状匹配:" + kw);
            }
            log.info("  │    [关键词] id={} name=\"{}\" → {}",
                    i.getId(), i.getIllnessName(),
                    matchedFields.isEmpty() ? "模糊命中" : String.join(", ", matchedFields));
        }
        if (results.isEmpty()) {
            log.info("  │    (无命中)");
        }
        log.info("  └─ 关键词疾病: {} 条 ─────────────────────────────", results.size());
        return results;
    }

    private List<Medicine> searchMedicines(List<String> keywords) {
        log.info("  ┌─ 路径2: 关键词检索 [药品] ───────────────────────");
        log.info("  │  搜索字段: medicine_name, keyword, medicine_effect");
        for (String kw : keywords) {
            log.info("  │  关键词: \"{}\"", kw);
        }

        QueryWrapper<Medicine> wrapper = new QueryWrapper<>();
        wrapper.and(w -> {
            for (int i = 0; i < keywords.size(); i++) {
                String like = "%" + keywords.get(i) + "%";
                if (i == 0) {
                    w.like("medicine_name", like)
                     .or().like("keyword", like)
                     .or().like("medicine_effect", like);
                } else {
                    w.or(sub -> sub
                            .like("medicine_name", like)
                            .or().like("keyword", like)
                            .or().like("medicine_effect", like));
                }
            }
        });
        wrapper.last("LIMIT " + MAX_MEDICINE);

        List<Medicine> results = medicineDao.selectList(wrapper);
        for (Medicine m : results) {
            List<String> matchedFields = new ArrayList<>();
            for (String kw : keywords) {
                if (m.getMedicineName() != null && m.getMedicineName().contains(kw))
                    matchedFields.add("名称匹配:" + kw);
                if (m.getKeyword() != null && m.getKeyword().contains(kw))
                    matchedFields.add("关键词匹配:" + kw);
                if (m.getMedicineEffect() != null && m.getMedicineEffect().contains(kw))
                    matchedFields.add("功效匹配:" + kw);
            }
            log.info("  │    [关键词] id={} name=\"{}\" → {}",
                    m.getId(), m.getMedicineName(),
                    matchedFields.isEmpty() ? "模糊命中" : String.join(", ", matchedFields));
        }
        if (results.isEmpty()) {
            log.info("  │    (无命中)");
        }
        log.info("  └─ 关键词药品: {} 条 ─────────────────────────────", results.size());
        return results;
    }

    // ==================== 实体装载 ====================

    private List<Illness> loadIllnessesByIds(Set<Integer> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        return illnessDao.selectList(new QueryWrapper<Illness>().in("id", ids));
    }

    private List<Medicine> loadMedicinesByIds(Set<Integer> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        return medicineDao.selectList(new QueryWrapper<Medicine>().in("id", ids));
    }

    // ==================== 关联数据补充 ====================

    private Map<Integer, String> buildKindNameMap(List<Illness> illnesses) {
        Map<Integer, String> map = new LinkedHashMap<>();
        Set<Integer> kindIdSet = illnesses.stream()
                .map(Illness::getKindId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!kindIdSet.isEmpty()) {
            List<IllnessKind> kinds = illnessKindDao.selectList(
                    new QueryWrapper<IllnessKind>().in("id", kindIdSet));
            for (IllnessKind kind : kinds) {
                map.put(kind.getId(), kind.getName());
            }
        }
        return map;
    }

    private Map<Integer, List<Medicine>> buildIllnessMedicineMap(List<Illness> illnesses) {
        Map<Integer, List<Medicine>> map = new LinkedHashMap<>();
        if (illnesses.isEmpty()) return map;

        List<Integer> illnessIds = illnesses.stream()
                .map(Illness::getId)
                .collect(Collectors.toList());

        List<IllnessMedicine> mappings = illnessMedicineDao.selectList(
                new QueryWrapper<IllnessMedicine>().in("illness_id", illnessIds));
        if (mappings.isEmpty()) return map;

        Set<Integer> medicineIds = mappings.stream()
                .map(IllnessMedicine::getMedicineId)
                .collect(Collectors.toSet());
        List<Medicine> medicines = medicineDao.selectList(
                new QueryWrapper<Medicine>().in("id", medicineIds));
        Map<Integer, Medicine> medicineMap = medicines.stream()
                .collect(Collectors.toMap(Medicine::getId, m -> m));

        for (IllnessMedicine mapping : mappings) {
            Medicine med = medicineMap.get(mapping.getMedicineId());
            if (med != null) {
                map.computeIfAbsent(mapping.getIllnessId(), k -> new ArrayList<>()).add(med);
            }
        }
        return map;
    }

    // ==================== 格式化输出 ====================

    private String formatKnowledgeContext(
            List<Illness> illnesses,
            List<Medicine> directMedicines,
            Map<Integer, String> kindNameMap,
            Map<Integer, List<Medicine>> illnessMedicineMap) {

        StringBuilder sb = new StringBuilder();
        sb.append("【参考知识库——来自系统数据库的疾病与药品信息】\n");

        if (!illnesses.isEmpty()) {
            sb.append("\n━━━ 疾病信息 ━━━\n");
            for (int i = 0; i < illnesses.size(); i++) {
                Illness illness = illnesses.get(i);
                sb.append("\n").append(i + 1).append(". 疾病名称：").append(illness.getIllnessName()).append("\n");

                String kindName = kindNameMap.get(illness.getKindId());
                if (StrUtil.isNotBlank(kindName)) {
                    sb.append("   所属科室：").append(kindName).append("\n");
                }
                if (StrUtil.isNotBlank(illness.getIncludeReason())) {
                    sb.append("   诱发原因：").append(illness.getIncludeReason()).append("\n");
                }
                if (StrUtil.isNotBlank(illness.getIllnessSymptom())) {
                    sb.append("   主要症状：").append(illness.getIllnessSymptom()).append("\n");
                }
                if (StrUtil.isNotBlank(illness.getSpecialSymptom())) {
                    sb.append("   特殊症状：").append(illness.getSpecialSymptom()).append("\n");
                }

                List<Medicine> relatedMeds = illnessMedicineMap.get(illness.getId());
                if (CollUtil.isNotEmpty(relatedMeds)) {
                    sb.append("   相关药品：");
                    sb.append(relatedMeds.stream()
                            .map(Medicine::getMedicineName)
                            .collect(Collectors.joining("、")));
                    sb.append("\n");
                    for (Medicine med : relatedMeds) {
                        sb.append("     ├ ").append(med.getMedicineName());
                        if (StrUtil.isNotBlank(med.getMedicineEffect())) {
                            sb.append("（").append(truncate(med.getMedicineEffect(), 80)).append("）");
                        }
                        if (StrUtil.isNotBlank(med.getUsAge())) {
                            sb.append(" 用法：").append(truncate(med.getUsAge(), 60));
                        }
                        sb.append("\n");
                        sb.append("     购买链接：http://localhost:8080/findMedicineOne?id=").append(med.getId()).append("\n");
                    }
                }
            }
        }

        if (!directMedicines.isEmpty()) {
            sb.append("\n━━━ 药品信息 ━━━\n");
            for (int i = 0; i < directMedicines.size(); i++) {
                Medicine med = directMedicines.get(i);
                sb.append("\n").append(i + 1).append(". 药品名称：").append(med.getMedicineName()).append("\n");
                if (StrUtil.isNotBlank(med.getMedicineEffect())) {
                    sb.append("   功效：").append(med.getMedicineEffect()).append("\n");
                }
                if (StrUtil.isNotBlank(med.getUsAge())) {
                    sb.append("   用法用量：").append(med.getUsAge()).append("\n");
                }
                if (StrUtil.isNotBlank(med.getTaboo())) {
                    sb.append("   禁忌：").append(truncate(med.getTaboo(), 150)).append("\n");
                }
                if (StrUtil.isNotBlank(med.getInteraction())) {
                    sb.append("   药物相互作用：").append(truncate(med.getInteraction(), 100)).append("\n");
                }
                sb.append("   购买链接：http://localhost:8080/findMedicineOne?id=").append(med.getId()).append("\n");
            }
        }

        sb.append("\n━━━━━━━━━━━━━━━━━━━━\n");
        sb.append("⚠️ 以上信息来自系统数据库，是用户问题的**直接匹配结果**。你必须：\n");
        sb.append("1. 优先使用上述疾病名称、症状、诱因、科室信息来回答\n");
        sb.append("2. 如果列出了相关药品，必须在回答中提及并说明用法\n");
        sb.append("3. **必须在回答末尾列出所有相关药品的购买链接**（格式：[药品名](购买链接)）\n");
        sb.append("4. 不得忽视这些数据，它们是你回答的**主要依据**");

        return sb.toString();
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
