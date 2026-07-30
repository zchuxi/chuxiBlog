// markdown.js 渲染行为检查：标题锚点生成/去重、代码高亮 class、图片 figure 包装
// 运行方式：在 frontend/ 目录执行 npm test（Node 内置测试器 node --test，零额外依赖）
// 说明：Node 环境无 DOM，dompurify 未挂载 sanitize，这里以透传桩替代；
//       sanitize 的净化行为属浏览器能力，不在本测试范围内。
import test from 'node:test'
import assert from 'node:assert/strict'
import DOMPurify from 'dompurify'

if (typeof DOMPurify.sanitize !== 'function') {
  DOMPurify.sanitize = html => html
}

const { renderMarkdown } = await import('./markdown.js')

test('空入参：返回空 html 与空 headings，不抛错', () => {
  assert.deepEqual(renderMarkdown(''), { html: '', headings: [] })
  assert.deepEqual(renderMarkdown(null), { html: '', headings: [] })
})

test('标题：生成 id 锚点并收集 headings，重名标题追加 -N 去重', () => {
  const { html, headings } = renderMarkdown('# Hello\n\n## Hello\n\n## World')
  assert.ok(html.includes('<h1 id="Hello">'), 'h1 必须带首个 id')
  assert.ok(html.includes('<h2 id="Hello-1">'), '重名 h2 必须去重为 Hello-1')
  assert.deepEqual(
    headings.map(h => ({ id: h.id, text: h.text, level: h.level })),
    [
      { id: 'Hello', text: 'Hello', level: 1 },
      { id: 'Hello-1', text: 'Hello', level: 2 },
      { id: 'World', text: 'World', level: 2 }
    ]
  )
})

test('标题：id 与目录文本剔除行内代码等标记符号', () => {
  const { headings } = renderMarkdown('## A `b`')
  assert.equal(headings.length, 1)
  assert.equal(headings[0].id, 'A-b')
  assert.equal(headings[0].text, 'A b')
})

test('代码块：已注册语言用其高亮 class，未知语言落 plaintext', () => {
  const known = renderMarkdown('```js\nconst x = 1\n```').html
  assert.ok(known.includes('<pre><code class="hljs language-js">'), 'js 为注册别名，必须保留语言 class')
  const unknown = renderMarkdown('```notalang\nplain text here\n```').html
  assert.ok(unknown.includes('class="hljs language-plaintext"'), '未注册语言必须落 plaintext')
})

test('图片：包装为 figure，携带 md-zoom、懒加载与可选 title', () => {
  const { html } = renderMarkdown('![封面](/img.png "标题")')
  assert.ok(html.includes('<figure><img class="md-zoom" src="/img.png" alt="封面" title="标题" loading="lazy">'))
  const noTitle = renderMarkdown('![](/img.png)').html
  assert.ok(noTitle.includes('<figure><img class="md-zoom" src="/img.png" alt="" loading="lazy">'), '无 title 时不输出 title 属性')
})
