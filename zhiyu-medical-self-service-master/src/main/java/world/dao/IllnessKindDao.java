package world.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import world.entity.IllnessKind;

/**
 * 疾病分类数据库访问
 *
 */
@Repository
public interface IllnessKindDao extends BaseMapper<IllnessKind> {

}
