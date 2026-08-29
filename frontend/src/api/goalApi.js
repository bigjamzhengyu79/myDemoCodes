import axios from 'axios'
import { attachColdStartTracking } from './coldStart'

// 后端部署在 Render 免费实例上，闲置后会休眠。冷启动实测需要 90 秒以上
// （整个 Spring Boot 应用重新拉起），预热后普通请求在 1 秒内返回。
// 原先的 10 秒超时在冷启动窗口内必然失败 —— 请求根本没到达后端就被 axios 断开。
const REQUEST_TIMEOUT = 90000

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api' : '/api',
  timeout: REQUEST_TIMEOUT,
})

// 必须在下面那个 res => res.data 之前挂载：
// 响应拦截器按注册顺序执行，data 一旦被解包出来就拿不到 res.config 了。
attachColdStartTracking(http)

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
    // 超时要能被调用方识别：客户端放弃等待 ≠ 服务端没执行成功。
    // 乐观更新的回滚逻辑必须区分这两种情况，否则会把已经写入的改动回滚掉。
    const isTimeout = err.code === 'ECONNABORTED' || err.code === 'ETIMEDOUT'
    const msg = isTimeout
      ? '服务响应超时。后端可能正在启动，请稍后刷新页面确认结果'
      : (err.response?.data?.message || err.message || '请求失败')

    const error = new Error(msg)
    error.isTimeout = isTimeout
    error.status = err.response?.status
    return Promise.reject(error)
  }
)

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const goalApi = {
  list(params = {}) {
    return http.get('/goals', { params })
  },
  listCopyable() {
    return http.get('/goals/copyable')
  },
  toggleCopyable(id, copyable) {
    return http.patch(`/goals/${id}/copyable`, { copyable })
  },
  /** 复制目标树（父目标 + 所有子目标递归） */
  copyGoal(id) {
    return http.post(`/goals/${id}/copy`)
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