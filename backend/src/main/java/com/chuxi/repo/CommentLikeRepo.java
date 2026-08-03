package com.chuxi.repo;

import com.chuxi.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentLikeRepo extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByCommentIdAndVisitorId(Long commentId, String visitorId);

    long deleteByCommentIdAndVisitorId(Long commentId, String visitorId);

    long deleteByCommentIdIn(Collection<Long> commentIds);

    @Query("SELECT l.commentId FROM CommentLike l WHERE l.visitorId = :visitorId AND l.commentId IN :commentIds")
    List<Long> findCommentIdsByVisitorIdAndCommentIdIn(@Param("visitorId") String visitorId,
                                                        @Param("commentIds") Collection<Long> commentIds);
}
