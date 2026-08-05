# 安全策略（Security Policy）

## 报告安全问题

发现安全漏洞请通过私有渠道联系项目维护者，**不要**在公开 Issue 中披露。

- 提交前请附：漏洞描述、影响范围、复现步骤（可包含 PoC）、建议修复方向
- 修复确认前请勿公开漏洞细节（负责任的披露）

## 安全边界

| 边界 | 说明 |
| --- | --- |
| `/api/admin/**` | `AdminAuthInterceptor` 统一鉴权；会话为服务端持久化 token（`site_content`），HttpOnly + SameSite=Strict Cookie |
| `/api/auth/**` | 登录失败锁定（5 次 / 15 分钟）+ 按 IP 速率预算；修改密码后吊销全部旧会话 |
| 匿名交互 | 评论/弹幕/点赞/浏览量开放；点赞身份为服务端 HMAC 签发的 visitor token（客户端无法自行构造），并按 IP 限流 |
| 评论读取 | 服务端强制分页（单页 ≤ 50 条），响应有界 |
| Markdown | `marked` 渲染 + `DOMPurify.sanitize` 后再 `v-html` |
| 媒体上传 | 扩展名 + MIME + Magic Number + 路径 containment 四重校验 |
| IP 限流 | `ClientIpResolver` 默认 fail-closed（不信任转发头）；生产启用 `APP_TRUST_PROXY=true` 时必须由 nginx 覆盖转发头 |

## 依赖与部署

- 后端依赖审计建议在每次发布前运行（如 `mvn dependency-check` / SCA 扫描）
- 部署安全响应头（HSTS / CSP / X-Content-Type-Options 等）由 nginx 层配置，需在部署清单中验证
- 线上 `JPA_DDL_AUTO=validate`：任何实体变更必须先人工审阅 DDL 再发布
