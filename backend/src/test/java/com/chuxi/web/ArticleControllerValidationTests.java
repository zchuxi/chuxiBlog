package com.chuxi.web;

import com.chuxi.common.ClientIpResolver;
import com.chuxi.entity.Article;
import com.chuxi.repo.ArticleRepo;
import com.chuxi.repo.CommentLikeRepo;
import com.chuxi.repo.CommentRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ArticleControllerValidationTests {

    private ArticleRepo articleRepo;
    private CommentRepo commentRepo;
    private CommentLikeRepo commentLikeRepo;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        articleRepo = mock(ArticleRepo.class);
        commentRepo = mock(CommentRepo.class);
        commentLikeRepo = mock(CommentLikeRepo.class);
        var clientIpResolver = mock(ClientIpResolver.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ArticleController(articleRepo, commentRepo, commentLikeRepo, clientIpResolver)
        ).build();
    }

    @Test
    void rejectsInvalidSearchPagination() throws Exception {
        mockMvc.perform(get("/api/front/articles/search")
                        .param("pageNo", "0")
                        .param("pageSize", "51"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("分页参数无效"));

        verifyNoInteractions(articleRepo);
    }

    @Test
    void rejectsCommentForMissingArticle() throws Exception {
        when(articleRepo.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/front/articles/999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"访客\",\"content\":\"测试评论\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("文章不存在"));

        verify(commentRepo, never()).save(any());
    }

    @Test
    void rejectsCommentForDraftArticle() throws Exception {
        Article draft = new Article();
        draft.setStatus("草稿");
        when(articleRepo.findById(7L)).thenReturn(Optional.of(draft));

        mockMvc.perform(post("/api/front/articles/7/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"访客\",\"content\":\"测试评论\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("文章不存在"));

        verify(commentRepo, never()).save(any());
    }
}
