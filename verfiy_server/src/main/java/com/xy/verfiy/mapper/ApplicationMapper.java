package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.Application;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApplicationMapper {
    int insert(Application app);
    List<Application> listByOwner(@Param("owner") String owner);
    Application findById(@Param("id") Long id);
    int update(Application app);
    int deleteById(@Param("id") Long id);
    Application findByApiKey(@Param("apiKey") String apiKey);
}


