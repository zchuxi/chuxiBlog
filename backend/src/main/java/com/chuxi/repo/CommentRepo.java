package com.chuxi.repo;

import com.chuxi.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CommentRepo extends JpaRepository<Comment, Long> {
    java.util.List<Comment> findByArticleIdAndApprovedTrueOrderByCreatedAtDesc(Long articleId);

    java.util.List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    void deleteByArticleId(Long articleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comment c WHERE c.id = :id")
    Optional<Comment> findByIdForUpdate(@Param("id") Long id);
}
