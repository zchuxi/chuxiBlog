/**
 * 日期字符串解析/格式化：CxDatePicker 与后端 LocalDateTime 的取值契约。
 *
 *   type='date'     → "YYYY-MM-DD"
 *   type='datetime' → "YYYY-MM-DDTHH:mm:ss"
 *
 * 全程按本地时间拼字符串，**不经 toISOString()** —— 那会转成 UTC，
 * 在 UTC+8 下会把 00:00~07:59 的日期整体退一天。
 * 抽成独立模块以便 node --test 直接加载（组件内的逻辑无法被 Node 导入）。
 */

const pad = (n) => String(n).padStart(2, '0')

/**
 * 解析 "YYYY-MM-DD" 前缀 + 可选 "THH:mm[:ss]"，容忍后端多出的纳秒尾巴。
 * 越界日期（如 2026-02-31）返回 null —— Date 会静默滚到下个月，必须拒掉。
 * @returns {Date|null}
 */
export function parseDateValue(str) {
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(String(str || ''))
  if (!m) return null
  const y = +m[1]
  const mo = +m[2]
  const d = +m[3]
  const dt = new Date(y, mo - 1, d, +(m[4] || 0), +(m[5] || 0), +(m[6] || 0))
  if (dt.getFullYear() !== y || dt.getMonth() !== mo - 1 || dt.getDate() !== d) return null
  return dt
}

/** 按契约格式化；withTime=false 时只出日期段 */
export function formatDateValue(dt, withTime = true) {
  const base = `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}`
  return withTime
    ? `${base}T${pad(dt.getHours())}:${pad(dt.getMinutes())}:${pad(dt.getSeconds())}`
    : base
}

/** 面板展示用：日期与时间以空格分隔，比 "T" 易读 */
export function displayDateValue(str, withTime = true) {
  const d = parseDateValue(str)
  if (!d) return ''
  const base = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  return withTime ? `${base} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` : base
}

/**
 * 6×7 日格，周一为首列，含上下月补位。
 * @param {Date} cursor 面板浏览到的年月
 * @param {Date|null} selected 当前选中值
 * @param {Date} today 今天（可注入以便测试）
 */
export function buildMonthGrid(cursor, selected, today = new Date()) {
  const y = cursor.getFullYear()
  const m = cursor.getMonth()
  // getDay() 周日=0，转成周一=0
  const lead = (new Date(y, m, 1).getDay() + 6) % 7
  const same = (a, b) =>
    !!a && !!b && a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate()
  return Array.from({ length: 42 }, (_, i) => {
    const d = new Date(y, m, 1 + i - lead)
    return {
      key: `${d.getFullYear()}-${d.getMonth()}-${d.getDate()}`,
      label: d.getDate(),
      date: d,
      outside: d.getMonth() !== m,
      today: same(d, today),
      active: same(d, selected)
    }
  })
}

export { pad }
