package com.xy.verfiy.domain;

import java.time.LocalDateTime;

public class Card {
    private Long id;
    private Long appId;
    private String cardCode;
    private CardStatus status;
    private LocalDateTime expireAt;
    private LocalDateTime activatedAt;
    private boolean disabled;
    private String metadata; // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer maxMachines; // 允许绑定的机器码数量（null/<=0 表示不限制）
    private String extra; // 附加信息（JSON 格式）
    private Boolean returnExtra; // 验证成功时是否返回附加信息

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppId() { return appId; }
    public void setAppId(Long appId) { this.appId = appId; }

    public String getCardCode() { return cardCode; }
    public void setCardCode(String cardCode) { this.cardCode = cardCode; }


    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }

    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }

    public LocalDateTime getActivatedAt() { return activatedAt; }
    public void setActivatedAt(LocalDateTime activatedAt) { this.activatedAt = activatedAt; }

    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getMaxMachines() { return maxMachines; }
    public void setMaxMachines(Integer maxMachines) { this.maxMachines = maxMachines; }

    public String getExtra() { return extra; }
    public void setExtra(String extra) { this.extra = extra; }

    public Boolean getReturnExtra() { return returnExtra; }
    public void setReturnExtra(Boolean returnExtra) { this.returnExtra = returnExtra; }
}


