package com.chuxi.repo;

import com.chuxi.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArticleRepo extends JpaRepository<Article, Long> {
    java.util.List<Article> findByTitleContainingOrSummaryContaining(String t, String s);

    @Query("SELECT a FROM Article a WHERE a.status <> '草稿' " +
           "ORDER BY CASE WHEN a.pinned = true THEN 0 ELSE 1 END, a.updatedAt DESC NULLS LAST")
    Page<Article> findPublishedPage(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status <> '草稿' " +
           "ORDER BY CASE WHEN a.pinned = true THEN 0 ELSE 1 END, a.updatedAt DESC NULLS LAST")
    java.util.List<Article> findAllPublished();

    @Query("SELECT a FROM Article a WHERE a.status <> '草稿' " +
           "ORDER BY a.updatedAt DESC NULLS LAST")
    Page<Article> findAllPublishedByUpdatedAtDesc(Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.status <> '草稿' AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(a.tags) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
           "ORDER BY a.updatedAt DESC NULLS LAST")
    Page<Article> searchPublished(String kw, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE a.id < :currentId AND a.status <> '草稿' ORDER BY a.id DESC")
    Optional<Article> findPrevious(@Param("currentId") Long currentId);

    @Query("SELECT a FROM Article a WHERE a.id > :currentId AND a.status <> '草稿' ORDER BY a.id ASC")
    Optional<Article> findNext(@Param("currentId") Long currentId);
}
