const STORAGE_KEY = 'chuxi-visitor-id'
const VALID_ID = /^[A-Za-z0-9_-]{16,64}$/

function createVisitorId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  const random = Math.random().toString(36).slice(2)
  return `visitor_${Date.now().toString(36)}_${random}`
}

export function getVisitorId() {
  try {
    const stored = globalThis.localStorage?.getItem(STORAGE_KEY)
    if (VALID_ID.test(stored || '')) return stored
    const created = createVisitorId()
    globalThis.localStorage?.setItem(STORAGE_KEY, created)
    return created
  } catch {
    return createVisitorId()
  }
}

export { STORAGE_KEY, VALID_ID }
