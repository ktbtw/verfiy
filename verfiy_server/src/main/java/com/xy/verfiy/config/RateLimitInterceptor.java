package com.xy.verfiy.config;

import com.xy.verfiy.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 速率限制拦截器
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    private final RateLimitService rateLimitService;
    
    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取客户端IP
        String clientIp = getClientIP(request);
        
        // 跳过登录和注册接口的限流（这些接口有自己的保护机制）
        String uri = request.getRequestURI();
        if (uri.contains("/api/auth/login") || uri.contains("/api/auth/register")) {
            return true;
        }
        
        // 检查是否被限流
        if (rateLimitService.isRateLimited(clientIp)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"请求过于频繁，请稍后再试\"}");
            return false;
        }
        
        // 记录请求
        rateLimitService.recordRequest(clientIp);
        
        // 添加限流信息到响应头
        int remaining = rateLimitService.getRemainingRequests(clientIp);
        response.setHeader("X-RateLimit-Limit", "60");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        
        return true;
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

