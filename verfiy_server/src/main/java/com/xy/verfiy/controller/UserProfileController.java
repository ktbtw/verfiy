package com.xy.verfiy.controller;

import com.xy.verfiy.domain.UserAccount;
import com.xy.verfiy.mapper.UserAccountMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/user")
public class UserProfileController {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    public UserProfileController(UserAccountMapper userAccountMapper, PasswordEncoder passwordEncoder) {
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 修改当前用户密码
     */
    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestBody Map<String, String> body, Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        
        // 参数验证
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            resp.put("success", false);
            resp.put("message", "请输入当前密码");
            return resp;
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            resp.put("success", false);
            resp.put("message", "请输入新密码");
            return resp;
        }
        
        if (newPassword.length() < 8) {
            resp.put("success", false);
            resp.put("message", "新密码长度不能少于8位");
            return resp;
        }
        
        // 获取当前用户
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "用户不存在");
            return resp;
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            resp.put("success", false);
            resp.put("message", "当前密码错误");
            return resp;
        }
        
        // 旧密码与新密码不能相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            resp.put("success", false);
            resp.put("message", "新密码不能与当前密码相同");
            return resp;
        }
        
        // 更新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        userAccountMapper.updatePassword(username, encodedPassword);
        
        resp.put("success", true);
        resp.put("message", "密码修改成功");
        return resp;
    }
}

