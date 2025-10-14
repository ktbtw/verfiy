package com.xy.verfiy.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 速率限制服务
 * 防止滥用和DDoS攻击
 */
@Service
public class RateLimitService {
    
    // 每个IP的请求限制（每分钟）
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    
    // 存储请求记录 <IP, RateLimit>
    private final ConcurrentHashMap<String, RateLimit> requestCache = new ConcurrentHashMap<>();
    
    /**
     * 检查是否超过限流
     * @param identifier 标识符（如IP地址）
     * @return true表示被限流，false表示可以继续
     */
    public boolean isRateLimited(String identifier) {
        RateLimit rateLimit = requestCache.computeIfAbsent(identifier, k -> new RateLimit());
        return rateLimit.isLimited();
    }
    
    /**
     * 记录一次请求
     */
    public void recordRequest(String identifier) {
        RateLimit rateLimit = requestCache.computeIfAbsent(identifier, k -> new RateLimit());
        rateLimit.incrementRequest();
    }
    
    /**
     * 获取剩余请求次数
     */
    public int getRemainingRequests(String identifier) {
        RateLimit rateLimit = requestCache.get(identifier);
        if (rateLimit == null) {
            return MAX_REQUESTS_PER_MINUTE;
        }
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - rateLimit.getRequestCount());
    }
    
    /**
     * 速率限制内部类
     */
    private static class RateLimit {
        private int requestCount = 0;
        private LocalDateTime windowStart = LocalDateTime.now();
        
        public synchronized boolean isLimited() {
            resetIfNeeded();
            return requestCount >= MAX_REQUESTS_PER_MINUTE;
        }
        
        public synchronized void incrementRequest() {
            resetIfNeeded();
            requestCount++;
        }
        
        public synchronized int getRequestCount() {
            resetIfNeeded();
            return requestCount;
        }
        
        private void resetIfNeeded() {
            LocalDateTime now = LocalDateTime.now();
            // 如果超过1分钟，重置计数
            if (now.isAfter(windowStart.plusMinutes(1))) {
                requestCount = 0;
                windowStart = now;
            }
        }
    }
}

