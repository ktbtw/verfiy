-- 数据库迁移脚本：添加邀请码权限和配额功能
-- 执行日期: 2025-10-14
-- 说明: 此脚本为已有数据库添加邀请码的权限和配额字段，实现批量生成邀请码并设置新用户权限的功能

USE verfiy;

-- 1. 为 invite_codes 表添加权限和配额字段
-- 这些字段用于设置通过邀请码注册的新用户的邀请权限和配额

-- 检查字段是否已存在，如果已存在会报错，可忽略
ALTER TABLE invite_codes ADD COLUMN can_invite TINYINT(1) DEFAULT 0 COMMENT '新用户是否有邀请权限';
ALTER TABLE invite_codes ADD COLUMN invite_quota INT DEFAULT 0 COMMENT '新用户的邀请配额（-1表示无限制）';

-- 2. 为现有邀请码设置默认值（如果有）
UPDATE invite_codes SET can_invite = 0, invite_quota = 0 WHERE can_invite IS NULL;

-- 完成提示
SELECT '迁移完成！邀请码表已添加权限和配额字段。' AS message;
SELECT '现在可以批量生成邀请码，并为通过邀请码注册的用户设置邀请权限和配额。' AS notice;

