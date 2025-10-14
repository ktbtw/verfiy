package com.xy.verfiy.domain;

import java.time.LocalDateTime;

public class InviteCode {
    private Long id;
    private String code;
    private String createdBy;
    private LocalDateTime createdAt;
    private Boolean used;
    private String usedBy;
    private LocalDateTime usedAt;
    private Boolean canInvite;  // 新用户是否有邀请权限
    private Integer inviteQuota; // 新用户的邀请配额（-1表示无限制）

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getUsed() { return used; }
    public void setUsed(Boolean used) { this.used = used; }

    public String getUsedBy() { return usedBy; }
    public void setUsedBy(String usedBy) { this.usedBy = usedBy; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }

    public Boolean getCanInvite() { return canInvite; }
    public void setCanInvite(Boolean canInvite) { this.canInvite = canInvite; }

    public Integer getInviteQuota() { return inviteQuota; }
    public void setInviteQuota(Integer inviteQuota) { this.inviteQuota = inviteQuota; }
}


