# Payment Feature Implementation Plan

## Analysis

The project already has most payment code (OrderController, PurchaseOrderService, SystemController routes, templates). Only 3 things are missing.

## Files to Create/Modify

### 1. `Illness.java` — Add `img_path` field
**Path**: `src/main/java/world/entity/Illness.java`
**Action**: Add 1 field:
```java
private String imgPath;  // 疾病图片路径
```
This is needed by `illness-reviews.html` template which displays `illness.imgPath`.

### 2. `DatabaseMigrationRunner.java` — Create
**Path**: `src/main/java/world/config/DatabaseMigrationRunner.java`
**Action**: Copy from `plus/后端代码/world/config/DatabaseMigrationRunner.java`
- Auto-adds `img_path` column to `illness` table on startup
- Updates all illness images to local paths (10 diseases)
- Updates all medicine images to local paths (7 medicines)

### 3. Static Images — Copy
**Source**: `src/main/resources/plus/静态资源/images/`
**Target**: `src/main/resources/static/assets/images/`
- `illness/` — 10 disease images (cold.webp, eczema.webp, fracture.webp, etc.)
- `medicine/` — 7 medicine images (amoxicillin.jpg, ganmaoling.webp, etc.)

## Payment Flow (Already Working After These Changes)

```
药品详情页 (medicine.html)
  → 选择数量 → 点击「立即支付」
  → POST /order/create → 创建订单 (status=0)
  → 跳转 /payment?orderId=xxx
  → payment.html: 30分钟倒计时 → 点击「确认支付」
  → POST /order/pay → 模拟支付 (status 0→1)
  → 支付成功 → 可查看 /orders 订单列表
```

## No Changes Needed (Already Exist)
- ✅ OrderController.java
- ✅ PurchaseOrderService.java (with PurchaseResult)
- ✅ PurchaseOrder.java entity
- ✅ PurchaseOrderDao.java
- ✅ SystemController.java (payment/orders routes)
- ✅ MedicineService.java (getMedicineList pagination)
- ✅ All templates (payment.html, orders.html, medicine.html, illness-reviews.html, etc.)
- ✅ purchase_order table (in smart-medicine.sql)
- ✅ AlipayController + AlipayService (real Alipay integration)
