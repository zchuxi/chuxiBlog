package com.chuxi.repo;

import com.chuxi.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepo extends JpaRepository<Article, Long> {
    java.util.List<Article> findByTitleContainingOrSummaryContaining(String t, String s);
}
