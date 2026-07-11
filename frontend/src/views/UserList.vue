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
        <div class="form-row">
          <div class="form-group">
            <label for="username">用户名</label>
            <input v-model="newUser.username" type="text" id="username" required />
          </div>
          <div class="form-group">
            <label for="realName">真实姓名</label>
            <input v-model="newUser.realName" type="text" id="realName" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label for="email">邮箱</label>
            <input v-model="newUser.email" type="email" id="email" />
          </div>
          <div class="form-group">
            <label for="password">密码</label>
            <input v-model="newUser.password" type="password" id="password" required />
          </div>
        </div>
        <div class="form-group">
          <label for="role">角色</label>
          <select v-model="newUser.role" id="role">
            <option value="STUDENT">学生</option>
            <option value="TEACHER">教师</option>
            <option value="ADMIN">管理员</option>
          </select>
        </div>
        <button type="submit" class="btn btn-primary">提交</button>
      </form>
    </div>

    <!-- 编辑用户表单 -->
    <div v-if="showEditForm" class="form-card">
      <h2>编辑用户</h2>
      <form @submit.prevent="updateUser">
        <div class="form-row">
          <div class="form-group">
            <label for="edit-username">用户名</label>
            <input v-model="editUser.username" type="text" id="edit-username" required />
          </div>
          <div class="form-group">
            <label for="edit-realName">真实姓名</label>
            <input v-model="editUser.realName" type="text" id="edit-realName" />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label for="edit-email">邮箱</label>
            <input v-model="editUser.email" type="email" id="edit-email" />
          </div>
          <div class="form-group">
            <label for="edit-role">角色</label>
            <select v-model="editUser.role" id="edit-role">
              <option value="STUDENT">学生</option>
              <option value="TEACHER">教师</option>
              <option value="ADMIN">管理员</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <label for="edit-password">新密码(留空则不修改)</label>
          <input v-model="editUser.password" type="password" id="edit-password" />
        </div>
        <button type="submit" class="btn btn-primary">保存</button>
        <button type="button" class="btn btn-cancel" @click="showEditForm = false">取消</button>
      </form>
    </div>

    <!-- 班级管理面板 -->
    <div v-if="showClassPanel" class="form-card">
      <h2>班级管理 - {{ classPanelUserName }}</h2>
      <div class="class-list">
        <h3>已加入的班级 ({{ userClassGroups.length }})</h3>
        <div v-if="userClassGroups.length === 0" class="empty-small">暂未加入班级</div>
        <div v-for="cg in userClassGroups" :key="cg.id" class="class-item">
          <span>{{ cg.name }}</span>
          <button @click="removeFromClass(cg.id)" class="btn btn-small-danger">移除</button>
        </div>
      </div>
      <div class="add-class">
        <h3>加入班级</h3>
        <div class="form-row-inline">
          <select v-model="newClassGroupId">
            <option :value="null" disabled>-- 选择班级 --</option>
            <option v-for="cg in availableClassGroups" :key="cg.id" :value="cg.id">
              {{ cg.name }}
            </option>
          </select>
          <button @click="addToClass" class="btn btn-primary" :disabled="!newClassGroupId">加入</button>
        </div>
      </div>
      <button type="button" class="btn btn-cancel" @click="showClassPanel = false">关闭</button>
    </div>

    <div v-if="message" :class="['message', message.type]">{{ message.text }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="users.length === 0" class="empty">暂无用户</div>
    <div v-else class="users-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>用户名</th>
            <th>真实姓名</th>
            <th>角色</th>
            <th>所属班级</th>
            <th>邮箱</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.id">
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td>{{ user.realName || '-' }}</td>
            <td><span :class="['role-tag', user.role]">{{ roleLabel(user.role) }}</span></td>
            <td>
              <div v-if="user.classNames && user.classNames.length > 0" class="class-tags">
                <span v-for="cn in user.classNames" :key="cn" class="class-tag">{{ cn }}</span>
              </div>
              <span v-else class="text-muted">-</span>
            </td>
            <td>{{ user.email || '-' }}</td>
            <td>
              <button @click="openClassPanel(user)" class="btn btn-sm btn-info">管理班级</button>
              <button @click="startEdit(user)" class="btn btn-edit">编辑</button>
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

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ? import.meta.env.VITE_API_BASE_URL + '/api' : '/api',
  timeout: 10000,
})
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default {
  name: 'UserList',
  setup() {
    const users = ref([])
    const loading = ref(true)
    const showAddForm = ref(false)
    const showEditForm = ref(false)
    const showClassPanel = ref(false)
    const message = ref(null)

    const newUser = ref({ username: '', email: '', password: '', realName: '', role: 'STUDENT' })
    const editUser = ref({ id: null, username: '', email: '', realName: '', role: 'STUDENT', password: '' })

    // 班级管理
    const classPanelUserId = ref(null)
    const classPanelUserName = ref('')
    const userClassGroups = ref([])
    const allClassGroups = ref([])
    const newClassGroupId = ref(null)

    const availableClassGroups = ref([])

    const fetchUsers = async () => {
      try {
        loading.value = true
        const resp = await http.get('/users')
        users.value = resp.data
      } catch (error) {
        message.value = { type: 'error', text: '获取用户列表失败' }
      } finally {
        loading.value = false
      }
    }

    const fetchAllClassGroups = async () => {
      try {
        const resp = await http.get('/class-groups')
        allClassGroups.value = resp.data
      } catch (e) {
        // ignore
      }
    }

    const addUser = async () => {
      try {
        await http.post('/users', newUser.value)
        newUser.value = { username: '', email: '', password: '', realName: '', role: 'STUDENT' }
        showAddForm.value = false
        message.value = { type: 'success', text: '用户添加成功' }
        await fetchUsers()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '添加用户失败' }
      }
    }

    const startEdit = (user) => {
      editUser.value = { ...user, password: '' }
      showEditForm.value = true
      showAddForm.value = false
    }

    const updateUser = async () => {
      try {
        const payload = { ...editUser.value }
        if (!payload.password) delete payload.password
        await http.put(`/users/${payload.id}`, payload)
        showEditForm.value = false
        message.value = { type: 'success', text: '用户更新成功' }
        await fetchUsers()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '更新用户失败' }
      }
    }

    const deleteUser = async (id) => {
      if (confirm('确定要删除这个用户吗？')) {
        try {
          await http.delete(`/users/${id}`)
          users.value = users.value.filter(u => u.id !== id)
          message.value = { type: 'success', text: '删除成功' }
          setTimeout(() => { message.value = null }, 3000)
        } catch (error) {
          message.value = { type: 'error', text: '删除失败' }
        }
      }
    }

    // 班级管理
    const openClassPanel = async (user) => {
      classPanelUserId.value = user.id
      classPanelUserName.value = user.realName || user.username
      showClassPanel.value = true
      showAddForm.value = false
      showEditForm.value = false
      newClassGroupId.value = null

      await Promise.all([
        fetchUserClassGroups(user.id),
        fetchAllClassGroups()
      ])
      // 过滤已加入的班级
      const existingIds = userClassGroups.value.map(cg => cg.id)
      availableClassGroups.value = allClassGroups.value.filter(cg => !existingIds.includes(cg.id))
    }

    const fetchUserClassGroups = async (userId) => {
      try {
        const resp = await http.get(`/class-groups/by-student/${userId}`)
        userClassGroups.value = resp.data
      } catch (e) {
        userClassGroups.value = []
      }
    }

    const addToClass = async () => {
      if (!newClassGroupId.value) return
      try {
        await http.post(`/class-groups/${newClassGroupId.value}/students`, {
          studentId: classPanelUserId.value
        })
        message.value = { type: 'success', text: '已加入班级' }
        // 刷新班级管理面板 + 用户主列表
        await Promise.all([
          openClassPanel({ id: classPanelUserId.value, name: classPanelUserName.value }),
          fetchUsers()
        ])
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '加入班级失败' }
      }
    }

    const removeFromClass = async (classGroupId) => {
      if (!confirm('确定从该班级移除该用户吗？')) return
      try {
        await http.delete(`/class-groups/${classGroupId}/students/${classPanelUserId.value}`)
        message.value = { type: 'success', text: '已从班级移除' }
        // 刷新班级管理面板 + 用户主列表
        await Promise.all([
          openClassPanel({ id: classPanelUserId.value, name: classPanelUserName.value }),
          fetchUsers()
        ])
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '移除失败' }
      }
    }

    const roleLabel = (role) => {
      const map = { ADMIN: '管理员', TEACHER: '教师', STUDENT: '学生' }
      return map[role] || role
    }

    onMounted(() => { fetchUsers() })

    return {
      users, loading, showAddForm, showEditForm, showClassPanel, message,
      newUser, editUser,
      addUser, startEdit, updateUser, deleteUser, roleLabel,
      classPanelUserName, userClassGroups, availableClassGroups, newClassGroupId,
      openClassPanel, addToClass, removeFromClass
    }
  }
}
</script>

<style scoped>
.user-list { background: white; border-radius: 12px; padding: 2rem; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 2rem; }
h1 { color: #667eea; margin: 0; }
h2 { color: #667eea; margin-top: 0; }
h3 { margin: 0.5rem 0; font-size: 1rem; color: #333; }
.btn { padding: 0.6rem 1.5rem; border: none; border-radius: 6px; cursor: pointer; font-weight: bold; transition: all 0.3s; }
.btn-add { background-color: #667eea; color: white; }
.btn-add:hover { background-color: #764ba2; }
.btn-primary { background-color: #667eea; color: white; margin-right: 8px; }
.btn-primary:hover { background-color: #764ba2; }
.btn-primary:disabled { background-color: #a0a0a0; cursor: not-allowed; }
.btn-cancel { background-color: #e2e8f0; color: #333; }
.btn-edit { background-color: #48bb78; color: white; padding: 0.4rem 0.8rem; font-size: 0.85rem; margin-right: 4px; }
.btn-delete { background-color: #f56565; color: white; padding: 0.4rem 0.8rem; font-size: 0.85rem; }
.btn-sm { padding: 0.3rem 0.7rem; font-size: 0.8rem; border: none; border-radius: 4px; cursor: pointer; }
.btn-info { background-color: #4299e1; color: white; }
.btn-small-danger { background-color: #f56565; color: white; border: none; border-radius: 4px; padding: 0.2rem 0.6rem; cursor: pointer; font-size: 0.8rem; }
.form-card { background-color: #f8f9fa; border-radius: 8px; padding: 1.5rem; margin-bottom: 2rem; border: 2px solid #667eea; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; color: #333; font-weight: bold; font-size: 0.9rem; }
.form-group input, .form-group select { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.95rem; }
.form-group input:focus, .form-group select:focus { outline: none; border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.1); }
.form-row-inline { display: flex; gap: 0.5rem; align-items: center; }
.form-row-inline select { flex: 1; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.95rem; }
.class-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.class-tag { display: inline-block; background: #e8f4fd; color: #2b6cb0; padding: 2px 8px; border-radius: 12px; font-size: 0.8rem; font-weight: 500; white-space: nowrap; }
.text-muted { color: #999; }
.message { padding: 1rem; border-radius: 6px; margin-bottom: 1rem; }
.message.success { background-color: #c6f6d5; color: #22543d; border: 1px solid #9ae6b4; }
.message.error { background-color: #fed7d7; color: #742a2a; border: 1px solid #fc8181; }
.loading, .empty { text-align: center; padding: 2rem; color: #666; }
.empty-small { text-align: center; padding: 0.5rem; color: #999; font-size: 0.9rem; }
.users-table { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th { background-color: #f8f9fa; padding: 0.8rem; text-align: left; border-bottom: 2px solid #667eea; font-weight: bold; font-size: 0.9rem; }
td { padding: 0.8rem; border-bottom: 1px solid #ddd; font-size: 0.9rem; }
tr:hover { background-color: #f8f9fa; }
.role-tag { padding: 2px 8px; border-radius: 4px; font-size: 0.8rem; font-weight: bold; }
.role-tag.ADMIN { background: #fed7e2; color: #c53030; }
.role-tag.TEACHER { background: #c6f6d5; color: #22543d; }
.role-tag.STUDENT { background: #e2e8f0; color: #4a5568; }
.class-list { margin-bottom: 1.5rem; }
.class-item { display: flex; justify-content: space-between; align-items: center; padding: 0.5rem; border-bottom: 1px solid #eee; }
.class-item:hover { background-color: #f0f0f0; }
.add-class { padding-top: 0.5rem; border-top: 1px solid #ddd; }
</style>