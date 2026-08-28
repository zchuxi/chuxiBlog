// layout.css 一次性拆分脚本（P3-2，已由人工确认结果后保留作冷档）
// 用法：node scripts/split-layout.mjs
// 原则：
// 1. 纯机械移动，规则的文本内容与相对顺序逐字节保留（@media 块拆散为「带包裹链的扁平规则」，
//    再按主题文件重组，同一文件内保持原顺序；关键路径 auth 的「基础规则在前、媒体查询在后」由此保证）
// 2. @keyframes 按被引用的使用方归类
// 3. 输出与原始规则多重集校验一致（数量 + 内容哈希）后才认为成功
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { fileURLToPath } from 'node:url'
import { join } from 'node:path'

const SRC = fileURLToPath(new URL('../src/assets/css/layout.css', import.meta.url))
const OUT_DIR = fileURLToPath(new URL('../src/assets/css/layout/', import.meta.url))

const css = readFileSync(SRC, 'utf8')

// ---------- 顶层规则切分 ----------
function splitTop(text) {
  const rules = []
  let depth = 0
  let cur = ''
  for (const ch of text) {
    cur += ch
    if (ch === '{') depth++
    else if (ch === '}') {
      depth--
      if (depth === 0) {
        rules.push(cur.trim())
        cur = ''
      }
    }
  }
  return rules.filter(Boolean)
}

// ---------- 扁平化：@media 拆为 { wrapper, body } ----------
// wrapper 为 '@media(...)' 前缀文本；keyframes 不拆
const flat = []
function flatten(text, wrapper = null) {
  for (const rule of splitTop(text)) {
    const open = rule.indexOf('{')
    const head = rule.slice(0, open)
    if (/^@media/.test(head)) {
      flatten(rule.slice(open + 1, -1), head)
    } else if (/^@keyframes/.test(head)) {
      flat.push({ wrapper, body: rule })
    } else if (/^@[a-z-]+/.test(head)) {
      throw new Error(`未知 at-rule：${head}`)
    } else {
      flat.push({ wrapper, body: rule })
    }
  }
}
flatten(css)
console.log('扁平规则数:', flat.length)

// ---------- 归类 ----------
// 取选择器中第一个非 dark/oled 的类名
function firstClass(sel) {
  for (const m of sel.matchAll(/\.([a-zA-Z0-9_-]+)/g)) {
    if (m[1] !== 'dark' && m[1] !== 'oled') return m[1]
  }
  return null
}

function bucketOf(cls) {
  if (!cls) return null
  if (/^(ai-chat|ai-markdown)/.test(cls)) return 'ai-chat'
  if (/^auth/.test(cls)) return 'auth'
  if (/^(setting|profile|background-mode-card|background-rotation-card|effect-|gallery)/.test(cls)) return 'dialogs'
  if (/^live2d/.test(cls)) return 'live2d'
  if (/^(music|control-btn|track-|test-icon-btn|top-nav-mobile-popover)/.test(cls)) return 'music'
  if (/^layout-right-sidebar/.test(cls)) return 'sidebar'
  if (/^layout-article-search/.test(cls)) return 'search'
  if (/^(app-shell|content-route|content-loader|layout-route-loader)/.test(cls)) return 'shell'
  if (/^(shell-|topbar|theme-icon|paw-rope|user-avatar|test-tag)/.test(cls)) return 'topbar'
  if (cls === 'cx-popover') return 'music' // .cx-popover.music-playlist-popover 系列
  return null
}

// @keyframes：按使用方归类
const kfUsage = {}
for (const { body } of flat) {
  if (body.startsWith('@keyframes')) continue
  const anims = [...body.matchAll(/animation(?:-name)?\s*:[^;}]+/g)].map(m => m[0])
  for (const a of anims) {
    const names = a.slice(a.indexOf(':') + 1).split(/[,\s]+/).filter(w => /^[a-z][\w-]*-\w/.test(w) && !/^(ease|linear|infinite|forwards|alternate|\d)/.test(w))
    for (const n of names) {
      if (!kfUsage[n]) kfUsage[n] = firstClass(body.slice(0, body.indexOf('{')))
    }
  }
}

const buckets = {}
const orphans = []
for (const item of flat) {
  const { body } = item
  let bucket
  if (body.startsWith('@keyframes')) {
    const name = body.match(/^@keyframes\s+([\w-]+)/)[1]
    bucket = bucketOf(kfUsage[name])
    if (!bucket) bucket = 'shell' // 未被引用的兜底
  } else {
    bucket = bucketOf(firstClass(body.slice(0, body.indexOf('{'))))
    if (!bucket) {
      orphans.push(body.slice(0, body.indexOf('{')))
      bucket = '_orphan'
    }
  }
  ;(buckets[bucket] ||= []).push(item)
}
if (orphans.length) {
  console.log('未识别选择器：\n' + orphans.join('\n'))
  process.exit(1)
}

// ---------- 输出 ----------
const ORDER = ['shell', 'topbar', 'sidebar', 'music', 'search', 'auth', 'dialogs', 'live2d', 'ai-chat']
const TITLE = {
  shell: '页面壳与背景 —— .app-shell 背景层、主区域、路由加载动画',
  topbar: '顶部导航 —— 品牌、导航项、动作按钮、歌词、主题切换、猫爪返回顶部、头像',
  sidebar: '右侧栏 —— .layout-right-sidebar 手机抽屉',
  music: '底部音乐条 —— .music-bottom-bar、曲目/控制/进度/音量、歌单弹层',
  search: '站内文章搜索浮层 —— .layout-article-search-*',
  auth: '登录/注册对话框 —— .auth-*（同特异性靠源码顺序，基础规则必须保持在前、媒体查询在后）',
  dialogs: '设置弹窗 —— .setting-*、背景模式切换、特效卡片、画廊、个人资料',
  live2d: 'live2d 看板娘 —— .live2d-widget、聊天气泡',
  'ai-chat': '站内 AI 助手 —— .ai-chat-*、.ai-markdown-*',
}

mkdirSync(OUT_DIR, { recursive: true })
const emitted = []
for (const name of ORDER) {
  const items = buckets[name] || []
  // 按 wrapper 聚合，保持首个出现位置；同 wrapper 内保持原顺序
  const blocks = []
  const idxByWrapper = new Map()
  for (const item of items) {
    if (!item.wrapper) {
      blocks.push({ wrapper: null, bodies: [item.body] })
      continue
    }
    if (!idxByWrapper.has(item.wrapper)) {
      idxByWrapper.set(item.wrapper, blocks.length)
      blocks.push({ wrapper: item.wrapper, bodies: [] })
    }
    blocks[idxByWrapper.get(item.wrapper)].bodies.push(item.body)
  }
  const lines = blocks.map(b => {
    if (!b.wrapper) return b.bodies.join('\n')
    return `${b.wrapper}{\n${b.bodies.join('\n')}\n}`
  })
  const content = `/* ${name}.css —— ${TITLE[name]}\n   由 layout.css 拆分而来（P3-2），纯机械移动；规则顺序与原文件一致。 */\n\n${lines.join('\n\n')}\n`
  writeFileSync(join(OUT_DIR, `${name}.css`), content)
  for (const it of items) emitted.push(it)
  console.log(`${name}.css 规则数:${items.length}`)
}

// ---------- 校验：输出多重集与原始一致 ----------
const hash = s => createHash('sha1').update(s).digest('hex')
const norm = s => s.replace(/\s+/g, '')
const a = flat.map(x => hash(norm((x.wrapper || '') + '|' + x.body))).sort()
const b = emitted.map(x => hash(norm((x.wrapper || '') + '|' + x.body))).sort()
if (a.length !== b.length || a.some((v, i) => v !== b[i])) {
  console.error('校验失败：输出规则集与原件不一致')
  process.exit(1)
}
console.log('校验通过：输出规则与原文件逐一对应')
