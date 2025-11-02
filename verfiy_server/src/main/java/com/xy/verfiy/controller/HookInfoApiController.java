package com.xy.verfiy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.verfiy.domain.Application;
import com.xy.verfiy.domain.HookInfo;
import com.xy.verfiy.service.ApplicationService;
import com.xy.verfiy.service.CardService;
import com.xy.verfiy.service.HookInfoService;
import com.xy.verfiy.util.CryptoUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/hook")
public class HookInfoApiController {

    private final ApplicationService applicationService;
    private final HookInfoService hookInfoService;
    private final CardService cardService;
    private final ObjectMapper mapper = new ObjectMapper();

    public HookInfoApiController(ApplicationService applicationService,
                                 HookInfoService hookInfoService,
                                 CardService cardService) {
        this.applicationService = applicationService;
        this.hookInfoService = hookInfoService;
        this.cardService = cardService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHookInfo(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                           @RequestHeader(value = "X-Timestamp", required = false) String ts,
                                                           @RequestHeader(value = "X-Sign", required = false) String sign,
                                                           @RequestParam("packageName") String packageName,
                                                           @RequestParam(value = "version", required = false) String version,
                                                           @RequestParam(value = "deviceId", required = false) String deviceId) {
        if (apiKey == null || apiKey.isBlank()) {
            return unauthorized("缺少 X-API-Key");
        }
        Application app = applicationService.findByApiKey(apiKey);
        if (app == null) {
            return unauthorized("API Key 无效");
        }
        if (ts == null) {
            return makeResponse(app, null, unauthorized("缺少 X-Timestamp"));
        }
        long reqTs;
        try {
            reqTs = Long.parseLong(ts);
        } catch (NumberFormatException e) {
            return makeResponse(app, null, unauthorized("时间戳格式错误"));
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - reqTs) > 60) {
            return makeResponse(app, null, unauthorized("请求已过期"));
        }

        String secret = app.getSecretKey() != null && !app.getSecretKey().isEmpty() ? app.getSecretKey() : apiKey;
        String expectedSign = CryptoUtils.md5Hex(secret + ts);
        if (sign == null || !expectedSign.equalsIgnoreCase(sign)) {
            return makeResponse(app, secret, unauthorized("签名不合法"));
        }

        HookInfo info = hookInfoService.findEffective(app.getId(), packageName, version);
        if (info == null) {
            return makeResponse(app, secret, notFound("未找到 Hook 配置"));
        }
        if (info.getEnabled() != null && !info.getEnabled()) {
            return makeResponse(app, secret, notFound("Hook 已禁用"));
        }

        if (Boolean.TRUE.equals(info.getRequireCardVerification())) {
            if (deviceId == null || deviceId.isBlank()) {
                return makeResponse(app, secret, forbidden("缺少设备标识，无法确认卡密验证"));
            }
            if (!cardService.existsVerifiedMachineForApp(app.getId(), deviceId)) {
                return makeResponse(app, secret, forbidden("该设备未完成卡密验证"));
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("success", true);
        body.put("packageName", info.getPackageName());
        body.put("version", info.getVersion());
        body.put("data", info.getData());
        body.put("dexData", info.getDexData());
        body.put("zipData", info.getZipData());
        body.put("zipVersion", info.getZipVersion());
        body.put("updatedAt", info.getUpdatedAt());
        body.put("requireCardVerification", info.getRequireCardVerification());
        if (deviceId != null) {
            body.put("deviceId", deviceId);
        }
        return makeResponse(app, secret, ResponseEntity.ok(body));
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", msg);
        return ResponseEntity.status(401).body(result);
    }

    private ResponseEntity<Map<String, Object>> notFound(String msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", msg);
        return ResponseEntity.status(404).body(result);
    }

    private ResponseEntity<Map<String, Object>> forbidden(String msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", msg);
        return ResponseEntity.status(403).body(result);
    }

    private ResponseEntity<Map<String, Object>> makeResponse(Application app, String secret, ResponseEntity<Map<String, Object>> original) {
        if (app == null || !Boolean.TRUE.equals(app.getSecure())) {
            return original;
        }
        String key = (secret != null && !secret.isBlank()) ? secret : app.getApiKey();
        if (key == null || key.isBlank()) {
            return original;
        }
        Map<String, Object> body = original.getBody();
        if (body == null) {
            return original;
        }
        String alg = app.getEncryptionAlg() == null ? "RC4" : app.getEncryptionAlg();
        long respTs = Instant.now().getEpochSecond();
        try {
            String json = mapper.writeValueAsString(body);
            String payload;
            if (alg.startsWith("AES")) {
                payload = CryptoUtils.encryptAesCbcToBase64(json, key);
            } else {
                payload = CryptoUtils.encryptRc4ToBase64(json, key);
            }
            String respSign = CryptoUtils.md5Hex(key + respTs + payload);
            Map<String, Object> secure = new HashMap<>();
            secure.put("timestamp", respTs);
            secure.put("payload", payload);
            secure.put("sign", respSign);
            secure.put("secure", true);
            return ResponseEntity.status(original.getStatusCode()).body(secure);
        } catch (Exception e) {
            e.printStackTrace();
            return original;
        }
    }
}


