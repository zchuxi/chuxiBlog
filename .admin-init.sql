-- 临时（云端预览）：初始化管理员密码记录，仅含 PBKDF2 哈希，不含明文
-- 幂等：记录已存在时跳过（与 scripts/init-admin-password.py 语义一致）
INSERT INTO site_content (content_key, content_json, views, updated_at)
SELECT 'admin-password', '{"password":"pbkdf2$120000$dKwbld7feSytyxXvowHK7Q==$vRZJeM5yRIGAgqARwSbV1Jd6aZRCE3KYutICxiCygRI="}', NULL, NOW()
WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE content_key = 'admin-password');
