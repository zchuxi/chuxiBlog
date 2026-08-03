package com.chuxi.web;

import com.chuxi.entity.Comment;
import com.chuxi.repo.CommentLikeRepo;
import com.chuxi.repo.CommentRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentLikeApiTests {

    private static final String VISITOR_A = "visitor_aaaaaaaaaaaaaaaa";
    private static final String VISITOR_B = "visitor_bbbbbbbbbbbbbbbb";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CommentRepo commentRepo;

    @Autowired
    private CommentLikeRepo commentLikeRepo;

    private Comment comment;

    @BeforeEach
    void createComment() {
        comment = new Comment();
        comment.setArticleId(1L);
        comment.setNickname("点赞测试");
        comment.setContent("隔离测试");
        comment.setLikeCount(0);
        comment.setLiked(false);
        comment.setApproved(true);
        comment.setCreatedAt(LocalDateTime.now());
        comment = commentRepo.save(comment);
    }

    @AfterEach
    void cleanUp() {
        commentLikeRepo.deleteAll();
        if (commentRepo.existsById(comment.getId())) commentRepo.deleteById(comment.getId());
    }

    @Test
    void likesAreIsolatedByVisitorAndToggleIndependently() throws Exception {
        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", VISITOR_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        JsonNode visitorBView = responseJson(mockMvc.perform(
                        get("/api/front/articles/1/comments").header("X-Visitor-Id", VISITOR_B))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode bComment = findComment(visitorBView, comment.getId());
        assertThat(bComment.path("liked").asBoolean()).isFalse();
        assertThat(bComment.path("likeCount").asInt()).isEqualTo(1);

        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", VISITOR_B))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(2));

        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", VISITOR_A))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertThat(commentLikeRepo.count()).isEqualTo(1);
    }

    @Test
    void likeRejectsInvalidVisitorId() throws Exception {
        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", "bad id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("访客标识无效"));

        assertThat(commentLikeRepo.count()).isZero();
    }

    private JsonNode responseJson(String body) throws Exception {
        return mapper.readTree(body);
    }

    private JsonNode findComment(JsonNode response, long id) {
        for (JsonNode item : response.path("data")) {
            if (item.path("id").asLong() == id) return item;
        }
        throw new AssertionError("评论未出现在列表中: " + id);
    }
}
