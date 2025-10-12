package com.xy.verfiy.dto;

public class RedeemRequest {
    private String code;
    private String machine;
    private String payload;

    public RedeemRequest() {
    }

    public RedeemRequest(String code, String machine, String payload) {
        this.code = code;
        this.machine = machine;
        this.payload = payload;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}






