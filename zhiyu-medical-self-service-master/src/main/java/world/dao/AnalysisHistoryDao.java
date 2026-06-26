package world.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import world.entity.AnalysisHistory;

@Mapper
public interface AnalysisHistoryDao extends BaseMapper<AnalysisHistory> {
}
