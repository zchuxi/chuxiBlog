-- 测试环境种子数据：明文密码 123456（PasswordHasher.matches 支持明文比较）
INSERT INTO site_content (content_key, content_json, updated_at)
VALUES ('admin-password', '{"password":"123456"}', NOW());
