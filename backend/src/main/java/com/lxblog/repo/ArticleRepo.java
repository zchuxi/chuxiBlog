package com.lxblog.repo;

import com.lxblog.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepo extends JpaRepository<Article, Long> {
    java.util.List<Article> findByTitleContainingOrSummaryContaining(String t, String s);
}
