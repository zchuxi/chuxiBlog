package com.chuxi.auth;

import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 基于 site_content 表的持久化 token 存储 */
@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);

    private static final String TOKEN_KEY_PREFIX = "auth-token-";
    public static final String COOKIE_NAME = "chuxi_admin_session";
    private static final int TOKEN_EXPIRE_DAYS = 7;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final SiteContentRepo siteContentRepo;
    private final ObjectMapper mapper;

    public TokenStore(SiteContentRepo siteContentRepo, ObjectMapper mapper) {
        this.siteContentRepo = siteContentRepo;
        this.mapper = mapper;
    }

    /** 登录成功后签发 token，存入 site_content 表 */
    @Transactional
    public String issue(String username) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        try {
            String key = TOKEN_KEY_PREFIX + token;
            String value = mapper.writeValueAsString(Map.of(
                    "username", username,
                    "issuedAt", now.format(DT_FMT),
                    "expiresAt", now.plusDays(TOKEN_EXPIRE_DAYS).format(DT_FMT)
            ));
            SiteContent sc = new SiteContent();
            sc.setContentKey(key);
            sc.setContentJson(value);
            sc.setUpdatedAt(now);
            siteContentRepo.save(sc);
            log.info("Token 签发成功：username={}, expiresAt={}", username, now.plusDays(TOKEN_EXPIRE_DAYS));
        } catch (Exception e) {
            log.error("Token 持久化失败：username={}", username, e);
        }
        return token;
    }

    /** 优先兼容 Bearer token，其次读取 HttpOnly Cookie。 */
    @Transactional
    public String resolveRequest(HttpServletRequest request) {
        String token = tokenFromRequest(request);
        return resolveToken(token);
    }

    public String tokenFromRequest(HttpServletRequest request) {
        if (request == null) return null;
        String bearer = bearerToken(request.getHeader("Authorization"));
        if (bearer != null) return bearer;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return java.util.Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    /** 从 Authorization: Bearer xxx 头解析用户名，无效或过期返回 null。 */
    @Transactional
    public String resolveBearer(String authorization) {
        return resolveToken(bearerToken(authorization));
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    @Transactional
    public String resolveToken(String token) {
        if (token == null || token.isBlank()) return null;

        String key = TOKEN_KEY_PREFIX + token;
        return siteContentRepo.findByContentKey(key)
                .map(sc -> {
                    try {
                        Map<?, ?> data = mapper.readValue(sc.getContentJson(), Map.class);
                        String username = Optional.ofNullable(data.get("username")).map(String::valueOf).orElse(null);
                        String expiresAt = Optional.ofNullable(data.get("expiresAt")).map(String::valueOf).orElse(null);
                        if (expiresAt != null && LocalDateTime.parse(expiresAt, DT_FMT).isBefore(LocalDateTime.now())) {
                            log.info("Token 已过期，自动清理：key={}", key);
                            siteContentRepo.deleteByContentKey(key);
                            return null;
                        }
                        return username;
                    } catch (Exception e) {
                        log.error("Token 记录解析失败：key={}", key, e);
                        return null;
                    }
                })
                .orElse(null);
    }

    /** 注销 token：从 site_content 表删除记录 */
    @Transactional
    public void invalidate(String token) {
        if (token == null || token.isBlank()) return;
        String key = TOKEN_KEY_PREFIX + token;
        siteContentRepo.deleteByContentKey(key);
        log.info("Token 已注销：key={}", key);
    }

    /** 清理所有过期 token（可被定时任务或手动调用） */
    @Transactional
    public void cleanExpiredTokens() {
        List<SiteContent> tokenRecords = siteContentRepo.findByContentKeyStartingWith(TOKEN_KEY_PREFIX);
        int cleaned = 0;
        for (SiteContent sc : tokenRecords) {
            try {
                Map<?, ?> data = mapper.readValue(sc.getContentJson(), Map.class);
                String expiresAt = Optional.ofNullable(data.get("expiresAt")).map(String::valueOf).orElse(null);
                if (expiresAt != null && LocalDateTime.parse(expiresAt, DT_FMT).isBefore(LocalDateTime.now())) {
                    siteContentRepo.delete(sc);
                    cleaned++;
                }
            } catch (Exception e) {
                log.warn("清理过期 token 时解析失败：key={}", sc.getContentKey(), e);
                siteContentRepo.delete(sc);
                cleaned++;
            }
        }
        if (cleaned > 0) {
            log.info("过期 token 清理完成：共清理 {} 条", cleaned);
        }
    }
}
