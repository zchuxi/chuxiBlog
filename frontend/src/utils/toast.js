// ── 全局轻量 toast 单例 ──────────────────────────────
// 用法：App.vue 挂载 <CxMessage ref> 后调用 registerToast(ref)，
// 之后任意模块 import { toastError } 即可弹提示。
// axios 拦截器失败时默认调用 toastError；调用方可传 { silent: true }
// 跳过自动提示（预期失败并已降级兜底的接口，如 views / bangumiCalendar）。
let messageApi = null

export function registerToast(api) {
  messageApi = api
}

function toast(type, text, duration) {
  if (messageApi && typeof messageApi.pushMessage === 'function') {
    messageApi.pushMessage(type, text, duration)
  } else {
    // 未注册时兜底 console，避免静默
    console.warn('[toast] 未注册 message 实例：', type, text)
  }
}

export function toastError(text, duration) {
  toast('error', text, duration)
}
export function toastInfo(text, duration) {
  toast('info', text, duration)
}
export function toastSuccess(text, duration) {
  toast('success', text, duration)
}

export default { registerToast, toastError, toastInfo, toastSuccess }
