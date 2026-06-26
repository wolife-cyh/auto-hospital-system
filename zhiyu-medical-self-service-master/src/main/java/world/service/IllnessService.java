package world.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import world.dao.IllnessDao;
import world.entity.*;
import world.utils.Assert;
import world.utils.BeanUtil;
import world.utils.VariableNameUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 疾病服务类
 *
 */
@Service
public class IllnessService extends BaseService<Illness> {

    @Autowired
    protected IllnessDao illnessDao;

    @Override
    public List<Illness> query(Illness o) {
        QueryWrapper<Illness> wrapper = new QueryWrapper<>();
        if (Assert.notEmpty(o)) {
            Map<String, Object> bean2Map = BeanUtil.bean2Map(o);
            for (String key : bean2Map.keySet()) {
                if (Assert.isEmpty(bean2Map.get(key))) {
                    continue;
                }
                wrapper.eq(VariableNameUtils.humpToLine(key), bean2Map.get(key));
            }
        }
        return illnessDao.selectList(wrapper);
    }

    @Override
    public List<Illness> all() {
        return query(null);
    }

    @Override
    public Illness save(Illness o) {
        if (Assert.isEmpty(o.getId())) {
            illnessDao.insert(o);
        } else {
            illnessDao.updateById(o);
        }
        return illnessDao.selectById(o.getId());
    }

    @Override
    public Illness get(Serializable id) {
        return illnessDao.selectById(id);
    }

    @Override
    public int delete(Serializable id) {
        return illnessDao.deleteById(id);
    }

    public Map<String, Object> findIllness(Integer kind, String illnessName, Integer page) {

        Map<String, Object> map = new HashMap<>(4);

        // 1. 构建查询条件（不含 limit，用于计数）
        QueryWrapper<Illness> countWrapper = new QueryWrapper<>();
        if (Assert.notEmpty(illnessName)) {
            countWrapper
                    .like("illness_name", illnessName)
                    .or()
                    .like("include_reason", illnessName)
                    .or()
                    .like("illness_symptom", illnessName)
                    .or()
                    .like("special_symptom", illnessName);
        }
        if (kind != null) {
            countWrapper.eq("kind_id", kind);
        }
        long totalCount = illnessDao.selectCount(countWrapper);
        int pageSize = 9;
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        if (totalPages < 1) totalPages = 1;

        // 2. 构建分页查询
        QueryWrapper<Illness> queryWrapper = new QueryWrapper<>();
        if (Assert.notEmpty(illnessName)) {
            queryWrapper
                    .like("illness_name", illnessName)
                    .or()
                    .like("include_reason", illnessName)
                    .or()
                    .like("illness_symptom", illnessName)
                    .or()
                    .like("special_symptom", illnessName);
        }
        if (kind != null) {
            if (Assert.notEmpty(illnessName)) {
                queryWrapper.last("and (kind_id = " + kind + ") ORDER BY create_time DESC limit " + (page - 1) * pageSize + "," + pageSize);
            } else {
                queryWrapper.eq("kind_id", kind);
                queryWrapper.orderByDesc("create_time");
                queryWrapper.last("limit " + (page - 1) * pageSize + "," + pageSize);
            }
        } else {
            queryWrapper.orderByDesc("create_time");
            queryWrapper.last("limit " + (page - 1) * pageSize + "," + pageSize);

        }
        List<Map<String, Object>> list = illnessDao.selectMaps(queryWrapper);
        list.forEach(l -> {
            Integer id = MapUtil.getInt(l, "id");
            Pageview pageInfo = pageviewDao.selectOne(new QueryWrapper<Pageview>().eq("illness_id", id));
            l.put("kindName", "暂无归属类");
            l.put("create_time", MapUtil.getDate(l, "create_time"));
            l.put("pageview", pageInfo == null ? 0 : pageInfo.getPageviews());
            Integer kindId = MapUtil.getInt(l, "kind_id");
            if (Assert.notEmpty(kindId)) {
                IllnessKind illnessKind = illnessKindDao.selectById(kindId);
                if (Assert.notEmpty(illnessKind)) {
                    l.put("kindName", illnessKind.getName());
                }
            }
        });
        map.put("illness", list);
        map.put("size", totalPages);
        return map;
    }

    public Map<String, Object> findIllnessOne(Integer id) {
        Illness illness = illnessDao.selectOne(new QueryWrapper<Illness>().eq("id", id));
        List<IllnessMedicine> illnessMedicines = illnessMedicineDao.selectList(new QueryWrapper<IllnessMedicine>().eq("illness_id", id));
        List<Medicine> list = new ArrayList<>(4);
        Map<String, Object> map = new HashMap<>(4);
        Pageview illness_id = pageviewDao.selectOne(new QueryWrapper<Pageview>().eq("illness_id", id));
        if (Assert.isEmpty(illness_id)) {
            illness_id = new Pageview();
            illness_id.setIllnessId(id);
            illness_id.setPageviews(1);
            pageviewDao.insert(illness_id);
        } else {
            illness_id.setPageviews(illness_id.getPageviews() + 1);
            pageviewDao.updateById(illness_id);
        }
        map.put("illness", illness);

        if (CollUtil.isNotEmpty(illnessMedicines)) {
            illnessMedicines.forEach(illnessMedicine -> {
                Medicine medicine = medicineDao.selectOne(new QueryWrapper<Medicine>().eq("id", illnessMedicine.getMedicineId()));
                if (ObjectUtil.isNotNull(medicine)) {
                    list.add(medicine);
                }
            });
            map.put("medicine", list);

        }

        return map;
    }

    public Illness getOne(QueryWrapper<Illness> queryWrapper) {
        return illnessDao.selectOne(queryWrapper);
    }

    /**
     * 按名称模糊搜索疾病（用于药品关联时的疾病搜索）
     *
     * @param keyword 搜索关键词
     * @return 匹配的疾病列表（最多 20 条）
     */
    public List<Illness> searchByName(String keyword) {
        QueryWrapper<Illness> wrapper = new QueryWrapper<>();
        wrapper.like("illness_name", keyword)
                .or().like("illness_symptom", keyword)
                .orderByDesc("create_time")
                .last("LIMIT 20");
        return illnessDao.selectList(wrapper);
    }
}