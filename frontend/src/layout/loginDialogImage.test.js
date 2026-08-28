import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

const layoutUrl = new URL('./LayoutView.vue', import.meta.url)

test('登录弹窗左侧使用当前竖屏背景图展示', async () => {
  const source = await readFile(layoutUrl, 'utf8')
  const sideStart = source.indexOf('<aside class="login-dialog__side"')
  const sideEnd = source.indexOf('<!-- 右侧表单区 -->')
  const side = source.slice(sideStart, sideEnd)

  assert.ok(sideStart >= 0 && sideEnd > sideStart)
  assert.match(side, /<img[\s\S]*?:src="settings\.selectedVerticalImage"/)
  assert.match(side, /class="login-dialog__side-image"/)
  assert.match(side, /alt=""/)
  assert.equal(side.includes('login-dialog__side-body'), false)
  assert.equal(side.includes('login-dialog__orb'), false)
})

test('登录弹窗图片支持循环切换并持久化选择', async () => {
  const source = await readFile(layoutUrl, 'utf8')
  const sideStart = source.indexOf('<aside class="login-dialog__side"')
  const sideEnd = source.indexOf('<!-- 右侧表单区 -->')
  const side = source.slice(sideStart, sideEnd)

  assert.match(side, /class="login-dialog__change-image"/)
  assert.match(side, /aria-label="更换登录侧栏图片"/)
  assert.match(side, /:disabled="settings\.verticalImages\.length < 2"/)
  assert.match(side, /@click="changeAuthSideImage"/)
  assert.match(side, /<SvgIcon name="common-exchange"/)
  assert.match(source, /function changeAuthSideImage\(\)[\s\S]*?settings\.verticalImages[\s\S]*?settings\.update\(\{ selectedVerticalImage: nextImage \}\)/)
})

test('auth 对话框移动版规则不得使用 !important（特异性治理已清零）', async () => {
  // 历史上移动版用 8 处 !important 对抗桌面版滑动动画规则。
  // 重构后：7 处靠「同特异性 + 源码顺序」胜出（媒体查询块在文件尾部），
  // is-forgot 冲突场景用 .auth-panel.auth-panel 双写类提到 (0,5,0)，均不再需要 !important。
  // auth 规则在 layout.css 拆分后归入 layout/auth.css（P3-2）
  const css = await readFile(new URL('../assets/css/layout/auth.css', import.meta.url), 'utf8')
  const authRules = css.match(/\.auth[a-zA-Z0-9_.:\-\s]*\{[^}]*\}/g) || []
  const offenders = authRules.filter(rule => rule.includes('!important'))
  assert.deepEqual(offenders, [], `auth 规则不得回潮 !important：${offenders.join(' | ')}`)
  // is-forgot 的 display:none 必须用双写类保住（对抗 is-register 的 display:flex，不能依赖规则顺序）
  assert.match(css, /\.auth-dialog__shell\.is-forgot \.auth-panel\.auth-panel\{display:none\}/)
})
