<template>
  <div id="app" class="app-container">
    <nav class="navbar">
      <div class="nav-brand">Vue 3 + Spring Boot</div>
      <ul class="nav-links">
        <li><router-link to="/">首页</router-link></li>
        <li><router-link to="/users">用户管理</router-link></li>
        <li><router-link to="/goals">🎯 目标管理</router-link></li>
        <li v-if="showUnitTest"><router-link to="/unit-test">🧪 单元测试</router-link></li>
        <li><router-link to="/assignments">✏️ 作业系统</router-link></li>
        <li><router-link to="/login">登录</router-link></li>
      </ul>
    </nav>
    <div v-if="isColdStarting" class="cold-start-banner">
      <span class="cold-start-spinner"></span>
      服务正在启动中，首次访问约需 1 分钟，请稍候…
    </div>
    <main class="main-content">
      <router-view></router-view>
    </main>
  </div>
</template>

<script>
import { provide } from 'vue'
import { createFormulaContext, FORMULA_KEY } from '@/composables/useFormulaContext'
import { isColdStarting } from '@/api/coldStart'

const showUnitTest = import.meta.env.VITE_SHOW_UNIT_TEST !== 'false'

export default {
  name: 'App',
  setup() {
    const formulaCtx = createFormulaContext()
    provide(FORMULA_KEY, formulaCtx)
    return { showUnitTest, isColdStarting }
  }
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #333;
}

.navbar {
  background-color: rgba(0, 0, 0, 0.8);
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.nav-brand {
  font-size: 1.5rem;
  font-weight: bold;
}

.nav-links {
  display: flex;
  list-style: none;
  gap: 2rem;
}

.nav-links a {
  color: white;
  text-decoration: none;
  transition: color 0.3s;
}

.nav-links a:hover {
  color: #667eea;
}

.main-content {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.cold-start-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.6rem;
  padding: 0.7rem 1rem;
  background-color: #FEF3C7;
  color: #92400E;
  font-size: 0.9rem;
  border-bottom: 1px solid #FCD34D;
}

.cold-start-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(146, 64, 14, 0.25);
  border-top-color: #92400E;
  border-radius: 50%;
  animation: cold-start-spin 0.8s linear infinite;
}

@keyframes cold-start-spin {
  to { transform: rotate(360deg); }
}
</style>