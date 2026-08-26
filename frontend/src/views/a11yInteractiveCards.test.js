// 前台可点击卡片与图标按钮的可访问性守护：
// 这些元素曾是 <article @click> / 无 aria 的图标按钮，
// 键盘与读屏用户完全无法触达。此文件锁住「键盘可达 + 焦点可见 + 读屏可辨识」三条底线。
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

// 从 source 中提取包含 className 的开标签（到 > 为止，属性可能换行）
function openingTag(source, className) {
  const classIndex = source.indexOf(`class="${className}"`)
  assert.ok(classIndex >= 0, `未找到 class="${className}"`)
  const tagStart = source.lastIndexOf('<', classIndex)
  const tagEnd = source.indexOf('>', classIndex)
  return source.slice(tagStart, tagEnd)
}

test('卡片式链接全部键盘可达：tabindex + role=link + Enter/Space 触发', async () => {
  const cases = [
    ['./ArchiveView.vue', 'archive-hero-feature-card', 'openArticle'],
    ['./HomeView.vue', 'article-gallery-card', 'openArticle'],
    ['./BangumiView.vue', 'bangumi-card', 'openDetail'],
    ['../views/BangumiDetailView.vue', 'bangumi-detail-more-card', 'router.push']
  ]
  for (const [file, className, handler] of cases) {
    const source = await read(file)
    const tag = openingTag(source, className)
    assert.match(tag, /tabindex="0"/, `${className} 必须 tabindex="0"`)
    assert.match(tag, /role="link"/, `${className} 必须 role="link"`)
    assert.match(tag, /aria-label/, `${className} 必须有 aria-label`)
    assert.match(tag, new RegExp(`@keydown\\.enter\\.prevent="${handler.replace('.', '\\.')}`), `${className} 必须 Enter 触发`)
    assert.match(tag, /@keydown\.space\.prevent=/, `${className} 必须 Space 触发`)
  }
})

test('可点击卡片的 hover 与 :focus-visible 焦点样式齐全', async () => {
  const archive = await read('../assets/css/archive.css')
  assert.match(archive, /\.archive-hero-feature-card\{[^}]*cursor:\s*pointer/)
  assert.match(archive, /\.archive-hero-feature-card:hover\{[^}]*translateY/)
  assert.match(archive, /\.archive-hero-feature-card:focus-visible\{[^}]*box-shadow/)

  const home = await read('../assets/css/home.css')
  assert.match(home, /\.article-gallery-card:focus-visible\{[^}]*var\(--accent-glow\)/)

  const bangumi = await read('./BangumiView.vue')
  assert.match(bangumi, /\.bangumi-card:focus-visible\s*\{[^}]*var\(--accent-glow\)/)

  const detail = await read('./BangumiDetailView.vue')
  assert.match(detail, /\.bangumi-detail-more-card:focus-visible\s*\{[^}]*var\(--accent-glow\)/)
})

test('搜索浮层主输入框焦点经 shell :focus-within 可见', async () => {
  const layout = await read('../assets/css/layout.css')
  // input 本体 outline:none 是允许的，但外壳必须接管焦点反馈
  assert.match(layout, /\.layout-article-search-input-shell:focus-within\{[^}]*box-shadow/)
})

test('音乐条控制按钮全部带 aria-label，且 :focus-visible 有焦点环', async () => {
  const bar = await read('../layout/components/MusicBar.vue')
  const buttons = bar.match(/<button[^>]*class="control-btn[^"]*"[^>]*>/g) || []
  assert.ok(buttons.length >= 9, `控制按钮数量异常：${buttons.length}`)
  for (const btn of buttons) {
    assert.match(btn, /aria-label="/, `按钮缺少 aria-label：${btn.slice(0, 80)}`)
  }
  // 两个 range 滑杆读屏也需要名称
  assert.match(bar, /class="music-bar-progress"[\s\S]*?aria-label="播放进度"/)
  assert.match(bar, /aria-label="音量"/)

  const layout = await read('../assets/css/layout.css')
  assert.match(layout, /\.control-btn:focus-visible\{[^}]*box-shadow/)
})

test('猫爪返回顶部是键盘可达的真按钮语义', async () => {
  const topBar = await read('../layout/components/TopBar.vue')
  const tag = openingTag(topBar, 'paw-rope__paw')
  assert.match(tag, /role="button"/)
  assert.match(tag, /tabindex="0"/)
  assert.match(tag, /aria-label="返回顶部"/)
  assert.match(tag, /@keydown\.enter\.prevent=/)

  const layout = await read('../assets/css/layout.css')
  assert.match(layout, /\.paw-rope__paw:focus-visible\{[^}]*var\(--accent-glow\)/)
})
