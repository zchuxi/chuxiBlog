-- 补全高频查询索引：为线上 schema 变更审阅用
-- 使用 ALGORITHM=INPLACE 减少锁表时间

CREATE INDEX idx_article_status_updated ON article (status, updatedAt);
CREATE INDEX idx_article_category ON article (categoryName);
CREATE INDEX idx_comment_article ON article_comment (articleId);
CREATE INDEX idx_barrage_approved ON barrage (approved, id);
