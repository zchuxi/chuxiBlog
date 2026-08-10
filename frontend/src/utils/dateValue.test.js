import assert from 'node:assert/strict'
import { test } from 'node:test'
import { buildMonthGrid, displayDateValue, formatDateValue, parseDateValue } from './dateValue.js'

test('parse：接受 date 与 datetime 两种契约格式', () => {
  const d = parseDateValue('2026-03-17')
  assert.equal(d.getFullYear(), 2026)
  assert.equal(d.getMonth(), 2)
  assert.equal(d.getDate(), 17)
  assert.equal(d.getHours(), 0)

  const dt = parseDateValue('2026-03-17T10:20:30')
  assert.equal(dt.getHours(), 10)
  assert.equal(dt.getMinutes(), 20)
  assert.equal(dt.getSeconds(), 30)
})

test('parse：容忍后端纳秒尾巴与空格分隔', () => {
  const a = parseDateValue('2026-03-17T10:20:30.123456789')
  assert.equal(a.getSeconds(), 30)
  const b = parseDateValue('2026-03-17 10:20:30')
  assert.equal(b.getHours(), 10)
})

test('parse：缺省秒按 0 处理', () => {
  assert.equal(parseDateValue('2026-03-17T10:20').getSeconds(), 0)
})

test('parse：非法输入与越界日期一律 null（Date 会静默滚月，必须拒掉）', () => {
  for (const bad of ['', null, undefined, 'abc', '2026-3-7', '2026-02-31', '2026-13-01']) {
    assert.equal(parseDateValue(bad), null, `应拒绝: ${String(bad)}`)
  }
})

test('format：按 withTime 出对应格式，且补零', () => {
  const dt = new Date(2026, 2, 7, 9, 5, 3)
  assert.equal(formatDateValue(dt, true), '2026-03-07T09:05:03')
  assert.equal(formatDateValue(dt, false), '2026-03-07')
})

test('往返：parse → format 不丢不移（UTC+8 下 00:00 尤其易退一天）', () => {
  for (const s of ['2026-01-01T00:00:00', '2026-03-17T10:20:30', '2026-12-31T23:59:59']) {
    assert.equal(formatDateValue(parseDateValue(s), true), s)
  }
  assert.equal(formatDateValue(parseDateValue('2026-01-01'), false), '2026-01-01')
})

test('display：以空格替代 T', () => {
  assert.equal(displayDateValue('2026-03-17T10:20:30', true), '2026-03-17 10:20:30')
  assert.equal(displayDateValue('2026-03-17', false), '2026-03-17')
  assert.equal(displayDateValue('', true), '')
})

test('日格：恒为 42 格，周一首列', () => {
  // 2026-03-01 是周日 → 周一首列时前面应补 6 格上月
  const grid = buildMonthGrid(new Date(2026, 2, 1), null, new Date(2026, 2, 17))
  assert.equal(grid.length, 42)
  assert.equal(grid.filter(c => !c.outside)[0].label, 1)
  assert.equal(grid.slice(0, 6).every(c => c.outside), true)
  assert.equal(grid[6].label, 1)
  assert.equal(grid[6].outside, false)
})

test('日格：today 与 active 各只标一格', () => {
  const grid = buildMonthGrid(new Date(2026, 2, 1), new Date(2026, 2, 17), new Date(2026, 2, 20))
  assert.equal(grid.filter(c => c.active).length, 1)
  assert.equal(grid.filter(c => c.active)[0].label, 17)
  assert.equal(grid.filter(c => c.today).length, 1)
  assert.equal(grid.filter(c => c.today)[0].label, 20)
})

test('日格：无选中值时没有 active 格', () => {
  const grid = buildMonthGrid(new Date(2026, 2, 1), null, new Date(2025, 0, 1))
  assert.equal(grid.filter(c => c.active).length, 0)
})
