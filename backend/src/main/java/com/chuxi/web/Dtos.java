package com.chuxi.web;

import com.chuxi.entity.Article;
import com.chuxi.entity.ToolSite;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Dtos {
    private Dtos() {}

    public static List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) return List.of();
        return Arrays.stream(tags.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** 列表视图: /home/articles、/home/landing.articles、/articles/search */
    public record ArticleItem(Long id, String title, String summary, String coverUrl,
                              Long categoryId, String categoryName, List<String> tags,
                              boolean pinned, LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static ArticleItem of(Article a) {
            return new ArticleItem(a.getId(), a.getTitle(), a.getSummary(), a.getCoverUrl(),
                    a.getCategoryId(), a.getCategoryName(), splitTags(a.getTags()),
                    Boolean.TRUE.equals(a.getPinned()), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    /** 详情视图: /articles/{id} */
    public record ArticleDetail(Long id, String title, String summary, String coverUrl,
                                Long categoryId, String categoryName, List<String> tags,
                                String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        public static ArticleDetail of(Article a) {
            return new ArticleDetail(a.getId(), a.getTitle(), a.getSummary(), a.getCoverUrl(),
                    a.getCategoryId(), a.getCategoryName(), splitTags(a.getTags()),
                    a.getContent(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    /** 归档视图: /archive/landing.entries */
    public record ArchiveEntry(Long id, String title, String summary, String category,
                               List<String> tags, LocalDateTime publishedAt, String readingTime, String mood) {
        public static ArchiveEntry of(Article a) {
            return new ArchiveEntry(a.getId(), a.getTitle(), a.getSummary(), a.getArchiveCategory(),
                    splitTags(a.getTags()), a.getPublishedAt(), a.getReadingTime(), a.getMood());
        }
    }

    public static Map<String, Object> toolOf(ToolSite t) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("websiteName", t.getWebsiteName());
        m.put("websiteDescription", t.getWebsiteDescription());
        m.put("websiteUrl", t.getWebsiteUrl());
        m.put("categoryId", t.getCategoryId());
        m.put("category", t.getCategory());
        m.put("iconUrl", t.getIconUrl() == null ? "" : t.getIconUrl());
        m.put("imageUrl", t.getImageUrl() == null ? "" : t.getImageUrl());
        m.put("tags", splitTags(t.getTags()));
        m.put("highlight", t.getHighlight() == null ? "" : t.getHighlight());
        m.put("featured", Boolean.TRUE.equals(t.getFeatured()));
        m.put("createdAt", t.getCreatedAt());
        m.put("updatedAt", t.getUpdatedAt());
        return m;
    }
}
