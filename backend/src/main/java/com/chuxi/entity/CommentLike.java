package com.chuxi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 匿名访客对评论的点赞记录；visitorId 由前端本地生成，不代表登录身份。 */
@Getter
@Setter
@Entity
@Table(name = "comment_like",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_like_visitor", columnNames = {"comment_id", "visitor_id"}),
        indexes = {
                @Index(name = "idx_comment_like_comment", columnList = "comment_id"),
                @Index(name = "idx_comment_like_visitor", columnList = "visitor_id")
        })
public class CommentLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "visitor_id", nullable = false, length = 64)
    private String visitorId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
