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
import jakarta.servlet.http.Cookie;
import com.chuxi.auth.TokenStore;

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
 * 1. 带管理 Cookie 上传小图走本地回退分支，返回 /api/uploads/ 可访问路径，用后经删除接口清理；
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
        Cookie session = login();

        MockMultipartFile file = new MockMultipartFile(
                "file", "tiny.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4});
        MvcResult uploaded = mockMvc.perform(multipart("/api/admin/upload")
                        .file(file)
                        .cookie(session))
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
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }

    @Test
    void fetchIsRejectedWhenOssNotConfigured() throws Exception {
        Cookie session = login();

        mockMvc.perform(post("/api/admin/media/fetch")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("url", "https://evil.example.com/x.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("OSS 未配置，暂不支持取回外链"));
    }

    @Test
    void replaceOverwritesInPlaceAndRejectsFormatChange() throws Exception {
        Cookie session = login();

        MockMultipartFile origin = new MockMultipartFile(
                "file", "tiny.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4});
        MvcResult uploaded = mockMvc.perform(multipart("/api/admin/upload").file(origin).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        String name = mapper.readTree(uploaded.getResponse().getContentAsString())
                .path("data").path("name").asText();

        try {
            // 同名同格式覆盖：文件名不变，url 带版本参数（本地与 OSS 都是 7 天强缓存，不带版本看不到新图）
            MockMultipartFile cropped = new MockMultipartFile(
                    "file", name, "image/png",
                    new byte[]{(byte) 0x89, 'P', 'N', 'G', 9, 9, 9, 9, 9, 9});
            MvcResult replaced = mockMvc.perform(multipart("/api/admin/media/" + name + "/replace")
                            .file(cropped)
                            .cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.name").value(name))
                    .andExpect(jsonPath("$.data.size").value(10))
                    .andReturn();
            String url = mapper.readTree(replaced.getResponse().getContentAsString())
                    .path("data").path("url").asText();
            assertTrue(url.startsWith("/api/uploads/" + name + "?v="), "覆盖应返回带版本号的原地址，实际：" + url);

            // 换格式覆盖必须被拒：扩展名还是 .png，内容却是 jpeg，服务端按扩展名给出的 Content-Type 会是错的
            MockMultipartFile jpeg = new MockMultipartFile(
                    "file", name, "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5});
            mockMvc.perform(multipart("/api/admin/media/" + name + "/replace").file(jpeg).cookie(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(400))
                    .andExpect(jsonPath("$.message").value("覆盖要求格式与原文件一致，请改用「保存为新图」"));
        } finally {
            mockMvc.perform(delete("/api/admin/media/" + name).cookie(session))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void replaceRefusesToCreateMissingFile() throws Exception {
        Cookie session = login();

        MockMultipartFile png = new MockMultipartFile(
                "file", "nope.png", "image/png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4});
        mockMvc.perform(multipart("/api/admin/media/nope-not-here.png/replace").file(png).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("原文件不存在，无法覆盖"));
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
