package com.xy.verfiy.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DexCompileTask {
    private Long id;
    private String taskId;
    private Long userId;
    private String javaCode;
    private String dexFilePath;
    private String compileLog;
    private Boolean success;
    private Boolean downloaded;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

