package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.DexCompileTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DexCompileTaskMapper {
    
    void insert(DexCompileTask task);
    
    DexCompileTask findByTaskId(@Param("taskId") String taskId);
    
    void updateDownloaded(@Param("taskId") String taskId);
    
    void deleteByTaskId(@Param("taskId") String taskId);
}

