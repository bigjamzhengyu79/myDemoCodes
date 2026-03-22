import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  const isAuthenticated = () => !!token.value
  const isTeacher = () => user.value?.role === 'TEACHER'

  function setAuthData(tokenValue, userData) {
    token.value = tokenValue
    user.value = userData
    localStorage.setItem('token', tokenValue)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  function clearAuthData() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  async function login(username, password) {
    const resp = await authApi.login(username, password)
    if (!resp.success) {
      throw new Error(resp.message || '登录失败')
    }
    setAuthData(resp.data.token, {
      id: resp.data.userId,
      username: resp.data.username,
      realName: resp.data.realName,
      role: resp.data.role,
      className: resp.data.className
    })
    return { success: true }
  }

  async function logout() {
    clearAuthData()
  }

  return {
    token,
    user,
    isAuthenticated,
    isTeacher,
    login,
    logout,
    setAuthData,
    clearAuthData
  }
})
