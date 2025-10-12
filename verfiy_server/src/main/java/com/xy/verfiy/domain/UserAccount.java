package com.xy.verfiy.domain;

import java.time.LocalDateTime;

public class UserAccount {
    private Long id;
    private String username;
    private String password;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private Boolean canInvite;  // 是否有邀请权限
    private Integer inviteQuota; // 邀请配额（-1表示无限制）

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getCanInvite() { return canInvite; }
    public void setCanInvite(Boolean canInvite) { this.canInvite = canInvite; }
    public Integer getInviteQuota() { return inviteQuota; }
    public void setInviteQuota(Integer inviteQuota) { this.inviteQuota = inviteQuota; }
}


