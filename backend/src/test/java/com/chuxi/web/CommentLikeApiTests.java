package com.chuxi.web;

import com.chuxi.common.RateLimiter;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommentLikeApiTests {

    /** 服务端签发的匿名身份 token（SEC-001：客户端不能自行构造合法身份） */
    private String visitorA;
    private String visitorB;

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
    void createComment() throws Exception {
        RateLimiter.reset();
        visitorA = issueToken();
        visitorB = issueToken();
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

    /** 通过签发接口获取合法访客身份，模拟正常前端流程 */
    private String issueToken() throws Exception {
        String body = mockMvc.perform(get("/api/front/visitor/token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    void likesAreIsolatedByVisitorAndToggleIndependently() throws Exception {
        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", visitorA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        JsonNode visitorBView = responseJson(mockMvc.perform(
                        get("/api/front/articles/1/comments").header("X-Visitor-Id", visitorB))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        JsonNode bComment = findComment(visitorBView, comment.getId());
        assertThat(bComment.path("liked").asBoolean()).isFalse();
        assertThat(bComment.path("likeCount").asInt()).isEqualTo(1);

        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", visitorB))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(2));

        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", visitorA))
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        assertThat(commentLikeRepo.count()).isEqualTo(1);
    }

    @Test
    void likeRejectsUnsignedVisitorId() throws Exception {
        // 客户端自行构造的裸 id 不再被信任（fail-closed），并下发新签发的 token 供前端更新
        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", "visitor_aaaaaaaaaaaaaaaa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("访客标识无效，请刷新后重试"))
                .andExpect(header().exists("X-Visitor-Token"));

        assertThat(commentLikeRepo.count()).isZero();
    }

    @Test
    void likeRejectsTamperedSignature() throws Exception {
        // 篡改签名（改最后一个字符）必须被拒绝且不产生记录
        String tampered = visitorA.substring(0, visitorA.length() - 1)
                + (visitorA.endsWith("0") ? "1" : "0");
        mockMvc.perform(post("/api/front/articles/comments/{id}/likes", comment.getId())
                        .header("X-Visitor-Id", tampered))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(commentLikeRepo.count()).isZero();
    }

    private JsonNode responseJson(String body) throws Exception {
        return mapper.readTree(body);
    }

    private JsonNode findComment(JsonNode response, long id) {
        // comments 接口分页后返回 PageData：data.records
        for (JsonNode item : response.path("data").path("records")) {
            if (item.path("id").asLong() == id) return item;
        }
        throw new AssertionError("评论未出现在列表中: " + id);
    }
}
