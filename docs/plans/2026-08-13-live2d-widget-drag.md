# Live2D 看板娘拖动实现计划

> **For implementer:** Use TDD throughout. Write failing test first. Watch it fail. Then implement.

**Goal:** 按已批准的方案 A，让用户通过模型展示区域拖动整个 Miku 看板娘，并在刷新、开关和重新加载后保留位置。

**Architecture:** 新增独立的 `live2dWidgetDrag.js` 管理位置解析、视口边界、Pointer Events 和 `localStorage`。`LayoutView.vue` 只提供根元素与拖动手柄引用，并在生命周期中绑定和清理，模型加载逻辑保持不变。

**Tech Stack:** Vue 3、原生 Pointer Events、CSS variables、Node 内置测试器。

---

### Task 1: 纯逻辑与拖动会话测试

**Files:**
- Create: `frontend/src/live2d/live2dWidgetDrag.test.js`
- Create: `frontend/src/live2d/live2dWidgetDrag.js`

1. 为非法存储回退、边界约束、拖动位移、结束持久化、按钮不绑定、清理监听编写失败测试。
2. 运行 `node --test src/live2d/live2dWidgetDrag.test.js`，确认因模块或导出缺失而失败。
3. 用最小实现提供 `readLive2dPosition`、`clampLive2dPosition` 与 `bindLive2dWidgetDrag`。
4. 再次运行定向测试，确认通过。

### Task 2: Vue 生命周期接入

**Files:**
- Modify: `frontend/src/layout/LayoutView.vue`
- Modify: `frontend/src/assets/css/layout.css`（仅在现有拖动态样式不足时修改）

1. 为看板娘根元素和舞台添加 Vue `ref`。
2. 在 `onMounted` 后绑定拖动，在 `onBeforeUnmount` 中清理。
3. 保持按钮点击区域、模型加载、开关和水印逻辑不变。

### Task 3: 完整验证

**Files:**
- Verify: `frontend/src/live2d/live2dWidgetDrag.test.js`
- Verify: `frontend/src/layout/LayoutView.vue`

1. 运行 `npm run lint`。
2. 运行 `npm test`。
3. 运行 `npm run build`。
4. 在浏览器中检查：模型区域可拖动、按钮仍可点击、边缘约束生效、刷新后位置恢复。
