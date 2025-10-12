package com.xy.verfiy.controller;

import com.xy.verfiy.domain.UserAccount;
import com.xy.verfiy.service.InviteCodeService;
import com.xy.verfiy.mapper.UserAccountMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final InviteCodeService inviteCodeService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, InviteCodeService inviteCodeService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.inviteCodeService = inviteCodeService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, 
                                                       HttpServletRequest request, 
                                                       HttpServletResponse response) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            
            // 创建安全上下文并保存到 session
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", true);
            resp.put("username", username);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("success", false);
            resp.put("message", "用户名或密码错误");
            return ResponseEntity.status(401).body(resp);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        String invite = body.getOrDefault("inviteCode", "");
        Map<String, Object> resp = new HashMap<>();
        if (username.isBlank() || password.isBlank()) {
            resp.put("success", false);
            resp.put("message", "用户名或密码不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        // 校验邀请码
        if (invite.isBlank()) {
            resp.put("success", false);
            resp.put("message", "邀请码不能为空");
            return ResponseEntity.badRequest().body(resp);
        }
        if (userAccountMapper.findByUsername(username) != null) {
            resp.put("success", false);
            resp.put("message", "用户名已存在");
            return ResponseEntity.badRequest().body(resp);
        }
        // 验证并核销邀请码
        boolean ok = inviteCodeService.validateAndUse(invite, username);
        if (!ok) {
            resp.put("success", false);
            resp.put("message", "邀请码无效或已被使用");
            return ResponseEntity.badRequest().body(resp);
        }
        UserAccount ua = new UserAccount();
        ua.setUsername(username);
        ua.setPassword(passwordEncoder.encode(password));
        ua.setEnabled(true);
        userAccountMapper.insert(ua);
        resp.put("success", true);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("authenticated", false);
            return ResponseEntity.ok(resp);
        }
        resp.put("authenticated", true);
        resp.put("username", authentication.getName());
        return ResponseEntity.ok(resp);
    }
}


