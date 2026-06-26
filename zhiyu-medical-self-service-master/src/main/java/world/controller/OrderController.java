package world.controller;

import jakarta.servlet.http.HttpSession;
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

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private MedicineService medicineService;

    @PostMapping("/create")
    public RespResult createOrder(Integer medicineId, Integer quantity, HttpSession session) {
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
    }

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
        if (order.getStatus() != 0) {
            return RespResult.fail("订单状态异常");
        }
        // 检查订单是否已过期（超过30分钟）
        if (purchaseOrderService.isExpired(order)) {
            purchaseOrderService.cancel(orderId);
            return RespResult.fail("订单已过期，已自动取消");
        }
        purchaseOrderService.pay(orderId, payType);
        return RespResult.success("支付成功");
    }

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
        purchaseOrderService.cancel(orderId);
        return RespResult.success("订单已取消");
    }

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