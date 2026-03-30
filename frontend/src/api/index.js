import axios from 'axios'
import { useAuthStore } from '@/store/auth'

console.log('API BASE URL:', import.meta.env.VITE_API_BASE_URL)
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api' : '/api',  // ✅ 读取环境变量
  //baseURL: '/api',  // ✅ 读取环境变量
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
  get: (id) => api.get(`/questions/${id}`).then(wrapResponse),
  create: (payload) => api.post('/questions', payload).then(wrapResponse),
  update: (id, payload) => api.put(`/questions/${id}`, payload).then(wrapResponse),
  delete: (id) => api.delete(`/questions/${id}`).then(wrapResponse),
  getTags: () => api.get('/questions/tags').then(wrapResponse)
}

export const gradingApi = {
  pending: () => api.get('/grading/pending').then(wrapResponse),
  grade: (payload) => api.post('/grading/grade', payload).then(wrapResponse)
}
