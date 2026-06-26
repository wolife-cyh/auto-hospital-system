package world.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import world.entity.PurchaseOrder;

/**
 * 订单数据库访问
 *
 * @author XUEW
 */
@Repository
public interface PurchaseOrderDao extends BaseMapper<PurchaseOrder> {

}
