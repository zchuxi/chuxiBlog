// 前台可点击卡片的按压反馈守护：
// hover 上浮的卡片按下时回压到上浮量的一半（浮起→回落的物理手感）。
// 规则必须与 hover 同文件定义（页面 CSS 按路由后加载，集中在 base.css
// 会被同特异性的 hover 规则覆盖而静默失效）。
// timeline 3D 卡片（rotateY/translateZ 复合变换）明确豁免，不纳入。
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

const CASES = [
  ['../assets/css/home.css', 'article-gallery-card', '-6px', '-3px'],
  ['../assets/css/archive.css', 'archive-entry-card', '-3px', '-1px'],
  ['../assets/css/archive.css', 'archive-hero-feature-card', '-3px', '-1px'],
  ['../assets/css/tool.css', 'tool-spotlight-card', '-4px', '-2px'],
  ['../assets/css/tool.css', 'tool-site-card', '-4px', '-2px'],
  ['../assets/css/tree-hole.css', 'tree-hole-healing-card', '-4px', '-2px'],
  ['./BangumiView.vue', 'bangumi-card', '-6px', '-3px'],
  ['./BangumiDetailView.vue', 'bangumi-detail-more-card', '-4px', '-2px']
]

test('hover 上浮卡片全部具备按压回压态，且回压到上浮量一半', async () => {
  for (const [file, cls, hoverY, activeY] of CASES) {
    const source = await read(file)
    const escape = cls.replace(/-/g, '\\-')
    assert.match(
      source,
      new RegExp(`\\.${escape}:hover\\s*\\{[^}]*translateY\\(${hoverY}\\)`),
      `${file} .${cls} hover 上浮 ${hoverY} 缺失`
    )
    assert.match(
      source,
      new RegExp(`\\.${escape}:active\\s*\\{[^}]*translateY\\(${activeY}\\)`),
      `${file} .${cls} active 回压 ${activeY} 缺失`
    )
  }
})
