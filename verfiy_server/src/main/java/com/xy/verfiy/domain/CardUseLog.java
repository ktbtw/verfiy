package com.xy.verfiy.domain;

import java.time.LocalDateTime;

public class CardUseLog {
    private Long id;
    private Long cardId;
    private String cardCode;
    private String action;
    private String requestIp;
    private String requestUser;
    private String remark;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCardId() { return cardId; }
    public void setCardId(Long cardId) { this.cardId = cardId; }

    public String getCardCode() { return cardCode; }
    public void setCardCode(String cardCode) { this.cardCode = cardCode; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRequestIp() { return requestIp; }
    public void setRequestIp(String requestIp) { this.requestIp = requestIp; }

    public String getRequestUser() { return requestUser; }
    public void setRequestUser(String requestUser) { this.requestUser = requestUser; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


