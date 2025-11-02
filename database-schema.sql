-- xyz验证 - 卡密验证系统数据库初始化脚本
-- 作者: ktbtw
-- 创建时间: 2025-10-11
-- 适用于 MySQL 5.4+

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS verfiy CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE verfiy;

-- ----------------------------
-- 表结构: users (用户表)
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码（加密）',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ----------------------------
-- 表结构: application (应用表)
-- ----------------------------
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '应用名称',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用描述',
  `owner` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '所有者',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `icon` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '应用图标URL',
  `secure` tinyint(1) DEFAULT '0' COMMENT '是否启用传输安全',
  `announcement` text COLLATE utf8mb4_unicode_ci COMMENT '公告内容',
  `version` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '版本号',
  `changelog` text COLLATE utf8mb4_unicode_ci COMMENT '更新日志',
  `encryption_alg` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '加密算法（RC4/AES-128-CBC/AES-256-CBC）',
  `redeem_extra` text COLLATE utf8mb4_unicode_ci COMMENT '自定义返回参数（JSON）',
  `api_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'API密钥',
  `secret_key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '签名密钥',
  `redeem_extra_mode` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'SUCCESS_ONLY' COMMENT '返回参数模式（ALWAYS/SUCCESS_ONLY/FAILURE_ONLY）',
  `app_type` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT 'NORMAL' COMMENT '应用类型（NORMAL/XPOSED）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_api_key` (`api_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应用表';

-- ----------------------------
-- 表结构: hook_info (Hook 配置表)
-- ----------------------------
DROP TABLE IF EXISTS `hook_info`;
CREATE TABLE `hook_info` (
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

-- ----------------------------
-- 表结构: card (卡密表)
-- ----------------------------
DROP TABLE IF EXISTS `card`;
CREATE TABLE `card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '卡密码',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态（NEW/ACTIVE/EXPIRED/USED）',
  `expire_at` timestamp NULL DEFAULT NULL COMMENT '过期时间',
  `activated_at` timestamp NULL DEFAULT NULL COMMENT '激活时间',
  `disabled` tinyint(1) DEFAULT '0' COMMENT '是否禁用',
  `metadata` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '元数据（JSON）',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `app_id` bigint DEFAULT NULL COMMENT '所属应用ID',
  `max_machines` int DEFAULT NULL COMMENT '最大机器数（NULL表示不限）',
  `extra` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '附加信息（JSON格式）',
  `return_extra` tinyint(1) DEFAULT '0' COMMENT '验证成功时是否返回附加信息',
  PRIMARY KEY (`id`),
  UNIQUE KEY `card_code` (`card_code`),
  KEY `idx_card_status` (`status`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡密表';

-- ----------------------------
-- 表结构: card_machine (卡密机器码表)
-- ----------------------------
DROP TABLE IF EXISTS `card_machine`;
CREATE TABLE `card_machine` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_id` bigint NOT NULL COMMENT '卡密ID',
  `machine` varchar(128) NOT NULL COMMENT '机器码',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_card_machine` (`card_id`,`machine`),
  CONSTRAINT `fk_cm_card` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡密机器码绑定表';

-- ----------------------------
-- 表结构: card_use_log (卡密使用日志表)
-- ----------------------------
DROP TABLE IF EXISTS `card_use_log`;
CREATE TABLE `card_use_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `card_id` bigint NOT NULL COMMENT '卡密ID',
  `card_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '卡密码',
  `action` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作类型',
  `request_ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求IP',
  `request_user` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求用户',
  `remark` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_card_id` (`card_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_card_id` FOREIGN KEY (`card_id`) REFERENCES `card` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡密使用日志表';

-- ----------------------------
-- 表结构: notice (公告表)
-- ----------------------------
DROP TABLE IF EXISTS `notice`;
CREATE TABLE `notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '公告标题',
  `content` text COLLATE utf8mb4_unicode_ci COMMENT '公告内容',
  `status` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '状态',
  `publish_at` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `app_id` bigint DEFAULT NULL COMMENT '所属应用ID',
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公告表';


-- ----------------------------
-- 邀请表: invite_codes (公告表)
-- ----------------------------
DROP TABLE IF EXISTS `invite_codes`;
CREATE TABLE invite_codes (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL UNIQUE,
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  used TINYINT(1) NOT NULL DEFAULT 0,
  used_by VARCHAR(64) DEFAULT NULL,
  used_at DATETIME DEFAULT NULL,
  can_invite TINYINT(1) DEFAULT 0 COMMENT '新用户是否有邀请权限',
  invite_quota INT DEFAULT 0 COMMENT '新用户的邀请配额（-1表示无限制）'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- 创建默认管理员账户
-- 用户名: admin
-- 密码: admin123 (请在首次登录后立即修改)
-- 密码使用 BCrypt 加密
-- ----------------------------
INSERT INTO `users` (`username`, `password`, `enabled`) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5E.', 1);

SET FOREIGN_KEY_CHECKS = 1;

-- 完成提示
SELECT '数据库初始化完成！' AS message;
SELECT '默认管理员账户 - 用户名: admin, 密码: admin123 (请务必修改!)' AS notice;
-- 添加用户邀请权限和配额字段

-- 如果字段已存在会报错，可以忽略错误继续执行

ALTER TABLE users ADD COLUMN can_invite BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN invite_quota INT DEFAULT 0;

-- 给 admin 用户默认的邀请权限（无限配额）
UPDATE users SET can_invite = TRUE, invite_quota = -1 WHERE username = 'admin';




