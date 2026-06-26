package world.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import world.dao.PurchaseOrderDao;
import world.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderDao purchaseOrderDao;

    public PurchaseOrder createOrder(Integer userId, Integer medicineId, String medicineName,
                                     String medicineImg, Integer quantity, BigDecimal price) {
        PurchaseOrder order = PurchaseOrder.builder()
                .orderNo(generateOrderNo())
                .userId(userId)
                .medicineId(medicineId)
                .medicineName(medicineName)
                .medicineImg(medicineImg)
                .quantity(quantity)
                .price(price)
                .totalPrice(price.multiply(BigDecimal.valueOf(quantity)))
                .status(0)
                .build();
        purchaseOrderDao.insert(order);
        return order;
    }

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<PurchaseOrder> getUserOrdersPage(Integer userId, Integer pageNum, Integer pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PurchaseOrder> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize);
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        return purchaseOrderDao.selectPage(page, wrapper);
    }

    public List<PurchaseOrder> getUserOrders(Integer userId) {
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("create_time");
        return purchaseOrderDao.selectList(wrapper);
    }

    public PurchaseOrder getById(Integer id) {
        return purchaseOrderDao.selectById(id);
    }

    public PurchaseOrder getByOrderNo(String orderNo) {
        QueryWrapper<PurchaseOrder> wrapper = new QueryWrapper<>();
        wrapper.eq("order_no", orderNo);
        return purchaseOrderDao.selectOne(wrapper);
    }

    public void pay(Integer orderId, String payType) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order != null && order.getStatus() == 0) {
            order.setStatus(1);
            order.setPayType(payType);
            order.setPayTime(new Date());
            purchaseOrderDao.updateById(order);
        }
    }

    public void cancel(Integer orderId) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order != null && order.getStatus() == 0) {
            order.setStatus(2);
            purchaseOrderDao.updateById(order);
        }
    }

    public void markPayType(Integer orderId, String payType) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order != null && order.getStatus() == 0) {
            order.setPayType(payType);
            purchaseOrderDao.updateById(order);
        }
    }

    /**
     * 判断订单是否已过期（创建超过30分钟未支付）
     */
    public boolean isExpired(PurchaseOrder order) {
        if (order == null || order.getStatus() != 0 || order.getCreateTime() == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long create = order.getCreateTime().getTime();
        return (now - create) > 30 * 60 * 1000; // 30分钟
    }

    /**
     * 如果订单已过期则自动取消，返回是否已取消
     */
    public boolean autoCancelIfExpired(Integer orderId) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order != null && isExpired(order)) {
            order.setStatus(2);
            purchaseOrderDao.updateById(order);
            return true;
        }
        return false;
    }

    private String generateOrderNo() {
        String time = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String random = String.valueOf((int) ((Math.random() * 9000) + 1000));
        return "MED" + time + random;
    }
}