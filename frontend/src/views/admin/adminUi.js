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
