package world.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import world.dto.RespResult;
import world.entity.Medicine;
import world.entity.PurchaseOrder;
import world.entity.User;
import world.service.MedicineService;
import world.service.PurchaseOrderService;
import world.utils.Assert;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * 订单控制器
 *
 * @author XUEW
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private MedicineService medicineService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public RespResult createOrder(Integer medicineId, Integer quantity, HttpSession session) {
        try {
            User loginUser = (User) session.getAttribute("loginUser");
            if (Assert.isEmpty(loginUser)) {
                return RespResult.fail("请先登录");
            }
            if (medicineId == null || quantity == null || quantity < 1) {
                return RespResult.fail("参数错误");
            }
            Medicine medicine = medicineService.get(medicineId);
            if (medicine == null) {
                return RespResult.fail("药品不存在");
            }
            PurchaseOrder order = purchaseOrderService.createOrder(
                    loginUser.getId(),
                    medicine.getId(),
                    medicine.getMedicineName(),
                    medicine.getImgPath(),
                    quantity,
                    medicine.getMedicinePrice()
            );
            return RespResult.success("订单创建成功", order);
        } catch (Exception e) {
            log.error("创建订单异常", e);
            return RespResult.fail("创建订单失败：" + e.getMessage());
        }
    }

    /**
     * 模拟支付
     */
    @PostMapping("/pay")
    public RespResult pay(Integer orderId, String payType, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (Assert.isEmpty(loginUser)) {
            return RespResult.fail("请先登录");
        }
        PurchaseOrder order = purchaseOrderService.getById(orderId);
        if (order == null || !order.getUserId().equals(loginUser.getId())) {
            return RespResult.fail("订单不存在");
        }
        PurchaseOrderService.PurchaseResult result = purchaseOrderService.pay(orderId, payType);
        if (result.isSuccess()) {
            return RespResult.success(result.getMessage());
        }
        return RespResult.fail(result.getMessage());
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public RespResult cancel(Integer orderId, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (Assert.isEmpty(loginUser)) {
            return RespResult.fail("请先登录");
        }
        PurchaseOrder order = purchaseOrderService.getById(orderId);
        if (order == null || !order.getUserId().equals(loginUser.getId())) {
            return RespResult.fail("订单不存在");
        }
        PurchaseOrderService.PurchaseResult result = purchaseOrderService.cancel(orderId);
        if (result.isSuccess()) {
            return RespResult.success(result.getMessage());
        }
        return RespResult.fail(result.getMessage());
    }

    /**
     * 用户订单列表
     */
    @PostMapping("/list")
    public RespResult list(HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (Assert.isEmpty(loginUser)) {
            return RespResult.fail("请先登录");
        }
        List<PurchaseOrder> orders = purchaseOrderService.getUserOrders(loginUser.getId());
        return RespResult.success("查询成功", orders);
    }
}
