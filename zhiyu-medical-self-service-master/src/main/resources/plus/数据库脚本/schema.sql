-- 如果 purchase_order 表不存在则自动创建
CREATE TABLE IF NOT EXISTS `purchase_order` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `medicine_id` int(11) NOT NULL COMMENT '药品ID',
  `medicine_name` varchar(100) DEFAULT NULL COMMENT '药品名称快照',
  `medicine_img` varchar(255) DEFAULT NULL COMMENT '药品图片快照',
  `quantity` int(11) NOT NULL DEFAULT 1 COMMENT '购买数量',
  `price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `total_price` decimal(10,2) NOT NULL COMMENT '总价',
  `status` int(1) NOT NULL DEFAULT 0 COMMENT '状态：0待支付，1已支付，2已取消',
  `pay_type` varchar(32) DEFAULT NULL COMMENT '支付方式',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_medicine_id` (`medicine_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品订单表';
