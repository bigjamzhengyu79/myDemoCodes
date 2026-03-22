<template>
  <div class="page">
    <div class="flex-between mb-3">
      <div>
        <h1 style="font-size:20px;font-weight:600">{{ auth.isTeacher() ? '作业管理' : '我的作业' }}</h1>
        <div class="text-muted text-sm" style="margin-top:2px">{{ auth.isTeacher() ? '创建和管理班级作业' : '查看并完成作业' }}</div>
      </div>
      <button v-if="auth.isTeacher()" class="btn btn-primary" @click="showCreate = true">
        + 新建作业
      </button>
    </div>

    <!-- Stats row -->
    <div v-if="auth.isTeacher()" class="stats-row mb-3">
      <div class="stat-card">
        <div class="stat-num">{{ assignments.length }}</div>
        <div class="stat-label">全部作业</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:var(--c-success)">{{ published }}</div>
        <div class="stat-label">已发布</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color:var(--c-text3)">{{ draft }}</div>
        <div class="stat-label">草稿</div>
      </div>
    </div>

    <!-- List -->
    <div v-if="loading" style="text-align:center;padding:60px"><div class="spinner" style="margin:auto"></div></div>
    <div v-else-if="assignments.length === 0" class="card" style="text-align:center;padding:60px;color:var(--c-text3)">
      暂无作业
    </div>
    <div v-else class="assignment-grid">
      <div v-for="a in assignments" :key="a.id" class="assignment-card card"
           @click="$router.push(auth.isTeacher() ? `/assignments/${a.id}` : `/assignments/${a.id}/do`)">
        <div class="flex-between mb-1">
          <span :class="['badge', statusBadge(a.status)]">{{ statusLabel(a.status) }}</span>
          <span class="text-sm text-muted">{{ a.questionCount }} 题</span>
        </div>
        <div style="font-size:15px;font-weight:500;margin-bottom:4px">{{ a.title }}</div>
        <div class="text-sm text-muted" style="margin-bottom:10px">{{ a.description }}</div>
        <hr class="divider" style="margin:10px 0">
        <div class="flex-between text-sm text-muted">
          <span>班级：{{ a.className }}</span>
          <span v-if="a.dueTime">截止 {{ fmtDate(a.dueTime) }}</span>
        </div>
        <div v-if="auth.isTeacher() && a.status === 'DRAFT'" style="margin-top:10px">
          <button class="btn btn-sm btn-success" @click.stop="publishAssignment(a.id)">发布作业</button>
        </div>
      </div>
    </div>

    <!-- Create modal -->
    <div v-if="showCreate" class="modal-mask" @click.self="showCreate = false">
      <div class="modal-box card">
        <h3 style="margin-bottom:16px;font-size:16px">新建作业</h3>
        <div class="form-group">
          <label class="form-label">作业标题</label>
          <input v-model="form.title" class="form-control" placeholder="如：导数专项练习 #2" />
        </div>
        <div class="form-group">
          <label class="form-label">说明</label>
          <textarea v-model="form.description" class="form-control" rows="2" placeholder="作业说明（可选）"></textarea>
        </div>
        <div class="form-group">
          <label class="form-label">班级</label>
          <input v-model="form.className" class="form-control" placeholder="如：高三(1)班" />
        </div>
        <div class="form-group">
          <label class="form-label">截止时间</label>
          <input v-model="form.dueTime" class="form-control" type="datetime-local" />
        </div>
        <div class="form-group">
          <label class="form-label">选择题目（ID，逗号分隔）</label>
          <input v-model="questionIds" class="form-control" placeholder="如：1,2,3" />
        </div>
        <div class="flex gap-2" style="justify-content:flex-end;margin-top:8px">
          <button class="btn" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" @click="createAssignment">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAuthStore } from '@/store/auth'
import { assignmentApi } from '@/api'

const auth = useAuthStore()
const assignments = ref([])
const loading = ref(true)
const showCreate = ref(false)
const form = ref({ title: '', description: '', className: '', dueTime: '' })
const questionIds = ref('1,2,3')

const published = computed(() => assignments.value.filter(a => a.status === 'PUBLISHED').length)
const draft = computed(() => assignments.value.filter(a => a.status === 'DRAFT').length)

async function load() {
  try { assignments.value = (await assignmentApi.list()).data || [] }
  finally { loading.value = false }
}

async function createAssignment() {
  const ids = questionIds.value.split(',').map(s => parseInt(s.trim())).filter(Boolean)
  await assignmentApi.create({ ...form.value, questionIds: ids, dueTime: form.value.dueTime || null })
  showCreate.value = false
  form.value = { title: '', description: '', className: '', dueTime: '' }
  await load()
}

async function publishAssignment(id) {
  await assignmentApi.publish(id)
  await load()
}

function statusLabel(s) { return { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已结束' }[s] || s }
function statusBadge(s) { return { DRAFT: 'badge-gray', PUBLISHED: 'badge-green', CLOSED: 'badge-red' }[s] || 'badge-gray' }
function fmtDate(d) { return new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) }

onMounted(load)
</script>

<style scoped>
.stats-row { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.stat-card { background: var(--c-surface); border: 1px solid var(--c-border); border-radius: var(--radius); padding: 16px; }
.stat-num { font-size: 28px; font-weight: 600; }
.stat-label { font-size: 12px; color: var(--c-text3); margin-top: 2px; }
.assignment-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 14px; }
.assignment-card { cursor: pointer; transition: box-shadow .15s, transform .15s; }
.assignment-card:hover { box-shadow: var(--shadow-md); transform: translateY(-1px); }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-box { width: 480px; max-height: 90vh; overflow-y: auto; }
</style>
