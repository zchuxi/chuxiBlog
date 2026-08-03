-- ============================================================================
-- 管理员密码初始化模板（fail-closed：不包含任何可用口令，绝不覆写已有密码）
-- ============================================================================
-- 本文件是说明性模板，不得直接执行。请使用配套脚本生成含哈希的等价 SQL：
--
--     python scripts/init-admin-password.py
--
-- 脚本会：
--   1) 交互式读取两次新口令（不回显，要求 >= 16 位，脚本内不存储明文）；
--   2) 按后端 PasswordHasher（PBKDF2WithHmacSHA256, 120000 迭代）生成哈希；
--   3) 设置 DB_URL / DB_USERNAME / DB_PASSWORD 环境变量后可直连 MySQL 写入，
--      否则输出等价 SQL 供人工执行。
--
-- 安全约束：
--   * 本模板不含真实/占位默认口令，执行前必须由脚本替换哈希；
--   * 以下语句带 WHERE NOT EXISTS：密码记录已存在时不执行，绝不覆盖线上密码；
--   * 数据库口令仅从环境变量读取，不落库、不入日志、不入仓库。
-- ============================================================================

INSERT INTO site_content (content_key, content_json, updated_at)
SELECT 'admin-password', '{"password":"<由 init-admin-password.py 生成的 PBKDF2 哈希>"}', NOW()
WHERE NOT EXISTS (SELECT 1 FROM site_content WHERE content_key = 'admin-password');
