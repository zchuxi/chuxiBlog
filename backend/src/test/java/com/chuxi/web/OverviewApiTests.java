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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 管理后台概览接口测试：
 * 1. 无 token 访问 /api/admin/overview 返回 401；
 * 2. 登录后访问返回 200，响应包含 articleCount、viewCount 等统计字段。
 */
@SpringBootTest
@AutoConfigureMockMvc
class OverviewApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void overviewWithoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void overviewWithToken_returns200AndContainsExpectedFields() throws Exception {
        String token = login();

        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.articleCount").isNumber())
                .andExpect(jsonPath("$.data.draftCount").isNumber())
                .andExpect(jsonPath("$.data.categoryCount").isNumber())
                .andExpect(jsonPath("$.data.tagCount").isNumber())
                .andExpect(jsonPath("$.data.viewCount").isNumber())
                .andExpect(jsonPath("$.data.bangumiCount").isNumber())
                .andExpect(jsonPath("$.data.toolCount").isNumber())
                .andExpect(jsonPath("$.data.musicCount").isNumber())
                .andExpect(jsonPath("$.data.carouselCount").isNumber())
                .andExpect(jsonPath("$.data.collapseCardCount").isNumber())
                .andExpect(jsonPath("$.data.timelineCount").isNumber())
                .andExpect(jsonPath("$.data.commentCount").isNumber())
                .andExpect(jsonPath("$.data.barrageCount").isNumber())
                .andExpect(jsonPath("$.data.categoryDistribution").isArray());
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
