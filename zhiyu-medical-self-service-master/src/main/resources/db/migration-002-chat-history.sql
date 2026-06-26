-- ============================================================
-- 智能医生对话历史 - 数据库迁移脚本（v2）
-- 新增会话表 + 重构对话消息表，支持边栏多会话管理
-- 执行时间: 2026-06
-- ============================================================

-- 1. 新建会话表
CREATE TABLE IF NOT EXISTS `chat_conversation` (
  `id` varchar(64) NOT NULL COMMENT '会话ID(UUID)',
  `user_id` int(11) NOT NULL COMMENT '用户ID',
  `title` varchar(200) DEFAULT NULL COMMENT '会话标题',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后活动时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_updated_at` (`updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能医生会话';

-- 2. 为 chat_history 添加 conversation_id 字段
-- 注意：如果之前已执行过 v1 版本，表可能已存在。这里先 alter 再重建索引
ALTER TABLE `chat_history`
  ADD COLUMN `conversation_id` varchar(64) DEFAULT NULL COMMENT '会话ID' AFTER `user_id`,
  ADD INDEX `idx_conversation_id` (`conversation_id`);

-- 3. 保持 session_id 字段向下兼容（不删旧字段，但不再使用）
