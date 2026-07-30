// settings store 纯逻辑行为检查：默认图库常量、localStorage 读写、update 补丁、远端图库回退
// 运行方式：在 frontend/ 目录执行 npm test（Node 内置测试器 node --test，零额外依赖）
// 说明：Node 环境无浏览器全局，这里以最小桩替代 localStorage / document，
//       DOM class 切换与真实网络请求不在本测试范围内。
import test from 'node:test'
import assert from 'node:assert/strict'
import { createPinia, setActivePinia } from 'pinia'
import { api } from '../api/index.js'
import { DEFAULT_LANDSCAPE, DEFAULT_VERTICAL, useSettingsStore } from './settings.js'

// 与 settings.js 内部持久化 KEY 保持一致
const KEY = 'chuxi-nest-settings'

const memoryStorage = new Map()
globalThis.localStorage = {
  getItem: k => (memoryStorage.has(k) ? memoryStorage.get(k) : null),
  setItem: (k, v) => memoryStorage.set(k, String(v)),
  removeItem: k => memoryStorage.delete(k),
  clear: () => memoryStorage.clear()
}
globalThis.document = { documentElement: { classList: { toggle() {} } } }

function freshStore(saved) {
  memoryStorage.clear()
  if (saved) memoryStorage.set(KEY, JSON.stringify(saved))
  setActivePinia(createPinia())
  return useSettingsStore()
}

const persisted = () => JSON.parse(memoryStorage.get(KEY))

test('默认图库常量：横 13 张 / 竖 16 张，序号两位补零', () => {
  assert.equal(DEFAULT_LANDSCAPE.length, 13)
  assert.equal(DEFAULT_VERTICAL.length, 16)
  assert.equal(DEFAULT_LANDSCAPE[0], '/image/bg/Landscape/01.webp')
  assert.equal(DEFAULT_VERTICAL[15], '/image/bg/Vertical/16.webp')
  for (const url of [...DEFAULT_LANDSCAPE, ...DEFAULT_VERTICAL]) {
    assert.match(url, /^\/image\/bg\/(Landscape|Vertical)\/\d{2}\.webp$/, `图库路径格式不符：${url}`)
  }
})

test('无本地记录时使用内置默认值', () => {
  const store = freshStore()
  assert.equal(store.theme, 'light')
  assert.equal(store.backgroundImageEnabled, true)
  assert.equal(store.backgroundCarouselEnabled, true)
  assert.equal(store.sakuraEnabled, false)
  assert.equal(store.live2dEnabled, true)
  assert.equal(store.selectedLandscapeImage, DEFAULT_LANDSCAPE[0])
  assert.equal(store.selectedVerticalImage, DEFAULT_VERTICAL[0])
  assert.equal(store.isDark, false)
})

test('本地已保存的设置在初始化时恢复', () => {
  const store = freshStore({ theme: 'dark', sakuraEnabled: true, backgroundImageEnabled: false })
  assert.equal(store.theme, 'dark')
  assert.equal(store.isDark, true)
  assert.equal(store.sakuraEnabled, true)
  assert.equal(store.backgroundImageEnabled, false)
})

test('update：已知键写入并持久化，未知键被忽略', () => {
  const store = freshStore()
  store.update({ sakuraEnabled: true, live2dEnabled: false, unknownKey: 'x' })
  assert.equal(store.sakuraEnabled, true)
  assert.equal(store.live2dEnabled, false)
  const saved = persisted()
  assert.equal(saved.sakuraEnabled, true)
  assert.equal(saved.live2dEnabled, false)
  assert.ok(!('unknownKey' in saved), '未知键不得写入本地存储')
})

test('setTheme：切换主题并持久化', () => {
  const store = freshStore()
  store.setTheme('dark')
  assert.equal(store.isDark, true)
  assert.equal(persisted().theme, 'dark')
})

test('loadRemoteGallery：后台图库覆盖默认，且被移除的选中图落回第一张', async () => {
  const original = api.siteContent
  try {
    api.siteContent = async () => ({
      contentJson: JSON.stringify({ landscape: ['/g/l1.webp', '/g/l2.webp'], vertical: ['/g/v1.webp'] })
    })
    const store = freshStore()
    await store.loadRemoteGallery()
    assert.deepEqual(store.galleryLandscape, ['/g/l1.webp', '/g/l2.webp'])
    assert.deepEqual(store.galleryVertical, ['/g/v1.webp'])
    assert.equal(store.selectedLandscapeImage, '/g/l1.webp', '原选中图不在新图库时落回第一张')
    assert.equal(store.selectedVerticalImage, '/g/v1.webp')
    assert.equal(persisted().selectedLandscapeImage, '/g/l1.webp', '回退结果必须持久化')
  } finally {
    api.siteContent = original
  }
})

test('loadRemoteGallery：请求失败时静默保留内置默认图库', async () => {
  const original = api.siteContent
  try {
    api.siteContent = async () => { throw new Error('未配置') }
    const store = freshStore()
    await store.loadRemoteGallery()
    assert.deepEqual(store.galleryLandscape, DEFAULT_LANDSCAPE)
    assert.deepEqual(store.galleryVertical, DEFAULT_VERTICAL)
    assert.equal(store.selectedLandscapeImage, DEFAULT_LANDSCAPE[0])
  } finally {
    api.siteContent = original
  }
})
