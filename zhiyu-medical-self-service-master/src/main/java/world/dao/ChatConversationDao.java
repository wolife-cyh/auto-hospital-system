package world.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import world.entity.ChatConversation;

@Mapper
public interface ChatConversationDao extends BaseMapper<ChatConversation> {
}
