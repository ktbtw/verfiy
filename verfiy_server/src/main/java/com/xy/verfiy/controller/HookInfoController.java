package com.xy.verfiy.controller;

import com.xy.verfiy.domain.HookInfo;
import com.xy.verfiy.service.HookInfoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/hook-info")
public class HookInfoController {

    private final HookInfoService hookInfoService;
    private static final long MAX_DEX_SIZE_BYTES = 6L * 1024 * 1024;  // 6 MB
    private static final long MAX_ZIP_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

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
        String owner = requireOwner(authentication);
        try {
            return hookInfoService.listByApp(targetAppId, owner);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, Authentication authentication) {
        try {
            HookInfo info = hookInfoService.getById(id, requireOwner(authentication));
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
            validateBinarySize(payload);
            String owner = requireOwner(authentication);
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
            String owner = requireOwner(authentication);
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
            String owner = requireOwner(authentication);
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

    private String requireOwner(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return authentication.getName();
    }

    private void validateBinarySize(HookInfo payload) {
        String dexData = payload.getDexData();
        if (dexData != null && !dexData.isBlank()) {
            long size = decodeBase64Size(dexData);
            if (size > MAX_DEX_SIZE_BYTES) {
                throw new IllegalArgumentException("Dex 数据超过 6MB 限制");
            }
        }

        String zipData = payload.getZipData();
        if (zipData != null && !zipData.isBlank()) {
            long size = decodeBase64Size(zipData);
            if (size > MAX_ZIP_SIZE_BYTES) {
                throw new IllegalArgumentException("Zip 数据超过 10MB 限制");
            }
        }
    }

    private long decodeBase64Size(String base64) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64.replaceAll("\\s", ""));
            return decoded.length;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("二进制数据不是有效的 Base64 编码", e);
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


