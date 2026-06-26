package world.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Accessors(chain = true)
@TableName("purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String orderNo;
    private Integer userId;
    private Integer medicineId;
    private String medicineName;
    private String medicineImg;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private Integer status;
    private String payType;
    private Date payTime;
    private Date createTime;
    private Date updateTime;

    /** 非持久化：订单是否已过期（超过30分钟未支付） */
    @TableField(exist = false)
    private Boolean expired;
}