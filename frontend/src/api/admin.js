import axios from 'axios'

// 管理端独立 axios 实例：自动携带 token，401 时清除本地登录态
const TOKEN_KEY = 'chuxi-admin-token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = token => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  res => {
    const body = res.data
    if (body && body.code === 0) return body.data
    return Promise.reject(new Error(body && body.message ? body.message : '请求失败'))
  },
  err => {
    if (err.response && err.response.status === 401) {
      clearToken()
      const e = new Error('未登录或登录已过期')
      e.unauthorized = true
      return Promise.reject(e)
    }
    return Promise.reject(err)
  }
)

export const login = (username, password) => http.post('/auth/login', { username, password })
export const me = () => http.get('/auth/me')

// 概览统计（仪表盘）
export const overview = () => http.get('/admin/overview')

// 修改管理密码（需登录态）
export const changePassword = (oldPassword, newPassword) =>
  http.post('/auth/password', { oldPassword, newPassword })

// 站点文案（SiteContent）：list 走管理端全量；get 走公开单 key（无记录时 code 1 → reject）
export const siteContentApi = {
  list: () => http.get('/admin/site-content'),
  get: key => http.get(`/front/site-content/${encodeURIComponent(key)}`),
  save: (key, jsonString) =>
    http.put(`/admin/site-content/${encodeURIComponent(key)}`, { contentJson: jsonString })
}

// 13 种资源统一 CRUD
const RESOURCE_KEYS = [
  'articles',
  'home-carousels',
  'collapse-cards',
  'team-members',
  'archive-categories',
  'timeline-carousels',
  'timeline-events',
  'parallax-stories',
  'tool-sites',
  'barrages',
  'called-texts',
  'musics',
  'comments',
  'bangumi-records',
  'friend-links'
]

// 媒体库（图片/音频）：音频文件体积大，上传单独放宽超时并支持进度回调
export const mediaApi = {
  list: () => http.get('/admin/media'),
  upload: (file, filename, onProgress) => {
    const form = new FormData()
    form.append('file', file, filename || file.name)
    return http.post('/admin/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 300000,
      onUploadProgress: e => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
      }
    })
  },
  remove: name => http.delete(`/admin/media/${encodeURIComponent(name)}`),
  // 取回外链图：仅支持本站 OSS 公网域；返回 { name, url, size }，url 是新的站内副本
  fetch: url => http.post('/admin/media/fetch', { url })
}

const crud = res => ({
  list: () => http.get(`/admin/${res}`),
  create: data => http.post(`/admin/${res}`, data),
  update: (id, data) => http.put(`/admin/${res}/${id}`, data),
  remove: id => http.delete(`/admin/${res}/${id}`)
})

export const adminApi = Object.fromEntries(RESOURCE_KEYS.map(key => [key, crud(key)]))
// 日历页通过 adminApi.me() 探活，单独挂上避免 tree-shake 把它干掉（被删后调用会 undefined → 抛错 → catch 清 token）
adminApi.me = () => http.get('/auth/me')

export default http
