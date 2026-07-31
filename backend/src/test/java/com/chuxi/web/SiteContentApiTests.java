package com.chuxi.web;

import com.chuxi.common.RateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 站点文案与浏览量接口测试：
 * 1. 前台读取白名单 key（如 site-settings）返回 200（code=0 或 code=400 取决于是否已配置）；
 * 2. 前台读取非白名单 key 返回 200 + code=400（"内容不存在"）；
 * 3. 管理端文案列表需登录，无 token 返回 401；
 * 4. 浏览量读取返回 200；
 * 5. 浏览量 bump 首次返回 200。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SiteContentApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void resetRateLimiter() {
        RateLimiter.reset();
    }

    @Test
    void frontRead_whitelistKey_returns200() throws Exception {
        // site-settings 在白名单中；无论是否已配置，HTTP 均为 200
        mockMvc.perform(get("/api/front/site-content/site-settings"))
                .andExpect(status().isOk());
    }

    @Test
    void frontRead_nonWhitelistKey_returnsCode400() throws Exception {
        // admin-password 不在白名单，返回 code=400（"内容不存在"），HTTP 仍为 200
        mockMvc.perform(get("/api/front/site-content/admin-password"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("内容不存在"));
    }

    @Test
    void frontRead_unknownKey_returnsCode400() throws Exception {
        // 完全未知的 key 同样返回 code=400
        mockMvc.perform(get("/api/front/site-content/some-unknown-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void adminList_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/site-content"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminList_withToken_returns200() throws Exception {
        String token = login();
        mockMvc.perform(get("/api/admin/site-content")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void viewsRead_returns200() throws Exception {
        mockMvc.perform(get("/api/front/views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.views").isNumber());
    }

    @Test
    void viewsBump_firstCall_returns200() throws Exception {
        mockMvc.perform(post("/api/front/views/bump"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.views").isNumber());
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
