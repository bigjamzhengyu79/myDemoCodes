import axios from 'axios'
import { useAuthStore } from '@/store/auth'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api' : '/api',
  timeout: 10000
})

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
  publish: (id) => api.patch(`/assignments/${id}/publish`).then(wrapResponse)
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
