// resourceSchemas 纯数据结构检查：约束 schema 与 ResourcePanel/FieldInput 的解析约定
// 运行方式：在 frontend/ 目录执行 npm test（Node 内置测试器 node --test，零额外依赖）
import test from 'node:test'
import assert from 'node:assert/strict'
import resourceSchemasDefault, { resourceSchemas } from './resourceSchemas.js'

// FieldInput.vue / ResourcePanel.vue 实际支持渲染的字段类型
const KNOWN_TYPES = new Set([
  'text', 'textarea', 'markdown', 'number', 'boolean',
  'tags', 'date', 'datetime', 'image', 'audio', 'select'
])

// 批量修改仅支持的类型（ResourcePanel：select/boolean 走下拉，number/text 走弹窗输入）
const BATCH_TYPES = new Set(['select', 'boolean', 'number', 'text'])

const isNonEmptyString = v => typeof v === 'string' && v.trim() !== ''

test('导出结构：默认导出与具名导出为同一非空数组', () => {
  assert.ok(Array.isArray(resourceSchemas), 'resourceSchemas 必须是数组')
  assert.ok(resourceSchemas.length > 0, 'resourceSchemas 不能为空')
  assert.equal(resourceSchemasDefault, resourceSchemas, '默认导出与具名导出必须是同一引用')
})

test('每个 schema 的 key/label 非空，key 全局唯一', () => {
  const seen = new Set()
  for (const schema of resourceSchemas) {
    assert.ok(isNonEmptyString(schema.key), `schema key 必须为非空字符串：${JSON.stringify(schema.key)}`)
    assert.ok(isNonEmptyString(schema.label), `[${schema.key}] label 必须为非空字符串`)
    assert.ok(!seen.has(schema.key), `schema key 重复：${schema.key}`)
    seen.add(schema.key)
  }
})

test('每个 schema 必须声明非空的 fields 与 columns', () => {
  for (const schema of resourceSchemas) {
    assert.ok(Array.isArray(schema.fields) && schema.fields.length > 0, `[${schema.key}] fields 必须为非空数组`)
    assert.ok(Array.isArray(schema.columns) && schema.columns.length > 0, `[${schema.key}] columns 必须为非空数组`)
  }
})

test('字段 name/label 非空、type 为已支持类型，且同一 schema 内字段名唯一', () => {
  for (const schema of resourceSchemas) {
    const names = new Set()
    for (const field of schema.fields) {
      assert.ok(isNonEmptyString(field.name), `[${schema.key}] 字段 name 必须为非空字符串：${JSON.stringify(field.name)}`)
      assert.ok(isNonEmptyString(field.label), `[${schema.key}.${field.name}] label 必须为非空字符串`)
      assert.ok(KNOWN_TYPES.has(field.type), `[${schema.key}.${field.name}] 未知字段类型：${field.type}`)
      assert.ok(!names.has(field.name), `[${schema.key}] 字段名重复：${field.name}`)
      names.add(field.name)
    }
  }
})

test('select 字段必须提供非空 options，且每项为非空字符串', () => {
  for (const schema of resourceSchemas) {
    for (const field of schema.fields) {
      if (field.type !== 'select') continue
      assert.ok(
        Array.isArray(field.options) && field.options.length > 0,
        `[${schema.key}.${field.name}] select 字段必须配置非空 options`
      )
      for (const option of field.options) {
        assert.ok(isNonEmptyString(option), `[${schema.key}.${field.name}] options 每项必须为非空字符串`)
      }
    }
  }
})

test('image 字段的 ratio（如配置）必须为正的有限数', () => {
  for (const schema of resourceSchemas) {
    for (const field of schema.fields) {
      if (!('ratio' in field)) continue
      assert.equal(field.type, 'image', `[${schema.key}.${field.name}] ratio 仅允许配置在 image 字段上`)
      assert.ok(
        typeof field.ratio === 'number' && Number.isFinite(field.ratio) && field.ratio > 0,
        `[${schema.key}.${field.name}] ratio 必须为正的有限数：${field.ratio}`
      )
    }
  }
})

test('batch 字段类型限定于批量修改支持的类型', () => {
  for (const schema of resourceSchemas) {
    for (const field of schema.fields) {
      if (!field.batch) continue
      assert.equal(field.batch, true, `[${schema.key}.${field.name}] batch 只能为布尔 true`)
      assert.ok(BATCH_TYPES.has(field.type), `[${schema.key}.${field.name}] batch 不支持类型：${field.type}`)
    }
  }
})

test('boolean 字段的 default（如配置）必须为布尔值', () => {
  for (const schema of resourceSchemas) {
    for (const field of schema.fields) {
      if (field.type !== 'boolean' || !('default' in field)) continue
      assert.equal(typeof field.default, 'boolean', `[${schema.key}.${field.name}] default 必须为布尔值`)
    }
  }
})

test('columns 列名必须能解析到字段，或为内置 id 兜底列', () => {
  // ResourcePanel 对未匹配字段的列仅对 id 提供 'ID' 标签兜底，其余列名必须命中字段
  for (const schema of resourceSchemas) {
    const fieldNames = new Set(schema.fields.map(f => f.name))
    for (const col of schema.columns) {
      assert.ok(isNonEmptyString(col), `[${schema.key}] 列名必须为非空字符串：${JSON.stringify(col)}`)
      assert.ok(
        fieldNames.has(col) || col === 'id',
        `[${schema.key}] 列 ${col} 未在 fields 中定义（仅 id 允许兜底）`
      )
    }
  }
})

test('字段可选元数据 group/tip/required 类型合法', () => {
  for (const schema of resourceSchemas) {
    for (const field of schema.fields) {
      if ('group' in field) assert.ok(isNonEmptyString(field.group), `[${schema.key}.${field.name}] group 必须为非空字符串`)
      if ('tip' in field) assert.ok(isNonEmptyString(field.tip), `[${schema.key}.${field.name}] tip 必须为非空字符串`)
      if ('required' in field) assert.equal(typeof field.required, 'boolean', `[${schema.key}.${field.name}] required 必须为布尔值`)
    }
  }
})

test('高字段量通用资源配置分组、说明和业务必填元数据', () => {
  const targetKeys = ['home-carousels', 'tool-sites', 'called-texts', 'musics', 'friend-links']
  for (const key of targetKeys) {
    const schema = resourceSchemas.find(item => item.key === key)
    assert.ok(schema, `缺少目标 schema：${key}`)
    assert.ok(schema.fields.every(field => isNonEmptyString(field.group)), `[${key}] 每个字段都应配置 group`)
    assert.ok(schema.fields.some(field => isNonEmptyString(field.tip)), `[${key}] 至少应提供一条字段说明`)
    assert.ok(schema.fields.some(field => field.required === true), `[${key}] 至少应标记一个业务必填字段`)
  }
})
