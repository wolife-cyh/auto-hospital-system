package world.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import world.entity.PurchaseOrder;

@Repository
public interface PurchaseOrderDao extends BaseMapper<PurchaseOrder> {
}