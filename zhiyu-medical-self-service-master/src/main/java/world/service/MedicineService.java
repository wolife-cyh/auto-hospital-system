package world.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import world.dao.MedicineDao;
import world.entity.Medicine;
import world.utils.Assert;
import world.utils.BeanUtil;
import world.utils.VariableNameUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 药品服务类
 *
 */
@Service
public class MedicineService extends BaseService<Medicine> {

    @Autowired
    protected MedicineDao medicineDao;

    @Override
    public List<Medicine> query(Medicine o) {
        QueryWrapper<Medicine> wrapper = new QueryWrapper<>();
        if (Assert.notEmpty(o)) {
            Map<String, Object> bean2Map = BeanUtil.bean2Map(o);
            for (String key : bean2Map.keySet()) {
                if (Assert.isEmpty(bean2Map.get(key))) {
                    continue;
                }
                wrapper.eq(VariableNameUtils.humpToLine(key), bean2Map.get(key));
            }
        }
        return medicineDao.selectList(wrapper);
    }

    @Override
    public List<Medicine> all() {
        return query(null);
    }

    @Override
    public Medicine save(Medicine o) {
        if (Assert.isEmpty(o.getId())) {
            medicineDao.insert(o);
        } else {
            medicineDao.updateById(o);
        }
        return medicineDao.selectById(o.getId());
    }

    /**
     * 根据药品名称查询药品
     */
    public Medicine getByMedicineName(String medicineName) {
        QueryWrapper<Medicine> wrapper = new QueryWrapper<>();
        wrapper.like("medicine_name", medicineName);
        return medicineDao.selectOne(wrapper);
    }

    @Override
    public Medicine get(Serializable id) {
        return medicineDao.selectById(id);
    }

    @Override
    public int delete(Serializable id) {
        return medicineDao.deleteById(id);
    }

    public Map<String, Object> getMedicineList(String nameValue, Integer page) {

        Map<String, Object> map = new HashMap<>(4);
        int pageSize = 5;

        // 先查总数
        QueryWrapper<Medicine> countWrapper = new QueryWrapper<>();
        if (Assert.notEmpty(nameValue)) {
            countWrapper.like("medicine_name", nameValue)
                    .or().like("keyword", nameValue)
                    .or().like("medicine_effect", nameValue);
        }
        long totalCount = medicineDao.selectCount(countWrapper);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        if (totalPages < 1) totalPages = 1;

        // 分页查询
        List<Medicine> medicineList;
        if (Assert.notEmpty(nameValue)) {
            medicineList = medicineDao.selectList(new QueryWrapper<Medicine>().
                    like("medicine_name", nameValue)
                    .or().like("keyword", nameValue)
                    .or().like("medicine_effect", nameValue)
                    .last("limit " + (page - 1) * pageSize + "," + pageSize));
        } else {
            medicineList = medicineDao.selectList(new QueryWrapper<Medicine>()
                    .last("limit " + (page - 1) * pageSize + "," + pageSize));
        }

        map.put("medicineList", medicineList);
        map.put("size", totalPages);
        return map;
    }
}