<template>
  <div class="page">
    <div class="top-bar">
      <div>
        <h1 class="page-title">学习目标管理</h1>
        <p class="page-sub">父目标可分解为若干子目标，进度自动汇总</p>
      </div>
      <button class="btn-add" @click="openCreate">+ 新增父目标</button>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">父目标数</div>
        <div class="stat-val">{{ stats?.totalParent ?? 0 }}</div>
        <div class="stat-sub">共 {{ stats?.totalSub ?? 0 }} 个子目标</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-val" style="color:#3B6D11">{{ stats?.done ?? 0 }}</div>
        <div class="stat-sub">完成率 {{ completionRate }}%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均进度</div>
        <div class="stat-val" style="color:#185FA5">{{ stats?.avgProgress ?? 0 }}%</div>
        <div class="stat-sub">全部父目标均值</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已延期</div>
        <div class="stat-val" style="color:#A32D2D">{{ stats?.late ?? 0 }}</div>
        <div class="stat-sub">需关注处理</div>
      </div>
    </div>

    <div class="toolbar">
      <button
        v-for="f in filters" :key="f.value"
        class="filter-btn" :class="{ active: store.filterStatus === f.value }"
        @click="store.filterStatus = f.value"
      >{{ f.label }}</button>
      <input
        v-model="store.keyword"
        class="search-input"
        type="text"
        placeholder="搜索目标…"
      />
    </div>

    <div v-if="store.loading" class="loading">加载中…</div>
    <div v-else-if="!store.filteredGoals.length" class="empty">暂无符合条件的目标</div>
    <div v-else class="goal-list">
      <GoalCard
        v-for="goal in store.filteredGoals"
        :key="goal.id"
        :goal="goal"
        @edit="openEdit"
        @delete="handleDelete"
        @addSub="openAddSub"
        @editSub="openEditSub"
        @deleteSub="handleDeleteSub"
      />
    </div>

    <GoalModal
      :visible="modal.visible"
      :goal-data="modal.goalData"
      :parent-id="modal.parentId"
      :parent-title="modal.parentTitle"
      @close="modal.visible = false"
      @saved="handleSaved"
    />
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { useGoalStore } from '@/stores/goalStore'
import GoalCard from '@/components/GoalCard.vue'
import GoalModal from '@/components/GoalModal.vue'

const store = useGoalStore()
const { stats } = storeToRefs(store)

onMounted(() => store.fetchGoals())

const completionRate = computed(() => {
  const total = stats.value?.totalParent ?? 0
  const done = stats.value?.done ?? 0
  return total ? Math.round((done / total) * 100) : 0
})

const filters = [
  { value: 'ALL', label: '全部' },
  { value: 'TODO', label: '未开始' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'DONE', label: '已完成' },
  { value: 'LATE', label: '已延期' },
]

const modal = reactive({
  visible: false,
  goalData: null,
  parentId: null,
  parentTitle: '',
})

function openCreate() {
  modal.goalData = null
  modal.parentId = null
  modal.parentTitle = ''
  modal.visible = true
}

function openEdit(goal) {
  modal.goalData = goal
  modal.parentId = null
  modal.parentTitle = ''
  modal.visible = true
}

function openAddSub(parentGoal) {
  modal.goalData = null
  modal.parentId = parentGoal.id
  modal.parentTitle = parentGoal.title
  modal.visible = true
}

function openEditSub({ parentId, sub }) {
  modal.goalData = sub
  modal.parentId = parentId
  modal.parentTitle = store.goals.find(g => g.id === parentId)?.title || ''
  modal.visible = true
}

async function handleSaved({ id, payload }) {
  if (id) {
    await store.updateGoal(id, payload)
  } else {
    await store.createGoal(payload)
  }
  modal.visible = false
}

async function handleDelete(id) {
  if (confirm('删除父目标将同时删除所有子目标，确认继续？')) {
    await store.deleteGoal(id)
  }
}

async function handleDeleteSub({ subId }) {
  if (confirm('确认删除该子目标？')) {
    await store.deleteGoal(subId)
  }
}
</script>

<style scoped>
.page { max-width: 900px; margin: 0 auto; padding: 2rem 1rem; font-family: sans-serif; }
.top-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; }
.page-title { font-size: 20px; font-weight: 500; color: #111; }
.page-sub { font-size: 13px; color: #888; margin-top: 2px; }
.btn-add {
  background: #1D9E75; color: #fff; border: none; border-radius: 8px;
  padding: 8px 16px; font-size: 13px; font-weight: 500; cursor: pointer;
}
.btn-add:hover { background: #0F6E56; }
.stats-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 10px; margin-bottom: 1.25rem; }
.stat-card { background: #f5f5f3; border-radius: 8px; padding: 12px 14px; }
.stat-label { font-size: 11px; color: #888; text-transform: uppercase; letter-spacing: .06em; margin-bottom: 5px; }
.stat-val { font-size: 22px; font-weight: 500; color: #111; }
.stat-sub { font-size: 11px; color: #888; margin-top: 2px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 1rem; align-items: center; flex-wrap: wrap; }
.filter-btn {
  border: 0.5px solid #ccc; background: transparent; border-radius: 20px;
  padding: 5px 14px; font-size: 12px; cursor: pointer; color: #888;
}
.filter-btn.active { background: #1D9E75; color: #fff; border-color: #1D9E75; }
.filter-btn:hover:not(.active) { background: #f5f5f3; }
.search-input {
  border: 0.5px solid #ccc; border-radius: 8px; padding: 5px 12px;
  font-size: 12px; color: #111; background: #fff; margin-left: auto; width: 180px;
}
.goal-list { display: flex; flex-direction: column; gap: 12px; }
.loading, .empty { text-align: center; padding: 2rem; color: #aaa; font-size: 13px; }
</style>
