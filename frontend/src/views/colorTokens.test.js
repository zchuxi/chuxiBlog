// 颜色令牌与样式纪律守护（P1-7 / P4 验收）：
// 1. base.css 颜色令牌的精确值（亮/暗两族，hex 或 rgba 形式）禁止在 base.css 之外的
//    非 dark 上下文直写——必须 var(--token) 引用让暗色主题自动跟随。
//    豁免：html.dark / .dark / oled 选择器块内的直写是有意的主题覆写；
//    自定义属性定义（--adm-accent: #3f77b5 这类局部令牌声明）合法。
// 2. z-index 禁止 6 位数及以上（层级区间见 docs/optimizations/style-guide.md）。
// 3. .vue 的 <style> 块禁止行首裸元素选择器（html/body/div/span/button 等）。
// 4. 除 CxMessage.vue / LayoutView.vue 外，禁止非 scoped <style>。
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, readdir } from 'node:fs/promises'
import { join } from 'node:path'
import { fileURLToPath } from 'node:url'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

async function* walk(dir) {
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const full = join(dir, entry.name)
    if (entry.isDirectory()) yield* walk(full)
    else if (/\.(css|vue)$/.test(entry.name)) yield full
  }
}

const SRC_PATH = fileURLToPath(new URL('..', import.meta.url))
async function collectFiles() {
  const files = []
  for await (const f of walk(SRC_PATH)) files.push(f)
  return files
}

// ---------- 颜色归一化（与 scripts/token-scan.mjs 同逻辑） ----------

function parseHex(h) {
  let s = h.slice(1).toLowerCase()
  if (s.length === 3 || s.length === 4) s = [...s].map(c => c + c).join('')
  if (s.length === 6) s += 'ff'
  if (s.length !== 8) return null
  const n = parseInt(s, 16)
  return [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, (n & 255) / 255]
}

function parseRgbFunc(str) {
  const m = str.match(/rgba?\(([^)]+)\)/i)
  if (!m) return null
  const parts = m[1].split(',').map(p => p.trim())
  if (parts.length < 3) return null
  const nums = parts.slice(0, 3).map(Number)
  if (nums.some(Number.isNaN)) return null
  let a = 1
  if (parts.length === 4) {
    a = Number(parts[3])
    if (Number.isNaN(a)) return null
  }
  return [...nums, a]
}

const colorKey = ([r, g, b, a]) => `${r},${g},${b},${Math.round(a * 10000) / 10000}`

/** base.css 全部颜色令牌值集合（亮 + 暗，新令牌自动纳入） */
async function tokenColorKeys() {
  const css = await read('../assets/css/base.css')
  const keys = new Set()
  for (const bm of css.matchAll(/(:root|html\.dark|html\.oled)\s*\{([^}]*)\}/g)) {
    for (const dm of bm[2].matchAll(/(--[a-zA-Z0-9-]+)\s*:\s*([^;]+);/g)) {
      const value = dm[2].trim()
      let rgba = null
      if (value.startsWith('#')) rgba = parseHex(value)
      else if (/^rgba?\(/i.test(value)) rgba = parseRgbFunc(value)
      if (rgba) keys.add(colorKey(rgba))
    }
  }
  return keys
}

/** .vue 的 <style> 块区间；.css 返回整个文件 */
function styleRanges(file, content) {
  if (file.endsWith('.css')) return [[0, content.length]]
  const ranges = []
  for (const m of content.matchAll(/<style\b[^>]*>/g)) {
    const close = content.indexOf('</style>', m.index)
    if (close > m.index) ranges.push([m.index + m[0].length, close])
  }
  return ranges
}

/** 花括号区间 + 选择器（用于 dark 上下文判断） */
function blockRanges(content) {
  const blocks = []
  const stack = []
  let selStart = 0
  for (let i = 0; i < content.length; i++) {
    if (content[i] === '{') {
      stack.push({ selector: content.slice(selStart, i).trim(), bodyStart: i + 1 })
      selStart = i + 1
    } else if (content[i] === '}') {
      const open = stack.pop()
      if (open) blocks.push({ start: open.bodyStart, end: i, selector: open.selector })
      selStart = i + 1
    }
  }
  return blocks
}

const DARK_RE = /\.dark|oled/i
const HEX_RE = /#[0-9a-fA-F]{3,8}\b/g
const RGB_RE = /rgba?\(\s*\d+\s*,\s*\d+\s*,\s*\d+\s*(?:,\s*[\d.]+\s*)?\)/gi

/** 剔除 var(...) 表达式（配对括号），避免 var(--token, #587699) 的 fallback 被误判为直写 */
function stripVarExpressions(content) {
  let out = ''
  for (let i = 0; i < content.length; i++) {
    if (content.startsWith('var(', i)) {
      let depth = 1
      let j = i + 4
      while (j < content.length && depth > 0) {
        if (content[j] === '(') depth++
        else if (content[j] === ')') depth--
        j++
      }
      out += ' '.repeat(j - i) // 保留长度，避免偏移错位
      i = j - 1
    } else {
      out += content[i]
    }
  }
  return out
}

test('base.css 之外的非 dark 上下文不直写颜色令牌值', async () => {
  const keys = await tokenColorKeys()
  const violations = []
  for (const file of await collectFiles()) {
    if (file.endsWith('base.css')) continue
    const raw = await readFile(file, 'utf8')
    const styles = styleRanges(file, raw)
    if (!styles.length) continue
    const content = stripVarExpressions(raw)
    const blocks = blockRanges(content)
    const comments = [...content.matchAll(/\/\*[\s\S]*?\*\//g)].map(m => [m.index, m.index + m[0].length])
    const inRanges = (pos, ranges) => ranges.some(([s, e]) => pos >= s && pos < e)

    for (const re of [HEX_RE, RGB_RE]) {
      for (const m of content.matchAll(re)) {
        const pos = m.index
        if (!inRanges(pos, styles) || inRanges(pos, comments)) continue
        const rgba = m[0].startsWith('#') ? parseHex(m[0]) : parseRgbFunc(m[0])
        if (!rgba || !keys.has(colorKey(rgba))) continue
        // 豁免：自定义属性定义（局部令牌声明，如 --adm-accent: #3f77b5）
        const before = content.slice(Math.max(0, pos - 120), pos)
        if (/--[\w-]+\s*:\s*[^;{}]*$/.test(before)) continue
        // 豁免：dark/oled 选择器块内的有意主题覆写
        const inner = blocks.filter(b => pos >= b.start && pos < b.end).pop()
        if (inner && DARK_RE.test(inner.selector)) continue
        // 豁免：声明内标注 token-guard-ignore（装饰色与令牌值巧合、固定深色组件等）
        const stmtEnd = content.indexOf(';', pos) < 0 ? content.length : content.indexOf(';', pos)
        const stmt = content.slice(Math.max(0, before.lastIndexOf(';') + 1), stmtEnd)
        if (stmt.includes('token-guard-ignore')) continue
        violations.push(`${file.replaceAll('\\', '/')}: ${m[0]}`)
      }
    }
  }
  assert.deepEqual(violations, [],
    '以下位置直写了颜色令牌值，应改为 var(--token) 引用（装饰巧合请在声明内加 /* token-guard-ignore */ 并注明原因）：\n'
    + violations.join('\n'))
})

test('z-index 不使用 6 位数及以上（层级区间见 style-guide）', async () => {
  const violations = []
  for (const file of await collectFiles()) {
    const content = await readFile(file, 'utf8')
    for (const m of content.matchAll(/z-index:\s*(\d{6,})/g)) {
      violations.push(`${file.replaceAll('\\', '/')}: z-index ${m[1]}`)
    }
  }
  assert.deepEqual(violations, [],
    '以下 z-index 超过 99999，违反层级区间约定：\n' + violations.join('\n'))
})

/** 提取 style 内容中以裸元素开头的选择器（跨行选择器列表安全；豁免 html.dark 主题覆写与 at-rule/keyframes） */
function bareElementSelectors(styleContent) {
  const violations = []
  const css = styleContent.replace(/\/\*[\s\S]*?\*\//g, '')
  let selStart = 0
  for (let i = 0; i < css.length; i++) {
    if (css[i] !== '{') continue
    const selText = css.slice(selStart, i).trim()
    // at-rule（@media/@keyframes/...）与 keyframes 帧（0%/from/to）不是元素选择器
    if (selText && !selText.startsWith('@') && !/^(\d+(\.\d+)?%|from|to)$/.test(selText)) {
      for (const part of selText.split(',')) {
        const p = part.trim()
        // html.dark / html.oled 主题覆写必须挂钩 html，属合法用法
        if (/^html\.(dark|oled)\b/.test(p)) continue
        if (/^(html|body|div|span|button|p|img|input|textarea|select|ul|ol|li|table)\b/.test(p)) {
          violations.push(p)
        }
      }
    }
    // 跳过整个块体，选择器只出现在块外
    let depth = 1
    while (i < css.length && depth > 0) {
      i++
      if (css[i] === '{') depth++
      else if (css[i] === '}') depth--
    }
    selStart = i + 1
  }
  return violations
}

test('.vue 的 <style> 块不使用裸元素选择器', async () => {
  const violations = []
  for (const file of await collectFiles()) {
    if (!file.endsWith('.vue')) continue
    const content = await readFile(file, 'utf8')
    for (const m of content.matchAll(/<style\b[^>]*>([\s\S]*?)<\/style>/g)) {
      for (const sel of bareElementSelectors(m[1])) {
        violations.push(`${file.replaceAll('\\', '/')}: ${sel}`)
      }
    }
  }
  assert.deepEqual(violations, [],
    '以下选择器以裸元素开头，必须使用类名（html.dark 主题覆写除外）：\n' + violations.join('\n'))
})

test('除 CxMessage / LayoutView 外不存在非 scoped <style>', async () => {
  const allowed = new Set(['CxMessage.vue', 'LayoutView.vue'])
  const violations = []
  for (const file of await collectFiles()) {
    if (!file.endsWith('.vue')) continue
    if (allowed.has(file.split(/[\\/]/).pop())) continue
    const content = await readFile(file, 'utf8')
    for (const m of content.matchAll(/<style\b([^>]*)>/g)) {
      if (!/\bscoped\b/.test(m[1])) violations.push(file.replaceAll('\\', '/'))
    }
  }
  assert.deepEqual([...new Set(violations)], [],
    '以下组件存在非 scoped <style>，违反样式规约：\n' + [...new Set(violations)].join('\n'))
})
