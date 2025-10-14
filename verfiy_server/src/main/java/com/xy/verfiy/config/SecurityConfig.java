package com.xy.verfiy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import com.xy.verfiy.mapper.UserAccountMapper;

@Configuration
public class SecurityConfig {

    private final UserAccountMapper userAccountMapper;

    public SecurityConfig(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // CSRF Token 处理器
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");
        
        http
            .csrf(csrf -> csrf
                // 使用 Cookie 存储 CSRF Token
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                // 仅对公共 API 禁用 CSRF（卡密验证、公告接口、认证接口）
                .ignoringRequestMatchers("/api/redeem/**", "/api/notice/**", "/api/auth/**", "/logout")
                // H2 控制台（仅开发环境）
                .ignoringRequestMatchers("/h2/**")
            )
            .authorizeHttpRequests(authorize -> authorize
                // 公开的认证接口
                .requestMatchers("/api/auth/**").permitAll()
                // 公开的 API 接口（卡密验证、公告）
                .requestMatchers("/api/redeem/**", "/api/notice/**").permitAll()
                // 静态资源和前端路由
                .requestMatchers("/", "/login", "/register", "/apps", "/cards").permitAll()
                .requestMatchers("/assets/**", "/vite.svg", "/*.js", "/*.css", "/*.html").permitAll()
                // 管理接口需要认证
                .requestMatchers("/admin/users/**").hasRole("ADMIN")  // 用户管理仅管理员
                .requestMatchers("/admin/invites/**").authenticated()  // 邀请码管理需要登录
                .requestMatchers("/admin/**").authenticated()  // 其他管理接口需要登录
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionFixation().changeSessionId()  // 防止会话固定攻击
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)  // 允许踢掉旧会话
            )
            .securityContext(context -> context
                .securityContextRepository(new HttpSessionSecurityContextRepository())
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .formLogin(AbstractHttpConfigurer::disable)
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"success\":false,\"message\":\"未登录\"}");
                    } else {
                        response.sendRedirect("/login");
                    }
                })
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())  // 只允许同源嵌入
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:"))
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))  // HSTS: 1年
            );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            var u = userAccountMapper.findByUsername(username);
            if (u == null || !Boolean.TRUE.equals(u.getEnabled())) throw new UsernameNotFoundException("用户不存在");
            
            // 根据用户名分配角色
            String role = "admin".equals(username) ? "ADMIN" : "USER";
            
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getUsername())
                    .password(u.getPassword())
                    .roles(role)
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

}


