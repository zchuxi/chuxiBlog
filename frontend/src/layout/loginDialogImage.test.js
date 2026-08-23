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
