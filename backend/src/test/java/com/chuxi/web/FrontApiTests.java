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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 前台评论 / 弹幕 / 浏览量 bump 接口测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class FrontApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @BeforeEach
    void resetRateLimiter() {
        RateLimiter.reset();
    }

    /* ========== 评论接口 ========== */

    @Test
    void addComment_validInput_succeeds() throws Exception {
        mockMvc.perform(post("/api/front/articles/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nickname", "测试用户", "content", "好文章！"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                .andExpect(jsonPath("$.data.content").value("好文章！"));
    }

    @Test
    void addComment_emptyContent_returns400() throws Exception {
        mockMvc.perform(post("/api/front/articles/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nickname", "用户", "content", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addComment_nicknameTooLong_returns400() throws Exception {
        String longName = "A".repeat(21);
        mockMvc.perform(post("/api/front/articles/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nickname", longName, "content", "内容"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addComment_scriptTagIsSanitized() throws Exception {
        mockMvc.perform(post("/api/front/articles/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "nickname", "用户",
                                "content", "你好<script>alert(1)</script>世界"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("你好世界"));
    }

    /* ========== 浏览量 bump 防刷 ========== */

    @Test
    void bump_firstCall_succeeds() throws Exception {
        mockMvc.perform(post("/api/front/views/bump"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.views").isNumber());
    }

    @Test
    void bump_secondCallWithinHour_silentNoIncrement() throws Exception {
        // 第一次 bump
        MvcResult firstResult = mockMvc.perform(post("/api/front/views/bump"))
                .andExpect(status().isOk())
                .andReturn();
        String body = firstResult.getResponse().getContentAsString();
        int firstViews = com.jayway.jsonpath.JsonPath.read(body, "$.data.views");

        // 第二次 bump（同一 IP，1 小时内）应静默返回当前计数，不增加
        mockMvc.perform(post("/api/front/views/bump"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.views").value(firstViews));
    }

    /* ========== 弹幕接口 ========== */

    @Test
    void addBarrage_validInput_succeeds() throws Exception {
        mockMvc.perform(post("/api/front/tree-hole/barrages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nickname", "树友A", "content", "好看！"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("树友A"));
    }

    @Test
    void addBarrage_emptyContent_returns400() throws Exception {
        mockMvc.perform(post("/api/front/tree-hole/barrages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("nickname", "用户", "content", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void addBarrage_anonymous_succeeds() throws Exception {
        // 弹幕允许匿名，不传 nickname 应成功
        mockMvc.perform(post("/api/front/tree-hole/barrages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("content", "匿名弹幕"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("树友-0001"));
    }
}
