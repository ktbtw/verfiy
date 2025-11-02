package com.xy.verfiy.service.impl;

import com.xy.verfiy.domain.Application;
import com.xy.verfiy.domain.Card;
import com.xy.verfiy.domain.CardStatus;
import com.xy.verfiy.domain.CardUseLog;
import com.xy.verfiy.mapper.CardMapper;
import com.xy.verfiy.mapper.CardUseLogMapper;
import com.xy.verfiy.service.ApplicationService;
import com.xy.verfiy.service.CardService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class CardServiceImpl implements CardService {

    private final CardMapper cardMapper;
    private final CardUseLogMapper logMapper;
    private final ApplicationService applicationService;

    public CardServiceImpl(CardMapper cardMapper, CardUseLogMapper logMapper, ApplicationService applicationService) {
        this.cardMapper = cardMapper;
        this.logMapper = logMapper;
        this.applicationService = applicationService;
    }

    @Override
    public List<Card> page(Long appId, String keyword, CardStatus status, int page, int size) {
        int offset = Math.max(0, (page - 1) * size);
        return cardMapper.pageQuery(appId, keyword, status, offset, size);
    }

    @Override
    public long count(Long appId, String keyword, CardStatus status) {
        return cardMapper.count(appId, keyword, status);
    }

    // 批次功能已移除

    @Override
    @Transactional
    public int generateCards(Long appId, String prefix, int count, Integer expireValue,
                             String expireUnit, String formatPattern, String suffix, Integer maxMachines) {
        List<Card> toInsert = new ArrayList<>(count);
        Random random = new Random();
        // 规则变更：到期时间 = 激活时间 + 拥有时长
        // 因此生成时不再预先写入 expireAt，而是把拥有时长写入 metadata，激活时再计算
        String metadataTemplate = null;
        try {
            if (expireUnit != null && !"".equals(expireUnit)) {
                ObjectMapper om = new ObjectMapper();
                ObjectNode node = om.createObjectNode();
                if (expireValue != null) node.put("expireValue", expireValue);
                node.put("expireUnit", expireUnit);
                metadataTemplate = om.writeValueAsString(node);
            }
        } catch (Exception ignored) {}
        for (int i = 0; i < count; i++) {
            String core = generateFromPattern(random, formatPattern);
            String code = joinWithHyphen(prefix, core, suffix);
            Card card = new Card();
            card.setAppId(appId);
            card.setCardCode(code);
            card.setStatus(CardStatus.NEW);
            card.setExpireAt(null); // 激活后再设置
            card.setDisabled(false);
            card.setMaxMachines(maxMachines);
            if (metadataTemplate != null) {
                card.setMetadata(metadataTemplate);
            }
            toInsert.add(card);
        }
        return cardMapper.insertBatch(toInsert);
    }

    private String randomCode(Random random, int len) {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateFromPattern(Random random, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return randomCode(random, 16);
        }
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(pattern.length());
        for (int i = 0; i < pattern.length(); i++) {
            char ch = pattern.charAt(i);
            if (ch == 'x' || ch == 'X') {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private String joinWithHyphen(String prefix, String core, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isBlank()) {
            sb.append(prefix.trim());
        }
        if (sb.length() > 0) sb.append('-');
        sb.append(core);
        if (suffix != null && !suffix.isBlank()) {
            sb.append('-').append(suffix.trim());
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public int importCardsCsv(Long appId, InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
                    .parse(reader);
            List<Card> toInsert = new ArrayList<>();
            for (CSVRecord record : records) {
                String code = record.get("code");
                Card card = new Card();
                card.setAppId(appId);
                card.setCardCode(code);
                card.setStatus(CardStatus.NEW);
                toInsert.add(card);
            }
            if (toInsert.isEmpty()) return 0;
            return cardMapper.insertBatch(toInsert);
        } catch (IOException e) {
            throw new RuntimeException("导入失败", e);
        }
    }

    @Override
    public byte[] exportCardsCsv(Long appId, String keyword, CardStatus status) {
        List<Card> cards = cardMapper.pageQuery(appId, keyword, status, 0, 10000);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVPrinter printer = new CSVPrinter(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8),
                     CSVFormat.Builder.create(CSVFormat.DEFAULT)
                             .setHeader("id", "code", "status", "disabled", "expireAt")
                             .build())) {
            for (Card c : cards) {
                printer.printRecord(c.getId(), c.getCardCode(), c.getStatus(), c.isDisabled(), c.getExpireAt());
            }
            printer.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    @Override
    @Transactional
    public boolean disable(Long id, boolean disabled) {
        return cardMapper.updateDisabled(id, disabled, LocalDateTime.now()) > 0;
    }

    @Override
    @Transactional
    public boolean updateStatus(Long id, CardStatus status) {
        // 确保外部不会将状态置为 USED；若传入 USED，则落为 ACTIVATED
        CardStatus safe = (status == CardStatus.USED) ? CardStatus.ACTIVATED : status;
        return cardMapper.updateStatus(id, safe, LocalDateTime.now()) > 0;
    }

    @Override
    @Transactional
    public boolean redeem(String code, String operator, String ip) {
        Card card = cardMapper.findByCode(code);
        if (card == null || card.isDisabled()) return false;
        if (card.getExpireAt() != null && card.getExpireAt().isBefore(LocalDateTime.now())) {
            cardMapper.updateStatus(card.getId(), CardStatus.EXPIRED, LocalDateTime.now());
            return false;
        }
        if (card.getStatus() == CardStatus.USED) return false;
        LocalDateTime now = LocalDateTime.now();
        cardMapper.updateStatusAndActivatedAtIfNull(card.getId(), CardStatus.USED, now, now);

        CardUseLog log = new CardUseLog();
        log.setCardId(card.getId());
        log.setCardCode(card.getCardCode());
        log.setAction("REDEEM");
        log.setRequestIp(ip);
        log.setRequestUser(operator);
        log.setRemark("核销成功");
        logMapper.insert(log);
        return true;
    }

    @Override
    @Transactional
    public boolean redeemForApp(String code, Long appId, String operator, String ip) {
        Card card = cardMapper.findByCode(code);
        if (card == null || card.isDisabled()) return false;
        if (card.getAppId() != null && appId != null && !card.getAppId().equals(appId)) {
            // 非当前应用的卡密，拒绝核销
            return false;
        }
        if (card.getExpireAt() != null && card.getExpireAt().isBefore(LocalDateTime.now())) {
            cardMapper.updateStatus(card.getId(), CardStatus.EXPIRED, LocalDateTime.now());
            return false;
        }
        if (card.getStatus() == CardStatus.USED) return false;
        LocalDateTime now = LocalDateTime.now();
        cardMapper.updateStatusAndActivatedAtIfNull(card.getId(), CardStatus.USED, now, now);

        CardUseLog log = new CardUseLog();
        log.setCardId(card.getId());
        log.setCardCode(card.getCardCode());
        log.setAction("REDEEM");
        log.setRequestIp(ip);
        log.setRequestUser(operator);
        log.setRemark("核销成功");
        logMapper.insert(log);
        return true;
    }

    @Override
    public Card getByCode(String code) {
        return cardMapper.findByCode(code);
    }

    @Override
    @Transactional
    public boolean verifyForApp(String code, Long appId, String machine) {
        Card current = cardMapper.findByCode(code);
        if (current == null || current.isDisabled()) return false;
        if (current.getAppId() != null && appId != null && !current.getAppId().equals(appId)) return false;
        if (current.getExpireAt() != null && current.getExpireAt().isBefore(LocalDateTime.now())) return false;
        
        // 核销时必须提供机器码
        if (machine == null || machine.isBlank()) {
            return false;
        }
        
        boolean isFirstActivation = (current.getStatus() == null || current.getStatus() == CardStatus.NEW);
        Integer max = current.getMaxMachines();
        boolean hasLimit = (max != null && max > 0);

        // 若配置了机器码限制：检查是否超出限制
        Integer exists = null;
        if (hasLimit) {
            exists = cardMapper.existsMachine(current.getId(), machine);
            if (exists == null) exists = 0;
            if (exists == 0) {
                int cnt = cardMapper.countMachines(current.getId());
                if (cnt >= max) return false;
            }
        }

        // 首次成功验证时，记为已激活，并写入激活时间与到期时间（到期=激活时间+拥有时长）
        if (isFirstActivation) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expireAt = null;
            try {
                // 从 metadata 解析拥有时长
                if (current.getMetadata() != null && !current.getMetadata().isBlank()) {
                    ObjectMapper om = new ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = om.readTree(current.getMetadata());
                    com.fasterxml.jackson.databind.JsonNode vNode = root.get("expireValue");
                    com.fasterxml.jackson.databind.JsonNode uNode = root.get("expireUnit");
                    Integer v = (vNode != null && vNode.isNumber()) ? vNode.intValue() : null;
                    String u = (uNode != null && !uNode.isNull()) ? uNode.asText() : null;
                    if (v != null && v > 0 && u != null) {
                        switch (u) {
                            case "MINUTES": expireAt = now.plusMinutes(v); break;
                            case "HOURS": expireAt = now.plusHours(v); break;
                            case "DAYS": expireAt = now.plusDays(v); break;
                            case "MONTHS": expireAt = now.plusMonths(v); break;
                            case "QUARTERS": expireAt = now.plusMonths((long)v * 3L); break;
                            case "YEARS": expireAt = now.plusYears(v); break;
                            case "FOREVER": expireAt = null; break;
                            default: expireAt = now.plusDays(v); break;
                        }
                    }
                }
            } catch (Exception ignored) {}
            try { cardMapper.updateStatusActivatedAndExpireIfNull(current.getId(), CardStatus.ACTIVATED, now, expireAt, now); } catch (Exception ignored) {}
        }

        // 处理机器码绑定（无限制时可忽略；有限制时上面已做预检）
        if (machine != null && !machine.isBlank()) {
            if (exists == null) {
                exists = cardMapper.existsMachine(current.getId(), machine);
                if (exists == null) exists = 0;
            }
            if (exists == 0) {
                if (hasLimit) {
                    int cnt = cardMapper.countMachines(current.getId());
                    if (cnt >= max) return false;
                }
                try { cardMapper.insertMachine(current.getId(), machine); } catch (Exception ignored) {}
            }
        }
        return true;
    }

    @Override
    @Transactional
    public boolean delete(Long id, Long appId) {
        // 先删使用日志与机器码绑定，避免外键约束与残留
        logMapper.deleteByCardId(id);
        try { cardMapper.deleteMachinesByCardId(id); } catch (Exception ignored) {}
        return cardMapper.deleteByIdAndAppId(id, appId) > 0;
    }
    
    @Override
    public int countByOwner(String owner) {
        // 获取用户所有应用
        List<Application> apps = applicationService.listByOwner(owner);
        if (apps == null || apps.isEmpty()) {
            return 0;
        }
        
        // 统计所有应用的卡密总数
        int total = 0;
        for (Application app : apps) {
            total += (int) cardMapper.count(app.getId(), null, null);
        }
        return total;
    }

    @Override
    public boolean existsVerifiedMachineForApp(Long appId, String machine) {
        if (appId == null || machine == null) {
            return false;
        }
        String trimmed = machine.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        Integer exists = cardMapper.existsMachineForApp(appId, trimmed);
        return exists != null && exists > 0;
    }
}


