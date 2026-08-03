package com.chuxi.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chuxi.common.ClientIpResolver;
import com.chuxi.common.R;
import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    static final String ADMIN_USERNAME = "admin";
    static final String PASSWORD_KEY = "admin-password";

    private final TokenStore tokenStore;
    private final SiteContentRepo siteContentRepo;
    private final ObjectMapper mapper;
    private final ClientIpResolver clientIpResolver;

    public AuthController(TokenStore tokenStore, SiteContentRepo siteContentRepo, ObjectMapper mapper, ClientIpResolver clientIpResolver) {
        this.tokenStore = tokenStore;
        this.siteContentRepo = siteContentRepo;
        this.mapper = mapper;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    @Transactional
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body,
                                        jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response) {
        String ip = clientIpResolver.resolve(request);
        if (isLocked(ip)) {
            log.warn("登录请求被限流拒绝：ip={}, uri={}", ip, request.getRequestURI());
            return R.fail("失败次数过多，请 " + LOCK_MINUTES + " 分钟后再试");
        }
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        String stored = storedPassword();
        if (stored == null) {
            log.error("管理员密码未设置，请先初始化密码");
            return R.fail("管理员密码未设置");
        }
        if (!ADMIN_USERNAME.equals(username) || !PasswordHasher.matches(password, stored)) {
            recordFailure(ip);
            // 只记用户名与来源 IP，不落口令
            log.warn("登录鉴权失败：ip={}, username={}, uri={}", ip, username, request.getRequestURI());
            return R.fail("账号或密码错误");
        }
        FAILURES.remove(ip);
        // 首次用明文口令登录成功后，顺手把库里的密码升级为哈希
        upgradeToHashIfNeeded(password);
        String token = tokenStore.issue(username);
        response.addHeader("Set-Cookie", sessionCookie(token, request, Duration.ofDays(7)).toString());
        return R.ok(Map.of("displayName", ADMIN_USERNAME));
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(jakarta.servlet.http.HttpServletRequest request) {
        String username = tokenStore.resolveRequest(request);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        return ResponseEntity.ok(R.ok(Map.of("username", username, "displayName", username)));
    }

    /** 修改密码：需管理 token；旧密码错误返回 code 1 */
    @PostMapping("/password")
    @Transactional
    public ResponseEntity<Object> changePassword(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestBody Map<String, String> body) {
        if (tokenStore.resolveRequest(request) == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        String oldPassword = body.getOrDefault("oldPassword", "");
        String newPassword = body.getOrDefault("newPassword", "").trim();
        if (!PasswordHasher.matches(oldPassword, storedPassword())) {
            return ResponseEntity.ok(R.fail("旧密码不正确"));
        }
        if (newPassword.length() < 8) {
            return ResponseEntity.ok(R.fail("新密码至少 8 位"));
        }
        SiteContent sc = siteContentRepo.findByContentKey(PASSWORD_KEY).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(PASSWORD_KEY);
            return n;
        });
        try {
            // 只存哈希，绝不落明文
            sc.setContentJson(mapper.writeValueAsString(Map.of("password", PasswordHasher.hash(newPassword))));
        } catch (Exception e) {
            log.error("修改密码时序列化失败：key={}", PASSWORD_KEY, e);
            return ResponseEntity.ok(R.fail("密码保存失败"));
        }
        sc.setUpdatedAt(LocalDateTime.now());
        siteContentRepo.save(sc);
        return ResponseEntity.ok(R.ok(null));
    }

    @PostMapping("/logout")
    public R<Object> logout(jakarta.servlet.http.HttpServletRequest request,
                            jakarta.servlet.http.HttpServletResponse response) {
        tokenStore.invalidate(tokenStore.tokenFromRequest(request));
        response.addHeader("Set-Cookie", sessionCookie("", request, Duration.ZERO).toString());
        return R.ok(null);
    }

    private ResponseCookie sessionCookie(String token, jakarta.servlet.http.HttpServletRequest request, Duration maxAge) {
        boolean secure = request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        return ResponseCookie.from(TokenStore.COOKIE_NAME, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path("/api")
                .maxAge(maxAge)
                .build();
    }

    /** 库里存的密码串（可能是哈希，也可能是历史明文），无记录返回 null */
    private String storedPassword() {
        return siteContentRepo.findByContentKey(PASSWORD_KEY)
                .map(sc -> {
                    try {
                        String pwd = mapper.readTree(sc.getContentJson()).path("password").asText("");
                        return pwd.isEmpty() ? null : pwd;
                    } catch (Exception e) {
                        log.error("解析存储密码 JSON 失败：key={}, err={}", PASSWORD_KEY, e.getMessage());
                        return null;
                    }
                })
                .orElse(null);
    }

    /** 历史明文密码在登录成功后自动升级为哈希 */
    private void upgradeToHashIfNeeded(String rawPassword) {
        String stored = storedPassword();
        if (PasswordHasher.isHashed(stored)) return;
        SiteContent sc = siteContentRepo.findByContentKey(PASSWORD_KEY).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(PASSWORD_KEY);
            return n;
        });
        try {
            sc.setContentJson(mapper.writeValueAsString(Map.of("password", PasswordHasher.hash(rawPassword))));
            sc.setUpdatedAt(LocalDateTime.now());
            siteContentRepo.save(sc);
        } catch (Exception e) {
            // 升级失败不影响本次登录
            log.warn("明文密码升级为哈希失败（不影响本次登录）：key={}", PASSWORD_KEY, e);
        }
    }

    /* ---------- 登录失败限流：同 IP 连续失败 5 次锁 15 分钟 ---------- */

    private static final int MAX_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;
    private static final java.util.Map<String, int[]> FAILURES = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Long> LOCKED_UNTIL = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean isLocked(String ip) {
        Long until = LOCKED_UNTIL.get(ip);
        if (until == null) return false;
        if (System.currentTimeMillis() < until) return true;
        LOCKED_UNTIL.remove(ip);
        FAILURES.remove(ip);
        return false;
    }

    private static void recordFailure(String ip) {
        int[] c = FAILURES.computeIfAbsent(ip, k -> new int[1]);
        if (++c[0] >= MAX_FAILURES) {
            LOCKED_UNTIL.put(ip, System.currentTimeMillis() + LOCK_MINUTES * 60_000L);
            FAILURES.remove(ip);
        }
    }

}
