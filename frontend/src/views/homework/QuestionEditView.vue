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

          <!-- LaTeX 编辑 -->
          <div style="display:grid;gap:12px;margin-bottom:14px">
            <div>
              <label class="form-label">LaTeX 源码</label>
              <textarea id="stem-input" v-model="form.contentLatex" class="form-control latex-mono"
                        rows="6" placeholder="输入题干，用 $..$ 包裹公式"
                        @paste="onTextareaPaste"></textarea>
            </div>
          </div>

          <!-- LaTeX 预览 -->
          <div style="display:grid;gap:12px;margin-bottom:14px">
            <div>
              <label class="form-label">预览</label>
              <div class="preview-box" v-html="renderLatexWithImages(form.contentLatex, form.imageUrls)"></div>
            </div>
          </div>

          <!-- ── 贴图区域 ── -->
          <div>
            <div class="flex-between" style="margin-bottom:8px">
              <label class="form-label" style="margin-bottom:0">题目配图</label>
              <span class="text-sm text-muted">支持粘贴 (Ctrl+V)、拖拽、点击上传</span>
            </div>

            <!-- Drop zone -->
            <div
              class="img-drop-zone"
              :class="{ 'dragging': isDragging }"
              @dragover.prevent="isDragging = true"
              @dragleave.prevent="isDragging = false"
              @drop.prevent="onDrop"
              @click="triggerFileInput"
              @paste.stop="onZonePaste"
              tabindex="0"
              @keydown.enter="triggerFileInput"
            >
              <input ref="fileInputRef" type="file" accept="image/*" multiple
                     style="display:none" @change="onFileChange" />
              <div v-if="form.imageUrls.length === 0" class="drop-hint">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="1.5" style="opacity:.4;margin-bottom:6px">
                  <rect x="3" y="3" width="18" height="18" rx="3"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <path d="M21 15l-5-5L5 21"/>
                </svg>
                <div style="font-size:13px;color:var(--c-text3)">点击选图 / 拖拽 / 直接粘贴截图</div>
                <div style="font-size:11px;color:var(--c-text3);margin-top:3px">支持 JPG、PNG、GIF，单张最大 5MB</div>
              </div>

              <!-- Image thumbnails -->
              <div v-else class="img-grid" @click.stop>
                <div v-for="(img, i) in form.imageUrls" :key="i" class="img-thumb-wrap">
                  <img :src="img.dataUrl" :alt="`图片 ${i+1}`" class="img-thumb" @click="openLightbox(i)" />
                  <div class="img-thumb-overlay">
                    <button class="img-action-btn" title="预览" @click.stop="openLightbox(i)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
                      </svg>
                    </button>
                    <button class="img-action-btn" title="上移" @click.stop="moveImage(i, -1)" :disabled="i === 0">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M18 15l-6-6-6 6"/>
                      </svg>
                    </button>
                    <button class="img-action-btn" title="下移" @click.stop="moveImage(i, 1)" :disabled="i === form.imageUrls.length - 1">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M6 9l6 6 6-6"/>
                      </svg>
                    </button>
                    <button class="img-action-btn danger" title="删除" @click.stop="removeImage(i)">
                      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                        <path d="M18 6L6 18M6 6l12 12"/>
                      </svg>
                    </button>
                  </div>
                  <div class="img-label">图 {{ i + 1 }}</div>
                </div>

                <!-- Add more button inside grid -->
                <div class="img-add-btn" @click.stop="triggerFileInput" title="继续添加图片">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 5v14M5 12h14"/>
                  </svg>
                </div>
              </div>
            </div>

            <!-- Upload progress -->
            <div v-if="uploadingCount > 0" class="upload-progress">
              <div class="spinner" style="width:12px;height:12px"></div>
              <span class="text-sm text-muted">正在处理 {{ uploadingCount }} 张图片...</span>
            </div>

            <!-- Error tip -->
            <div v-if="imgError" class="img-error">{{ imgError }}</div>
          </div>
        </div>

        <!-- Lightbox -->
        <div v-if="lightboxIndex !== null" class="lightbox-mask" @click="lightboxIndex = null">
          <div class="lightbox-box" @click.stop>
            <img :src="form.imageUrls[lightboxIndex]?.dataUrl" class="lightbox-img" />
            <div class="lightbox-nav">
              <button class="btn btn-sm" :disabled="lightboxIndex === 0"
                      @click="lightboxIndex--">‹ 上一张</button>
              <span class="text-muted text-sm">{{ lightboxIndex + 1 }} / {{ form.imageUrls.length }}</span>
              <button class="btn btn-sm" :disabled="lightboxIndex === form.imageUrls.length - 1"
                      @click="lightboxIndex++">下一张 ›</button>
              <button class="btn btn-sm btn-danger" @click="lightboxIndex = null">关闭</button>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
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

// ── Image state ──────────────────────────────────────────────
const fileInputRef = ref(null)
const isDragging = ref(false)
const uploadingCount = ref(0)
const imgError = ref('')
const lightboxIndex = ref(null)
const MAX_SIZE = 5 * 1024 * 1024   // 5 MB
const ACCEPTED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

const form = ref({
  title: '', contentLatex: '', questionType: 'OPEN_ENDED',
  difficulty: 3, totalScore: 5, answerKey: '', source: '',
  knowledgeTagIds: [],
  imageUrls: [],   // [{ dataUrl, name, size }]
  options: [
    { optionLabel: 'A', contentLatex: '', isCorrect: false },
    { optionLabel: 'B', contentLatex: '', isCorrect: false },
    { optionLabel: 'C', contentLatex: '', isCorrect: false },
    { optionLabel: 'D', contentLatex: '', isCorrect: false },
  ],
  solutionSteps: [{ stepOrder: 1, contentLatex: '', stepScore: 0, commonErrors: '' }],
})

const stepTotal = computed(() => form.value.solutionSteps.reduce((s, st) => s + (st.stepScore || 0), 0))

// ── Image handlers ───────────────────────────────────────────
function triggerFileInput() { fileInputRef.value?.click() }

function onFileChange(e) {
  processFiles([...e.target.files])
  e.target.value = ''
}

function onDrop(e) {
  isDragging.value = false
  const files = [...e.dataTransfer.files].filter(f => f.type.startsWith('image/'))
  processFiles(files)
}

// Paste anywhere on the drop zone
function onZonePaste(e) {
  const items = [...(e.clipboardData?.items || [])]
  const imgItems = items.filter(it => it.kind === 'file' && it.type.startsWith('image/'))
  if (imgItems.length) {
    e.preventDefault()
    processFiles(imgItems.map(it => it.getAsFile()).filter(Boolean))
  }
}

// Paste while inside the textarea (Ctrl+V with image on clipboard)
function onTextareaPaste(e) {
  const items = [...(e.clipboardData?.items || [])]
  const imgItems = items.filter(it => it.kind === 'file' && it.type.startsWith('image/'))
  if (imgItems.length) {
    e.preventDefault()
    processFiles(imgItems.map(it => it.getAsFile()).filter(Boolean))
  }
}

// Global paste listener — catch Ctrl+V anywhere on the page
function onGlobalPaste(e) {
  // Only if no text input is focused (avoid interfering with typing)
  const active = document.activeElement
  const isTextInput = active && (active.tagName === 'INPUT' || active.tagName === 'TEXTAREA')
  if (isTextInput) return
  const items = [...(e.clipboardData?.items || [])]
  const imgItems = items.filter(it => it.kind === 'file' && it.type.startsWith('image/'))
  if (imgItems.length) {
    processFiles(imgItems.map(it => it.getAsFile()).filter(Boolean))
  }
}

function processFiles(files) {
  imgError.value = ''
  const valid = files.filter(f => {
    if (!ACCEPTED.includes(f.type)) { imgError.value = `不支持的格式：${f.name}`; return false }
    if (f.size > MAX_SIZE) { imgError.value = `图片过大（最大5MB）：${f.name}`; return false }
    return true
  })
  if (!valid.length) return
  uploadingCount.value += valid.length
  valid.forEach(file => {
    const reader = new FileReader()
    reader.onload = (ev) => {
      form.value.imageUrls.push({ dataUrl: ev.target.result, name: file.name, size: file.size })
      uploadingCount.value--
    }
    reader.readAsDataURL(file)
  })
}

function removeImage(i) { form.value.imageUrls.splice(i, 1) }

function moveImage(i, dir) {
  const arr = form.value.imageUrls
  const j = i + dir
  if (j < 0 || j >= arr.length) return
  ;[arr[i], arr[j]] = [arr[j], arr[i]]
}

function openLightbox(i) { lightboxIndex.value = i }

// ── Render ───────────────────────────────────────────────────
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

// Preview with images appended below text
function renderLatexWithImages(text, images) {
  const textHtml = renderLatex(text)
  if (!images?.length) return textHtml
  const imgHtml = images.map((img, i) =>
    `<div style="margin-top:10px">
       <div style="font-size:11px;color:var(--c-text3);margin-bottom:4px">图 ${i + 1}</div>
       <img src="${img.dataUrl}" style="max-width:100%;border-radius:4px;border:1px solid var(--c-border)" />
     </div>`
  ).join('')
  return textHtml + imgHtml
}

// ── Save ─────────────────────────────────────────────────────
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
    if (form.value.questionType === 'SINGLE_CHOICE') {
      form.value.options.forEach(o => { o.isCorrect = o.optionLabel === correctOption.value })
      form.value.answerKey = correctOption.value
    }
    // Send imageUrls as dataUrl strings (backend stores as JSON text column)
    const payload = {
      ...form.value,
      imageUrlsJson: JSON.stringify(form.value.imageUrls.map(img => img.dataUrl)),
    }
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
  window.addEventListener('paste', onGlobalPaste)
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
      // Restore saved images
      if (q.imageUrlsJson) {
        try {
          const urls = JSON.parse(q.imageUrlsJson)
          form.value.imageUrls = urls.map((dataUrl, i) => ({ dataUrl, name: `图片${i+1}`, size: 0 }))
        } catch {}
      }
    }
  }
})

onUnmounted(() => {
  window.removeEventListener('paste', onGlobalPaste)
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

/* ── 贴图区域 ── */
.img-drop-zone {
  border: 1.5px dashed var(--c-border-med);
  border-radius: var(--radius);
  padding: 10px;
  cursor: pointer;
  transition: border-color .15s, background .15s;
  outline: none;
  min-height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.img-drop-zone:hover,
.img-drop-zone:focus { border-color: var(--c-primary); background: var(--c-primary-bg); }
.img-drop-zone.dragging { border-color: var(--c-primary); background: var(--c-primary-bg); border-style: solid; }

.drop-hint { display: flex; flex-direction: column; align-items: center; padding: 12px 0; }

.img-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
  padding: 4px;
  align-items: flex-start;
}
.img-thumb-wrap {
  position: relative;
  width: 100px;
  flex-shrink: 0;
}
.img-thumb {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--c-border);
  display: block;
  cursor: zoom-in;
  transition: opacity .12s;
}
.img-thumb-wrap:hover .img-thumb { opacity: .75; }
.img-thumb-overlay {
  position: absolute;
  top: 4px; right: 4px;
  display: flex;
  gap: 3px;
  opacity: 0;
  transition: opacity .12s;
}
.img-thumb-wrap:hover .img-thumb-overlay { opacity: 1; }
.img-action-btn {
  width: 22px; height: 22px;
  border-radius: 4px;
  background: rgba(0,0,0,.55);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .1s;
}
.img-action-btn:hover { background: rgba(0,0,0,.8); }
.img-action-btn.danger:hover { background: var(--c-danger); }
.img-action-btn:disabled { opacity: .35; cursor: not-allowed; }
.img-label {
  font-size: 11px;
  color: var(--c-text3);
  text-align: center;
  margin-top: 3px;
}
.img-add-btn {
  width: 100px; height: 80px;
  border: 1.5px dashed var(--c-border-med);
  border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
  color: var(--c-text3);
  transition: border-color .12s, color .12s;
  flex-shrink: 0;
}
.img-add-btn:hover { border-color: var(--c-primary); color: var(--c-primary); }

.upload-progress { display: flex; align-items: center; gap: 6px; margin-top: 6px; }
.img-error { margin-top: 6px; font-size: 12px; color: var(--c-danger); }

/* ── Lightbox ── */
.lightbox-mask {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.82);
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  z-index: 200;
}
.lightbox-box { display: flex; flex-direction: column; align-items: center; gap: 12px; max-width: 90vw; }
.lightbox-img { max-width: 80vw; max-height: 76vh; border-radius: var(--radius); object-fit: contain; }
.lightbox-nav { display: flex; align-items: center; gap: 10px; }
</style>
