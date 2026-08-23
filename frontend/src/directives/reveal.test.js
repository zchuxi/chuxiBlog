import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

// 指令在模块顶层就创建 IntersectionObserver，Node 环境没有这个全局对象，
// 缺失时指令会走「直接显形」兜底分支，无法验证滚动入场的 pending 行为。
// 因此必须在 import 之前注入最小桩。
const observed = new Set()
globalThis.IntersectionObserver = class {
  constructor(callback) { this.callback = callback }
  observe(el) { observed.add(el) }
  unobserve(el) { observed.delete(el) }
}

const { default: reveal } = await import('./reveal.js')

const sourceUrl = new URL('./reveal.js', import.meta.url)

// 项目没有引入 jsdom，这里手搓最小 DOM 替身：只实现指令用到的
// classList / style.setProperty 语义，足以验证钩子的分支行为。
function createElement() {
  const classes = new Set()
  const vars = new Map()
  return {
    classList: {
      add: (...names) => names.forEach(name => classes.add(name)),
      remove: (...names) => names.forEach(name => classes.delete(name)),
      contains: name => classes.has(name)
    },
    style: {
      setProperty: (name, value) => vars.set(name, value)
    },
    delay: () => vars.get('--viewport-reveal-delay')
  }
}

test('取值兼容数字与对象两种形态', async () => {
  const source = await readFile(sourceUrl, 'utf8')
  assert.match(source, /function parseValue\(value\)/)
  assert.match(source, /typeof value === 'object'/)
  assert.match(source, /instant: !!value\.instant/)
})

test('数字取值保持原有滚动入场：先 pending 再等观察器', () => {
  const el = createElement()
  reveal.mounted(el, { value: 120 })
  assert.ok(el.classList.contains('viewport-reveal'))
  assert.ok(el.classList.contains('viewport-reveal-pending'))
  assert.equal(el.classList.contains('viewport-reveal-visible'), false)
  assert.equal(el.delay(), '120ms')
  assert.ok(observed.has(el), '数字取值应交给观察器等待进入视口')
})

test('instant 挂载时直接显形且不排入错峰延迟', () => {
  const el = createElement()
  reveal.mounted(el, { value: { delay: 260, instant: true } })
  assert.ok(el.classList.contains('viewport-reveal-visible'))
  assert.equal(el.classList.contains('viewport-reveal-pending'), false)
  assert.equal(el.delay(), '0ms')
  assert.equal(observed.has(el), false, 'instant 元素无需观察')
})

test('instant 更新时促活仍停在 pending 的元素', () => {
  // 曾出现筛选后列表变短、原本在视口外的卡片被推进视口，
  // 却仍停在 opacity:0 的 pending 态，看起来像一片空白卡位
  const el = createElement()
  reveal.mounted(el, { value: 190 })
  assert.ok(el.classList.contains('viewport-reveal-pending'))

  reveal.updated(el, { value: { delay: 190, instant: true } })
  assert.equal(el.classList.contains('viewport-reveal-pending'), false)
  assert.ok(el.classList.contains('viewport-reveal-visible'))
  assert.equal(el.delay(), '0ms')
})

test('退出 instant 后无条件回写错峰延迟', () => {
  // 这些元素是从 instant 态（delay=0）复用的 DOM，此时已是 visible；
  // 若只在 pending 时回写延迟，错峰会永久丢失
  const el = createElement()
  reveal.mounted(el, { value: { delay: 330, instant: true } })
  assert.equal(el.delay(), '0ms')

  reveal.updated(el, { value: 330 })
  assert.equal(el.delay(), '330ms')
})

test('卸载时解除观察，避免脱离文档的元素被持有', () => {
  const el = createElement()
  reveal.mounted(el, { value: 60 })
  assert.ok(observed.has(el))
  reveal.unmounted(el)
  assert.equal(observed.has(el), false)
})

test('无 IntersectionObserver 时直接显形，避免内容永久隐藏', async () => {
  const source = await readFile(sourceUrl, 'utf8')
  assert.match(source, /if \(instant \|\| !observer\)/)
  assert.match(source, /typeof IntersectionObserver !== 'undefined'/)
})
