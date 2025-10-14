package com.xy.verfiy.controller;

import com.xy.verfiy.domain.Application;
import com.xy.verfiy.dto.RedeemRequest;
import com.xy.verfiy.service.ApplicationService;
import com.xy.verfiy.service.CardService;
import com.xy.verfiy.util.CryptoUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/redeem")
public class RedeemApiController {

    private final CardService cardService;
    private final ApplicationService applicationService;

    public RedeemApiController(CardService cardService, ApplicationService applicationService) {
        this.cardService = cardService;
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> redeem(@RequestHeader(value = "X-API-Key", required = false) String apiKey,
                                                      @RequestHeader(value = "X-Timestamp", required = false) String ts,
                                                      @RequestHeader(value = "X-Sign", required = false) String sign,
                                                      @RequestBody(required = false) RedeemRequest bodyRequest,
                                                      @RequestParam(value = "code", required = false) String queryCode,
                                                      @RequestParam(value = "machine", required = false) String queryMachine,
                                                      @RequestParam(value = "payload", required = false) String queryPayload,
                                                      HttpServletRequest request) {
        // 优先使用 body 参数，如果没有则使用查询参数（向后兼容）
        String code = null;
        String machine = null;
        String payload = null;
        
        if (bodyRequest != null) {
            code = bodyRequest.getCode();
            machine = bodyRequest.getMachine();
            payload = bodyRequest.getPayload();
        }
        
        // 如果 body 为空，尝试从查询参数获取（向后兼容）
        if (code == null) code = queryCode;
        if (machine == null) machine = queryMachine;
        if (payload == null) payload = queryPayload;
        
        Map<String, Object> body = new HashMap<>();
        if (apiKey == null) return unauthorized("缺少 X-API-Key");
        Application app = applicationService.findByApiKey(apiKey);
        if (app == null) return unauthorized("API Key 无效");
        // 校验时间戳（防重放）
        if (ts == null) return makeResponse(app, null, unauthorized("缺少 X-Timestamp"));
        try {
            long reqTs = Long.parseLong(ts);
            long now = Instant.now().getEpochSecond();
            // 缩短防重放窗口到60秒（更安全）
            if (Math.abs(now - reqTs) > 60) return makeResponse(app, null, unauthorized("请求已过期"));
        } catch (NumberFormatException e) {
            return makeResponse(app, null, unauthorized("时间戳格式错误"));
        }
        // 取待签名原文（优先 payload）
        String toSign = payload != null && !payload.isEmpty() ? payload : (code == null ? "" : code);
        // 使用 secretKey 进行签名验证，如果没有配置则使用 apiKey
        String secret = app.getSecretKey() != null && !app.getSecretKey().isEmpty() ? app.getSecretKey() : apiKey;
        String expect = CryptoUtils.md5Hex(secret + ts + toSign);
        
        if (sign == null || !expect.equalsIgnoreCase(sign)) return makeResponse(app, secret, unauthorized("签名不合法"));

        String finalCode = code;
        String finalMachine = machine;
        
        if (Boolean.TRUE.equals(app.getSecure())) {
            // 安全模式：必须提供加密的 payload
            if (payload == null || payload.isEmpty()) {
                return makeResponse(app, secret, bad("缺少加密负载 payload"));
            }
            
            String alg = app.getEncryptionAlg() == null ? "RC4" : app.getEncryptionAlg();
            try {
                String decrypted;
                if (alg.startsWith("AES")) {
                    decrypted = CryptoUtils.decryptAesCbcBase64(payload, secret);
                } else { // RC4
                    decrypted = CryptoUtils.decryptRc4Base64(payload, secret);
                }
                
                // 解密后的数据必须是 JSON 格式：{"code":"xxx","machine":"yyy"}
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, String> decryptedData = mapper.readValue(decrypted, Map.class);
                
                finalCode = decryptedData.get("code");
                finalMachine = decryptedData.get("machine");
                
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                return makeResponse(app, secret, bad("payload 格式错误，必须是加密的 JSON 对象"));
            } catch (Exception e) {
                return makeResponse(app, secret, bad("解密失败"));
            }
        }
        
        if (finalCode == null || finalCode.isEmpty()) {
            return makeResponse(app, secret, bad("code 不能为空"));
        }

        // 多次验证：不改变状态为 USED，仅进行校验、激活与机器码处理
        // 先获取卡密信息，用于详细判断
        com.xy.verfiy.domain.Card card = cardService.getByCode(finalCode);
        
        // 详细判断并返回清晰的错误信息
        if (card == null) {
            body.put("success", false);
            body.put("message", "卡密不存在");
            body.put("code", 1001);
        } else if (card.isDisabled()) {
            body.put("success", false);
            body.put("message", "卡密已被禁用");
            body.put("code", 1002);
        } else if (card.getAppId() != null && app.getId() != null && !card.getAppId().equals(app.getId())) {
            body.put("success", false);
            body.put("message", "卡密不属于当前应用");
            body.put("code", 1003);
        } else if (card.getExpireAt() != null && card.getExpireAt().isBefore(LocalDateTime.now())) {
            body.put("success", false);
            body.put("message", "卡密已过期");
            body.put("code", 1004);
            if (card.getExpireAt() != null) {
                long expireTs = card.getExpireAt().atZone(ZoneId.systemDefault()).toEpochSecond();
                body.put("expiredAt", expireTs);
            }
        } else {
            // 卡密验证通过
        boolean ok = cardService.verifyForApp(finalCode, app.getId(), finalMachine);
            
            if (ok) {
                // 重新获取卡密信息（因为激活后 expireAt 可能已更新）
                card = cardService.getByCode(finalCode);
                
                body.put("success", true);
                body.put("message", "核销成功");
                body.put("code", 0);
                
                // 返回到期时间（使用时间戳，永久有效返回 2099-12-12 的时间戳）
                if (card.getExpireAt() != null) {
                    long expireTs = card.getExpireAt().atZone(ZoneId.systemDefault()).toEpochSecond();
                    body.put("expireAt", expireTs);
                    body.put("expireAtReadable", card.getExpireAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else {
                    // 永久有效：2099-12-12 00:00:00 的时间戳
                    LocalDateTime farFuture = LocalDateTime.of(2099, 12, 12, 0, 0, 0);
                    long farFutureTs = farFuture.atZone(ZoneId.systemDefault()).toEpochSecond();
                    body.put("expireAt", farFutureTs);
                    body.put("expireAtReadable", "永久有效");
                }
                
                // 返回附加信息（如果配置了返回）
                if (Boolean.TRUE.equals(card.getReturnExtra()) && card.getExtra() != null && !card.getExtra().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> extraData = mapper.readValue(card.getExtra(), Map.class);
                        if (extraData != null) {
                            // 将附加信息合并到返回体中（但不覆盖核心字段）
                            for (Map.Entry<String, Object> entry : extraData.entrySet()) {
                                String key = entry.getKey();
                                // 保护核心字段
                                if (!"success".equalsIgnoreCase(key) && 
                                    !"code".equalsIgnoreCase(key) && 
                                    !"message".equalsIgnoreCase(key) && 
                                    !"expireAt".equalsIgnoreCase(key) &&
                                    !"expireAtReadable".equalsIgnoreCase(key)) {
                                    body.put(key, entry.getValue());
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // JSON 解析失败，忽略
                    }
                }
            } else {
                // 验证失败（可能是机器码限制等其他原因）
                body.put("success", false);
                if (finalMachine == null || finalMachine.isBlank()) {
                    body.put("message", "机器码不能为空");
                    body.put("code", 1006);
                } else {
                    body.put("message", "核销失败，可能是机器码已达上限或其他限制");
                    body.put("code", 1005);
                }
            }
        }
        
        boolean ok = body.get("success") == Boolean.TRUE;
        // 合并自定义返回参数（按应用配置的合并时机）
        if (app.getRedeemExtra() != null && !app.getRedeemExtra().isEmpty()) {
            try {
                // 解析结构：{"k":{"value":...,"mode":"ALWAYS|SUCCESS_ONLY|FAILURE_ONLY"}} 或 旧格式 {"k":"v"}
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> extra = mapper.readValue(app.getRedeemExtra(), java.util.Map.class);
                if (extra != null) {
                    long nowTs = Instant.now().getEpochSecond();
                    long nowMs = Instant.now().toEpochMilli();
                    String date = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String dateTime = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    String iso8601 = Instant.now().toString();
                    String uuid = UUID.randomUUID().toString();
                    String nonce = randomAlphaNum(16);
                    // 计算卡密到期时间戳（使用已获取的 card 对象）
                    Long expireTs;
                    if (card != null && card.getExpireAt() != null) {
                        expireTs = card.getExpireAt().atZone(ZoneId.systemDefault()).toEpochSecond();
                    } else {
                        LocalDateTime ex = LocalDateTime.of(2099, 12, 12, 0, 0);
                        expireTs = ex.atZone(ZoneId.systemDefault()).toEpochSecond();
                    }
                    for (Map.Entry<String, Object> en : extra.entrySet()) {
                        String key = en.getKey();
                        Object val = en.getValue();
                        String mode;
                        Object valueToMerge;
                        if (val instanceof Map<?,?> m) {
                            Object mv = m.get("value");
                            Object mm = m.get("mode");
                            mode = mm == null ? "SUCCESS_ONLY" : String.valueOf(mm);
                            valueToMerge = mv;
                        } else {
                            // 向后兼容：旧格式
                            mode = app.getRedeemExtraMode() == null ? "SUCCESS_ONLY" : app.getRedeemExtraMode();
                            valueToMerge = val;
                        }
                        if (shouldMergeExtra(mode, ok)) {
                            // 保护核心字段，不允许覆盖
                            if ("success".equalsIgnoreCase(key) || 
                                "code".equalsIgnoreCase(key) || 
                                "message".equalsIgnoreCase(key) || 
                                "expireAt".equalsIgnoreCase(key) ||
                                "expireAtReadable".equalsIgnoreCase(key)) {
                                continue; // 跳过核心字段
                            }
                            
                            if (valueToMerge instanceof String s) {
                                String r = s;
                                r = r.replace("${timestamp}", String.valueOf(nowTs));
                                r = r.replace("${millis}", String.valueOf(nowMs));
                                r = r.replace("${date}", date);
                                r = r.replace("${datetime}", dateTime);
                                r = r.replace("${iso8601}", iso8601);
                                r = r.replace("${uuid}", uuid);
                                r = r.replace("${nonce}", nonce);
                                r = r.replace("${expireTs}", expireTs == null ? "" : String.valueOf(expireTs));
                                body.put(key, r);
                            } else {
                                body.put(key, valueToMerge);
                            }
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
        // 统一返回：无论成功失败，都根据 app.getSecure() 决定是否加密
        return makeResponse(app, secret, ok ? ResponseEntity.ok(body) : ResponseEntity.badRequest().body(body));
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
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
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

    private boolean shouldMergeExtra(String mode, boolean success) {
        String m = (mode == null || mode.isEmpty()) ? "SUCCESS_ONLY" : mode.toUpperCase();
        return switch (m) {
            case "ALWAYS" -> true;
            case "FAILURE_ONLY" -> !success;
            default -> success; // SUCCESS_ONLY
        };
    }

    private String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            int idx = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(idx));
        }
        return sb.toString();
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


