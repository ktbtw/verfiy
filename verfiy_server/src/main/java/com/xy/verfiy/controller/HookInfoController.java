package com.xy.verfiy.controller;

import com.xy.verfiy.domain.HookInfo;
import com.xy.verfiy.service.HookInfoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/hook-info")
public class HookInfoController {

    private final HookInfoService hookInfoService;

    public HookInfoController(HookInfoService hookInfoService) {
        this.hookInfoService = hookInfoService;
    }

    @GetMapping
    public List<HookInfo> list(@RequestParam(value = "appId", required = false) Long appId,
                               Authentication authentication,
                               HttpSession session) {
        Long targetAppId = resolveAppId(appId, session);
        if (targetAppId == null) {
            return List.of();
        }
        String owner = authentication != null ? authentication.getName() : "admin";
        try {
            return hookInfoService.listByApp(targetAppId, owner);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, Authentication authentication) {
        try {
            HookInfo info = hookInfoService.getById(id, authentication != null ? authentication.getName() : "admin");
            if (info == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> save(@RequestBody HookInfo payload,
                                                    Authentication authentication,
                                                    HttpSession session) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (payload.getAppId() == null) {
                Long sessionApp = resolveAppId(null, session);
                if (sessionApp == null) {
                    throw new IllegalArgumentException("appId 不能为空");
                }
                payload.setAppId(sessionApp);
            }
            String owner = authentication != null ? authentication.getName() : "admin";
            HookInfo saved = hookInfoService.saveOrUpdate(payload, owner);
            result.put("success", true);
            result.put("data", saved);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            result.put("success", false);
            result.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(@PathVariable Long id,
                                                            @RequestParam("enabled") boolean enabled,
                                                            Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        try {
            String owner = authentication != null ? authentication.getName() : "admin";
            boolean ok = hookInfoService.updateStatus(id, enabled, owner);
            result.put("success", ok);
            if (!ok) {
                result.put("message", "未找到 HookInfo");
                return ResponseEntity.status(404).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            result.put("success", false);
            result.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id,
                                                      Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        try {
            String owner = authentication != null ? authentication.getName() : "admin";
            boolean ok = hookInfoService.delete(id, owner);
            result.put("success", ok);
            if (!ok) {
                result.put("message", "未找到 HookInfo");
                return ResponseEntity.status(404).body(result);
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException ex) {
            result.put("success", false);
            result.put("message", ex.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    private Long resolveAppId(Long appId, HttpSession session) {
        if (appId != null) {
            return appId;
        }
        if (session == null) {
            return null;
        }
        Object sessionValue = session.getAttribute("currentAppId");
        if (sessionValue instanceof Long) {
            return (Long) sessionValue;
        }
        if (sessionValue instanceof Integer) {
            return ((Integer) sessionValue).longValue();
        }
        return null;
    }
}


