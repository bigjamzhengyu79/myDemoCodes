<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="login-logo">
        <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
          <rect width="32" height="32" rx="8" fill="#2563eb"/>
          <text x="16" y="22" text-anchor="middle" fill="white" font-size="16" font-weight="600" font-family="serif">∑</text>
        </svg>
        <span>高中数学习题系统</span>
      </div>
      <h2 class="login-title">登录</h2>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label class="form-label">用户名</label>
          <input v-model="form.username" class="form-control" placeholder="teacher01 / student01" required />
        </div>
        <div class="form-group">
          <label class="form-label">密码</label>
          <input v-model="form.password" class="form-control" type="password" placeholder="123456" required />
        </div>
        <div v-if="error" class="login-error">{{ error }}</div>
        <button class="btn btn-primary" style="width:100%;justify-content:center;padding:10px" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:14px;height:14px"></span>
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
      <div class="login-hint">
        <div>演示账号：</div>
        <div>教师：teacher01 / 123456</div>
        <div>学生：student01 / 123456</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const auth = useAuthStore()
const form = ref({ username: '', password: '' })
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  try {
    const res = await auth.login(form.value.username, form.value.password)
    if (res.success) router.push('/')
    else error.value = res.message || '登录失败'
  } catch (e) {
    error.value = typeof e === 'string' ? e : '登录失败，请检查网络'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--c-bg); }
.login-card { width: 380px; background: var(--c-surface); border: 1px solid var(--c-border); border-radius: 14px; padding: 36px 32px; box-shadow: var(--shadow-md); }
.login-logo { display: flex; align-items: center; gap: 10px; font-weight: 600; font-size: 15px; margin-bottom: 24px; }
.login-title { font-size: 20px; font-weight: 600; margin-bottom: 20px; }
.login-error { background: var(--c-danger-bg); color: var(--c-danger); border-radius: var(--radius-sm); padding: 8px 12px; font-size: 13px; margin-bottom: 12px; }
.login-hint { margin-top: 20px; padding: 12px; background: var(--c-surface2); border-radius: var(--radius-sm); font-size: 12px; color: var(--c-text3); line-height: 1.8; }
</style>
