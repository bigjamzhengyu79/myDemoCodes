<template>
  <div class="home">
    <div class="welcome-card">
      <h1>欢迎来到Vue 3 + Spring Boot应用</h1>
      <p>这是一个全栈示例项目</p>
      
      <div class="tech-stack">
        <h2>技术栈</h2>
        <ul>
          <li>✨ 前端: Vue 3 + Vite</li>
          <li>🚀 后端: Spring Boot 3</li>
          <li>🗄️ 数据库: MySQL / PostgreSQL</li>
          <li>📦 API: RESTful</li>
        </ul>
      </div>

      <div class="quick-links">
        <h2>快速开始</h2>
        <router-link to="/users" class="btn btn-primary">用户管理</router-link>
      </div>

      <div class="api-status">
        <h2>服务状态</h2>
        <p id="status">检查中...</p>
      </div>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import axios from 'axios'

export default {
  name: 'Home',
  setup() {
    const status = ref('检查中...')

    onMounted(async () => {
      try {
        const response = await axios.get('/api/users/health')
        status.value = '✅ 后端服务运行正常'
      } catch (error) {
        status.value = '❌ 后端服务未连接'
      }
    })

    return { status }
  }
}
</script>

<style scoped>
.home {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}

.welcome-card {
  background: white;
  border-radius: 12px;
  padding: 3rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
  max-width: 600px;
  width: 100%;
}

h1 {
  color: #667eea;
  margin-bottom: 1rem;
  text-align: center;
  font-size: 2rem;
}

h2 {
  color: #764ba2;
  margin-top: 2rem;
  margin-bottom: 1rem;
  font-size: 1.3rem;
}

p {
  text-align: center;
  color: #666;
  margin-bottom: 2rem;
}

.tech-stack ul {
  list-style: none;
  padding: 0;
}

.tech-stack li {
  padding: 0.5rem 0;
  color: #555;
  font-size: 1rem;
}

.quick-links {
  text-align: center;
  margin-top: 2rem;
}

.btn {
  display: inline-block;
  padding: 0.75rem 2rem;
  border-radius: 6px;
  text-decoration: none;
  font-weight: bold;
  transition: all 0.3s;
  cursor: pointer;
}

.btn-primary {
  background-color: #667eea;
  color: white;
}

.btn-primary:hover {
  background-color: #764ba2;
  transform: translateY(-2px);
}

.api-status {
  margin-top: 2rem;
  padding: 1rem;
  background-color: #f5f5f5;
  border-radius: 6px;
}

#status {
  margin: 0;
  text-align: center;
  font-weight: bold;
}
</style>
