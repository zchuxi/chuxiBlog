package com.chuxi.auth;

import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证路径行为检查（MockMvc，复用 src/test/resources 的 H2 内存库配置）：
 * 1. 库里没有密码记录时登录被拒绝（不再回退默认口令）；
 * 2. 修改密码时短于 16 位的新密码被拒绝；
 * 3. 正确凭据仍可正常登录并取得 HttpOnly 管理 Cookie。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SiteContentRepo siteContentRepo;

    @Test
    void loginWithoutStoredPasswordRejected() throws Exception {
        // 暂时删掉密码记录，模拟"密码未初始化"的库状态
        SiteContent original = siteContentRepo.findByContentKey(AuthController.PASSWORD_KEY)
                .orElseThrow(() -> new IllegalStateException("测试种子数据缺少 admin-password 记录"));
        String contentJson = original.getContentJson();
        LocalDateTime updatedAt = original.getUpdatedAt();
        siteContentRepo.delete(original);
        try {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "123456"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("管理员密码未设置"));
        } finally {
            // 按 upsert 还原密码记录（若登录意外成功会重建记录），避免影响同一上下文里的其他测试
            SiteContent restored = siteContentRepo.findByContentKey(AuthController.PASSWORD_KEY)
                    .orElseGet(() -> {
                        SiteContent n = new SiteContent();
                        n.setContentKey(AuthController.PASSWORD_KEY);
                        return n;
                    });
            restored.setContentJson(contentJson);
            restored.setUpdatedAt(updatedAt);
            siteContentRepo.save(restored);
        }
    }

    @Test
    void changePasswordShorterThanSixteenRejected() throws Exception {
        Cookie session = login();
        mockMvc.perform(post("/api/auth/password")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("oldPassword", "123456", "newPassword", "1234567890abcde"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("新密码至少 16 位"));
    }

    @Test
    void loginWithCorrectCredentialsSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.displayName").value(AuthController.ADMIN_USERNAME));
    }

    @Test
    void loginCookieAuthenticatesAndLogoutInvalidatesSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andReturn();
        String setCookie = loginResult.getResponse().getHeader("Set-Cookie");
        org.assertj.core.api.Assertions.assertThat(setCookie)
                .contains(TokenStore.COOKIE_NAME + "=")
                .contains("HttpOnly")
                .contains("SameSite=Strict")
                .contains("Path=/api");

        Cookie session = loginResult.getResponse().getCookie(TokenStore.COOKIE_NAME);
        org.assertj.core.api.Assertions.assertThat(session).isNotNull();
        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(AuthController.ADMIN_USERNAME));

        mockMvc.perform(post("/api/auth/logout").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/auth/me").cookie(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** 用默认账号登录换取 HttpOnly 管理 Cookie。 */
    private Cookie login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return result.getResponse().getCookie(TokenStore.COOKIE_NAME);
    }
}
