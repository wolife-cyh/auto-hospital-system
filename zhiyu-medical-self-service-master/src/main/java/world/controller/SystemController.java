package world.controller;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import world.constant.MedicalConstants;
import world.entity.*;
import world.service.PurchaseOrderService;
import world.utils.Assert;

import java.util.*;

@Controller
public class SystemController extends BaseController<User> {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    /**
     * 首页
     */
    @GetMapping("/index.html")
    public String index(Map<String, Object> map) {
        return "index";
    }

    /**
     * 智能医生
     */
    @GetMapping("/doctor")
    public String doctor(Map<String, Object> map,
                         @RequestParam(required = false) String consultContext,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (Assert.isEmpty(loginUser)) {
            redirectAttributes.addFlashAttribute("needLogin", true);
            return "redirect:/index.html";
        }

        // 保存咨询上下文到session
        if (consultContext != null && !consultContext.isEmpty()) {
            session.setAttribute("consultContext", consultContext);
        }

        return "doctor";
    }

    /**
     * 退出登录
     */
    @GetMapping("/logout")
    public String logout() {
        session.invalidate();
        return "redirect:/index.html";
    }

    /**
     * 所有反馈
     */
    @GetMapping("/all-feedback")
    public String feedback(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Feedback> feedbackList = feedbackService.all();

        map.put("feedbackList", feedbackList);
        return "all-feedback";
    }

    /**
     * 我的资料
     */
    @GetMapping("/profile")
    public String profile(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        return "profile";
    }

    /**
     * 查询相关疾病
     */
    @GetMapping("findIllness")
    public String findIllness(Map<String, Object> map, Integer kind, String illnessName, Integer page) {
        // 处理page
        page = ObjectUtils.isEmpty(page) ? 1 : page;

        Map<String, Object> illness = illnessService.findIllness(kind, illnessName, page);
        if (Assert.notEmpty(kind)) {
            map.put("title", illnessKindService.get(kind).getName() + (illnessName == null ? "" : ('"' + illnessName + '"' + "的搜索结果")));
        } else {
            map.put("title", illnessName == null ? "全部" : ('"' + illnessName + '"' + "的搜索结果"));
        }
        if (loginUser != null && kind != null) {
            historyService.insetOne(loginUser.getId(), MedicalConstants.TYPE_OPERATE,
                    illnessKindService.get(kind).getId() + "," + (Assert.isEmpty(illnessName) ? "无" : illnessName));
        }
        if (loginUser != null && Assert.notEmpty(illnessName)) {
            historyService.insetOne(loginUser.getId(), MedicalConstants.TYPE_ILLNESS, illnessName);
        }
        map.putAll(illness);
        map.put("page", page);
        map.put("kind", kind);
        map.put("illnessName", illnessName);
        map.put("kindList", illnessKindService.findList());
        map.put("history", loginUser == null ? null : historyService.findList(loginUser.getId()));
        return "search-illness";
    }

    /**
     * 查询相关疾病下的药
     */
    @GetMapping("findIllnessOne")
    public String findIllnessOne(Map<String, Object> map, Integer id) {
        Map<String, Object> illnessOne = illnessService.findIllnessOne(id);
        Illness illness = illnessService.get(id);
        if (loginUser != null) {
            historyService.insetOne(loginUser.getId(), MedicalConstants.TYPE_ILLNESS, illness.getIllnessName());
        }
        map.putAll(illnessOne);
        return "illness-reviews";
    }

    /**
     * 查询相关疾病下的药
     */
    @GetMapping("findMedicineOne")
    public String findMedicineOne(Map<String, Object> map, Integer id) {
        Medicine medicine = medicineService.get(id);
//        historyService.insetOne(loginUser.getId(),MedicalConstants.TYPE_MEDICINE,medicine.getMedicineName());
        map.put("medicine", medicine);
        return "medicine";
    }

    /**
     * 查询相关疾病下的药
     */
    @GetMapping("findMedicines")
    public String findMedicines(Map<String, Object> map, String nameValue, Integer page) {
        // 处理page
        page = ObjectUtils.isEmpty(page) ? 1 : page;
        if (loginUser != null && Assert.notEmpty(nameValue)) {
            historyService.insetOne(loginUser.getId(), MedicalConstants.TYPE_MEDICINE, nameValue);
        }
        map.putAll(medicineService.getMedicineList(nameValue, page));
        map.put("page", page);
        map.put("history", loginUser == null ? null : historyService.findList(loginUser.getId()));
        map.put("title", nameValue);
        return "illness";
    }

    /**
     * 查询相关疾病下的药
     */
    @GetMapping("globalSelect")
    public String globalSelect(Map<String, Object> map, String nameValue) {
        nameValue = nameValue.replace("，", ",");
        List<String> idArr = Arrays.asList(nameValue.split(","));
        //首先根据关键字去查询
        Set<Illness> illnessSet = new HashSet<>();
        idArr.forEach(s -> {
            Illness one = illnessService.getOne(new QueryWrapper<Illness>().like("illness_name", s));
            if (ObjectUtil.isNotNull(one)) {
                illnessSet.add(one);
            }
        });
        idArr.forEach(s -> {
            Illness one = illnessService.getOne(new QueryWrapper<Illness>().like("special_symptom", s));
            if (ObjectUtil.isNotNull(one)) {
                illnessSet.add(one);
            }
        });
        idArr.forEach(s -> {
            Illness one = illnessService.getOne(new QueryWrapper<Illness>().like("illness_symptom", s));
            if (ObjectUtil.isNotNull(one)) {
                illnessSet.add(one);
            }
        });
        map.put("illnessSet", illnessSet);
        return "index";
    }

    /**
     * 添加疾病页面
     */
    @GetMapping("add-illness")
    public String addIllness(Integer id, Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        Illness illness = new Illness();
        if (Assert.notEmpty(id)) {
            illness = illnessService.get(id);
        }
        List<IllnessKind> illnessKinds = illnessKindService.all();
        map.put("illness", illness);
        map.put("kinds", illnessKinds);
        return "add-illness";
    }

    /**
     * 添加药品页面
     */
    @GetMapping("add-medical")
    public String addMedical(Integer id, Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        Medicine medicine = new Medicine();
        // 构建已关联疾病列表（JSON 友好格式）
        List<Map<String, Object>> associatedIllnesses = new ArrayList<>();
        if (Assert.notEmpty(id)) {
            medicine = medicineService.get(id);
            List<IllnessMedicine> mappings = illnessMedicineService.query(
                    IllnessMedicine.builder().medicineId(id).build());
            for (IllnessMedicine mapping : mappings) {
                Illness illness = illnessService.get(mapping.getIllnessId());
                if (illness != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("illnessMedicineId", mapping.getId());
                    item.put("illnessId", illness.getId());
                    item.put("illnessName", illness.getIllnessName());
                    associatedIllnesses.add(item);
                }
            }
        }
        map.put("medicine", medicine);
        map.put("associatedIllnesses", associatedIllnesses);
        return "add-medical";
    }

    /**
     * 疾病管理页面
     */
    @GetMapping("all-illness")
    public String allIllness(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Illness> illnesses = illnessService.all();
        for (Illness illness : illnesses) {
            illness.setKind(illnessKindService.get(illness.getKindId()));
        }
        map.put("illnesses", illnesses);
        return "all-illness";
    }

    /**
     * 药品管理页面
     */
    @GetMapping("all-medical")
    public String allMedical(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<Medicine> medicines = medicineService.all();
        map.put("medicines", medicines);
        return "all-medical";
    }

    /**
     * 我的订单页面
     */
    @GetMapping("orders")
    public String orders(Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        List<PurchaseOrder> orders = purchaseOrderService.getUserOrders(loginUser.getId());
        // 标记过期/未过期状态，避免前端对null取反报错
        for (PurchaseOrder order : orders) {
            if (order.getStatus() == 0) {
                order.setExpired(purchaseOrderService.isExpired(order));
            }
        }
        map.put("orders", orders);
        return "orders";
    }

    /**
     * 支付页面
     */
    @GetMapping("payment")
    public String payment(Integer orderId, Map<String, Object> map) {
        if (Assert.isEmpty(loginUser)) {
            return "redirect:/index.html";
        }
        if (orderId == null) {
            return "redirect:/orders";
        }
        PurchaseOrder order = purchaseOrderService.getById(orderId);
        if (order == null || !order.getUserId().equals(loginUser.getId())) {
            return "redirect:/orders";
        }
        if (order.getStatus() != 0) {
            return "redirect:/orders";
        }
        // 检查订单是否已过期
        if (purchaseOrderService.isExpired(order)) {
            purchaseOrderService.cancel(orderId);
            return "redirect:/orders";
        }
        map.put("order", order);
        return "payment";
    }
}
