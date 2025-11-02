-- 增量数据库迁移脚本
-- 日期: 2025-11-02
-- 说明: 添加 Hook 配置功能所需的字段和表
-- 安全性: 只添加不存在的字段和表，不会删除任何数据

USE verfiy;

-- ====================================
-- 步骤 1: 为 application 表添加 app_type 字段
-- ====================================
-- 检查字段是否存在，不存在则添加
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = 'verfiy' 
  AND TABLE_NAME = 'application' 
  AND COLUMN_NAME = 'app_type';

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE application ADD COLUMN `app_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT ''NORMAL'' COMMENT ''应用类型（NORMAL/XPOSED）''',
    'SELECT ''字段 app_type 已存在，跳过添加'' AS info');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 将现有应用的 app_type 设置为 NORMAL（如果为 NULL）
UPDATE application SET app_type = 'NORMAL' WHERE app_type IS NULL;

-- ====================================
-- 步骤 2: 创建 hook_info 表（如果不存在）
-- ====================================
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
  `created_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '创建人',
  `updated_by` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '更新人',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_pkg_ver` (`app_id`,`package_name`,`version`),
  KEY `idx_app_id` (`app_id`),
  CONSTRAINT `fk_hook_app` FOREIGN KEY (`app_id`) REFERENCES `application` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Hook 配置表';

-- ====================================
-- 完成提示
-- ====================================
SELECT '✓ 数据库迁移完成！' AS status;
SELECT 
    CASE 
        WHEN @col_exists = 0 THEN '✓ 已添加 application.app_type 字段'
        ELSE '- application.app_type 字段已存在'
    END AS app_type_status;
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'verfiy' AND TABLE_NAME = 'hook_info') > 0 
        THEN '✓ hook_info 表已就绪'
        ELSE '✗ hook_info 表创建失败，请检查'
    END AS hook_info_status;

