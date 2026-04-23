<template>
  <div id="app" class="app-container">
    <nav class="navbar">
      <div class="nav-brand">Vue 3 + Spring Boot</div>
      <ul class="nav-links">
        <li><router-link to="/">首页</router-link></li>
        <li><router-link to="/users">用户管理</router-link></li>
        <li><router-link to="/goals">🎯 目标管理</router-link></li>
        <li><router-link to="/unit-test">🧪 单元测试</router-link></li>
        <li><router-link to="/assignments">✏️ 作业系统</router-link></li>
        <li><router-link to="/login">登录</router-link></li>
      </ul>
    </nav>
    <main class="main-content">
      <router-view></router-view>
    </main>
  </div>
</template>

<script>
import { provide } from 'vue'
import { createFormulaContext, FORMULA_KEY } from '@/composables/useFormulaContext'

export default {
  name: 'App',
  setup() {
    const formulaCtx = createFormulaContext()
    provide(FORMULA_KEY, formulaCtx)
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
</style>
