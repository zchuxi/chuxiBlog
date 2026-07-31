package com.chuxi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "article", indexes = {
    @Index(name = "idx_article_status_updated", columnList = "status, updatedAt"),
    @Index(name = "idx_article_category", columnList = "categoryName")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(length = 1000)
    private String summary;
    @Column(length = 500)
    private String coverUrl;
    private Long categoryId;
    private String categoryName;
    private String archiveCategory;
    private String tags;
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    private String readingTime;
    @Column(length = 500)
    private String mood;
    /** 发布状态："已发布" / "草稿"，历史数据 null 视为已发布 */
    private String status = "已发布";
    /** 是否置顶，历史数据 null 视为不置顶 */
    private Boolean pinned = false;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
