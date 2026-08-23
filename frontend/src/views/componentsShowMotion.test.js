import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const cssUrl = new URL('../assets/css/components-show.css', import.meta.url)

test('组件展示页沿用全局 viewport reveal 的过渡时序', async () => {
  const css = await readFile(cssUrl, 'utf8')
  const rule = css.match(/\.components-page__hero\.viewport-reveal,[^{]+\{([^}]+)\}/)?.[1] || ''

  assert.match(rule, /opacity \.84s cubic-bezier\(\.16,1,\.3,1\)/)
  assert.match(rule, /transform \.9s cubic-bezier\(\.16,1,\.3,1\)/)
  assert.match(
    rule,
    /transition-delay:var\(--viewport-reveal-delay\),var\(--viewport-reveal-delay\),0s,0s/
  )
})
