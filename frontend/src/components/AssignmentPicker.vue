<template>
  <div class="ap">
    <!-- 左栏：候选作业 -->
    <div class="ap-pane">
      <div class="ap-pane-head">
        <span class="ap-pane-title">可选作业</span>
        <span class="ap-muted">共 {{ totalElements }} 份</span>
      </div>

      <div class="ap-toolbar">
        <input v-model="keyword" class="ap-input" placeholder="搜索作业标题…" />
        <div class="ap-filters">
          <select v-model="classGroupId" class="ap-input">
            <option :value="null">全部班级</option>
            <option v-for="cg in classGroups" :key="cg.id" :value="cg.id">{{ cg.name }}</option>
          </select>
          <label class="ap-check">
            <input type="checkbox" v-model="onlyOngoing" />
            只看进行中
          </label>
          <button type="button" class="ap-btn-sm" @click="resetFilters">重置</button>
        </div>
      </div>

      <div class="ap-list">
        <div v-if="loading" class="ap-state">加载中…</div>
        <div v-else-if="loadError" class="ap-state">
          <div style="margin-bottom:8px">{{ loadError }}</div>
          <button type="button" class="ap-btn-sm" @click="load">重试</button>
        </div>
        <div v-else-if="!items.length && hasFilter" class="ap-state">
          没有匹配的作业，试试调整筛选条件
        </div>
        <div v-else-if="!items.length" class="ap-state">
          暂无已发布的作业
        </div>
        <template v-else>
          <label v-for="a in items" :key="a.id" :class="['ap-row', isPicked(a.id) ? 'picked' : '']">
            <input type="checkbox" :checked="isPicked(a.id)" @change="toggle(a)" />
            <span class="ap-text">
              <span class="ap-title">{{ a.title }}</span>
              <span class="ap-sub">
                <span v-if="a.classGroupName" class="ap-tag">{{ a.classGroupName }}</span>
                <span v-if="a.questionCount">{{ a.questionCount }} 题</span>
                <span :class="['ap-due', isOverdue(a) ? 'overdue' : '']">{{ dueLabel(a) }}</span>
              </span>
            </span>
          </label>
        </template>
      </div>

      <div class="ap-pane-foot">
        <span class="ap-pager">
          <button type="button" class="ap-icon-btn" :disabled="page === 0 || loading"
                  title="上一页" @click="goPage(page - 1)">‹</button>
          <span class="ap-muted">{{ totalPages === 0 ? 0 : page + 1 }} / {{ totalPages }}</span>
          <button type="button" class="ap-icon-btn" :disabled="page >= totalPages - 1 || loading"
                  title="下一页" @click="goPage(page + 1)">›</button>
        </span>
        <!-- 刻意是「全选本页」而非「全选」：作用范围可见可控，
             避免在几百份作业里误点一次「全选」 -->
        <button type="button" class="ap-btn-sm" :disabled="!items.length" @click="addAllOnPage">
          全选本页
        </button>
      </div>
    </div>

    <!-- 右栏：已选作业 -->
    <div class="ap-pane">
      <div class="ap-pane-head">
        <span class="ap-pane-title">已选作业（{{ modelValue.length }}）</span>
        <button type="button" class="ap-btn-sm" :disabled="!modelValue.length" @click="clearAll">清空</button>
      </div>

      <div class="ap-list">
        <div v-if="!modelValue.length" class="ap-state">从左侧选择作业</div>
        <template v-else>
          <div v-for="id in modelValue" :key="id" class="ap-row">
            <span class="ap-text">
              <span class="ap-title">{{ titleOf(id) }}</span>
            </span>
            <button type="button" class="ap-icon-btn danger" title="移除" @click="remove(id)">✕</button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { assignmentApi } from '@/api'

const props = defineProps({
  /** 已选作业 ID 数组 */
  modelValue: { type: Array, default: () => [] },
  /** 班级下拉选项，由父组件传入（GoalModal 已经加载过，不重复请求） */
  classGroups: { type: Array, default: () => [] },
})
const emit = defineEmits(['update:modelValue'])

const items = ref([])
const loading = ref(false)
const loadError = ref('')
const page = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)

const keyword = ref('')
const classGroupId = ref(null)
const onlyOngoing = ref(false)

const PAGE_SIZE = 20

/**
 * 已选作业的标题缓存：id -> title。
 *
 * 右栏要显示已选作业的名字，但翻页/筛选后这些作业可能不在当前页的 items 里，
 * 光靠 items 查不到。所以每次见到作业就记下标题；
 * 编辑既有目标时初始选中的 id 可能一次都没出现在候选页中 —— 此时回退显示
 * 「作业#12」，而不是留空让用户不知道选了什么。
 */
const titleCache = ref({})

const hasFilter = computed(() =>
  !!keyword.value.trim() || classGroupId.value !== null || onlyOngoing.value
)

function isPicked(id) {
  return props.modelValue.includes(id)
}

function titleOf(id) {
  return titleCache.value[id] || ('作业#' + id)
}

function toggle(a) {
  const next = isPicked(a.id)
    ? props.modelValue.filter(x => x !== a.id)
    : [...props.modelValue, a.id]
  emit('update:modelValue', next)
}

function remove(id) {
  emit('update:modelValue', props.modelValue.filter(x => x !== id))
}

function addAllOnPage() {
  const ids = items.value.map(a => a.id)
  const merged = [...new Set([...props.modelValue, ...ids])]
  emit('update:modelValue', merged)
}

function clearAll() {
  emit('update:modelValue', [])
}

function isOverdue(a) {
  return !!a.dueTime && new Date(a.dueTime) < new Date()
}

function dueLabel(a) {
  if (!a.dueTime) return '无截止'
  const d = new Date(a.dueTime)
  const s = `${d.getMonth() + 1}/${d.getDate()} 截止`
  return isOverdue(a) ? s + '（已过）' : s
}

function resetFilters() {
  keyword.value = ''
  classGroupId.value = null
  onlyOngoing.value = false
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const resp = await assignmentApi.page({
      keyword: keyword.value.trim() || undefined,
      classGroupId: classGroupId.value ?? undefined,
      onlyOngoing: onlyOngoing.value,
      page: page.value,
      size: PAGE_SIZE,
    })
    const data = resp?.data ?? resp
    items.value = data?.content || []
    totalPages.value = data?.totalPages || 0
    totalElements.value = data?.totalElements || 0
    for (const a of items.value) {
      titleCache.value[a.id] = a.title
    }
  } catch (e) {
    loadError.value = e?.isTimeout ? e.message : '加载作业失败'
    items.value = []
  } finally {
    loading.value = false
  }
}

function goPage(p) {
  if (p < 0 || (totalPages.value && p >= totalPages.value)) return
  page.value = p
  load()
}

// 筛选条件变化时回到第一页；关键词做防抖，避免每敲一个字都请求
let timer = null
watch([keyword, classGroupId, onlyOngoing], () => {
  clearTimeout(timer)
  timer = setTimeout(() => {
    page.value = 0
    load()
  }, 300)
})

onMounted(load)
</script>

<style scoped>
.ap { display: grid; grid-template-columns: 1fr 260px; gap: 10px; }
.ap-pane {
  border: 0.5px solid #ddd; border-radius: 8px; background: #fff;
  display: flex; flex-direction: column; min-height: 280px; max-height: 340px;
}
.ap-pane-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px; border-bottom: 0.5px solid #eee;
}
.ap-pane-title { font-size: 12px; font-weight: 500; color: #333; }
.ap-muted { font-size: 11px; color: #999; }

.ap-toolbar { padding: 8px 10px; border-bottom: 0.5px solid #eee; }
.ap-input {
  width: 100%; border: 0.5px solid #ddd; border-radius: 6px;
  padding: 5px 8px; font-size: 12px; outline: none; background: #fff;
}
.ap-input:focus { border-color: #1D9E75; }
.ap-filters { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
.ap-filters .ap-input { flex: 1; }
.ap-check { font-size: 11px; color: #666; display: flex; align-items: center; gap: 4px; white-space: nowrap; cursor: pointer; }
.ap-btn-sm {
  border: 0.5px solid #ddd; background: #fff; border-radius: 6px;
  padding: 4px 8px; font-size: 11px; color: #555; cursor: pointer; white-space: nowrap;
}
.ap-btn-sm:hover:not(:disabled) { background: #f5f5f3; }
.ap-btn-sm:disabled { color: #ccc; cursor: default; }

.ap-list { flex: 1; overflow-y: auto; padding: 4px 0; }
.ap-state { text-align: center; padding: 28px 12px; font-size: 12px; color: #aaa; }

.ap-row {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 10px; font-size: 12px; cursor: pointer;
}
.ap-row:hover { background: #fafaf9; }
.ap-row.picked { background: #E1F5EE; }
.ap-text { flex: 1; min-width: 0; }
.ap-title {
  display: block; color: #222;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ap-sub { display: flex; gap: 8px; align-items: center; margin-top: 2px; font-size: 11px; color: #999; }
.ap-tag { background: #f0f0ee; border-radius: 4px; padding: 1px 5px; color: #666; }
.ap-due.overdue { color: #C2410C; }

.ap-pane-foot {
  display: flex; align-items: center; justify-content: space-between;
  padding: 6px 10px; border-top: 0.5px solid #eee;
}
.ap-pager { display: flex; align-items: center; gap: 8px; }
.ap-icon-btn {
  border: 0.5px solid #ddd; background: #fff; border-radius: 5px;
  width: 22px; height: 22px; line-height: 1; font-size: 12px; color: #666; cursor: pointer;
}
.ap-icon-btn:hover:not(:disabled) { background: #f5f5f3; }
.ap-icon-btn:disabled { color: #ddd; cursor: default; }
.ap-icon-btn.danger { color: #B91C1C; border-color: #FCA5A5; }
.ap-icon-btn.danger:hover { background: #FEF2F2; }

/* 断点按视口算，但弹窗宽度是 min(760px, 95vw)、内容区还要再减 44px 内边距，
   两者不是一回事：视口 600px 时容器只有 ~526px，若按 560px 断点此时仍是双栏，
   左栏会被挤到 ~256px。取 660px 让容器降到 ~583px 前就转单栏。 */
@media (max-width: 660px) {
  .ap { grid-template-columns: 1fr; }
  /* 单栏时两块各自压缩，避免整体过高需要滚动 */
  .ap-pane { min-height: 180px; max-height: 220px; }
}
</style>
