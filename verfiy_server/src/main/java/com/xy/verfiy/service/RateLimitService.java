package com.xy.verfiy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局访问频率限制服务（基于 IP）
 */
@Slf4j
@Service
public class RateLimitService {

    // IP 访问记录：clientIp -> 访问记录
    private final Map<String, IpAccessRecord> accessRecords = new ConcurrentHashMap<>();

    // 每分钟最多请求次数
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    
    // 时间窗口：1分钟
    private static final int TIME_WINDOW_MINUTES = 1;

    /**
     * 检查 IP 是否被限流
     */
    public boolean isRateLimited(String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        IpAccessRecord record = accessRecords.computeIfAbsent(clientIp, k -> new IpAccessRecord());

        // 清理过期的访问记录
        record.cleanExpiredRecords(now, TIME_WINDOW_MINUTES);

        // 检查是否超过限制
        int currentCount = record.getRequestCount();
        if (currentCount >= MAX_REQUESTS_PER_MINUTE) {
            log.warn("IP {} 访问频率超限，当前次数: {}/{}", clientIp, currentCount, MAX_REQUESTS_PER_MINUTE);
            return true;
        }

        return false;
    }

    /**
     * 记录请求
     */
    public void recordRequest(String clientIp) {
        LocalDateTime now = LocalDateTime.now();
        IpAccessRecord record = accessRecords.computeIfAbsent(clientIp, k -> new IpAccessRecord());
        record.addRequest(now);
    }

    /**
     * 获取剩余请求次数
     */
    public int getRemainingRequests(String clientIp) {
        IpAccessRecord record = accessRecords.get(clientIp);
        if (record == null) {
            return MAX_REQUESTS_PER_MINUTE;
        }

        LocalDateTime now = LocalDateTime.now();
        record.cleanExpiredRecords(now, TIME_WINDOW_MINUTES);
        
        int used = record.getRequestCount();
        return Math.max(0, MAX_REQUESTS_PER_MINUTE - used);
    }

    /**
     * IP 访问记录
     */
    private static class IpAccessRecord {
        private final ConcurrentHashMap<LocalDateTime, Boolean> requestTimes = new ConcurrentHashMap<>();

        public void addRequest(LocalDateTime time) {
            requestTimes.put(time, true);
        }

        public int getRequestCount() {
            return requestTimes.size();
        }

        public void cleanExpiredRecords(LocalDateTime now, int windowMinutes) {
            LocalDateTime cutoffTime = now.minusMinutes(windowMinutes);
            requestTimes.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoffTime));
        }
    }
}
