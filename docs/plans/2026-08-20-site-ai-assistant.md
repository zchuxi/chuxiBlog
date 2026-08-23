# 站内 AI 助手 Implementation Plan

> **For implementer:** Use TDD throughout. Write failing test first. Watch it fail. Then implement.

**Goal:** 实现可配置的站内 AI 问答，并在模型不可用时提供站内检索降级。

**Architecture:** Spring Boot 后端新增 AI 配置、服务和前台 controller；服务检索已发布文章并调用 OpenAI-compatible chat completions。Vue 弹窗改为真实请求，维护有限多轮消息和加载/错误状态。

**Tech Stack:** Java 17 `HttpClient`、Spring MVC/JPA/JUnit、Vue 3、Axios、Node `node:test`。

---

### Task 1: 后端 AI 配置与检索/降级服务

**Files:**
- Create: `backend/src/main/java/com/chuxi/config/AiProperties.java`
- Create: `backend/src/main/java/com/chuxi/service/AiChatService.java`
- Create: `backend/src/test/java/com/chuxi/service/AiChatServiceTests.java`
- Modify: `backend/src/main/resources/application.yml`

**Steps:**
1. 写测试：验证消息限制、未启用/无 key 时的文章检索降级、已发布文章上下文数量上限。
2. 运行 `mvn -q -Dtest=AiChatServiceTests test`，确认因类不存在或行为缺失而失败。
3. 添加配置类、文章检索和安全截断；用 JDK `HttpClient` 封装上游请求，异常统一降级。
4. 重新运行目标测试并确认通过。
5. 提交 `feat: add site ai chat service`。

### Task 2: 后端 AI Controller 与接口测试

**Files:**
- Create: `backend/src/main/java/com/chuxi/web/AiController.java`
- Create: `backend/src/test/java/com/chuxi/web/AiApiTests.java`

**Steps:**
1. 写 MockMvc 测试：合法请求返回统一响应，空消息/超长消息返回 400，限流路径可控。
2. 运行目标测试确认失败。
3. 实现 `POST /api/front/ai/chat`，复用 `ClientIpResolver`/`RateLimiter`，不暴露上游错误。
4. 运行目标测试确认通过。
5. 提交 `feat: expose site ai chat api`。

### Task 3: 前端 API 与聊天交互

**Files:**
- Modify: `frontend/src/api/index.js`
- Modify: `frontend/src/layout/components/AiChatPanel.vue`
- Create: `frontend/src/layout/components/aiChatState.js`
- Create: `frontend/src/layout/components/aiChatState.test.js`

**Steps:**
1. 写纯逻辑测试：消息裁剪、请求消息构造、重置和发送状态转换。
2. 运行 `node --test src/layout/components/aiChatState.test.js` 确认失败。
3. 添加 `api.aiChat`，将输入改为 textarea，接入真实请求、加载状态、错误降级、引用文章和自动滚动。
4. 运行目标测试确认通过。
5. 提交 `feat: connect ai chat panel to api`。

### Task 4: 全量验证

**Files:** 无新增。

**Steps:**
1. 在 `backend/` 运行 `mvn test`。
2. 在 `frontend/` 依次运行 `npm run lint`、`npm test`、`npm run build`。
3. 修复仅由本功能引入的问题，并记录验证结果。
