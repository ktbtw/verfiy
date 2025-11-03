-- 创建 Dex 编译任务表
CREATE TABLE IF NOT EXISTS dex_compile_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE COMMENT '任务ID',
    java_code TEXT NOT NULL COMMENT 'Java 源代码',
    dex_file_path VARCHAR(512) COMMENT 'Dex 文件路径',
    compile_log TEXT COMMENT '编译日志',
    success BOOLEAN DEFAULT FALSE COMMENT '是否编译成功',
    downloaded BOOLEAN DEFAULT FALSE COMMENT '是否已下载',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_task_id (task_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dex 编译任务表';
ALTER TABLE dex_compile_task 
ADD COLUMN user_id BIGINT COMMENT '用户ID' AFTER task_id,
ADD INDEX idx_user_id (user_id);
