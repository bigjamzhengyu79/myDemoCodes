<template>
  <div class="paper-root">
    <!-- 屏幕可见、打印时隐藏的工具条 -->
    <div class="toolbar no-print">
      <button class="btn btn-sm" @click="closeWindow">关闭</button>
      <div class="toolbar-title">导出 PDF · {{ assignment?.title || '' }}</div>
      <div class="toolbar-opts">
        <label><input type="checkbox" v-model="opts.header" /> 抬头</label>
        <label><input type="checkbox" v-model="opts.blank" /> 留白作答区</label>
        <label><input type="checkbox" v-model="opts.answers" /> 参考答案/解析</label>
      </div>
      <button class="btn btn-sm btn-primary" @click="doPrint" :disabled="loading || !assignment">
        打印 / 另存为 PDF
      </button>
    </div>

    <div v-if="loading" class="loading no-print">
      <div class="spinner"></div>
    </div>
    <div v-else-if="error" class="loading no-print">{{ error }}</div>

    <!-- 打印纸张 -->
    <div v-else-if="assignment" class="paper">
      <!-- 抬头 -->
      <header v-if="opts.header" class="paper-header">
        <h1 class="paper-title">{{ assignment.title }}</h1>
        <div class="paper-meta">
          <span v-if="assignment.classGroupName">班级：{{ assignment.classGroupName }}</span>
          <span>共 {{ questions.length }} 题</span>
          <span>满分 {{ totalScore }} 分</span>
          <span v-if="assignment.dueTime">截止 {{ fmtDate(assignment.dueTime) }}</span>
        </div>
        <div v-if="assignment.description" class="paper-desc">{{ assignment.description }}</div>
        <div class="paper-fillin">
          <span>姓名：____________</span>
          <span>班级：____________</span>
          <span>学号：____________</span>
          <span>日期：____________</span>
        </div>
        <hr class="paper-rule" />
      </header>

      <!-- 题目区 -->
      <section class="questions">
        <article v-for="(q, idx) in questions" :key="q.id" class="q-block">
          <div class="q-line">
            <span class="q-no">{{ idx + 1 }}.</span>
            <span class="q-type">[{{ typeLabel(q.questionType) }}]</span>
            <span class="q-score">（{{ q.totalScore }} 分）</span>
            <span v-for="tag in q.knowledgeTags" :key="tag.id" class="q-tag">{{ tag.name }}</span>
          </div>

          <!-- 题干 -->
          <div class="q-stem" v-html="renderLatex(q.contentLatex)"></div>

          <!-- 配图 -->
          <div v-if="parseImages(q.imageUrlsJson).length" class="q-images">
            <img v-for="(src, i) in parseImages(q.imageUrlsJson)" :key="i" :src="src" :alt="`图${i + 1}`" />
          </div>

          <!-- 单选题选项 -->
          <div v-if="q.questionType === 'SINGLE_CHOICE'" class="q-options">
            <div v-for="opt in q.options" :key="opt.optionLabel" class="q-option">
              <span class="q-option-label">{{ opt.optionLabel }}.</span>
              <span v-html="renderLatex(opt.contentLatex)"></span>
            </div>
          </div>

          <!-- 留白作答区 -->
          <div v-if="opts.blank" class="q-answer-space" :class="blankClass(q.questionType)"></div>
        </article>
      </section>

      <!-- 参考答案 / 解析 -->
      <section v-if="opts.answers" class="answers">
        <h2 class="answers-title">参考答案与解析</h2>
        <article v-for="(q, idx) in questions" :key="'a' + q.id" class="a-block">
          <div class="a-head">
            <span class="q-no">{{ idx + 1 }}.</span>
            <span class="a-key">
              <template v-if="q.questionType === 'SINGLE_CHOICE'">
                正确答案：{{ correctOptionLabels(q) || '—' }}
              </template>
              <template v-else>
                参考答案：<span v-html="renderLatex(q.answerKey || '—')"></span>
              </template>
            </span>
          </div>
          <div v-if="q.solutionSteps && q.solutionSteps.length" class="a-steps">
            <div v-for="step in q.solutionSteps" :key="step.id" class="a-step">
              <div class="a-step-body">
                <span class="a-step-order">步骤 {{ step.stepOrder }}</span>
                <span v-if="step.stepScore != null" class="a-step-score">（{{ step.stepScore }} 分）</span>
                <span v-html="renderLatex(step.contentLatex)"></span>
              </div>
              <div v-if="step.commonErrors" class="a-step-err">易错：{{ step.commonErrors }}</div>
              <div v-if="parseImages(step.imageUrlsJson).length" class="q-images">
                <img v-for="(src, i) in parseImages(step.imageUrlsJson)" :key="i" :src="src" :alt="`步骤图${i + 1}`" />
              </div>
            </div>
          </div>
        </article>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { assignmentApi } from '@/api'
import { renderLatex, parseImages } from '@/utils/latex'

const route = useRoute()
const router = useRouter()
const assignmentId = Number(route.params.id)

const assignment = ref(null)
const loading = ref(true)
const error = ref('')

// 内容开关：默认含抬头与留白，答案默认不含；可被 URL query 覆盖
const opts = reactive({
  header: route.query.header !== '0',
  blank: route.query.blank !== '0',
  answers: route.query.answers === '1'
})

const questions = computed(() => assignment.value?.questions || [])
const totalScore = computed(() => questions.value.reduce((s, q) => s + (q.totalScore || 0), 0))

function typeLabel(t) {
  return { SINGLE_CHOICE: '单选', FILL_BLANK: '填空', OPEN_ENDED: '解答' }[t] || t
}

function correctOptionLabels(q) {
  return (q.options || []).filter(o => o.isCorrect).map(o => o.optionLabel).join('、')
}

function blankClass(t) {
  return { OPEN_ENDED: 'space-lg', FILL_BLANK: 'space-sm', SINGLE_CHOICE: 'space-none' }[t] || 'space-md'
}

function fmtDate(d) {
  if (!d) return ''
  const dt = new Date(d)
  return isNaN(dt) ? d : dt.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

// 导出页通常由入口按钮 window.open('_blank') 打开，直接关闭标签页即可。
// 兜底：若页面被直接打开（window.close() 对非脚本新开的标签无效），跳回详情页。
function closeWindow() {
  window.close()
  router.push(`/assignments/${assignmentId}`)
}

function doPrint() {
  window.print()
}

onMounted(async () => {
  // 让全局 @media print 规则生效（隐藏 App.vue 顶部导航等）
  document.body.classList.add('printing-paper')
  try {
    const res = await assignmentApi.get(assignmentId)
    if (res.success) assignment.value = res.data
    else error.value = res.message || '加载失败'
  } catch (e) {
    error.value = '加载失败：' + (e.message || e)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  document.body.classList.remove('printing-paper')
})
</script>

<style scoped>
.paper-root { background: var(--c-surface2, #f3f4f6); min-height: 100vh; }

/* 工具条 */
.toolbar {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; gap: 16px;
  padding: 10px 16px; background: var(--c-surface, #fff);
  border-bottom: 1px solid var(--c-border, #e5e7eb);
}
.toolbar-title { font-weight: 600; font-size: 14px; margin-right: auto; }
.toolbar-opts { display: flex; gap: 14px; font-size: 13px; color: var(--c-text2, #4b5563); }
.toolbar-opts label { display: flex; align-items: center; gap: 4px; cursor: pointer; }

.loading { text-align: center; padding: 80px; color: var(--c-text3, #9ca3af); }
.loading .spinner { margin: 0 auto; }

/* 纸张：屏幕上模拟 A4 白纸 */
.paper {
  background: #fff; color: #111;
  max-width: 800px; margin: 24px auto; padding: 32px 40px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, .12);
  font-size: 14px; line-height: 1.9;
}

/* 抬头 */
.paper-title { font-size: 22px; font-weight: 700; text-align: center; margin-bottom: 8px; }
.paper-meta {
  display: flex; justify-content: center; flex-wrap: wrap; gap: 16px;
  font-size: 13px; color: #444; margin-bottom: 6px;
}
.paper-desc { font-size: 13px; color: #555; text-align: center; margin-bottom: 10px; }
.paper-fillin { display: flex; flex-wrap: wrap; gap: 20px; font-size: 13px; margin: 10px 0; }
.paper-rule { border: none; border-top: 1.5px solid #333; margin: 10px 0 20px; }

/* 题目 */
.q-block { margin-bottom: 20px; }
.q-line { display: flex; align-items: baseline; flex-wrap: wrap; gap: 6px; font-weight: 600; margin-bottom: 4px; }
.q-no { font-weight: 700; }
.q-type { font-size: 12px; color: #666; font-weight: 500; }
.q-score { font-size: 12px; color: #888; font-weight: 400; }
.q-tag { font-size: 11px; color: #555; background: #f0f0f0; border-radius: 4px; padding: 0 6px; font-weight: 400; }
.q-stem { margin: 2px 0 6px; }
.q-images { display: flex; flex-wrap: wrap; gap: 8px; margin: 6px 0; }
.q-images img { max-width: 320px; max-height: 220px; object-fit: contain; border: 1px solid #ddd; }
.q-options { margin: 4px 0 0 8px; }
.q-option { display: flex; gap: 8px; margin: 3px 0; }
.q-option-label { font-weight: 600; }

/* 留白作答区 */
.q-answer-space { border-bottom: none; }
.space-sm { height: 40px; }
.space-md { height: 80px; }
.space-lg { height: 150px; }
.space-none { height: 0; }

/* 答案区 */
.answers { margin-top: 28px; }
.answers-title { font-size: 18px; font-weight: 700; text-align: center; margin-bottom: 16px; padding-top: 8px; border-top: 1.5px solid #333; }
.a-block { margin-bottom: 14px; }
.a-head { display: flex; gap: 8px; font-weight: 600; }
.a-key { font-weight: 500; }
.a-steps { margin: 4px 0 0 16px; }
.a-step { margin: 4px 0; }
.a-step-order { font-weight: 600; font-size: 13px; }
.a-step-score { font-size: 12px; color: #888; }
.a-step-err { font-size: 12px; color: #b45309; margin-top: 2px; }

/* ============ 打印样式 ============ */
@media print {
  .no-print { display: none !important; }
  .paper-root { background: #fff; min-height: 0; }
  .paper {
    max-width: none; margin: 0; padding: 0;
    box-shadow: none; font-size: 12pt;
  }
  .q-block { break-inside: avoid; page-break-inside: avoid; }
  .answers { break-before: page; page-break-before: always; }
  .answers-title { border-top: none; }
  .q-images img { max-width: 60mm; max-height: 45mm; }
}

@page { margin: 18mm; }
</style>
