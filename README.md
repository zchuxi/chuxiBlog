# 初曦的窝（SpringBoot + Vue3）

以 https://blog.lin-xin.top 为视觉基准完整复刻的前后端分离博客。前端样式取自原站编译产物（去除 scoped 哈希后逐页复用），DOM 结构按原站渲染结果 1:1 重建，数据结构与原站 API 返回体完全一致。

## 技术栈

- **后端**：Spring Boot 3.3 + Spring Data JPA + MySQL 8，端口 `8081`
- **前端**：Vite 5 + Vue 3 + Vue Router + Pinia + Naive UI（HERO 轮播）+ marked / highlight.js（文章渲染），端口 `5173`

## 启动方式

1. 确保 MySQL 在 `localhost:3306`，root 密码 `1234`（数据库 `blog_db` 首次启动自动创建并注入种子数据）
2. 双击 `start-backend.bat`（首次会自动 `mvn package`；**务必用 java -jar 方式运行**，中文路径下 `mvn spring-boot:run` 会因 GBK argfile 报 ClassNotFoundException）
3. 双击 `start-frontend.bat`，浏览器打开 http://localhost:5173

> 8080 端口被本机另一旧进程占用，所以后端固定使用 8081（`vite.config.js` 已配好代理）。

## 提交纪律（约定）

- **重命名/移动类重构必须独立成提交**（包重命名、文件移动、目录调整等不与任何功能改动混在同一提交），保证 git 重命名检测与历史热点信号不断裂
- **功能特性按边界拆分提交**：一个提交只做一件事，互不相关的特性/修复各自成提交，使 `git log --oneline` 可按特性归因、可选择性回退
- 本约定仅对**后续提交**生效，不要求重写既有历史

## 改动后验证（约定）

- **后端改动后必须在 `backend/` 目录运行 `mvn test`**：冒烟测试 `BlogApplicationTests` 会完整加载 Spring 上下文（H2 内存库替代 MySQL，测试配置见 `src/test/resources/application.yml`，无需本地 MySQL/OSS），Bean 装配、实体映射、种子数据导入出错都会直接失败
- **前端改动后在 `frontend/` 目录运行 `npm run lint` 和 `npm run build`**，以 lint 与构建双通过作为机械检查（lint 为最小 eslint 配置，见 `frontend/eslint.config.js`，已纳入 pre-commit 钩子；build 耗时较长仍为手动约定；type 检查暂未引入，按需另行评估）
- **前端改动后在 `frontend/` 目录运行 `npm test`**：Node 内置测试器（`node --test`，Node ≥ 21，零额外依赖）跑 `src/**/*.test.js` 下的纯数据行为检查（当前覆盖 `resourceSchemas.js` 的 schema 结构约束：字段类型合法性、select 必填 options、batch/ratio/default 规则、columns 与字段的对应关系）；新增可被 Node 直接加载的纯逻辑模块时按同一约定补测试
- `start-backend.bat` 打包时保留 `-DskipTests`（启动提速），因此**跳过测试仅限启动脚本，提交前仍须手动跑 `mvn test`**
- **机械检查点（对应上面前两条）**：新克隆/新环境必须先运行一次 `scripts\install-git-hooks.bat` 安装仓库内版本化的 pre-commit 钩子（`scripts/git-hooks/pre-commit`，钩子不随 clone 自动生效），此后暂存区含 `backend/` 改动的提交自动执行 `mvn test`、含 `frontend/` 改动的提交自动执行 `npm run lint`，失败即阻断提交（紧急绕过 `git commit --no-verify`）；其余约定暂维持流程文档形式
- **CI 兜底执行点（不依赖本机安装）**：推送到 GitHub 或发起 PR 时，`.github/workflows/backend-ci.yml` 在 `backend/` 改动时自动运行 `mvn test`，`.github/workflows/frontend-ci.yml` 在 `frontend/` 改动时自动依次运行 `npm run lint`、`npm test` 与 `npm run build`（lint 命令与 pre-commit 钩子保持一致，test/build 为 CI 额外兜底）；本机钩子未安装或被 `--no-verify` 绕过时，CI 仍会拦截失败的改动（Gitee 远端不触发此工作流）

## 页面清单

| 路由 | 内容 |
| --- | --- |
| `/index` | 首页：HERO 五幕轮播、人员卡片、站点信号板（count-up）、折叠手风琴卡片、文章画廊（2/3/4/3 行式布局 + 加载更多） |
| `/article/:id` | 文章详情：封面 HERO、markdown 正文（代码高亮）、目录导航、评论区 |
| `/timeline` | 页面概览 + 顶部卡片轮播 + 故事时间线（滚动激活） |
| `/tree-hole` | 树洞弹幕流（可发送/点赞/暂停）+ 疗愈书架卡片 |
| `/parallax` | 17 屏滚动视差叙事（开场/15 个故事/告别屏） |
| `/archive` | 归档开场、概览统计、按年月归档 + 目录侧栏 |
| `/tool` | 工具地图：统计、搜索、分类筛选、精选站点、27 个站点卡片 |
| `/tool/:id` | 工具详情页：站点档案、亮点、同类推荐 |
| `/bangumi` | 番剧记录：封面卡、进度、评分、状态筛选、详情浮层（bgm.tv 数据） |
| `/components` | CX 组件展示页（button/message/popover/radio/switch/tag） |

全局：左上导航（滑动指示器）、暗色模式（日月过渡动画）、背景图轮换（横/竖屏两套 29 张）、底部音乐播放器、AI 助手侧栏（界面演示）、live2d 看板娘（Mashiro）、樱花特效、文章搜索浮层、设置弹窗、登录/注册弹窗（原创玻璃拟态设计，视觉演示）。

## 后台管理端

- 入口：`http://localhost:5173/admin`（或右上角头像菜单 →「后台管理」），账号 `admin` / `123456`
- 覆盖前台展示的全部内容：文章（含 markdown 正文宽抽屉）、首页轮播、折叠卡片、团队成员、归档分类、时间线轮播、时间线事件、视差故事、工具站点、树洞弹幕、疗愈文本、音乐曲库、评论管理、番剧记录（支持从 Bangumi 搜索导入），均支持增删改查
- 图片库：上传 / 复制链接 / 删除 / 自研裁切（比例预设，另存为新图），所有 image 字段可「从图库选择」；文件存 `backend/uploads/`，经 `/api/uploads/{name}` 公开访问
- 管理端支持暗色模式（侧栏底部切换，与前台主题联动）
- 换网站图标：把头像图存为项目根 `avatar.png`，运行 `pwsh scripts/make-favicon.ps1`（自动裁白边 + 圆形遮罩 → `frontend/public/favicon.png`）
- 管理 API：`POST /api/auth/login` 签发 token，`/api/admin/{resource}` 系列 REST 接口（Bearer 鉴权，未登录 401）；删除文章会级联删除其评论；tags 在 API 层为数组、库内为 CSV，由服务端互转

## 主要接口

统一返回体 `{code, message, data}`：

- `GET /api/front/home/landing` · `GET /api/front/home/articles` · `GET /api/front/home/team-members`
- `GET /api/front/articles/{id}` · `GET/POST /api/front/articles/{id}/comments` · `GET /api/front/articles/search`
- `GET /api/front/timeline/landing` · `GET /api/front/archive/landing`
- `GET/POST /api/front/tree-hole/barrages`（`POST .../{id}/likes`）· `GET /api/front/tree-hole/called-texts`
- `GET /api/front/parallax/stories` · `GET /api/front/tools/landing` · `GET /api/music`

种子数据在 `backend/src/main/resources/seed/*.json`（取自原站接口样例），表空时自动导入，可随意清库重建。

## 线上 schema 变更流程（审阅点）

`spring.jpa.hibernate.ddl-auto` 已改为环境变量注入（`${JPA_DDL_AUTO:update}`）：

- **本地**：缺省 `update`，实体改动自动同步到本地 `blog_db`，无需额外操作
- **线上**：由服务器 systemd unit 注入 `JPA_DDL_AUTO=validate`，启动时只校验实体与库结构是否一致，**绝不自动改表**；不一致时启动直接失败（SchemaManagementException），以此兜底拦截未经审阅的结构变更

因此**实体字段一旦变更（新增/删除/改名/改类型），上线前必须人工出具对应 DDL，先在线上库执行完毕，再启动新版本服务**，顺序不可颠倒。评审涉及 `backend/src/main/java/com/chuxi/entity/` 改动的提交时，须确认对应 DDL 已随变更给出。

> 后续项（未实施）：评估以线上既有 `flyway_schema_history` 表为基线重新启用 Flyway 纳管后续变更；涉及线上执行，需单独授权后再做。

## 线上部署

生产环境部署在阿里云（`106.14.202.90`），由 nginx 反向代理到本机 Spring Boot：

- **域名**：`https://www.chuxi.online`（主）；`https://chuxi.online` 同步可用
- **静态资源**：`/opt/chuxi/dist`（vite 构建产物），由 nginx 直接服务
- **后端**：`127.0.0.1:8080`，systemd unit `chuxi.service`，环境变量在 unit 内声明（DB/CORS/JWT/OSS/上传目录等）
- **数据库**：MySQL 8，库名 `chuxi`（`chuxi` / `chuxi123`）
- **对象存储**：阿里云 OSS 桶 `chuxisleep`（`oss-cn-beijing`），`/api/admin/media/fetch` 提供白名单 SSRF 安全代理供外链图传回站内
- **架构**：`nginx(:80/:443) → /api/* → 127.0.0.1:8080`
- **部署脚本**：`scripts/deploy/deploy_upload.py`（全量 jar+dist，重启服务）、`scripts/deploy/deploy_frontend_only.py`（仅前端，跳过重启）；依赖 `paramiko`，运行前设置环境变量 `SSH_PWD`
- **前端产物**：`vite.config.js` 已固定 `build.outDir=dist`，在 `frontend/` 运行 `npm run build` 后产物只落在 `frontend/dist/`；打包命令 `cd frontend && tar czf <DIST_TGZ> dist`（tar 顶层必须是 `dist/`）
- **DDL 流程**：线上 `JPA_DDL_AUTO=validate`，新表必须在重启前先在库内执行（参 `scripts/ddl-friend-link.sql` 风格），顺序不可颠倒
- **构建路径坑**：Maven 必须在**纯英文路径**（如 `D:/build/chuxi2-backend`）下构建，中文 `backend/` 目录会触发 GBK 乱码；产出 jar 路径通过 `JAR_LOCAL` 环境变量传给部署脚本
