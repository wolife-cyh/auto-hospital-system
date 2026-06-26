-- ============================================================
-- 异步任务队列支持 - 数据库迁移脚本
-- 为 analysis_history 表添加状态跟踪和任务管理字段
-- 执行时间: 2026-06
-- ============================================================

ALTER TABLE `analysis_history`
    ADD COLUMN `file_size`      BIGINT        DEFAULT NULL COMMENT '文件大小(字节)' AFTER `file_type`,
    ADD COLUMN `status`         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '任务状态：PENDING/PROCESSING/SUCCESS/WARNING/FAILED' AFTER `image_url`,
    ADD COLUMN `progress`       INT           NOT NULL DEFAULT 0 COMMENT '分析进度(0-100)' AFTER `status`,
    ADD COLUMN `error_message`  VARCHAR(1000) DEFAULT NULL COMMENT '失败原因' AFTER `progress`,
    ADD COLUMN `confidence`     DOUBLE        DEFAULT NULL COMMENT 'AI置信度' AFTER `primary_diagnosis`,
    ADD INDEX `idx_status` (`status`);
