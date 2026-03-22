<template>
  <div class="page" style="max-width:900px">
    <div class="flex gap-2 mb-3" style="align-items:center">
      <button class="btn btn-sm" @click="$router.back()">← 返回</button>
      <h1 style="font-size:18px;font-weight:600">{{ isEdit ? '编辑题目' : '新建题目' }}</h1>
    </div>

    <div style="display:grid;grid-template-columns:1fr 260px;gap:16px">
      <!-- Left: main editor -->
      <div>
        <!-- Toolbar -->
        <div class="card card-sm mb-2">
          <div class="flex gap-2" style="flex-wrap:wrap">
            <button class="btn btn-sm" v-for="sym in symbols" :key="sym.label"
                    @click="insertSymbol(sym.latex)">{{ sym.label }}</button>
          </div>
        </div>

        <!-- Question stem -->
        <div class="card mb-2">
          <div class="editor-section-title">题干</div>
          <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
            <div>
              <label class="form-label">LaTeX 源码</label>
              <textarea id="stem-input" v-model="form.contentLatex" class="form-control latex-mono"
                        rows="6" placeholder="输入题干，用 $..$ 包裹公式"></textarea>
            </div>
            <div>
              <label class="form-label">预览</label>
              <div class="preview-box" v-html="renderLatex(form.contentLatex)"></div>
            </div>
          </div>
        </div>

        <!-- Options (single choice) -->
        <div v-if="form.questionType === 'SINGLE_CHOICE'" class="card mb-2">
          <div class="editor-section-title">选项</div>
          <div v-for="(opt, i) in form.options" :key="i" style="margin-bottom:10px">
            <div class="flex gap-2" style="align-items:center;margin-bottom:4px">
              <div class="opt-letter">{{ opt.optionLabel }}</div>
              <label class="flex gap-2" style="font-size:12px;color:var(--c-text2);cursor:pointer">
                <input type="radio" :value="opt.optionLabel" v-model="correctOption" /> 正确答案
              </label>
            </div>
            <input v-model="opt.contentLatex" class="form-control" :placeholder="`选项${opt.optionLabel}内容（支持LaTeX）`" />
          </div>
        </div>

        <!-- Solution steps -->
        <div class="card mb-2">
          <div class="flex-between editor-section-title" style="margin-bottom:12px">
            <span>解析步骤</span>
            <button class="btn btn-sm" @click="addStep">+ 加步骤</button>
          </div>
          <div v-for="(step, i) in form.solutionSteps" :key="i" class="step-row">
            <div class="step-num-badge">{{ i + 1 }}</div>
            <div style="flex:1">
              <textarea v-model="step.contentLatex" class="form-control latex-mono" rows="2"
                        :placeholder="`步骤 ${i+1} 内容（LaTeX）`"></textarea>
              <div class="flex gap-2" style="margin-top:6px">
                <input v-model.number="step.stepScore" class="form-control" type="number" min="0"
                       style="width:80px" placeholder="分值" />
                <input v-model="step.commonErrors" class="form-control"
                       placeholder="常见错误（可选）" style="flex:1" />
              </div>
            </div>
            <button class="btn btn-sm btn-danger" @click="form.solutionSteps.splice(i,1)">×</button>
          </div>
        </div>
      </div>

      <!-- Right: properties -->
      <div>
        <div class="card mb-2">
          <div class="editor-section-title">题目属性</div>
          <div class="form-group">
            <label class="form-label">题型</label>
            <select v-model="form.questionType" class="form-control">
              <option value="SINGLE_CHOICE">单选题</option>
              <option value="FILL_BLANK">填空题</option>
              <option value="OPEN_ENDED">解答题</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">难度（{{ form.difficulty }}星）</label>
            <input v-model.number="form.difficulty" type="range" min="1" max="5"
                   style="width:100%;cursor:pointer" />
            <div class="flex-between text-sm text-muted"><span>基础</span><span>压轴</span></div>
          </div>
          <div class="form-group">
            <label class="form-label">总分</label>
            <input v-model.number="form.totalScore" class="form-control" type="number" min="1" />
          </div>
          <div class="form-group" v-if="form.questionType !== 'OPEN_ENDED'">
            <label class="form-label">标准答案</label>
            <input v-model="form.answerKey" class="form-control" placeholder="如：A 或 10" />
          </div>
          <div class="form-group">
            <label class="form-label">来源</label>
            <input v-model="form.source" class="form-control" placeholder="如：2024全国甲卷" />
          </div>
        </div>

        <!-- Knowledge tags -->
        <div class="card mb-2">
          <div class="editor-section-title">知识点标签</div>
          <div style="max-height:200px;overflow-y:auto">
            <label v-for="tag in tags" :key="tag.id"
                   class="flex gap-2" style="padding:4px 0;cursor:pointer;font-size:13px">
              <input type="checkbox" :value="tag.id" v-model="form.knowledgeTagIds" />
              <span>{{ tag.name }}</span>
              <span class="text-muted text-sm">{{ tag.chapter }}</span>
            </label>
          </div>
        </div>

        <!-- Preview total score -->
        <div class="card" style="background:var(--c-primary-bg);border-color:var(--c-primary)">
          <div class="text-sm" style="color:var(--c-primary);margin-bottom:2px">步骤分合计</div>
          <div style="font-size:22px;font-weight:600;color:var(--c-primary)">
            {{ stepTotal }} <span style="font-size:14px">/ {{ form.totalScore }} 分</span>
          </div>
        </div>

        <button class="btn btn-primary" style="width:100%;justify-content:center;margin-top:12px;padding:10px"
                @click="save" :disabled="saving">
          <span v-if="saving" class="spinner" style="width:14px;height:14px"></span>
          {{ saving ? '保存中...' : (isEdit ? '保存修改' : '创建题目') }}
        </button>
        <div v-if="saveMsg" :class="['save-msg', saveMsg.ok ? 'ok' : 'err']">{{ saveMsg.text }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { questionApi } from '@/api'
import katex from 'katex'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const saving = ref(false)
const saveMsg = ref(null)
const tags = ref([])
const correctOption = ref('A')

const form = ref({
  title: '', contentLatex: '', questionType: 'OPEN_ENDED',
  difficulty: 3, totalScore: 5, answerKey: '', source: '',
  knowledgeTagIds: [],
  options: [
    { optionLabel: 'A', contentLatex: '', isCorrect: false },
    { optionLabel: 'B', contentLatex: '', isCorrect: false },
    { optionLabel: 'C', contentLatex: '', isCorrect: false },
    { optionLabel: 'D', contentLatex: '', isCorrect: false },
  ],
  solutionSteps: [{ stepOrder: 1, contentLatex: '', stepScore: 0, commonErrors: '' }],
})

const stepTotal = computed(() => form.value.solutionSteps.reduce((s, st) => s + (st.stepScore || 0), 0))

const symbols = [
  { label: '分数', latex: '\\frac{}{}' },
  { label: '根号', latex: '\\sqrt{}' },
  { label: '上标', latex: '^{}' },
  { label: '下标', latex: '_{}' },
  { label: '积分', latex: '\\int_{}^{}' },
  { label: '求和', latex: '\\sum_{}^{}' },
  { label: '极限', latex: '\\lim_{}' },
  { label: '导数', latex: "f'(x)" },
  { label: '无穷', latex: '\\infty' },
  { label: '≥', latex: '\\geq' },
  { label: '≤', latex: '\\leq' },
]

function insertSymbol(latex) {
  const el = document.getElementById('stem-input')
  if (!el) { form.value.contentLatex += '$' + latex + '$'; return }
  const start = el.selectionStart, end = el.selectionEnd
  const text = form.value.contentLatex
  form.value.contentLatex = text.slice(0, start) + '$' + latex + '$' + text.slice(end)
}

function renderLatex(text) {
  if (!text) return '<span style="color:var(--c-text3)">预览...</span>'
  return text.replace(/\$\$([^$]+)\$\$/g, (_, m) => {
    try { return katex.renderToString(m, { displayMode: true }) } catch { return m }
  }).replace(/\$([^$]+)\$/g, (_, m) => {
    try { return katex.renderToString(m, { displayMode: false }) } catch { return m }
  }).replace(/\n/g, '<br>')
}

function addStep() {
  form.value.solutionSteps.push({
    stepOrder: form.value.solutionSteps.length + 1,
    contentLatex: '', stepScore: 0, commonErrors: ''
  })
}

async function save() {
  saving.value = true
  saveMsg.value = null
  try {
    // Sync correct answer for single choice
    if (form.value.questionType === 'SINGLE_CHOICE') {
      form.value.options.forEach(o => { o.isCorrect = o.optionLabel === correctOption.value })
      form.value.answerKey = correctOption.value
    }
    const payload = { ...form.value }
    if (isEdit.value) {
      await questionApi.update(route.params.id, payload)
    } else {
      await questionApi.create(payload)
    }
    saveMsg.value = { ok: true, text: isEdit.value ? '已保存' : '题目创建成功！' }
    setTimeout(() => router.push('/questions'), 1000)
  } catch (e) {
    saveMsg.value = { ok: false, text: typeof e === 'string' ? e : '保存失败' }
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  const tRes = await questionApi.getTags()
  tags.value = tRes.data || []
  if (isEdit.value) {
    const res = await questionApi.get(route.params.id)
    if (res.success) {
      const q = res.data
      form.value.contentLatex = q.contentLatex
      form.value.questionType = q.questionType
      form.value.difficulty = q.difficulty
      form.value.totalScore = q.totalScore
      form.value.answerKey = q.answerKey || ''
      form.value.source = q.source || ''
      form.value.knowledgeTagIds = q.knowledgeTags.map(t => t.id)
      if (q.options?.length) form.value.options = q.options.map(o => ({ ...o }))
      if (q.solutionSteps?.length) form.value.solutionSteps = q.solutionSteps.map(s => ({ ...s }))
      correctOption.value = q.answerKey || 'A'
    }
  }
})
</script>

<style scoped>
.editor-section-title { font-size: 13px; font-weight: 500; color: var(--c-text2); margin-bottom: 12px; }
.preview-box { border: 1px solid var(--c-border); border-radius: var(--radius-sm); padding: 10px 12px; min-height: 80px; font-size: 14px; line-height: 1.9; background: var(--c-surface2); }
.latex-mono { font-family: 'JetBrains Mono', monospace; font-size: 12px; }
.opt-letter { width: 22px; height: 22px; border-radius: 50%; background: var(--c-surface2); border: 1px solid var(--c-border-med); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 500; flex-shrink: 0; }
.step-row { display: flex; gap: 10px; align-items: flex-start; margin-bottom: 12px; }
.step-num-badge { width: 22px; height: 22px; border-radius: 50%; background: var(--c-primary); color: #fff; font-size: 11px; font-weight: 500; display: flex; align-items: center; justify-content: center; flex-shrink: 0; margin-top: 6px; }
.save-msg { margin-top: 8px; padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px; }
.save-msg.ok { background: var(--c-success-bg); color: var(--c-success); }
.save-msg.err { background: var(--c-danger-bg); color: var(--c-danger); }
</style>
