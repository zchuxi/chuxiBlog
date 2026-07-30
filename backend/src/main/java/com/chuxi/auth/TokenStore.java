package com.chuxi.auth;

import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 基于 site_content 表的持久化 token 存储 */
@Component
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);

    private static final String TOKEN_KEY_PREFIX = "auth-token-";
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

    /** 从 Authorization: Bearer xxx 头解析用户名，无效或过期返回 null */
    @Transactional
    public String resolveBearer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        String token = authorization.substring(7).trim();
        if (token.isEmpty()) return null;

        String key = TOKEN_KEY_PREFIX + token;
        return siteContentRepo.findByContentKey(key)
                .map(sc -> {
                    try {
                        Map<String, Object> data = mapper.readValue(sc.getContentJson(), Map.class);
                        String username = (String) data.get("username");
                        String expiresAt = (String) data.get("expiresAt");
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
                Map<String, Object> data = mapper.readValue(sc.getContentJson(), Map.class);
                String expiresAt = (String) data.get("expiresAt");
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
