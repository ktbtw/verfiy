-- ====================================================================
-- 统一增量迁移脚本
-- 说明: 将所有增量迁移合并到一个脚本中，用于在现有数据库上执行增量更新
-- 创建时间: 2025-11-04
-- 适用于: 在基于旧版 database-schema.sql 创建的数据库上执行增量更新
-- ====================================================================

USE verfiy;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ====================================================================
-- 1. 为 users 表添加邀请权限相关字段
-- ====================================================================

-- 检查 can_invite 字段是否存在
SET @can_invite_exists = 0;
SELECT COUNT(*) INTO @can_invite_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'can_invite';

SET @sql = IF(@can_invite_exists = 0,
    'ALTER TABLE users ADD COLUMN `can_invite` TINYINT(1) DEFAULT 0 COMMENT ''是否有邀请权限''',
    'SELECT ''字段 can_invite 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 检查 invite_quota 字段是否存在
SET @invite_quota_exists = 0;
SELECT COUNT(*) INTO @invite_quota_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'users' 
  AND COLUMN_NAME = 'invite_quota';

SET @sql = IF(@invite_quota_exists = 0,
    'ALTER TABLE users ADD COLUMN `invite_quota` INT DEFAULT 0 COMMENT ''邀请配额（-1表示无限制）''',
    'SELECT ''字段 invite_quota 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 给 admin 用户默认的邀请权限（无限配额）
UPDATE users SET can_invite = TRUE, invite_quota = -1 WHERE username = 'admin' AND invite_quota = 0;

SELECT '✓ users 表邀请权限字段已添加/验证' AS status;

-- ====================================================================
-- 2. 为 application 表添加 app_type 字段
-- ====================================================================

SET @app_type_exists = 0;
SELECT COUNT(*) INTO @app_type_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'application' 
  AND COLUMN_NAME = 'app_type';

SET @sql = IF(@app_type_exists = 0,
    'ALTER TABLE application ADD COLUMN `app_type` VARCHAR(32) COLLATE utf8mb4_unicode_ci DEFAULT ''NORMAL'' COMMENT ''应用类型（NORMAL/XPOSED）''',
    'SELECT ''字段 app_type 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 将现有应用的 app_type 设置为 NORMAL（如果为 NULL）
UPDATE application SET app_type = 'NORMAL' WHERE app_type IS NULL;

SELECT '✓ application.app_type 字段已添加/验证' AS status;

-- ====================================================================
-- 3. 为 application 表添加 update_url 字段
-- ====================================================================

SET @update_url_exists = 0;
SELECT COUNT(*) INTO @update_url_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'application' 
  AND COLUMN_NAME = 'update_url';

SET @sql = IF(@update_url_exists = 0,
    'ALTER TABLE application ADD COLUMN `update_url` VARCHAR(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''更新下载链接'' AFTER `changelog`',
    'SELECT ''字段 update_url 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✓ application.update_url 字段已添加/验证' AS status;

-- ====================================================================
-- 4. 创建 hook_info 表（如果不存在）
-- ====================================================================

CREATE TABLE IF NOT EXISTS `hook_info` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL COMMENT '所属应用ID',
  `package_name` varchar(191) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '包名',
  `version` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '*' COMMENT '版本号，* 表示通配',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `data` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Hook 配置数据(JSON)',
  `dex_data` longtext COLLATE utf8mb4_unicode_ci COMMENT 'Dex 数据（Base64/Hex）',
  `zip_data` longtext COLLATE utf8mb4_unicode_ci COMMENT '资源压缩包（Base64/Hex）',
  `zip_version` int DEFAULT '0' COMMENT '资源版本号',
  `require_card_verification` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否需要卡密验证',
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_pkg_ver` (`app_id`,`package_name`,`version`),
  KEY `idx_app_id` (`app_id`),
  CONSTRAINT `fk_hook_app` FOREIGN KEY (`app_id`) REFERENCES `application` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hook 配置表';

SELECT '✓ hook_info 表已创建/验证' AS status;

-- ====================================================================
-- 5. 为 hook_info 表添加 require_card_verification 字段（如果不存在）
-- ====================================================================

SET @require_card_exists = 0;
SELECT COUNT(*) INTO @require_card_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'verfiy'
  AND TABLE_NAME = 'hook_info'
  AND COLUMN_NAME = 'require_card_verification';

SET @sql = IF(@require_card_exists = 0,
    'ALTER TABLE hook_info ADD COLUMN `require_card_verification` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否需要卡密验证''',
    'SELECT ''字段 require_card_verification 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE hook_info SET require_card_verification = 0 WHERE require_card_verification IS NULL;

SELECT '✓ hook_info.require_card_verification 字段已添加/验证' AS status;

-- ====================================================================
-- 6. 创建 dex_compile_task 表
-- ====================================================================

CREATE TABLE IF NOT EXISTS `dex_compile_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(64) NOT NULL COMMENT '任务ID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `java_code` TEXT NOT NULL COMMENT 'Java 源代码',
    `dex_file_path` VARCHAR(512) COMMENT 'Dex 文件路径',
    `compile_log` TEXT COMMENT '编译日志',
    `success` TINYINT(1) DEFAULT '0' COMMENT '是否编译成功',
    `downloaded` TINYINT(1) DEFAULT '0' COMMENT '是否已下载',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `task_id` (`task_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Dex 编译任务表';

SELECT '✓ dex_compile_task 表已创建/验证' AS status;

-- ====================================================================
-- 7. 添加 dex_hash 字段到 hook_info 表
-- 用途: 优化 dex 传输，只在哈希变化时才下载完整 dex
-- ====================================================================

-- 检查字段是否存在
SET @dex_hash_exists = 0;
SELECT COUNT(*) INTO @dex_hash_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'hook_info' 
  AND COLUMN_NAME = 'dex_hash';

-- 如果不存在则添加
SET @sql = IF(@dex_hash_exists = 0,
    'ALTER TABLE hook_info ADD COLUMN `dex_hash` VARCHAR(64) COMMENT ''Dex文件的SHA-256哈希值'' AFTER `dex_data`',
    'SELECT ''字段 dex_hash 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 创建索引（如果不存在）
SET @idx_dex_hash_exists = 0;
SELECT COUNT(*) INTO @idx_dex_hash_exists
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'verfiy'
  AND TABLE_NAME = 'hook_info'
  AND INDEX_NAME = 'idx_dex_hash';

SET @sql = IF(@idx_dex_hash_exists = 0,
    'CREATE INDEX idx_dex_hash ON hook_info(dex_hash)',
    'SELECT ''索引 idx_dex_hash 已存在，跳过创建'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✓ hook_info.dex_hash 字段已添加/验证' AS status;

-- ====================================================================
-- 8. 文件存储优化 - 修改 dex_data 和 zip_data 字段类型
-- 说明: 将 LONGTEXT 改为 VARCHAR(255) 存储文件路径
-- 警告: 此步骤会清空现有的文件数据！
-- ====================================================================

-- 检查当前 dex_data 字段类型
SET @dex_data_type = '';
SELECT DATA_TYPE INTO @dex_data_type
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'verfiy'
  AND TABLE_NAME = 'hook_info'
  AND COLUMN_NAME = 'dex_data';

-- 只有当前是 LONGTEXT 类型时才执行迁移
SET @need_migration = IF(@dex_data_type = 'longtext', 1, 0);

-- 如果需要迁移，先创建备份表
SET @sql = IF(@need_migration = 1,
    'CREATE TABLE IF NOT EXISTS `hook_info_file_backup` (
      `id` bigint NOT NULL,
      `dex_data` longtext COLLATE utf8mb4_unicode_ci COMMENT ''Dex Base64 数据（迁移前备份）'',
      `zip_data` longtext COLLATE utf8mb4_unicode_ci COMMENT ''Zip Base64 数据（迁移前备份）'',
      `backup_time` timestamp DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''Hook 文件数据迁移备份表''',
    'SELECT ''dex_data 已是 VARCHAR 类型，跳过文件存储迁移'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 备份数据
SET @sql = IF(@need_migration = 1,
    'INSERT IGNORE INTO `hook_info_file_backup` (`id`, `dex_data`, `zip_data`)
     SELECT `id`, `dex_data`, `zip_data`
     FROM `hook_info`
     WHERE `dex_data` IS NOT NULL OR `zip_data` IS NOT NULL',
    'SELECT ''跳过数据备份'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 清空文件数据
SET @sql = IF(@need_migration = 1,
    'UPDATE `hook_info` SET `dex_data` = NULL WHERE `dex_data` IS NOT NULL',
    'SELECT ''跳过清空 dex_data'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@need_migration = 1,
    'UPDATE `hook_info` SET `zip_data` = NULL WHERE `zip_data` IS NOT NULL',
    'SELECT ''跳过清空 zip_data'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 修改字段类型为 VARCHAR(255)
SET @sql = IF(@need_migration = 1,
    'ALTER TABLE `hook_info` 
      MODIFY COLUMN `dex_data` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''Dex 文件路径（相对于存储根目录）''',
    'SELECT ''跳过修改 dex_data 类型'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@need_migration = 1,
    'ALTER TABLE `hook_info` 
      MODIFY COLUMN `zip_data` VARCHAR(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT ''Zip 文件路径（相对于存储根目录）''',
    'SELECT ''跳过修改 zip_data 类型'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✓ 文件存储优化已完成/验证' AS status;

-- ====================================================================
-- 完成验证和状态报告
-- ====================================================================

SET FOREIGN_KEY_CHECKS = 1;

SELECT '====================================================================' AS '';
SELECT '✓ 统一增量迁移脚本执行完成！' AS message;
SELECT '====================================================================' AS '';

-- 验证所有关键表和字段
SELECT 
    '验证结果：' AS section,
    CASE WHEN (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'verfiy' AND TABLE_NAME = 'dex_compile_task') > 0 
         THEN '✓' ELSE '✗' END AS 'dex_compile_task 表',
    CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'verfiy' AND TABLE_NAME = 'hook_info' AND COLUMN_NAME = 'dex_hash') > 0 
         THEN '✓' ELSE '✗' END AS 'dex_hash 字段',
    CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'verfiy' AND TABLE_NAME = 'application' AND COLUMN_NAME = 'update_url') > 0 
         THEN '✓' ELSE '✗' END AS 'update_url 字段',
    CASE WHEN (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'verfiy' AND TABLE_NAME = 'users' AND COLUMN_NAME = 'can_invite') > 0 
         THEN '✓' ELSE '✗' END AS 'can_invite 字段';

-- 显示各表的记录数
SELECT '当前数据统计：' AS section;
SELECT 
    (SELECT COUNT(*) FROM users) AS 'users 用户数',
    (SELECT COUNT(*) FROM application) AS 'application 应用数',
    (SELECT COUNT(*) FROM hook_info) AS 'hook_info 配置数',
    (SELECT COUNT(*) FROM dex_compile_task) AS 'dex_compile_task 任务数',
    (SELECT COUNT(*) FROM card) AS 'card 卡密数';

SELECT '====================================================================' AS '';
SELECT '迁移脚本说明：' AS '';
SELECT '1. 所有字段和表的添加都是安全的（使用条件检查）' AS note;
SELECT '2. 不会删除任何现有数据（文件存储迁移除外）' AS note;
SELECT '3. 可以重复执行此脚本而不会出错' AS note;
SELECT '4. 文件存储迁移会备份到 hook_info_file_backup 表' AS note;
SELECT '====================================================================' AS '';

