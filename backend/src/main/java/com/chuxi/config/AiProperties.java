package com.chuxi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** OpenAI-compatible chat completions 配置。密钥仅从环境变量注入，不通过接口回显。 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {
    private boolean enabled = false;
    private String baseUrl = "https://api.deepseek.com/v1";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 20;
    private int maxContextArticles = 5;
    /**
     * 全站每日上游调用上限（0 或负数表示不限制）。
     * 公开接口消耗站方付费 key，每 IP 限流可被换 IP 绕过，故再加一道与来源无关的日闸。
     * 超出后不报错，降级为站内检索结果（零成本），站点功能不中断。
     */
    private int dailyQuota = 300;

    /** 将后台持久化的非敏感配置覆盖当前环境配置，apiKey 始终保留环境注入值。 */
    public void applyNonSensitive(boolean enabled, String baseUrl, String model,
                                  int timeoutSeconds, int maxContextArticles) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
        this.maxContextArticles = maxContextArticles;
    }

    public boolean ready() {
        return enabled && notBlank(baseUrl) && notBlank(apiKey) && notBlank(model);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
