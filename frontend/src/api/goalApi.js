import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
})

http.interceptors.response.use(
  res => res.data,
  err => {
    const msg = err.response?.data?.message || err.message || '请求失败'
    return Promise.reject(new Error(msg))
  }
)

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
}
