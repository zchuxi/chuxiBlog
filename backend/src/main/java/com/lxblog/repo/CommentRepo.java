package com.lxblog.repo;

import com.lxblog.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepo extends JpaRepository<Comment, Long> {
    java.util.List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    void deleteByArticleId(Long articleId);
}
