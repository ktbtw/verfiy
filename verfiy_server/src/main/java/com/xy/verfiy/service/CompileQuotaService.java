package com.xy.verfiy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 编译配额管理服务（专门用于管理用户的编译次数限制）
 */
@Slf4j
@Service
public class CompileQuotaService {

    // 用户编译记录：userId -> 编译记录
    private final Map<Long, UserCompileRecord> compileRecords = new ConcurrentHashMap<>();

    // 普通用户限制：每小时最多编译次数
    private static final int NORMAL_USER_LIMIT_PER_HOUR = 5;
    
    // 时间窗口：1小时
    private static final int TIME_WINDOW_HOURS = 1;

    /**
     * 检查用户是否允许编译
     * @param userId 用户ID
     * @param isAdmin 是否是管理员
     * @return 是否允许编译
     */
    public boolean checkCompileAccess(Long userId, boolean isAdmin) {
        // 管理员不限制
        if (isAdmin) {
            log.debug("管理员用户 {} 无编译次数限制", userId);
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        UserCompileRecord record = compileRecords.computeIfAbsent(userId, k -> new UserCompileRecord());

        // 清理过期的编译记录
        record.cleanExpiredRecords(now, TIME_WINDOW_HOURS);

        // 检查是否超过限制
        int currentCount = record.getCompileCount();
        if (currentCount >= NORMAL_USER_LIMIT_PER_HOUR) {
            log.warn("用户 {} 编译次数超限，当前次数: {}/{}", userId, currentCount, NORMAL_USER_LIMIT_PER_HOUR);
            return false;
        }

        // 记录本次编译
        record.addCompile(now);
        log.info("用户 {} 编译次数: {}/{}", userId, currentCount + 1, NORMAL_USER_LIMIT_PER_HOUR);
        return true;
    }

    /**
     * 获取用户剩余编译次数
     */
    public int getRemainingCompiles(Long userId, boolean isAdmin) {
        if (isAdmin) {
            return -1; // -1 表示无限制
        }

        UserCompileRecord record = compileRecords.get(userId);
        if (record == null) {
            return NORMAL_USER_LIMIT_PER_HOUR;
        }

        LocalDateTime now = LocalDateTime.now();
        record.cleanExpiredRecords(now, TIME_WINDOW_HOURS);
        
        int used = record.getCompileCount();
        return Math.max(0, NORMAL_USER_LIMIT_PER_HOUR - used);
    }

    /**
     * 获取用户下次可以编译的时间
     */
    public LocalDateTime getNextAvailableTime(Long userId) {
        UserCompileRecord record = compileRecords.get(userId);
        if (record == null || record.getCompileCount() == 0) {
            return LocalDateTime.now();
        }

        return record.getOldestCompileTime().plusHours(TIME_WINDOW_HOURS);
    }

    /**
     * 用户编译记录
     */
    private static class UserCompileRecord {
        private final ConcurrentHashMap<LocalDateTime, Boolean> compileTimes = new ConcurrentHashMap<>();

        public void addCompile(LocalDateTime time) {
            compileTimes.put(time, true);
        }

        public int getCompileCount() {
            return compileTimes.size();
        }

        public LocalDateTime getOldestCompileTime() {
            return compileTimes.keySet().stream()
                    .min(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
        }

        public void cleanExpiredRecords(LocalDateTime now, int windowHours) {
            LocalDateTime cutoffTime = now.minusHours(windowHours);
            compileTimes.entrySet().removeIf(entry -> entry.getKey().isBefore(cutoffTime));
        }
    }
}

