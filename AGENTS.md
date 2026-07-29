# AGENTS

agent 最小约定入口。仅收录三条硬约束，完整表述以 README 对应章节为准，此处不重复维护细节。

## 1. 改动后验证

- 后端改动后必须在 `backend/` 目录运行 `mvn test`
- 前端改动后必须在 `frontend/` 目录运行 `npm run lint`、`npm test` 和 `npm run build`

详见 [README · 改动后验证（约定）](README.md#改动后验证约定)。

## 2. 实体改动须同步 DDL 审阅

- 涉及 `backend/src/main/java/com/chuxi/entity/` 的实体字段变更（新增/删除/改名/改类型），必须随变更给出对应 DDL 供人工审阅；线上为 `validate` 模式，不会自动改表

详见 [README · 线上 schema 变更流程（审阅点）](README.md#线上-schema-变更流程审阅点)。

## 3. 提交纪律

- 重命名/移动类重构必须独立成提交，不与功能改动混合；功能特性按边界拆分提交

详见 [README · 提交纪律（约定）](README.md#提交纪律约定)。
