import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api' : '/api',
  timeout: 10000,
})

// 添加 token 请求拦截器
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  res => res.data,
  err => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const goalApi = {
  list(params = {}) {
    return http.get('/goals', { params })
  },
  stats() {
    return http.get('/goals/stats')
  },
  get(id) {
    return http.get(`/goals/${id}`)
  },
  create(data) {
    return http.post('/goals', data)
  },
  update(id, data) {
    return http.put(`/goals/${id}`, data)
  },
  remove(id) {
    return http.delete(`/goals/${id}`)
  },
  loadSubGoals(parentId, depth = 2) {
    return http.get('/goals/sub-goals', { params: { parentId, depth } })
  },
  listMy() {
    return http.get('/goals/my')
  },
  updateMyProgress(id, data) {
    return http.put(`/goals/${id}/my-progress`, data)
  },

  // 评论（公开）
  getComments(goalId) {
    return http.get(`/goals/${goalId}/comments`)
  },
  addComment(goalId, data) {
    return http.post(`/goals/${goalId}/comments`, data)
  },
  updateComment(goalId, commentId, data) {
    return http.put(`/goals/${goalId}/comments/${commentId}`, data)
  },
  deleteComment(goalId, commentId) {
    return http.delete(`/goals/${goalId}/comments/${commentId}`)
  },
  // 私密评论
  getPrivateComments(goalId) {
    return http.get(`/goals/${goalId}/private-comments`)
  },
  addPrivateComment(goalId, data) {
    return http.post(`/goals/${goalId}/comments`, { ...data, visibility: 'PRIVATE_TO_STUDENT' })
  },
  updatePrivateComment(goalId, commentId, data) {
    return http.put(`/goals/${goalId}/comments/${commentId}`, { ...data, visibility: 'PRIVATE_TO_STUDENT' })
  },
  deletePrivateComment(goalId, commentId) {
    return http.delete(`/goals/${goalId}/comments/${commentId}`)
  },

  // 学生概览（老师）
  getStudentOverview(goalId) {
    return http.get(`/goals/${goalId}/student-overview`)
  },

  // 关联作业
  getAssignments(goalId) {
    return http.get(`/goals/${goalId}/assignments`)
  },
  updateAssignments(goalId, assignmentIds) {
    return http.put(`/goals/${goalId}/assignments`, { assignmentIds })
  },

  // 文件上传：返回 { url, originalName }
  async uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    const result = await http.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    // 拼完整 URL
    if (result && result.url) {
      if (result.url.startsWith('/')) {
        result.url = baseURL + result.url
      }
    }
    // 确保 originalName 不为空
    if (result && !result.originalName) {
      result.originalName = file.name || 'file'
    }
    return result
  },
}

export { baseURL }