-- 为 card 表添加附加信息字段
-- 执行此脚本前请确保已备份数据库

USE verfiy;

-- 添加 extra 字段（存储附加信息的 JSON）
ALTER TABLE `card` ADD COLUMN `extra` TEXT COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附加信息（JSON 格式）';

-- 添加 return_extra 字段（是否在验证成功时返回附加信息）
ALTER TABLE `card` ADD COLUMN `return_extra` TINYINT(1) DEFAULT 0 COMMENT '验证成功时是否返回附加信息';

-- 验证字段已添加
SHOW COLUMNS FROM `card`;

