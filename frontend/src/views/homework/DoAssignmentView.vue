<template>
  <div class="page">
    <div v-if="loading" style="text-align:center;padding:80px"><div class="spinner" style="margin:auto"></div></div>
    <template v-else-if="assignment">
      <!-- Header -->
      <div class="flex-between mb-3" style="position:relative">
        <div>
          <button v-if="canGoBack" class="btn btn-ghost btn-back" @click="goBack">← 返回</button>
          <h1 style="font-size:18px;font-weight:600;display:inline-block;vertical-align:middle">{{ assignment.title }}</h1>
          <div class="text-muted text-sm" style="margin-top:2px">
            共 {{ assignment.questions.length }} 题 · 截止 {{ fmtDate(assignment.dueTime) }}
            <span class="status-badge">{{ assignmentStatusLabel }}</span>
          </div>
        </div>
        <div class="header-right">
          <button class="btn btn-sm" @click="exportPdf">导出 PDF</button>
          <div class="progress-summary">
            <div class="prog-num">{{ answeredCount }}/{{ assignment.questions.length }}</div>
            <div class="text-sm text-muted">已作答</div>
          </div>
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
          <!-- 收藏到错题本。刻意不看对错 —— 需求是"做过的题都能存"，做对的题同样可收藏。
               单独占一格并紧跟分值，不放进右侧那组标签里：那里挤着知识点徽章和难度点，
               纯图标按钮混在其中会被当成装饰，学生找不到（实测反馈）。 -->
          <button class="btn-star" :class="{ on: collected.has(q.id) }"
                  :disabled="starring[q.id]"
                  :title="collected.has(q.id) ? '已在错题本，点击移出' : '加入错题本'"
                  @click="toggleCollect(q.id)">
            <span class="star-icon">{{ collected.has(q.id) ? '★' : '☆' }}</span>
          </button>
          <div style="margin-left:auto;display:flex;gap:6px;align-items:center">
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
import { useRoute, useRouter } from 'vue-router'
import { assignmentApi, answerApi, mistakeApi } from '@/api'
import katex from 'katex'
import LatexEditor from '@/components/LatexEditor.vue'
import FormulaWidget from '@/components/FormulaWidget.vue'

const route = useRoute()
const router = useRouter()

const canGoBack = computed(() => {
  // 当浏览器历史记录多于1条时显示返回按钮
  // 或者有 referrer 来源时也显示
  return window.history.length > 1
})

function goBack() {
  router.back()
}
const assignmentId = Number(route.params.id)

// 在新标签页打开打印/导出 PDF 页面（默认含抬头与留白、不含答案，可在打印页切换）
function exportPdf() { window.open(`/assignments/${assignmentId}/print`, '_blank') }
const assignment = ref(null)
const answers = ref([])
const drafts = ref({})
const imageDrafts = ref({})
const submitting = ref({})
const loading = ref(true)
const lightbox = ref(null)

// 错题本收藏状态。Set 存的是已收藏的 questionId，页面加载后批量回填一次。
const collected = ref(new Set())
const starring = ref({})

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

/**
 * 加入 / 移出错题本。
 * 乐观更新 + 失败回滚：星标要立刻响应点击，全应用没有 toast，
 * 失败时唯一的反馈就是星标弹回原状。
 *
 * 注意这里不判断对错 —— 错题本是"做过习题的保存"，学生想收哪题就收哪题。
 */
async function toggleCollect(qid) {
  if (starring.value[qid]) return
  const wasCollected = collected.value.has(qid)
  // Set 是引用类型，必须换新对象才能触发 Vue 的响应式更新
  const next = new Set(collected.value)
  wasCollected ? next.delete(qid) : next.add(qid)
  collected.value = next
  starring.value[qid] = true
  try {
    if (wasCollected) {
      await mistakeApi.remove(qid)
    } else {
      await mistakeApi.add({ questionId: qid, sourceAssignmentId: assignmentId })
    }
  } catch (e) {
    const rollback = new Set(collected.value)
    wasCollected ? rollback.add(qid) : rollback.delete(qid)
    collected.value = rollback
  } finally {
    starring.value[qid] = false
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

    // 批量回填错题本星标。单独 try —— 收藏状态取不到不该影响整个做题页，
    // 未登录时该接口返回 403（/api/mistakes/** 不在 permitAll 里）。
    if (assignment.value?.questions?.length) {
      try {
        const ids = assignment.value.questions.map(q => q.id)
        const cRes = await mistakeApi.collected(ids)
        if (cRes.success) collected.value = new Set(cRes.data || [])
      } catch (e) {
        console.warn('错题本收藏状态加载失败', e)
      }
    }
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.header-right { display: flex; align-items: center; gap: 14px; }
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
.btn-back { margin-right: 8px; vertical-align: middle; }
/* 收藏按钮：做成有边框的胶囊，而不是一个裸图标。
   最初是浅灰无边框的纯 ☆，和旁边的难度圆点一样是灰色小符号，学生反馈找不到。
   现在【未收藏态也用琥珀色】—— 不用 --c-text3 那类中性灰：
   灰色在这一行里与「5 分」「知识点标签」「难度点」完全同色，无论加不加边框都会被
   当成静态信息读过去。用色彩把它从信息里区分出来，才是它显眼的真正原因。
   两态的区别改由「描边 / 填充」承担，而不是「有色 / 无色」。 */
.btn-star {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 27px; padding: 0; margin-left: 2px;
  background: var(--c-warning-bg); border: 1.5px solid var(--c-warning);
  border-radius: 999px; cursor: pointer; line-height: 1;
  color: var(--c-warning);
  transition: background .14s, border-color .14s, color .14s, transform .1s, box-shadow .14s;
}
.btn-star .star-icon { font-size: 16px; transform: translateY(-.5px); }
.btn-star:hover:not(:disabled) {
  background: var(--c-warning); color: #fff; transform: scale(1.1);
  box-shadow: 0 2px 6px rgba(217, 119, 6, .4);
}
.btn-star:active:not(:disabled) { transform: scale(.94); }
.btn-star:focus-visible { outline: 2px solid var(--c-primary); outline-offset: 2px; }
/* 已收藏：整块填充，与未收藏的描边态形成明确的开/关对比 */
.btn-star.on {
  background: var(--c-warning); border-color: var(--c-warning); color: #fff;
  box-shadow: 0 1px 3px rgba(217, 119, 6, .35);
}
.btn-star.on:hover:not(:disabled) { background: var(--c-warning); border-color: var(--c-warning); color: #fff; }
.btn-star:disabled { cursor: default; opacity: .55; }
.q-images { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.q-image { max-width: 240px; max-height: 180px; object-fit: contain; border: 1px solid var(--c-border); border-radius: var(--radius-sm); cursor: zoom-in; transition: opacity .12s; }
.q-image:hover { opacity: .85; }
.q-lightbox-mask { position: fixed; inset: 0; background: rgba(0,0,0,.8); display: flex; align-items: center; justify-content: center; z-index: 200; }
</style>
