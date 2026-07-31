package com.chuxi.entity;

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
@Table(name = "article_comment", indexes = {
    @Index(name = "idx_comment_article", columnList = "articleId")
})
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long articleId;
    private String nickname;
    @Column(length = 1000)
    private String content;
    private Integer likeCount;
    private Boolean liked;
    private Boolean approved;
    private LocalDateTime createdAt;
}
