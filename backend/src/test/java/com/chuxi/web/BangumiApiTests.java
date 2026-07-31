package com.chuxi.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 前台番剧接口测试：
 * 1. 番剧列表 /api/front/bangumi 返回 200，data 为数组；
 * 2. 番剧详情 /api/front/bangumi/1 返回 200（种子数据存在）或 200+code=400（不存在）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class BangumiApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bangumiList_returns200AndDataIsArray() throws Exception {
        mockMvc.perform(get("/api/front/bangumi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void bangumiDetail_id1_returns200OrNotFound() throws Exception {
        // 种子数据中是否有 id=1 的记录取决于 H2 自增；两种结果均合法
        mockMvc.perform(get("/api/front/bangumi/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void bangumiDetail_nonExistentId_returnsCode400() throws Exception {
        // 使用一个极大 id，保证不存在
        mockMvc.perform(get("/api/front/bangumi/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("记录不存在"));
    }
}
