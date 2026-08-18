import test from 'node:test'
import assert from 'node:assert/strict'
import {
  createFormSnapshot,
  filterMenuGroups,
  filterResourceRows,
  groupFields,
  isFormDirty
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
