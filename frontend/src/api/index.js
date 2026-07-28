import axios from 'axios'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.response.use(res => {
  const body = res.data
  if (body && body.code === 0) return body.data
  return Promise.reject(new Error(body && body.message ? body.message : '请求失败'))
})

export const api = {
  homeLanding: () => http.get('/front/home/landing'),
  homeArticles: (pageNo = 1, pageSize = 6) => http.get('/front/home/articles', { params: { pageNo, pageSize } }),
  teamMembers: () => http.get('/front/home/team-members'),
  articleDetail: id => http.get(`/front/articles/${id}`),
  articleComments: id => http.get(`/front/articles/${id}/comments`),
  addComment: (id, data) => http.post(`/front/articles/${id}/comments`, data),
  likeComment: id => http.post(`/front/articles/comments/${id}/likes`),
  searchArticles: (keyword, pageNo = 1, pageSize = 6) => http.get('/front/articles/search', { params: { keyword, pageNo, pageSize } }),
  timelineLanding: () => http.get('/front/timeline/landing'),
  archiveLanding: () => http.get('/front/archive/landing'),
  treeHoleBarrages: () => http.get('/front/tree-hole/barrages'),
  addBarrage: data => http.post('/front/tree-hole/barrages', data),
  likeBarrage: id => http.post(`/front/tree-hole/barrages/${id}/likes`),
  calledTexts: () => http.get('/front/tree-hole/called-texts'),
  parallaxStories: () => http.get('/front/parallax/stories'),
  toolsLanding: () => http.get('/front/tools/landing'),
  bangumiRecords: () => http.get('/front/bangumi'),
  bangumiDetail: id => http.get(`/front/bangumi/${id}`),
  friendLinks: () => http.get('/front/friend-links'),
  siteContent: key => http.get(`/front/site-content/${key}`),
  bumpViews: () => http.post('/front/views/bump'),
  views: () => http.get('/front/views'),
  music: () => http.get('/music')
}

export default http
