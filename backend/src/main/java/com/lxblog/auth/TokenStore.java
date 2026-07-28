package com.lxblog.auth;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 内存 token 存储：token -> username */
@Component
public class TokenStore {
    private final Map<String, String> tokens = new ConcurrentHashMap<>();

    /** 登录成功后签发 token */
    public String issue(String username) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, username);
        return token;
    }

    /** 从 Authorization: Bearer xxx 头解析用户名，无效返回 null */
    public String resolveBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : tokens.get(token);
    }
}
