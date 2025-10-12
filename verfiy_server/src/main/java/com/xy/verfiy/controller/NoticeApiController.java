package com.xy.verfiy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.verfiy.domain.Application;
import com.xy.verfiy.service.ApplicationService;
import com.xy.verfiy.util.CryptoUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notice")
public class NoticeApiController {

    private final ApplicationService applicationService;
    private final ObjectMapper mapper = new ObjectMapper();

    public NoticeApiController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getNotice(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                         @RequestHeader(value = "X-Timestamp", required = false) String ts,
                                                         @RequestHeader(value = "X-Sign", required = false) String sign) {
        if (apiKey == null) return unauthorized("缺少 X-API-Key");
        Application app = applicationService.findByApiKey(apiKey);
        if (app == null) return unauthorized("API Key 无效");
        if (ts == null) return makeResponse(app, null, unauthorized("缺少 X-Timestamp"));
        try {
            long reqTs = Long.parseLong(ts);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - reqTs) > 300) return makeResponse(app, null, unauthorized("请求已过期"));
        } catch (NumberFormatException e) {
            return makeResponse(app, null, unauthorized("时间戳格式错误"));
        }
        // 使用 secretKey 进行签名验证，如果没有配置则使用 apiKey
        String secret = app.getSecretKey() != null && !app.getSecretKey().isEmpty() ? app.getSecretKey() : apiKey;
        String expect = CryptoUtils.md5Hex(secret + ts);
        if (sign == null || !expect.equalsIgnoreCase(sign)) return makeResponse(app, secret, unauthorized("签名不合法"));

        Map<String, Object> plain = new HashMap<>();
        plain.put("success", true);
        plain.put("announcement", app.getAnnouncement());
        plain.put("version", app.getVersion());
        plain.put("changelog", app.getChangelog());

        // 统一返回：根据 app.getSecure() 决定是否加密
        return makeResponse(app, secret, ResponseEntity.ok(plain));
    }

    /**
     * 统一响应处理：如果应用开启了传输安全，则加密响应；否则直接返回原响应
     */
    private ResponseEntity<Map<String, Object>> makeResponse(Application app, String secret, ResponseEntity<Map<String, Object>> original) {
        if (app == null || !Boolean.TRUE.equals(app.getSecure())) {
            return original;
        }
        
        // secret 为 null 时使用 apiKey
        String key = secret != null ? secret : app.getApiKey();
        if (key == null) {
            return original; // 无密钥，直接返回
        }
        
        Map<String, Object> body = original.getBody();
        if (body == null) {
            return original;
        }
        
        String alg = app.getEncryptionAlg() == null ? "RC4" : app.getEncryptionAlg();
        long respTs = Instant.now().getEpochSecond();
        
        try {
            String json = mapper.writeValueAsString(body);
            String encPayload;
            if (alg.startsWith("AES")) {
                encPayload = CryptoUtils.encryptAesCbcToBase64(json, key);
            } else {
                encPayload = CryptoUtils.encryptRc4ToBase64(json, key);
            }
            String respSign = CryptoUtils.md5Hex(key + respTs + encPayload);
            Map<String, Object> secure = new HashMap<>();
            secure.put("timestamp", respTs);
            secure.put("payload", encPayload);
            secure.put("sign", respSign);
            secure.put("secure", true);
            return ResponseEntity.status(original.getStatusCode()).body(secure);
        } catch (Exception e) {
            e.printStackTrace();
            return original; // 加密失败，返回原响应
        }
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", msg);
        return ResponseEntity.status(401).body(m);
    }

    private ResponseEntity<Map<String, Object>> bad(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", msg);
        return ResponseEntity.badRequest().body(m);
    }
}


