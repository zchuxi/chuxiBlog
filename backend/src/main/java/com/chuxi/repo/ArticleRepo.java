package com.chuxi.repo;

import com.chuxi.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ArticleRepo extends JpaRepository<Article, Long> {
    java.util.List<Article> findByTitleContainingOrSummaryContaining(String t, String s);

    @Query("SELECT a FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "ORDER BY CASE WHEN a.pinned = true THEN 0 ELSE 1 END, a.updatedAt DESC NULLS LAST")
    java.util.List<Article> findAllPublished();

    @Query("SELECT a FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(a.tags) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
           "ORDER BY a.updatedAt DESC NULLS LAST")
    Page<Article> searchPublished(String kw, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') AND a.id < :currentId ORDER BY a.id DESC")
    java.util.List<Article> findPrevPublished(Long currentId, Pageable pageable);

    @Query("SELECT a FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') AND a.id > :currentId ORDER BY a.id ASC")
    java.util.List<Article> findNextPublished(Long currentId, Pageable pageable);

    long countByStatus(String status);

    @Query("SELECT a.categoryName, COUNT(a) FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "AND a.categoryName IS NOT NULL AND a.categoryName <> '' " +
           "GROUP BY a.categoryName ORDER BY COUNT(a) DESC")
    java.util.List<Object[]> findCategoryDistribution();

    @Query("SELECT a.tags FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') AND a.tags IS NOT NULL")
    java.util.List<String> findPublishedTags();

    @Query("SELECT DISTINCT a.categoryName FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "AND a.categoryName IS NOT NULL AND a.categoryName <> ''")
    java.util.List<String> findDistinctPublishedCategoryNames();

    @Query("SELECT DISTINCT a.archiveCategory FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "AND a.archiveCategory IS NOT NULL AND a.archiveCategory <> ''")
    java.util.List<String> findDistinctPublishedArchiveCategories();

    @Query("SELECT a FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "ORDER BY a.publishedAt DESC NULLS LAST")
    java.util.List<Article> findAllPublishedOrderByPublishedAtDesc();

    /** 列表投影：不加载 LONGTEXT content，供首页/列表等只读场景避免全量正文入内存 */
    interface ArticleLite {
        Long getId();
        String getTitle();
        String getSummary();
        String getCoverUrl();
        Long getCategoryId();
        String getCategoryName();
        String getArchiveCategory();
        String getTags();
        Boolean getPinned();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
    }

    @Query("SELECT a.id AS id, a.title AS title, a.summary AS summary, a.coverUrl AS coverUrl, " +
           "a.categoryId AS categoryId, a.categoryName AS categoryName, a.archiveCategory AS archiveCategory, " +
           "a.tags AS tags, a.pinned AS pinned, a.createdAt AS createdAt, a.updatedAt AS updatedAt " +
           "FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "ORDER BY CASE WHEN a.pinned = true THEN 0 ELSE 1 END, a.updatedAt DESC NULLS LAST")
    java.util.List<ArticleLite> findPublishedLite();

    @Query("SELECT a.id AS id, a.title AS title, a.summary AS summary, a.coverUrl AS coverUrl, " +
           "a.categoryId AS categoryId, a.categoryName AS categoryName, a.archiveCategory AS archiveCategory, " +
           "a.tags AS tags, a.pinned AS pinned, a.createdAt AS createdAt, a.updatedAt AS updatedAt " +
           "FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "ORDER BY CASE WHEN a.pinned = true THEN 0 ELSE 1 END, a.updatedAt DESC NULLS LAST")
    Page<ArticleLite> findPublishedLitePage(Pageable pageable);

    @Query("SELECT a.id AS id, a.title AS title, a.summary AS summary, a.coverUrl AS coverUrl, " +
           "a.categoryId AS categoryId, a.categoryName AS categoryName, a.archiveCategory AS archiveCategory, " +
           "a.tags AS tags, a.pinned AS pinned, a.createdAt AS createdAt, a.updatedAt AS updatedAt " +
           "FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') " +
           "ORDER BY a.updatedAt DESC NULLS LAST")
    Page<ArticleLite> findPublishedLiteByUpdatedAtDesc(Pageable pageable);

    @Query("SELECT a.id AS id, a.title AS title, a.summary AS summary, a.coverUrl AS coverUrl, " +
           "a.categoryId AS categoryId, a.categoryName AS categoryName, a.archiveCategory AS archiveCategory, " +
           "a.tags AS tags, a.pinned AS pinned, a.createdAt AS createdAt, a.updatedAt AS updatedAt " +
           "FROM Article a WHERE (a.status IS NULL OR a.status <> '草稿') AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(a.summary) LIKE LOWER(CONCAT('%', :kw, '%')) " +
           "OR LOWER(a.tags) LIKE LOWER(CONCAT('%', :kw, '%'))) " +
           "ORDER BY a.updatedAt DESC NULLS LAST")
    Page<ArticleLite> searchPublishedLite(String kw, Pageable pageable);
}
