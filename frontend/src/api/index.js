import axios from 'axios'
import { useAuthStore } from '@/store/auth'
import { attachColdStartTracking } from './coldStart'

// 冷启动说明见 goalApi.js —— Render 免费实例休眠后首次请求需 90 秒以上。
// 登录接口同样走这个实例，10 秒超时会让用户在冷启动期间连登录都做不到。
const REQUEST_TIMEOUT = 90000

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api' : '/api',
  timeout: REQUEST_TIMEOUT
})

attachColdStartTracking(api)

api.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

function wrapResponse(res) {
  if (res.data && typeof res.data === 'object') return res.data
  return { success: true, data: res.data }
}

export const authApi = {
  login: (username, password) => api.post('/auth/login', { username, password }).then(wrapResponse),
  me: () => api.get('/auth/me').then(wrapResponse)
}

export const assignmentApi = {
  list: () => api.get('/assignments').then(wrapResponse),
  get: (id) => api.get(`/assignments/${id}`).then(wrapResponse),
  create: (payload) => api.post('/assignments', payload).then(wrapResponse),
  publish: (id) => api.patch(`/assignments/${id}/publish`).then(wrapResponse),
  // 作业选择器专用：分页 + 关键词/班级/进行中筛选。
  // 状态过滤在后端完成（默认只返回 PUBLISHED），前端不再自行筛选。
  page: (params) => api.get(`/assignments/page`, { params }).then(wrapResponse)
}

export const answerApi = {
  submit: (assignmentId, payload) => api.post(`/assignments/${assignmentId}/answers`, payload).then(wrapResponse),
  list: (assignmentId) => api.get(`/assignments/${assignmentId}/answers`).then(wrapResponse),
  stats: (assignmentId) => api.get(`/assignments/${assignmentId}/answers/stats`).then(wrapResponse)
}

export const questionApi = {
  list: () => api.get('/questions').then(wrapResponse),
  // 列表/选择器用的轻量数据，不含选项、解析步骤与图片
  listSummary: () => api.get('/questions/summary').then(wrapResponse),
  // 分页 + 筛选版，供题目选择器使用（题库变大后只取当前页）
  listSummaryPaged: (params) => api.get('/questions/summary/page', { params }).then(wrapResponse),
  get: (id) => api.get(`/questions/${id}`).then(wrapResponse),
  create: (payload) => api.post('/questions', payload).then(wrapResponse),
  update: (id, payload) => api.put(`/questions/${id}`, payload).then(wrapResponse),
  delete: (id) => api.delete(`/questions/${id}`).then(wrapResponse),
  // 管理员专用：设置共享范围与共享教师名单。后端会拒绝非管理员调用
  updateShares: (id, payload) => api.patch(`/questions/${id}/shares`, payload).then(wrapResponse),
  getTags: () => api.get('/questions/tags').then(wrapResponse)
}

// 错题本：学生手动收藏的题目。
// 所有接口都不传 studentId —— 学生身份完全由 JWT 决定（后端从 credentials 取 userId）。
//
// 注意 /api/mistakes/** 不在后端 SecurityConfig 的 permitAll 列表里，
// 未登录时返回 403（而不是 {success:false}），axios 会 reject 到调用方的 catch。
export const mistakeApi = {
  list: (params) => api.get('/mistakes', { params }).then(wrapResponse),
  summary: () => api.get('/mistakes/summary').then(wrapResponse),
  get: (id) => api.get(`/mistakes/${id}`).then(wrapResponse),
  add: (payload) => api.post('/mistakes', payload).then(wrapResponse),
  // 按 questionId 而非 note id：做题页只知道题目 ID
  remove: (questionId) => api.delete(`/mistakes/questions/${questionId}`).then(wrapResponse),
  saveNote: (questionId, payload) => api.put(`/mistakes/questions/${questionId}/note`, payload).then(wrapResponse),
  // 做题页批量回填星标状态。
  //
  // 【注意】必须传逗号拼接的字符串，不能直接传数组：
  // axios 默认把数组序列化成 questionIds[]=1&questionIds[]=2（带方括号），
  // Spring 的 List<Long> 绑定不认这种 key，会静默地收到空列表 —— 请求返回 200，
  // 但结果永远是 []，星标全部显示为未收藏。逗号形式 questionIds=1,2,3 才能正确绑定。
  collected: (questionIds) =>
    api.get('/mistakes/collected', { params: { questionIds: (questionIds || []).join(',') } }).then(wrapResponse)
}

export const gradingApi = {
  pending: () => api.get('/grading/pending').then(wrapResponse),
  grade: (payload) => api.post('/grading/grade', payload).then(wrapResponse)
}

export const userApi = {
  // 教师列表，仅供管理员的「共享设置」使用。
  // 注意：/api/users 直接返回数组，不是 ApiResponse 包装，所以取 res.data（与 classGroupApi 同）。
  // 后端按角色裁剪该接口 —— 非管理员调用时只会拿到学生，过滤后为空数组，符合预期。
  listTeachers: () => api.get('/users').then(res => (res.data || []).filter(u => u.role === 'TEACHER'))
}

export const classGroupApi = {
  list: () => api.get('/class-groups').then(res => res.data),
  get: (id) => api.get(`/class-groups/${id}`).then(res => res.data),
  getStudents: (id) => api.get(`/class-groups/${id}/students`).then(res => res.data)
}
