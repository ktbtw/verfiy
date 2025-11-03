package com.xy.verfiy.domain;

import java.time.LocalDateTime;

public class Application {
    private Long id;
    private String name;
    private String description;
    private String owner; // username
    private String icon; // URL or base64
    private Boolean secure; // 是否加密传输
    private String encryptionAlg; // 加密算法：NONE/RC4/AES-128-CBC/AES-256-CBC
    private String apiKey; // 调用鉴权标识（放在 X-API-Key）
    private String secretKey; // 签名/加密密钥
    private String announcement; // 全局公告（该应用）
    private String version;
    private String changelog;
    private String updateUrl; // 更新下载链接
    private LocalDateTime createdAt;
    // 自定义核销成功返回的参数(JSON)
    private String redeemExtra;
    // 自定义返回参数的合并时机：ALWAYS / SUCCESS_ONLY / FAILURE_ONLY
    private String redeemExtraMode;
    // 应用类型：NORMAL / XPOSED
    private String appType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Boolean getSecure() { return secure; }
    public void setSecure(Boolean secure) { this.secure = secure; }
    public String getEncryptionAlg() { return encryptionAlg; }
    public void setEncryptionAlg(String encryptionAlg) { this.encryptionAlg = encryptionAlg; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getAnnouncement() { return announcement; }
    public void setAnnouncement(String announcement) { this.announcement = announcement; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getChangelog() { return changelog; }
    public void setChangelog(String changelog) { this.changelog = changelog; }
    public String getUpdateUrl() { return updateUrl; }
    public void setUpdateUrl(String updateUrl) { this.updateUrl = updateUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getRedeemExtra() { return redeemExtra; }
    public void setRedeemExtra(String redeemExtra) { this.redeemExtra = redeemExtra; }
    public String getRedeemExtraMode() { return redeemExtraMode; }
    public void setRedeemExtraMode(String redeemExtraMode) { this.redeemExtraMode = redeemExtraMode; }
    public String getAppType() { return appType; }
    public void setAppType(String appType) { this.appType = appType; }
}


