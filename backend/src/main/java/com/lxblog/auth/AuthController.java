package com.lxblog.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lxblog.common.R;
import com.lxblog.entity.SiteContent;
import com.lxblog.repo.SiteContentRepo;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    static final String ADMIN_USERNAME = "admin";
    static final String DEFAULT_PASSWORD = "123456";
    static final String PASSWORD_KEY = "admin-password";

    private final TokenStore tokenStore;
    private final SiteContentRepo siteContentRepo;
    private final ObjectMapper mapper;

    public AuthController(TokenStore tokenStore, SiteContentRepo siteContentRepo, ObjectMapper mapper) {
        this.tokenStore = tokenStore;
        this.siteContentRepo = siteContentRepo;
        this.mapper = mapper;
    }

    @PostMapping("/login")
    @Transactional
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body,
                                        jakarta.servlet.http.HttpServletRequest request) {
        String ip = clientIp(request);
        if (isLocked(ip)) {
            return R.fail("失败次数过多，请 " + LOCK_MINUTES + " 分钟后再试");
        }
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if (!ADMIN_USERNAME.equals(username) || !PasswordHasher.matches(password, storedPassword())) {
            recordFailure(ip);
            return R.fail("账号或密码错误");
        }
        FAILURES.remove(ip);
        // 首次用明文口令登录成功后，顺手把库里的密码升级为哈希
        upgradeToHashIfNeeded(password);
        return R.ok(Map.of("token", tokenStore.issue(username), "displayName", ADMIN_USERNAME));
    }

    @GetMapping("/me")
    public ResponseEntity<Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String username = tokenStore.resolveBearer(authorization);
        if (username == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        return ResponseEntity.ok(R.ok(Map.of("username", username, "displayName", username)));
    }

    /** 修改密码：需管理 token；旧密码错误返回 code 1 */
    @PostMapping("/password")
    @Transactional
    public ResponseEntity<Object> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, String> body) {
        if (tokenStore.resolveBearer(authorization) == null) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "message", "未登录"));
        }
        String oldPassword = body.getOrDefault("oldPassword", "");
        String newPassword = body.getOrDefault("newPassword", "").trim();
        if (!PasswordHasher.matches(oldPassword, storedPassword())) {
            return ResponseEntity.ok(R.fail("旧密码不正确"));
        }
        if (newPassword.length() < 4) {
            return ResponseEntity.ok(R.fail("新密码至少 4 位"));
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
            return ResponseEntity.ok(R.fail("密码保存失败"));
        }
        sc.setUpdatedAt(LocalDateTime.now());
        siteContentRepo.save(sc);
        return ResponseEntity.ok(R.ok(null));
    }

    /** 库里存的密码串（可能是哈希，也可能是历史明文），无记录用默认口令 */
    private String storedPassword() {
        return siteContentRepo.findByContentKey(PASSWORD_KEY)
                .map(sc -> {
                    try {
                        String pwd = mapper.readTree(sc.getContentJson()).path("password").asText("");
                        return pwd.isEmpty() ? DEFAULT_PASSWORD : pwd;
                    } catch (Exception e) {
                        return DEFAULT_PASSWORD;
                    }
                })
                .orElse(DEFAULT_PASSWORD);
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
        } catch (Exception ignored) {
            // 升级失败不影响本次登录
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

    private static String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
