import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import {
  LIVE2D_POSITION_STORAGE_KEY,
  bindLive2dWidgetDrag,
  clampLive2dPosition,
  readLive2dPosition
} from './live2dWidgetDrag.js'

const layoutViewUrl = new URL('../layout/LayoutView.vue', import.meta.url)

function createEventTarget() {
  const listeners = new Map()
  return {
    listeners,
    addEventListener(type, handler) {
      const handlers = listeners.get(type) || new Set()
      handlers.add(handler)
      listeners.set(type, handlers)
    },
    removeEventListener(type, handler) {
      listeners.get(type)?.delete(handler)
    },
    dispatch(type, event = {}) {
      for (const handler of listeners.get(type) || []) handler(event)
    }
  }
}

function createStyle() {
  const values = new Map()
  return {
    values,
    setProperty(name, value) { values.set(name, value) }
  }
}

function createFixture() {
  const widgetTarget = createEventTarget()
  const handleTarget = createEventTarget()
  const windowTarget = createEventTarget()
  const style = createStyle()
  const classes = new Set()
  const captured = []
  const released = []
  const writes = []
  const storage = {
    getItem() { return null },
    setItem(key, value) { writes.push([key, value]) }
  }
  const translatedRect = rect => {
    const x = Number.parseFloat(style.values.get('--live2d-translate-x')) || 0
    const y = Number.parseFloat(style.values.get('--live2d-translate-y')) || 0
    return {
      ...rect,
      left: rect.left + x,
      right: rect.right + x,
      top: rect.top + y,
      bottom: rect.bottom + y
    }
  }
  const actions = {
    getBoundingClientRect() {
      return translatedRect({ left: 650, top: 500, right: 690, bottom: 680, width: 40, height: 180 })
    }
  }
  const widget = {
    ...widgetTarget,
    style,
    classList: {
      add(name) { classes.add(name) },
      remove(name) { classes.delete(name) }
    },
    getBoundingClientRect() {
      return translatedRect({ left: 700, top: 400, right: 980, bottom: 680, width: 280, height: 280 })
    },
    querySelector(selector) {
      return selector === '.live2d-widget__actions' ? actions : null
    }
  }
  const handle = {
    ...handleTarget,
    setPointerCapture(pointerId) { captured.push(pointerId) },
    releasePointerCapture(pointerId) { released.push(pointerId) },
    hasPointerCapture(pointerId) { return captured.includes(pointerId) && !released.includes(pointerId) }
  }
  const windowObject = {
    ...windowTarget,
    innerWidth: 1000,
    innerHeight: 700
  }
  return { widget, handle, windowObject, storage, writes, classes, captured, released, actions }
}

test('readLive2dPosition：存储缺失或损坏时回退到原点', () => {
  assert.deepEqual(readLive2dPosition(null), { x: 0, y: 0 })
  assert.deepEqual(readLive2dPosition('{bad json'), { x: 0, y: 0 })
  assert.deepEqual(readLive2dPosition('{"x":"12","y":3}'), { x: 0, y: 0 })
  assert.deepEqual(readLive2dPosition('{"x":12,"y":-3}'), { x: 12, y: -3 })
})

test('clampLive2dPosition：将组件完整限制在视口安全间距内', () => {
  const rect = { left: 700, top: 400, right: 980, bottom: 680 }
  const viewport = { width: 1000, height: 700 }
  assert.deepEqual(clampLive2dPosition({ x: 100, y: 100 }, rect, viewport, 8), { x: 12, y: 12 })
  assert.deepEqual(clampLive2dPosition({ x: -900, y: -900 }, rect, viewport, 8), { x: -692, y: -392 })
})

test('Live2D 画布与拖动层共用稳定光标，避免 Pixi 内联样式造成闪烁', async () => {
  const layoutView = await readFile(layoutViewUrl, 'utf8')

  assert.match(
    layoutView,
    /\.live2d-widget__stage,\s*\.live2d-widget__stage canvas\s*\{[^}]*cursor:\s*grab\s*!important/s
  )
  assert.match(
    layoutView,
    /\.live2d-widget\.is-dragging \.live2d-widget__stage,\s*\.live2d-widget\.is-dragging \.live2d-widget__stage canvas\s*\{[^}]*cursor:\s*grabbing\s*!important/s
  )
})

test('Live2D canvas 只负责渲染，stage 是唯一的 DOM 指针命中面', async () => {
  const layoutView = await readFile(layoutViewUrl, 'utf8')

  assert.match(
    layoutView,
    /\.live2d-widget__stage canvas\s*\{[^}]*pointer-events:\s*none\s*!important/s
  )
})

test('bindLive2dWidgetDrag：初始化时把包含操作按钮的视觉范围约束在视口内', () => {
  const fixture = createFixture()
  fixture.storage.getItem = () => '{"x":-900,"y":0}'

  bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage
  })

  assert.equal(fixture.widget.style.values.get('--live2d-translate-x'), '-642px')
  assert.equal(fixture.widget.style.values.get('--live2d-translate-y'), '0px')
})

test('bindLive2dWidgetDrag：刷新后保持合法的非零位置且只修正越界位置', () => {
  const validFixture = createFixture()
  validFixture.storage.getItem = () => '{"x":-120,"y":-40}'
  bindLive2dWidgetDrag({
    widget: validFixture.widget,
    handle: validFixture.handle,
    windowObject: validFixture.windowObject,
    storage: validFixture.storage
  })
  assert.equal(validFixture.widget.style.values.get('--live2d-translate-x'), '-120px')
  assert.equal(validFixture.widget.style.values.get('--live2d-translate-y'), '-40px')

  const overflowFixture = createFixture()
  overflowFixture.storage.getItem = () => '{"x":-900,"y":100}'
  bindLive2dWidgetDrag({
    widget: overflowFixture.widget,
    handle: overflowFixture.handle,
    windowObject: overflowFixture.windowObject,
    storage: overflowFixture.storage
  })
  assert.equal(overflowFixture.widget.style.values.get('--live2d-translate-x'), '-642px')
  assert.equal(overflowFixture.widget.style.values.get('--live2d-translate-y'), '12px')
})

test('bindLive2dWidgetDrag：只从模型手柄开始拖动，移动根组件并在结束后保存位置', () => {
  const fixture = createFixture()
  const cleanup = bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage
  })

  assert.equal(fixture.widget.listeners.has('pointerdown'), false, '操作按钮所在根元素不应成为拖动手柄')

  fixture.handle.dispatch('pointerdown', {
    button: 0,
    isPrimary: true,
    pointerId: 7,
    clientX: 900,
    clientY: 500,
    preventDefault() {}
  })
  fixture.handle.dispatch('pointermove', {
    pointerId: 7,
    clientX: 840,
    clientY: 450,
    preventDefault() {}
  })

  assert.equal(fixture.widget.style.values.get('--live2d-translate-x'), '-60px')
  assert.equal(fixture.widget.style.values.get('--live2d-translate-y'), '-50px')
  assert.equal(fixture.classes.has('is-dragging'), true)
  assert.deepEqual(fixture.captured, [7])

  fixture.handle.dispatch('pointerup', { pointerId: 7 })

  assert.equal(fixture.classes.has('is-dragging'), false)
  assert.deepEqual(fixture.released, [7])
  assert.deepEqual(fixture.writes, [[LIVE2D_POSITION_STORAGE_KEY, '{"x":-60,"y":-50}']])

  cleanup()
  assert.equal(fixture.handle.listeners.get('pointerdown')?.size || 0, 0)
  assert.equal(fixture.windowObject.listeners.get('resize')?.size || 0, 0)
})

test('bindLive2dWidgetDrag：忽略非主指针与非主键', () => {
  const fixture = createFixture()
  bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage
  })

  fixture.handle.dispatch('pointerdown', {
    button: 2,
    isPrimary: true,
    pointerId: 1,
    clientX: 0,
    clientY: 0
  })
  fixture.handle.dispatch('pointerdown', {
    button: 0,
    isPrimary: false,
    pointerId: 2,
    clientX: 0,
    clientY: 0
  })

  assert.deepEqual(fixture.captured, [])
  assert.equal(fixture.classes.has('is-dragging'), false)
})

test('bindLive2dWidgetDrag：窗口缩小时重新约束并保存位置', () => {
  const fixture = createFixture()
  bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage
  })

  fixture.windowObject.innerWidth = 900
  fixture.windowObject.innerHeight = 620
  fixture.windowObject.dispatch('resize')

  assert.equal(fixture.widget.style.values.get('--live2d-translate-x'), '-88px')
  assert.equal(fixture.widget.style.values.get('--live2d-translate-y'), '-68px')
  assert.deepEqual(fixture.writes, [[LIVE2D_POSITION_STORAGE_KEY, '{"x":-88,"y":-68}']])
})

test('bindLive2dWidgetDrag：组件在移动端隐藏时保留桌面端位置', () => {
  const fixture = createFixture()
  fixture.storage.getItem = () => '{"x":-120,"y":-40}'
  fixture.widget.getBoundingClientRect = () => ({ left: 0, top: 0, right: 0, bottom: 0, width: 0, height: 0 })
  fixture.actions.getBoundingClientRect = fixture.widget.getBoundingClientRect

  bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage
  })
  fixture.windowObject.dispatch('resize')

  assert.equal(fixture.widget.style.values.get('--live2d-translate-x'), '-120px')
  assert.equal(fixture.widget.style.values.get('--live2d-translate-y'), '-40px')
  assert.deepEqual(fixture.writes, [])
})

test('bindLive2dWidgetDrag：localStorage getter 抛错时仍可绑定', () => {
  const fixture = createFixture()
  Object.defineProperty(fixture.windowObject, 'localStorage', {
    get() { throw new Error('SecurityError') }
  })

  assert.doesNotThrow(() => bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject
  }))
})

test('bindLive2dWidgetDrag：Pointer Capture 失败时回滚拖动态', () => {
  const fixture = createFixture()
  fixture.handle.setPointerCapture = () => { throw new Error('NotFoundError') }

  bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage
  })
  fixture.handle.dispatch('pointerdown', {
    button: 0,
    isPrimary: true,
    pointerId: 9,
    clientX: 900,
    clientY: 500,
    preventDefault() {}
  })

  assert.equal(fixture.classes.has('is-dragging'), false)
  fixture.handle.dispatch('pointermove', { pointerId: 9, clientX: 800, clientY: 400 })
  assert.equal(fixture.widget.style.values.get('--live2d-translate-x'), '0px')
  assert.equal(fixture.widget.style.values.get('--live2d-translate-y'), '0px')
})

test('bindLive2dWidgetDrag：轻点手柄触发点击回调，实际拖动不触发', () => {
  const fixture = createFixture()
  const taps = []
  bindLive2dWidgetDrag({
    widget: fixture.widget,
    handle: fixture.handle,
    windowObject: fixture.windowObject,
    storage: fixture.storage,
    onTap: () => taps.push('tap')
  })

  fixture.handle.dispatch('pointerdown', {
    button: 0,
    isPrimary: true,
    pointerId: 3,
    clientX: 900,
    clientY: 500
  })
  fixture.handle.dispatch('pointerup', { pointerId: 3 })
  assert.deepEqual(taps, ['tap'])

  fixture.handle.dispatch('pointerdown', {
    button: 0,
    isPrimary: true,
    pointerId: 4,
    clientX: 900,
    clientY: 500
  })
  fixture.handle.dispatch('pointermove', { pointerId: 4, clientX: 820, clientY: 500 })
  fixture.handle.dispatch('pointerup', { pointerId: 4 })
  assert.deepEqual(taps, ['tap'], '发生位移后释放指针不应再触发点击')
})
