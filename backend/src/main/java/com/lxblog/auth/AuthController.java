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
    public R<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if (!ADMIN_USERNAME.equals(username) || !currentPassword().equals(password)) {
            return R.fail("账号或密码错误");
        }
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
        if (!currentPassword().equals(oldPassword)) {
            return ResponseEntity.ok(R.fail("旧密码不正确"));
        }
        if (newPassword.isEmpty()) {
            return ResponseEntity.ok(R.fail("新密码不能为空"));
        }
        SiteContent sc = siteContentRepo.findByContentKey(PASSWORD_KEY).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(PASSWORD_KEY);
            return n;
        });
        try {
            sc.setContentJson(mapper.writeValueAsString(Map.of("password", newPassword)));
        } catch (Exception e) {
            return ResponseEntity.ok(R.fail("密码保存失败"));
        }
        sc.setUpdatedAt(LocalDateTime.now());
        siteContentRepo.save(sc);
        return ResponseEntity.ok(R.ok(null));
    }

    /** 当前管理密码：优先读库（site-content: admin-password），无记录用默认 123456 */
    private String currentPassword() {
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
}
