package world.service;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import world.dao.IllnessDao;
import world.dao.IllnessKindDao;
import world.dao.MedicineDao;
import world.entity.Illness;
import world.entity.IllnessKind;
import world.entity.Medicine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库数据向量化摄入服务
 * <p>
 * 系统启动时将数据库中所有疾病和药品数据转换为文本向量，
 * 存入 EmbeddingStore 供语义检索使用。
 * <p>
 * 同时支持增量摄入：新增或更新疾病/药品后调用对应方法即可同步到向量库。
 */
@Slf4j
@Service
public class KnowledgeIngestionService {

    @Resource
    private IllnessDao illnessDao;

    @Resource
    private IllnessKindDao illnessKindDao;

    @Resource
    private MedicineDao medicineDao;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    /** 摄入到向量库的疾病总数 */
    private int ingestedIllnessCount = 0;

    /** 摄入到向量库的药品总数 */
    private int ingestedMedicineCount = 0;

    /**
     * 系统启动时全量摄入
     */
    @PostConstruct
    public void ingestAllOnStartup() {
        long start = System.currentTimeMillis();
        log.info("开始全量摄入知识库数据到向量库...");

        try {
            // 预加载科室名称映射
            Map<Integer, String> kindNameMap = loadKindNameMap();

            // 摄入疾病
            ingestedIllnessCount = ingestIllnesses(kindNameMap);
            log.info("疾病摄入完成: {} 条", ingestedIllnessCount);

            // 摄入药品
            ingestedMedicineCount = ingestMedicines();
            log.info("药品摄入完成: {} 条", ingestedMedicineCount);

            long elapsed = System.currentTimeMillis() - start;
            log.info("知识库向量化摄入完成，总计 {} 条，耗时 {}ms",
                    ingestedIllnessCount + ingestedMedicineCount, elapsed);
        } catch (Exception e) {
            log.error("知识库向量化摄入失败，语义搜索将回退到关键词匹配", e);
        }
    }

    // ==================== 全量摄入 ====================

    /**
     * 摄入所有疾病到向量库
     */
    private int ingestIllnesses(Map<Integer, String> kindNameMap) {
        List<Illness> illnesses = illnessDao.selectList(null);
        if (illnesses == null || illnesses.isEmpty()) {
            log.warn("疾病表为空，跳过摄入");
            return 0;
        }

        for (Illness illness : illnesses) {
            try {
                TextSegment segment = buildIllnessSegment(illness, kindNameMap);
                Response<Embedding> response = embeddingModel.embed(segment.text());
                if (response != null && response.content() != null) {
                    embeddingStore.add(response.content(), segment);
                }
            } catch (Exception e) {
                log.warn("摄入疾病失败: id={}, name={}, error={}",
                        illness.getId(), illness.getIllnessName(), e.getMessage());
            }
        }
        return illnesses.size();
    }

    /**
     * 摄入所有药品到向量库
     */
    private int ingestMedicines() {
        List<Medicine> medicines = medicineDao.selectList(null);
        if (medicines == null || medicines.isEmpty()) {
            log.warn("药品表为空，跳过摄入");
            return 0;
        }

        for (Medicine medicine : medicines) {
            try {
                TextSegment segment = buildMedicineSegment(medicine);
                Response<Embedding> response = embeddingModel.embed(segment.text());
                if (response != null && response.content() != null) {
                    embeddingStore.add(response.content(), segment);
                }
            } catch (Exception e) {
                log.warn("摄入药品失败: id={}, name={}, error={}",
                        medicine.getId(), medicine.getMedicineName(), e.getMessage());
            }
        }
        return medicines.size();
    }

    // ==================== 增量摄入（公开 API） ====================

    /**
     * 增量摄入一条疾病记录
     */
    public void ingestIllness(Illness illness) {
        if (illness == null) return;
        Map<Integer, String> kindNameMap = loadKindNameMap();
        try {
            TextSegment segment = buildIllnessSegment(illness, kindNameMap);
            Response<Embedding> response = embeddingModel.embed(segment.text());
            if (response != null && response.content() != null) {
                embeddingStore.add(response.content(), segment);
                ingestedIllnessCount++;
                log.info("增量摄入疾病: {}", illness.getIllnessName());
            }
        } catch (Exception e) {
            log.warn("增量摄入疾病失败: {}", e.getMessage());
        }
    }

    /**
     * 增量摄入一条药品记录
     */
    public void ingestMedicine(Medicine medicine) {
        if (medicine == null) return;
        try {
            TextSegment segment = buildMedicineSegment(medicine);
            Response<Embedding> response = embeddingModel.embed(segment.text());
            if (response != null && response.content() != null) {
                embeddingStore.add(response.content(), segment);
                ingestedMedicineCount++;
                log.info("增量摄入药品: {}", medicine.getMedicineName());
            }
        } catch (Exception e) {
            log.warn("增量摄入药品失败: {}", e.getMessage());
        }
    }

    // ==================== TextSegment 构建 ====================

    /**
     * 构建疾病的文本表示（用于向量化）
     */
    private TextSegment buildIllnessSegment(Illness illness, Map<Integer, String> kindNameMap) {
        StringBuilder sb = new StringBuilder();
        sb.append("疾病名称：").append(illness.getIllnessName());

        String kindName = kindNameMap.get(illness.getKindId());
        if (StrUtil.isNotBlank(kindName)) {
            sb.append(" | 所属科室：").append(kindName);
        }
        if (StrUtil.isNotBlank(illness.getIncludeReason())) {
            sb.append(" | 诱因：").append(illness.getIncludeReason());
        }
        if (StrUtil.isNotBlank(illness.getIllnessSymptom())) {
            sb.append(" | 主要症状：").append(illness.getIllnessSymptom());
        }
        if (StrUtil.isNotBlank(illness.getSpecialSymptom())) {
            sb.append(" | 特殊症状：").append(illness.getSpecialSymptom());
        }

        Metadata metadata = new Metadata();
        metadata.put("type", "illness");
        metadata.put("id", String.valueOf(illness.getId()));
        metadata.put("name", illness.getIllnessName());

        return TextSegment.from(sb.toString(), metadata);
    }

    /**
     * 构建药品的文本表示（用于向量化）
     */
    private TextSegment buildMedicineSegment(Medicine medicine) {
        StringBuilder sb = new StringBuilder();
        sb.append("药品名称：").append(medicine.getMedicineName());

        if (StrUtil.isNotBlank(medicine.getMedicineEffect())) {
            sb.append(" | 功效：").append(medicine.getMedicineEffect());
        }
        if (StrUtil.isNotBlank(medicine.getUsAge())) {
            sb.append(" | 用法用量：").append(medicine.getUsAge());
        }
        if (StrUtil.isNotBlank(medicine.getTaboo())) {
            sb.append(" | 禁忌：").append(medicine.getTaboo());
        }
        if (StrUtil.isNotBlank(medicine.getKeyword())) {
            sb.append(" | 关键词：").append(medicine.getKeyword());
        }

        Metadata metadata = new Metadata();
        metadata.put("type", "medicine");
        metadata.put("id", String.valueOf(medicine.getId()));
        metadata.put("name", medicine.getMedicineName());

        return TextSegment.from(sb.toString(), metadata);
    }

    // ==================== 辅助方法 ====================

    /**
     * 加载科室 ID → 名称映射
     */
    private Map<Integer, String> loadKindNameMap() {
        Map<Integer, String> map = new HashMap<>();
        List<IllnessKind> kinds = illnessKindDao.selectList(null);
        if (kinds != null) {
            for (IllnessKind kind : kinds) {
                map.put(kind.getId(), kind.getName());
            }
        }
        return map;
    }

    /**
     * 获取当前向量库中的记录总数
     */
    public int getTotalIngested() {
        return ingestedIllnessCount + ingestedMedicineCount;
    }
}
