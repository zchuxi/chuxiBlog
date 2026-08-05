# 初曦の巣 — 后端 API 文档

> **Base URL**: `http://localhost:8080`（可通过环境变量 `SERVER_PORT` 覆盖）
> **前端代理**: 开发环境 `/api/*` → `http://localhost:8081`
> **文档版本**: 基于 Spring Boot 3.x (Jakarta EE)

---

## 目录

- [全局约定](#全局约定)
  - [统一响应格式](#统一响应格式-r)
  - [分页格式](#分页格式-pagedatat)
  - [认证机制](#认证机制)
  - [CORS 配置](#cors-配置)
- [一、认证模块](#一认证模块--authcontroller)
- [二、管理端通用 CRUD](#二管理端通用-crud--admincontentcontroller)
- [三、管理端概览](#三管理端概览--overviewcontroller)
- [四、管理端站点文案](#四管理端站点文案--sitecontentcontroller-管理端)
- [五、管理端媒体](#五管理端媒体--mediacontroller-管理端)
- [六、前台文章](#六前台文章--articlecontroller)
- [七、前台首页](#七前台首页--homecontroller)
- [八、前台番剧](#八前台番剧--bangumicontroller)
- [九、前台各版块](#九前台各版块--sectioncontroller)
- [十、前台站点文案与浏览量](#十前台站点文案与浏览量--sitecontentcontroller-前台)
- [十一、公开文件服务](#十一公开文件服务)
- [十二、RSS 订阅](#十二rss-订阅)
- [十三、实体字段清单](#十三实体字段清单)
- [十四、DTO 输出格式](#十四dto-输出格式)
- [十五、全局异常处理](#十五全局异常处理)
- [十六、安全机制汇总](#十六安全机制汇总)
- [十七、API 端点汇总清单](#十七api-端点汇总清单)

---

## 全局约定

### 统一响应格式 `R<T>`

```json
{
  "code": 0,
  "message": "ok",
  "data": "..."
}
```

| 字段    | 类型     | 说明                                  |
| ------- | -------- | ------------------------------------- |
| code    | int      | `0` = 成功，`400` = 业务失败，`401` = 未认证 |
| message | string   | 描述信息                              |
| data    | T (泛型) | 业务数据                              |

### 分页格式 `PageData<T>`

```json
{
  "records": [],
  "total": 100,
  "pageNo": 1,
  "pageSize": 10
}
```

### 认证机制

| 路径前缀              | 认证要求           |
| --------------------- | ------------------ |
| `/api/admin/**`       | 需要 `Authorization: Bearer <token>` |
| `/api/front/**`       | 无需认证           |
| `/api/rss`            | 无需认证           |
| `/api/music`          | 无需认证           |
| `/api/uploads/**`     | 无需认证           |

- **Token 有效期**: 7 天
- **登录限流**: 同一 IP 连续失败 5 次后锁定 15 分钟

### CORS 配置

- 允许来源通过环境变量 `APP_CORS_ALLOWED_ORIGINS` 配置（Spring relaxed binding 映射到 `app.cors.allowed-origins`），默认仅 `http://localhost:5173`；本地开发另经 `application-dev.yml` 追加 `http://localhost:3000`
- 允许方法: `GET, POST, PUT, DELETE, OPTIONS`

---

## 一、认证模块 — AuthController

> 基础路径: `/api/auth`

### 1.1 管理员登录

```
POST /api/auth/login
```

**认证**: 不需要

**请求体**:

```json
{
  "username": "admin",
  "password": "your-password"
}
```

**成功响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "displayName": "admin"
  }
}
```

> 登录成功不返回 token 明文：会话经 `Set-Cookie`（HttpOnly + SameSite=Strict + Path=/api）下发，前端只读 `displayName`。

**失败响应**:

```json
{ "code": 400, "message": "账号或密码错误" }
{ "code": 400, "message": "管理员密码未设置" }
{ "code": 400, "message": "失败次数过多，请 15 分钟后再试" }
```

**业务逻辑**:
- 用户名固定为 `admin`
- 密码使用 PBKDF2WithHmacSHA256 加盐哈希存储（120,000 次迭代，256-bit key）
- 历史明文密码在首次登录成功后自动升级为哈希
- 登录失败按 IP 限流：连续 5 次失败锁定 15 分钟

---

### 1.2 获取当前用户信息

```
GET /api/auth/me
```

**认证**: 需要 Bearer Token

**成功响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "username": "admin",
    "displayName": "admin"
  }
}
```

**失败响应**: HTTP 401

```json
{ "code": 401, "message": "未登录" }
```

---

### 1.3 修改密码

```
POST /api/auth/password
```

**认证**: 需要 Bearer Token

**请求体**:

```json
{
  "oldPassword": "old-password",
  "newPassword": "new-password"
}
```

**成功响应**:

```json
{ "code": 0, "message": "ok", "data": null }
```

**失败响应**:

```json
{ "code": 400, "message": "旧密码不正确" }
{ "code": 400, "message": "新密码至少 16 位" }
```

---

## 二、管理端通用 CRUD — AdminContentController

> 基础路径: `/api/admin`，全部需要 Bearer Token

统一资源分发控制器，通过 `{res}` 路径段路由到对应资源处理器，支持 **15 种资源类型**。

### 通用端点

#### 列表

```
GET /api/admin/{res}
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": [...] }
```

#### 创建

```
POST /api/admin/{res}
```

**请求体**: `Map<String, Object>`（JSON 对象，字段与实体属性对应）

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "..." : "创建的资源对象" } }
```

#### 更新

```
PUT /api/admin/{res}/{id}
```

**请求体**: `Map<String, Object>`

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "..." : "更新后的资源对象" } }
{ "code": 400, "message": "记录不存在" }
```

#### 删除

```
DELETE /api/admin/{res}/{id}
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": null }
```

> **特殊逻辑**: 删除文章时级联删除其所有评论

### 支持的 15 种资源

| 资源路径 | 实体类 | 自增ID | tags数组转换 | 降序排列 | 特殊逻辑 |
|----------|--------|:------:|:-----------:|:--------:|---------|
| `articles` | Article | ✗ | ✓ | ✗ | 删文章级联删评论 |
| `home-carousels` | HomeCarousel | ✗ | ✗ | ✗ | — |
| `collapse-cards` | CollapseCard | ✗ | ✗ | ✗ | — |
| `team-members` | TeamMember | ✗ | ✗ | ✗ | — |
| `archive-categories` | ArchiveCategory | ✓ | ✓ | ✗ | — |
| `timeline-carousels` | TimelineCarousel | ✗ | ✗ | ✗ | — |
| `timeline-events` | TimelineEvent | ✗ | ✗ | ✗ | — |
| `parallax-stories` | ParallaxStory | ✗ | ✗ | ✗ | — |
| `tool-sites` | ToolSite | ✗ | ✓ | ✗ | — |
| `barrages` | Barrage | ✓ | ✗ | ✓ | — |
| `called-texts` | CalledText | ✗ | ✗ | ✗ | — |
| `musics` | Music | ✗ | ✗ | ✗ | — |
| `comments` | Comment | ✓ | ✗ | ✓ | — |
| `bangumi-records` | BangumiRecord | ✓ | ✓ | ✓ | bgm.tv subjectId 去重 |
| `friend-links` | FriendLink | ✗ | ✗ | ✗ | — |

**番剧去重逻辑**: 创建/更新 `bangumi-records` 时，按 `subjectId` 检查是否已存在，若已收录返回 `"「xxx」已经收录过了"`。

**时间字段自动填充**: `createdAt` 为空时新建取当前时间、更新保留原值；`updatedAt` 为空时始终取当前时间。

**tags 字段**: 入参为数组 `["tag1","tag2"]`，存储为 CSV `"tag1,tag2"`；出参从 CSV 转回数组。

---

## 三、管理端概览 — OverviewController

### 获取后台概览统计

```
GET /api/admin/overview
```

**认证**: 需要 Bearer Token

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "articleCount": 10,
    "draftCount": 2,
    "categoryCount": 5,
    "tagCount": 20,
    "viewCount": 12345,
    "bangumiCount": 8,
    "toolCount": 15,
    "musicCount": 30,
    "carouselCount": 4,
    "collapseCardCount": 3,
    "timelineCount": 12,
    "commentCount": 50,
    "barrageCount": 100,
    "categoryDistribution": [
      { "name": "技术", "count": 5 }
    ]
  }
}
```

---

## 四、管理端站点文案 — SiteContentController (管理端)

### 4.1 获取全部文案列表

```
GET /api/admin/site-content
```

**认证**: 需要 Bearer Token

**响应**:

```json
{ "code": 0, "message": "ok", "data": [{ "id": 1, "contentKey": "home-landing", "contentJson": "...", "updatedAt": "..." }] }
```

> 过滤掉 `admin-password` 等受保护 key

### 4.2 更新文案 (Upsert)

```
PUT /api/admin/site-content/{key}
```

**认证**: 需要 Bearer Token

**请求体**:

```json
{ "contentJson": "..." }
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "id": 1, "contentKey": "home-landing", "contentJson": "...", "updatedAt": "..." } }
```

> **受保护 key 黑名单**: `admin-password`、`visitor-secret` — 不可通过此接口修改

---

## 五、管理端媒体 — MediaController (管理端)

### 5.1 上传文件

```
POST /api/admin/upload
Content-Type: multipart/form-data
```

**认证**: 需要 Bearer Token

**请求参数**: `file` — MultipartFile (form-data)

**成功响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "name": "uuid-cleaned.png",
    "url": "/api/uploads/uuid-cleaned.png",
    "size": 12345
  }
}
```

**文件类型白名单**:

| 类别 | 扩展名 |
|------|--------|
| 图片 | `.jpg` `.jpeg` `.png` `.gif` `.webp` `.bmp` |
| 音频 | `.mp3` `.ogg` `.wav` `.flac` `.aac` |

> 三重校验：扩展名 + MIME + Magic Number
> 存储策略：OSS 配置齐全时上传到 OSS，否则回退到本地 `uploads/`

### 5.2 取回外链图

```
POST /api/admin/media/fetch
```

**认证**: 需要 Bearer Token

**请求体**:

```json
{ "url": "https://..." }
```

**成功响应**: 同上传格式

> **安全限制**: 仅接受本站 OSS 公网域名（防 SSRF），禁止重定向跟随

### 5.3 媒体文件列表

```
GET /api/admin/media
```

**认证**: 需要 Bearer Token

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "name": "xxx.png", "url": "/api/uploads/xxx.png", "size": 12345, "lastModified": "2026-07-31T10:00:00" }
  ]
}
```

> OSS 对象 + 本地文件合并列表，按 `lastModified` 降序

### 5.4 删除媒体文件

```
DELETE /api/admin/media/{name}
```

**认证**: 需要 Bearer Token

**响应**:

```json
{ "code": 0, "message": "ok", "data": true }
```

> 防路径穿越校验

---

## 六、前台文章 — ArticleController

> 基础路径: `/api/front/articles`，无需认证

### 6.1 搜索/分页文章

```
GET /api/front/articles/search
```

**请求参数**:

| 参数     | 类型   | 默认值 | 说明         |
| -------- | ------ | ------ | ------------ |
| keyword  | string | `""`   | 搜索关键词   |
| pageNo   | int    | `1`    | 页码         |
| pageSize | int    | `6`    | 每页条数     |

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [
      { "id": 1, "title": "...", "summary": "...", "coverUrl": "...", "categoryId": 1, "categoryName": "技术", "tags": ["tag1"], "pinned": false, "createdAt": "...", "updatedAt": "..." }
    ],
    "total": 50,
    "pageNo": 1,
    "pageSize": 6
  }
}
```

> 搜索范围：标题、摘要、标签（LIKE 模糊匹配）
> 排序：置顶优先，其余按 `updatedAt` 降序

### 6.2 文章详情

```
GET /api/front/articles/{id}
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "article": {
      "id": 1, "title": "...", "summary": "...", "coverUrl": "...",
      "categoryId": 1, "categoryName": "技术", "tags": ["tag1", "tag2"],
      "content": "# Markdown content...", "createdAt": "...", "updatedAt": "..."
    },
    "prev": { "id": 0, "title": "上一篇文章" },
    "next": { "id": 2, "title": "下一篇文章" }
  }
}
```

> 草稿状态文章不可访问（返回"文章不存在"），同时返回上一篇/下一篇导航

### 6.3 获取文章评论列表

```
GET /api/front/articles/{id}/comments
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    { "id": 1, "articleId": 1, "nickname": "访客", "content": "好文章！", "likeCount": 5, "liked": false, "approved": true, "createdAt": "..." }
  ]
}
```

> 只返回已审核（`approved=true`）的评论，按 `createdAt` 降序

### 6.4 发表评论

```
POST /api/front/articles/{id}/comments
```

**限流**: 同 IP 60 秒内最多 3 次

**请求体**:

```json
{
  "nickname": "访客",
  "content": "好文章！"
}
```

| 字段     | 必填 | 约束         | 说明                       |
| -------- | ---- | ------------ | -------------------------- |
| content  | ✓    | 最多 500 字符 | 评论内容                   |
| nickname | ✗    | 最多 20 字符  | 为空时默认 `"访客"`        |

**成功响应**:

```json
{ "code": 0, "message": "ok", "data": { "id": 1, "articleId": 1, "nickname": "访客", "content": "好文章！", "likeCount": 0, "liked": false, "approved": true, "createdAt": "..." } }
```

> 安全措施：InputSanitizer 过滤 XSS 危险内容

### 6.5 点赞/取消点赞评论

```
POST /api/front/articles/comments/{commentId}/likes
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "id": 1, "likeCount": 6, "liked": true } }
{ "code": 400, "message": "评论不存在" }
```

> 切换式点赞

---

## 七、前台首页 — HomeController

> 基础路径: `/api/front/home`，无需认证

### 7.1 首页 Landing 数据

```
GET /api/front/home/landing
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "carousels": [{ "id": 1, "title": "...", "imageUrl": "...", "sortIndex": 1, "visible": true }],
    "collapseCards": [{ "id": 1, "title": "...", "description": "..." }],
    "articles": [{ "id": 1, "title": "...", "summary": "..." }],
    "stats": { "viewCount": 12345 }
  }
}
```

> `carousels` 过滤 `visible!=false` 按 `sortIndex` 降序；`articles` 最新 6 篇已发布

### 7.2 首页文章分页

```
GET /api/front/home/articles
```

**请求参数**:

| 参数     | 类型 | 默认值 |
| -------- | ---- | ------ |
| pageNo   | int  | `1`    |
| pageSize | int  | `10`   |

**响应**: 同 [6.1 搜索/分页文章](#61-搜索分页文章) 的 `PageData<ArticleItem>` 格式

### 7.3 团队成员列表

```
GET /api/front/home/team-members
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": [{ "id": 1, "displayName": "...", "roleCode": "...", "roleLabel": "...", "avatarUrl": "..." }] }
```

---

## 八、前台番剧 — BangumiController

> 无需认证

### 8.1 番剧列表

```
GET /api/front/bangumi
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1, "subjectId": 12345, "name": "Original Name", "nameCn": "中文名",
      "coverUrl": "...", "totalEps": 12, "watchedEps": 6, "status": "在看",
      "rating": 8, "score": 7.5, "airDate": "2026-01-01", "platform": "TV",
      "rank": 100, "ratingTotal": 5000, "summary": "简介...",
      "tags": ["热血", "冒险"], "category": "番剧", "sortIndex": 0,
      "createdAt": "...", "updatedAt": "..."
    }
  ]
}
```

> 过滤 `visible!=true`，按 `sortIndex` 升序 + `updatedAt` 降序

### 8.2 番剧详情

```
GET /api/front/bangumi/{id}
```

**响应**: 同单条番剧数据结构

---

## 九、前台各版块 — SectionController

> 无需认证

### 9.1 时间线 Landing

```
GET /api/front/timeline/landing
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "carousels": [{ "id": 1, "title": "...", "content": "...", "imageUrl": "..." }],
    "timelines": [{ "id": 1, "title": "...", "content": "...", "imageUrl": "...", "timelineDate": "2026-01-01" }]
  }
}
```

### 9.2 归档 Landing

```
GET /api/front/archive/landing
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "entries": [
      { "id": 1, "title": "...", "summary": "...", "category": "技术", "tags": ["tag1"], "publishedAt": "...", "readingTime": "5分钟", "mood": "开心" }
    ],
    "categories": [
      { "category": "tech", "title": "技术", "description": "...", "tags": ["Java", "Vue"] }
    ]
  }
}
```

> entries 排除草稿，按 `publishedAt` 降序

### 9.3 树洞弹幕列表

```
GET /api/front/tree-hole/barrages
```

**请求参数**:

| 参数     | 类型 | 默认值 |
| -------- | ---- | ------ |
| pageNo   | int  | `1`    |
| pageSize | int  | `50`   |

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [{ "id": 1, "nickname": "树友-0001", "mood": "轻声", "content": "...", "likeCount": 0, "liked": false, "approved": true, "createdAt": "..." }],
    "total": 200,
    "pageNo": 1,
    "pageSize": 50
  }
}
```

> 只返回 `approved=true`，按 `id` 降序

### 9.4 发表弹幕

```
POST /api/front/tree-hole/barrages
```

**限流**: 同 IP 60 秒内最多 3 次

**请求体**:

```json
{
  "nickname": "树友-0001",
  "content": "大家好",
  "mood": "轻声"
}
```

| 字段     | 必填 | 约束         | 说明                         |
| -------- | ---- | ------------ | ---------------------------- |
| content  | ✓    | 最多 500 字符 | 弹幕内容                     |
| nickname | ✗    | 最多 20 字符  | 为空时默认 `"树友-0001"`     |
| mood     | ✗    | 最多 10 字符  | 为空时默认 `"轻声"`          |

### 9.5 点赞/取消点赞弹幕

```
POST /api/front/tree-hole/barrages/{id}/likes
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "id": 1, "likeCount": 1, "liked": true } }
{ "code": 400, "message": "弹幕不存在" }
```

### 9.6 召唤文字列表

```
GET /api/front/tree-hole/called-texts
```

**请求参数**:

| 参数     | 类型 | 默认值 |
| -------- | ---- | ------ |
| pageNo   | int  | `1`    |
| pageSize | int  | `50`   |

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [{ "id": 1, "title": "...", "tag": "...", "content": "...", "summary": "...", "imageUrl": "...", "readTime": "...", "audioUrl": "...", "sortIndex": 1 }],
    "total": 30,
    "pageNo": 1,
    "pageSize": 50
  }
}
```

> 按 `sortIndex` 降序

### 9.7 视差故事列表

```
GET /api/front/parallax/stories
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": [{ "id": 1, "title": "...", "description": "...", "note": "...", "align": "center", "imageUrl": "...", "sortIndex": 1 }] }
```

> 按 `sortIndex` 升序

### 9.8 工具导航 Landing

```
GET /api/front/tools/landing
```

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": [
    {
      "id": 1, "websiteName": "工具名", "websiteDescription": "描述",
      "websiteUrl": "https://...", "categoryId": 1, "category": "开发",
      "iconUrl": "...", "imageUrl": "...", "tags": ["工具"],
      "highlight": "推荐", "featured": true, "createdAt": "...", "updatedAt": "..."
    }
  ]
}
```

> 按 `id` 升序

### 9.9 音乐列表

```
GET /api/music
```

**请求参数**:

| 参数     | 类型 | 默认值 |
| -------- | ---- | ------ |
| pageNo   | int  | `1`    |
| pageSize | int  | `10`   |

**响应**:

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "records": [{ "id": 1, "title": "歌曲名", "artist": "艺术家", "album": "专辑", "coverUrl": "...", "musicUrl": "...", "lyric": "...", "updatedAt": "..." }],
    "total": 30,
    "pageNo": 1,
    "pageSize": 10
  }
}
```

### 9.10 友链列表

```
GET /api/front/friend-links
```

**响应**: 直接返回 `List<FriendLink>`（**注意**: 未包裹在 `R` 中）

```json
[
  { "id": 1, "siteName": "友站", "siteUrl": "https://...", "logoUrl": "...", "description": "...", "sortIndex": 1, "visible": true }
]
```

> 只返回 `visible=true`，按 `sortIndex` 升序

---

## 十、前台站点文案与浏览量 — SiteContentController (前台)

> 无需认证

### 10.1 读取前台文案

```
GET /api/front/site-content/{key}
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "id": 1, "contentKey": "about", "contentJson": "...", "updatedAt": "..." } }
{ "code": 400, "message": "内容不存在" }
```

**公开 key 白名单**:

| Key | 说明 |
|-----|------|
| `home-landing` | 首页 Landing |
| `about` | 关于页 |
| `archive-hero` | 归档页头图 |
| `background-gallery` | 背景图库 |
| `site-views` | 站点浏览量 |
| `site-settings` | 站点设置 |
| `nav-menu` | 导航菜单 |
| `appearance-settings` | 外观设置 |
| `timeline-hero` | 时间线头图 |
| `treehole-config` | 树洞配置 |
| `parallax-config` | 视差配置 |
| `bangumi-hero` | 番剧页头图 |
| `calendar-hero` | 日历页头图 |
| `tool-hero` | 工具页头图 |

> 非白名单 key 与不存在返回相同响应

### 10.2 读取站点浏览量

```
GET /api/front/views
```

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "views": 12345 } }
```

### 10.3 浏览量 +1

```
POST /api/front/views/bump
```

**限流**: 同 IP 每小时最多生效 1 次

**响应**:

```json
{ "code": 0, "message": "ok", "data": { "views": 12346 } }
```

> 超限静默返回当前计数

---

## 十一、公开文件服务

### 读取本地上传文件

```
GET /api/uploads/{name}
```

**响应**: 原始文件流（二进制）

**响应头**:

```
Cache-Control: public, max-age=604800
```

> 缓存 7 天，防路径穿越校验

---

## 十二、RSS 订阅

### Atom Feed

```
GET /api/rss
```

**Content-Type**: `application/atom+xml;charset=UTF-8`

**响应**: Atom XML 格式，最新 20 篇已发布文章

---

## 十三、实体字段清单

### Article（`article` 表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | @Id | 非自增 |
| title | String | — | 标题 |
| summary | String | length=1000 | 摘要 |
| coverUrl | String | length=500 | 封面图 URL |
| categoryId | Long | — | 分类 ID |
| categoryName | String | — | 分类名 |
| archiveCategory | String | — | 归档分类 |
| tags | String | — | CSV 存储 |
| content | String | LONGTEXT | Markdown 正文 |
| readingTime | String | — | 阅读时间 |
| mood | String | length=500 | 心情 |
| status | String | 默认 `"已发布"` | `"已发布"` / `"草稿"` |
| pinned | Boolean | 默认 `false` | 是否置顶 |
| publishedAt | LocalDateTime | — | 发布时间 |
| createdAt | LocalDateTime | — | 创建时间 |
| updatedAt | LocalDateTime | — | 更新时间 |

### BangumiRecord（`bangumi_record` 表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | Long | @Id, IDENTITY | 自增 |
| subjectId | Long | unique | bgm.tv 条目 ID |
| name | String | — | 原名 |
| nameCn | String | — | 中文名 |
| coverUrl | String | length=500 | 封面 |
| totalEps | Integer | — | 总集数 |
| watchedEps | Integer | — | 已看集数 |
| status | String | — | 想看/在看/看过/搁置/弃番 |
| rating | Integer | — | 个人评分 0-10 |
| score | Double | — | bgm 站均分 |
| airDate | String | — | 开播日期 |
| platform | String | — | TV/剧场版/OVA |
| rank | Integer | column: `bgm_rank` | bgm 站排名 |
| ratingTotal | Integer | — | 评分人数 |
| summary | String | TEXT | 简介 |
| tags | String | — | CSV |
| category | String | — | 分类 |
| sortIndex | Integer | — | 排序值 |
| visible | Boolean | — | 前台是否展示 |
| createdAt | LocalDateTime | — | 创建时间 |
| updatedAt | LocalDateTime | — | 更新时间 |

### Comment（`article_comment` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id, IDENTITY |
| articleId | Long | — |
| nickname | String | — |
| content | String | length=1000 |
| likeCount | Integer | — |
| liked | Boolean | — |
| approved | Boolean | — |
| createdAt | LocalDateTime | — |

### Barrage（`barrage` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id, IDENTITY |
| userId | Long | — |
| nickname | String | — |
| mood | String | — |
| likeCount | Integer | — |
| liked | Boolean | — |
| content | String | length=500 |
| approved | Boolean | — |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### SiteContent（`site_content` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id, IDENTITY |
| contentKey | String | nullable=false, unique, length=128 |
| contentJson | String | LONGTEXT |
| updatedAt | LocalDateTime | — |

### HomeCarousel（`home_carousel` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| description | String | length=500 |
| content | String | length=1000 |
| imageUrl | String | length=500 |
| sortIndex | Integer | — |
| sceneLabel | String | — |
| kicker | String | — |
| badge | String | — |
| visible | Boolean | 默认 `true` |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### CollapseCard（`collapse_card` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| description | String | length=500 |
| content | String | length=1000 |
| imageUrl | String | length=500 |
| sortIndex | Integer | — |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### TeamMember（`team_member` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| displayName | String | — |
| email | String | — |
| avatarUrl | String | length=500 |
| roleCode | String | — |
| roleLabel | String | — |
| position | String | — |
| description | String | length=500 |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### ArchiveCategory（`archive_category` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id, IDENTITY |
| category | String | — |
| title | String | — |
| description | String | length=500 |
| tags | String | CSV |

### TimelineCarousel（`timeline_carousel` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| content | String | length=1000 |
| imageUrl | String | length=500 |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### TimelineEvent（`timeline_event` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| content | String | length=1000 |
| imageUrl | String | length=500 |
| timelineDate | LocalDate | — |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### ParallaxStory（`parallax_story` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| description | String | length=1000 |
| note | String | length=1000 |
| align | String | — |
| imageUrl | String | length=500 |
| sortIndex | Integer | — |

### ToolSite（`tool_site` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| websiteName | String | — |
| websiteDescription | String | length=500 |
| websiteUrl | String | length=500 |
| categoryId | Long | — |
| category | String | — |
| iconUrl | String | length=500 |
| imageUrl | String | length=500 |
| tags | String | CSV |
| highlight | String | — |
| featured | Boolean | — |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### CalledText（`called_text` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| tag | String | — |
| content | String | length=1000 |
| summary | String | length=500 |
| imageUrl | String | length=500 |
| readTime | String | — |
| audioUrl | String | length=500 |
| sortIndex | Integer | — |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

### Music（`music` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| title | String | — |
| artist | String | — |
| album | String | — |
| coverUrl | String | length=500 |
| musicUrl | String | length=500 |
| lyric | String | LONGTEXT |
| updatedAt | LocalDateTime | — |

### FriendLink（`friend_link` 表）

| 字段 | 类型 | 约束 |
|------|------|------|
| id | Long | @Id |
| siteName | String | length=128, nullable=false |
| siteUrl | String | length=512, nullable=false |
| logoUrl | String | length=512 |
| description | String | length=512 |
| sortIndex | Integer | — |
| visible | Boolean | — |
| createdAt | LocalDateTime | — |
| updatedAt | LocalDateTime | — |

---

## 十四、DTO 输出格式

### ArticleItem（列表视图）

```
id, title, summary, coverUrl, categoryId, categoryName,
tags: [String], pinned: boolean, createdAt, updatedAt
```

### ArticleDetail（详情视图）

```
id, title, summary, coverUrl, categoryId, categoryName,
tags: [String], content, createdAt, updatedAt
```

### ArchiveEntry（归档视图）

```
id, title, summary, category, tags: [String],
publishedAt, readingTime, mood
```

---

## 十五、全局异常处理

| 异常类型 | HTTP 状态码 | 响应 |
|----------|:----------:|------|
| `MethodArgumentNotValidException` | 400 | `{ "code": 400, "message": "字段: 错误信息" }` |
| `IllegalArgumentException` | 400 | `{ "code": 400, "message": "异常消息" }` |
| `EntityNotFoundException` | 404 | `{ "code": 400, "message": "资源不存在" }` |
| `Exception`（兜底） | 500 | `{ "code": 400, "message": "服务器内部错误" }` |

---

## 十六、安全机制汇总

| # | 机制 | 说明 |
|:-:|------|------|
| 1 | **认证** | `AdminAuthInterceptor` 拦截 `/api/admin/**`，Bearer Token 校验 |
| 2 | **密码** | PBKDF2WithHmacSHA256 加盐哈希（120,000 次迭代，256-bit key） |
| 3 | **登录限流** | 同 IP 连续 5 次失败锁定 15 分钟 |
| 4 | **接口限流** | 评论/弹幕 60 秒 3 次；浏览量 bump 每小时 1 次 |
| 5 | **XSS 防护** | InputSanitizer 过滤 `script` / `iframe` / `on*` 事件属性 |
| 6 | **文件上传** | 扩展名 + MIME + Magic Number 三重校验 |
| 7 | **路径穿越** | 文件名校验 + `resolveSafe` 确保落在 `uploads/` 内 |
| 8 | **SSRF 防护** | 外链抓取仅允许本站 OSS 域名，禁止重定向 |
| 9 | **敏感数据保护** | `admin-password` 不可通过管理端文案接口读写；前台文案 key 白名单机制 |

---

## 十七、API 端点汇总清单

### 公开接口（无需认证）

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 1 | POST | `/api/auth/login` | 管理员登录 |
| 2 | GET | `/api/front/articles/search` | 文章搜索/分页 |
| 3 | GET | `/api/front/articles/{id}` | 文章详情 |
| 4 | GET | `/api/front/articles/{id}/comments` | 文章评论列表 |
| 5 | POST | `/api/front/articles/{id}/comments` | 发表评论 (限流) |
| 6 | POST | `/api/front/articles/comments/{commentId}/likes` | 评论点赞切换 |
| 7 | GET | `/api/front/home/landing` | 首页 Landing 数据 |
| 8 | GET | `/api/front/home/articles` | 首页文章分页 |
| 9 | GET | `/api/front/home/team-members` | 团队成员列表 |
| 10 | GET | `/api/front/bangumi` | 番剧列表 |
| 11 | GET | `/api/front/bangumi/{id}` | 番剧详情 |
| 12 | GET | `/api/front/timeline/landing` | 时间线 Landing |
| 13 | GET | `/api/front/archive/landing` | 归档 Landing |
| 14 | GET | `/api/front/tree-hole/barrages` | 弹幕分页列表 |
| 15 | POST | `/api/front/tree-hole/barrages` | 发表弹幕 (限流) |
| 16 | POST | `/api/front/tree-hole/barrages/{id}/likes` | 弹幕点赞切换 |
| 17 | GET | `/api/front/tree-hole/called-texts` | 召唤文字列表 |
| 18 | GET | `/api/front/parallax/stories` | 视差故事列表 |
| 19 | GET | `/api/front/tools/landing` | 工具导航列表 |
| 20 | GET | `/api/front/friend-links` | 友链列表 |
| 21 | GET | `/api/front/site-content/{key}` | 读取前台文案 |
| 22 | GET | `/api/front/views` | 读取浏览量 |
| 23 | POST | `/api/front/views/bump` | 浏览量+1 (限流) |
| 24 | GET | `/api/music` | 音乐分页列表 |
| 25 | GET | `/api/rss` | Atom RSS 订阅 |
| 26 | GET | `/api/uploads/{name}` | 公开读取本地文件 |

### 管理接口（需 Bearer Token）

| # | 方法 | 路径 | 说明 |
|:-:|------|------|------|
| 27 | GET | `/api/auth/me` | 获取当前用户信息 |
| 28 | POST | `/api/auth/password` | 修改密码 |
| 29 | GET | `/api/admin/overview` | 后台概览统计 |
| 30 | GET | `/api/admin/site-content` | 文案列表 |
| 31 | PUT | `/api/admin/site-content/{key}` | 文案 Upsert |
| 32 | GET | `/api/admin/{res}` | 通用资源列表 (15种) |
| 33 | POST | `/api/admin/{res}` | 通用资源创建 |
| 34 | PUT | `/api/admin/{res}/{id}` | 通用资源更新 |
| 35 | DELETE | `/api/admin/{res}/{id}` | 通用资源删除 |
| 36 | POST | `/api/admin/upload` | 文件上传 |
| 37 | POST | `/api/admin/media/fetch` | 取回外链图 |
| 38 | GET | `/api/admin/media` | 媒体文件列表 |
| 39 | DELETE | `/api/admin/media/{name}` | 删除媒体文件 |

> `{res}` 可选值: `articles`, `home-carousels`, `collapse-cards`, `team-members`, `archive-categories`, `timeline-carousels`, `timeline-events`, `parallax-stories`, `tool-sites`, `barrages`, `called-texts`, `musics`, `comments`, `bangumi-records`, `friend-links`
