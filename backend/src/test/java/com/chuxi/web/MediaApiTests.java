package com.chuxi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 媒体接口高危分支最小行为检查（复用 src/test/resources 的 H2 配置，OSS enabled=false）：
 * 1. 带 token 上传小图走本地回退分支，返回 /api/uploads/ 可访问路径，用后经删除接口清理；
 * 2. OSS 未配置时外链抓取接口直接拒绝，返回业务失败码而非 200+code 0。
 */
@SpringBootTest
@AutoConfigureMockMvc
class MediaApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void uploadFallsBackToLocalAndUrlIsServable() throws Exception {
        String token = login();

        MockMultipartFile file = new MockMultipartFile(
                "file", "tiny.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4});
        MvcResult uploaded = mockMvc.perform(multipart("/api/admin/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").isString())
                .andExpect(jsonPath("$.data.url").isString())
                .andReturn();

        var data = mapper.readTree(uploaded.getResponse().getContentAsString()).path("data");
        String name = data.path("name").asText();
        String url = data.path("url").asText();
        assertTrue(url.equals("/api/uploads/" + name), "本地回退分支应返回 /api/uploads/ 路径，实际：" + url);

        try {
            // 返回的路径必须真实可读（公开读取接口 200 且有内容）
            mockMvc.perform(get(url))
                    .andExpect(status().isOk());
        } finally {
            // 清理测试落盘文件，避免污染 uploads/ 目录
            mockMvc.perform(delete("/api/admin/media/" + name)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Test
    void fetchIsRejectedWhenOssNotConfigured() throws Exception {
        String token = login();

        mockMvc.perform(post("/api/admin/media/fetch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("url", "https://evil.example.com/x.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("OSS 未配置，暂不支持取回外链"));
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
