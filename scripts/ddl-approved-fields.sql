-- 弹幕/评论新增 approved 审核字段（线上 validate 模式需手动执行）
-- 默认 TRUE：存量数据视为已审核，行为与上线前一致；前台查询仅展示 approved = TRUE
ALTER TABLE barrage ADD COLUMN approved BOOLEAN DEFAULT TRUE;
ALTER TABLE article_comment ADD COLUMN approved BOOLEAN DEFAULT TRUE;
UPDATE barrage SET approved = TRUE WHERE approved IS NULL;
UPDATE article_comment SET approved = TRUE WHERE approved IS NULL;
