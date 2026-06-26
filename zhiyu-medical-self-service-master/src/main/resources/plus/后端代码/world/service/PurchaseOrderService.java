package world.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import world.dao.PurchaseOrderDao;
import world.entity.PurchaseOrder;
import world.utils.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务类
 *
 * @author XUEW
 */
@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderDao purchaseOrderDao;

    /**
     * 创建订单
     */
    public PurchaseOrder createOrder(Integer userId, Integer medicineId, String medicineName,
                                     String medicineImg, Integer quantity, java.math.BigDecimal price) {
        PurchaseOrder order = PurchaseOrder.builder()
                .orderNo(generateOrderNo())
                .userId(userId)
                .medicineId(medicineId)
                .medicineName(medicineName)
                .medicineImg(medicineImg)
                .quantity(quantity)
                .price(price)
                .totalPrice(price.multiply(java.math.BigDecimal.valueOf(quantity)))
                .status(0) // 待支付
                .build();
        purchaseOrderDao.insert(order);
        return order;
    }

    /**
     * 模拟支付
     */
    public PurchaseResult pay(Integer orderId, String payType) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order == null) {
            return PurchaseResult.fail("订单不存在");
        }
        if (order.getStatus() != 0) {
            return PurchaseResult.fail("订单状态异常");
        }
        // 模拟支付处理（在实际项目中这里会调用支付宝/微信的支付SDK）
        order.setStatus(1);
        order.setPayType(payType);
        order.setPayTime(new java.util.Date());
        purchaseOrderDao.updateById(order);
        return PurchaseResult.success("支付成功", order);
    }

    /**
     * 取消订单
     */
    public PurchaseResult cancel(Integer orderId) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order == null) {
            return PurchaseResult.fail("订单不存在");
        }
        if (order.getStatus() != 0) {
            return PurchaseResult.fail("只能取消待支付的订单");
        }
        order.setStatus(2);
        purchaseOrderDao.updateById(order);
        return PurchaseResult.success("订单已取消", order);
    }

    /**
     * 查询用户的订单列表
     */
    public List<PurchaseOrder> getUserOrders(Integer userId) {
        return purchaseOrderDao.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PurchaseOrder>()
                        .eq("user_id", userId)
                        .orderByDesc("create_time"));
    }

    /**
     * 根据ID查询订单
     */
    public PurchaseOrder getById(Integer id) {
        return purchaseOrderDao.selectById(id);
    }

    /**
     * 根据订单号查询
     */
    public PurchaseOrder getByOrderNo(String orderNo) {
        return purchaseOrderDao.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PurchaseOrder>()
                        .eq("order_no", orderNo));
    }

    /**
     * 标记支付方式（在跳转支付宝前记录）
     */
    public void markPayType(Integer orderId, String payType) {
        PurchaseOrder order = purchaseOrderDao.selectById(orderId);
        if (order != null && order.getStatus() == 0) {
            order.setPayType(payType);
            purchaseOrderDao.updateById(order);
        }
    }

    /**
     * 生成订单号：年月日时分秒 + 4位随机数
     */
    private String generateOrderNo() {
        String time = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());
        String random = String.valueOf((int) ((Math.random() * 9000) + 1000));
        return "MED" + time + random;
    }

    /**
     * 支付结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PurchaseResult {
        private boolean success;
        private String message;
        private PurchaseOrder order;

        public static PurchaseResult success(String message, PurchaseOrder order) {
            return new PurchaseResult(true, message, order);
        }

        public static PurchaseResult fail(String message) {
            return new PurchaseResult(false, message, null);
        }
    }
}
