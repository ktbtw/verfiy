package com.xy.verfiy.service;

import com.xy.verfiy.domain.Card;
import com.xy.verfiy.domain.CardStatus;

import java.io.InputStream;
import java.util.List;

public interface CardService {
    List<Card> page(Long appId, String keyword, CardStatus status, int page, int size);
    long count(Long appId, String keyword, CardStatus status);

    int generateCards(Long appId, String prefix, int count, Integer expireValue,
                      String expireUnit, String formatPattern, String suffix, Integer maxMachines);
    
    int importCardsCsv(Long appId, InputStream inputStream);
    byte[] exportCardsCsv(Long appId, String keyword, CardStatus status);

    boolean disable(Long id, boolean disabled);
    boolean updateStatus(Long id, CardStatus status);

    boolean redeem(String code, String operator, String ip);

    // 新增：限定应用进行核销，防止跨应用核销
    boolean redeemForApp(String code, Long appId, String operator, String ip);

    // 查询卡密（用于占位符替换等场景）
    Card getByCode(String code);

    // 可重复验证（不改变状态），并支持机器码绑定与数量限制
    boolean verifyForApp(String code, Long appId, String machine);

    boolean delete(Long id, Long appId);
    
    // 统计用户所有应用的卡密总数
    int countByOwner(String owner);
}


