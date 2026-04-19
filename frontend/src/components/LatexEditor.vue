<template>
  <div>
    <!-- LaTeX 编辑 -->
    <div style="display:grid;gap:12px;margin-bottom:14px" v-if="showInput">
      <div>
        <FormulaInput
          v-model="localContentLatex"
          :multiline="multiline"
          :rows=rows
          :label="inputLabel"
          :placeholder="placeholder"
          data-label="LaTeX 输入框"
        />
      </div>
    </div>

    <!-- LaTeX 预览 -->
    <div v-if="showPreview" style="display:grid;gap:12px;margin-bottom:14px">
      <div>
        <div class="preview-box" v-html="renderLatexWithImages(localContentLatex, localImageUrls)"></div>
      </div>
    </div>

    <!-- ── 贴图区域 ── -->
    <div v-if="showImageUpload">
      <div class="flex-between" style="margin-bottom:8px">
        <label class="form-label" style="margin-bottom:0">配图</label>
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
        <div v-if="localImageUrls.length === 0" class="drop-hint">
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
          <div v-for="(img, i) in localImageUrls" :key="i" class="img-thumb-wrap">
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
              <button class="img-action-btn" title="下移" @click.stop="moveImage(i, 1)" :disabled="i === localImageUrls.length - 1">
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

    <!-- Lightbox -->
    <div v-if="lightboxIndex !== null" class="lightbox-mask" @click="lightboxIndex = null">
      <div class="lightbox-box" @click.stop>
        <img :src="localImageUrls[lightboxIndex]?.dataUrl" class="lightbox-img" />
        <div class="lightbox-nav">
          <button class="btn btn-sm" :disabled="lightboxIndex === 0"
                  @click="lightboxIndex--">‹ 上一张</button>
          <span class="text-muted text-sm">{{ lightboxIndex + 1 }} / {{ localImageUrls.length }}</span>
          <button class="btn btn-sm" :disabled="lightboxIndex === localImageUrls.length - 1"
                  @click="lightboxIndex++">下一张 ›</button>
          <button class="btn btn-sm btn-danger" @click="lightboxIndex = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import katex from 'katex'
import FormulaInput from './FormulaInput.vue'

// Props
const props = defineProps({
  contentLatex: {
    type: String,
    default: ''
  },
  imageUrls: {
    type: Array,
    default: () => []
  },
  // 新增 props
  multiline: {
    type: Boolean,
    default: true
  },
  rows: {
    type: Number,
    default: 6
  },
  inputLabel: {
    type: String,
    default: '题目编辑(支持 LaTeX)'
  },
  placeholder: {
    type: String,
    default: '输入题干，用 $..$ 包裹公式'
  },
  showPreview: {
    type: Boolean,
    default: true
  },
  showImageUpload: {
    type: Boolean,
    default: true
  },
  showInput: {
    type: Boolean,
    default: true
  }
})

// Emits
const emit = defineEmits(['update:contentLatex', 'update:imageUrls'])

// Local reactive data
const localContentLatex = ref(props.contentLatex)
const localImageUrls = ref([...props.imageUrls])

// Image state
const fileInputRef = ref(null)
const isDragging = ref(false)
const uploadingCount = ref(0)
const imgError = ref('')
const lightboxIndex = ref(null)
const MAX_SIZE = 5 * 1024 * 1024   // 5 MB
const ACCEPTED = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

// Watch for prop changes
watch(() => props.contentLatex, (newVal) => {
  localContentLatex.value = newVal
})

watch(() => props.imageUrls, (newVal) => {
  if (newVal !== localImageUrls.value) {
    localImageUrls.value = [...newVal]
  }
})

// Emit updates
watch(localContentLatex, (newVal) => {
  emit('update:contentLatex', newVal)
})

watch(localImageUrls, (newVal) => {
  emit('update:imageUrls', newVal)
}, { deep: true })

// Image handlers
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
      localImageUrls.value.push({ dataUrl: ev.target.result, name: file.name, size: file.size })
      uploadingCount.value--
    }
    reader.readAsDataURL(file)
  })
}

function removeImage(i) { localImageUrls.value.splice(i, 1) }

function moveImage(i, dir) {
  const arr = localImageUrls.value
  const j = i + dir
  if (j < 0 || j >= arr.length) return
  ;[arr[i], arr[j]] = [arr[j], arr[i]]
}

function openLightbox(i) { lightboxIndex.value = i }

// Render functions
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

onMounted(() => {
  window.addEventListener('paste', onGlobalPaste)
})

onUnmounted(() => {
  window.removeEventListener('paste', onGlobalPaste)
})
</script>

<style scoped>
.preview-box { border: 1px solid var(--c-border); border-radius: var(--radius-sm); padding: 10px 12px; min-height: 80px; font-size: 14px; line-height: 1.9; background: var(--c-surface2); }
.latex-mono { font-family: 'JetBrains Mono', monospace; font-size: 12px; }

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