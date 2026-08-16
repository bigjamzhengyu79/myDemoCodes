<template>
  <div class="page">
    <button class="btn btn-sm btn-back" @click="$router.push('/mistakes')">← 返回错题本</button>

    <div v-if="loading" style="text-align:center;padding:60px">
      <div class="spinner" style="margin:auto"></div>
    </div>

    <div v-else-if="loadError" class="card" style="text-align:center;padding:40px;color:var(--c-danger)">
      {{ loadError }}
    </div>

    <template v-else-if="item">
      <!-- 题目 -->
      <div class="card mb-2 sect">
        <div class="q-header">
          <span :class="['badge', typeBadge(item.questionType)]">{{ typeLabel(item.questionType) }}</span>
          <span class="text-sm text-muted">{{ item.totalScore }} 分</span>
          <div style="margin-left:auto;display:flex;gap:6px;align-items:center">
            <span v-for="tag in item.knowledgeTags" :key="tag.id" class="badge badge-gray">{{ tag.name }}</span>
            <span class="diff-dots">
              <span v-for="i in 5" :key="i" :class="['diff-dot', i <= item.difficulty ? 'on' : '']"></span>
            </span>
          </div>
        </div>
        <div class="q-body">
          <div class="q-stem" v-html="renderLatex(item.contentLatex)"></div>

          <div v-if="parseImages(item.questionImageUrlsJson).length" class="q-images">
            <img v-for="(src, i) in parseImages(item.questionImageUrlsJson)" :key="i"
                 :src="src" :alt="`图${i+1}`" class="q-image"
                 @click="lightbox = src" />
          </div>

          <!-- 选择题选项。已判分时红绿高亮，与做题页同一套 class -->
          <template v-if="item.questionType === 'SINGLE_CHOICE'">
            <div v-for="opt in item.options" :key="opt.optionLabel"
                 :class="['choice-row', choiceClass(opt)]">
              <div class="choice-letter">{{ opt.optionLabel }}</div>
              <span v-html="renderLatex(opt.contentLatex)"></span>
            </div>
          </template>
        </div>
      </div>

      <!-- 我的作答 -->
      <div class="card mb-2 sect">
        <div class="sect-title">我的作答</div>
        <div class="q-body">
          <template v-if="item.answerStatus">
            <div class="result-box">
              <div class="flex gap-2 mb-1" style="flex-wrap:wrap">
                <span class="badge" :class="scoreColor(item.score, item.totalScore)">
                  得分：{{ item.score !== null ? item.score : '—' }} / {{ item.totalScore }}
                </span>
                <span v-if="item.errorType && item.errorType !== 'NONE'"
                      class="badge" :class="errorBadge(item.errorType)">{{ errorLabel(item.errorType) }}</span>
                <span v-if="item.answerStatus === 'SUBMITTED'" class="badge badge-amber">待教师批改</span>
              </div>
              <div v-if="item.feedback" class="feedback-text">{{ item.feedback }}</div>
            </div>
            <div v-if="item.answerContent" class="answer-body" v-html="renderLatex(item.answerContent)"></div>
            <div v-if="parseImages(item.answerImageUrlsJson).length" class="q-images">
              <img v-for="(src, i) in parseImages(item.answerImageUrlsJson)" :key="i"
                   :src="src" :alt="`作答图${i+1}`" class="q-image" @click="lightbox = src" />
            </div>
          </template>
          <div v-else class="text-muted text-sm">这道题还没有作答记录</div>
        </div>
      </div>

      <!-- 正确答案与解析 -->
      <div class="card mb-2 sect">
        <div class="sect-title">正确答案与解析</div>
        <div class="q-body">
          <template v-if="item.answerRevealed">
            <div v-if="item.answerKey" class="mb-3">
              <div class="form-label">参考答案</div>
              <div class="answer-key" v-html="renderLatex(item.answerKey)"></div>
            </div>
            <div v-if="item.solutionSteps && item.solutionSteps.length">
              <div class="form-label">解析步骤</div>
              <div v-for="step in item.solutionSteps" :key="step.id" class="solution-step">
                <div class="text-sm text-muted mb-1">
                  步骤 {{ step.stepOrder }}
                  <span v-if="step.stepScore">（{{ step.stepScore }} 分）</span>
                </div>
                <div v-html="renderLatex(step.contentLatex)"></div>
                <div v-if="parseImages(step.imageUrlsJson).length" class="q-images">
                  <img v-for="(src, i) in parseImages(step.imageUrlsJson)" :key="i"
                       :src="src" :alt="`步骤图${i+1}`" class="q-image" @click="lightbox = src" />
                </div>
                <div v-if="step.commonErrors" class="text-sm common-errors">
                  常见错误：{{ step.commonErrors }}
                </div>
              </div>
            </div>
            <div v-if="!item.answerKey && !(item.solutionSteps || []).length" class="text-muted text-sm">
              这道题还没有录入参考答案
            </div>
          </template>
          <div v-else class="text-muted text-sm">答案将在教师批改后公开</div>
        </div>
      </div>

      <!-- 我的订正 -->
      <div class="card sect">
        <div class="sect-title">我的订正</div>
        <div class="q-body">
          <LatexEditor v-model:content-latex="noteContent"
                       v-model:image-urls="noteImages"
                       inputLabel="订正与反思（支持 LaTeX）"
                       :rows="5"
                       placeholder="这道题错在哪？正确思路是什么？下次注意什么？" />

          <div class="form-label" style="margin-top:6px">掌握状态</div>
          <div class="flex gap-3 mb-3" style="flex-wrap:wrap">
            <label v-for="m in MASTERY_TYPES" :key="m.value" class="mastery-opt">
              <input type="radio" :value="m.value" v-model="mastery" />
              <span class="badge" :class="m.badge">{{ m.label }}</span>
            </label>
          </div>

          <div class="flex gap-2" style="align-items:center">
            <button class="btn btn-primary" :disabled="saving" @click="save">
              <span v-if="saving" class="spinner" style="width:12px;height:12px"></span>
              保存订正
            </button>
            <span v-if="savedAt" class="text-sm text-muted">已保存于 {{ savedAt }}</span>
          </div>
          <div v-if="saveError" class="form-error" style="margin-top:8px">{{ saveError }}</div>
        </div>
      </div>

      <!-- Lightbox -->
      <div v-if="lightbox" class="q-lightbox-mask" @click="lightbox = null">
        <div style="text-align:center" @click.stop>
          <img :src="lightbox" style="max-width:90vw;max-height:82vh;border-radius:8px" />
          <div style="margin-top:10px">
            <button class="btn" @click="lightbox = null">关闭</button>
          </div>
        </div>
      </div>

      <FormulaWidget />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { mistakeApi } from '@/api'
import { renderLatex, parseImages } from '@/utils/latex'
import {
  MASTERY_TYPES, typeLabel, typeBadge,
  errorLabel, errorBadge, scoreColor
} from '@/utils/question'
import LatexEditor from '@/components/LatexEditor.vue'
import FormulaWidget from '@/components/FormulaWidget.vue'

const route = useRoute()
const noteId = Number(route.params.id)

const item = ref(null)
const loading = ref(true)
const loadError = ref('')

const noteContent = ref('')
const noteImages = ref([])
const mastery = ref('UNREVIEWED')
const saving = ref(false)
const saveError = ref('')
const savedAt = ref('')
const lightbox = ref(null)

/**
 * 选项配色：只在已判分时才揭示对错。
 * 与做题页 choiceClass 同一套语义 —— 学生在两处看到的红绿含义必须一致。
 */
function choiceClass(opt) {
  if (!item.value?.answerRevealed) {
    return item.value?.answerContent === opt.optionLabel ? 'selected' : ''
  }
  if (opt.isCorrect) return 'correct'
  if (item.value.answerContent === opt.optionLabel) return 'wrong'
  return ''
}

function applyDetail(d) {
  item.value = d
  noteContent.value = d.noteContent || ''
  noteImages.value = parseImages(d.noteImageUrlsJson).map(url => ({ dataUrl: url }))
  mastery.value = d.mastery || 'UNREVIEWED'
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await mistakeApi.get(noteId)
    if (res.success) applyDetail(res.data)
    else loadError.value = res.message || '加载失败'
  } catch (e) {
    // /api/mistakes/** 需要登录，未登录时是 403（不是 {success:false}）
    loadError.value = e?.response?.data?.message || '加载失败，请确认已登录后重试'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  saveError.value = ''
  try {
    const res = await mistakeApi.saveNote(item.value.questionId, {
      noteContent: noteContent.value,
      imageUrlsJson: noteImages.value.length
        ? JSON.stringify(noteImages.value.map(img => img.dataUrl))
        : null,
      mastery: mastery.value
    })
    if (res.success) {
      applyDetail(res.data)
      savedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    } else {
      saveError.value = res.message || '保存失败'
    }
  } catch (e) {
    saveError.value = e?.response?.data?.message || '保存失败，请重试'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.btn-back { margin-bottom: 14px; }
.sect { overflow: hidden; padding: 0; }
.sect-title { padding: 10px 16px; background: var(--c-surface2); border-bottom: 1px solid var(--c-border); font-size: 13px; font-weight: 600; }
.q-header { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--c-surface2); border-bottom: 1px solid var(--c-border); flex-wrap: wrap; }
.q-body { padding: 16px; }
.q-stem { font-size: 14px; line-height: 1.9; margin-bottom: 14px; }
.choice-row { display: flex; align-items: center; gap: 10px; padding: 8px 12px; border-radius: var(--radius-sm); border: 1px solid transparent; font-size: 13px; margin-bottom: 5px; }
.choice-row.selected { background: var(--c-primary-bg); border-color: var(--c-primary); }
.choice-row.correct { background: var(--c-success-bg); border-color: var(--c-success); }
.choice-row.wrong { background: var(--c-danger-bg); border-color: var(--c-danger); }
.choice-letter { width: 22px; height: 22px; border-radius: 50%; border: 1px solid var(--c-border); display: flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 500; flex-shrink: 0; }
.result-box { background: var(--c-surface2); border-radius: var(--radius-sm); padding: 10px 12px; margin-bottom: 12px; border-left: 3px solid var(--c-primary); }
.feedback-text { font-size: 13px; color: var(--c-text2); margin-top: 4px; }
.answer-body { font-size: 14px; line-height: 1.9; }
.answer-key { font-size: 14px; line-height: 1.9; background: var(--c-success-bg); border-radius: var(--radius-sm); padding: 10px 12px; }
.solution-step { border-left: 3px solid var(--c-border); padding: 8px 0 8px 12px; margin-bottom: 10px; font-size: 14px; line-height: 1.9; }
.common-errors { color: var(--c-warning); margin-top: 6px; }
.mastery-opt { display: flex; align-items: center; gap: 6px; cursor: pointer; }
.form-error { color: var(--c-danger); font-size: 13px; }
.q-images { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.q-image { max-width: 240px; max-height: 180px; object-fit: contain; border: 1px solid var(--c-border); border-radius: var(--radius-sm); cursor: zoom-in; }
.q-lightbox-mask { position: fixed; inset: 0; background: rgba(0,0,0,.72); display: flex; align-items: center; justify-content: center; z-index: 200; }
</style>
