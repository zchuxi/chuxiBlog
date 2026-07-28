package com.chuxi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 外链抓取白名单（防 SSRF）分支检查：用假的 OSS 配置让 available() 为真，
 * 白名单外 URL 在触碰任何网络 / OSS 客户端之前就被拒绝，因此不需要真实凭证。
 * 假配置仅通过本类 properties 注入，不改动共享的 src/test/resources/application.yml。
 */
@SpringBootTest(properties = {
        "aliyun.oss.enabled=true",
        "aliyun.oss.endpoint=oss-cn-test.example.com",
        "aliyun.oss.bucket=test-bucket",
        "aliyun.oss.access-key-id=test-fake-ak",
        "aliyun.oss.access-key-secret=test-fake-sk"
})
@AutoConfigureMockMvc
class MediaFetchWhitelistTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void fetchRejectsUrlOutsideOssWhitelist() throws Exception {
        String token = login();

        // 白名单主机为 https://test-bucket.oss-cn-test.example.com，内网地址必须被拒绝
        mockMvc.perform(post("/api/admin/media/fetch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("url", "http://169.254.169.254/latest/meta-data"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.message").value("仅支持本站 OSS 公网地址"));
    }

    @Test
    void fetchRejectsWhitelistHostPrefixTrick() throws Exception {
        String token = login();

        // 前缀伪装（白名单域作为恶意域子串）同样必须被拒绝
        mockMvc.perform(post("/api/admin/media/fetch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "url", "https://test-bucket.oss-cn-test.example.com.evil.com/x.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.message").value("仅支持本站 OSS 公网地址"));
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
