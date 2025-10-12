-- 移除 app_key 列（不再需要向后兼容）
-- 执行此脚本前请确保已备份数据库

USE verfiy;

-- 删除 app_key 列（使用兼容的语法）
ALTER TABLE `application` DROP COLUMN `app_key`;

-- 验证列已删除
SHOW COLUMNS FROM `application`;
