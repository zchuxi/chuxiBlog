package com.chuxi.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import jakarta.servlet.http.Cookie;
import com.chuxi.auth.TokenStore;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 核心 API 最小行为检查（MockMvc，复用 src/test/resources 的 H2 内存库配置）：
 * 1. /api/admin/** 无管理 Cookie 被拦截为 401；
 * 2. 密码错误的登录返回业务失败（同时触发 AuthController 的鉴权失败 warn 日志）；
 * 3. 登录取得管理 Cookie 后走一条 CRUD 正常路径（archive-categories 新建→列表→删除）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void adminApiWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/articles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminBangumiSearch_requiresAdminCookie() throws Exception {
        mockMvc.perform(get("/api/admin/bangumi/search").param("keyword", "芙莉莲"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminBangumiSearch_emptyKeyword_rejects() throws Exception {
        Cookie session = login();
        mockMvc.perform(get("/api/admin/bangumi/search").param("keyword", "").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void adminBangumiSync_missingToken_rejects() throws Exception {
        Cookie session = login();
        mockMvc.perform(post("/api/admin/bangumi/sync-collections")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", "wrong-password"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void loginThenArchiveCategoryCrud() throws Exception {
        Cookie session = login();

        // 新建
        Map<String, Object> body = Map.of(
                "category", "test-category",
                "title", "测试分类",
                "description", "MockMvc 测试用",
                "tags", List.of("测试", "临时"));
        MvcResult created = mockMvc.perform(post("/api/admin/archive-categories")
                        .cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.title").value("测试分类"))
                .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 列表可见
        MvcResult listed = mockMvc.perform(get("/api/admin/archive-categories")
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        boolean found = false;
        for (JsonNode item : mapper.readTree(listed.getResponse().getContentAsString()).path("data")) {
            if (item.path("id").asLong() == id) {
                found = true;
                break;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(found, "新建的分类应出现在列表中");

        // 删除
        mockMvc.perform(delete("/api/admin/archive-categories/" + id)
                        .cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
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
