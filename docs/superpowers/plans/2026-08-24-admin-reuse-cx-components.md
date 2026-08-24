# 后台控件层复用前台 cx 组件体系 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 后台表单控件层（按钮/开关/Toast/输入框/徽章）完全复用前台 cx 组件体系，删除 admin.css 中的重复控件样式，全站视觉与交互统一。

**Architecture:** 三批连续改造：批次一（按钮+开关模板替换，19 文件）、批次二（Toast 复用全局 CxMessage 单例）、批次三（输入框/徽章视觉对齐，只改 admin.css）。cx 组件本身零改动；后台布局层（sidebar/topbar/table/modal）保留 `--adm-*` 令牌。

**Tech Stack:** Vue 3 (script setup) + Vite；组件从 `frontend/src/components/cx/` 直接路径 import；样式为全局 CSS（无 scoped 隔离 admin.css，由 AdminView.vue:162 组件级 import，加载顺序在 main.js 的 cx-*.css 之后）。

**设计文档:** `docs/superpowers/specs/2026-08-24-admin-reuse-cx-components-design.md`

---

## 关键事实（执行前必读）

1. **CxButton API**（`frontend/src/components/cx/CxButton.vue`）：
   - props：`type`（primary/success/warning/danger/info/section，默认 primary）、`plain`、`round`、`circle`、`loading`、`disabled`、`nativeType`（默认 `'button'`）
   - 事件：`@click`；slot 为按钮文字。样式来自全局 `cx-button.css`（36px 高、min-width 92px、玻璃拟态）
2. **CxSwitch API**（`frontend/src/components/cx/CxSwitch.vue`）：props `modelValue`（Boolean）、`disabled`；`v-model` 可用；根元素自带 `role="switch"` + `aria-checked`；**非 props 属性（id/name/aria-label/aria-invalid 等）自动透传到根 button**，无需额外处理
3. **全局 Toast 单例**：`App.vue:3` 已挂载 `<CxMessage ref="msgRef" />`（/admin 路由也在 App.vue 内渲染，后台可用）；`frontend/src/utils/toast.js` 提供 `toastSuccess/toastError/toastInfo(text, duration)`，已 `registerToast` 注册
4. **admin.css 由 `AdminView.vue:162` import**，加载顺序在 `main.js` 中 base.css/cx-*.css 之后。因此 admin.css 中 `.admin-root :is(button, ...):focus-visible` 统一焦点规则（特异性更高 + 源码靠后）会覆盖 CxButton 自带的 focus-visible 阴影——**这是预期行为**（后台统一键盘焦点环 `--adm-focus-ring`），不要"修复"
5. **验证硬约束**（AGENTS.md）：前端改动后在 `frontend/` 运行 `npm run lint`、`npm test`、`npm run build`。后端无改动，不需要 `mvn test`
6. **提交纪律**：本计划共 3 个提交（每批一个），提交信息用中文，格式与仓库历史一致（`feat:`/`refactor:` 前缀）

---

## 批次一：按钮 + 开关模板替换

### 统一映射规则（Task 2/3 所有替换都遵循）

import 语句（加在各组件 `<script setup>` 顶部现有 import 附近；如组件已有 cx 组件 import 则合并）：

```js
import CxButton from '../../components/cx/CxButton.vue'
import CxSwitch from '../../components/cx/CxSwitch.vue'
```

模板替换规则（`type="button"` 可省略，CxButton 的 nativeType 默认就是 button）：

| 原写法 | 新写法 |
|---|---|
| `<button class="admin-btn" type="button" ...>` | `<CxButton ...>` |
| `<button class="admin-btn admin-btn-ghost" ...>` | `<CxButton plain ...>` |
| `<button class="admin-btn admin-btn-danger" ...>` | `<CxButton type="danger" ...>` |
| `<button class="admin-btn admin-btn-block" type="submit" ...>` | `<CxButton class="admin-block" native-type="submit" ...>` |
| `<button class="admin-switch" ...><span class="admin-switch-dot"></span></button>` | `<CxSwitch v-model="..." ... />` |

所有 `:disabled`、`@click`、动态文字（`{{ saving ? '保存中…' : '保存' }}`）原样保留。若按钮内文字为空仅图标，仍直接放 slot。

### Task 1: 开关替换（FieldInput + AiConfigPanel）

**Files:**
- Modify: `frontend/src/views/admin/FieldInput.vue:8-25`（boolean 分支）
- Modify: `frontend/src/views/admin/AiConfigPanel.vue:27-37`（启用站内 AI 开关）

- [ ] **Step 1: 替换 FieldInput.vue 的 boolean 开关**

原代码（L8-25）：

```vue
    <!-- 布尔开关 -->
    <button
      v-if="field.type === 'boolean'"
      type="button"
      :id="inputId"
      :name="field.name"
      class="admin-switch"
      :class="{ on: !!modelValue }"
      :aria-pressed="!!modelValue"
      :aria-label="field.label"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      :aria-required="field.required ? 'true' : undefined"
      :disabled="disabled"
      @click="emit('update:modelValue', !modelValue)"
    >
      <span class="admin-switch-dot"></span>
    </button>
```

替换为（CxSwitch 内置 role="switch"/aria-checked，原 aria-pressed 删除；其余属性透传）：

```vue
    <!-- 布尔开关 -->
    <CxSwitch
      v-if="field.type === 'boolean'"
      :id="inputId"
      :name="field.name"
      :model-value="!!modelValue"
      :aria-label="field.label"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="describedBy"
      :aria-required="field.required ? 'true' : undefined"
      :disabled="disabled"
      @update:model-value="v => emit('update:modelValue', v)"
    />
```

在同文件 `<script setup>` 的 `import CxDatePicker ...`（L200）后新增：

```js
import CxSwitch from '../../components/cx/CxSwitch.vue'
```

- [ ] **Step 2: 替换 AiConfigPanel.vue 的开关**

原代码（L27-37）：

```vue
          <button
            type="button"
            class="admin-switch"
            :class="{ on: form.enabled }"
            role="switch"
            :aria-checked="form.enabled"
            aria-label="启用站内 AI"
            @click="form.enabled = !form.enabled"
          >
            <span class="admin-switch-dot"></span>
          </button>
```

替换为：

```vue
          <CxSwitch v-model="form.enabled" aria-label="启用站内 AI" />
```

在 `<script setup>` import 区新增（若 Task 2 已由他人完成 import 则跳过重复）：

```js
import CxSwitch from '../../components/cx/CxSwitch.vue'
```

- [ ] **Step 3: 验证无残留**

Run: `grep -rn "admin-switch" frontend/src/views/`
Expected: 无任何输出（样式清理在 Task 4 做，模板必须先干净）

---

### Task 2: 基础层按钮替换（7 个文件）

**Files（admin-btn 出现次数）:**
- `frontend/src/views/admin/AdminView.vue`（2 处，含 admin-btn-block + type="submit" 登录按钮 L21）
- `frontend/src/views/admin/PasswordDialog.vue`（3 处，含 type="submit" L25）
- `frontend/src/views/admin/CropDialog.vue`（3 处）
- `frontend/src/views/admin/ImageSelect.vue`（4 处）
- `frontend/src/views/admin/MediaPicker.vue`（2 处）
- `frontend/src/views/admin/FieldInput.vue`（8 处，Task 1 已改此文件的开关）
- `frontend/src/views/admin/DashboardPanel.vue`（2 处）

- [ ] **Step 1: 逐文件按映射规则替换**

每个文件：新增 `import CxButton from '../../components/cx/CxButton.vue'`（FieldInput 与 CxSwitch import 并列），然后按统一映射规则替换所有 `admin-btn` 按钮。

AdminView.vue L21 特例（block + submit）：

```vue
<!-- 原 -->
<button class="admin-btn admin-btn-block" type="submit" :disabled="loggingIn">
<!-- 改为 -->
<CxButton class="admin-block" native-type="submit" :disabled="loggingIn">
```

PasswordDialog.vue L25 特例（submit）：

```vue
<!-- 原 -->
<button class="admin-btn" type="submit" :disabled="saving">{{ saving ? '保存中…' : '确认修改' }}</button>
<!-- 改为 -->
<CxButton native-type="submit" :disabled="saving">{{ saving ? '保存中…' : '确认修改' }}</CxButton>
```

- [ ] **Step 2: 验证本组无残留**

Run: `grep -rn "admin-btn" frontend/src/views/admin/AdminView.vue frontend/src/views/admin/PasswordDialog.vue frontend/src/views/admin/CropDialog.vue frontend/src/views/admin/ImageSelect.vue frontend/src/views/admin/MediaPicker.vue frontend/src/views/admin/FieldInput.vue frontend/src/views/admin/DashboardPanel.vue`
Expected: 无输出

---

### Task 3: 面板层按钮替换（12 个文件）

**Files（admin-btn 出现次数）:**
- `frontend/src/views/admin/ArticlesPanel.vue`（18 处，含 admin-btn-danger L27）
- `frontend/src/views/admin/NavMenuPanel.vue`（13 处）
- `frontend/src/views/admin/ScenePanel.vue`（13 处）
- `frontend/src/views/admin/BackgroundPanel.vue`（11 处）
- `frontend/src/views/admin/ResourcePanel.vue`（12 处，含 admin-btn-danger L65）
- `frontend/src/views/admin/PageContentPanel.vue`（8 处）
- `frontend/src/views/admin/BangumiPanel.vue`（7 处）
- `frontend/src/views/admin/AiConfigPanel.vue`（3 处，Task 1 已改此文件的开关）
- `frontend/src/views/admin/MediaPanel.vue`（3 处）
- `frontend/src/views/admin/AppearancePanel.vue`（1 处）
- `frontend/src/views/admin/SiteContentPanel.vue`（1 处）
- `frontend/src/views/admin/SiteSettingsPanel.vue`（1 处）

- [ ] **Step 1: 逐文件按统一映射规则替换**

每个文件新增 `import CxButton from '../../components/cx/CxButton.vue'`，按映射规则替换。danger 两处示例：

```vue
<!-- ArticlesPanel.vue L27 / ResourcePanel.vue L65 原写法 -->
<button class="admin-btn admin-btn-danger" type="button" :disabled="batching" @click="batchRemove">
<!-- 改为 -->
<CxButton type="danger" :disabled="batching" @click="batchRemove">
```

注意闭合标签：`</button>` 必须同步改为 `</CxButton>`。

BangumiPanel.vue 特例（L529-532，scoped 样式引用了将被删除的类名）：

```css
/* 原 */
.bgm-result-item .admin-btn-ghost {
  margin-left: auto;
  padding: 8px 16px;
}
/* 改为（CxButton plain 变体的类名是 is-plain；scoped 样式可命中子组件根元素） */
.bgm-result-item .cx-button.is-plain {
  margin-left: auto;
  padding: 8px 16px;
}
```

- [ ] **Step 2: 验证全后台无 admin-btn 残留**

Run: `grep -rn "admin-btn" frontend/src/views/`
Expected: 无输出（admin.css 内的样式块在 Task 4 删除）

---

### Task 4: admin.css 样式清理 + 测试更新 + 批次一提交

**Files:**
- Modify: `frontend/src/assets/css/admin.css`（删除按钮/开关样式块，新增 admin-block 辅助类）
- Modify: `frontend/src/views/admin/adminStructure.test.js:37-44`（更新按钮层叠断言）
- Test: `frontend/src/views/admin/adminStructure.test.js`

- [ ] **Step 1: 删除 admin.css 按钮样式块（L72-111 区域）**

删除以下选择器块（含 L72 注释行 `/* ---------- 通用按钮 / 链接 ---------- */` 中的"按钮"部分——`.admin-link` 系列保留）：

- `.admin-btn { ... }`（L74-84）
- `.admin-btn:hover:not(:disabled) { ... }`（L86-90）
- `.admin-btn:disabled { ... }`（L92-95）
- `.admin-btn-ghost { ... }`（L97-101）
- `.admin-btn-ghost:hover:not(:disabled) { ... }`（L103-106）
- `.admin-btn-block { ... }`（L108-111）

原位置替换为：

```css
/* 全宽按钮（登录页等）：CxButton 默认 min-width 92px，全宽场景取消 */
.cx-button.admin-block {
  width: 100%;
  min-width: 0;
  margin-top: 8px;
}
```

- [ ] **Step 2: 删除 admin.css 开关样式块（L409-439 区域）**

删除 `/* 开关 */` 注释及以下 4 块：`.admin-switch`、`.admin-switch.on`、`.admin-switch-dot`、`.admin-switch.on .admin-switch-dot`。

- [ ] **Step 3: 更新 admin.css 图片字段按钮组规则（L1981）**

原：

```css
.admin-img-actions .admin-btn { flex: 1; min-width: 118px; }
```

改为（覆盖 CxButton 的 92px 最小宽度，允许按钮在窄侧栏等宽压缩，容器已有 flex-wrap: wrap）：

```css
.admin-img-actions .cx-button { flex: 1; min-width: 0; }
```

- [ ] **Step 4: 更新 adminStructure.test.js 按钮层叠断言（L37-44）**

原测试：

```js
test('统一键盘焦点规则以足够优先级覆盖按钮 hover 阴影', async () => {
  const css = await read('../../assets/css/admin.css')
  const selector = '.admin-root :is(button, a, input, textarea, select, [tabindex]):focus-visible'
  const focusIndex = css.lastIndexOf(selector)
  assert.ok(focusIndex > css.lastIndexOf('.admin-btn:hover:not(:disabled)'))
  assert.ok(focusIndex > css.lastIndexOf('.admin-btn-ghost:hover:not(:disabled)'))
  assert.match(extractBlock(css, focusIndex), /box-shadow:\s*var\(--adm-focus-ring\)/)
})
```

改为（admin-btn 样式已删，改为守护「清理完成 + 焦点规则仍存在」）：

```js
test('统一键盘焦点规则存在，且自建按钮样式已迁移到 cx-button', async () => {
  const css = await read('../../assets/css/admin.css')
  const selector = '.admin-root :is(button, a, input, textarea, select, [tabindex]):focus-visible'
  const focusIndex = css.lastIndexOf(selector)
  assert.ok(focusIndex > 0)
  // admin-btn/admin-switch 样式已由 CxButton/CxSwitch 取代，不允许回潮
  assert.ok(!css.includes('.admin-btn'))
  assert.ok(!css.includes('.admin-switch'))
  assert.match(extractBlock(css, focusIndex), /box-shadow:\s*var\(--adm-focus-ring\)/)
})
```

- [ ] **Step 5: 运行批次一完整验证**

Run（在 `frontend/` 目录）: `npm run lint; npm test; npm run build`
Expected: 三条命令全部通过（0 error / test 全绿 / build 成功）

- [ ] **Step 6: 提交批次一**

```bash
git add frontend/src/views/admin/ frontend/src/assets/css/admin.css
git commit -m "refactor: 后台按钮与开关替换为前台 CxButton/CxSwitch，统一全站控件视觉"
```

---

## 批次二：Toast 复用全局 CxMessage 单例

### Task 5: AdminView toast 替换 + 样式清理

**Files:**
- Modify: `frontend/src/views/admin/AdminView.vue`（L130-134 模板、L239-246 script）
- Modify: `frontend/src/assets/css/admin.css`（L1138-1175 Toast 样式块）
- Modify: `frontend/src/assets/css/base.css`（删除与 CxMessage.vue 冲突的死样式）

- [ ] **Step 1: 替换 AdminView.vue 模板**

删除 L130-134：

```vue
    <!-- 右上角简易 toast -->
    <div class="admin-toasts">
      <transition-group name="admin-toast">
        <div v-for="t in toasts" :key="t.id" class="admin-toast" :class="t.type">{{ t.text }}</div>
      </transition-group>
    </div>
```

（全局 `<CxMessage ref="msgRef" />` 已由 App.vue 挂载，admin 路由同在其渲染树内，无需再挂。）

- [ ] **Step 2: 替换 AdminView.vue script 的 toast 实现**

删除 L239-246 的自建实现：

```js
// 简易 toast：右上角堆叠，自动消失
const toasts = ref([])
let toastSeq = 0
function toast(text, type = 'success') {
  const id = ++toastSeq
  toasts.value.push({ id, text, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, 2600)
}
```

替换为（复用全局单例；`provide('adminToast', toast)` L281 及所有子组件 `inject('adminToast')` 零改动）：

```js
// toast 复用 App.vue 挂载的全局 CxMessage 单例（与前台同款提示）
import { toastSuccess, toastError } from '../../utils/toast'
function toast(text, type = 'success') {
  ;(type === 'error' ? toastError : toastSuccess)(text)
}
```

（import 按文件内现有 import 顺序放置，`provide` 行不动。）

- [ ] **Step 3: 删除 admin.css Toast 样式块**

删除 `/* ---------- Toast ---------- */` 注释及其后全部 4 块（约 L1138-1175）：`.admin-toasts`、`.admin-toast`、`.admin-toast.error`、`.admin-toast-enter-active/.admin-toast-leave-active`、`.admin-toast-enter-from/.admin-toast-leave-to`。

- [ ] **Step 4: 清理 base.css 中与 CxMessage.vue 冲突的死样式**

base.css（单行压缩文件）中存在一套 `.cx-message-container`（`left: 20px; width: 420px;` 版本）及 `.cx-message-item`、`.message-icon`、`.message-content`、`.message-close-btn`、`.cx-message-slide-*` 规则——它们属于带关闭按钮的旧版实现，当前 CxMessage.vue 不渲染这些结构（已确认全仓库仅 CxMessage.vue 与本文件引用这些类名），且与 CxMessage.vue 组件内样式（`top: 24px; left: 50%;` 版本）冲突。base.css 先加载、组件样式后注入，组件版胜出——base.css 版本是死代码。

用 Edit 精确删除 base.css 中从 `.cx-message-container{--message-gap: 12px;` 开始到文件中 `.cx-message-slide-move{transition:transform .35s cubic-bezier(.4,0,.2,1)}` 结束的整段（位于该单行文件末尾区域）。删除前先 `grep -o` 截取该段确认边界。**禁止重排文件其余内容**。

- [ ] **Step 5: 验证 Toast 批次**

Run: `grep -rn "admin-toast" frontend/src/`
Expected: 无输出

Run（在 `frontend/` 目录）: `npm run lint; npm test; npm run build`
Expected: 全部通过。手动检查点（可启动 dev server）：登录成功/失败 toast 出现在顶部居中（与前台一致）、保存操作 toast 正常消失。

- [ ] **Step 6: 提交批次二**

```bash
git add frontend/src/views/admin/AdminView.vue frontend/src/assets/css/admin.css frontend/src/assets/css/base.css
git commit -m "refactor: 后台 toast 复用全局 CxMessage 单例，清理自建与冲突样式"
```

---

## 批次三：输入框 / 徽章 / link 视觉对齐（只改 admin.css）

**原则：** 类名与模板不动，只把样式定义改为引用前台令牌；**focus 环保留 `--adm-focus-ring`**（后台键盘焦点可访问性基建，adminStructure.test.js L30-32 有断言，且统一焦点规则同时覆盖新引入的 CxButton/CxSwitch，行为一致）。

### Task 6: admin.css 控件视觉对齐 + 令牌收尾

**Files:**
- Modify: `frontend/src/assets/css/admin.css`（.admin-input 系列、.admin-badge、.admin-link、令牌清理）
- Test: `frontend/src/views/admin/adminStructure.test.js`（现有断言必须继续通过）

- [ ] **Step 1: .admin-input 对齐玻璃拟态令牌**

`.admin-input`（L154-166）改为：

```css
.admin-input {
  width: 100%;
  padding: 9px 13px;
  border: 1px solid var(--input-border);
  border-radius: 12px;
  background-color: var(--input-bg);
  color: var(--adm-text);
  font-family: inherit;
  font-size: 15px;
  outline: none;
  box-sizing: border-box;
  transition: border-color 0.24s ease, box-shadow 0.24s ease, background-color 0.24s ease;
}
```

`.admin-input:focus`（L168-171）改为（焦点环保留后台统一规则）：

```css
.admin-input:focus {
  border-color: var(--accent-solid);
  box-shadow: var(--adm-focus-ring);
}
```

`.admin-input[type='date']/.admin-input[type='datetime-local']` 两块（L194-216）中的 `--adm-input-bg`/`--adm-input-border` 引用同步改为 `--input-bg`/`--input-border`（其余 color-scheme 等保留原样）。

- [ ] **Step 2: .admin-badge 对齐 CxTag 视觉**

`.admin-badge`（L982-994）改为（取 CxTag primary/neutral 变体同款视觉值，含深色）：

```css
.admin-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  border: 1px solid #cadbee;
  background: linear-gradient(135deg, #eef5fd, #e0ecf9);
  color: #264968;
}

.admin-badge.off {
  border-color: rgba(141, 154, 173, 0.2);
  background: linear-gradient(135deg, rgba(84, 95, 110, 0.24), rgba(60, 69, 82, 0.34));
  color: #e4ebf5;
}

html.dark .admin-badge {
  border-color: rgba(126, 160, 198, 0.28);
  background: linear-gradient(135deg, rgba(73, 104, 140, 0.22), rgba(52, 76, 104, 0.32));
  color: #deebf8;
}
```

- [ ] **Step 3: .admin-link 颜色对齐前台令牌**

`.admin-link`（L113-122）中 `color: var(--adm-accent-ink)` 改为 `color: var(--accent-strong)`；`.admin-link:hover`（L124-126）的 `--adm-accent-soft` 保留（同色系 soft 背景，视觉一致）；`.admin-link.danger` 保留 `--adm-danger`。只改这一处颜色引用，尺寸不动。

- [ ] **Step 4: 清理控件层不再使用的 --adm-* 令牌**

删除 L22-23 的 `--adm-input-bg`、`--adm-input-border`（:root）与 L49-50（html.dark）两处定义。删除前验证无残留：

Run: `grep -n "adm-input-bg\|adm-input-border" frontend/src/assets/css/admin.css`
Expected: 仅剩令牌定义 4 行（删除后为 0）。若出现其他引用，将该引用一并改为 `--input-bg`/`--input-border` 后再删令牌。

**注意：`--adm-accent` 系列令牌保留**（admin-link hover、登录页 brand、布局层仍在用）。

- [ ] **Step 5: 运行批次三完整验证**

Run（在 `frontend/` 目录）: `npm run lint; npm test; npm run build`
Expected: 全部通过。adminStructure.test.js 中 `.admin-input:focus` 断言（L30-31）必须继续通过——Step 1 已保留 `box-shadow: var(--adm-focus-ring)`。

手动检查点：表单输入框 focus 有主题色边框 + 光晕；徽章为浅蓝渐变胶囊；深色模式下输入框/徽章正常。

- [ ] **Step 6: 更新知识图谱并提交批次三**

```bash
graphify update .
git add frontend/src/assets/css/admin.css graphify-out/
git commit -m "style: 后台输入框与徽章对齐前台 cx 令牌，视觉与全站统一"
```

---

## 完成标准（对照设计文档）

- [ ] `grep -rn "admin-btn\|admin-switch\|admin-toast" frontend/src/` 无输出
- [ ] admin.css 中不存在 `.admin-btn`、`.admin-switch`、`.admin-toast`、`--adm-input-bg`、`--adm-input-border`
- [ ] 3 个独立提交，每个提交后 lint/test/build 全绿
- [ ] cx 组件库（components/cx/ 与 cx-*.css）零改动
- [ ] 后台布局层类（sidebar/topbar/table/modal/drawer/pager）未被触碰
