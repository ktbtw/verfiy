package com.xy.verfiy.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CsrfController {

    /**
     * 显式获取并返回 CSRF Token。访问此接口会触发服务端生成并下发 XSRF-TOKEN Cookie。
     */
    @GetMapping("/api/csrf-token")
    public Map<String, Object> getCsrfToken(CsrfToken csrfToken) {
        Map<String, Object> resp = new HashMap<>();
        if (csrfToken != null) {
            resp.put("token", csrfToken.getToken());
            resp.put("headerName", csrfToken.getHeaderName());
            resp.put("parameterName", csrfToken.getParameterName());
        }
        return resp;
    }
}


