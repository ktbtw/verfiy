package com.xy.verfiy.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF Token Filter
 * 确保每个请求都会生成并设置 CSRF Token 到 Cookie
 */
public class CsrfTokenFilter extends OncePerRequestFilter {

    private final CsrfTokenRepository tokenRepository;

    public CsrfTokenFilter(CsrfTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        // 加载或生成 CSRF Token
        CsrfToken csrfToken = tokenRepository.loadToken(request);
        if (csrfToken == null) {
            csrfToken = tokenRepository.generateToken(request);
            tokenRepository.saveToken(csrfToken, request, response);
        }
        
        // 设置到 request attribute，供后续使用
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        request.setAttribute(csrfToken.getParameterName(), csrfToken);
        
        filterChain.doFilter(request, response);
    }
}

