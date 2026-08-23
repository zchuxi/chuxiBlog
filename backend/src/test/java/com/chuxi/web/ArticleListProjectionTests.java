package com.chuxi.web;

import com.chuxi.common.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文章列表接口走 ArticleLite 投影（不加载 LONGTEXT content）后的字段契约测试。
 *
 * <p>投影按别名逐字段映射，写错别名或漏字段不会让查询失败、只会让响应里静默出现 null，
 * 因此这里对每个字段断言其存在与类型，而不只校验 HTTP 状态。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleListProjectionTests {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetRateLimiter() {
        RateLimiter.reset();
    }

    @Test
    void homeArticles_projectionExposesAllListFields() throws Exception {
        mockMvc.perform(get("/api/front/home/articles?pageNo=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].id").isNumber())
                .andExpect(jsonPath("$.data.records[0].title").isString())
                .andExpect(jsonPath("$.data.records[0].summary").exists())
                // tags 库内是 CSV，DTO 层必须已切成数组
                .andExpect(jsonPath("$.data.records[0].tags").isArray())
                .andExpect(jsonPath("$.data.records[0].pinned").isBoolean())
                .andExpect(jsonPath("$.data.records[0].createdAt").exists())
                .andExpect(jsonPath("$.data.records[0].updatedAt").exists())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void homeArticles_projectionNeverLeaksContent() throws Exception {
        // 列表投影的意义就在于不读正文；content 一旦出现说明退回了整实体查询
        mockMvc.perform(get("/api/front/home/articles?pageNo=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].content").doesNotExist());
    }

    @Test
    void searchArticles_emptyKeywordListsPublished() throws Exception {
        mockMvc.perform(get("/api/front/articles/search?pageNo=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].title").isString())
                .andExpect(jsonPath("$.data.records[0].tags").isArray())
                .andExpect(jsonPath("$.data.records[0].content").doesNotExist());
    }

    @Test
    void searchArticles_keywordKeepsProjectionShape() throws Exception {
        // 关键词命中标题/摘要/标签任一即可；这里只要求响应结构与投影一致
        mockMvc.perform(get("/api/front/articles/search?keyword=a&pageNo=1&pageSize=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.total").isNumber());
    }

    @Test
    void addComment_onMissingArticle_returnsFail() throws Exception {
        // countPublishedById 取代 findById 后，不存在的 id 仍须判为「文章不存在」
        mockMvc.perform(post("/api/front/articles/999999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"访客\",\"content\":\"测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }
}
