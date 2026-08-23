package com.chuxi.service;

import com.chuxi.config.AiProperties;
import com.chuxi.entity.Article;
import com.chuxi.repo.ArticleRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.Objects;

/** 站内文章增强的 OpenAI-compatible 对话服务。 */
@Service
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_ARTICLE_TEXT_LENGTH = 1800;

    private final ArticleRepo articleRepo;
    private final AiProperties properties;
    private final RestClient.Builder restClientBuilder;

    public AiChatService(ArticleRepo articleRepo, AiProperties properties, RestClient.Builder restClientBuilder) {
        this.articleRepo = articleRepo;
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    /** 使用站内已发布文章构造上下文，并在模型不可用时返回可用的检索降级结果。 */
    public ChatResult chat(String message) {
        return chat(List.of(new Message("user", message)));
    }

    public ChatResult chat(List<Message> messages) {
        if (messages == null || messages.isEmpty()) throw new IllegalArgumentException("消息不能为空");
        if (messages.size() > 16) throw new IllegalArgumentException("消息数量过多");
        for (Message item : messages) {
            if (item == null || item.content() == null || item.content().isBlank()) throw new IllegalArgumentException("消息不能为空");
            if (item.content().length() > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException("消息过长");
            if (!"user".equalsIgnoreCase(item.role()) && !"assistant".equalsIgnoreCase(item.role())) {
                throw new IllegalArgumentException("消息角色无效");
            }
        }
        Message last = messages.get(messages.size() - 1);
        if (last == null || last.content() == null || !"user".equalsIgnoreCase(last.role())) {
            throw new IllegalArgumentException("最后一条消息必须来自用户");
        }
        String prompt = normalizeMessage(last.content());
        List<Article> articles = findArticles(prompt);
        List<ArticleRef> references = articles.stream()
                .map(a -> new ArticleRef(a.getId(), safe(a.getTitle())))
                .toList();
        if (!properties.ready()) {
            return new ChatResult(fallbackReply(prompt, articles), references, true);
        }

        try {
            String reply = callModel(prompt, messages, articles);
            if (reply == null || reply.isBlank()) {
                return new ChatResult(fallbackReply(prompt, articles), references, true);
            }
            return new ChatResult(reply.trim(), references, false);
        } catch (RuntimeException ex) {
            // 只记录异常类型，绝不记录请求头、密钥或上游原文。
            log.warn("AI 上游请求失败，已降级: {}", ex.getClass().getSimpleName());
            return new ChatResult(fallbackReply(prompt, articles), references, true);
        }
    }

    private String normalizeMessage(String message) {
        if (message == null || message.isBlank()) throw new IllegalArgumentException("消息不能为空");
        String normalized = message.trim();
        if (normalized.length() > MAX_MESSAGE_LENGTH) throw new IllegalArgumentException("消息过长");
        return normalized;
    }

    private List<Article> findArticles(String prompt) {
        int limit = Math.max(1, Math.min(properties.getMaxContextArticles(), 8));
        try {
            List<Article> matches = new ArrayList<>(articleRepo.searchPublished(prompt, PageRequest.of(0, limit)).getContent());
            // searchPublished 覆盖标题/摘要/标签；仍不足时用数据库侧 LIKE 补正文匹配。
            // 交给数据库并用 Pageable 限流：此前在 Java 内存里对全部已发布文章做 contains，
            // 等于每次公开 AI 请求都把所有 LONGTEXT 正文读一遍。
            // 多取一页余量，过滤掉已命中的 id 后仍能补足 limit。
            if (matches.size() < limit) {
                int remaining = limit - matches.size();
                List<Article> byContent = articleRepo.searchPublishedByContent(
                        prompt, PageRequest.of(0, remaining + matches.size()));
                if (byContent == null) byContent = List.of();
                byContent.stream()
                        .filter(a -> matches.stream().noneMatch(existing -> Objects.equals(existing.getId(), a.getId())))
                        .limit(remaining)
                        .forEach(matches::add);
            }
            return matches;
        } catch (RuntimeException ex) {
            log.warn("AI 文章检索失败，继续无上下文请求: {}", ex.getClass().getSimpleName());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private String callModel(String prompt, List<Message> history, List<Article> articles) {
        String endpoint = endpoint(properties.getBaseUrl());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", 0.3);
        List<Map<String, String>> upstreamMessages = new ArrayList<>();
        upstreamMessages.add(Map.of("role", "system", "content", systemPrompt(articles)));
        int from = Math.max(0, history.size() - 8);
        for (int i = from; i < history.size(); i++) {
            Message item = history.get(i);
            if (item == null || item.content() == null) continue;
            String role = "assistant".equalsIgnoreCase(item.role()) ? "assistant" : "user";
            upstreamMessages.add(Map.of("role", role, "content", truncate(item.content().trim(), MAX_MESSAGE_LENGTH)));
        }
        // 确保检索用的最后一条用户消息始终位于请求末尾。
        if (upstreamMessages.size() == 1 || !prompt.equals(upstreamMessages.get(upstreamMessages.size() - 1).get("content"))) {
            upstreamMessages.add(Map.of("role", "user", "content", prompt));
        }
        body.put("messages", upstreamMessages);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeout = Math.max(1, Math.min(properties.getTimeoutSeconds(), 120));
        requestFactory.setConnectTimeout(Duration.ofSeconds(timeout));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeout));
        Map<String, Object> response = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .build()
                .post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null) return null;
        Object choices = response.get("choices");
        if (!(choices instanceof List<?> list) || list.isEmpty() || !(list.get(0) instanceof Map<?, ?> choice)) return null;
        Object message = choice.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) return null;
        Object content = messageMap.get("content");
        return content instanceof String ? (String) content : null;
    }

    private String systemPrompt(List<Article> articles) {
        StringBuilder context = new StringBuilder("你是本站的中文文章助手。仅依据给定的站内文章回答；没有依据时明确说明，不要编造。\n\n");
        if (articles.isEmpty()) return context.append("当前没有检索到匹配的已发布文章。").toString();
        context.append("已发布文章参考：\n");
        for (Article article : articles) {
            context.append("- ").append(safe(article.getTitle())).append("：");
            String text = safe(article.getSummary());
            if (text.isBlank()) text = safe(article.getContent());
            context.append(truncate(text, MAX_ARTICLE_TEXT_LENGTH)).append('\n');
        }
        return context.toString();
    }

    private String fallbackReply(String prompt, List<Article> articles) {
        if (articles.isEmpty()) return "暂时没有找到相关的已发布文章。你可以换个关键词再试试。";
        StringBuilder reply = new StringBuilder("我在站内找到这些相关文章：\n");
        articles.forEach(article -> reply.append("· ").append(safe(article.getTitle())).append('\n'));
        reply.append("\n当前 AI 模型暂不可用，先为你列出检索结果。");
        return reply.toString();
    }

    private static String endpoint(String baseUrl) {
        String base = baseUrl.trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
    }

    private static String safe(String text) { return text == null ? "" : text; }
    private static String truncate(String text, int max) { return text.length() <= max ? text : text.substring(0, max); }

    public record ChatResult(String reply, List<ArticleRef> references, boolean degraded) {}
    public record ArticleRef(Long id, String title) {}
    public record Message(String role, String content) {}
}
