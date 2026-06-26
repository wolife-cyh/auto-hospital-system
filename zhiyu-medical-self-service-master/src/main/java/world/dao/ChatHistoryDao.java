package world.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import world.entity.ChatHistory;

@Mapper
public interface ChatHistoryDao extends BaseMapper<ChatHistory> {
}
