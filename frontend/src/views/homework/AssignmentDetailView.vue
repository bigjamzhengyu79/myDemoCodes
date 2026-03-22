<template>
  <div class="page">
    <div class="flex gap-2 mb-3" style="align-items:flex-start">
      <button class="btn btn-sm" style="margin-top:3px" @click="$router.back()">← 返回</button>
      <div style="flex:1">
        <h1 style="font-size:18px;font-weight:600">{{ assignment?.title }}</h1>
        <div class="text-muted text-sm" style="margin-top:2px">
          {{ assignment?.className }} · {{ assignment?.questionCount }} 题
          <span v-if="assignment?.dueTime"> · 截止 {{ fmtDate(assignment?.dueTime) }}</span>
        </div>
      </div>
      <span v-if="assignment" :class="['badge', statusBadge(assignment.status)]" style="font-size:13px">
        {{ statusLabel(assignment.status) }}
      </span>
    </div>

    <!-- Stats cards -->
    <div v-if="stats" class="stats-grid mb-3">
      <div class="stat-card card">
        <div class="stat-num">{{ stats.submitted }}</div>
        <div class="stat-label">已提交人数</div>
      </div>
      <div class="stat-card card">
        <div class="stat-num" style="color:var(--c-success)">{{ stats.reviewed }}</div>
        <div class="stat-label">已批改份数</div>
      </div>
      <div class="stat-card card">
        <div class="stat-num" style="color:var(--c-warning)">{{ stats.pending }}</div>
        <div class="stat-label">待批改份数</div>
      </div>
      <div class="stat-card card">
        <div class="stat-num">{{ stats.avgScore }}</div>
        <div class="stat-label">平均得分</div>
      </div>
    </div>

    <!-- Answers table -->
    <div class="card" style="padding:0;overflow:hidden">
      <div style="padding:14px 16px;border-bottom:1px solid var(--c-border);font-weight:500;font-size:14px">
        学生答题情况
      </div>
      <div v-if="loading" style="text-align:center;padding:40px"><div class="spinner" style="margin:auto"></div></div>
      <table v-else class="table">
        <thead>
          <tr>
            <th>学生</th>
            <th>题目</th>
            <th style="width:80px">题型</th>
            <th style="width:80px">状态</th>
            <th style="width:80px">得分</th>
            <th>批改反馈</th>
            <th style="width:90px">提交时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="a in answers" :key="a.id">
            <td style="font-weight:500">{{ a.studentName }}</td>
            <td class="text-muted text-sm" style="max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
              {{ a.questionTitle }}
            </td>
            <td><span :class="['badge', typeBadge(a.questionType)]">{{ typeLabel(a.questionType) }}</span></td>
            <td>
              <span :class="['badge', statusAnswerBadge(a.status)]">{{ statusAnswerLabel(a.status) }}</span>
            </td>
            <td>
              <span v-if="a.score !== null" style="font-weight:500"
                    :style="{color: a.score >= a.totalScore ? 'var(--c-success)' : a.score > 0 ? 'var(--c-warning)' : 'var(--c-danger)'}">
                {{ a.score }}/{{ a.totalScore }}
              </span>
              <span v-else class="text-muted">-</span>
            </td>
            <td class="text-muted text-sm">{{ a.feedback || '-' }}</td>
            <td class="text-muted text-sm">{{ fmtTime(a.submittedAt) }}</td>
          </tr>
          <tr v-if="answers.length === 0">
            <td colspan="7" style="text-align:center;padding:32px;color:var(--c-text3)">暂无学生提交</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { assignmentApi, answerApi } from '@/api'

const route = useRoute()
const id = Number(route.params.id)
const assignment = ref(null)
const answers = ref([])
const loading = ref(true)

const stats = computed(() => {
  if (!answers.value.length) return null
  const submitted = new Set(answers.value.map(a => a.studentId)).size
  const reviewed = answers.value.filter(a => a.status === 'REVIEWED').length
  const pending = answers.value.filter(a => a.status === 'SUBMITTED').length
  const scored = answers.value.filter(a => a.score !== null)
  const avgScore = scored.length ? Math.round(scored.reduce((s, a) => s + a.score, 0) / scored.length * 10) / 10 : '-'
  return { submitted, reviewed, pending, avgScore }
})

function typeLabel(t) { return { SINGLE_CHOICE: '单选', FILL_BLANK: '填空', OPEN_ENDED: '解答' }[t] || t }
function typeBadge(t) { return { SINGLE_CHOICE: 'badge-blue', FILL_BLANK: 'badge-purple', OPEN_ENDED: 'badge-amber' }[t] || '' }
function statusLabel(s) { return { DRAFT: '草稿', PUBLISHED: '进行中', CLOSED: '已结束' }[s] || s }
function statusBadge(s) { return { DRAFT: 'badge-gray', PUBLISHED: 'badge-green', CLOSED: 'badge-red' }[s] || '' }
function statusAnswerLabel(s) { return { SUBMITTED: '待批改', AUTO_GRADED: '已自动批', REVIEWED: '已批改' }[s] || s }
function statusAnswerBadge(s) { return { SUBMITTED: 'badge-amber', AUTO_GRADED: 'badge-blue', REVIEWED: 'badge-green' }[s] || '' }
function fmtDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) : '' }
function fmtTime(d) { return d ? new Date(d).toLocaleTimeString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '' }

onMounted(async () => {
  try {
    const [aRes, ansRes] = await Promise.all([assignmentApi.get(id), answerApi.list(id)])
    if (aRes.success) assignment.value = aRes.data
    if (ansRes.success) answers.value = ansRes.data || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.stat-card { padding: 14px 16px; }
.stat-num { font-size: 26px; font-weight: 600; }
.stat-label { font-size: 12px; color: var(--c-text3); margin-top: 2px; }
</style>
