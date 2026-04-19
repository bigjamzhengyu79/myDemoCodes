<template>
  <div class="page">
    <div v-if="loading" style="text-align:center;padding:80px"><div class="spinner" style="margin:auto"></div></div>
    <template v-else-if="assignment">
      <!-- Header -->
      <div class="flex-between mb-3">
        <div>
          <button class="btn btn-sm" style="margin-bottom:8px" @click="$router.back()">← 返回</button>
          <h1 style="font-size:18px;font-weight:600">{{ assignment.title }}</h1>
          <div class="text-muted text-sm" style="margin-top:2px">
            共 {{ assignment.questions.length }} 题 · 截止 {{ fmtDate(assignment.dueTime) }}
            <span class="status-badge">{{ assignmentStatusLabel }}</span>
          </div>
        </div>
        <div class="progress-summary">
          <div class="prog-num">{{ answeredCount }}/{{ assignment.questions.length }}</div>
          <div class="text-sm text-muted">已作答</div>
        </div>
      </div>

      <!-- Progress bar -->
      <div style="height:4px;background:var(--c-border);border-radius:2px;margin-bottom:24px;overflow:hidden">
        <div style="height:100%;background:var(--c-primary);border-radius:2px;transition:width .3s"
             :style="{width: (answeredCount/assignment.questions.length*100)+'%'}"></div>
      </div>

      <!-- Questions -->
      <div v-for="(q, idx) in assignment.questions" :key="q.id" class="q-wrapper card mb-2">
        <!-- Question header -->
        <div class="q-header">
          <div class="q-num">{{ idx + 1 }}</div>
          <span :class="['badge', typeBadge(q.questionType)]">{{ typeLabel(q.questionType) }}</span>
          <span class="text-sm text-muted">{{ q.totalScore }} 分</span>
          <div style="margin-left:auto;display:flex;gap:6px">
            <span v-for="tag in q.knowledgeTags" :key="tag.id" class="badge badge-gray">{{ tag.name }}</span>
            <span class="diff-dots">
              <span v-for="i in 5" :key="i" :class="['diff-dot', i <= q.difficulty ? 'on' : '']"></span>
            </span>
          </div>
        </div>

        <!-- Question body -->
        <div class="q-body">
          <!-- Stem -->
          <div class="q-stem" v-html="renderLatex(q.contentLatex)"></div>

          <!-- 题目配图 -->
          <div v-if="parseImages(q.imageUrlsJson).length" class="q-images">
            <img v-for="(src, i) in parseImages(q.imageUrlsJson)" :key="i"
                 :src="src" :alt="`图${i+1}`" class="q-image"
                 @click="lightbox = { src, label: `图 ${i+1}` }" />
          </div>

          <!-- Lightbox -->
          <div v-if="lightbox" class="q-lightbox-mask" @click="lightbox = null">
            <div style="text-align:center" @click.stop>
              <img :src="lightbox.src" style="max-width:90vw;max-height:82vh;border-radius:8px" />
              <div style="margin-top:10px">
                <button class="btn" @click="lightbox = null">关闭</button>
              </div>
            </div>
          </div>

          <!-- Already reviewed result -->
          <div v-if="getAnswer(q.id)?.status === 'REVIEWED'" class="result-box">
            <div class="flex gap-2 mb-1">
              <span class="badge" :class="scoreColor(getAnswer(q.id))">
                得分：{{ getAnswer(q.id).score }} / {{ q.totalScore }}
              </span>
              <span v-if="getAnswer(q.id).errorType && getAnswer(q.id).errorType !== 'NONE'"
                    class="badge badge-red">{{ errorLabel(getAnswer(q.id).errorType) }}</span>
            </div>
            <div v-if="getAnswer(q.id).feedback" class="feedback-text">
              {{ getAnswer(q.id).feedback }}
            </div>
          </div>

          <!-- Auto graded result (choice/fill) -->
          <div v-else-if="getAnswer(q.id)?.status === 'AUTO_GRADED'" class="result-box auto">
            <span class="badge" :class="scoreColor(getAnswer(q.id))">
              {{ getAnswer(q.id).score > 0 ? '✓ 正确' : '✗ 错误' }}
              · {{ getAnswer(q.id).score }}/{{ q.totalScore }}
            </span>
            <span class="text-sm text-muted" style="margin-left:8px">{{ getAnswer(q.id).feedback }}</span>
          </div>

          <!-- SINGLE_CHOICE -->
          <template v-if="q.questionType === 'SINGLE_CHOICE'">
            <div v-for="opt in q.options" :key="opt.optionLabel"
                 :class="['choice-row', choiceClass(q.id, opt)]"
                 @click="selectChoice(q.id, opt.optionLabel)">
              <div class="choice-letter">{{ opt.optionLabel }}</div>
              <span v-html="renderLatex(opt.contentLatex)"></span>
            </div>
          </template>

          <!-- FILL_BLANK -->
          <template v-else-if="q.questionType === 'FILL_BLANK'">
            <div class="flex gap-2" style="margin-top:8px">
              <input v-model="drafts[q.id]" class="form-control"
                     placeholder="输入答案（如：10 或 3/2）"
                     :disabled="getAnswer(q.id) && getAnswer(q.id).status !== 'DRAFT'"
                     @keyup.enter="submitAnswer(q.id)" />
              <button v-if="!getAnswer(q.id) || getAnswer(q.id).status === 'DRAFT'" class="btn btn-primary"
                      @click="saveAnswer(q.id)" :disabled="submitting[q.id]">
                <span v-if="submitting[q.id]" class="spinner" style="width:12px;height:12px"></span>
                保存
              </button>
              <button v-if="!getAnswer(q.id) || getAnswer(q.id).status === 'DRAFT'" class="btn btn-primary"
                      @click="submitAnswer(q.id)" :disabled="submitting[q.id]">
                <span v-if="submitting[q.id]" class="spinner" style="width:12px;height:12px"></span>
                提交
              </button>
            </div>
            <div v-if="getAnswer(q.id)?.status === 'DRAFT'" class="text-sm text-muted" style="margin-top:6px">已保存草稿，可继续编辑后提交</div>
          </template>

          <!-- OPEN_ENDED -->
          <template v-else>
            <div v-if="!getAnswer(q.id) || getAnswer(q.id).status === 'DRAFT'">
              <LatexEditor v-model:content-latex="drafts[q.id]" 
                           v-model:image-urls="imageDrafts[q.id]"
                           inputLabel="解题过程"
                           class="form-control" 
                           :rows=5
                           :placeholder="`在此输入解题过程（支持LaTeX，如：$f'(x) = ...$）`"
              />
              <div style="margin-top:8px;display:flex;gap:8px;align-items:center">
                <button v-if="!getAnswer(q.id) || getAnswer(q.id).status === 'DRAFT'" class="btn btn-primary"
                        @click="saveAnswer(q.id)" :disabled="submitting[q.id]">
                  <span v-if="submitting[q.id]" class="spinner" style="width:12px;height:12px"></span>
                  保存
                </button>
                <button class="btn btn-primary" @click="submitAnswer(q.id)" :disabled="submitting[q.id]">
                  <span v-if="submitting[q.id]" class="spinner" style="width:12px;height:12px"></span>
                  提交答案
                </button>
                <span class="text-sm text-muted">解答题将由教师批改</span>
              </div>
              <div v-if="getAnswer(q.id)?.status === 'DRAFT'" class="text-sm text-muted" style="margin-top:6px">已保存草稿，可继续编辑后提交</div>
            </div>
            <div v-else class="submitted-answer">
              <div class="text-sm text-muted" style="margin-bottom:4px">已提交的答案：</div>
              <div style="white-space:pre-wrap;font-size:13px" v-html="renderLatex(getAnswer(q.id).answerContent)"></div>
              <div v-if="getAnswer(q.id).status === 'SUBMITTED'" class="pending-badge">等待教师批改...</div>
            </div>
          </template>
        </div>
      </div>
      <!--
      FormulaWidget 通过 <Teleport to="body"> 渲染到 body 顶层，
      放在这里仅用于保持在 provide 作用域内，不占据任何布局空间。
      -->
      <FormulaWidget />

      <!-- Done summary -->
      <div v-if="answeredCount === assignment.questions.length" class="done-banner card">
        <div style="font-size:16px;font-weight:500;margin-bottom:4px">🎉 全部题目已作答完毕！</div>
        <div class="text-muted text-sm">选择题和填空题已自动批改，解答题等待教师批改后显示成绩</div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { assignmentApi, answerApi } from '@/api'
import katex from 'katex'
import LatexEditor from '@/components/LatexEditor.vue'
import FormulaWidget from '@/components/FormulaWidget.vue'

const route = useRoute()
const assignmentId = Number(route.params.id)
const assignment = ref(null)
const answers = ref([])
const drafts = ref({})
const imageDrafts = ref({})
const submitting = ref({})
const loading = ref(true)
const lightbox = ref(null)

function parseImages(json) {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

const answeredCount = computed(() => {
  if (!assignment.value) return 0
  return assignment.value.questions.filter(q => getAnswer(q.id)).length
})

const assignmentStatusLabel = computed(() => {
  if (!assignment.value) return ''
  if (answeredCount.value === 0) return '未开始'
  if (answeredCount.value < assignment.value.questions.length) return '进行中'
  return '已完成'
})

function getAnswer(qid) {
  return answers.value.find(a => a.questionId === qid) || null
}

function renderLatex(text) {
  if (!text) return '<span style="color:var(--c-text3)">预览...</span>'
  
  // 用占位符保护 KaTeX 输出，防止后续 replace 破坏 HTML 结构
  const placeholders = []
  let counter = 0
  
  // 替换块级公式并保存到占位符
  text = text.replace(/\$\$([^$]+)\$\$/g, (match, m) => {
    try {
      const html = katex.renderToString(m, { displayMode: true })
      const placeholder = `__KATEX_PLACEHOLDER_${counter}__`
      placeholders.push(html)
      counter++
      return placeholder
    } catch {
      return match
    }
  })
  
  // 替换行内公式并保存到占位符
  text = text.replace(/\$([^$]+)\$/g, (match, m) => {
    try {
      const html = katex.renderToString(m, { displayMode: false })
      const placeholder = `__KATEX_PLACEHOLDER_${counter}__`
      placeholders.push(html)
      counter++
      return placeholder
    } catch {
      return match
    }
  })
  
  // 现在安全地处理换行符（不会影响 KaTeX 占位符）
  text = text.replace(/\n/g, '<br>')
  
  // 恢复所有 KaTeX 占位符
  placeholders.forEach((html, i) => {
    text = text.replace(`__KATEX_PLACEHOLDER_${i}__`, html)
  })
  
  return text
}

async function selectChoice(qid, label) {
  if (getAnswer(qid)) return
  drafts.value[qid] = label
  await submitAnswer(qid)
}

async function saveAnswer(qid) {
  const content = drafts.value[qid]
  const images = imageDrafts.value[qid] || []
  if (!content?.trim() && images.length === 0) return
  submitting.value[qid] = true
  try {
    const imageUrlJson = images.length > 0 ? JSON.stringify(images.map(img => img.dataUrl)) : null
    const res = await answerApi.submit(assignmentId, { 
      questionId: qid, 
      answerContent: content, 
      imageUrlsJson: imageUrlJson,
      saveOnly: true 
    })
    if (res.success) {
      const idx = answers.value.findIndex(a => a.questionId === qid)
      if (idx >= 0) answers.value[idx] = res.data
      else answers.value.push(res.data)
    }
  } finally {
    submitting.value[qid] = false
  }
}

async function submitAnswer(qid) {
  const content = drafts.value[qid]
  const images = imageDrafts.value[qid] || []
  if (!content?.trim() && images.length === 0) return
  submitting.value[qid] = true
  try {
    const imageUrlsJson = images.length > 0 ? JSON.stringify(images.map(img => img.dataUrl)) : null
    const res = await answerApi.submit(assignmentId, { 
      questionId: qid, 
      answerContent: content,
      imageUrlsJson: imageUrlsJson
    })
    if (res.success) {
      const idx = answers.value.findIndex(a => a.questionId === qid)
      if (idx >= 0) answers.value[idx] = res.data
      else answers.value.push(res.data)
    }
  } finally {
    submitting.value[qid] = false
  }
}

function choiceClass(qid, opt) {
  const ans = getAnswer(qid)
  if (!ans) return drafts.value[qid] === opt.optionLabel ? 'selected' : ''
  if (ans.status === 'AUTO_GRADED') {
    if (opt.isCorrect) return 'correct'
    if (ans.answerContent === opt.optionLabel && !opt.isCorrect) return 'wrong'
  }
  return ans.answerContent === opt.optionLabel ? 'selected' : ''
}

function scoreColor(ans) {
  if (!ans || ans.score === null) return 'badge-gray'
  return ans.score >= ans.totalScore ? 'badge-green' : ans.score > 0 ? 'badge-amber' : 'badge-red'
}

function typeLabel(t) { return { SINGLE_CHOICE: '单选', FILL_BLANK: '填空', OPEN_ENDED: '解答' }[t] || t }
function typeBadge(t) { return { SINGLE_CHOICE: 'badge-blue', FILL_BLANK: 'badge-purple', OPEN_ENDED: 'badge-amber' }[t] || 'badge-gray' }
function errorLabel(e) { return { CONCEPT: '概念错误', CALC: '计算失误', READING: '审题有误', NONE: '' }[e] || e }
function fmtDate(d) { return d ? new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) : '无' }

onMounted(async () => {
  try {
    const [aRes, ansRes] = await Promise.all([
      assignmentApi.get(assignmentId),
      answerApi.list(assignmentId),
    ])
    if (aRes.success) assignment.value = aRes.data
    if (ansRes.success) {
      answers.value = ansRes.data || []
      answers.value.forEach(a => {
        if (a.answerContent) drafts.value[a.questionId] = a.answerContent
        if (a.imageUrlsJson) {
          try {
            const imageUrlsJson = JSON.parse(a.imageUrlsJson)
            imageDrafts.value[a.questionId] = imageUrlsJson.map(url => ({ dataUrl: url }))
          } catch (e) {
            console.warn('Failed to parse image URLs for question', a.questionId, e)
          }
        }
      })
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.progress-summary { text-align: center; }
.prog-num { font-size: 24px; font-weight: 600; color: var(--c-primary); }
.q-wrapper { overflow: hidden; }
.q-header { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--c-surface2); border-bottom: 1px solid var(--c-border); }
.q-num { width: 22px; height: 22px; border-radius: 50%; background: var(--c-primary); color: #fff; font-size: 11px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.q-body { padding: 16px; }
.q-stem { font-size: 14px; line-height: 1.9; margin-bottom: 14px; }
.choice-row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: var(--radius-sm); border: 1px solid transparent; cursor: pointer; font-size: 13px; margin-bottom: 5px; transition: all .12s; }
.choice-row:hover { background: var(--c-surface2); }
.choice-row.selected { background: var(--c-primary-bg); border-color: var(--c-primary); }
.choice-row.correct { background: var(--c-success-bg); border-color: var(--c-success); }
.choice-row.wrong { background: var(--c-danger-bg); border-color: var(--c-danger); }
.choice-letter { width: 22px; height: 22px; border-radius: 50%; border: 1px solid var(--c-border-med); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 500; flex-shrink: 0; }
.selected .choice-letter { background: var(--c-primary); color: #fff; border-color: transparent; }
.result-box { background: var(--c-surface2); border-radius: var(--radius-sm); padding: 10px 12px; margin-bottom: 12px; border-left: 3px solid var(--c-primary); }
.result-box.auto { border-left-color: var(--c-success); }
.feedback-text { font-size: 13px; color: var(--c-text2); margin-top: 4px; }
.submitted-answer { background: var(--c-surface2); border-radius: var(--radius-sm); padding: 12px; margin-top: 8px; }
.pending-badge { margin-top: 8px; font-size: 12px; color: var(--c-warning); }
.done-banner { background: var(--c-success-bg); border-color: var(--c-success); text-align: center; padding: 24px; margin-top: 8px; }
.status-badge { display: inline-block; margin-left: 10px; padding: 2px 8px; border-radius: 999px; background: var(--c-primary-bg); color: var(--c-primary); font-size: 12px; }
.q-images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.q-image { max-width: 240px; max-height: 180px; object-fit: contain; border: 1px solid var(--c-border); border-radius: var(--radius-sm); cursor: zoom-in; transition: opacity .12s; }
.q-image:hover { opacity: .85; }
.q-lightbox-mask { position: fixed; inset: 0; background: rgba(0,0,0,.8); display: flex; align-items: center; justify-content: center; z-index: 200; }
</style>
