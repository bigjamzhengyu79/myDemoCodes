<template>
  <div class="class-group-list">
    <div class="header">
      <h1>班级管理</h1>
      <button v-if="isAdmin" @click="showAddForm = !showAddForm" class="btn btn-add">
        {{ showAddForm ? '取消' : '添加班级' }}
      </button>
    </div>

    <div v-if="!isAdmin" class="admin-notice">您没有管理员权限，无法进行班级管理操作。</div>

    <div v-if="isAdmin && showAddForm" class="form-card">
      <h2>添加新班级</h2>
      <form @submit.prevent="addClassGroup">
        <div class="form-group">
          <label for="name">班级名称</label>
          <input v-model="newClassGroup.name" type="text" id="name" required />
        </div>
        <div class="form-group">
          <label for="description">描述</label>
          <input v-model="newClassGroup.description" type="text" id="description" />
        </div>
        <button type="submit" class="btn btn-primary">提交</button>
      </form>
    </div>

    <div v-if="isAdmin && showEditForm" class="form-card">
      <h2>编辑班级</h2>
      <form @submit.prevent="updateClassGroup">
        <div class="form-group">
          <label for="edit-name">班级名称</label>
          <input v-model="editClassGroup.name" type="text" id="edit-name" required />
        </div>
        <div class="form-group">
          <label for="edit-description">描述</label>
          <input v-model="editClassGroup.description" type="text" id="edit-description" />
        </div>
        <button type="submit" class="btn btn-primary">保存</button>
        <button type="button" class="btn btn-cancel" @click="showEditForm = false">取消</button>
      </form>
    </div>

    <!-- 设置负责老师 -->
    <div v-if="isAdmin && showTeacherForm" class="form-card">
      <h2>设置负责老师 - {{ teacherFormGroupName }}</h2>
      <form @submit.prevent="setTeacher">
        <div class="form-group">
          <label for="teacher">选择老师</label>
          <select v-model="teacherForm.teacherId" id="teacher">
            <option :value="null">-- 不指定老师 --</option>
            <option v-for="t in teachers" :key="t.id" :value="t.id">
              {{ t.realName || t.username }}
            </option>
          </select>
        </div>
        <button type="submit" class="btn btn-primary">保存</button>
        <button type="button" class="btn btn-cancel" @click="showTeacherForm = false">取消</button>
      </form>
    </div>

    <!-- 学生管理 -->
    <div v-if="isAdmin && showStudentsPanel" class="form-card">
      <h2>学生管理 - {{ studentsPanelGroupName }}</h2>

      <div class="student-list">
        <h3>当前学生 ({{ students.length }})</h3>
        <div v-if="students.length === 0" class="empty-small">暂无学生</div>
        <div v-for="s in students" :key="s.id" class="student-item">
          <span>{{ s.realName || s.username }}</span>
          <button @click="removeStudent(s.id)" class="btn btn-small-danger">移除</button>
        </div>
      </div>

      <div class="add-student">
        <h3>添加学生</h3>
        <div class="form-row-inline">
          <select v-model="newStudentId">
            <option :value="null" disabled>-- 选择学生 --</option>
            <option v-for="s in availableStudents" :key="s.id" :value="s.id">
              {{ s.realName || s.username }}
            </option>
          </select>
          <button @click="addStudent" class="btn btn-primary" :disabled="!newStudentId">添加</button>
        </div>
      </div>

      <button type="button" class="btn btn-cancel" @click="showStudentsPanel = false">关闭</button>
    </div>

    <div v-if="message" :class="['message', message.type]">{{ message.text }}</div>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="classGroups.length === 0" class="empty">暂无班级</div>
    <div v-else class="groups-table">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>班级名称</th>
            <th>描述</th>
            <th>负责老师</th>
            <th>学生数</th>
            <th>创建时间</th>
            <th v-if="isAdmin">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="group in classGroups" :key="group.id">
            <td>{{ group.id }}</td>
            <td>{{ group.name }}</td>
            <td>{{ group.description || '-' }}</td>
            <td>{{ group.teacher ? (group.teacher.realName || group.teacher.username) : '-' }}</td>
            <td>{{ group.students ? group.students.length : 0 }}</td>
            <td>{{ formatDate(group.createdAt) }}</td>
            <td v-if="isAdmin">
              <button @click="openTeacherForm(group)" class="btn btn-edit">设置老师</button>
              <button @click="openStudentsPanel(group)" class="btn btn-info">学生</button>
              <button @click="startEdit(group)" class="btn btn-edit">编辑</button>
              <button @click="deleteClassGroup(group.id)" class="btn btn-delete">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  timeout: 10000,
})
http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default {
  name: 'ClassGroupList',
  setup() {
    const classGroups = ref([])
    const loading = ref(true)
    const showAddForm = ref(false)
    const showEditForm = ref(false)
    const showTeacherForm = ref(false)
    const showStudentsPanel = ref(false)
    const message = ref(null)
    const newClassGroup = ref({ name: '', description: '' })
    const editClassGroup = ref({ id: null, name: '', description: '' })

    // 老师管理
    const teachers = ref([])
    const teacherForm = ref({ classGroupId: null, teacherId: null })
    const teacherFormGroupName = ref('')

    // 学生管理
    const students = ref([])
    const availableStudents = ref([])
    const newStudentId = ref(null)
    const studentsPanelGroupId = ref(null)
    const studentsPanelGroupName = ref('')

    const isAdmin = computed(() => {
      try {
        const userStr = localStorage.getItem('user')
        if (userStr) {
          const user = JSON.parse(userStr)
          return user.role === 'ADMIN'
        }
      } catch (e) {}
      return false
    })

    const fetchClassGroups = async () => {
      try {
        loading.value = true
        const response = await http.get('/class-groups')
        classGroups.value = response.data
      } catch (error) {
        message.value = { type: 'error', text: '获取班级列表失败' }
      } finally {
        loading.value = false
      }
    }

    const fetchTeachers = async () => {
      try {
        const resp = await http.get('/users')
        teachers.value = resp.data.filter(u => u.role === 'TEACHER' || u.role === 'ADMIN')
      } catch (e) {
        // ignore
      }
    }

    const fetchStudents = async () => {
      try {
        const resp = await http.get('/users')
        // 班级中已存在的学生不能选，所以在打开学生管理面板时过滤
        availableStudents.value = resp.data.filter(u => u.role === 'STUDENT')
      } catch (e) {
        // ignore
      }
    }

    const addClassGroup = async () => {
      try {
        await http.post('/class-groups', newClassGroup.value)
        newClassGroup.value = { name: '', description: '' }
        showAddForm.value = false
        message.value = { type: 'success', text: '班级添加成功' }
        await fetchClassGroups()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '添加失败' }
      }
    }

    const startEdit = (group) => {
      editClassGroup.value = { id: group.id, name: group.name, description: group.description }
      showEditForm.value = true
      showAddForm.value = false
    }

    const updateClassGroup = async () => {
      try {
        await http.put(`/class-groups/${editClassGroup.value.id}`, editClassGroup.value)
        showEditForm.value = false
        message.value = { type: 'success', text: '班级更新成功' }
        await fetchClassGroups()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '更新失败' }
      }
    }

    const deleteClassGroup = async (id) => {
      if (confirm('确定要删除这个班级吗？')) {
        try {
          await http.delete(`/class-groups/${id}`)
          classGroups.value = classGroups.value.filter(g => g.id !== id)
          message.value = { type: 'success', text: '删除成功' }
          setTimeout(() => { message.value = null }, 3000)
        } catch (error) {
          message.value = { type: 'error', text: '删除失败' }
        }
      }
    }

    // 老师管理
    const openTeacherForm = (group) => {
      teacherForm.value = { classGroupId: group.id, teacherId: group.teacher ? group.teacher.id : null }
      teacherFormGroupName.value = group.name
      showTeacherForm.value = true
      showAddForm.value = false
      showEditForm.value = false
      showStudentsPanel.value = false
      fetchTeachers()
    }

    const setTeacher = async () => {
      try {
        await http.put(`/class-groups/${teacherForm.value.classGroupId}/teacher`, {
          teacherId: teacherForm.value.teacherId
        })
        showTeacherForm.value = false
        message.value = { type: 'success', text: '老师设置成功' }
        await fetchClassGroups()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '设置失败' }
      }
    }

    // 学生管理
    const openStudentsPanel = async (group) => {
      studentsPanelGroupId.value = group.id
      studentsPanelGroupName.value = group.name
      showStudentsPanel.value = true
      showAddForm.value = false
      showEditForm.value = false
      showTeacherForm.value = false
      newStudentId.value = null
      await Promise.all([
        fetchGroupStudents(group.id),
        fetchStudents()
      ])
      // 过滤掉已在班级中的学生
      const existingIds = students.value.map(s => s.id)
      availableStudents.value = availableStudents.value.filter(s => !existingIds.includes(s.id))
    }

    const fetchGroupStudents = async (groupId) => {
      try {
        const resp = await http.get(`/class-groups/${groupId}/students`)
        students.value = resp.data
      } catch (e) {
        students.value = []
      }
    }

    const addStudent = async () => {
      if (!newStudentId.value) return
      try {
        await http.post(`/class-groups/${studentsPanelGroupId.value}/students`, {
          studentId: newStudentId.value
        })
        message.value = { type: 'success', text: '学生添加成功' }
        // 刷新学生列表
        await openStudentsPanel({ id: studentsPanelGroupId.value, name: studentsPanelGroupName.value })
        await fetchClassGroups()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '添加失败' }
      }
    }

    const removeStudent = async (studentId) => {
      if (!confirm('确定要移除该学生吗？')) return
      try {
        await http.delete(`/class-groups/${studentsPanelGroupId.value}/students/${studentId}`)
        message.value = { type: 'success', text: '学生移除成功' }
        await openStudentsPanel({ id: studentsPanelGroupId.value, name: studentsPanelGroupName.value })
        await fetchClassGroups()
        setTimeout(() => { message.value = null }, 3000)
      } catch (error) {
        message.value = { type: 'error', text: error.response?.data || '移除失败' }
      }
    }

    const formatDate = (dateString) => {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString('zh-CN')
    }

    onMounted(() => { fetchClassGroups() })

    return {
      classGroups, loading, showAddForm, showEditForm, showTeacherForm, showStudentsPanel,
      message, newClassGroup, editClassGroup, isAdmin,
      addClassGroup, startEdit, updateClassGroup, deleteClassGroup, formatDate,
      teachers, teacherForm, teacherFormGroupName, openTeacherForm, setTeacher,
      students, availableStudents, newStudentId, studentsPanelGroupName,
      openStudentsPanel, addStudent, removeStudent
    }
  }
}
</script>

<style scoped>
.class-group-list { background: white; border-radius: 12px; padding: 2rem; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }
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
.btn-info { background-color: #4299e1; color: white; padding: 0.4rem 0.8rem; font-size: 0.85rem; margin-right: 4px; }
.btn-small-danger { background-color: #f56565; color: white; border: none; border-radius: 4px; padding: 0.2rem 0.6rem; cursor: pointer; font-size: 0.8rem; }
.form-card { background-color: #f8f9fa; border-radius: 8px; padding: 1.5rem; margin-bottom: 2rem; border: 2px solid #667eea; }
.form-group { margin-bottom: 1rem; }
.form-group label { display: block; margin-bottom: 0.5rem; color: #333; font-weight: bold; }
.form-group input, .form-group select { width: 100%; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 1rem; }
.form-group input:focus, .form-group select:focus { outline: none; border-color: #667eea; box-shadow: 0 0 0 3px rgba(102,126,234,0.1); }
.form-row-inline { display: flex; gap: 0.5rem; align-items: center; }
.form-row-inline select { flex: 1; padding: 0.6rem; border: 1px solid #ddd; border-radius: 4px; font-size: 0.95rem; }
.message { padding: 1rem; border-radius: 6px; margin-bottom: 1rem; }
.message.success { background-color: #c6f6d5; color: #22543d; border: 1px solid #9ae6b4; }
.message.error { background-color: #fed7d7; color: #742a2a; border: 1px solid #fc8181; }
.loading, .empty { text-align: center; padding: 2rem; color: #666; }
.empty-small { text-align: center; padding: 0.5rem; color: #999; font-size: 0.9rem; }
.admin-notice { background-color: #fff3cd; color: #856404; padding: 1rem; border-radius: 6px; margin-bottom: 1rem; border: 1px solid #ffc107; text-align: center; }
table { width: 100%; border-collapse: collapse; margin-top: 1rem; }
th { background-color: #f8f9fa; padding: 1rem; text-align: left; border-bottom: 2px solid #667eea; font-weight: bold; }
td { padding: 1rem; border-bottom: 1px solid #ddd; }
tr:hover { background-color: #f8f9fa; }
.student-list { margin-bottom: 1.5rem; }
.student-item { display: flex; justify-content: space-between; align-items: center; padding: 0.5rem; border-bottom: 1px solid #eee; }
.student-item:hover { background-color: #f0f0f0; }
.add-student { padding-top: 0.5rem; border-top: 1px solid #ddd; }
</style>