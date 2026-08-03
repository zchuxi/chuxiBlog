import axios from 'axios'
import { toastError } from '../utils/toast.js'
import { getVisitorId } from '../utils/visitorId.js'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

// 失败时是否弹全局 toast：接口调用方传 { silent: true } 可跳过（预期失败并已降级兜底）
function shouldToast(config) {
  return !(config && config.silent)
}

http.interceptors.response.use(res => {
  const body = res.data
  if (body && body.code === 0) return body.data
  // 业务码失败（如 4xx 语义错误），默认弹提示
  const message = body && body.message ? body.message : '请求失败'
  if (shouldToast(res.config)) toastError(message)
  return Promise.reject(new Error(message))
}, err => {
  // 网络错误 / 超时 / 5xx
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
  articleComments: id => http.get(`/front/articles/${id}/comments`, {
    headers: { 'X-Visitor-Id': getVisitorId() }
  }),
  addComment: (id, data) => http.post(`/front/articles/${id}/comments`, data),
  likeComment: id => http.post(`/front/articles/comments/${id}/likes`, null, {
    headers: { 'X-Visitor-Id': getVisitorId() }
  }),
  searchArticles: (keyword, pageNo = 1, pageSize = 6) => http.get('/front/articles/search', { params: { keyword, pageNo, pageSize } }),
  timelineLanding: () => http.get('/front/timeline/landing', { silent: true }),
  archiveLanding: () => http.get('/front/archive/landing', { silent: true }),
  treeHoleBarrages: () => http.get('/front/tree-hole/barrages', { silent: true }),
  addBarrage: data => http.post('/front/tree-hole/barrages', data),
  likeBarrage: id => http.post(`/front/tree-hole/barrages/${id}/likes`),
  calledTexts: () => http.get('/front/tree-hole/called-texts', { silent: true }),
  parallaxStories: () => http.get('/front/parallax/stories', { silent: true }),
  toolsLanding: () => http.get('/front/tools/landing', { silent: true }),
  bangumiRecords: () => http.get('/front/bangumi', { silent: true }),
  bangumiDetail: id => http.get(`/front/bangumi/${id}`),
  // 每周放送日历：后端三层缓存（内存→磁盘→直连兜底），不再浏览器直连 api.bgm.tv
  bangumiCalendar: () => http.get('/front/bangumi/calendar', { silent: true }),
  friendLinks: () => http.get('/front/friend-links'),
  // 站点文案/外观/导航等配置读取：业务语义为"读不到就用默认"，缺失返回 R.fail 是预期路径，不弹 toast
  siteContent: key => http.get(`/front/site-content/${key}`, { silent: true }),
  bumpViews: () => http.post('/front/views/bump', null, { silent: true }),
  views: () => http.get('/front/views', { silent: true }),
  music: () => http.get('/music')
}

export default http
