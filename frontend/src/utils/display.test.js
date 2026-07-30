// display.js 纯逻辑行为检查：标签色板哈希、封面回退、日期截取
// 运行方式：在 frontend/ 目录执行 npm test（Node 内置测试器 node --test，零额外依赖）
import test from 'node:test'
import assert from 'node:assert/strict'
import { TAG_PALETTES, tagPaletteStyle, FALLBACK_COVERS, coverOf, mmdd, ymd } from './display.js'

test('tagPaletteStyle：同名标签结果稳定，且映射到色板中的某一项', () => {
  const a = tagPaletteStyle('随笔')
  const b = tagPaletteStyle('随笔')
  assert.deepEqual(a, b, '同一标签名必须得到相同样式')
  const matched = TAG_PALETTES.some(p =>
    a['--cx-tag-text'] === p.textColor &&
    a['--cx-tag-border'] === p.borderColor &&
    a['--cx-tag-background'] === p.backgroundColor
  )
  assert.ok(matched, '样式必须完整来自 TAG_PALETTES 的同一项')
})

test('tagPaletteStyle：空名与 null/undefined 落到首个色板，不抛错', () => {
  const expected = {
    '--cx-tag-text': TAG_PALETTES[0].textColor,
    '--cx-tag-border': TAG_PALETTES[0].borderColor,
    '--cx-tag-background': TAG_PALETTES[0].backgroundColor
  }
  assert.deepEqual(tagPaletteStyle(''), expected)
  assert.deepEqual(tagPaletteStyle(null), expected)
  assert.deepEqual(tagPaletteStyle(undefined), expected)
})

test('coverOf：有 coverUrl 时优先返回，无则按序号取回退封面并循环', () => {
  assert.equal(coverOf({ coverUrl: '/image/custom.webp' }, 3), '/image/custom.webp')
  assert.equal(coverOf(null, 0), FALLBACK_COVERS[0])
  assert.equal(coverOf({ coverUrl: '' }, 2), FALLBACK_COVERS[2], '空 coverUrl 视为无封面')
  assert.equal(
    coverOf({}, FALLBACK_COVERS.length + 1),
    FALLBACK_COVERS[1],
    '序号超出回退列表长度时按取模循环'
  )
})

test('mmdd：ISO 日期串截取为 MM/DD，空值返回空串', () => {
  assert.equal(mmdd('2024-05-06'), '05/06')
  assert.equal(mmdd('2024-05-06T10:20:30'), '05/06', '带时间部分不影响截取')
  assert.equal(mmdd(''), '')
  assert.equal(mmdd(null), '')
})

test('ymd：截取前 10 位年月日，空值返回空串', () => {
  assert.equal(ymd('2024-05-06T10:20:30'), '2024-05-06')
  assert.equal(ymd('2024-05-06'), '2024-05-06')
  assert.equal(ymd(''), '')
  assert.equal(ymd(undefined), '')
})
