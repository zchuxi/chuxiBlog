# 初曦の巢 — 项目完整参考文档

> 本文档涵盖项目架构、开发指南、数据模型、安全机制、部署运维等全部内容。
>
> API 接口文档请参阅 [API.md](./API.md)

## 目录

- [第一章：项目概述](#第一章项目概述)
- [第二章：技术栈](#第二章技术栈)
  - [2.1 后端](#21-后端)
  - [2.2 前端](#22-前端)
  - [2.3 部署/运维](#23-部署运维)
- [第三章：项目结构](#第三章项目结构)
- [第四章：开发环境搭建](#第四章开发环境搭建)
  - [4.1 前置条件](#41-前置条件)
  - [4.2 后端启动](#42-后端启动)
  - [4.3 前端启动](#43-前端启动)
  - [4.4 Git Hooks 安装](#44-git-hooks-安装)
  - [4.5 注意事项](#45-注意事项)
- [第五章：后端架构](#第五章后端架构)
  - [5.1 分层结构](#51-分层结构)
  - [5.2 统一响应格式](#52-统一响应格式)
  - [5.3 分页格式](#53-分页格式)
  - [5.4 全局异常处理](#54-全局异常处理)
  - [5.5 CORS 配置](#55-cors-配置)
- [第六章：前端架构](#第六章前端架构)
  - [6.1 路由系统](#61-路由系统)
  - [6.2 状态管理（Pinia）](#62-状态管理pinia)
  - [6.3 布局系统](#63-布局系统)
  - [6.4 CX 组件库](#64-cx-组件库)
  - [6.5 管理后台组件](#65-管理后台组件)
  - [6.6 API 层设计](#66-api-层设计)
  - [6.7 CSS 方案](#67-css-方案)
  - [6.8 自定义指令](#68-自定义指令)
  - [6.9 工具函数](#69-工具函数)
- [第七章：前台页面说明](#第七章前台页面说明)
  - [7.1 页面总览](#71-页面总览)
  - [7.2 首页（HomeView）](#72-首页homeview)
  - [7.3 文章详情（ArticleView）](#73-文章详情articleview)
  - [7.4 树洞弹幕（TreeHoleView）](#74-树洞弹幕treeholeview)
  - [7.5 番剧相关](#75-番剧相关bangumiview--bangumidetailview--calendarview)
- [第八章：数据模型](#第八章数据模型)
  - [8.1 实体关系概览](#81-实体关系概览)
  - [8.2 实体字段明细](#82-实体字段明细)
- [第九章：SiteContent 键值存储](#第九章sitecontent-键值存储)
  - [9.1 机制说明](#91-机制说明)
  - [9.2 已使用的 Key 清单](#92-已使用的-key-清单)
- [第十章：API 鉴权机制](#第十章api-鉴权机制)
  - [10.1 认证流程](#101-认证流程)
  - [10.2 Token 生命周期](#102-token-生命周期)
  - [10.3 接口权限分类](#103-接口权限分类)
- [第十一章：安全机制](#第十一章安全机制)
  - [11.1 密码存储](#111-密码存储)
  - [11.2 XSS 防护](#112-xss-防护)
  - [11.3 频率限制](#113-频率限制)
  - [11.4 CORS 安全](#114-cors-安全)
- [第十二章：种子数据](#第十二章种子数据)
- [第十三章：测试策略](#第十三章测试策略)
  - [13.1 后端测试](#131-后端测试)
  - [13.2 前端测试](#132-前端测试)
- [第十四章：CI/CD](#第十四章cicd)
  - [14.1 后端 CI](#141-后端-ci)
  - [14.2 前端 CI](#142-前端-ci)
  - [14.3 实体-DDL 同步门禁](#143-实体-ddl-同步门禁)
- [第十五章：部署指南](#第十五章部署指南)
  - [15.1 部署架构](#151-部署架构)
  - [15.2 前端部署](#152-前端部署)
  - [15.3 nginx 配置](#153-nginx-配置)
  - [15.4 后端 systemd 服务](#154-后端-systemd-服务)
- [第十六章：配置参考](#第十六章配置参考)
  - [16.1 后端环境变量](#161-后端环境变量)
  - [16.2 前端环境变量](#162-前端环境变量)

---

## 第一章：项目概述

| 属性 | 说明 |
|------|------|
| **项目名称** | 初曦の巣（chuxi-nest） |
| **定位** | 以 `https://blog.lin-xin.top` 为视觉基准 1:1 复刻的前后端分离个人博客系统 |
| **线上地址** | `https://www.chuxi.online` |
| **架构模式** | 前后端分离 — Vue 3 SPA + Spring Boot REST API |

### 核心功能清单

**前台展示**

| 功能模块 | 说明 |
|----------|------|
| 首页 HERO 轮播 | 全屏轮播图 + 折叠卡片 + 最新文章 + 团队成员展示 |
| 文章详情 | Markdown 渲染 + 代码语法高亮 + 评论系统 + 上下篇导航 |
| 时间线 | 轮播图 + 时间轴事件列表 |
| 树洞弹幕 | 弹幕发送 + 弹幕列表 + 疗愈文本 |
| 视差滚动叙事 | 多段视差滚动故事 |
| 归档 | 全部文章按时间排列 + 分类筛选 |
| 工具地图 | 工具站点导航 |
| 番剧记录 | 番剧收藏与追番列表 |
| 番剧日历 | 番剧更新日历 |
| CX 组件展示 | 自研 UI 组件库展示页 |
| 关于页 | 关于本站 + 友情链接 |

**后台管理**

- 15 种资源类型的完整 CRUD 管理（文章、轮播、折叠卡片、团队成员、归档分类、时间线轮播、时间线事件、视差故事、工具站点、弹幕、疗愈文本、音乐、评论、番剧记录、友情链接）
- 站点内容管理（SiteContent）：通过 key-value 方式管理页面文案
- 仪表盘概览统计
- 外观设置、背景图库管理、导航菜单配置
- 图片库管理 + 图片裁切上传
- 密码修改

**全局特性**

| 特性 | 说明 |
|------|------|
| 暗色模式 | 日/月过渡动画，一键切换 light/dark 主题 |
| 背景图轮换 | 横屏 13 张 + 竖屏 16 张，共 29 张背景图轮播 |
| 底部音乐播放器 | 全局音乐播放条 |
| AI 助手侧栏 | AI 聊天面板 |
| Live2D 看板娘 | Mashiro 模型，动态交互 |
| 樱花特效 | Canvas 樱花飘落动画 |
| 文章搜索 | 全局搜索浮层 |
| 登录/注册弹窗 | 玻璃拟态设计风格 |

---

## 第二章：技术栈

### 2.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行时 |
| Spring Boot | 3.3.5 | 应用框架 |
| Spring Data JPA | （Spring Boot 管理） | ORM / 数据访问 |
| Spring Boot Starter Web | （Spring Boot 管理） | Web 服务 + 内嵌 Tomcat |
| Spring Boot Starter Validation | （Spring Boot 管理） | 参数校验（`@Valid`） |
| MySQL Connector/J | （Spring Boot 管理） | MySQL 8 驱动 |
| H2 Database | （test scope） | 测试内存数据库 |
| Lombok | （Spring Boot 管理） | 样板代码生成（`@Data`、`@Builder` 等） |
| 阿里云 OSS SDK | 3.18.1 | 媒体文件对象存储 |
| JAXB API | 2.3.1 | OSS SDK 依赖（JDK 17 不再内置 JAXB） |
| JAXB Runtime | 2.3.3 | JAXB 运行时实现 |
| Activation | 1.1.1 | JAXB 依赖 |
| Spring Boot Starter Test | （test scope） | 测试框架（JUnit 5 + MockMvc） |
| Maven | — | 构建工具 |

### 2.2 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.13 | UI 框架（Composition API） |
| Vue Router | 4.4.5 | 路由管理（HTML5 History 模式） |
| Pinia | 2.2.6 | 状态管理 |
| Vite | 5.4.11 | 构建工具 + 开发服务器 |
| Naive UI | 2.40.1 | UI 组件库（后台管理使用） |
| Axios | 1.7.7 | HTTP 客户端 |
| marked | 14.1.3 | Markdown 解析 |
| highlight.js | 11.10.0 | 代码语法高亮 |
| DOMPurify | 3.4.12 | HTML 净化（防 XSS） |
| ESLint | 10.8.0 | 代码检查 |
| eslint-plugin-vue | 10.10.0 | Vue lint 规则 |
| @vitejs/plugin-vue | 5.2.0 | Vite 的 Vue SFC 支持 |

### 2.3 部署/运维

| 技术 | 用途 |
|------|------|
| Python + paramiko | SSH 部署脚本（前端上传 + 后端重启） |
| nginx | 反向代理 + 静态资源服务 |
| systemd | 后端进程管理（`chuxi-backend.service`） |
| GitHub Actions | CI/CD（前端构建检查 + 后端 Maven 测试） |
| 阿里云 ECS | 应用服务器 |
| 阿里云 OSS | 媒体文件存储（图片、音频） |

---

## 第三章：项目结构

```
d:\workspace\网站2\
├── backend/                              # 后端 Spring Boot 项目
│   ├── src/main/java/com/chuxi/
│   │   ├── ChuxiApplication.java         # Spring Boot 启动类
│   │   ├── auth/                         # 认证模块（4 文件）
│   │   │   ├── AuthController.java       #   登录/修改密码接口
│   │   │   ├── AdminAuthInterceptor.java #   Bearer 鉴权拦截器
│   │   │   ├── PasswordHasher.java       #   PBKDF2 加盐哈希
│   │   │   └── TokenStore.java           #   持久化 Token 存储
│   │   ├── common/                       # 通用工具（5 文件）
│   │   │   ├── R.java                    #   统一返回体
│   │   │   ├── PageData.java             #   分页封装
│   │   │   ├── ClientIpResolver.java     #   IP 解析（X-Forwarded-For）
│   │   │   ├── InputSanitizer.java       #   输入净化（XSS 防护）
│   │   │   └── RateLimiter.java          #   频率限制器
│   │   ├── config/                       # 配置类（3 文件）
│   │   │   ├── CorsConfig.java           #   CORS + 拦截器注册
│   │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│   │   │   └── OssProperties.java        #   OSS 配置属性
│   │   ├── entity/                       # JPA 实体（16 文件）
│   │   ├── init/                         # 数据初始化
│   │   │   └── DataInitializer.java      #   种子数据导入
│   │   ├── repo/                         # Spring Data JPA Repository（16 文件）
│   │   └── web/                          # Controller 层（13 文件）
│   │       ├── HomeController.java       #   首页数据
│   │       ├── ArticleController.java    #   文章 + 评论
│   │       ├── SectionController.java    #   分区内容
│   │       ├── BangumiController.java    #   番剧
│   │       ├── MediaController.java      #   媒体管理（上传/删除/列表）
│   │       ├── OssStorageService.java    #   阿里云 OSS 服务
│   │       ├── AdminContentController.java # 通用 CRUD 控制器
│   │       ├── SiteContentController.java  # 站点内容（key-value）
│   │       ├── OverviewController.java   #   概览统计（仪表盘）
│   │       ├── RssController.java        #   RSS 订阅
│   │       ├── Dtos.java                 #   DTO 转换
│   │       ├── BarrageRequest.java       #   弹幕请求体
│   │       └── CommentRequest.java       #   评论请求体
│   ├── src/main/resources/
│   │   ├── seed/                         # 种子数据（14 个 JSON 文件）
│   │   └── application.yml               # 应用配置（端口、数据库、OSS）
│   ├── src/test/                         # 测试代码
│   │   ├── java/com/chuxi/               #   测试类
│   │   └── resources/
│   │       ├── application.yml           #   测试配置（H2 内存库）
│   │       └── data.sql                  #   测试数据
│   ├── uploads/                          # 本地文件存储（开发环境）
│   └── pom.xml                           # Maven 配置
├── frontend/                             # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                          # API 调用层
│   │   │   ├── index.js                  #   前台 API（公开接口）
│   │   │   └── admin.js                  #   后台 API（Bearer 鉴权）
│   │   ├── assets/
│   │   │   ├── css/                      # 样式文件（19 个）
│   │   │   ├── sakura.png                # 樱花花瓣图片
│   │   │   └── svg-sprite.svg            # SVG 图标精灵
│   │   ├── components/
│   │   │   ├── cx/                       # CX 自研组件库（8 个）
│   │   │   ├── CxSection.vue             # 分区容器组件
│   │   │   └── SvgIcon.vue               # SVG 图标组件
│   │   ├── directives/
│   │   │   └── reveal.js                 # v-reveal 滚动揭示指令
│   │   ├── layout/
│   │   │   ├── components/               # 布局子组件（7 个）
│   │   │   └── LayoutView.vue            # 主布局
│   │   ├── router/
│   │   │   └── index.js                  # 路由配置
│   │   ├── stores/
│   │   │   ├── settings.js               # 全局设置 Store
│   │   │   └── settings.test.js          # Store 单元测试
│   │   ├── utils/
│   │   │   ├── display.js                # 显示工具函数
│   │   │   ├── display.test.js           # 工具函数测试
│   │   │   ├── markdown.js               # Markdown 渲染
│   │   │   ├── markdown.test.js          # Markdown 测试
│   │   │   └── svgSprite.js              # SVG Sprite 注入
│   │   ├── views/
│   │   │   ├── admin/                    # 管理后台页面（22 个）
│   │   │   ├── HomeView.vue              # 首页
│   │   │   ├── ArticleView.vue           # 文章详情
│   │   │   ├── TimelineView.vue          # 时间线
│   │   │   ├── TreeHoleView.vue          # 树洞弹幕
│   │   │   ├── ParallaxView.vue          # 视差滚动
│   │   │   ├── ArchiveView.vue           # 归档
│   │   │   ├── ToolView.vue              # 工具地图
│   │   │   ├── ToolDetailView.vue        # 工具详情
│   │   │   ├── BangumiView.vue           # 番剧记录
│   │   │   ├── BangumiDetailView.vue     # 番剧详情
│   │   │   ├── CalendarView.vue          # 番剧日历
│   │   │   ├── ComponentsShowView.vue    # CX 组件展示
│   │   │   └── AboutView.vue             # 关于
│   │   ├── App.vue                       # 根组件
│   │   └── main.js                       # 入口文件
│   ├── public/                           # 静态资源
│   │   ├── image/bg/                     # 背景图（Landscape 13 + Vertical 16）
│   │   ├── image/seed/                   # 种子图片
│   │   ├── live2d/                       # Live2D 看板娘（Mashiro 模型）
│   │   ├── favicon.png                   # 站点图标
│   │   ├── robots.txt                    # 搜索引擎爬虫规则
│   │   └── sitemap.xml                   # 站点地图
│   ├── package.json                      # npm 配置
│   ├── vite.config.js                    # Vite 配置（代理、别名）
│   └── eslint.config.js                  # ESLint 配置
├── scripts/                              # 脚本工具
│   ├── ci/                               # CI 检查脚本
│   │   └── check-entity-ddl.sh           #   实体-DDL 同步门禁
│   ├── deploy/                           # 部署脚本
│   │   ├── deploy_frontend_only.py       #   前端部署（SSH 上传 + nginx 重载）
│   │   └── deploy_upload.py              #   上传文件部署
│   ├── git-hooks/                        # Git 钩子
│   │   └── pre-commit                    #   提交前检查
│   ├── ddl-approved-fields.sql           # DDL 审批字段
│   ├── ddl-friend-link.sql               # 友情链接 DDL
│   ├── init-admin-password.sql           # 初始管理员密码
│   ├── install-git-hooks.bat             # Git Hooks 安装脚本
│   └── make-favicon.ps1                  # favicon 生成脚本
├── docs/                                 # 文档
│   ├── screenshots/                      # 项目截图
│   └── API.md                            # API 接口文档
├── .github/workflows/                    # CI/CD 配置
│   ├── backend-ci.yml                    # 后端 CI（Maven test）
│   └── frontend-ci.yml                   # 前端 CI（lint + build）
├── start-backend.bat                     # 后端启动脚本（Windows）
├── start-frontend.bat                    # 前端启动脚本（Windows）
├── README.md                             # 项目说明
└── AGENTS.md                             # AI 代理约定
```

---

## 第四章：开发环境搭建

### 4.1 前置条件

| 工具 | 最低版本 | 说明 |
|------|----------|------|
| JDK | 17+ | 后端编译运行 |
| Maven | 3.6+ | 后端构建 |
| Node.js | 22+ | 前端构建 |
| npm | 10+ | 前端包管理 |
| MySQL | 8 | 生产数据库（测试用 H2 内存库可跳过） |

### 4.2 后端启动

1. 确保 MySQL 在 `localhost:3306` 运行，创建数据库 `chuxi`
2. 双击 `start-backend.bat`，脚本会自动：
   - 检测 Maven 是否可用
   - 执行 `mvn -DskipTests package` 编译打包
   - 以 `java -Dfile.encoding=UTF-8 -jar target/chuxi-backend.jar` 启动
3. 服务运行在 **端口 8081**
4. 首次启动时，`DataInitializer` 自动创建数据库表并导入 `resources/seed/` 下的 14 个 JSON 种子数据文件

> **提示**：Maven 必须在纯英文路径下构建，中文路径会导致编译失败。

### 4.3 前端启动

1. 双击 `start-frontend.bat`，脚本会自动：
   - 检测 Node.js 是否可用
   - 执行 `npm install` 安装依赖
   - 运行 `npm run dev` 启动开发服务器
2. 开发服务器运行在 **端口 5173**
3. 访问前台：`http://localhost:5173`
4. 访问管理后台：`http://localhost:5173/admin`

### 4.4 Git Hooks 安装

```bash
scripts\install-git-hooks.bat
```

安装后，每次 `git commit` 前会自动执行 `pre-commit` 钩子，进行代码检查。

### 4.5 注意事项

| 项目 | 说明 |
|------|------|
| Maven 路径 | 必须在纯英文路径下构建，中文路径会报错 |
| 前端开发代理 | `/api/*` 请求自动代理到 `http://localhost:8081` |
| 自定义代理目标 | 设置环境变量 `VITE_PROXY_TARGET` 可覆盖默认代理目标 |
| 数据库配置 | 后端 `application.yml` 中配置 MySQL 连接信息 |
| OSS 配置 | 通过环境变量或 `application.yml` 配置阿里云 OSS 参数 |

---

## 第五章：后端架构

### 5.1 分层结构

后端采用经典三层架构：

```
com.chuxi
├── ChuxiApplication.java              # Spring Boot 启动类
│
├── auth/                               # ══ 认证模块 ══
│   ├── AuthController.java             #   登录 / 修改密码 REST 接口
│   ├── AdminAuthInterceptor.java       #   Bearer Token 鉴权拦截器
│   ├── PasswordHasher.java             #   PBKDF2 加盐哈希（密码加密）
│   └── TokenStore.java                 #   持久化 Token 存储（内存 + 文件）
│
├── config/                             # ══ 配置层 ══
│   ├── CorsConfig.java                 #   CORS 跨域 + 拦截器注册
│   ├── GlobalExceptionHandler.java     #   全局异常处理（@ControllerAdvice）
│   └── OssProperties.java             #   阿里云 OSS 配置属性绑定
│
├── common/                             # ══ 通用工具 ══
│   ├── R.java                          #   统一响应体 { code, message, data }
│   ├── PageData.java                   #   分页封装 { records, total, pageNo, pageSize }
│   ├── ClientIpResolver.java           #   客户端 IP 解析（支持 X-Forwarded-For）
│   ├── InputSanitizer.java             #   输入净化（防 XSS）
│   └── RateLimiter.java               #   频率限制器（滑动窗口）
│
├── entity/                             # ══ JPA 实体层（16 个） ══
│   ├── Article.java                    #   文章
│   ├── HomeCarousel.java               #   首页轮播
│   ├── CollapseCard.java               #   折叠卡片
│   ├── TeamMember.java                 #   团队成员
│   ├── ArchiveCategory.java            #   归档分类
│   ├── TimelineCarousel.java           #   时间线轮播
│   ├── TimelineEvent.java              #   时间线事件
│   ├── ParallaxStory.java              #   视差故事
│   ├── ToolSite.java                   #   工具站点
│   ├── Barrage.java                    #   弹幕
│   ├── CalledText.java                 #   疗愈文本
│   ├── Music.java                      #   音乐
│   ├── Comment.java                    #   评论
│   ├── BangumiRecord.java              #   番剧记录
│   ├── FriendLink.java                 #   友情链接
│   └── SiteContent.java                #   站点内容（key-value）
│
├── repo/                               # ══ Repository 层（16 个） ══
│   └── （每个实体对应一个 Repository 接口）
│
├── web/                                # ══ Controller 层（13 文件） ══
│   ├── HomeController.java             #   首页聚合数据
│   ├── ArticleController.java          #   文章 CRUD + 评论 + 搜索
│   ├── SectionController.java          #   时间线 / 归档 / 树洞 / 视差 / 工具
│   ├── BangumiController.java          #   番剧记录
│   ├── MediaController.java            #   媒体上传 / 下载 / 删除
│   ├── OssStorageService.java          #   阿里云 OSS 存储服务
│   ├── AdminContentController.java     #   通用 CRUD（15 种资源）
│   ├── SiteContentController.java      #   站点内容管理
│   ├── OverviewController.java         #   仪表盘概览统计
│   ├── RssController.java              #   RSS 订阅输出
│   ├── Dtos.java                       #   Entity → DTO 转换工具
│   ├── BarrageRequest.java             #   弹幕请求 DTO
│   └── CommentRequest.java             #   评论请求 DTO
│
└── init/                               # ══ 初始化 ══
    └── DataInitializer.java            #   种子数据导入（首次启动）
```

### 5.2 统一响应格式

所有 API 返回统一的 JSON 结构：

```json
{
  "code": 0,
  "message": "ok",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `code` | int | `0` = 成功；`400` = 业务失败；`401` = 未认证 |
| `message` | String | 提示信息 |
| `data` | Object | 业务数据（失败时可能为 `null`） |

**成功示例：**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 1,
    "title": "Hello World",
    "content": "..."
  }
}
```

**失败示例：**

```json
{
  "code": 400,
  "message": "标题不能为空",
  "data": null
}
```

### 5.3 分页格式

分页接口返回的数据结构：

```json
{
  "records": [
    { "id": 1, "title": "文章一" },
    { "id": 2, "title": "文章二" }
  ],
  "total": 100,
  "pageNo": 1,
  "pageSize": 10
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `records` | Array | 当前页数据列表 |
| `total` | long | 总记录数 |
| `pageNo` | int | 当前页码（从 1 开始） |
| `pageSize` | int | 每页条数 |

### 5.4 全局异常处理

通过 `GlobalExceptionHandler`（`@RestControllerAdvice`）统一捕获异常：

| 异常类型 | HTTP 状态码 | 响应 message |
|----------|------------|-------------|
| `MethodArgumentNotValidException` | 400 | 第一个字段校验错误信息 |
| `IllegalArgumentException` | 400 | 异常消息 |
| `EntityNotFoundException` | 404 | `"资源不存在"` |
| `Exception`（兜底） | 500 | `"服务器内部错误"` |

### 5.5 CORS 配置

| 配置项 | 说明 |
|--------|------|
| 环境变量 | `APP_CORS_ALLOWED_ORIGINS` |
| 默认值 | `http://localhost:5173,http://localhost:3000` |
| 允许方法 | GET, POST, PUT, DELETE, OPTIONS |
| 允许头 | `*` |
| 凭证 | 允许（`allowCredentials = true`） |

CORS 配置在 `CorsConfig.java` 中实现，同时注册 `AdminAuthInterceptor` 到 `/api/admin/**` 和 `/api/auth/**` 路径。

---

## 第六章：前端架构

### 6.1 路由系统

采用 Vue Router 4，HTML5 History 模式：

| 路径 | 组件 | 说明 |
|------|------|------|
| `/` | redirect → `/index` | 根路径重定向 |
| `/index` | HomeView.vue | 首页 |
| `/article/:id` | ArticleView.vue | 文章详情 |
| `/timeline` | TimelineView.vue | 时间线 |
| `/tree-hole` | TreeHoleView.vue | 树洞弹幕 |
| `/parallax` | ParallaxView.vue | 视差滚动 |
| `/archive` | ArchiveView.vue | 归档 |
| `/tool` | ToolView.vue | 工具地图 |
| `/tool/:id` | ToolDetailView.vue | 工具详情 |
| `/bangumi` | BangumiView.vue | 番剧记录 |
| `/bangumi/:id` | BangumiDetailView.vue | 番剧详情 |
| `/calendar` | CalendarView.vue | 番剧日历 |
| `/components` | ComponentsShowView.vue | CX 组件展示 |
| `/about` | AboutView.vue | 关于 |
| `/admin` | AdminView.vue | 管理后台 |

**路由守卫：**

- `afterEach` 守卫在每次导航后动态设置：
  - `document.title`：格式为 `"{页面标题} - 初曦の巢"`
  - `<meta name="description">`：页面描述
  - Open Graph 标签：`og:title`、`og:description`、`og:type`、`og:url`

**滚动行为：**

- `scrollBehavior` 支持前进/后退位置恢复
- 实际滚动容器为 `.app-shell-main`（`overflow-y: auto`），而非 `window`
- 普通跳转统一回到容器顶部

### 6.2 状态管理（Pinia）

项目只有一个 Store：`useSettingsStore`（`src/stores/settings.js`），管理全局设置：

| 状态 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `theme` | `'light' \| 'dark'` | `'light'` | 当前主题 |
| `backgroundImageEnabled` | boolean | `true` | 背景图开关 |
| `backgroundCarouselEnabled` | boolean | `true` | 背景轮播开关 |
| `sakuraEnabled` | boolean | `false` | 樱花特效开关 |
| `live2dEnabled` | boolean | `true` | 看板娘开关 |
| `selectedLandscapeImage` | string | 第 1 张横屏图 | 当前横屏背景图 |
| `selectedVerticalImage` | string | 第 1 张竖屏图 | 当前竖屏背景图 |
| `galleryLandscape` | string[] | 内置 13 张 | 横屏背景图库 |
| `galleryVertical` | string[] | 内置 16 张 | 竖屏背景图库 |

**计算属性：**

| 属性 | 说明 |
|------|------|
| `landscapeImages` | 当前横屏图库（computed） |
| `verticalImages` | 当前竖屏图库（computed） |
| `isDark` | 是否为暗色主题（computed） |

**核心方法：**

| 方法 | 说明 |
|------|------|
| `setTheme(val)` | 切换主题，同时切换 `html.dark` 类 |
| `update(patch)` | 批量更新设置 |
| `persist()` | 持久化到 `localStorage` |
| `loadRemoteGallery()` | 从后台拉取背景图库配置（key: `background-gallery`） |

**持久化：**

- 存储 key：`chuxi-nest-settings`
- 存储格式：JSON 字符串
- 每次设置变更自动调用 `persist()`

### 6.3 布局系统

```
LayoutView.vue                          # 主布局容器
├── BackgroundLayer.vue                 # 背景图层（横/竖屏自适应切换）
├── TopBar.vue                          # 顶部导航栏（毛玻璃效果）
├── <router-view>                       # 页面内容区（.app-shell-main）
├── MusicBar.vue                        # 底部音乐播放条
├── AiChatPanel.vue                     # AI 助手面板（侧栏）
├── SearchOverlay.vue                   # 文章搜索浮层
├── SettingsDialog.vue                  # 设置弹窗（主题/背景/特效）
└── SakuraCanvas.vue                    # 樱花特效 Canvas 画布
```

### 6.4 CX 组件库

自研轻量 UI 组件，位于 `src/components/cx/`：

| 组件 | 功能 | 主要 Props |
|------|------|-----------|
| `CxButton` | 按钮 | `type`（primary/default/text）、`size`、`disabled`、`loading` |
| `CxMessage` | 消息提示 | `type`（success/error/warning/info）、`content`、`duration` |
| `CxPopover` | 气泡卡片 | `trigger`（hover/click）、`placement`、`title` |
| `CxRadio` | 单选按钮 | `v-model`、`value`、`disabled` |
| `CxRadioGroup` | 单选组 | `v-model`、`options` |
| `CxSwitch` | 开关 | `v-model`、`disabled` |
| `CxTag` | 标签 | `type`、`closable`、`size` |

### 6.5 管理后台组件

管理后台位于 `src/views/admin/`，共 22 个组件：

| 组件 | 功能 |
|------|------|
| `AdminView.vue` | 管理端主框架（侧栏导航 + 内容区） |
| `DashboardPanel.vue` | 仪表盘统计（文章数、评论数、访问量等） |
| `ResourcePanel.vue` | 通用资源 CRUD 面板（表格 + 新增/编辑/删除） |
| `resourceSchemas.js` | 15 种资源的字段 schema 定义 |
| `ArticlesPanel.vue` | 文章管理（Markdown 宽抽屉编辑器） |
| `BangumiPanel.vue` | 番剧管理（支持 Bangumi 搜索导入） |
| `MediaPanel.vue` | 图片库管理（上传/删除/列表） |
| `MediaPicker.vue` | 媒体选择器 |
| `CropDialog.vue` | 图片裁切对话框 |
| `ImageSelect.vue` | 从图库选择图片 |
| `AdminSelect.vue` | 自研下拉选择组件 |
| `FieldInput.vue` | 动态字段输入（根据 schema 渲染） |
| `AppearancePanel.vue` | 外观设置（主题色、字体等） |
| `BackgroundPanel.vue` | 背景图库管理 |
| `NavMenuPanel.vue` | 导航菜单配置 |
| `PageContentPanel.vue` | 页面内容管理 |
| `ScenePanel.vue` | 场景面板 |
| `SiteContentPanel.vue` | 站点内容管理（key-value 编辑器） |
| `SiteSettingsPanel.vue` | 站点设置（站点名称、描述等） |
| `PasswordDialog.vue` | 修改密码对话框 |

### 6.6 API 层设计

基于 Axios 封装两个独立实例：

**前台 API**（`src/api/index.js`）：

```javascript
const http = axios.create({ baseURL: '/api', timeout: 15000 })

// 响应拦截器：code === 0 时直接返回 data
http.interceptors.response.use(res => {
  const body = res.data
  if (body && body.code === 0) return body.data
  return Promise.reject(new Error(body.message || '请求失败'))
})
```

**后台 API**（`src/api/admin.js`）：

```javascript
const http = axios.create({ baseURL: '/api', timeout: 15000 })

// 请求拦截器：自动注入 Bearer Token
http.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截器：401 时自动清除 token
http.interceptors.response.use(
  res => { /* 同前台 */ },
  err => {
    if (err.response?.status === 401) {
      clearToken()
      // ...
    }
  }
)
```

| 配置项 | 值 |
|--------|-----|
| baseURL | `/api` |
| timeout | 15000ms（媒体上传 300000ms） |
| Token 存储 | `localStorage['chuxi-admin-token']` |
| 认证方式 | `Authorization: Bearer {token}` |

**前台 API 方法一览：**

| 方法 | HTTP | 路径 |
|------|------|------|
| `homeLanding()` | GET | `/front/home/landing` |
| `homeArticles(pageNo, pageSize)` | GET | `/front/home/articles` |
| `teamMembers()` | GET | `/front/home/team-members` |
| `articleDetail(id)` | GET | `/front/articles/{id}` |
| `articleComments(id)` | GET | `/front/articles/{id}/comments` |
| `addComment(id, data)` | POST | `/front/articles/{id}/comments` |
| `likeComment(id)` | POST | `/front/articles/comments/{id}/likes` |
| `searchArticles(keyword, pageNo, pageSize)` | GET | `/front/articles/search` |
| `timelineLanding()` | GET | `/front/timeline/landing` |
| `archiveLanding()` | GET | `/front/archive/landing` |
| `treeHoleBarrages()` | GET | `/front/tree-hole/barrages` |
| `addBarrage(data)` | POST | `/front/tree-hole/barrages` |
| `likeBarrage(id)` | POST | `/front/tree-hole/barrages/{id}/likes` |
| `calledTexts()` | GET | `/front/tree-hole/called-texts` |
| `parallaxStories()` | GET | `/front/parallax/stories` |
| `toolsLanding()` | GET | `/front/tools/landing` |
| `bangumiRecords()` | GET | `/front/bangumi` |
| `bangumiDetail(id)` | GET | `/front/bangumi/{id}` |
| `friendLinks()` | GET | `/front/friend-links` |
| `siteContent(key)` | GET | `/front/site-content/{key}` |
| `bumpViews()` | POST | `/front/views/bump` |
| `views()` | GET | `/front/views` |
| `music()` | GET | `/music` |

### 6.7 CSS 方案

项目使用纯 CSS 方案（无 CSS Modules），共 19 个独立 CSS 文件：

**主题切换：**

- 通过 CSS 变量实现（`--cx-primary`、`--cx-bg`、`--cx-text` 等）
- 暗色模式：`html.dark` 类选择器切换变量值
- `setTheme('dark')` 时自动添加 `html.dark` 类

**视觉风格：**

- 玻璃拟态：`backdrop-filter: blur() saturate()` + 半透明背景
- 卡片组件：轻透质感设计
- Tab 栏：穿透式视觉效果 + 动态背景跟随

**全局样式引入顺序：**

```
base.css → cx-button.css → cx-input.css → cx-tag.css
→ cx-switch.css → cx-popover.css → cx-section.css
→ layout.css → components-show.css → hljs-atom-one-dark.css
```

### 6.8 自定义指令

| 指令 | 文件 | 功能 |
|------|------|------|
| `v-reveal` | `src/directives/reveal.js` | 滚动进入视口时的揭示动画（基于 IntersectionObserver） |

### 6.9 工具函数

| 文件 | 功能 | 说明 |
|------|------|------|
| `utils/display.js` | 显示相关工具函数 | 格式化、截断等 |
| `utils/markdown.js` | Markdown 渲染 | 集成 marked + highlight.js + DOMPurify |
| `utils/svgSprite.js` | SVG Sprite 注入 | 将 SVG 精灵图注入 DOM |

---

## 第七章：前台页面说明

### 7.1 页面总览

| 页面 | 路由 | 功能 | 核心 API |
|------|------|------|----------|
| HomeView | `/index` | 首页：HERO 轮播、折叠卡片、最新文章、团队成员 | `homeLanding`、`homeArticles`、`teamMembers`、`siteContent` |
| ArticleView | `/article/:id` | 文章详情：Markdown 渲染、代码高亮、评论系统、上下篇导航 | `articleDetail`、`articleComments`、`addComment`、`likeComment` |
| TimelineView | `/timeline` | 时间线：轮播图 + 时间轴事件列表 | `timelineLanding`、`siteContent('timeline-hero')` |
| TreeHoleView | `/tree-hole` | 树洞：弹幕发送 + 弹幕列表 + 疗愈文本 | `treeHoleBarrages`、`addBarrage`、`likeBarrage`、`calledTexts` |
| ParallaxView | `/parallax` | 视差滚动叙事 | `parallaxStories`、`siteContent('parallax-config')` |
| ArchiveView | `/archive` | 归档：全部文章按时间排列 + 分类筛选 | `archiveLanding`、`siteContent('archive-hero')` |
| ToolView | `/tool` | 工具地图：工具站点导航 | `toolsLanding`、`siteContent('tool-hero')` |
| BangumiView | `/bangumi` | 番剧记录列表 | `bangumiRecords` |
| BangumiDetailView | `/bangumi/:id` | 番剧详情 | `bangumiDetail` |
| CalendarView | `/calendar` | 番剧更新日历 | `bangumiRecords`、`siteContent('calendar-hero')` |
| AboutView | `/about` | 关于页 + 友情链接 | `friendLinks`、`siteContent('about')` |
| ComponentsShowView | `/components` | CX 组件展示 | 无（纯前端展示） |

### 7.2 首页（HomeView）

首页是用户进入站点的第一印象，包含以下区域：

- **HERO 轮播**：全屏轮播图，数据来自 `homeLanding` 接口
- **折叠卡片**：可展开/收起的内容卡片，数据来自 `collapse-cards` 资源
- **最新文章**：展示最新 6 篇文章摘要，调用 `homeArticles(pageNo, pageSize)`
- **团队成员**：展示团队成员卡片，调用 `teamMembers()`
- **站点文案**：通过 `siteContent` 接口获取首页个性化文案

### 7.3 文章详情（ArticleView）

- **Markdown 渲染**：使用 `marked` 解析 + `highlight.js` 代码高亮 + `DOMPurify` 净化
- **评论系统**：支持发表评论（需填写昵称、邮箱、内容）和点赞
- **上下篇导航**：文章底部显示上一篇/下一篇链接
- **浏览量统计**：进入页面时调用 `bumpViews()` 增加访问计数

### 7.4 树洞弹幕（TreeHoleView）

- **弹幕发送**：用户可发送文字弹幕，内容实时显示
- **弹幕列表**：所有弹幕按时间排列
- **点赞**：支持对弹幕和评论点赞
- **疗愈文本**：展示疗愈文字内容，来自 `calledTexts` 接口

### 7.5 番剧相关（BangumiView / BangumiDetailView / CalendarView）

- **番剧列表**：展示所有番剧收藏记录
- **番剧详情**：单部番剧的详细信息
- **番剧日历**：按星期展示番剧更新日程

---

## 第八章：数据模型

### 8.1 实体关系概览

项目共有 **16 个 JPA 实体**，对应 MySQL 中的 16 张表。实体之间**无 JPA 关联注解**（`@OneToMany` 等），均通过 ID 字段逻辑关联，保持模型扁平。

| 实体类 | 数据库表 | 说明 | 主键策略 |
|--------|----------|------|----------|
| `Article` | `article` | 文章 | 手动指定 `Long id` |
| `HomeCarousel` | `home_carousel` | 首页轮播 | 手动指定 `Long id` |
| `CollapseCard` | `collapse_card` | 折叠卡片 | 手动指定 `Long id` |
| `TeamMember` | `team_member` | 团队成员 | 手动指定 `Long id` |
| `ArchiveCategory` | `archive_category` | 归档分类 | 自增 `IDENTITY` |
| `TimelineCarousel` | `timeline_carousel` | 时间线轮播 | 手动指定 `Long id` |
| `TimelineEvent` | `timeline_event` | 时间线事件 | 手动指定 `Long id` |
| `ParallaxStory` | `parallax_story` | 视差故事 | 手动指定 `Long id` |
| `ToolSite` | `tool_site` | 工具站点 | 手动指定 `Long id` |
| `Barrage` | `barrage` | 弹幕 | 自增 `IDENTITY` |
| `CalledText` | `called_text` | 疗愈文本 | 手动指定 `Long id` |
| `Music` | `music` | 音乐 | 手动指定 `Long id` |
| `Comment` | `article_comment` | 文章评论 | 自增 `IDENTITY` |
| `BangumiRecord` | `bangumi_record` | 番剧记录 | 自增 `IDENTITY` |
| `FriendLink` | `friend_link` | 友情链接 | 手动指定 `Long id` |
| `SiteContent` | `site_content` | 站点内容（KV） | 自增 `IDENTITY` |

**逻辑关联关系：**

| 关系 | 关联方式 | 说明 |
|------|----------|------|
| Article → ArchiveCategory | `article.archiveCategory` = `category.category` | 归档分类名关联 |
| Comment → Article | `comment.articleId` → `article.id` | 评论属于文章 |
| HomeCarousel（visible） | `visible` 字段 | 控制前台是否展示 |
| BangumiRecord（subjectId） | `subjectId` 唯一约束 | 同一部番剧只允许收录一次 |
| SiteContent（auth-token-*） | `contentKey` 前缀 | Token 存储复用此表 |

### 8.2 实体字段明细

#### Article（文章）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 文章 ID |
| `title` | String | `title` | 标题 |
| `summary` | String | `summary` (VARCHAR 1000) | 摘要 |
| `coverUrl` | String | `cover_url` (VARCHAR 500) | 封面图 URL |
| `categoryId` | Long | `category_id` | 分类 ID |
| `categoryName` | String | `category_name` | 分类名称 |
| `archiveCategory` | String | `archive_category` | 归档分类 |
| `tags` | String | `tags` | 标签（逗号分隔） |
| `content` | String | `content` (LONGTEXT) | Markdown 正文 |
| `readingTime` | String | `reading_time` | 预计阅读时间 |
| `mood` | String | `mood` (VARCHAR 500) | 心情标签 |
| `status` | String | `status` | 发布状态："已发布" / "草稿" |
| `pinned` | Boolean | `pinned` | 是否置顶 |
| `publishedAt` | LocalDateTime | `published_at` | 发布时间 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### HomeCarousel（首页轮播）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 轮播 ID |
| `title` | String | `title` | 标题 |
| `description` | String | `description` (VARCHAR 500) | 描述 |
| `content` | String | `content` (VARCHAR 1000) | 内容 |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 图片 URL |
| `sortIndex` | Integer | `sort_index` | 排序值 |
| `sceneLabel` | String | `scene_label` | 场景编号（如 "SCENE 01"） |
| `kicker` | String | `kicker` | 顶部小标语 |
| `badge` | String | `badge` | 角标文案 |
| `visible` | Boolean | `visible` | 前台是否展示 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### CollapseCard（折叠卡片）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 卡片 ID |
| `title` | String | `title` | 标题 |
| `description` | String | `description` (VARCHAR 500) | 描述 |
| `content` | String | `content` (VARCHAR 1000) | 内容 |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 图片 URL |
| `sortIndex` | Integer | `sort_index` | 排序值 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### TeamMember（团队成员）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 成员 ID |
| `displayName` | String | `display_name` | 显示名称 |
| `email` | String | `email` | 邮箱 |
| `avatarUrl` | String | `avatar_url` (VARCHAR 500) | 头像 URL |
| `roleCode` | String | `role_code` | 角色代码 |
| `roleLabel` | String | `role_label` | 角色标签 |
| `position` | String | `position` | 职位 |
| `description` | String | `description` (VARCHAR 500) | 描述 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### ArchiveCategory（归档分类）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK, 自增) | 分类 ID |
| `category` | String | `category` | 分类标识 |
| `title` | String | `title` | 分类标题 |
| `description` | String | `description` (VARCHAR 500) | 分类描述 |
| `tags` | String | `tags` | 标签 |

#### TimelineCarousel（时间线轮播）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 轮播 ID |
| `title` | String | `title` | 标题 |
| `content` | String | `content` (VARCHAR 1000) | 内容 |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 图片 URL |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### TimelineEvent（时间线事件）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 事件 ID |
| `title` | String | `title` | 标题 |
| `content` | String | `content` (VARCHAR 1000) | 内容 |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 图片 URL |
| `timelineDate` | LocalDate | `timeline_date` | 事件日期 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### ParallaxStory（视差故事）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 故事 ID |
| `title` | String | `title` | 标题 |
| `description` | String | `description` (VARCHAR 1000) | 描述 |
| `note` | String | `note` (VARCHAR 1000) | 备注 |
| `align` | String | `align` | 对齐方式 |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 图片 URL |
| `sortIndex` | Integer | `sort_index` | 排序值 |

#### ToolSite（工具站点）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 站点 ID |
| `websiteName` | String | `website_name` | 网站名称 |
| `websiteDescription` | String | `website_description` (VARCHAR 500) | 网站描述 |
| `websiteUrl` | String | `website_url` (VARCHAR 500) | 网站 URL |
| `categoryId` | Long | `category_id` | 分类 ID |
| `category` | String | `category` | 分类名称 |
| `iconUrl` | String | `icon_url` (VARCHAR 500) | 图标 URL |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 详情配图 URL |
| `tags` | String | `tags` | 标签 |
| `highlight` | String | `highlight` | 高亮标记 |
| `featured` | Boolean | `featured` | 是否推荐 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### Barrage（弹幕）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK, 自增) | 弹幕 ID |
| `userId` | Long | `user_id` | 用户 ID |
| `nickname` | String | `nickname` | 昵称 |
| `mood` | String | `mood` | 心情 |
| `likeCount` | Integer | `like_count` | 点赞数 |
| `liked` | Boolean | `liked` | 是否已赞 |
| `content` | String | `content` (VARCHAR 500) | 弹幕内容 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |
| `approved` | Boolean | `approved` | 是否审核通过 |

#### CalledText（疗愈文本）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 文本 ID |
| `title` | String | `title` | 标题 |
| `tag` | String | `tag` | 标签 |
| `content` | String | `content` (VARCHAR 1000) | 内容 |
| `summary` | String | `summary` (VARCHAR 500) | 摘要 |
| `imageUrl` | String | `image_url` (VARCHAR 500) | 图片 URL |
| `readTime` | String | `read_time` | 阅读时间 |
| `sortIndex` | Integer | `sort_index` | 排序值 |
| `audioUrl` | String | `audio_url` (VARCHAR 500) | 音频 URL |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### Music（音乐）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 音乐 ID |
| `title` | String | `title` | 歌曲标题 |
| `artist` | String | `artist` | 艺术家 |
| `album` | String | `album` | 专辑 |
| `coverUrl` | String | `cover_url` (VARCHAR 500) | 封面图 URL |
| `musicUrl` | String | `music_url` (VARCHAR 500) | 音频文件 URL |
| `lyric` | String | `lyric` (LONGTEXT) | 歌词（LRC 格式） |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### Comment（文章评论）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK, 自增) | 评论 ID |
| `articleId` | Long | `article_id` | 所属文章 ID |
| `nickname` | String | `nickname` | 昵称 |
| `content` | String | `content` (VARCHAR 1000) | 评论内容 |
| `likeCount` | Integer | `like_count` | 点赞数 |
| `liked` | Boolean | `liked` | 是否已赞 |
| `approved` | Boolean | `approved` | 是否审核通过 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |

#### BangumiRecord（番剧记录）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK, 自增) | 记录 ID |
| `subjectId` | Long | `subject_id` (UNIQUE) | bgm.tv 条目 ID（唯一） |
| `name` | String | `name` | 原名 |
| `nameCn` | String | `name_cn` | 中文名 |
| `coverUrl` | String | `cover_url` (VARCHAR 500) | 封面图 URL |
| `totalEps` | Integer | `total_eps` | 总集数 |
| `watchedEps` | Integer | `watched_eps` | 已看集数 |
| `status` | String | `status` | 观看状态：想看/在看/看过/搁置/弃番 |
| `rating` | Integer | `rating` | 个人评分（0-10） |
| `score` | Double | `score` | bgm 站均分 |
| `airDate` | String | `air_date` | 放送日期 |
| `platform` | String | `platform` | 放送形态：TV/剧场版/OVA |
| `rank` | Integer | `bgm_rank` | bgm 站排名 |
| `ratingTotal` | Integer | `rating_total` | 评分人数 |
| `summary` | String | `summary` (TEXT) | 剧情简介 |
| `tags` | String | `tags` | 标签（CSV 存储，API 层转数组） |
| `category` | String | `category` | 分类（热血/日常/奇幻等） |
| `sortIndex` | Integer | `sort_index` | 排序值 |
| `visible` | Boolean | `visible` | 前台是否展示 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### FriendLink（友情链接）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK) | 链接 ID |
| `siteName` | String | `site_name` (VARCHAR 128, NOT NULL) | 站点名称 |
| `siteUrl` | String | `site_url` (VARCHAR 512, NOT NULL) | 站点 URL |
| `logoUrl` | String | `logo_url` (VARCHAR 512) | Logo URL |
| `description` | String | `description` (VARCHAR 512) | 描述 |
| `sortIndex` | Integer | `sort_index` | 排序值 |
| `visible` | Boolean | `visible` | 是否展示 |
| `createdAt` | LocalDateTime | `created_at` | 创建时间 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

#### SiteContent（站点内容）

| 字段 | 类型 | 数据库列 | 说明 |
|------|------|----------|------|
| `id` | Long | `id` (PK, 自增) | 记录 ID |
| `contentKey` | String | `content_key` (VARCHAR 128, UNIQUE, NOT NULL) | 键名 |
| `contentJson` | String | `content_json` (LONGTEXT) | JSON 值 |
| `updatedAt` | LocalDateTime | `updated_at` | 更新时间 |

---

## 第九章：SiteContent 键值存储

### 9.1 机制说明

`SiteContent` 表是一个通用的 **key-value 存储机制**，用于管理站点中的各种动态配置内容。

| 特性 | 说明 |
|------|------|
| 表名 | `site_content` |
| 键字段 | `content_key`（VARCHAR 128，唯一约束） |
| 值字段 | `content_json`（LONGTEXT，存储 JSON 字符串） |
| 更新时间 | `updated_at` |

**设计优势：**

- 无需为每种配置创建独立表
- 后台管理界面统一编辑
- 前台通过 `GET /api/front/site-content/{key}` 获取
- 后台通过 `PUT /api/admin/site-content/{key}` 更新

**前台 API 行为：**

- 查询到记录时：返回 `{ code: 0, data: { contentKey, contentJson, updatedAt } }`
- 未查询到时：返回 `{ code: 1, message: "..." }`（前台 API 拦截器会 reject）
- 前端通常对 `contentJson` 做 `JSON.parse()` 获取结构化数据

### 9.2 已使用的 Key 清单

| Key | 用途 | 值格式 |
|-----|------|--------|
| `about` | 关于页内容 | JSON 对象 |
| `timeline-hero` | 时间线页 Hero 区配置 | JSON 对象 |
| `archive-hero` | 归档页 Hero 区配置 | JSON 对象 |
| `tool-hero` | 工具页 Hero 区配置 | JSON 对象 |
| `calendar-hero` | 日历页 Hero 区配置 | JSON 对象 |
| `parallax-config` | 视差页全局配置 | JSON 对象 |
| `background-gallery` | 背景图库配置 | `{ landscape: [...], vertical: [...] }` |
| `auth-token-{uuid}` | 管理端登录 Token | `{ username, issuedAt, expiresAt }` |

> **注意**：`auth-token-*` 前缀的 key 由 `TokenStore` 自动管理，不应在后台手动编辑。

---

## 第十章：API 鉴权机制

### 10.1 认证流程

```
┌─────────┐    POST /api/auth/login     ┌──────────┐
│  浏览器  │  ─────────────────────────> │  后端 API │
│         │    { username, password }    │          │
│         │                             │ 验证密码  │
│         │  <───────────────────────── │ 签发 Token│
│         │    { code:0, data: token }  │          │
│         │                             │          │
│         │    GET /api/admin/xxx       │          │
│         │    Authorization: Bearer xx │          │
│         │  ─────────────────────────> │          │
│         │                             │ 校验 Token│
│         │  <───────────────────────── │          │
│         │    { code:0, data: ... }    │          │
└─────────┘                             └──────────┘
```

1. **登录**：`POST /api/auth/login` 提交用户名密码
2. **验证**：`PasswordHasher.matches()` 校验 PBKDF2 哈希
3. **签发**：`TokenStore.issue()` 生成 UUID Token，存入 `site_content` 表
4. **携带**：前端将 Token 存入 `localStorage['chuxi-admin-token']`，后续请求通过 `Authorization: Bearer {token}` 头携带
5. **校验**：`AdminAuthInterceptor` 拦截 `/api/admin/**` 请求，调用 `TokenStore.resolveBearer()` 验证

### 10.2 Token 生命周期

| 阶段 | 说明 |
|------|------|
| 签发 | `UUID.randomUUID()` 生成，存储到 `site_content` 表（key: `auth-token-{uuid}`） |
| 有效期 | **7 天**（`TOKEN_EXPIRE_DAYS = 7`） |
| 存储格式 | JSON：`{ username, issuedAt, expiresAt }` |
| 校验 | 从 `site_content` 表查找对应 key，检查 `expiresAt` 是否过期 |
| 过期清理 | 校验时自动清理过期 Token；也可手动调用 `cleanExpiredTokens()` |
| 注销 | `TokenStore.invalidate(token)` 从数据库删除记录 |

### 10.3 接口权限分类

| 路径模式 | 权限要求 | 说明 |
|----------|----------|------|
| `/api/front/**` | 无（公开） | 前台所有只读接口 |
| `/api/front/tree-hole/barrages` (POST) | 频率限制 | 弹幕发送受 RateLimiter 限制 |
| `/api/front/articles/*/comments` (POST) | 频率限制 | 评论发送受 RateLimiter 限制 |
| `/api/auth/login` | 无（公开） | 登录接口 |
| `/api/auth/password` | Bearer Token | 修改密码需登录态 |
| `/api/auth/me` | Bearer Token | 获取当前用户信息 |
| `/api/admin/**` | Bearer Token | 所有管理端接口 |
| `/api/admin/upload` | Bearer Token | 文件上传 |
| `/api/admin/site-content/**` | Bearer Token | 站点内容管理 |

---

## 第十一章：安全机制

### 11.1 密码存储

采用 **PBKDF2WithHmacSHA256** 算法进行密码哈希：

| 参数 | 值 |
|------|-----|
| 算法 | PBKDF2WithHmacSHA256 |
| 迭代次数 | 120,000 |
| 密钥长度 | 256 bit |
| 盐长度 | 16 字节（随机生成） |
| 存储格式 | `pbkdf2$迭代次数$Base64(盐)$Base64(哈希)` |

**示例存储值：**

```
pbkdf2$120000$xxxxxxxxxxxxxxxxxxxxxx==$yyyyyyyyyyyyyyyyyyyyyy==
```

**兼容机制：**

- 若存储值不以 `pbkdf2$` 开头，按明文比对（便于旧数据平滑迁移）
- `PasswordHasher.isHashed(stored)` 可判断是否已哈希

### 11.2 XSS 防护

**后端：`InputSanitizer`**

通过正则过滤危险 HTML 片段：

| 过滤规则 | 说明 |
|----------|------|
| `<script>` 标签 | 移除所有 script 开/闭标签 |
| `<script>...</script>` 块 | 移除整个 script 块（含内容） |
| `<iframe>` 标签 | 移除所有 iframe 标签 |
| `on*=` 事件属性 | 移除 `onclick=`、`onerror=` 等事件处理器 |

**前端：DOMPurify**

- Markdown 渲染后通过 `DOMPurify.sanitize()` 净化 HTML
- 防止存储型 XSS 攻击

### 11.3 频率限制

`RateLimiter` 是一个轻量级 IP 级滑动窗口限流器：

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `windowSeconds` | 60 | 滑动窗口时长（秒） |
| `maxRequests` | 3 | 窗口内最大请求数 |
| 存储 | `ConcurrentHashMap<String, Deque<Long>>` | 内存存储，JVM 级别 |
| 线程安全 | `synchronized` 同步块 | 适用于低并发场景 |

**使用场景：**

- 弹幕发送：防止同一 IP 频繁发送弹幕
- 评论提交：防止同一 IP 频繁发表评论

### 11.4 CORS 安全

| 配置项 | 说明 |
|--------|------|
| 允许源 | 通过 `APP_CORS_ALLOWED_ORIGINS` 环境变量配置 |
| 默认值 | `http://localhost:5173,http://localhost:3000` |
| 允许方法 | GET, POST, PUT, DELETE, OPTIONS |
| 允许头 | `*` |
| 凭证 | `allowCredentials = true` |
| OPTIONS 预检 | `AdminAuthInterceptor` 直接放行 OPTIONS 请求 |

---

## 第十二章：种子数据

项目首次启动时，`DataInitializer` 自动从 `backend/src/main/resources/seed/` 目录导入 14 个 JSON 种子数据文件：

| 种子文件 | 对应实体 | 说明 |
|----------|----------|------|
| `articles.json` | Article | 示例文章 |
| `home-carousels.json` | HomeCarousel | 首页轮播数据 |
| `collapse-cards.json` | CollapseCard | 折叠卡片数据 |
| `team-members.json` | TeamMember | 团队成员数据 |
| `archive-categories.json` | ArchiveCategory | 归档分类数据 |
| `timeline-carousels.json` | TimelineCarousel | 时间线轮播数据 |
| `timeline-events.json` | TimelineEvent | 时间线事件数据 |
| `parallax-stories.json` | ParallaxStory | 视差故事数据 |
| `tool-sites.json` | ToolSite | 工具站点数据 |
| `barrages.json` | Barrage | 示例弹幕数据 |
| `called-texts.json` | CalledText | 疗愈文本数据 |
| `musics.json` | Music | 示例音乐数据 |
| `comments.json` | Comment | 示例评论数据 |
| `bangumi-records.json` | BangumiRecord | 番剧记录数据 |

**导入逻辑：**

- 仅在对应表为空时导入（避免覆盖已有数据）
- 使用 `ObjectMapper` 解析 JSON 文件
- 通过 `Repository.save()` 批量写入数据库

---

## 第十三章：测试策略

### 13.1 后端测试

**测试框架：**

| 技术 | 用途 |
|------|------|
| JUnit 5 | 测试框架（Spring Boot Starter Test 管理） |
| Spring Boot Test | 集成测试（上下文加载） |
| MockMvc | HTTP 接口测试 |
| H2 Database | 测试内存数据库（替代 MySQL） |

**测试配置：**

- 测试环境使用独立的 `application.yml`（`src/test/resources/application.yml`）
- 数据库切换为 H2 内存库，避免依赖外部 MySQL
- 测试数据通过 `data.sql` 初始化

**测试目录：**

```
src/test/java/com/chuxi/
├── ChuxiApplicationTests.java    # 上下文加载冒烟测试
├── auth/
│   └── AuthTests.java            # 认证相关测试
└── web/
    ├── ArticleControllerTests.java    # 文章接口测试
    ├── BangumiControllerTests.java    # 番剧接口测试
    ├── SiteContentControllerTests.java # 站点内容接口测试
    └── HomeControllerTests.java       # 首页接口测试
```

**运行命令：**

```bash
cd backend
mvn test
```

### 13.2 前端测试

**测试框架：**

| 技术 | 用途 |
|------|------|
| Node.js `node:test` | 内置测试运行器（无需额外框架） |
| `assert` | Node.js 内置断言模块 |

**测试文件：**

| 文件 | 测试内容 |
|------|----------|
| `stores/settings.test.js` | Settings Store 状态管理测试 |
| `utils/display.test.js` | 显示工具函数测试 |
| `utils/markdown.test.js` | Markdown 渲染工具测试 |

**运行命令：**

```bash
cd frontend
npm test
```

> **注意**：`npm test` 依赖 `node --test` 的 glob 支持，需要 Node.js ≥ 21。

---

## 第十四章：CI/CD

### 14.1 后端 CI

配置文件：`.github/workflows/backend-ci.yml`

**触发条件：**

- 推送或 PR 到 `master` / `main` 分支
- 且涉及 `backend/**` 或 `.github/workflows/backend-ci.yml` 变更

**Job 列表：**

| Job | 说明 |
|-----|------|
| `entity-ddl-check` | 实体-DDL 同步门禁检查 |
| `mvn-test` | 运行 `mvn -B test`（JDK 17 + Maven 缓存） |

### 14.2 前端 CI

配置文件：`.github/workflows/frontend-ci.yml`

**触发条件：**

- 推送或 PR 到 `master` / `main` 分支
- 且涉及 `frontend/**` 或 `.github/workflows/frontend-ci.yml` 变更

**Job 列表：**

| Job | 步骤 | 说明 |
|-----|------|------|
| `lint-build` | `npm ci` | 安装依赖（CI 模式） |
| | `npm run lint` | ESLint 代码检查 |
| | `npm test` | 运行单元测试 |
| | `npm run build` | 生产构建 |

**环境：**

- Node.js 22（依赖 `node --test` 的 glob 支持）
- npm 缓存（`frontend/package-lock.json`）

### 14.3 实体-DDL 同步门禁

**目的：** 确保每次修改 JPA 实体时，都随附对应的 DDL 脚本供人工审阅。

**检查脚本：** `scripts/ci/check-entity-ddl.sh`

**检查逻辑：**

1. 从标准输入读取 `git diff --name-status` 输出
2. 检测是否涉及 `backend/src/main/java/com/chuxi/entity/` 目录的变更
3. 若涉及实体变更，检查同一提交是否包含 `scripts/` 目录下新增或修改的 `.sql` 文件
4. 若无随附 DDL，门禁失败

**本地模拟：**

```bash
git diff --name-status <base> HEAD | sh scripts/ci/check-entity-ddl.sh
```

**规则说明：**

| 场景 | 结果 |
|------|------|
| 修改实体 + 新增 `.sql` | 通过 |
| 修改实体 + 无 `.sql` | 失败 |
| 未修改实体 | 跳过（直接通过） |
| 仅删除 `.sql` | 不算随附 DDL（需新增或修改） |

---

## 第十五章：部署指南

### 15.1 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                     阿里云 ECS 服务器                        │
│                                                             │
│  ┌───────────┐     ┌──────────────────┐     ┌────────────┐ │
│  │   nginx   │────>│  Spring Boot     │────>│   MySQL 8  │ │
│  │  (80/443) │     │  (端口 8080)     │     │  (3306)    │ │
│  │           │     │  systemd 管理    │     │            │ │
│  │ 静态资源   │     │                  │     └────────────┘ │
│  │ /opt/chuxi│     └──────────────────┘                    │
│  │ /dist     │              │                              │
│  └───────────┘              │                              │
│                      ┌──────┴──────┐                       │
│                      │ 阿里云 OSS  │                       │
│                      │ (媒体文件)  │                       │
│                      └─────────────┘                       │
└─────────────────────────────────────────────────────────────┘
```

| 组件 | 路径/端口 | 说明 |
|------|----------|------|
| nginx | 80/443 | 反向代理 + 静态资源 |
| Spring Boot | 8080 | 后端 API 服务 |
| MySQL | 3306 | 数据库 |
| 前端 dist | `/opt/chuxi/dist` | 静态文件目录 |
| 阿里云 OSS | — | 图片/音频存储 |

### 15.2 前端部署

使用 Python 脚本通过 SSH 部署前端：

**脚本：** `scripts/deploy/deploy_frontend_only.py`

**部署步骤：**

1. 本地构建：`cd frontend && npm run build`
2. 打包：`cd frontend && tar czf dist_build.tgz dist`
3. 设置环境变量：`set SSH_PWD=你的服务器密码`
4. 执行部署：`python scripts/deploy/deploy_frontend_only.py`

**脚本行为：**

| 步骤 | 操作 |
|------|------|
| 1/3 | 上传 `dist_build.tgz` 到服务器 `/tmp/dist.tgz` |
| 2/3 | 解压到 `/opt/chuxi/dist`（先清空旧文件） |
| 3/3 | 无需重启后端（nginx 直接服务静态文件） |

**环境变量：**

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SSH_PWD` | 服务器 root 密码（必填） | — |
| `DIST_TGZ` | 本地 dist 包路径 | `D:\workspace\dist_build.tgz` |

### 15.3 nginx 配置

nginx 主要职责：

| 路径 | 处理方式 |
|------|----------|
| `/` | 返回前端 `index.html`（SPA fallback） |
| `/assets/*` | 直接返回静态文件（`/opt/chuxi/dist/assets/`） |
| `/api/*` | 反向代理到 `http://localhost:8080` |
| 其他静态资源 | 直接返回（favicon、robots.txt 等） |

### 15.4 后端 systemd 服务

后端通过 systemd 管理进程：

**关键配置：**

| 配置项 | 值 |
|--------|-----|
| 服务名 | `chuxi-backend` |
| 工作目录 | `/opt/chuxi` |
| JAR 路径 | `/opt/chuxi/chuxi-backend.jar` |
| JVM 参数 | `-Dfile.encoding=UTF-8` |
| 环境变量 | 通过 systemd unit 注入（`DB_URL`、`APP_OSS_*` 等） |
| JPA DDL | 线上 `JPA_DDL_AUTO=validate`（禁止自动改表） |

**常用命令：**

```bash
systemctl start chuxi-backend     # 启动
systemctl stop chuxi-backend      # 停止
systemctl restart chuxi-backend   # 重启
systemctl status chuxi-backend    # 查看状态
journalctl -u chuxi-backend -f    # 查看日志
```

---

## 第十六章：配置参考

### 16.1 后端环境变量

以下环境变量在 `application.yml` 中通过 `${VAR:default}` 语法引用：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `SERVER_PORT` | `8080` | 服务端口 |
| `SERVER_ADDRESS` | `127.0.0.1` | 监听地址（默认仅回环，生产经 nginx 反代） |
| `DB_URL` | `jdbc:mysql://localhost:3306/chuxi_db?...` | MySQL 连接 URL |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | （空） | 数据库密码（线上由 systemd unit 注入，不写入 README/仓库） |
| `JPA_DDL_AUTO` | `update` | Hibernate DDL 策略（线上设为 `validate`） |
| `APP_TRUST_PROXY` | `false` | 是否信任反向代理转发头（fail-closed；线上经 nginx 时须设 `true` 且 nginx 覆盖 XFF/X-Real-IP/X-Forwarded-Proto） |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | CORS 允许源 |
| `APP_OSS_ENABLED` | `true` | 是否启用阿里云 OSS |
| `APP_OSS_ENDPOINT` | `https://oss-cn-beijing.aliyuncs.com` | OSS Endpoint |
| `APP_OSS_BUCKET` | `chuxisleep` | OSS Bucket 名称 |
| `APP_OSS_ACCESS_KEY_ID` | （空） | OSS AccessKey ID |
| `APP_OSS_ACCESS_KEY_SECRET` | （空） | OSS AccessKey Secret |
| `APP_OSS_PREFIX` | `uploads/` | OSS 对象前缀（目录） |
| `APP_OSS_BASE_URL` | （空） | OSS 自定义域名（留空用默认公网地址） |

**application.yml 关键配置说明：**

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `spring.servlet.multipart.max-file-size` | 100MB | 单文件上传限制 |
| `spring.servlet.multipart.max-request-size` | 110MB | 请求总大小限制 |
| `spring.jpa.open-in-view` | false | 关闭 OSIV，避免懒加载陷阱 |
| `spring.jackson.serialization.write-dates-as-timestamps` | false | 日期序列化为 ISO 字符串 |
| `spring.jackson.default-property-inclusion` | non_null | JSON 输出忽略 null 字段 |
| `spring.flyway.enabled` | false | 显式关闭 Flyway |

### 16.2 前端环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_PROXY_TARGET` | `http://localhost:8081` | 开发代理目标地址 |

**vite.config.js 关键配置：**

| 配置项 | 值 | 说明 |
|--------|-----|------|
| 开发端口 | 5173 | Vite 默认端口 |
| 代理路径 | `/api` → `VITE_PROXY_TARGET` | API 请求代理到后端 |
| 插件 | `@vitejs/plugin-vue` | Vue SFC 支持 |

**npm scripts：**

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 |
| `npm run build` | 生产构建 |
| `npm run preview` | 预览构建产物 |
| `npm run lint` | ESLint 代码检查 |
| `npm test` | 运行单元测试（Node.js 内置测试运行器） |
