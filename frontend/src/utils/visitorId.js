const STORAGE_KEY = 'chuxi-visitor-token'

/**
 * 匿名访客身份（SEC-001）：标识由服务端 HMAC 签发（{rawId}.{sig}），
 * 客户端无法自行构造合法身份，只能通过 GET /api/front/visitor/token 获取。
 * 旧版 localStorage 键（chuxi-visitor-id，裸 id）不再使用。
 */

export function getVisitorToken() {
  try {
    const stored = globalThis.localStorage?.getItem(STORAGE_KEY)
    return stored || ''
  } catch {
    return ''
  }
}

export function saveVisitorToken(token) {
  if (!token) return
  try {
    globalThis.localStorage?.setItem(STORAGE_KEY, token)
  } catch { /* 存储不可用时静默降级 */ }
}

/** 确保本地持有有效签名 token；缺失时向服务端签发（写操作前调用） */
export async function ensureVisitorToken() {
  const existing = getVisitorToken()
  if (existing) return existing
  try {
    const { api } = await import('../api/index.js')
    const data = await api.visitorToken()
    if (data && data.token) {
      saveVisitorToken(data.token)
      return data.token
    }
  } catch { /* 签发失败由调用方降级处理 */ }
  return ''
}

export { STORAGE_KEY }
