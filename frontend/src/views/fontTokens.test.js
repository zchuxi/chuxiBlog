// 字号令牌体系守护：
// base.css 的 :root 定义 --text-xs/sm/base/md/lg/xl 六档（映射全站事实标准：
// 13px×36 / 14.5px×26 / 15.5px×16），cx 组件层已试点引用。
// 新代码应写 var(--text-md, 15.5px) 而非裸像素；fallback 保证令牌缺失时视觉不变。
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const read = relative => readFile(new URL(relative, import.meta.url), 'utf8')

test('base.css 定义六档字号令牌', async () => {
  const css = await read('../assets/css/base.css')
  for (const token of ['--text-xs', '--text-sm', '--text-base', '--text-md', '--text-lg', '--text-xl']) {
    assert.match(css, new RegExp(`${token}:\\s*[0-9.]+px`), `缺少令牌 ${token}`)
  }
})

test('cx 组件层引用字号令牌且带 fallback', async () => {
  const cases = [
    ['../assets/css/cx-button.css', '--text-md'],
    ['../assets/css/cx-input.css', '--text-md'],
    ['../assets/css/cx-switch.css', '--text-md'],
    ['../assets/css/cx-tag.css', '--text-base']
  ]
  for (const [file, token] of cases) {
    const css = await read(file)
    // 必须带 fallback（var(--token, Npx)），令牌缺失时静默退回像素值，视觉不崩
    assert.match(css, new RegExp(`var\\(${token},\\s*[0-9.]+px\\)`), `${file} 应引用 ${token} 且带 fallback`)
  }
})
