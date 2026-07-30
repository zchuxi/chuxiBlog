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

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证路径行为检查（MockMvc，复用 src/test/resources 的 H2 内存库配置）：
 * 1. 库里没有密码记录时登录被拒绝（不再回退默认口令）；
 * 2. 修改密码时短于 8 位的新密码被拒绝；
 * 3. 正确凭据仍可正常登录换取 token。
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
    void changePasswordShorterThanEightRejected() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/auth/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("oldPassword", "123456", "newPassword", "1234567"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("新密码至少 8 位"));
    }

    @Test
    void loginWithCorrectCredentialsSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.displayName").value(AuthController.ADMIN_USERNAME));
    }

    /** 用默认账号登录换取管理 token */
    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }
}
