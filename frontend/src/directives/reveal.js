// v-reveal：进入视口时 viewport-reveal-pending -> viewport-reveal-visible
//
// 取值两种形态：
//   v-reveal="120"                            错峰延迟毫秒数
//   v-reveal="{ delay: 120, instant: true }"  instant 为真时跳过入场动画直接显形
//
// instant 用于筛选/搜索这类用户主动触发的列表更新：结果应当立刻可见，
// 而不是先留白再逐个淡入（错峰延迟叠加 .84s 过渡最长可拖到 1.2s）。
const observer = typeof IntersectionObserver !== 'undefined'
  ? new IntersectionObserver(entries => {
      for (const entry of entries) {
        if (entry.isIntersecting) reveal(entry.target)
      }
    }, { threshold: 0.12 })
  : null

function parseValue(value) {
  if (value && typeof value === 'object') return { delay: value.delay, instant: !!value.instant }
  return { delay: value, instant: false }
}

function setDelay(el, delay) {
  if (delay != null) el.style.setProperty('--viewport-reveal-delay', `${delay}ms`)
}

function reveal(el) {
  el.classList.remove('viewport-reveal-pending')
  el.classList.add('viewport-reveal-visible')
  if (observer) observer.unobserve(el)
  scheduleCleanup(el)
}

// 入场动画跑完后挂上 done 类，由 CSS 撤掉 preserve-3d 与两层渐变伪元素。
// 否则每张卡片常驻两个 3D 合成层，列表页 hover / 筛选切换时会大面积重绘卡顿。
function scheduleCleanup(el) {
  const done = () => el.classList.add('viewport-reveal-done')
  if (typeof el.addEventListener === 'function') {
    el.addEventListener('transitionend', function onEnd(e) {
      // 扫光伪元素 ::after 的过渡最长（1.02s），以它结束为准；
      // 元素自身 transform 提前结束会截断扫光尾段
      if (e.target === el && e.pseudoElement === '::after' && e.propertyName === 'transform') {
        el.removeEventListener('transitionend', onEnd)
        done()
      }
    })
  }
  // transitionend 兜底：动画被打断（快速滚动、筛选切换）时也要收尾
  if (typeof setTimeout === 'function') setTimeout(done, 1800)
}

export default {
  mounted(el, binding) {
    const { delay, instant } = parseValue(binding.value)
    el.classList.add('viewport-reveal')
    setDelay(el, instant ? 0 : delay)
    // 无观察器（SSR/老浏览器）时同样直接显形，避免内容永久隐藏
    if (instant || !observer) {
      // 无入场动画可等，直接收尾
      el.classList.add('viewport-reveal-visible', 'viewport-reveal-done')
      return
    }
    el.classList.add('viewport-reveal-pending')
    observer.observe(el)
  },
  updated(el, binding) {
    const { delay, instant } = parseValue(binding.value)
    // 筛选态下促活仍停在 pending 的元素：列表变短后它们会被推进视口，
    // 若继续等观察器回调 + 错峰延迟，用户会看到一片空白卡位
    if (instant) {
      setDelay(el, 0)
      if (el.classList.contains('viewport-reveal-pending')) reveal(el)
      return
    }
    // 退出筛选态后需无条件回写错峰延迟：这些元素是从 instant 态（delay=0）
    // 复用的 DOM，此时多为 visible，若只在 pending 时回写，错峰会永久丢失
    setDelay(el, delay)
  },
  unmounted(el) {
    if (observer) observer.unobserve(el)
  }
}
