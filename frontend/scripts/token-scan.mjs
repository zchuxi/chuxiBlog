#!/usr/bin/env node
/**
 * token-scan.mjs
 * ==============
 * P1-0 令牌映射分析工具：
 * 1. 解析 base.css 的三套令牌表（:root 亮色 / html.dark 暗色 / html.oled）
 * 2. 扫描 src 下所有 .css/.vue 的 hex 与 rgb/rgba 直写颜色
 * 3. 归一化后与令牌值精确比对，输出映射报告（Markdown）
 *
 * 用法：
 *   node scripts/token-scan.mjs            # 输出报告到 stdout
 *   node scripts/token-scan.mjs --json     # 输出机器可读 JSON
 */

import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const SRC = join(fileURLToPath(new URL('.', import.meta.url)), '..', 'src')
const BASE_CSS = join(SRC, 'assets', 'css', 'base.css')

// ---------- 颜色归一化 ----------

/** hex -> [r,g,b,a(0-1)]，支持 #rgb #rgba #rrggbb #rrggbbaa */
function parseHex(h) {
  let s = h.slice(1).toLowerCase()
  if (s.length === 3 || s.length === 4) s = [...s].map(c => c + c).join('')
  if (s.length === 6) s += 'ff'
  if (s.length !== 8) return null
  const n = parseInt(s, 16)
  return [(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, (n & 255) / 255]
}

/** rgb/rgba() -> [r,g,b,a]，归一化空格与 .5 / 0.5 差异 */
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

/** 归一化颜色键：r,g,b,a（a 保留 4 位小数） */
function colorKey([r, g, b, a]) {
  return `${r},${g},${b},${Math.round(a * 10000) / 10000}`
}

// ---------- 1. 解析 base.css 令牌 ----------

function parseTokens() {
  const css = readFileSync(BASE_CSS, 'utf8')
  // 按作用域块切分：:root{...} html.dark{...} html.oled{...}
  const tokens = [] // {name, scope, value, key}
  const blockRe = /(:root|html\.dark|html\.oled)\s*\{([^}]*)\}/g
  let bm
  while ((bm = blockRe.exec(css))) {
    const scope = bm[1]
    const body = bm[2]
    const declRe = /(--[a-zA-Z0-9-]+)\s*:\s*([^;]+);/g
    let dm
    while ((dm = declRe.exec(body))) {
      const name = dm[1]
      const value = dm[2].trim()
      let rgba = null
      if (value.startsWith('#')) rgba = parseHex(value)
      else if (/^rgba?\(/i.test(value)) rgba = parseRgbFunc(value)
      tokens.push({ name, scope, value, key: rgba ? colorKey(rgba) : null })
    }
  }
  return tokens
}

// ---------- 2. 扫描源码颜色直写 ----------

function* walk(dir) {
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry)
    if (statSync(full).isDirectory()) yield* walk(full)
    else if (/\.(css|vue)$/.test(entry)) yield full
  }
}

const HEX_RE = /#[0-9a-fA-F]{3,8}\b/g
const RGB_RE = /rgba?\(\s*\d+\s*,\s*\d+\s*,\s*\d+\s*(?:,\s*[\d.]+\s*)?\)/gi

function scanColors() {
  // Map<colorKey, {raw:Set, files:Map<file, count>, total}>
  const found = new Map()
  for (const file of walk(SRC)) {
    const rel = relative(SRC, file).replaceAll('\\', '/')
    const content = readFileSync(file, 'utf8')
    const hits = []
    for (const m of content.matchAll(HEX_RE)) hits.push(parseHex(m[0]) && { rgba: parseHex(m[0]), raw: m[0] })
    for (const m of content.matchAll(RGB_RE)) hits.push(parseRgbFunc(m[0]) && { rgba: parseRgbFunc(m[0]), raw: m[0] })
    for (const h of hits.filter(Boolean)) {
      const key = colorKey(h.rgba)
      if (!found.has(key)) found.set(key, { raw: new Set(), files: new Map(), total: 0, rgba: h.rgba })
      const rec = found.get(key)
      rec.raw.add(h.raw)
      rec.files.set(rel, (rec.files.get(rel) || 0) + 1)
      rec.total++
    }
  }
  return found
}

// ---------- 3. 汇总报告 ----------

function main() {
  const tokens = parseTokens()
  const found = scanColors()

  // 令牌值 -> 令牌（亮/暗分开索引）；令牌名 -> 是否有暗色覆写
  const lightByKey = new Map()
  const darkByKey = new Map()
  const darkTokenNames = new Set(tokens.filter(t => t.scope !== ':root').map(t => t.name))
  for (const t of tokens.filter(t => t.key)) {
    if (t.scope === ':root') {
      if (!lightByKey.has(t.key)) lightByKey.set(t.key, [])
      lightByKey.get(t.key).push(t)
    } else {
      if (!darkByKey.has(t.key)) darkByKey.set(t.key, [])
      darkByKey.get(t.key).push(t)
    }
  }

  const rows = []
  let exactMapped = 0
  let unmapped = 0
  for (const [key, rec] of [...found.entries()].sort((a, b) => b[1].total - a[1].total)) {
    const light = lightByKey.get(key)
    const dark = darkByKey.get(key)
    const inBase = rec.files.has('assets/css/base.css')
    let mapping
    if (light) {
      // 按令牌名检查暗色覆写：有覆写则替换后暗色主题自动跟随，无覆写则两主题同值
      mapping = `var(${light[0].name})` + (darkTokenNames.has(light[0].name) ? '' : '（⚠ 无暗色覆写，两主题同值）')
      exactMapped += rec.total - (rec.files.get('assets/css/base.css') || 0)
    } else if (dark) {
      mapping = `暗色令牌 ${dark[0].name} 的值（出现在非 dark 上下文需人工确认）`
    } else {
      mapping = '**特例**（无精确令牌，待聚类或保留）'
      if (!inBase) unmapped += rec.total
    }
    rows.push({ key, rgba: rec.rgba, total: rec.total, raw: [...rec.raw], mapping, files: rec.files, inBase })
  }

  if (process.argv.includes('--json')) {
    console.log(JSON.stringify(rows.map(r => ({ ...r, files: Object.fromEntries(r.files) })), null, 2))
    return
  }

  const out = []
  out.push('# 令牌映射表（P1-0）', '')
  out.push('> 由 `frontend/scripts/token-scan.mjs` 生成。归一化后与 base.css 令牌精确比对。', '')
  out.push(`- base.css 令牌数：${tokens.length}（:root ${tokens.filter(t => t.scope === ':root').length} / html.dark ${tokens.filter(t => t.scope === 'html.dark').length} / html.oled ${tokens.filter(t => t.scope === 'html.oled').length}）`)
  const totalOutside = rows.reduce((s, r) => s + r.total - (r.files.get('assets/css/base.css') || 0), 0)
  out.push(`- base.css 之外直写颜色总数：${totalOutside}`)
  out.push(`- 其中可精确映射到亮色令牌：${exactMapped}`)
  out.push(`- 无精确令牌（特例/待聚类）：${unmapped}`, '')
  out.push('| 颜色 (r,g,b,a) | 原始写法 | 出现次数 | 映射 | 主要文件 |')
  out.push('| --- | --- | --- | --- | --- |')
  for (const r of rows) {
    if (r.inBase && r.files.size === 1) continue // 只在 base.css 出现的是令牌定义本身
    const topFiles = [...r.files.entries()]
      .filter(([f]) => f !== 'assets/css/base.css')
      .sort((a, b) => b[1] - a[1]).slice(0, 3)
      .map(([f, c]) => `${f} (${c})`).join('<br>')
    out.push(`| ${r.key} | ${r.raw.slice(0, 3).join(' ')} | ${r.total} | ${r.mapping} | ${topFiles} |`)
  }
  console.log(out.join('\n'))
}

main()
