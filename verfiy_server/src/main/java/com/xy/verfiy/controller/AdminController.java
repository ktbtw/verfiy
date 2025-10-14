package com.xy.verfiy.controller;

import com.xy.verfiy.domain.Card;
import com.xy.verfiy.domain.CardStatus;
import com.xy.verfiy.domain.Application;
import com.xy.verfiy.domain.UserAccount;
import com.xy.verfiy.service.ApplicationService;
import com.xy.verfiy.mapper.CardMapper;
import com.xy.verfiy.mapper.UserAccountMapper;
import com.xy.verfiy.service.CardService;
import com.xy.verfiy.service.InviteCodeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final CardService cardService;
    private final ApplicationService applicationService;
    private final CardMapper cardMapper;
    private final InviteCodeService inviteCodeService;
    private final UserAccountMapper userAccountMapper;

    public AdminController(CardService cardService, ApplicationService applicationService, CardMapper cardMapper, InviteCodeService inviteCodeService, UserAccountMapper userAccountMapper) {
        this.cardService = cardService;
        this.applicationService = applicationService;
        this.cardMapper = cardMapper;
        this.inviteCodeService = inviteCodeService;
        this.userAccountMapper = userAccountMapper;
    }

    @GetMapping({"/cards","/app/cards"})
    public Map<String, Object> list(@RequestParam(value = "appId", required = false) Long appId,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       @RequestParam(value = "status", required = false) CardStatus status,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "size", defaultValue = "20") int size,
                       Authentication authentication,
                       HttpSession session) {
        Long effectiveAppId = appId != null ? appId : (Long) session.getAttribute("currentAppId");
        Map<String, Object> out = new HashMap<>();
        if (effectiveAppId == null) {
            out.put("cards", List.of());
            out.put("total", 0);
            return out;
        }
        // 所有权校验
        Application ownedApp = applicationService.findById(effectiveAppId);
        String owner = authentication != null ? authentication.getName() : "admin";
        if (ownedApp == null || ownedApp.getOwner() == null || !ownedApp.getOwner().equals(owner)) {
            session.removeAttribute("currentAppId");
            out.put("cards", List.of());
            out.put("total", 0);
            return out;
        }
        List<Card> data = cardService.page(effectiveAppId, keyword, status, page, size);
        long total = cardService.count(effectiveAppId, keyword, status);
        
        // 将 Card 转换为 Map 并添加 boundMachinesCount
        List<Map<String, Object>> cardsWithCount = data.stream().map(card -> {
            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("id", card.getId());
            cardMap.put("appId", card.getAppId());
            cardMap.put("cardCode", card.getCardCode());
            cardMap.put("status", card.getStatus());
            cardMap.put("expireAt", card.getExpireAt());
            cardMap.put("activatedAt", card.getActivatedAt());
            cardMap.put("disabled", card.isDisabled());
            cardMap.put("metadata", card.getMetadata());
            cardMap.put("createdAt", card.getCreatedAt());
            cardMap.put("updatedAt", card.getUpdatedAt());
            cardMap.put("maxMachines", card.getMaxMachines());
            cardMap.put("extra", card.getExtra());
            cardMap.put("returnExtra", card.getReturnExtra());
            // 添加当前绑定的机器码数量
            cardMap.put("boundMachinesCount", cardMapper.listMachines(card.getId()).size());
            return cardMap;
        }).toList();
        
        out.put("cards", cardsWithCount);
        out.put("total", total);
        out.put("page", page);
        out.put("size", size);
        out.put("keyword", keyword);
        out.put("status", status);
        return out;
    }

    @GetMapping("/cards/{id}/machines")
    @ResponseBody
    public java.util.List<String> listMachines(@PathVariable Long id,
                                               @RequestParam(value = "appId", required = false) Long appId,
                                               Authentication authentication,
                                               HttpSession session) {
        Long effectiveAppId = appId != null ? appId : (Long) session.getAttribute("currentAppId");
        Card card = cardMapper.findById(id);
        if (card == null) return java.util.List.of();
        if (card.getAppId() != null && effectiveAppId != null && !card.getAppId().equals(effectiveAppId)) {
            return java.util.List.of();
        }
        return cardMapper.listMachines(id);
    }

    @PostMapping("/cards/generate")
    public Map<String, Object> generate(@RequestParam(value = "appId", required = false) Long appId,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String prefix,
                           @RequestParam(defaultValue = "10") int count,
                           @RequestParam(required = false, name = "expireValue") Integer expireValue,
                           @RequestParam(required = false, name = "expireUnit") String expireUnit,
                           @RequestParam(required = false, name = "formatPattern") String formatPattern,
                           @RequestParam(required = false, name = "suffix") String suffix,
                           @RequestParam(required = false, name = "maxMachines") Integer maxMachines,
                           Authentication authentication,
                           HttpSession session) {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) { resp.put("success", false); return resp; }
        // 不再使用批次：直接生成卡密
        cardService.generateCards(resolved, prefix, count, expireValue, expireUnit, formatPattern, suffix, maxMachines);
        resp.put("success", true);
        return resp;
    }

    @PostMapping("/cards/import")
    public Map<String, Object> importCsv(@RequestParam(value = "appId", required = false) Long appId,
                            @RequestParam("file") MultipartFile file,
                            Authentication authentication,
                            HttpSession session) throws IOException {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) { resp.put("success", false); return resp; }
        // 不再使用批次：导入的卡密不绑定批次
        cardService.importCardsCsv(resolved, file.getInputStream());
        resp.put("success", true);
        return resp;
    }

    @GetMapping("/cards/export")
    public void exportCsv(@RequestParam(value = "appId", required = false) Long appId,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "status", required = false) CardStatus status,
                          HttpServletResponse response,
                          Authentication authentication,
                          HttpSession session) throws IOException {
        Long resolved = resolveAppId(appId, session, authentication);
        if (resolved == null) { response.sendRedirect("/admin/cards"); return; }
        byte[] csv = cardService.exportCardsCsv(resolved, keyword, status);
        response.setContentType("text/csv;charset=UTF-8");
        String filename = URLEncoder.encode("cards.csv", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        response.getOutputStream().write(csv);
    }

    // ======= 邀请码管理 =======
    @PostMapping("/invites/generate")
    public Map<String, Object> generateInvite(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "用户不存在");
            return resp;
        }
        
        // 检查邀请权限
        if (user.getCanInvite() == null || !user.getCanInvite()) {
            resp.put("success", false);
            resp.put("message", "无权限");
            return resp;
        }
        
        // 检查邀请配额（-1表示无限）
        if (user.getInviteQuota() != null && user.getInviteQuota() != -1) {
            int used = inviteCodeService.countByCreator(username);
            if (used >= user.getInviteQuota()) {
                resp.put("success", false);
                resp.put("message", "邀请配额已用尽");
                return resp;
            }
        }
        
        String code = inviteCodeService.generate(username);
        resp.put("success", true);
        resp.put("code", code);
        return resp;
    }

    @PostMapping("/invites/generate-batch")
    public Map<String, Object> generateInviteBatch(
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(required = false) Boolean canInvite,
            @RequestParam(required = false) Integer inviteQuota,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "用户不存在");
            return resp;
        }
        
        // 检查邀请权限
        if (user.getCanInvite() == null || !user.getCanInvite()) {
            resp.put("success", false);
            resp.put("message", "无权限");
            return resp;
        }
        
        // 限制批量生成数量
        if (count < 1 || count > 100) {
            resp.put("success", false);
            resp.put("message", "生成数量必须在1-100之间");
            return resp;
        }
        
        // 检查邀请配额（-1表示无限）
        if (user.getInviteQuota() != null && user.getInviteQuota() != -1) {
            int used = inviteCodeService.countByCreator(username);
            if (used + count > user.getInviteQuota()) {
                resp.put("success", false);
                resp.put("message", "邀请配额不足，当前已使用 " + used + "，剩余 " + (user.getInviteQuota() - used));
                return resp;
            }
        }
        
        // 批量生成邀请码
        List<String> codes = inviteCodeService.generateBatch(username, count, canInvite, inviteQuota);
        resp.put("success", true);
        resp.put("codes", codes);
        resp.put("count", codes.size());
        return resp;
    }

    @GetMapping("/invites")
    public Map<String, Object> listInvites(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "用户不存在");
            return resp;
        }
        
        // 检查邀请权限
        if (user.getCanInvite() == null || !user.getCanInvite()) {
            resp.put("success", false);
            resp.put("message", "无权限");
            return resp;
        }
        
        resp.put("success", true);
        resp.put("list", inviteCodeService.listByCreator(username));
        return resp;
    }

    @DeleteMapping("/invites/{code}")
    public Map<String, Object> deleteInvite(@PathVariable String code, Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "用户不存在");
            return resp;
        }
        
        // 检查邀请权限
        if (user.getCanInvite() == null || !user.getCanInvite()) {
            resp.put("success", false);
            resp.put("message", "无权限");
            return resp;
        }
        
        boolean ok = inviteCodeService.deleteByCode(code, username);
        resp.put("success", ok);
        if (!ok) resp.put("message", "删除失败：不存在/无权/已使用");
        return resp;
    }

    @PostMapping("/cards/{id}/disable")
    public Map<String, Object> disable(@PathVariable Long id, @RequestParam boolean disabled,
                          @RequestParam(value = "appId", required = false) Long appId,
                          Authentication authentication,
                          HttpSession session) {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) {
            resp.put("success", false);
            resp.put("message", "无效的应用");
            return resp;
        }
        
        // 验证卡密是否属于当前应用
        Card card = cardMapper.findById(id);
        if (card == null) {
            resp.put("success", false);
            resp.put("message", "卡密不存在");
            return resp;
        }
        if (!resolved.equals(card.getAppId())) {
            resp.put("success", false);
            resp.put("message", "卡密不属于当前应用");
            return resp;
        }
        
        try {
            boolean success = cardService.disable(id, disabled);
            if (success) {
                resp.put("success", true);
                resp.put("message", disabled ? "禁用成功" : "启用成功");
            } else {
                resp.put("success", false);
                resp.put("message", "操作失败");
            }
        } catch (Exception e) {
            resp.put("success", false);
            resp.put("message", "操作失败: " + e.getMessage());
        }
        return resp;
    }

    @PostMapping("/cards/{id}/extra")
    public Map<String, Object> updateExtra(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           @RequestParam(value = "appId", required = false) Long appId,
                                           Authentication authentication,
                                           HttpSession session) {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) {
            resp.put("success", false);
            resp.put("message", "无效的应用");
            return resp;
        }
        
        // 验证卡密是否属于当前应用
        Card card = cardMapper.findById(id);
        if (card == null || !resolved.equals(card.getAppId())) {
            resp.put("success", false);
            resp.put("message", "卡密不存在或不属于当前应用");
            return resp;
        }
        
        String extra = (String) body.get("extra");
        Boolean returnExtra = (Boolean) body.get("returnExtra");
        
        cardMapper.updateExtra(id, extra, returnExtra);
        resp.put("success", true);
        return resp;
    }

    @PostMapping("/cards/{id}/status")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestParam CardStatus status,
                               @RequestParam(value = "appId", required = false) Long appId,
                               Authentication authentication,
                               HttpSession session) {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) { resp.put("success", false); return resp; }
        cardService.updateStatus(id, status);
        resp.put("success", true);
        return resp;
    }

    @PostMapping("/cards/{id}/delete")
    public Map<String, Object> delete(@PathVariable Long id,
                         @RequestParam(value = "appId", required = false) Long appId,
                         Authentication authentication,
                         HttpSession session) {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) { resp.put("success", false); return resp; }
        cardService.delete(id, resolved);
        resp.put("success", true);
        return resp;
    }

    @PostMapping("/cards/{id}/unbind")
    public Map<String, Object> unbindMachine(@PathVariable Long id,
                                             @RequestParam String machineCode,
                                             @RequestParam(value = "appId", required = false) Long appId,
                                             Authentication authentication,
                                             HttpSession session) {
        Long resolved = resolveAppId(appId, session, authentication);
        Map<String, Object> resp = new HashMap<>();
        if (resolved == null) {
            resp.put("success", false);
            resp.put("message", "无效的应用");
            return resp;
        }
        
        // 验证卡密是否属于当前应用
        Card card = cardMapper.findById(id);
        if (card == null || !resolved.equals(card.getAppId())) {
            resp.put("success", false);
            resp.put("message", "卡密不存在或不属于当前应用");
            return resp;
        }
        
        // 删除指定的机器码
        int deleted = cardMapper.deleteMachine(id, machineCode);
        if (deleted > 0) {
            resp.put("success", true);
            resp.put("message", "解绑成功");
        } else {
            resp.put("success", false);
            resp.put("message", "机器码不存在");
        }
        return resp;
    }

    @DeleteMapping("/apps/{id}")
    public Map<String, Object> deleteApp(@PathVariable Long id,
                                         Authentication authentication,
                                         HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        String owner = authentication != null ? authentication.getName() : "admin";
        Application app = applicationService.findById(id);
        
        // 检查应用是否存在
        if (app == null) {
            resp.put("success", false);
            resp.put("message", "应用不存在");
            return resp;
        }
        
        // 检查所有权
        if (!owner.equals(app.getOwner())) {
            resp.put("success", false);
            resp.put("message", "无权删除此应用");
            return resp;
        }
        
        // 级联删除应用及其所有卡密
        boolean deleted = applicationService.deleteCascade(id);
        
        // 如果当前会话选中的是这个应用，清除会话
        Long currentAppId = (Long) session.getAttribute("currentAppId");
        if (currentAppId != null && currentAppId.equals(id)) {
            session.removeAttribute("currentAppId");
        }
        
        resp.put("success", deleted);
        if (!deleted) {
            resp.put("message", "删除失败");
        }
        return resp;
    }

    private Long resolveAppId(Long appId, HttpSession session, Authentication authentication) {
        Long candidate = appId != null ? appId : (Long) session.getAttribute("currentAppId");
        if (candidate == null) return null;
        Application app = applicationService.findById(candidate);
        String owner = authentication != null ? authentication.getName() : "admin";
        if (app == null || app.getOwner() == null || !owner.equals(app.getOwner())) {
            session.removeAttribute("currentAppId");
            return null;
        }
        return candidate;
    }

    // ========== 用户管理接口 ==========

    @GetMapping("/users")
    public Map<String, Object> listUsers(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        if (!"admin".equals(authentication.getName())) {
            resp.put("success", false);
            resp.put("message", "无权限");
            return resp;
        }
        
        List<UserAccount> users = userAccountMapper.findAll();
        // 不返回密码字段
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("enabled", user.getEnabled());
            userMap.put("createdAt", user.getCreatedAt());
            userMap.put("canInvite", user.getCanInvite() != null ? user.getCanInvite() : false);
            userMap.put("inviteQuota", user.getInviteQuota() != null ? user.getInviteQuota() : 0);
            // 统计已生成的邀请码数量
            int usedQuota = inviteCodeService.countByCreator(user.getUsername());
            userMap.put("usedQuota", usedQuota);
            return userMap;
        }).collect(Collectors.toList());
        
        resp.put("success", true);
        resp.put("users", userList);
        return resp;
    }

    @PostMapping("/users/{username}/invite-permission")
    public Map<String, Object> updateInvitePermission(
            @PathVariable String username,
            @RequestParam Boolean canInvite,
            @RequestParam Integer inviteQuota,
            Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        if (!"admin".equals(authentication.getName())) {
            resp.put("success", false);
            resp.put("message", "无权限");
            return resp;
        }
        
        // 不能修改 admin 自己的权限
        if ("admin".equals(username)) {
            resp.put("success", false);
            resp.put("message", "不能修改 admin 的权限");
            return resp;
        }
        
        int updated = userAccountMapper.updateInvitePermission(username, canInvite, inviteQuota);
        resp.put("success", updated > 0);
        if (updated == 0) {
            resp.put("message", "用户不存在");
        }
        return resp;
    }

    @GetMapping("/profile")
    public Map<String, Object> getProfile(Authentication authentication) {
        Map<String, Object> resp = new HashMap<>();
        if (authentication == null) {
            resp.put("success", false);
            resp.put("message", "未登录");
            return resp;
        }
        
        String username = authentication.getName();
        UserAccount user = userAccountMapper.findByUsername(username);
        
        if (user == null) {
            resp.put("success", false);
            resp.put("message", "用户不存在");
            return resp;
        }
        
        // 统计信息
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", user.getUsername());
        profile.put("createdAt", user.getCreatedAt());
        profile.put("canInvite", user.getCanInvite() != null ? user.getCanInvite() : false);
        profile.put("inviteQuota", user.getInviteQuota() != null ? user.getInviteQuota() : 0);
        
        // 统计应用数量
        int appCount = applicationService.countByOwner(username);
        profile.put("appCount", appCount);
        
        // 统计卡密数量（所有应用的卡密总数）
        int cardCount = cardService.countByOwner(username);
        profile.put("cardCount", cardCount);
        
        // 统计已生成的邀请码数量
        int usedInviteQuota = inviteCodeService.countByCreator(username);
        profile.put("usedInviteQuota", usedInviteQuota);
        
        // 计算剩余配额
        int remainingQuota = user.getInviteQuota() != null ? 
            (user.getInviteQuota() == -1 ? -1 : user.getInviteQuota() - usedInviteQuota) : 0;
        profile.put("remainingQuota", remainingQuota);
        
        resp.put("success", true);
        resp.put("profile", profile);
        return resp;
    }
}


