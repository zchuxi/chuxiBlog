# 初曦の窝

> 一个前后端分离的个人博客系统。Spring Boot 3 + Vue 3，带完整的内容管理后台。

[![backend-ci](https://github.com/zchuxi/chuxiBlog/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/zchuxi/chuxiBlog/actions/workflows/backend-ci.yml)
[![frontend-ci](https://github.com/zchuxi/chuxiBlog/actions/workflows/frontend-ci.yml/badge.svg)](https://github.com/zchuxi/chuxiBlog/actions/workflows/frontend-ci.yml)
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3.5-42b883)
![Vite](https://img.shields.io/badge/Vite-5-646CFF)

线上站点：<https://www.chuxi.online>

不只是「文章列表 + 详情页」。13 个前台页面各有独立的交互设计：17 屏滚动视差叙事、可发送可点赞的树洞弹幕流、接 bgm.tv 数据的追番记录、按年月归档的时间线。所有内容都能在后台改，不用碰代码。

## 功能

**前台**

- 文章系统：markdown 渲染 + 代码高亮 + 目录导航 + 评论与点赞
- 全站暗色模式，日月过渡动画，主题在前后台联动
- live2d 看板娘（可拖拽）、背景图轮换、底部音乐播放器、樱花特效
- 站内 AI 助手：以已发布文章作检索上下文，模型不可用时自动降级为站内搜索
- 番剧记录：接 bgm.tv 在线数据，后端三层缓存（内存 → 磁盘 → 直连兜底），浏览器无需代理
- RSS 订阅（`/api/rss`，Atom 格式）

**后台**

- 覆盖前台全部展示内容：文章、轮播、折叠卡片、团队成员、归档分类、时间线事件、视差故事、工具站点、树洞弹幕、疗愈文本、音乐曲库、评论、番剧记录、友情链接，均支持增删改查
- 13 个管理面板，含站点设置、导航菜单、外观、页面文案、AI 配置
- 图片库：上传 / 复制链接 / 删除 / 自研裁切（比例预设，可另存或覆盖原图），所有图片字段支持「从图库选择」
- 番剧可直接从 Bangumi 搜索导入，走后端代理缓存
- Cookie 鉴权：`HttpOnly` + `SameSite=Strict`，HTTPS 下自动 `Secure`，明文 token 不下发浏览器

## 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Spring Boot 3.3 · Java 17 · Spring Data JPA · MySQL 8 · Caffeine · Actuator |
| 前端 | Vue 3.5 · Vite 5 · Vue Router 4 · Pinia 2 |
| 渲染 | marked + highlight.js + DOMPurify（XSS 净化） |
| 看板娘 | pixi.js 7 + pixi-live2d-display |
| 存储 | 本地磁盘 / 阿里云 OSS（二选一，配置切换） |
| 安全 | OWASP HTML Sanitizer（白名单净化）· PBKDF2 口令哈希 |

后端 17 个实体对应的接口统一走 `{code, message, data}` 返回体。

## 快速开始

**环境要求**：JDK 17+、Node ≥ 22、npm ≥ 10、MySQL 8

```bash
git clone https://github.com/zchuxi/chuxiBlog.git
cd chuxiBlog
```

**1. 配置数据库**

复制 `.env.example` 为 `.env` 并填写，或直接用环境变量注入：

```bash
DB_URL=jdbc:mysql://127.0.0.1:3306/chuxi_db?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=your_user
DB_PASSWORD=your_password
```

库不存在会自动创建，表为空时自动导入 `backend/src/main/resources/seed/*.json` 的种子数据，可随意清库重建。

**2. 启动后端**（端口 `8081`）

```bash
cd backend
./mvnw spring-boot:run
```

Windows 用户可直接双击根目录 `start-backend.bat`。

**3. 启动前端**（端口 `5173`）

```bash
cd frontend
npm install
npm run dev
```

打开 <http://localhost:5173> 即可，Vite 已配好到 `8081` 的代理。

**4. 初始化管理员口令**

管理员账号固定为 `admin`，**密码无默认值**，必须先初始化：

```bash
python scripts/init-admin-password.py
```

交互式输入 ≥16 位口令，脚本按 PBKDF2 生成哈希写入数据库。仓库内不含任何明文口令。之后访问 <http://localhost:5173/admin> 登录。

## 页面

| 路由 | 内容 |
| --- | --- |
| `/index` | 首页：HERO 五幕轮播、人员卡片、站点信号板（count-up）、折叠手风琴、文章画廊 |
| `/article/:id` | 文章详情：封面 HERO、markdown 正文、目录导航、评论区 |
| `/timeline` | 顶部卡片轮播 + 故事时间线（滚动激活） |
| `/tree-hole` | 树洞弹幕流（发送 / 点赞 / 暂停）+ 疗愈书架 |
| `/parallax` | 17 屏滚动视差叙事 |
| `/archive` | 归档开场、概览统计、按年月归档 + 目录侧栏 |
| `/tool` · `/tool/:id` | 工具地图：统计、搜索、分类筛选、精选站点 / 站点详情 |
| `/bangumi` · `/bangumi/:id` | 番剧记录：进度、评分、状态筛选 / 番剧详情 |
| `/calendar` | 番剧更新日历 |
| `/about` | 关于页 |
| `/components` | CX 组件展示（button / message / popover / radio / switch / tag） |
| `/admin` | 管理后台 |

## 接口

统一返回体 `{code, message, data}`。完整清单见 [docs/API.md](docs/API.md)，常用的几个：

```
GET  /api/front/home/landing          首页聚合数据
GET  /api/front/home/articles         文章列表（分页）
GET  /api/front/articles/{id}         文章详情
GET  /api/front/articles/search       文章搜索
GET  /api/front/articles/{id}/comments
POST /api/front/tree-hole/barrages    发送弹幕
POST /api/front/ai/chat               AI 助手（最近 8 轮上下文）
GET  /api/rss                         Atom 订阅
GET  /actuator/health                 健康检查
```

管理接口在 `/api/admin/{resource}` 下，Cookie 鉴权，未登录返回 401。

## 配置

全部可选配置见 [.env.example](.env.example)。几组常用的：

**AI 助手**（默认关闭，不配置则降级为站内检索）

```bash
APP_AI_ENABLED=true
APP_AI_API_KEY=sk-xxx
APP_AI_BASE_URL=https://api.deepseek.com/v1   # OpenAI 兼容即可
APP_AI_MODEL=deepseek-chat
```

API key 仅后端读取，不下发浏览器。

**对象存储**（不启用则图片存本地 `backend/uploads/`，经 `/api/uploads/{name}` 访问）

```bash
APP_OSS_ENABLED=true
APP_OSS_ENDPOINT=https://oss-cn-beijing.aliyuncs.com
APP_OSS_BUCKET=your_bucket
```

**schema 管理**

`JPA_DDL_AUTO` 本地缺省 `update`（实体改动自动同步），生产应设为 `validate`：只校验不改表，不一致时启动直接失败，以此拦截未经审阅的结构变更。因此**生产环境的实体字段变更必须先人工执行 DDL，再启动新版本**。

## 开发约定

**改动后验证**

```bash
cd backend  && ./mvnw test      # 17 个测试类，含上下文加载冒烟 + 安全边界回归
cd frontend && npm run lint     # ESLint
cd frontend && npm run lint:css # Stylelint
cd frontend && npm test         # 24 个测试文件，node --test 零额外依赖
cd frontend && npm run build
```

后端测试用 H2 内存库，不需要本地 MySQL 或 OSS。

**本地钩子**（新克隆需手动装一次，git 不随 clone 分发钩子）

```bash
scripts\install-git-hooks.bat
```

装好后，暂存区含 `backend/` 改动自动跑 `mvn test`，含 `frontend/` 改动自动跑 `npm run lint`，失败阻断提交。

**CI 兜底**：即使本机没装钩子或用了 `--no-verify`，推送和 PR 仍会在 GitHub Actions 上跑同样的检查。后端流水线还包含一道「实体变更须随附 DDL」检查。

**提交纪律**

- 重命名 / 移动类重构独立成提交，不与功能改动混在一起，保证 git 重命名检测不断裂
- 功能按边界拆分，一个提交只做一件事，便于按特性归因和选择性回退

## 目录结构

```
backend/
  src/main/java/com/chuxi/
    entity/    17 个 JPA 实体
    repo/      Spring Data 仓库
    service/   业务逻辑
    web/       REST 控制器
    auth/      Cookie 鉴权
    init/      种子数据导入
  src/main/resources/seed/    种子 JSON
frontend/
  src/
    views/     页面组件（含 admin/ 下 13 个管理面板）
    components/
    assets/css/  按主题拆分的样式
    live2d/    看板娘
    stores/    Pinia
    api/
docs/          API 文档与设计方案
scripts/       初始化、部署、图标生成等脚本
```

## 安全

漏洞报告方式见 [SECURITY.md](SECURITY.md)。已落地的措施：PBKDF2 口令哈希、HttpOnly Cookie 鉴权、DOMPurify + OWASP Sanitizer 双层 XSS 净化、媒体抓取白名单（防 SSRF）、Actuator 仅暴露健康状态、生产默认只监听回环且不信任客户端转发头。

## 致谢

视觉设计以 [blog.lin-xin.top](https://blog.lin-xin.top) 为参考基准复刻，感谢原作者的设计。后台管理、AI 助手、番剧模块、树洞、组件库等功能为本项目自行实现。
