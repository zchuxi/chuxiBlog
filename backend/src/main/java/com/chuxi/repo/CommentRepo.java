package com.chuxi.repo;

import com.chuxi.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepo extends JpaRepository<Comment, Long> {
    java.util.List<Comment> findByArticleIdAndApprovedTrueOrderByCreatedAtDesc(Long articleId);

    java.util.List<Comment> findByArticleIdOrderByCreatedAtDesc(Long articleId);

    void deleteByArticleId(Long articleId);

    @Modifying
    @Query("UPDATE Comment c SET c.liked = :liked, c.likeCount = :likeCount WHERE c.id = :id")
    int updateLike(@Param("id") Long id, @Param("liked") boolean liked, @Param("likeCount") int likeCount);
}
