export const LIVE2D_POSITION_STORAGE_KEY = 'chuxi-live2d-position'

const DEFAULT_POSITION = Object.freeze({ x: 0, y: 0 })
const DEFAULT_SAFE_AREA = 8
const TAP_SLOP_PX = 6

function isFinitePosition(position) {
  return position
    && Number.isFinite(position.x)
    && Number.isFinite(position.y)
}

export function readLive2dPosition(value) {
  if (!value) return { ...DEFAULT_POSITION }
  try {
    const position = JSON.parse(value)
    return isFinitePosition(position) ? { x: position.x, y: position.y } : { ...DEFAULT_POSITION }
  } catch {
    return { ...DEFAULT_POSITION }
  }
}

function clamp(value, min, max) {
  if (min > max) return (min + max) / 2
  return Math.min(max, Math.max(min, value))
}

export function clampLive2dPosition(position, rect, viewport, safeArea = DEFAULT_SAFE_AREA) {
  const nextPosition = isFinitePosition(position) ? position : DEFAULT_POSITION
  const viewportWidth = Number.isFinite(viewport?.width) ? viewport.width : 0
  const viewportHeight = Number.isFinite(viewport?.height) ? viewport.height : 0
  const gap = Number.isFinite(safeArea) ? Math.max(0, safeArea) : DEFAULT_SAFE_AREA

  return {
    x: clamp(nextPosition.x, gap - rect.left, viewportWidth - gap - rect.right),
    y: clamp(nextPosition.y, gap - rect.top, viewportHeight - gap - rect.bottom)
  }
}

function unionRects(rects) {
  return rects.reduce((result, rect) => ({
    left: Math.min(result.left, rect.left),
    top: Math.min(result.top, rect.top),
    right: Math.max(result.right, rect.right),
    bottom: Math.max(result.bottom, rect.bottom)
  }))
}

function hasVisualArea(rect) {
  return rect && rect.right > rect.left && rect.bottom > rect.top
}

function getWidgetVisualRect(widget) {
  const rects = [widget.getBoundingClientRect()]
  const actions = widget.querySelector?.('.live2d-widget__actions')
  if (actions?.getBoundingClientRect) rects.push(actions.getBoundingClientRect())
  return unionRects(rects)
}

function getViewport(windowObject) {
  return {
    width: windowObject?.innerWidth || 0,
    height: windowObject?.innerHeight || 0
  }
}

function applyPosition(widget, position) {
  widget.style.setProperty('--live2d-translate-x', `${position.x}px`)
  widget.style.setProperty('--live2d-translate-y', `${position.y}px`)
}

function loadPosition(storage) {
  try {
    return readLive2dPosition(storage?.getItem(LIVE2D_POSITION_STORAGE_KEY))
  } catch {
    return { ...DEFAULT_POSITION }
  }
}

function savePosition(storage, position) {
  try {
    storage?.setItem(LIVE2D_POSITION_STORAGE_KEY, JSON.stringify(position))
  } catch {
    // 隐私模式或存储空间不可用时仅保留当前会话内的位置。
  }
}

export function bindLive2dWidgetDrag({
  widget,
  handle,
  windowObject = globalThis.window,
  storage,
  safeArea = DEFAULT_SAFE_AREA,
  onTap
} = {}) {
  if (!widget || !handle || !windowObject) return () => {}

  let resolvedStorage = storage
  if (resolvedStorage === undefined) {
    try {
      resolvedStorage = windowObject.localStorage
    } catch {
      resolvedStorage = null
    }
  }

  let position = { ...DEFAULT_POSITION }
  let activePointerId = null
  let startPointer = { x: 0, y: 0 }
  let startPosition = { ...DEFAULT_POSITION }
  let startRect = null
  let moved = false

  position = loadPosition(resolvedStorage)
  applyPosition(widget, position)
  const initialRect = getWidgetVisualRect(widget)
  if (hasVisualArea(initialRect)) {
    const correction = clampLive2dPosition(
      DEFAULT_POSITION,
      initialRect,
      getViewport(windowObject),
      safeArea
    )
    if (correction.x !== 0 || correction.y !== 0) {
      position = {
        x: position.x + correction.x,
        y: position.y + correction.y
      }
      applyPosition(widget, position)
    }
  }

  function finishDrag(pointerId, persist = true) {
    if (pointerId !== activePointerId) return
    widget.classList.remove('is-dragging')
    if (handle.hasPointerCapture?.(pointerId)) handle.releasePointerCapture(pointerId)
    activePointerId = null
    startRect = null
    if (persist) savePosition(resolvedStorage, position)
  }

  function onPointerDown(event) {
    if (activePointerId !== null || event.button !== 0 || event.isPrimary === false) return
    activePointerId = event.pointerId
    startPointer = { x: event.clientX, y: event.clientY }
    startPosition = { ...position }
    startRect = getWidgetVisualRect(widget)
    moved = false
    if (!hasVisualArea(startRect)) {
      activePointerId = null
      startRect = null
      return
    }
    widget.classList.add('is-dragging')
    try {
      handle.setPointerCapture?.(event.pointerId)
    } catch {
      widget.classList.remove('is-dragging')
      activePointerId = null
      startRect = null
      return
    }
    event.preventDefault?.()
  }

  function onPointerMove(event) {
    if (event.pointerId !== activePointerId || !startRect) return
    const deltaX = event.clientX - startPointer.x
    const deltaY = event.clientY - startPointer.y
    if (Math.hypot(deltaX, deltaY) > TAP_SLOP_PX) moved = true
    const movement = clampLive2dPosition(
      {
        x: deltaX,
        y: deltaY
      },
      startRect,
      getViewport(windowObject),
      safeArea
    )
    position = {
      x: startPosition.x + movement.x,
      y: startPosition.y + movement.y
    }
    applyPosition(widget, position)
    event.preventDefault?.()
  }

  function onPointerUp(event) {
    if (event.pointerId !== activePointerId) return
    const shouldTap = !moved
    finishDrag(event.pointerId)
    if (shouldTap) onTap?.(event)
  }

  function onPointerCancel(event) {
    finishDrag(event.pointerId)
  }

  function onLostPointerCapture(event) {
    finishDrag(event.pointerId)
  }

  function onResize() {
    const visualRect = getWidgetVisualRect(widget)
    if (!hasVisualArea(visualRect)) return
    const correction = clampLive2dPosition(
      DEFAULT_POSITION,
      visualRect,
      getViewport(windowObject),
      safeArea
    )
    if (correction.x === 0 && correction.y === 0) return
    position = {
      x: position.x + correction.x,
      y: position.y + correction.y
    }
    applyPosition(widget, position)
    savePosition(resolvedStorage, position)
  }

  handle.addEventListener('pointerdown', onPointerDown)
  handle.addEventListener('pointermove', onPointerMove)
  handle.addEventListener('pointerup', onPointerUp)
  handle.addEventListener('pointercancel', onPointerCancel)
  handle.addEventListener('lostpointercapture', onLostPointerCapture)
  windowObject.addEventListener('resize', onResize)

  return () => {
    if (activePointerId !== null) finishDrag(activePointerId, false)
    handle.removeEventListener('pointerdown', onPointerDown)
    handle.removeEventListener('pointermove', onPointerMove)
    handle.removeEventListener('pointerup', onPointerUp)
    handle.removeEventListener('pointercancel', onPointerCancel)
    handle.removeEventListener('lostpointercapture', onLostPointerCapture)
    windowObject.removeEventListener('resize', onResize)
    widget.classList.remove('is-dragging')
  }
}
