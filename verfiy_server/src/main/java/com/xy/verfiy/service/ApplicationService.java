package com.xy.verfiy.service;

import com.xy.verfiy.domain.Application;

import java.util.List;

public interface ApplicationService {
    Application create(String name, String description, String owner);
    List<Application> listByOwner(String owner);
    Application findById(Long id);
    Application findByApiKey(String apiKey);
    boolean update(Application app);
    boolean delete(Long id);

    // 级联删除：日志 -> 卡密 -> 应用
    boolean deleteCascade(Long appId);
    
    // 统计用户的应用数量
    int countByOwner(String owner);
}


