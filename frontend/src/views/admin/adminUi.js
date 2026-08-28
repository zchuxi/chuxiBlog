const normalizeText = value => String(value ?? '').trim().toLocaleLowerCase('zh-CN')

export function filterMenuGroups(groups, query) {
  const keyword = normalizeText(query)
  if (!keyword) return groups
  return groups
    .map(group => {
      const groupMatched = normalizeText(group.title).includes(keyword)
      const items = groupMatched
        ? group.items
        : group.items.filter(item =>
          [item.label, item.description].some(value => normalizeText(value).includes(keyword))
        )
      return { ...group, items }
    })
    .filter(group => group.items.length > 0)
}

function searchableValue(value, column) {
  if (column.type === 'boolean') return value ? '是 true' : '否 false'
  if (Array.isArray(value)) return value.join(' ')
  return value
}

export function filterResourceRows(rows, columns, query) {
  const keyword = normalizeText(query)
  if (!keyword) return rows
  return rows.filter(row => columns.some(column =>
    normalizeText(searchableValue(row[column.name], column)).includes(keyword)
  ))
}

/**
 * 中文优先的比较器。numeric: true 让「文章 2」排在「文章 10」前面，
 * 否则按字符串比较会得到 10 < 2 的反直觉结果。
 */
const COLLATOR = new Intl.Collator('zh-Hans-CN', { numeric: true, sensitivity: 'base' })

/** 排序取值：按列类型归一化为可比较的原始值。空值统一返回 null，由比较器沉底 */
function sortableValue(value, column) {
  if (value == null || value === '') return null
  if (Array.isArray(value)) return value.length ? value.join(', ') : null
  if (column.type === 'boolean') return value ? 1 : 0
  if (column.type === 'number') {
    const num = Number(value)
    return Number.isNaN(num) ? null : num
  }
  if (column.type === 'date' || column.type === 'datetime') {
    const ts = Date.parse(value)
    // 非标准格式解析失败时退回字符串比较，避免整列被误判为空值而全部沉底
    return Number.isNaN(ts) ? String(value) : ts
  }
  if (typeof value === 'number' || typeof value === 'boolean') return value
  return String(value)
}

/**
 * 按单列排序，返回新数组（不改动入参：无关键词时 filterResourceRows 直接返回
 * 原始 rows 引用，原地 sort 会污染数据源）。
 * 空值恒定排在末尾，不随升降序翻转——列表里「没填」始终应该垫底。
 */
export function sortResourceRows(rows, column, direction) {
  if (!column || (direction !== 'asc' && direction !== 'desc')) return rows
  const factor = direction === 'asc' ? 1 : -1
  return [...rows].sort((rowA, rowB) => {
    const a = sortableValue(rowA[column.name], column)
    const b = sortableValue(rowB[column.name], column)
    if (a === null && b === null) return 0
    if (a === null) return 1
    if (b === null) return -1
    if (typeof a === 'number' && typeof b === 'number') return (a - b) * factor
    return COLLATOR.compare(String(a), String(b)) * factor
  })
}

export function groupFields(fields) {
  const groups = new Map()
  for (const field of fields) {
    const label = field.group || '基本信息'
    if (!groups.has(label)) groups.set(label, [])
    groups.get(label).push(field)
  }
  return Array.from(groups, ([label, groupedFields]) => ({
    key: label,
    label,
    fields: groupedFields
  }))
}

function normalizeFieldValue(field, raw) {
  if (field.type === 'tags') {
    return String(raw || '')
      .split(/[,，]/)
      .map(value => value.trim())
      .filter(Boolean)
  }
  if (field.type === 'number') return raw === '' || raw == null ? null : Number(raw)
  if (field.type === 'boolean') return Boolean(raw)
  if (field.type === 'date' || field.type === 'datetime') return raw || null
  return raw ?? ''
}

export function createFormSnapshot(form, fields) {
  return JSON.stringify(Object.fromEntries(
    fields.map(field => [field.name, normalizeFieldValue(field, form[field.name])])
  ))
}

export function isFormDirty(form, fields, initialSnapshot) {
  return createFormSnapshot(form, fields) !== initialSnapshot
}

/**
 * 裁切结果的上传文件名：<原名去噪>-crop-<宽>x<高><扩展名>。
 * 去噪 = 去掉后端上传时加的 8 位 uuid 前缀（外链取回的还多一段 fetch-）和上一轮的
 * -crop-WxH，否则反复裁切会让前缀后缀一层层叠上去，既看不出原图是谁也越滚越长。
 * 后端保存时还会再加一段新的 uuid 前缀保证唯一，最终形如 a1b2c3d4-封面-crop-1200x800.png。
 */
export function buildCropFileName(sourceName, width, height, ext) {
  const name = String(sourceName || '')
  const dot = name.lastIndexOf('.')
  const base = (dot > 0 ? name.slice(0, dot) : name)
    .replace(/^[0-9a-f]{8}-(fetch-)?/i, '')
    .replace(/-crop-\d+x\d+$/i, '')
  return `${base || 'image'}-crop-${width}x${height}${ext}`
}
