<template>
  <div class="page">
    <div class="flex-between mb-3">
      <div>
        <h1 style="font-size:20px;font-weight:600">错题本</h1>
        <div class="text-muted text-sm" style="margin-top:2px">
          共 {{ summary.total }} 题 · 已掌握 {{ summary.mastered }} 题
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="card card-sm mb-2 flex gap-3" style="flex-wrap:wrap;align-items:center">
      <select v-model="filter.type" class="form-control" style="width:auto">
        <option value="">全部题型</option>
        <option v-for="t in QUESTION_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
      </select>
      <select v-model="filter.mastery" class="form-control" style="width:auto">
        <option value="">全部状态</option>
        <option v-for="m in MASTERY_TYPES" :key="m.value" :value="m.value">{{ m.label }}</option>
      </select>
      <select v-model="filter.tag" class="form-control" style="width:auto">
        <option value="">全部知识点</option>
        <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
      </select>
      <button v-if="hasFilter" class="btn btn-sm" @click="resetFilter">重置</button>
      <span v-if="!loading" class="text-sm text-muted" style="margin-left:auto">
        {{ totalElements }} 条结果
      </span>
    </div>

    <div v-if="loadError" class="card card-sm mb-2" style="color:var(--c-danger)">
      {{ loadError }}
      <button class="btn btn-sm" style="margin-left:8px" @click="loadPage">重试</button>
    </div>

    <div v-if="loading" style="text-align:center;padding:60px">
      <div class="spinner" style="margin:auto"></div>
    </div>

    <template v-else>
      <div v-if="items.length === 0" class="card" style="text-align:center;padding:60px;color:var(--c-text3)">
        <template v-if="hasFilter">没有匹配的题目，试试调整筛选条件</template>
        <template v-else>还没有收藏的题目<br /><span class="text-sm">做题时点右上角的 ☆ 就能加入错题本</span></template>
      </div>

      <div v-for="item in items" :key="item.id" class="card mb-2 m-row">
        <div class="q-header">
          <span :class="['badge', typeBadge(item.questionType)]">{{ typeLabel(item.questionType) }}</span>
          <span v-if="item.score !== null && item.score !== undefined"
                class="badge" :class="scoreColor(item.score, item.totalScore)">
            得分：{{ item.score }} / {{ item.totalScore }}
          </span>
          <span v-else class="badge badge-gray">未作答</span>
          <span class="badge" :class="masteryBadge(item.mastery)">{{ masteryLabel(item.mastery) }}</span>
          <span v-if="item.errorType && item.errorType !== 'NONE'"
                class="badge" :class="errorBadge(item.errorType)">{{ errorLabel(item.errorType) }}</span>
          <div style="margin-left:auto;display:flex;gap:6px;align-items:center">
            <span v-for="tag in item.knowledgeTags" :key="tag.id" class="badge badge-gray">{{ tag.name }}</span>
            <span class="diff-dots">
              <span v-for="i in 5" :key="i" :class="['diff-dot', i <= item.difficulty ? 'on' : '']"></span>
            </span>
          </div>
        </div>

        <div class="q-body">
          <div class="q-stem" v-html="renderLatex(item.contentLatex)"></div>
          <div class="flex-between text-sm text-muted" style="margin-top:10px">
            <span>
              <template v-if="item.sourceAssignmentTitle">来自《{{ item.sourceAssignmentTitle }}》 · </template>
              收藏于 {{ fmtDate(item.createdAt) }}
              <span v-if="item.hasNote" style="margin-left:8px;color:var(--c-primary)">已写订正</span>
            </span>
            <span class="flex gap-2">
              <router-link class="btn btn-sm" :to="`/mistakes/${item.id}`">查看详情 →</router-link>
              <button class="btn btn-sm btn-danger" :disabled="removing[item.questionId]"
                      @click="confirmRemove = item">移出</button>
            </span>
          </div>
        </div>
      </div>

      <!-- 翻页 -->
      <div v-if="totalPages > 1" class="flex" style="justify-content:center;align-items:center;gap:12px;margin-top:16px">
        <button class="btn btn-sm" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
        <span class="text-sm text-muted">{{ page + 1 }} / {{ totalPages }}</span>
        <button class="btn btn-sm" :disabled="page >= totalPages - 1" @click="goPage(page + 1)">›</button>
      </div>
    </template>

    <!-- 移出确认。全应用无 window.confirm，一律用自建弹窗 -->
    <div v-if="confirmRemove" class="modal-mask" @click.self="confirmRemove = null">
      <div class="card modal-box-sm">
        <h3 style="font-size:16px;font-weight:600;margin-bottom:10px">移出错题本</h3>
        <p class="text-sm text-muted" style="margin-bottom:16px">
          确定把这道题移出错题本吗？你写的订正笔记也会一并删除。
        </p>
        <div class="flex gap-2" style="justify-content:flex-end">
          <button class="btn btn-sm" @click="confirmRemove = null">取消</button>
          <button class="btn btn-sm btn-danger" @click="doRemove">确定移出</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { mistakeApi, questionApi } from '@/api'
import { renderLatex } from '@/utils/latex'
import {
  QUESTION_TYPES, MASTERY_TYPES, typeLabel, typeBadge,
  errorLabel, errorBadge, masteryLabel, masteryBadge, scoreColor
} from '@/utils/question'

const PAGE_SIZE = 20

const items = ref([])
const tags = ref([])
const summary = ref({ total: 0, mastered: 0 })
const loading = ref(true)
const loadError = ref('')
const removing = ref({})
const confirmRemove = ref(null)

const filter = ref({ type: '', mastery: '', tag: '' })
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const hasFilter = computed(() => !!filter.value.type || !!filter.value.mastery || !!filter.value.tag)

// 并发保护：翻页/筛选快速切换时，只认最后一次请求的结果（同 QuestionPicker）
let reqSeq = 0

async function loadPage() {
  const seq = ++reqSeq
  loading.value = true
  loadError.value = ''
  try {
    const res = await mistakeApi.list({
      questionType: filter.value.type || undefined,
      mastery: filter.value.mastery || undefined,
      tagId: filter.value.tag || undefined,
      page: page.value,
      size: PAGE_SIZE
    })
    if (seq !== reqSeq) return          // 已有更新的请求，丢弃这次结果
    const d = res.data || {}
    items.value = d.content || []
    totalPages.value = d.totalPages || 0
    totalElements.value = d.totalElements || 0
    // 筛选后总页数变少时，页码可能落在范围外
    if (page.value > 0 && items.value.length === 0 && totalPages.value > 0) {
      page.value = Math.min(page.value, totalPages.value - 1)
      await loadPage()
    }
  } catch (e) {
    if (seq !== reqSeq) return
    // /api/mistakes/** 需要登录，未登录时是 403（不是 {success:false}），会走到这里
    loadError.value = '错题本加载失败，请确认已登录后重试'
    items.value = []
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

async function loadSummary() {
  try {
    const res = await mistakeApi.summary()
    if (res.success) summary.value = res.data || { total: 0, mastered: 0 }
  } catch (e) { /* 统计失败不影响列表 */ }
}

async function loadTags() {
  try {
    tags.value = (await questionApi.getTags()).data || []
  } catch (e) { /* 标签加载失败不影响筛选其他项 */ }
}

async function doRemove() {
  const item = confirmRemove.value
  if (!item) return
  confirmRemove.value = null
  removing.value[item.questionId] = true
  try {
    await mistakeApi.remove(item.questionId)
    await Promise.all([loadPage(), loadSummary()])
  } catch (e) {
    loadError.value = '移出失败，请重试'
  } finally {
    removing.value[item.questionId] = false
  }
}

function resetFilter() {
  filter.value = { type: '', mastery: '', tag: '' }
}

function goPage(p) {
  if (p < 0 || p > totalPages.value - 1 || loading.value) return
  page.value = p
  loadPage()
}

function fmtDate(d) {
  return d ? new Date(d).toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' }) : ''
}

// 筛选变化要回到第 1 页，否则会停在越界页码上
watch(() => [filter.value.type, filter.value.mastery, filter.value.tag], () => {
  page.value = 0
  loadPage()
})

onMounted(() => { loadTags(); loadSummary(); loadPage() })
</script>

<style scoped>
.m-row { overflow: hidden; padding: 0; }
.q-header { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--c-surface2); border-bottom: 1px solid var(--c-border); flex-wrap: wrap; }
.q-body { padding: 14px 16px; }
.q-stem { font-size: 14px; line-height: 1.9; max-height: 5.7em; overflow: hidden; }
.modal-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; }
.modal-box-sm { width: 380px; max-width: 100%; }
</style>
