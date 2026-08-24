# 后台控件层复用前台 cx 组件体系 — 设计文档

日期：2026-08-24
状态：已与用户确认设计，待实施

## 一、背景与问题

前台已有完整的 cx 组件库（`frontend/src/components/cx/`：CxButton、CxSwitch、
CxTag、CxRadio/CxRadioGroup、CxMessage、CxPopover、CxDatePicker），基于 base.css
全局令牌（`--accent-*`、`--card-bg`、`--input-bg` 等）实现玻璃拟态设计。前后台
同属一个 SPA，这些 CSS 变量在后台 DOM 中同样可用。

后台（`frontend/src/views/admin/`）在 admin.css 中自建了完全独立的 `--adm-*`
令牌体系与控件样式，与前台的复用关系几乎为零：

- `admin-btn`：117 处使用（19 个文件），未复用 CxButton
- `admin-switch`：FieldInput.vue、AiConfigPanel.vue 两处，未复用 CxSwitch
- `admin-toast`（AdminView.vue 自建右上角 toast），未复用 CxMessage
- `admin-input`/`admin-badge`/`admin-state`/`admin-link` 等视觉语言与前台不一致
- 已有先例：FieldInput.vue 的 date/datetime 字段已复用 CxDatePicker

两套体系品牌色同源（`--adm-accent` 与前台 `--accent-strong` 同为 `#3f77b5`），
但视觉语言不同：前台玻璃拟态（半透明 + blur + 扫光），后台实色扁平。

## 二、目标

后台**表单控件层**完全复用前台 cx 组件——视觉、交互、尺寸与前台完全一致；
**布局层**（sidebar、topbar、卡片骨架、表格）保留 `--adm-*` 令牌不动。

## 三、决策记录（用户已确认）

| 决策点 | 结论 |
|---|---|
| 统一程度 | 完全统一：后台直接用前台 cx 组件与玻璃拟态视觉，删除 admin.css 重复样式 |
| 范围 | 按钮→CxButton、开关→CxSwitch、Toast→CxMessage、输入框/徽章视觉对齐（四项全选） |
| 尺寸 | CxButton 原样使用（36px 高 / min-width 92px），cx 组件零改动 |
| 执行策略 | 分三批连续执行，每批独立验证（lint + test + build）并独立提交 |

## 四、批次设计

### 批次一：按钮 + 开关（模板替换）

映射规则（全站机械统一）：

| 现状 | 替换为 |
|---|---|
| `class="admin-btn"` | `<CxButton>`（primary） |
| `admin-btn admin-btn-ghost` | `<CxButton plain>` |
| `admin-btn admin-btn-danger` | `<CxButton type="danger">` |
| `admin-btn admin-btn-block` | `<CxButton class="admin-block">` |
| `class="admin-switch"` | `<CxSwitch v-model>` |

实施要点：

- 涉及 19 个文件的 117 处 `admin-btn`、2 个文件的 `admin-switch`
- `@click`/`disabled` 原样保留；“上传中…进度%”等文本态变化保持现有文本方式，
  不引入 CxButton 的 loading prop
- `type="submit"`（登录按钮）用 CxButton 的 `native-type="submit"`
- CxSwitch 非 props 属性（id/name/aria-*）自动透传到根 button，可无痕替换
  FieldInput 的 boolean 字段与 AiConfigPanel 的开关
- 图片字段按钮组（上传/图库/裁切）容器加 `flex-wrap: wrap`，兜底 92px 最小宽度
- admin.css 删除 `.admin-btn*`、`.admin-switch*` 全部样式块，新增 3 行辅助类
  `.cx-button.admin-block { width: 100%; min-width: 0; margin-top: 8px; }`
- 同步更新 adminStructure.test.js 中断言 `admin-btn` 的测试

### 批次二：Toast → CxMessage

- AdminView.vue：`<CxMessage ref="msgRef" />` 替换 admin-toasts 模板块；
  `toast(text, type)` 内部改调 `msgRef.value.pushMessage(type, text)`；
  `provide('adminToast', toast)` 接口不变——所有子组件 `inject('adminToast')`
  零改动
- 删除 admin.css 的 `.admin-toasts`、`.admin-toast*` 及 `admin-toast-*` 动效样式
- 清理样式冲突：base.css 与 CxMessage.vue 内部存在两套全局 `.cx-message-*`
  样式（容器位置不同：base.css 版 left:20px，组件版 top:24px 居中）。保留
  CxMessage.vue 组件内一套，删除 base.css 中的重复块；实施前先 grep 确认
  base.css 版样式没有其他使用方
- 行为变化（已确认接受）：toast 显示位置从右上角变为与前台一致（顶部居中）

### 批次三：输入框 / 徽章 / link 视觉对齐（只改 admin.css）

- `.admin-input` / `.admin-textarea`：背景/边框/focus 改引用前台令牌
  `--input-bg`、`--input-border`、`--accent-solid`、`--accent-glow`（玻璃拟态
  focus 光晕）；number/date 的 `color-scheme`、spin 按钮隐藏等特殊处理保留
- `.admin-badge` / `.admin-state`：对齐 cx-tag.css 令牌视觉
- `.admin-link`：hover/focus 交互模式对齐前台（保留紧凑尺寸，表格操作列不放大）
- 删除 admin.css 中控件层不再使用的 `--adm-*` 令牌（input/accent 相关），
  布局令牌（bg-grad/card/sidebar 等）保留

## 五、验证方式（每批必做）

- `frontend/` 目录运行 `npm run lint`、`npm test`、`npm run build`（AGENTS.md 硬约束）
- 每批完成后运行 `graphify update .` 保持知识图谱最新
- 手动检查点：
  - 批次一：登录页按钮、各面板工具栏按钮、图片字段按钮组换行、AiConfigPanel
    开关、深色模式
  - 批次二：登录成功/失败 toast、表单保存 toast、深色模式
  - 批次三：各表单输入框 focus 光晕、徽章/状态标签、表格操作列 hover
- 后端无改动，无需跑 `mvn test`

## 六、风险与边界

- CxButton `min-width: 92px` 在个别紧凑处偏宽——flex-wrap 兜底，不做 size 变体
- toast 位置变化（右上角 → 顶部居中）——用户已确认接受
- cx 组件样式依赖的前台令牌定义在 base.css `:root`/`html.dark`，全局可用，
  后台无需重复定义
- admin.css 中布局层类（~110 个类中的 sidebar/topbar/table/modal/drawer/pager
  等）不在本次范围，保持现状
