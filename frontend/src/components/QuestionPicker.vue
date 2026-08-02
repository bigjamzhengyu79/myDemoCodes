<template>
  <div class="qp">
    <!-- 左栏：题库 -->
    <div class="qp-pane">
      <div class="qp-pane-head">
        <span class="qp-pane-title">题库</span>
        <span class="text-sm text-muted">共 {{ totalElements }} 题</span>
      </div>

      <div class="qp-toolbar">
        <input v-model="keyword" class="form-control" placeholder="搜索题目标题，或输入 #编号" />
        <div class="qp-filters">
          <select v-model="filter.type" class="form-control">
            <option value="">全部题型</option>
            <option v-for="t in QUESTION_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
          </select>
          <select v-model="filter.difficulty" class="form-control">
            <option value="">全部难度</option>
            <option v-for="i in 5" :key="i" :value="i">{{ '★'.repeat(i) }}</option>
          </select>
          <select v-model="filter.tag" class="form-control">
            <option value="">全部知识点</option>
            <option v-for="t in tags" :key="t.id" :value="t.id">{{ t.name }}</option>
          </select>
          <button type="button" class="btn btn-sm" @click="resetFilters">重置</button>
        </div>
      </div>

      <div class="qp-list">
        <div v-if="loading" style="text-align:center;padding:40px">
          <div class="spinner" style="margin:auto"></div>
        </div>
        <div v-else-if="loadError" class="qp-empty">
          <div style="margin-bottom:10px">{{ loadError }}</div>
          <button type="button" class="btn btn-sm" @click="loadData">重试</button>
        </div>
        <div v-else-if="pageItems.length === 0 && hasFilter" class="qp-empty">
          没有匹配的题目，试试调整筛选条件
        </div>
        <div v-else-if="pageItems.length === 0" class="qp-empty">
          题库中暂无题目，请先到「题库管理」新建题目
        </div>
        <template v-else>
          <label v-for="q in pageItems" :key="q.id" :class="['qp-row', isPicked(q) ? 'picked' : '']">
            <input type="checkbox" :checked="isPicked(q)" @change="toggle(q)" />
            <span class="qp-id">#{{ q.id }}</span>
            <span class="qp-text">
              <span v-if="q.title" style="font-weight:500">{{ q.title }}</span>
              <span :class="q.title ? 'text-muted' : ''">{{ q.title ? ' · ' : '' }}{{ stripLatex(q.contentLatex) }}</span>
            </span>
            <span :class="['badge', typeBadge(q.questionType)]">{{ typeLabel(q.questionType) }}</span>
            <span class="diff-dots">
              <span v-for="i in 5" :key="i" :class="['diff-dot', i <= q.difficulty ? 'on' : '']"></span>
            </span>
            <span class="qp-score">{{ q.totalScore }}分</span>
          </label>
        </template>
      </div>

      <div class="qp-pane-foot">
        <span class="qp-pager">
          <button type="button" class="qp-icon-btn" :disabled="page === 0 || loading"
                  title="上一页" @click="goPage(page - 1)">‹</button>
          <span class="text-sm text-muted">{{ totalPages === 0 ? 0 : page + 1 }} / {{ totalPages }}</span>
          <button type="button" class="qp-icon-btn" :disabled="page >= totalPages - 1 || loading"
                  title="下一页" @click="goPage(page + 1)">›</button>
        </span>
        <button type="button" class="btn btn-sm" :disabled="pageItems.length === 0" @click="addAllOnPage">
          全选本页
        </button>
      </div>
    </div>

    <!-- 右栏：已选题目 -->
    <!-- 注意：后端 assignment_questions 无排序列（@OrderBy("id ASC")），
         此处排序仅在创建界面生效，保存后按题目 ID 升序展示。 -->
    <div class="qp-pane">
      <div class="qp-pane-head">
        <span class="qp-pane-title">已选题目</span>
        <button type="button" class="btn btn-sm" :disabled="modelValue.length === 0" @click="clearAll">清空</button>
      </div>

      <div class="qp-list">
        <div v-if="modelValue.length === 0" class="qp-empty">从左侧选择题目</div>
        <template v-else>
          <div v-for="(q, i) in modelValue" :key="q.id" class="qp-row">
            <span class="qp-ord">{{ i + 1 }}.</span>
            <span class="qp-id">#{{ q.id }}</span>
            <span class="qp-text">{{ q.title || stripLatex(q.contentLatex) }}</span>
            <span class="qp-score">{{ q.totalScore }}分</span>
            <span class="qp-actions">
              <button type="button" class="qp-icon-btn" :disabled="i === 0" title="上移" @click="move(i, -1)">↑</button>
              <button type="button" class="qp-icon-btn" :disabled="i === modelValue.length - 1" title="下移" @click="move(i, 1)">↓</button>
              <button type="button" class="qp-icon-btn danger" title="移除" @click="removeAt(i)">✕</button>
            </span>
          </div>
        </template>
      </div>

      <div class="qp-summary">已选 {{ modelValue.length }} 题 · 共 {{ totalScore }} 分</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { questionApi } from '@/api'
import { stripLatex, typeLabel, typeBadge, QUESTION_TYPES } from '@/utils/question'

const PAGE_SIZE = 50
const SEARCH_DEBOUNCE = 300

const props = defineProps({
  // 绑定完整题目对象数组（而非 ID），右栏才能直接展示题干与分值
  modelValue: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

const pageItems = ref([])          // 当前页题目（服务端返回）
const tags = ref([])
const loading = ref(true)
const loadError = ref('')
const keyword = ref('')
const filter = ref({ type: '', difficulty: '', tag: '' })
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const hasFilter = computed(() =>
  !!keyword.value.trim() || !!filter.value.type || !!filter.value.difficulty || !!filter.value.tag
)

// 并发保护：翻页/搜索快速切换时，只认最后一次请求的结果
let reqSeq = 0

async function loadPage() {
  const seq = ++reqSeq
  loading.value = true
  loadError.value = ''
  try {
    const res = await questionApi.listSummaryPaged({
      keyword: keyword.value.trim() || undefined,
      questionType: filter.value.type || undefined,
      difficulty: filter.value.difficulty || undefined,
      tagId: filter.value.tag || undefined,
      page: page.value,
      size: PAGE_SIZE
    })
    if (seq !== reqSeq) return          // 已有更新的请求，丢弃这次结果
    const d = res.data || {}
    pageItems.value = d.content || []
    totalPages.value = d.totalPages || 0
    totalElements.value = d.totalElements || 0
    // 服务端夹紧后页码可能落在范围外（例如筛选后总页数变少）
    if (page.value > 0 && pageItems.value.length === 0 && totalPages.value > 0) {
      page.value = Math.min(page.value, totalPages.value - 1)
      await loadPage()
    }
  } catch (e) {
    if (seq !== reqSeq) return
    loadError.value = '题库加载失败，请重试'
    pageItems.value = []
  } finally {
    if (seq === reqSeq) loading.value = false
  }
}

async function loadTags() {
  try {
    tags.value = (await questionApi.getTags()).data || []
  } catch (e) { /* 标签加载失败不影响选题 */ }
}

// 重试按钮：重新拉当前页
function loadData() { loadPage() }

onMounted(() => { loadTags(); loadPage() })

// 搜索防抖；筛选项变化立即生效。两者都要回到第 1 页，否则会停在越界页码上
let debounceTimer = null
watch(keyword, () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => { page.value = 0; loadPage() }, SEARCH_DEBOUNCE)
})
watch(() => [filter.value.type, filter.value.difficulty, filter.value.tag], () => {
  page.value = 0
  loadPage()
})
onBeforeUnmount(() => clearTimeout(debounceTimer))

function goPage(p) {
  if (p < 0 || p > totalPages.value - 1 || loading.value) return
  page.value = p
  loadPage()
}

const selectedIds = computed(() => new Set(props.modelValue.map(q => q.id)))
const totalScore = computed(() =>
  props.modelValue.reduce((sum, q) => sum + (Number(q.totalScore) || 0), 0)
)

// 始终发出新数组，不直接修改 props
function setSelection(next) { emit('update:modelValue', next) }

function isPicked(q) { return selectedIds.value.has(q.id) }

function toggle(q) {
  isPicked(q)
    ? setSelection(props.modelValue.filter(x => x.id !== q.id))
    : setSelection([...props.modelValue, q])
}

function removeAt(i) { setSelection(props.modelValue.filter((_, idx) => idx !== i)) }

function clearAll() { setSelection([]) }

// 只追加本页中尚未选中的题目，不产生重复。
// 分页后服务端一次只返回一页，"全选"的语义限定为当前页。
function addAllOnPage() {
  const additions = pageItems.value.filter(q => !isPicked(q))
  if (additions.length) setSelection([...props.modelValue, ...additions])
}

function move(i, delta) {
  const j = i + delta
  if (j < 0 || j >= props.modelValue.length) return
  const next = [...props.modelValue]
  ;[next[i], next[j]] = [next[j], next[i]]
  setSelection(next)
}

function resetFilters() {
  // 先取消待触发的搜索防抖，避免与下面 filter 的 watch 重复发一次请求
  clearTimeout(debounceTimer)
  keyword.value = ''
  const unchanged = !filter.value.type && !filter.value.difficulty && !filter.value.tag
  filter.value = { type: '', difficulty: '', tag: '' }
  // filter 无变化时 watch 不会触发，这里手动重载
  if (unchanged) { page.value = 0; loadPage() }
}
</script>

<style scoped>
.qp { display: grid; grid-template-columns: 1.35fr 1fr; gap: 12px; }
@media (max-width: 760px) { .qp { grid-template-columns: 1fr; } }

.qp-pane {
  display: flex; flex-direction: column;
  border: 1px solid var(--c-border); border-radius: var(--radius);
  background: var(--c-surface); overflow: hidden; min-width: 0;
}
.qp-pane-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 12px; border-bottom: 1px solid var(--c-border); background: var(--c-surface2);
}
.qp-pane-title { font-size: 12px; font-weight: 500; color: var(--c-text2); }

.qp-toolbar { padding: 8px 10px; border-bottom: 1px solid var(--c-border); display: flex; flex-direction: column; gap: 6px; }
.qp-filters { display: flex; gap: 6px; flex-wrap: wrap; }
.qp-filters .form-control { width: auto; flex: 1 1 88px; min-width: 0; padding: 5px 8px; font-size: 12px; }

.qp-list { height: 340px; overflow-y: auto; }

.qp-row {
  display: flex; align-items: center; gap: 8px;
  padding: 7px 10px; border-bottom: 1px solid var(--c-border);
  font-size: 12px; cursor: pointer; transition: background .12s; min-width: 0;
}
.qp-row:last-child { border-bottom: none; }
.qp-row:hover { background: var(--c-surface2); }
.qp-row.picked { background: var(--c-primary-bg); }
.qp-row input[type="checkbox"] { margin: 0; cursor: pointer; accent-color: var(--c-primary); flex-shrink: 0; }

.qp-id { color: var(--c-text3); flex-shrink: 0; width: 38px; }
.qp-ord { color: var(--c-text3); flex-shrink: 0; width: 20px; text-align: right; }
.qp-text { flex: 1 1 auto; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.qp-score { color: var(--c-text3); flex-shrink: 0; width: 38px; text-align: right; }
.qp-row .badge, .qp-row .diff-dots { flex-shrink: 0; }

.qp-actions { display: flex; gap: 4px; flex-shrink: 0; }
.qp-icon-btn {
  border: 1px solid var(--c-border-med); background: var(--c-surface);
  border-radius: var(--radius-sm); width: 22px; height: 22px; line-height: 1;
  cursor: pointer; color: var(--c-text2); font-size: 11px; padding: 0;
}
.qp-icon-btn:hover:not(:disabled) { background: var(--c-surface2); }
.qp-icon-btn:disabled { opacity: .35; cursor: not-allowed; }
.qp-icon-btn.danger { color: var(--c-danger); }

.qp-pane-foot, .qp-summary {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 7px 12px; border-top: 1px solid var(--c-border); background: var(--c-surface2);
  font-size: 12px; color: var(--c-text2);
}
.qp-pane-foot .text-sm { min-width: 0; overflow: hidden; text-overflow: ellipsis; }
.qp-pager { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.qp-summary { justify-content: flex-end; font-weight: 500; }
.qp-empty { text-align: center; padding: 40px 12px; color: var(--c-text3); font-size: 12px; }
</style>
