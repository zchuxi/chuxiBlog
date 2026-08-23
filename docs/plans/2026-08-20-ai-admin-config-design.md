# AI 后台配置设计

## 目标

在 `/admin` 增加 AI 配置页面，让管理员查看和修改 AI 开关、兼容接口地址、模型、超时和站内文章上下文数量。API Key 继续只从环境变量读取，后台只显示是否已配置，不回显密钥。

## 架构

- 后端新增受 `/api/admin/**` 拦截器保护的 `AiAdminController`。
- 非敏感覆盖配置复用已有 `SiteContent` 的 `ai-settings` 键值记录，避免新增实体和 DDL。
- `AiProperties` 保留环境变量默认值；服务启动时加载 `ai-settings` 覆盖项，管理端保存后同步更新运行时配置。
- 配置读取响应只返回 `apiKeyConfigured`，不返回 `apiKey`。
- 前台 `/api/front/ai/chat` 继续复用 `AiChatService`，未配置或上游不可用时保留检索降级。

## 数据流

1. Spring 启动绑定 `APP_AI_*` 环境变量。
2. AI 配置服务读取 `site_content.ai-settings`，校验后覆盖非敏感字段。
3. 管理端 GET 获取当前配置和密钥配置状态。
4. 管理端 PUT 保存非敏感字段到 `ai-settings`，并立即更新内存配置。
5. 前台聊天服务读取同一个运行时配置，下一次请求立即生效。

## 安全与错误处理

- AI Key 永不写入 `ai-settings`，也不出现在响应或日志中。
- 管理接口校验 URL、模型、超时和上下文数量边界；非法输入返回 400。
- 配置记录损坏时忽略数据库覆盖，回退到环境变量配置。
- 管理页面显示加载、保存、失败和未配置状态。

## 验证

- 后端测试覆盖未认证拦截、配置读取脱敏、保存校验和运行时生效。
- 前端测试覆盖导航入口、表单字段和 API 调用契约。
- 按项目约定运行 `mvn test`、`npm run lint`、`npm test` 和 `npm run build`。
