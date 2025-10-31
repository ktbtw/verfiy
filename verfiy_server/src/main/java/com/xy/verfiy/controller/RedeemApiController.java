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
        if (apiKey == null) return unauthorized(null, null, "缺少 X-API-Key", null);
        Application app = applicationService.findByApiKey(apiKey);
        if (app == null) return unauthorized(null, null, "API Key 无效", null);
        // 校验时间戳（防重放）
        if (ts == null) return unauthorized(app, null, "缺少 X-Timestamp", null);
        try {
            long reqTs = Long.parseLong(ts);
            long now = Instant.now().getEpochSecond();
            // 缩短防重放窗口到60秒（更安全）
            if (Math.abs(now - reqTs) > 60) return unauthorized(app, null, "请求已过期", null);
        } catch (NumberFormatException e) {
            return unauthorized(app, null, "时间戳格式错误", null);
        }
        // 取待签名原文（优先 payload）
        String toSign = payload != null && !payload.isEmpty() ? payload : (code == null ? "" : code);
        // 使用 secretKey 进行签名验证，如果没有配置则使用 apiKey
        String secret = app.getSecretKey() != null && !app.getSecretKey().isEmpty() ? app.getSecretKey() : apiKey;
        String expect = CryptoUtils.md5Hex(secret + ts + toSign);
        
        if (sign == null || !expect.equalsIgnoreCase(sign)) return unauthorized(app, secret, "签名不合法", null);

        String finalCode = code;
        String finalMachine = machine;
        
        if (Boolean.TRUE.equals(app.getSecure())) {
            // 安全模式：必须提供加密的 payload
            if (payload == null || payload.isEmpty()) {
                return bad(app, secret, "缺少加密负载 payload", null);
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
                return bad(app, secret, "payload 格式错误，必须是加密的 JSON 对象", null);
            } catch (Exception e) {
                return bad(app, secret, "解密失败", null);
            }
        }
        
        if (finalCode == null || finalCode.isEmpty()) {
            return bad(app, secret, "code 不能为空", null);
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
        return respond(app, secret, body, ok ? 200 : 400, card);
    }

    /**
     * 统一响应处理：如果应用开启了传输安全，则加密响应；否则直接返回原响应
     */
    private ResponseEntity<Map<String, Object>> respond(Application app,
                                                        String secret,
                                                        Map<String, Object> body,
                                                        int status,
                                                        com.xy.verfiy.domain.Card card) {
        boolean ok = body.get("success") == Boolean.TRUE;
        mergeRedeemExtra(app, body, ok, card);
        ResponseEntity<Map<String, Object>> original = ResponseEntity.status(status).body(body);
        return makeResponse(app, secret, original);
    }

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

    private void mergeRedeemExtra(Application app,
                                  Map<String, Object> body,
                                  boolean ok,
                                  com.xy.verfiy.domain.Card card) {
        if (app == null || app.getRedeemExtra() == null || app.getRedeemExtra().isEmpty()) {
            return;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> extra = mapper.readValue(app.getRedeemExtra(), java.util.Map.class);
            if (extra == null || extra.isEmpty()) {
                return;
            }
            long nowTs = Instant.now().getEpochSecond();
            long nowMs = Instant.now().toEpochMilli();
            String date = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String dateTime = LocalDateTime.now(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String iso8601 = Instant.now().toString();
            String uuid = UUID.randomUUID().toString();
            String nonce = randomAlphaNum(16);
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

                String groupMode = normalizeMode(key);
                if ("ALWAYS".equals(groupMode) || "SUCCESS_ONLY".equals(groupMode) || "FAILURE_ONLY".equals(groupMode)) {
                    if (!shouldMergeExtra(groupMode, ok)) {
                        continue;
                    }
                    if (val instanceof Map<?,?> groupMap) {
                        for (Map.Entry<?,?> entry : groupMap.entrySet()) {
                            String groupKey = entry.getKey() == null ? null : String.valueOf(entry.getKey());
                            Object groupValue = entry.getValue();
                            mergeCustomValue(body, groupKey, groupValue, nowTs, nowMs, date, dateTime, iso8601, uuid, nonce, expireTs);
                        }
                    } else {
                        mergeCustomValue(body, key, val, nowTs, nowMs, date, dateTime, iso8601, uuid, nonce, expireTs);
                    }
                    continue;
                }

                if (val instanceof Map<?,?> valueMap) {
                    if (valueMap.containsKey("value") || valueMap.containsKey("mode")) {
                        Object mv = valueMap.get("value");
                        Object mm = valueMap.get("mode");
                        if (shouldMergeExtra(mm == null ? null : String.valueOf(mm), ok)) {
                            mergeCustomValue(body, key, mv, nowTs, nowMs, date, dateTime, iso8601, uuid, nonce, expireTs);
                        }
                    } else {
                        if (shouldMergeExtra(app.getRedeemExtraMode(), ok)) {
                            mergeCustomValue(body, key, valueMap, nowTs, nowMs, date, dateTime, iso8601, uuid, nonce, expireTs);
                        }
                    }
                } else {
                    if (shouldMergeExtra(app.getRedeemExtraMode(), ok)) {
                        mergeCustomValue(body, key, val, nowTs, nowMs, date, dateTime, iso8601, uuid, nonce, expireTs);
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    private void mergeCustomValue(Map<String, Object> body,
                                  String key,
                                  Object value,
                                  long nowTs,
                                  long nowMs,
                                  String date,
                                  String dateTime,
                                  String iso8601,
                                  String uuid,
                                  String nonce,
                                  Long expireTs) {
        if (key == null || isCoreField(key)) {
            return;
        }
        Object resolved = applyPlaceholders(value, nowTs, nowMs, date, dateTime, iso8601, uuid, nonce, expireTs);
        body.put(key, resolved);
    }

    private Object applyPlaceholders(Object value,
                                     long nowTs,
                                     long nowMs,
                                     String date,
                                     String dateTime,
                                     String iso8601,
                                     String uuid,
                                     String nonce,
                                     Long expireTs) {
        if (!(value instanceof String s)) {
            return value;
        }
        String r = s;
        r = r.replace("${timestamp}", String.valueOf(nowTs));
        r = r.replace("${millis}", String.valueOf(nowMs));
        r = r.replace("${date}", date);
        r = r.replace("${datetime}", dateTime);
        r = r.replace("${iso8601}", iso8601);
        r = r.replace("${uuid}", uuid);
        r = r.replace("${nonce}", nonce);
        r = r.replace("${expireTs}", expireTs == null ? "" : String.valueOf(expireTs));
        return r;
    }

    private boolean isCoreField(String key) {
        return "success".equalsIgnoreCase(key) ||
               "code".equalsIgnoreCase(key) ||
               "message".equalsIgnoreCase(key) ||
               "expireAt".equalsIgnoreCase(key) ||
               "expireAtReadable".equalsIgnoreCase(key);
    }

    private String normalizeMode(String mode) {
        if (mode == null) {
            return "SUCCESS_ONLY";
        }
        String raw = mode.trim();
        if (raw.isEmpty()) {
            return "SUCCESS_ONLY";
        }
        String upper = raw.toUpperCase();
        if ("ALWAYS".equals(upper)) {
            return "ALWAYS";
        }
        if ("SUCCESS_ONLY".equals(upper) || "SUCCESS".equals(upper)) {
            return "SUCCESS_ONLY";
        }
        if ("FAILURE_ONLY".equals(upper) || "FAILURE".equals(upper)) {
            return "FAILURE_ONLY";
        }
        // 支持中文配置
        if ("总是".equals(raw) || "总是返回".equals(raw)) {
            return "ALWAYS";
        }
        if ("成功".equals(raw) || "仅成功".equals(raw) || "仅成功时返回".equals(raw)) {
            return "SUCCESS_ONLY";
        }
        if ("失败".equals(raw) || "仅失败".equals(raw) || "仅失败时返回".equals(raw)) {
            return "FAILURE_ONLY";
        }
        return upper;
    }

    private boolean shouldMergeExtra(String mode, boolean success) {
        String m = normalizeMode(mode);
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

    private ResponseEntity<Map<String, Object>> unauthorized(Application app,
                                                             String secret,
                                                             String msg,
                                                             com.xy.verfiy.domain.Card card) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", msg);
        return respond(app, secret, m, 401, card);
    }

    private ResponseEntity<Map<String, Object>> bad(Application app,
                                                    String secret,
                                                    String msg,
                                                    com.xy.verfiy.domain.Card card) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", false);
        m.put("message", msg);
        return respond(app, secret, m, 400, card);
    }
}


