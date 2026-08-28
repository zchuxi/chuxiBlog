package com.chuxi.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chuxi.auth.TokenStore;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全边界回归测试（P0）——锁定既有安全防护不在重构中被无意放宽：
 * 1. 伪造 MIME / 伪造文件头的上传必须被拒绝；
 * 2. 路径穿越（..、反斜杠、编码斜杠）访问上传资源必须被拒绝；
 * 3. 未登录访问各代表性管理 API（GET/POST/PUT/DELETE）一律 401；
 * 4. 管理端写入后前台读取必须立即生效（缓存精确失效）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityBoundaryRegressionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    // ---------- 1. 伪造 MIME / 文件头上传拒绝 ----------

    @Test
    void uploadRejectsForgedMimeAndExtension() throws Exception {
        Cookie session = login();
        // 文本内容伪装成 .txt / text/plain：扩展名与 MIME 均不在白名单
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.txt", "text/plain", "not an image at all".getBytes());
        mockMvc.perform(multipart("/api/admin/upload").file(file).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("不支持的文件类型，仅允许图片和音频文件"));
    }

    @Test
    void uploadRejectsForgedMagicNumber() throws Exception {
        Cookie session = login();
        // 扩展名 .png + MIME image/png 均合法，但文件头不是 PNG magic number
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.png", "image/png", "MZ this is actually an exe".getBytes());
        mockMvc.perform(multipart("/api/admin/upload").file(file).cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文件内容与声明类型不符"));
    }

    // ---------- 2. 路径穿越访问上传资源拒绝 ----------

    @Test
    void serveRejectsDotDotName() throws Exception {
        mockMvc.perform(get("/api/uploads/.."))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serveRejectsBackslashName() throws Exception {
        // %5C 解码后为反斜杠，命中 badName 的 \\ 检查
        mockMvc.perform(get("/api/uploads/..%5Csecret.ini"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serveRejectsEncodedTraversal() throws Exception {
        // 解码后含 / 的名字匹配不到单段路由（404），或被 badName 拦截（400），一律不得 200
        mockMvc.perform(get("/api/uploads/%2e%2e%2fetc%2fpasswd"))
                .andExpect(status().is4xxClientError());
    }

    // ---------- 3. 未登录访问管理 API 一律 401 ----------

    @Test
    void adminEndpointsWithoutTokenReturn401() throws Exception {
        mockMvc.perform(get("/api/admin/site-content"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/admin/media"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(put("/api/admin/site-content/about")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("contentJson", "x"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(delete("/api/admin/media/nonexistent.png"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
        mockMvc.perform(post("/api/admin/media/fetch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("url", "https://example.com/a.png"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ---------- 4. 管理端写入后前台立即读到新内容（缓存精确失效） ----------

    @Test
    void frontReadReflectsAdminWriteImmediately() throws Exception {
        Cookie session = login();
        String key = "about";

        // 记录原始值，测试结束恢复，避免污染共享测试上下文
        MvcResult before = mockMvc.perform(get("/api/front/site-content/" + key)).andReturn();
        String originalJson = null;
        var beforeTree = mapper.readTree(before.getResponse().getContentAsString());
        if (beforeTree.path("code").asInt() == 0) {
            originalJson = beforeTree.path("data").path("contentJson").asText();
        }

        try {
            // 写入 v1 并读一次（让 v1 进入缓存）
            mockMvc.perform(put("/api/admin/site-content/" + key)
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("contentJson", "regression-v1"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/front/site-content/" + key))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.contentJson").value("regression-v1"));

            // 改写 v2 后前台必须立即读到 v2；若 @CacheEvict 失效此处会读到缓存的 v1
            mockMvc.perform(put("/api/admin/site-content/" + key)
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("contentJson", "regression-v2"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            mockMvc.perform(get("/api/front/site-content/" + key))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.contentJson").value("regression-v2"));
        } finally {
            // 恢复原始值（原本无记录则写入空串占位，避免留下测试数据）
            mockMvc.perform(put("/api/admin/site-content/" + key)
                            .cookie(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(
                                    Map.of("contentJson", originalJson != null ? originalJson : ""))))
                    .andExpect(status().isOk());
        }
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
