<template>
  <div class="page">
    <div class="flex-between mb-3">
      <div>
        <h1 style="font-size:20px;font-weight:600">题库管理</h1>
        <div class="text-muted text-sm" style="margin-top:2px">管理所有题目，支持按知识点筛选</div>
      </div>
      <button class="btn btn-primary" @click="$router.push('/questions/new')">+ 新建题目</button>
    </div>

    <!-- Filters -->
    <div class="card card-sm mb-3 flex gap-3" style="flex-wrap:wrap">
      <div>
        <label class="form-label">题型</label>
        <select v-model="filter.type" class="form-control" style="width:120px">
          <option value="">全部</option>
          <option value="SINGLE_CHOICE">单选</option>
          <option value="FILL_BLANK">填空</option>
          <option value="OPEN_ENDED">解答</option>
        </select>
      </div>
      <div>
        <label class="form-label">难度</label>
        <select v-model="filter.difficulty" class="form-control" style="width:100px">
          <option value="">全部</option>
          <option v-for="i in 5" :key="i" :value="i">{{ '★'.repeat(i) }}</option>
        </select>
      </div>
      <div>
        <label class="form-label">知识点</label>
        <select v-model="filter.tag" class="form-control" style="width:140px">
          <option value="">全部</option>
          <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
        </select>
      </div>
      <div style="display:flex;align-items:flex-end">
        <button class="btn" @click="filter = { type:'', difficulty:'', tag:'' }">重置</button>
      </div>
    </div>

    <!-- Table -->
    <div v-if="loading" style="text-align:center;padding:60px"><div class="spinner" style="margin:auto"></div></div>
    <div v-else class="card" style="padding:0;overflow:hidden">
      <table class="table">
        <thead>
          <tr>
            <th style="width:50px">ID</th>
            <th>题目</th>
            <th style="width:80px">题型</th>
            <th style="width:80px">难度</th>
            <th style="width:60px">分值</th>
            <th>知识点</th>
            <th style="width:60px">来源</th>
            <th style="width:100px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="q in filtered" :key="q.id">
            <td class="text-muted">#{{ q.id }}</td>
            <td style="max-width:280px">
              <div style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px">
                {{ stripLatex(q.contentLatex) }}
              </div>
            </td>
            <td><span :class="['badge', typeBadge(q.questionType)]">{{ typeLabel(q.questionType) }}</span></td>
            <td>
              <div class="diff-dots">
                <span v-for="i in 5" :key="i" :class="['diff-dot', i <= q.difficulty ? 'on' : '']"></span>
              </div>
            </td>
            <td class="text-muted">{{ q.totalScore }}分</td>
            <td>
              <div style="display:flex;gap:4px;flex-wrap:wrap">
                <span v-for="t in q.knowledgeTags" :key="t.id" class="badge badge-gray">{{ t.name }}</span>
              </div>
            </td>
            <td class="text-muted text-sm">{{ q.source || '-' }}</td>
            <td>
              <div class="flex gap-2">
                <button class="btn btn-sm" @click="$router.push(`/questions/${q.id}/edit`)">编辑</button>
                <button class="btn btn-sm btn-danger" @click="confirmDelete(q)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="filtered.length === 0">
            <td colspan="8" style="text-align:center;padding:40px;color:var(--c-text3)">暂无题目</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Delete confirm -->
    <div v-if="deleteTarget" class="modal-mask" @click.self="deleteTarget = null">
      <div class="modal-box card" style="width:360px">
        <h3 style="margin-bottom:8px">确认删除</h3>
        <p class="text-muted" style="font-size:13px;margin-bottom:16px">
          确定要删除题目 #{{ deleteTarget.id }} 吗？此操作不可恢复。
        </p>
        <div class="flex gap-2" style="justify-content:flex-end">
          <button class="btn" @click="deleteTarget = null">取消</button>
          <button class="btn btn-danger" @click="doDelete">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { questionApi } from '@/api'

const questions = ref([])
const tags = ref([])
const loading = ref(true)
const filter = ref({ type: '', difficulty: '', tag: '' })
const deleteTarget = ref(null)

const filtered = computed(() => questions.value.filter(q => {
  if (filter.value.type && q.questionType !== filter.value.type) return false
  if (filter.value.difficulty && q.difficulty !== Number(filter.value.difficulty)) return false
  if (filter.value.tag && !q.knowledgeTags.some(t => t.id === Number(filter.value.tag))) return false
  return true
}))

function stripLatex(s) { return (s || '').replace(/\$[^$]*\$/g, '[公式]').replace(/\n/g, ' ') }
function typeLabel(t) { return { SINGLE_CHOICE: '单选', FILL_BLANK: '填空', OPEN_ENDED: '解答' }[t] || t }
function typeBadge(t) { return { SINGLE_CHOICE: 'badge-blue', FILL_BLANK: 'badge-purple', OPEN_ENDED: 'badge-amber' }[t] || '' }
function confirmDelete(q) { deleteTarget.value = q }
async function doDelete() {
  await questionApi.delete(deleteTarget.value.id)
  deleteTarget.value = null
  await loadQuestions()
}

async function loadQuestions() {
  const res = await questionApi.list()
  questions.value = res.data || []
}

onMounted(async () => {
  try {
    const [qRes, tRes] = await Promise.all([questionApi.list(), questionApi.getTags()])
    questions.value = qRes.data || []
    tags.value = tRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-box { max-height: 90vh; overflow-y: auto; }
</style>
