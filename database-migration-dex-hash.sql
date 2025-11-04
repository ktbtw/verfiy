-- 添加 dex_hash 字段到 hook_info 表
-- 用于优化 dex 传输，只在哈希变化时才下载完整 dex

ALTER TABLE hook_info ADD COLUMN dex_hash VARCHAR(64) COMMENT 'Dex文件的SHA-256哈希值' AFTER dex_data;

-- 为已存在的记录更新索引（可选）
CREATE INDEX idx_dex_hash ON hook_info(dex_hash);

