INSERT INTO site_content (content_key, content_json, updated_at)
VALUES ('admin-password', '{"password":"admin1234"}', NOW())
ON DUPLICATE KEY UPDATE content_json='{"password":"admin1234"}', updated_at=NOW();
