package com.xy.verfiy.controller;

import com.xy.verfiy.domain.Application;
import com.xy.verfiy.service.ApplicationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/apps")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public List<Application> list(Authentication authentication) {
        String owner = authentication != null ? authentication.getName() : "admin";
        return applicationService.listByOwner(owner);
    }

    @PostMapping
    public Application create(@RequestParam String name,
                              @RequestParam(required = false) String description,
                              Authentication authentication,
                              HttpSession session) {
        String owner = authentication != null ? authentication.getName() : "admin";
        Application app = applicationService.create(name, description, owner);
        session.setAttribute("currentAppId", app.getId());
        return app;
    }

    @PostMapping("/select")
    public void select(@RequestParam Long appId, HttpSession session) {
        session.setAttribute("currentAppId", appId);
    }

    @PostMapping("/delete")
    public void delete(@RequestParam Long id, Authentication authentication, HttpSession session) {
        applicationService.deleteCascade(id);
        Long current = (Long) session.getAttribute("currentAppId");
        if (current != null && current.equals(id)) {
            session.removeAttribute("currentAppId");
        }
    }

    @GetMapping("/api/my-apps")
    @ResponseBody
    public List<Application> myApps(Authentication authentication) {
        String owner = authentication != null ? authentication.getName() : "admin";
        return applicationService.listByOwner(owner);
    }
    
    @GetMapping("/api/current-app")
    @ResponseBody
    public Map<String, Object> currentApp(HttpSession session, Authentication authentication) {
        Long appId = (Long) session.getAttribute("currentAppId");
        Map<String, Object> result = new HashMap<>();
        
        if (appId == null) {
            result.put("id", null);
            result.put("name", null);
            return result;
        }
        
        Application app = applicationService.findById(appId);
        if (app == null) {
            session.removeAttribute("currentAppId");
            result.put("id", null);
            result.put("name", null);
            return result;
        }
        
        // 验证所有权
        String owner = authentication != null ? authentication.getName() : "admin";
        if (!owner.equals(app.getOwner())) {
            session.removeAttribute("currentAppId");
            result.put("id", null);
            result.put("name", null);
            return result;
        }
        
        result.put("id", app.getId());
        result.put("name", app.getName());
        result.put("description", app.getDescription());
        result.put("apiKey", app.getApiKey());
        result.put("secretKey", app.getSecretKey());
        result.put("secure", app.getSecure());
        result.put("encryptionAlg", app.getEncryptionAlg());
        result.put("announcement", app.getAnnouncement());
        result.put("version", app.getVersion());
        result.put("changelog", app.getChangelog());
        result.put("redeemExtra", app.getRedeemExtra());
        return result;
    }

    @GetMapping("/select")
    public void selectGet(@RequestParam Long appId,
                          @RequestParam(required = false) String to,
                          HttpSession session) {
        session.setAttribute("currentAppId", appId);
    }

    @PostMapping("/update")
    @ResponseBody
    public Map<String, Object> update(@RequestBody Application app, 
                                      Authentication authentication, 
                                      HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        
        // 验证应用是否存在
        Application existing = applicationService.findById(app.getId());
        if (existing == null) {
            result.put("success", false);
            result.put("message", "应用不存在");
            return result;
        }
        
        // 验证所有权
        String owner = authentication != null ? authentication.getName() : "admin";
        if (!owner.equals(existing.getOwner())) {
            result.put("success", false);
            result.put("message", "无权修改此应用");
            return result;
        }
        
        // 更新应用信息
        app.setOwner(owner); // 确保 owner 不被修改
        boolean success = applicationService.update(app);
        
        result.put("success", success);
        result.put("message", success ? "更新成功" : "更新失败");
        if (success) {
            result.put("app", applicationService.findById(app.getId()));
        }
        return result;
    }
    
    @GetMapping("/{id}")
    @ResponseBody
    public Application getById(@PathVariable Long id, Authentication authentication) {
        Application app = applicationService.findById(id);
        if (app == null) {
            return null;
        }
        
        // 验证所有权
        String owner = authentication != null ? authentication.getName() : "admin";
        if (!owner.equals(app.getOwner())) {
            return null;
        }
        
        return app;
    }
}


