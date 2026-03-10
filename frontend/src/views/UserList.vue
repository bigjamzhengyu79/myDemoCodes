<template>
  <div class="user-list">
    <div class="header">
      <h1>用户管理</h1>
      <button @click="showAddForm = !showAddForm" class="btn btn-add">
        {{ showAddForm ? '取消' : '添加用户' }}
      </button>
    </div>

    <!-- 添加用户表单 -->
    <div v-if="showAddForm" class="form-card">
      <h2>添加新用户</h2>
      <form @submit.prevent="addUser">
        <div class="form-group">
          <label for="username">用户名</label>
          <input 
            v-model="newUser.username" 
            type="text" 
            id="username" 
            required
          >
        </div>
        <div class="form-group">
          <label for="email">邮箱</label>
          <input 
            v-model="newUser.email" 
            type="email" 
            id="email" 
            required
          >
        </div>
        <div class="form-group">
          <label for="password">密码</label>
          <input 
            v-model="newUser.password" 
            type="password" 
            id="password" 
            required
          >
        </div>
        <button type="submit" class="btn btn-primary">提交</button>
      </form>
    </div>

    <!-- 提示信息 -->
    <div v-if="message" :class="['message', message.type]">
      {{ message.text }}
    </div>

    <!-- 用户列表 -->
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="users.length === 0" class="empty">暂无用户</div>
    <div v-else class="users-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>邮箱</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.email }}</td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td>
              <button @click="deleteUser(user.id)" class="btn btn-delete">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import axios from 'axios'

export default {
  name: 'UserList',
  setup() {
    const users = ref([])
    const loading = ref(true)
    const showAddForm = ref(false)
    const message = ref(null)
    const newUser = ref({
      username: '',
      email: '',
      password: ''
    })

    const fetchUsers = async () => {
      try {
        loading.value = true
        const response = await axios.get('/api/users')
        users.value = response.data
      } catch (error) {
        console.error('获取用户列表失败:', error)
        message.value = { type: 'error', text: '获取用户列表失败' }
      } finally {
        loading.value = false
      }
    }

    const addUser = async () => {
      try {
        const response = await axios.post('/api/users', {
          ...newUser.value,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString()
        })
        users.value.push(response.data)
        newUser.value = { username: '', email: '', password: '' }
        showAddForm.value = false
        message.value = { type: 'success', text: '用户添加成功' }
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        console.error('添加用户失败:', error)
        message.value = { type: 'error', text: '添加用户失败' }
      }
    }

    const deleteUser = async (id) => {
      if (confirm('确定要删除这个用户吗？')) {
        try {
          await axios.delete(`/api/users/${id}`)
          users.value = users.value.filter(u => u.id !== id)
          message.value = { type: 'success', text: '用户删除成功' }
          setTimeout(() => { message.value = null }, 3000)
        } catch (error) {
          console.error('删除用户失败:', error)
          message.value = { type: 'error', text: '删除用户失败' }
        }
      }
    }

    const formatDate = (dateString) => {
      if (!dateString) return '-'
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN')
    }

    onMounted(() => {
      fetchUsers()
    })

    return {
      users,
      loading,
      showAddForm,
      message,
      newUser,
      addUser,
      deleteUser,
      formatDate
    }
  }
}
</script>

<style scoped>
.user-list {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

h1 {
  color: #667eea;
  margin: 0;
}

.btn {
  padding: 0.6rem 1.5rem;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
  transition: all 0.3s;
}

.btn-add {
  background-color: #667eea;
  color: white;
}

.btn-add:hover {
  background-color: #764ba2;
}

.btn-primary {
  background-color: #667eea;
  color: white;
}

.btn-primary:hover {
  background-color: #764ba2;
}

.btn-delete {
  background-color: #f56565;
  color: white;
  padding: 0.4rem 1rem;
  font-size: 0.9rem;
}

.btn-delete:hover {
  background-color: #e53e3e;
}

.form-card {
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 1.5rem;
  margin-bottom: 2rem;
  border: 2px solid #667eea;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  color: #333;
  font-weight: bold;
}

.form-group input {
  width: 100%;
  padding: 0.6rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.message {
  padding: 1rem;
  border-radius: 6px;
  margin-bottom: 1rem;
  margin-top: 1rem;
}

.message.success {
  background-color: #c6f6d5;
  color: #22543d;
  border: 1px solid #9ae6b4;
}

.message.error {
  background-color: #fed7d7;
  color: #742a2a;
  border: 1px solid #fc8181;
}

.loading,
.empty {
  text-align: center;
  padding: 2rem;
  color: #666;
  font-size: 1.1rem;
}

.users-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 1rem;
}

th {
  background-color: #f8f9fa;
  padding: 1rem;
  text-align: left;
  border-bottom: 2px solid #667eea;
  color: #333;
  font-weight: bold;
}

td {
  padding: 1rem;
  border-bottom: 1px solid #ddd;
}

tr:hover {
  background-color: #f8f9fa;
}
</style>
