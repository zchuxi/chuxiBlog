package com.chuxi.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 站点文案/键值内容：contentKey 唯一，contentJson 存 JSON 字符串 */
@Getter
@Setter
@Entity
@Table(name = "site_content")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SiteContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content_key", nullable = false, unique = true, length = 128)
    private String contentKey;

    @Column(columnDefinition = "LONGTEXT")
    private String contentJson;

    private LocalDateTime updatedAt;
}
