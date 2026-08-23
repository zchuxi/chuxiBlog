package com.chuxi.web;

import com.chuxi.common.RateLimiter;
import com.chuxi.service.AiChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiApiTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @MockBean AiChatService aiChatService;

    @BeforeEach void reset() { RateLimiter.reset(); }

    @Test void blankMessageReturns400() throws Exception {
        mockMvc.perform(post("/api/front/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("messages", List.of(Map.of("role", "user", "content", " "))))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
    }

    @Test void successfulChatReturnsReply() throws Exception {
        when(aiChatService.chat(anyList())).thenReturn(new AiChatService.ChatResult("你好", List.of(), false));
        mockMvc.perform(post("/api/front/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("messages", List.of(Map.of("role", "user", "content", "你好"))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.reply").value("你好"));
    }

    @Test void missingMessagesReturns400WithoutCallingUpstream() throws Exception {
        mockMvc.perform(post("/api/front/ai/chat").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("请求参数不合法"));
    }

    @Test void eleventhRequestWithinMinuteIsRateLimited() throws Exception {
        when(aiChatService.chat(anyList())).thenReturn(new AiChatService.ChatResult("ok", List.of(), false));
        String body = mapper.writeValueAsString(Map.of("messages", List.of(Map.of("role", "user", "content", "你好"))));
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/front/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(jsonPath("$.code").value(0));
        }
        mockMvc.perform(post("/api/front/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请求过于频繁，请稍后再试"));
    }
}
