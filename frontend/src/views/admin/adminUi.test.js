import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createFormSnapshot,
  filterMenuGroups,
  filterResourceRows,
  groupFields,
  isFormDirty,
  sortResourceRows
} from './adminUi.js'

test('filterMenuGroups 同时按菜单名、分组名和说明过滤，并移除空分组', () => {
  const groups = [
    { title: '内容', items: [{ key: 'articles', label: '文章管理', description: '编辑博客正文' }] },
    { title: '资源', items: [{ key: 'media', label: '图片管理', description: '上传素材' }] }
  ]
  assert.deepEqual(filterMenuGroups(groups, '博客'), [{
    title: '内容',
    items: [{ key: 'articles', label: '文章管理', description: '编辑博客正文' }]
  }])
  assert.equal(filterMenuGroups(groups, '资源')[0].items[0].key, 'media')
  assert.equal(filterMenuGroups(groups, '不存在').length, 0)
})

test('filterResourceRows 只检索展示列，忽略大小写并支持数组与布尔值', () => {
  const rows = [
    { id: 1, title: 'Hello Vue', tags: ['前端', 'Vue'], visible: true, secret: '隐藏命中' },
    { id: 2, title: 'Spring', tags: ['后端'], visible: false, secret: '' }
  ]
  const columns = [{ name: 'title' }, { name: 'tags' }, { name: 'visible', type: 'boolean' }]
  assert.deepEqual(filterResourceRows(rows, columns, 'vue').map(row => row.id), [1])
  assert.deepEqual(filterResourceRows(rows, columns, '前端').map(row => row.id), [1])
  assert.deepEqual(filterResourceRows(rows, columns, '否').map(row => row.id), [2])
  assert.equal(filterResourceRows(rows, columns, '隐藏命中').length, 0)
})

test('sortResourceRows 不改动入参数组', () => {
  // filterResourceRows 无关键词时返回原引用，原地 sort 会污染 rows.value
  const rows = [{ id: 2 }, { id: 1 }]
  const sorted = sortResourceRows(rows, { name: 'id', type: 'number' }, 'asc')
  assert.deepEqual(rows.map(row => row.id), [2, 1])
  assert.deepEqual(sorted.map(row => row.id), [1, 2])
  assert.notEqual(sorted, rows)
})

test('sortResourceRows 按列类型比较：数字按数值、布尔真在前、日期按时间', () => {
  const rows = [{ n: 10 }, { n: 2 }, { n: 9 }]
  assert.deepEqual(
    sortResourceRows(rows, { name: 'n', type: 'number' }, 'asc').map(row => row.n),
    [2, 9, 10]
  )
  const flags = [{ v: false }, { v: true }]
  assert.deepEqual(
    sortResourceRows(flags, { name: 'v', type: 'boolean' }, 'asc').map(row => row.v),
    [false, true]
  )
  const dates = [{ d: '2026-08-23' }, { d: '2025-01-05' }]
  assert.deepEqual(
    sortResourceRows(dates, { name: 'd', type: 'datetime' }, 'asc').map(row => row.d),
    ['2025-01-05', '2026-08-23']
  )
})

test('sortResourceRows 文本按中文排序且数字部分按数值，降序对称', () => {
  const rows = [{ t: '文章 10' }, { t: '文章 2' }]
  assert.deepEqual(
    sortResourceRows(rows, { name: 't', type: 'text' }, 'asc').map(row => row.t),
    ['文章 2', '文章 10']
  )
  assert.deepEqual(
    sortResourceRows(rows, { name: 't', type: 'text' }, 'desc').map(row => row.t),
    ['文章 10', '文章 2']
  )
})

test('sortResourceRows 空值恒定沉底，升降序都不翻到前面', () => {
  const rows = [{ t: '' }, { t: 'b' }, { t: null }, { t: 'a' }]
  assert.deepEqual(
    sortResourceRows(rows, { name: 't', type: 'text' }, 'asc').map(row => row.t),
    ['a', 'b', '', null]
  )
  assert.deepEqual(
    sortResourceRows(rows, { name: 't', type: 'text' }, 'desc').map(row => row.t),
    ['b', 'a', '', null]
  )
})

test('sortResourceRows 无排序列或方向非法时原样返回', () => {
  const rows = [{ id: 2 }, { id: 1 }]
  assert.equal(sortResourceRows(rows, null, 'asc'), rows)
  assert.equal(sortResourceRows(rows, { name: 'id' }, ''), rows)
})

test('groupFields 按首次出现顺序分组，未配置字段进入基本信息', () => {
  const fields = [
    { name: 'title', label: '标题' },
    { name: 'cover', label: '封面', group: '内容与媒体' },
    { name: 'visible', label: '显示', group: '状态与排序' }
  ]
  assert.deepEqual(groupFields(fields).map(group => group.label), ['基本信息', '内容与媒体', '状态与排序'])
  assert.deepEqual(groupFields(fields)[0].fields.map(field => field.name), ['title'])
})

test('表单快照按字段类型规范化，等价输入不误报脏状态', () => {
  const fields = [
    { name: 'tags', type: 'tags' },
    { name: 'sortIndex', type: 'number' },
    { name: 'visible', type: 'boolean' },
    { name: 'publishedAt', type: 'datetime' }
  ]
  const initial = createFormSnapshot(
    { tags: 'Vue, 博客', sortIndex: '2', visible: 1, publishedAt: '' },
    fields
  )
  assert.equal(isFormDirty(
    { tags: 'Vue，博客', sortIndex: 2, visible: true, publishedAt: null },
    fields,
    initial
  ), false)
  assert.equal(isFormDirty(
    { tags: 'Vue, 博客, 新增', sortIndex: 2, visible: true, publishedAt: null },
    fields,
    initial
  ), true)
})
