-- 友情链接表（线上 validate 模式需手动执行）
CREATE TABLE IF NOT EXISTS friend_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    site_name VARCHAR(128) NOT NULL,
    site_url VARCHAR(512) NOT NULL,
    logo_url VARCHAR(512),
    description VARCHAR(512),
    sort_index INT DEFAULT 0,
    visible BOOLEAN DEFAULT TRUE,
    created_at DATETIME,
    updated_at DATETIME
);
