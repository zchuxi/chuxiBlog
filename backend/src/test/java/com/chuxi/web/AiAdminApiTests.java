package com.chuxi.web;

import com.chuxi.config.AiProperties;
import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import com.chuxi.auth.TokenStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AiAdminApiTests {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired SiteContentRepo siteContentRepo;
    @Autowired AiProperties aiProperties;
    @Autowired TokenStore tokenStore;

    private String sessionToken;
    private boolean originalEnabled;
    private String originalBaseUrl;
    private String originalModel;
    private int originalTimeoutSeconds;
    private int originalMaxContextArticles;

    @BeforeEach
    void rememberRuntimeConfig() {
        sessionToken = tokenStore.issue("admin");
        originalEnabled = aiProperties.isEnabled();
        originalBaseUrl = aiProperties.getBaseUrl();
        originalModel = aiProperties.getModel();
        originalTimeoutSeconds = aiProperties.getTimeoutSeconds();
        originalMaxContextArticles = aiProperties.getMaxContextArticles();
    }

    @AfterEach
    void cleanup() {
        siteContentRepo.findByContentKey("ai-settings").ifPresent(siteContentRepo::delete);
        tokenStore.invalidate(sessionToken);
        aiProperties.applyNonSensitive(originalEnabled, originalBaseUrl, originalModel,
                originalTimeoutSeconds, originalMaxContextArticles);
    }

    @Test
    void getConfigRequiresAdminAuth() throws Exception {
        mockMvc.perform(get("/api/admin/ai/config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getConfigDoesNotExposeApiKey() throws Exception {
        Cookie session = login();
        MvcResult result = mockMvc.perform(get("/api/admin/ai/config").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").isBoolean())
                .andExpect(jsonPath("$.data.baseUrl").isString())
                .andExpect(jsonPath("$.data.model").isString())
                .andExpect(jsonPath("$.data.timeoutSeconds").isNumber())
                .andExpect(jsonPath("$.data.maxContextArticles").isNumber())
                .andExpect(jsonPath("$.data.apiKeyConfigured").isBoolean())
                .andReturn();
        JsonNode data = mapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.has("apiKey")).isFalse();
    }

    @Test
    void putConfigValidatesAndPersistsNonSensitiveFields() throws Exception {
        Cookie session = login();
        Map<String, Object> body = Map.of(
                "enabled", true,
                "baseUrl", "https://example.com/v1",
                "model", "demo-model",
                "timeoutSeconds", 45,
                "maxContextArticles", 7);

        mockMvc.perform(put("/api/admin/ai/config").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.model").value("demo-model"))
                .andExpect(jsonPath("$.data.apiKeyConfigured").isBoolean())
                .andExpect(jsonPath("$.data.apiKey").doesNotExist());

        assertThat(aiProperties.isEnabled()).isTrue();
        assertThat(aiProperties.getBaseUrl()).isEqualTo("https://example.com/v1");
        assertThat(aiProperties.getModel()).isEqualTo("demo-model");
        assertThat(aiProperties.getTimeoutSeconds()).isEqualTo(45);
        assertThat(aiProperties.getMaxContextArticles()).isEqualTo(7);
        SiteContent saved = siteContentRepo.findByContentKey("ai-settings").orElseThrow();
        JsonNode json = mapper.readTree(saved.getContentJson());
        assertThat(json.has("apiKey")).isFalse();
        assertThat(json.path("model").asText()).isEqualTo("demo-model");
    }

    @Test
    void putConfigRejectsInvalidValues() throws Exception {
        Cookie session = login();
        Map<String, Object> body = Map.of(
                "enabled", true,
                "baseUrl", "not-a-url",
                "model", " ",
                "timeoutSeconds", 121,
                "maxContextArticles", 9);

        mockMvc.perform(put("/api/admin/ai/config").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void putConfigRejectsSensitiveOrUnknownFields() throws Exception {
        Cookie session = login();
        Map<String, Object> body = Map.of(
                "enabled", true,
                "baseUrl", "https://example.com/v1",
                "model", "demo-model",
                "timeoutSeconds", 20,
                "maxContextArticles", 5,
                "apiKey", "must-not-be-stored");

        mockMvc.perform(put("/api/admin/ai/config").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));

        assertThat(siteContentRepo.findByContentKey("ai-settings")).isEmpty();
    }

    @Test
    void genericSiteContentApiCannotBypassAiValidation() throws Exception {
        Cookie session = login();
        mockMvc.perform(put("/api/admin/site-content/ai-settings").cookie(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("contentJson", "{\"apiKey\":\"leak\"}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
        assertThat(siteContentRepo.findByContentKey("ai-settings")).isEmpty();
    }

    private Cookie login() throws Exception {
        return new Cookie(TokenStore.COOKIE_NAME, sessionToken);
    }
}
