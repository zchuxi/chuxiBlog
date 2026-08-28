#!/usr/bin/env node
/**
 * token-apply.mjs
 * ===============
 * P1 阶段 A：把"与 base.css 亮色令牌值精确相同"的直写颜色替换为 var(--token)。
 *
 * 安全规则：
 * - 跳过 base.css 本身（令牌定义处）
 * - 跳过 .vue 文件 <style> 块之外的内容（模板 SVG fill、script 字符串不动）
 * - 跳过 CSS 注释内的匹配
 * - 跳过处于 html.dark / .dark / oled 选择器上下文中的匹配（暗色覆写是有意的）
 * - 只替换颜色令牌（渐变、尺寸、字体类令牌不参与）
 *
 * 用法：
 *   node scripts/token-apply.mjs          # dry-run，只输出计划
 *   node scripts/token-apply.mjs --write  # 实际写入
 */

import { readFileSync, writeFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const SRC = join(fileURLToPath(new URL('.', import.meta.url)), '..', 'src')
const BASE_CSS = join(SRC, 'assets', 'css', 'base.css')
const WRITE = process.argv.includes('--write')

// ---------- 颜色归一化（与 token-scan.mjs 同逻辑） ----------

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

function colorKey([r, g, b, a]) {
  return `${r},${g},${b},${Math.round(a * 10000) / 10000}`
}

// ---------- 亮色令牌：颜色值 -> 令牌名 ----------

function lightTokenMap() {
  const css = readFileSync(BASE_CSS, 'utf8')
  const map = new Map() // colorKey -> tokenName
  const rootBlocks = css.matchAll(/:root\s*\{([^}]*)\}/g)
  for (const bm of rootBlocks) {
    for (const dm of bm[1].matchAll(/(--[a-zA-Z0-9-]+)\s*:\s*([^;]+);/g)) {
      const value = dm[2].trim()
      let rgba = null
      if (value.startsWith('#')) rgba = parseHex(value)
      else if (/^rgba?\(/i.test(value)) rgba = parseRgbFunc(value)
      if (rgba && !map.has(colorKey(rgba))) map.set(colorKey(rgba), dm[1])
    }
  }
  return map
}

// ---------- 文件遍历 ----------

function* walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) yield* walk(full)
    else if (/\.(css|vue)$/.test(entry)) yield full
  }
}

/** 提取 .vue 的 <style> 块区间；.css 返回整个文件 */
function styleRanges(file, content) {
  if (file.endsWith('.css')) return [[0, content.length]]
  const ranges = []
  for (const m of content.matchAll(/<style\b[^>]*>/g)) {
    const close = content.indexOf('</style>', m.index)
    if (close > m.index) ranges.push([m.index + m[0].length, close])
  }
  return ranges
}

/** CSS 注释区间 */
function commentRanges(content) {
  const ranges = []
  for (const m of content.matchAll(/\/\*[\s\S]*?\*\//g)) ranges.push([m.index, m.index + m[0].length])
  return ranges
}

/** 花括号区间 + 选择器：返回 [{start, end, selector}]，start/end 为块体范围 */
function blockRanges(content) {
  const blocks = []
  const stack = []
  let selStart = 0
  for (let i = 0; i < content.length; i++) {
    const c = content[i]
    if (c === '{') {
      const selector = content.slice(selStart, i).trim()
      stack.push({ selector, bodyStart: i + 1 })
      selStart = i + 1
    } else if (c === '}') {
      const open = stack.pop()
      if (open) blocks.push({ start: open.bodyStart, end: i, selector: open.selector })
      selStart = i + 1
    }
  }
  return blocks
}

const inRanges = (pos, ranges) => ranges.some(([s, e]) => pos >= s && pos < e)
const DARK_RE = /\.dark|oled/i

// ---------- 主流程 ----------

function main() {
  const tokenMap = lightTokenMap()
  const HEX_RE = /#[0-9a-fA-F]{3,8}\b/g
  const RGB_RE = /rgba?\(\s*\d+\s*,\s*\d+\s*,\s*\d+\s*(?:,\s*[\d.]+\s*)?\)/gi

  let totalReplaced = 0
  let totalSkippedDark = 0
  const perFile = []

  for (const file of walk(SRC)) {
    const rel = relative(SRC, file).replaceAll('\\', '/')
    if (rel === 'assets/css/base.css') continue
    const content = readFileSync(file, 'utf8')
    const styles = styleRanges(file, content)
    if (!styles.length) continue
    const comments = commentRanges(content)
    const blocks = blockRanges(content)

    // 收集替换点（从后往前应用，避免偏移）
    const edits = []
    let skippedDark = 0
    const applyMatches = (re, parser) => {
      for (const m of content.matchAll(re)) {
        const pos = m.index
        if (!inRanges(pos, styles)) continue
        if (inRanges(pos, comments)) continue
        const rgba = parser(m[0])
        if (!rgba) continue
        const token = tokenMap.get(colorKey(rgba))
        if (!token) continue
        // 最内层块的选择器上下文
        const inner = blocks.filter(b => pos >= b.start && pos < b.end).pop()
        if (inner && DARK_RE.test(inner.selector)) {
          skippedDark++
          continue
        }
        edits.push({ start: pos, end: pos + m[0].length, text: `var(${token})` })
      }
    }
    applyMatches(HEX_RE, parseHex)
    applyMatches(RGB_RE, parseRgbFunc)
    if (!edits.length) {
      if (skippedDark) perFile.push({ rel, replaced: 0, skippedDark })
      totalSkippedDark += skippedDark
      continue
    }

    let next = content
    for (const e of edits.sort((a, b) => b.start - a.start)) {
      next = next.slice(0, e.start) + e.text + next.slice(e.end)
    }
    if (WRITE) writeFileSync(file, next)
    totalReplaced += edits.length
    totalSkippedDark += skippedDark
    perFile.push({ rel, replaced: edits.length, skippedDark })
  }

  console.log(WRITE ? '=== 已写入 ===' : '=== DRY-RUN（加 --write 实际写入）===')
  for (const f of perFile) {
    console.log(`${f.rel}: 替换 ${f.replaced}` + (f.skippedDark ? `，跳过 dark 上下文 ${f.skippedDark}` : ''))
  }
  console.log(`合计：替换 ${totalReplaced}，跳过 dark 上下文 ${totalSkippedDark}`)
}

main()
