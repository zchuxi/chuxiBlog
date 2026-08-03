-- 评论点赞隔离：每个匿名访客对同一评论最多一条记录。
-- 执行前请先确认线上 article_comment.id 与 comment_like.comment_id 的类型一致。
CREATE TABLE IF NOT EXISTS comment_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    visitor_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_comment_like_visitor UNIQUE (comment_id, visitor_id),
    KEY idx_comment_like_comment (comment_id),
    KEY idx_comment_like_visitor (visitor_id),
    CONSTRAINT fk_comment_like_comment
        FOREIGN KEY (comment_id) REFERENCES article_comment (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
