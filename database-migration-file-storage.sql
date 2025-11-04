-- 数据库迁移脚本：文件系统存储优化
-- 将 dex_data 和 zip_data 字段从 LONGTEXT 改为 VARCHAR(255)
-- 原因：现在存储文件路径而非 Base64 数据，大大减少数据库体积

-- 使用数据库
USE verfiy;

-- ===================================================
-- 第一步：备份现有数据（重要！）
-- ===================================================
-- 建议在执行迁移前手动备份数据库：
-- mysqldump -u root -p verfiy > verfiy_backup_$(date +%Y%m%d_%H%M%S).sql

-- ===================================================
-- 第二步：创建临时表保存需要迁移的文件数据
-- ===================================================
DROP TABLE IF EXISTS `hook_info_file_backup`;
CREATE TABLE `hook_info_file_backup` (
  `id` bigint NOT NULL,
  `dex_data` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Dex Base64 数据（迁移前备份）',
  `zip_data` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Zip Base64 数据（迁移前备份）',
  `backup_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hook 文件数据迁移备份表';

-- 备份所有包含文件数据的记录
INSERT INTO `hook_info_file_backup` (`id`, `dex_data`, `zip_data`)
SELECT `id`, `dex_data`, `zip_data`
FROM `hook_info`
WHERE `dex_data` IS NOT NULL OR `zip_data` IS NOT NULL;

SELECT CONCAT('已备份 ', COUNT(*), ' 条包含文件数据的记录') AS message
FROM `hook_info_file_backup`;

-- ===================================================
-- 第三步：清空现有的文件数据（准备迁移）
-- ===================================================
-- 警告：此步骤会清空所有文件数据！
-- 如果需要保留数据，请先运行文件提取脚本将 Base64 转为文件

UPDATE `hook_info` SET `dex_data` = NULL WHERE `dex_data` IS NOT NULL;
UPDATE `hook_info` SET `zip_data` = NULL WHERE `zip_data` IS NOT NULL;

SELECT '已清空所有文件数据字段' AS message;

-- ===================================================
-- 第四步：修改字段类型
-- ===================================================
-- 将 LONGTEXT 改为 VARCHAR(255)，存储文件路径

ALTER TABLE `hook_info` 
  MODIFY COLUMN `dex_data` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Dex 文件路径（相对于存储根目录）';

ALTER TABLE `hook_info` 
  MODIFY COLUMN `zip_data` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'Zip 文件路径（相对于存储根目录）';

SELECT '字段类型修改完成：LONGTEXT → VARCHAR(255)' AS message;

-- ===================================================
-- 第五步：验证迁移结果
-- ===================================================
SELECT 
  TABLE_NAME,
  COLUMN_NAME,
  COLUMN_TYPE,
  COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'hook_info'
  AND COLUMN_NAME IN ('dex_data', 'zip_data');

-- ===================================================
-- 清理说明
-- ===================================================
-- 迁移完成后的操作：
-- 
-- 1. 确认新系统运行正常后，可以删除备份表：
--    DROP TABLE IF EXISTS `hook_info_file_backup`;
--
-- 2. 如果需要从备份恢复数据：
--    UPDATE hook_info h
--    INNER JOIN hook_info_file_backup b ON h.id = b.id
--    SET h.dex_data = b.dex_data, h.zip_data = b.zip_data;
--
-- 3. 后续可以添加定期清理孤儿文件的定时任务

-- ===================================================
-- 完成提示
-- ===================================================
SELECT '===================================================' AS '';
SELECT '✓ 数据库迁移完成！' AS message;
SELECT '===================================================' AS '';
SELECT '下一步操作：' AS '';
SELECT '1. 更新 application.properties 配置文件存储路径' AS step;
SELECT '2. 重启后端服务' AS step;
SELECT '3. 测试文件上传功能' AS step;
SELECT '4. 确认无误后可删除 hook_info_file_backup 表' AS step;
SELECT '===================================================' AS '';

