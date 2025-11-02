package com.xy.verfiy.mapper;

import com.xy.verfiy.domain.HookInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HookInfoMapper {

    List<HookInfo> listByApp(@Param("appId") Long appId);

    HookInfo findById(@Param("id") Long id);

    HookInfo findByIdAndAppId(@Param("id") Long id, @Param("appId") Long appId);

    HookInfo findExact(@Param("appId") Long appId,
                       @Param("packageName") String packageName,
                       @Param("version") String version);

    HookInfo findEffective(@Param("appId") Long appId,
                           @Param("packageName") String packageName,
                           @Param("version") String version);

    int insert(HookInfo hookInfo);

    int update(HookInfo hookInfo);

    int updateEnabled(@Param("id") Long id,
                      @Param("appId") Long appId,
                      @Param("enabled") boolean enabled,
                      @Param("updatedBy") String updatedBy);

    int delete(@Param("id") Long id, @Param("appId") Long appId);
}


