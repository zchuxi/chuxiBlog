-- 补全高频查询索引：为线上 schema 变更审阅用
-- 使用 ALGORITHM=INPLACE 减少锁表时间
-- 注意：列名必须与线上实际列一致（Spring 默认 snake_case 命名策略），camelCase 会导致执行失败

CREATE INDEX idx_article_status_updated ON article (status, updated_at);
CREATE INDEX idx_article_category ON article (category_name);
CREATE INDEX idx_comment_article ON article_comment (article_id);
CREATE INDEX idx_barrage_approved ON barrage (approved, id);
