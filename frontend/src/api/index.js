import axios from 'axios'
import { toastError } from '../utils/toast.js'
import { getVisitorToken, saveVisitorToken } from '../utils/visitorId.js'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

// 失败时是否弹全局 toast：接口调用方传 { silent: true } 可跳过（预期失败并已降级兜底）
function shouldToast(config) {
  return !(config && config.silent)
}

http.interceptors.response.use(res => {
  // 服务端签发的匿名身份（SEC-001）：任何响应都可能带回新 token，前端持久化
  const issued = res.headers && res.headers['x-visitor-token']
  if (issued) saveVisitorToken(issued)
  const body = res.data
  if (body && body.code === 0) return body.data
  // 业务码失败（如 4xx 语义错误），默认弹提示
  const message = body && body.message ? body.message : '请求失败'
  if (shouldToast(res.config)) toastError(message)
  return Promise.reject(new Error(message))
}, err => {
  // 网络错误 / 超时 / 5xx
  if (err.response && err.response.headers) {
    const issued = err.response.headers['x-visitor-token']
    if (issued) saveVisitorToken(issued)
  }
  const message = (err.response && err.response.data && err.response.data.message)
    || err.message
    || '网络请求失败，请稍后重试'
  if (shouldToast(err.config)) toastError(message)
  return Promise.reject(err)
})

export const api = {
  homeLanding: () => http.get('/front/home/landing', { silent: true }),
  homeArticles: (pageNo = 1, pageSize = 6) => http.get('/front/home/articles', { params: { pageNo, pageSize } }),
  teamMembers: () => http.get('/front/home/team-members', { silent: true }),
  articleDetail: id => http.get(`/front/articles/${id}`),
  articleComments: (id, pageNo = 1, pageSize = 20) => http.get(`/front/articles/${id}/comments`, {
    params: { pageNo, pageSize },
    headers: { 'X-Visitor-Id': getVisitorToken() }
  }),
  addComment: (id, data) => http.post(`/front/articles/${id}/comments`, data),
  likeComment: id => http.post(`/front/articles/comments/${id}/likes`, null, {
    headers: { 'X-Visitor-Id': getVisitorToken() }
  }),
  // 匿名访客身份签发（SEC-001）：写操作前调用确保服务端签名 token
  visitorToken: () => http.get('/front/visitor/token', { silent: true }),
  searchArticles: (keyword, pageNo = 1, pageSize = 6) => http.get('/front/articles/search', { params: { keyword, pageNo, pageSize } }),
  timelineLanding: () => http.get('/front/timeline/landing', { silent: true }),
  archiveLanding: () => http.get('/front/archive/landing', { silent: true }),
  treeHoleBarrages: () => http.get('/front/tree-hole/barrages', { silent: true }),
  addBarrage: data => http.post('/front/tree-hole/barrages', data),
  likeBarrage: id => http.post(`/front/tree-hole/barrages/${id}/likes`, null, {
    headers: { 'X-Visitor-Id': getVisitorToken() }
  }),
  calledTexts: () => http.get('/front/tree-hole/called-texts', { silent: true }),
  parallaxStories: () => http.get('/front/parallax/stories', { silent: true }),
  toolsLanding: () => http.get('/front/tools/landing', { silent: true }),
  bangumiRecords: () => http.get('/front/bangumi', { silent: true }),
  bangumiDetail: id => http.get(`/front/bangumi/${id}`),
  // 每周放送日历：后端三层缓存（内存→磁盘→直连兜底），不再浏览器直连 api.bgm.tv
  bangumiCalendar: () => http.get('/front/bangumi/calendar', { silent: true }),
  // 番剧详情在线数据（条目/剧集/角色）：后端三层缓存（内存→磁盘→直连兜底），无代理也能看
  bangumiBgm: (kind, sid) => http.get(`/front/bangumi/bgm/${kind}/${sid}`, { silent: true }),
  // AI 助手对话：失败在面板内展示错误气泡，不弹全局 toast；上游模型调用较慢，放宽前端超时
  aiChat: messages => http.post('/front/ai/chat', { messages }, { silent: true, timeout: 60000 }),
  friendLinks: () => http.get('/front/friend-links'),
  // 站点文案/外观/导航等配置读取：业务语义为"读不到就用默认"，缺失返回 R.fail 是预期路径，不弹 toast
  siteContent: key => http.get(`/front/site-content/${key}`, { silent: true }),
  bumpViews: () => http.post('/front/views/bump', null, { silent: true }),
  views: () => http.get('/front/views', { silent: true }),
  music: () => http.get('/music')
}

export default http
