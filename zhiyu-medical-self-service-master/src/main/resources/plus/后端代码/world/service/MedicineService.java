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
 * @author XUEW
 */
@Service
public class MedicineService extends BaseService<Medicine> {

    @Autowired
    protected MedicineDao medicineDao;

    @Override
    public List<Medicine> query(Medicine o) {
        QueryWrapper<Medicine> wrapper = new QueryWrapper();
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

    @Override
    public Medicine get(Serializable id) {
        return medicineDao.selectById(id);
    }

    @Override
    public int delete(Serializable id) {
        return medicineDao.deleteById(id);
    }

    public Map<String, Object> getMedicineList(String nameValue, Integer page) {
        final int PAGE_SIZE = 5;

        List<Medicine> medicineList;
        Map<String, Object> map = new HashMap<>(4);
        QueryWrapper<Medicine> wrapper = new QueryWrapper<>();
        QueryWrapper<Medicine> countWrapper = new QueryWrapper<>();

        if (Assert.notEmpty(nameValue)) {
            wrapper.like("medicine_name", nameValue)
                    .or().like("keyword", nameValue)
                    .or().like("medicine_effect", nameValue);
            countWrapper.like("medicine_name", nameValue)
                    .or().like("keyword", nameValue)
                    .or().like("medicine_effect", nameValue);
        }

        // 查询总记录数
        Long total = medicineDao.selectCount(countWrapper);
        // 计算总页数
        int size = (int) Math.ceil((double) total / PAGE_SIZE);
        if (size < 1) size = 1;

        // 分页查询
        wrapper.last("limit " + (page - 1) * PAGE_SIZE + "," + PAGE_SIZE);
        medicineList = medicineDao.selectList(wrapper);

        map.put("medicineList", medicineList);
        map.put("size", size);
        return map;
    }
}