package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.DexCompileTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DexCompileTaskMapper {
    
    void insert(DexCompileTask task);
    
    DexCompileTask findByTaskId(@Param("taskId") String taskId);
    
    void updateDownloaded(@Param("taskId") String taskId);
    
    void deleteByTaskId(@Param("taskId") String taskId);
    
    /**
     * 查询某个用户未下载的编译成功的任务
     */
    List<DexCompileTask> findUndownloadedByUserId(@Param("userId") Long userId);
    
    /**
     * 查询某个用户的所有编译任务（最近20条）
     */
    List<DexCompileTask> findAllByUserId(@Param("userId") Long userId);
}

