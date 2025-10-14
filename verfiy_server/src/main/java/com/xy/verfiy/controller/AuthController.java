package com.xy.verfiy.controller;

import com.xy.verfiy.domain.UserAccount;
import com.xy.verfiy.service.InviteCodeService;
import com.xy.verfiy.service.LoginAttemptService;
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
    private final LoginAttemptService loginAttemptService;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, InviteCodeService inviteCodeService, LoginAttemptService loginAttemptService) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.inviteCodeService = inviteCodeService;
        this.loginAttemptService = loginAttemptService;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body, 
                                                       HttpServletRequest request, 
                                                       HttpServletResponse response) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        Map<String, Object> resp = new HashMap<>();
        
        // 检查是否被锁定
        if (loginAttemptService.isBlocked(username)) {
            long remainingMinutes = loginAttemptService.getLockRemainingMinutes(username);
            resp.put("success", false);
            resp.put("message", "账号已被锁定，请在 " + remainingMinutes + " 分钟后重试");
            return ResponseEntity.status(429).body(resp);
        }
        
        try {
            Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
            
            // 登录成功，清除失败记录
            loginAttemptService.loginSucceeded(username);
            
            // 创建安全上下文并保存到 session
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            
            resp.put("success", true);
            resp.put("username", username);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            // 登录失败，记录失败次数
            loginAttemptService.loginFailed(username);
            
            int remainingAttempts = loginAttemptService.getRemainingAttempts(username);
            resp.put("success", false);
            if (remainingAttempts > 0) {
                resp.put("message", "用户名或密码错误，还有 " + remainingAttempts + " 次尝试机会");
            } else {
                resp.put("message", "登录失败次数过多，账号已被锁定15分钟");
            }
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
        
        // 密码强度验证
        String passwordError = validatePasswordStrength(password);
        if (passwordError != null) {
            resp.put("success", false);
            resp.put("message", passwordError);
            return ResponseEntity.badRequest().body(resp);
        }
        
        // 用户名格式验证
        if (username.length() < 3 || username.length() > 20) {
            resp.put("success", false);
            resp.put("message", "用户名长度必须在3-20个字符之间");
            return ResponseEntity.badRequest().body(resp);
        }
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            resp.put("success", false);
            resp.put("message", "用户名只能包含字母、数字和下划线");
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
        
        // 先查询邀请码信息（包含权限和配额）
        com.xy.verfiy.domain.InviteCode inviteCode = inviteCodeService.listAll().stream()
                .filter(ic -> ic.getCode().equals(invite) && !Boolean.TRUE.equals(ic.getUsed()))
                .findFirst()
                .orElse(null);
        
        if (inviteCode == null) {
            resp.put("success", false);
            resp.put("message", "邀请码无效或已被使用");
            return ResponseEntity.badRequest().body(resp);
        }
        
        // 验证并核销邀请码
        boolean ok = inviteCodeService.validateAndUse(invite, username);
        if (!ok) {
            resp.put("success", false);
            resp.put("message", "邀请码无效或已被使用");
            return ResponseEntity.badRequest().body(resp);
        }
        
        // 创建用户并设置邀请码中定义的权限和配额
        UserAccount ua = new UserAccount();
        ua.setUsername(username);
        ua.setPassword(passwordEncoder.encode(password));
        ua.setEnabled(true);
        ua.setCanInvite(inviteCode.getCanInvite() != null ? inviteCode.getCanInvite() : false);
        ua.setInviteQuota(inviteCode.getInviteQuota() != null ? inviteCode.getInviteQuota() : 0);
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
    
    /**
     * 密码强度验证
     * @return 错误信息，如果密码合法则返回 null
     */
    private String validatePasswordStrength(String password) {
        if (password.length() < 8) {
            return "密码长度至少8个字符";
        }
        if (password.length() > 64) {
            return "密码长度不能超过64个字符";
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        
        int strength = 0;
        if (hasUpper) strength++;
        if (hasLower) strength++;
        if (hasDigit) strength++;
        
        if (strength < 2) {
            return "密码必须包含大写字母、小写字母、数字中的至少两种";
        }
        
        // 检查常见弱密码
        String lowerPassword = password.toLowerCase();
        String[] weakPasswords = {"password", "12345678", "qwertyui", "admin123", "123456789"};
        for (String weak : weakPasswords) {
            if (lowerPassword.contains(weak)) {
                return "密码过于简单，请使用更强的密码";
            }
        }
        
        return null;  // 密码合法
    }
}


