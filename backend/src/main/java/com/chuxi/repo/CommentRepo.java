package com.chuxi.repo;

import com.chuxi.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepo extends JpaRepository<Comment, Long> {
    java.util.List<Comment> findByArticleIdAndApprovedTrueOrderByCreatedAtDesc(Long articleId);

    java.util.List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    void deleteByArticleId(Long articleId);
}
