package com.xy.verfiy.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录尝试次数限制服务
 * 防止暴力破解攻击
 */
@Service
public class LoginAttemptService {
    
    // 最大失败次数
    private static final int MAX_ATTEMPTS = 5;
    // 锁定时间（分钟）
    private static final int LOCK_TIME_MINUTES = 15;
    
    // 存储登录失败记录 <username, LoginAttempt>
    private final ConcurrentHashMap<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();
    
    /**
     * 记录登录成功
     */
    public void loginSucceeded(String username) {
        attemptsCache.remove(username);
    }
    
    /**
     * 记录登录失败
     */
    public void loginFailed(String username) {
        LoginAttempt attempt = attemptsCache.computeIfAbsent(username, k -> new LoginAttempt());
        attempt.incrementFailCount();
        
        if (attempt.getFailCount() >= MAX_ATTEMPTS) {
            attempt.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_TIME_MINUTES));
        }
    }
    
    /**
     * 检查用户是否被锁定
     */
    public boolean isBlocked(String username) {
        LoginAttempt attempt = attemptsCache.get(username);
        if (attempt == null) {
            return false;
        }
        
        // 检查是否已过锁定时间
        if (attempt.getLockedUntil() != null) {
            if (LocalDateTime.now().isAfter(attempt.getLockedUntil())) {
                // 锁定时间已过，清除记录
                attemptsCache.remove(username);
                return false;
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取剩余失败次数
     */
    public int getRemainingAttempts(String username) {
        LoginAttempt attempt = attemptsCache.get(username);
        if (attempt == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - attempt.getFailCount());
    }
    
    /**
     * 获取锁定剩余时间（分钟）
     */
    public long getLockRemainingMinutes(String username) {
        LoginAttempt attempt = attemptsCache.get(username);
        if (attempt == null || attempt.getLockedUntil() == null) {
            return 0;
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(attempt.getLockedUntil())) {
            return 0;
        }
        
        return java.time.Duration.between(now, attempt.getLockedUntil()).toMinutes();
    }
    
    /**
     * 登录尝试记录内部类
     */
    private static class LoginAttempt {
        private int failCount = 0;
        private LocalDateTime lockedUntil;
        
        public void incrementFailCount() {
            failCount++;
        }
        
        public int getFailCount() {
            return failCount;
        }
        
        public LocalDateTime getLockedUntil() {
            return lockedUntil;
        }
        
        public void setLockedUntil(LocalDateTime lockedUntil) {
            this.lockedUntil = lockedUntil;
        }
    }
}

