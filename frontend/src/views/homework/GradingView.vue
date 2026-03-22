<template>
  <div class="page">
    <div class="flex-between mb-3">
      <div>
        <h1 style="font-size:20px;font-weight:600">批改作业</h1>
        <div class="text-muted text-sm" style="margin-top:2px">待批改解答题 · 点击题目开始批改</div>
      </div>
      <div class="flex gap-2">
        <div class="stat-mini">
          <div class="stat-mini-num">{{ pending.length }}</div>
          <div class="text-sm text-muted">待批改</div>
        </div>
        <div class="stat-mini">
          <div class="stat-mini-num" style="color:var(--c-success)">{{ gradedToday }}</div>
          <div class="text-sm text-muted">今日已批</div>
        </div>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1fr 380px;gap:16px">
      <!-- Left: pending list -->
      <div>
        <div v-if="loading" style="text-align:center;padding:60px"><div class="spinner" style="margin:auto"></div></div>
        <div v-else-if="pending.length === 0" class="card" style="text-align:center;padding:60px;color:var(--c-text3)">
          <div style="font-size:32px;margin-bottom:8px">✓</div>
          暂无待批改的解答题
        </div>
        <div v-else>
          <div v-for="a in pending" :key="a.id"
               :class="['answer-item card mb-2', selected?.id === a.id ? 'selected' : '']"
               @click="selectAnswer(a)">
            <div class="flex-between mb-1">
              <div class="flex gap-2">
                <span class="badge badge-amber">解答题</span>
                <span class="badge badge-gray">#{{ a.questionId }}</span>
              </div>
              <span class="text-sm text-muted">{{ fmtTime(a.submittedAt) }}</span>
            </div>
            <div style="font-size:13px;font-weight:500;margin-bottom:4px">{{ a.questionTitle }}</div>
            <div style="font-size:13px;color:var(--c-text2)">{{ a.studentName }}</div>
            <div v-if="a.autoScore !== null" style="margin-top:6px">
              <span class="badge badge-blue">AI建议分: {{ a.autoScore }}/{{ a.totalScore }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: grading panel -->
      <div>
        <div v-if="!selected" class="card" style="text-align:center;padding:40px;color:var(--c-text3)">
          点击左侧题目开始批改
        </div>
        <div v-else class="card">
          <div class="flex-between mb-2">
            <h3 style="font-size:14px;font-weight:600">批改面板</h3>
            <span class="badge badge-amber">{{ selected.studentName }}</span>
          </div>

          <!-- Question stem -->
          <div style="margin-bottom:12px">
            <div class="form-label">题目</div>
            <div class="grading-stem" v-html="renderLatex(questionDetail?.contentLatex || selected.questionTitle)"></div>
          </div>

          <!-- Solution steps reference -->
          <div v-if="questionDetail?.solutionSteps?.length" style="margin-bottom:12px">
            <div class="form-label">参考解析</div>
            <div class="solution-box">
              <div v-for="(step, i) in questionDetail.solutionSteps" :key="i" class="solution-step">
                <div class="flex-between">
                  <span class="step-badge">步骤 {{ step.stepOrder }}</span>
                  <span class="badge badge-blue">{{ step.stepScore }}分</span>
                </div>
                <div class="step-content" v-html="renderLatex(step.contentLatex)"></div>
                <div v-if="step.commonErrors" class="step-error">⚠ {{ step.commonErrors }}</div>
              </div>
            </div>
          </div>

          <!-- Student answer -->
          <div style="margin-bottom:14px">
            <div class="form-label">学生答案</div>
            <div class="student-answer-box" v-html="renderLatex(selected.answerContent || '（未作答）')"></div>
          </div>

          <!-- Grade form -->
          <div class="form-group">
            <label class="form-label">评分（满分 {{ selected.totalScore }} 分）</label>
            <div class="flex gap-2" style="align-items:center">
              <input v-model.number="gradeForm.score" class="form-control" type="number"
                     :min="0" :max="selected.totalScore" style="width:80px" />
              <span class="text-muted">/ {{ selected.totalScore }}</span>
              <div class="score-quick-btns">
                <button class="btn btn-sm" @click="gradeForm.score = 0">0</button>
                <button class="btn btn-sm" @click="gradeForm.score = Math.floor(selected.totalScore/2)">
                  {{ Math.floor(selected.totalScore/2) }}
                </button>
                <button class="btn btn-sm btn-success" @click="gradeForm.score = selected.totalScore">
                  满分
                </button>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">错误类型</label>
            <select v-model="gradeForm.errorType" class="form-control">
              <option value="NONE">无</option>
              <option value="CONCEPT">概念理解错误</option>
              <option value="CALC">计算失误</option>
              <option value="READING">审题有误</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">批改反馈（展示给学生）</label>
            <textarea v-model="gradeForm.feedback" class="form-control" rows="3"
                      placeholder="如：步骤一正确，步骤二求导时漏考虑定义域..."></textarea>
          </div>

          <button class="btn btn-primary" style="width:100%;justify-content:center;padding:10px"
                  @click="submitGrade" :disabled="grading">
            <span v-if="grading" class="spinner" style="width:12px;height:12px"></span>
            {{ grading ? '提交中...' : '提交批改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { gradingApi, questionApi } from '@/api'
import katex from 'katex'

const pending = ref([])
const selected = ref(null)
const questionDetail = ref(null)
const loading = ref(true)
const grading = ref(false)
const gradedToday = ref(0)
const gradeForm = ref({ score: 0, feedback: '', errorType: 'NONE' })

function renderLatex(text) {
  if (!text) return ''
  return text.replace(/\$\$([^$]+)\$\$/g, (_, m) => {
    try { return katex.renderToString(m, { displayMode: true }) } catch { return m }
  }).replace(/\$([^$]+)\$/g, (_, m) => {
    try { return katex.renderToString(m, { displayMode: false }) } catch { return m }
  }).replace(/\n/g, '<br>')
}

async function selectAnswer(a) {
  selected.value = a
  gradeForm.value = { score: a.autoScore ?? 0, feedback: '', errorType: 'NONE' }
  try {
    const res = await questionApi.get(a.questionId)
    if (res.success) questionDetail.value = res.data
  } catch {}
}

async function submitGrade() {
  grading.value = true
  try {
    await gradingApi.grade({
      answerId: selected.value.id,
      score: gradeForm.value.score,
      feedback: gradeForm.value.feedback,
      errorType: gradeForm.value.errorType,
    })
    pending.value = pending.value.filter(a => a.id !== selected.value.id)
    gradedToday.value++
    selected.value = null
  } finally {
    grading.value = false
  }
}

function fmtTime(d) {
  return new Date(d).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  try {
    const res = await gradingApi.pending()
    pending.value = (res.data || []).filter(a => a.questionType === 'OPEN_ENDED')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.stat-mini { text-align: center; min-width: 60px; }
.stat-mini-num { font-size: 24px; font-weight: 600; }
.answer-item { cursor: pointer; transition: all .12s; border-left: 3px solid transparent; }
.answer-item:hover { border-left-color: var(--c-primary); }
.answer-item.selected { border-left-color: var(--c-primary); background: var(--c-primary-bg); }
.grading-stem { font-size: 13px; line-height: 1.8; padding: 10px; background: var(--c-surface2); border-radius: var(--radius-sm); }
.solution-box { border: 1px solid var(--c-border); border-radius: var(--radius-sm); overflow: hidden; }
.solution-step { padding: 10px 12px; border-bottom: 1px solid var(--c-border); }
.solution-step:last-child { border-bottom: none; }
.step-badge { font-size: 11px; font-weight: 500; color: var(--c-text2); }
.step-content { font-size: 13px; line-height: 1.7; margin: 4px 0; }
.step-error { font-size: 11px; color: var(--c-warning); margin-top: 2px; }
.student-answer-box { background: var(--c-surface2); border-radius: var(--radius-sm); padding: 10px 12px; font-size: 13px; line-height: 1.8; min-height: 60px; white-space: pre-wrap; }
.score-quick-btns { display: flex; gap: 4px; }
</style>
