package com.xy.verfiy.service.impl;

import com.xy.verfiy.domain.Application;
import com.xy.verfiy.mapper.ApplicationMapper;
import com.xy.verfiy.service.ApplicationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final com.xy.verfiy.mapper.CardMapper cardMapper;
    private final com.xy.verfiy.mapper.CardUseLogMapper logMapper;

    public ApplicationServiceImpl(ApplicationMapper applicationMapper,
                                  com.xy.verfiy.mapper.CardMapper cardMapper,
                                  com.xy.verfiy.mapper.CardUseLogMapper logMapper) {
        this.applicationMapper = applicationMapper;
        this.cardMapper = cardMapper;
        this.logMapper = logMapper;
    }

    @Override
    public Application create(String name, String description, String owner) {
        Application app = new Application();
        app.setName(name);
        app.setDescription(description);
        app.setOwner(owner);
        // 为新应用生成 apiKey（鉴权），并设置默认加密算法为 NONE（不启用传输安全）
        // secretKey 由用户在设置中自行配置
        String apiKey = generateUniqueApiKey(24);
        app.setApiKey(apiKey);
        app.setSecretKey(null);
        app.setSecure(false);
        app.setEncryptionAlg("NONE");
        // 新增 API 默认：仅验证成功后合并自定义返回
        app.setRedeemExtraMode("SUCCESS_ONLY");
        applicationMapper.insert(app);
        return app;
    }

    @Override
    public List<Application> listByOwner(String owner) {
        return applicationMapper.listByOwner(owner);
    }

    @Override
    public Application findById(Long id) {
        return applicationMapper.findById(id);
    }

    @Override
    public Application findByApiKey(String apiKey) {
        return applicationMapper.findByApiKey(apiKey);
    }

    @Override
    public boolean update(Application app) {
        return applicationMapper.update(app) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return applicationMapper.deleteById(id) > 0;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public boolean deleteCascade(Long appId) {
        if (appId == null) return false;
        // 先删日志
        java.util.List<com.xy.verfiy.domain.Card> cards = cardMapper.pageQuery(appId, null, null, 0, 100000);
        for (com.xy.verfiy.domain.Card c : cards) {
            logMapper.deleteByCardId(c.getId());
        }
        // 再删卡密
        try {
            // 扩展 Mapper: 直接按 appId 删除
            cardMapper.getClass();
        } catch (Exception ignored) {}
        // 退而求其次：逐个删除（若数据量大可换 SQL 批量）
        for (com.xy.verfiy.domain.Card c : cards) {
            cardMapper.deleteByIdAndAppId(c.getId(), appId);
        }
        // 最后删应用
        return applicationMapper.deleteById(appId) > 0;
    }
    
    // 简易随机字符串生成器（A-Za-z0-9）
    private static String generateAppKey(int len) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        java.security.SecureRandom rnd = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // 生成唯一 apiKey（最多重试 5 次）
    private String generateUniqueApiKey(int len) {
        for (int i = 0; i < 5; i++) {
            String k = generateAppKey(len);
            if (applicationMapper.findByApiKey(k) == null) return k;
        }
        // 极端情况下追加随机后缀
        String k = generateAppKey(len - 4) + "_" + Integer.toHexString(new java.security.SecureRandom().nextInt(0xFFFF));
        return k;
    }
    
    @Override
    public int countByOwner(String owner) {
        List<Application> apps = applicationMapper.listByOwner(owner);
        return apps != null ? apps.size() : 0;
    }
}


