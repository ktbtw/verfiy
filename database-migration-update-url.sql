-- 添加更新链接字段到 application 表
-- 创建时间: 2025-11-03
-- 用途: 支持在应用设置中配置更新下载链接

USE verfiy;

-- 添加 update_url 字段
ALTER TABLE `application` ADD COLUMN `update_url` VARCHAR(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新下载链接' AFTER `changelog`;

-- 完成提示
SELECT '更新链接字段添加完成！' AS message;

