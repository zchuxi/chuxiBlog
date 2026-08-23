# AI 后台配置 Implementation Plan

> **For implementer:** Use TDD throughout. Write failing test first. Watch it fail. Then implement.

**Goal:** 在后台提供安全的 AI 运行参数配置页面，并保留 API Key 环境变量注入。

**Architecture:** 复用 `SiteContent` 的 `ai-settings` 键保存非敏感覆盖项；新增受管理认证保护的 AI 配置 Controller 和前端 API；新增 `AiConfigPanel.vue` 并接入后台菜单与概览快捷操作。

**Tech Stack:** Spring Boot、JPA `SiteContent`、Vue 3、Axios、Node test。

---

### Task 1: 后端运行时配置与管理接口

**Files:**
- Create: `backend/src/main/java/com/chuxi/service/AiConfigService.java`
- Create: `backend/src/main/java/com/chuxi/web/AiAdminController.java`
- Create: `backend/src/test/java/com/chuxi/web/AiAdminApiTests.java`
- Modify: `backend/src/main/java/com/chuxi/config/AiProperties.java`

**Requirements:** GET 返回非敏感配置和 `apiKeyConfigured`；PUT 只接受 enabled/baseUrl/model/timeoutSeconds/maxContextArticles；持久化到 `ai-settings`；不接受或保存 apiKey；非法边界返回 400；配置在当前进程立即生效。

**Verify:** `mvn -q -Dtest=AiAdminApiTests test` then `mvn test`.

### Task 2: 后台配置面板与导航接入

**Files:**
- Create: `frontend/src/views/admin/AiConfigPanel.vue`
- Create: `frontend/src/views/admin/aiConfigPanel.test.js`
- Modify: `frontend/src/api/admin.js`
- Modify: `frontend/src/views/admin/AdminView.vue`
- Modify: `frontend/src/views/admin/adminMenu.js`
- Modify: `frontend/src/views/admin/DashboardPanel.vue`

**Requirements:** 页面沿用后台现有面板样式；提供开关、接口地址、模型、超时、上下文数量；显示密钥配置状态和安全说明；支持加载、保存、错误、未授权状态；概览快捷操作跳转到 `ai-config`；菜单进入后能渲染面板。

**Verify:** `node --test src/views/admin/aiConfigPanel.test.js`, `npm run lint`, `npm test`, `npm run build`。

### Task 3: 集成验证

**Files:**
- No new files.

**Verify:** `git diff --check`，检查未修改实体字段，无需 DDL；确认本地 `/admin` 页面可访问。
