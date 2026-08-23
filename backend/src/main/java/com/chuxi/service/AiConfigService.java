package com.chuxi.service;

import com.chuxi.config.AiProperties;
import com.chuxi.entity.SiteContent;
import com.chuxi.repo.SiteContentRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Map;

/** AI 后台配置：非敏感项持久化于 site_content，密钥始终来自环境变量。 */
@Service
public class AiConfigService {
    public static final String CONTENT_KEY = "ai-settings";
    private static final Set<String> CONFIG_KEYS = Set.of(
            "enabled", "baseUrl", "model", "timeoutSeconds", "maxContextArticles");
    private static final Logger log = LoggerFactory.getLogger(AiConfigService.class);

    private final SiteContentRepo siteContentRepo;
    private final ObjectMapper mapper;
    private final AiProperties properties;

    public AiConfigService(SiteContentRepo siteContentRepo, ObjectMapper mapper, AiProperties properties) {
        this.siteContentRepo = siteContentRepo;
        this.mapper = mapper;
        this.properties = properties;
    }

    /** 应用启动时加载数据库覆盖项；损坏数据保留环境配置并记录日志。 */
    @PostConstruct
    @Transactional(readOnly = true)
    public void loadPersistedConfig() {
        siteContentRepo.findByContentKey(CONTENT_KEY).ifPresent(content -> {
            try {
                ConfigValue value = readAndValidate(mapper.readTree(content.getContentJson()));
                properties.applyNonSensitive(value.enabled(), value.baseUrl(), value.model(),
                        value.timeoutSeconds(), value.maxContextArticles());
            } catch (Exception e) {
                log.warn("AI 持久化配置损坏，回退环境变量：key={}", CONTENT_KEY);
            }
        });
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConfig() {
        return response();
    }

    @Transactional
    public synchronized Map<String, Object> updateConfig(Map<String, Object> body) {
        if (body == null || !CONFIG_KEYS.equals(body.keySet())) {
            throw new IllegalArgumentException("仅支持非敏感配置项");
        }
        ConfigValue value = readAndValidate(mapper.valueToTree(body));
        SiteContent content = siteContentRepo.findByContentKey(CONTENT_KEY).orElseGet(() -> {
            SiteContent n = new SiteContent();
            n.setContentKey(CONTENT_KEY);
            return n;
        });
        try {
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("enabled", value.enabled());
            saved.put("baseUrl", value.baseUrl());
            saved.put("model", value.model());
            saved.put("timeoutSeconds", value.timeoutSeconds());
            saved.put("maxContextArticles", value.maxContextArticles());
            content.setContentJson(mapper.writeValueAsString(saved));
        } catch (Exception e) {
            throw new IllegalArgumentException("配置保存失败", e);
        }
        content.setUpdatedAt(LocalDateTime.now());
        siteContentRepo.save(content);
        properties.applyNonSensitive(value.enabled(), value.baseUrl(), value.model(),
                value.timeoutSeconds(), value.maxContextArticles());
        return response();
    }

    private Map<String, Object> response() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", properties.isEnabled());
        out.put("baseUrl", properties.getBaseUrl());
        out.put("model", properties.getModel());
        out.put("timeoutSeconds", properties.getTimeoutSeconds());
        out.put("maxContextArticles", properties.getMaxContextArticles());
        out.put("apiKeyConfigured", properties.getApiKey() != null && !properties.getApiKey().isBlank());
        return out;
    }

    private ConfigValue readAndValidate(JsonNode node) {
        if (node == null || !node.isObject()
                || !node.has("enabled") || !node.has("baseUrl") || !node.has("model")
                || !node.has("timeoutSeconds") || !node.has("maxContextArticles")) {
            throw new IllegalArgumentException("配置字段不完整");
        }
        if (!node.path("enabled").isBoolean()) {
            throw new IllegalArgumentException("配置字段类型不正确");
        }
        boolean enabled = node.path("enabled").asBoolean();
        String baseUrl = node.path("baseUrl").asText("").trim();
        String model = node.path("model").asText("").trim();
        if (!node.path("timeoutSeconds").isIntegralNumber()
                || !node.path("maxContextArticles").isIntegralNumber()) {
            throw new IllegalArgumentException("配置字段类型不正确");
        }
        int timeout = node.path("timeoutSeconds").asInt();
        int context = node.path("maxContextArticles").asInt();
        try {
            URI uri = URI.create(baseUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new IllegalArgumentException("baseUrl 必须是有效的 HTTP(S) URL");
        }
        if (model.isBlank()) throw new IllegalArgumentException("model 不能为空");
        if (timeout < 1 || timeout > 120) throw new IllegalArgumentException("timeoutSeconds 必须在 1-120 之间");
        if (context < 1 || context > 8) throw new IllegalArgumentException("maxContextArticles 必须在 1-8 之间");
        return new ConfigValue(enabled, baseUrl, model, timeout, context);
    }

    private record ConfigValue(boolean enabled, String baseUrl, String model,
                               int timeoutSeconds, int maxContextArticles) { }
}
