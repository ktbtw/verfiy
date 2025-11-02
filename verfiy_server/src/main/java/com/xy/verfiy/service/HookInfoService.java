package com.xy.verfiy.service;

import com.xy.verfiy.domain.HookInfo;

import java.util.List;

public interface HookInfoService {

    List<HookInfo> listByApp(Long appId, String owner);

    HookInfo getById(Long id, String owner);

    HookInfo saveOrUpdate(HookInfo hookInfo, String owner);

    boolean updateStatus(Long id, boolean enabled, String owner);

    boolean delete(Long id, String owner);

    HookInfo findEffective(Long appId, String packageName, String version);

    HookInfo findZipInfo(Long appId, String packageName, String version);
}


