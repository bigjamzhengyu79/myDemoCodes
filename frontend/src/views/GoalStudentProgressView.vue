<template>
  <div class="page">
    <div class="top-bar">
      <button class="btn-back" @click="$router.back()">← 返回</button>
      <div>
        <h1 class="page-title">学生执行情况</h1>
        <p class="page-sub">查看每个目标下所有学生的进度、实际时间和评论</p>
      </div>
    </div>

    <!-- 选择父目标 -->
    <div class="select-section">
      <label>选择目标：</label>
      <select v-model="selectedGoalId" @change="onGoalSelect" class="form-control">
        <option :value="null">请选择一个目标</option>
        <option v-for="g in parentGoals" :key="g.id" :value="g.id">
          {{ g.title }} ({{ g.subGoals?.length || 0 }} 个子目标)
        </option>
      </select>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading">加载中…</div>

    <!-- 概览树 -->
    <div v-else-if="overview" class="overview-tree">
      <GoalStudentNode :data="overview" :depth="0" />
    </div>

    <div v-else-if="selectedGoalId && !loading" class="empty">暂无数据</div>

    <div v-else class="empty">请在上方选择一个目标查看</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useGoalStore } from '@/stores/goalStore'
import { goalApi } from '@/api/goalApi'
import GoalStudentNode from '@/components/GoalStudentNode.vue'

const store = useGoalStore()
const parentGoals = ref([])
const selectedGoalId = ref(null)
const overview = ref(null)
const loading = ref(false)

async function loadParentGoals() {
  try {
    parentGoals.value = await goalApi.list()
  } catch (e) {
    parentGoals.value = []
  }
}

async function onGoalSelect() {
  if (!selectedGoalId.value) {
    overview.value = null
    return
  }
  loading.value = true
  try {
    overview.value = await goalApi.getStudentOverview(selectedGoalId.value)
  } catch (e) {
    overview.value = null
    alert('加载失败：' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadParentGoals()
})
</script>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; padding: 2rem 1rem; font-family: sans-serif; }
.top-bar { display: flex; align-items: center; gap: 12px; margin-bottom: 1.25rem; }
.btn-back {
  border: 0.5px solid #ccc; background: transparent; border-radius: 8px;
  padding: 6px 12px; font-size: 13px; cursor: pointer; color: #666; white-space: nowrap;
}
.btn-back:hover { background: #f5f5f3; }
.page-title { font-size: 20px; font-weight: 500; color: #111; }
.page-sub { font-size: 13px; color: #888; margin-top: 2px; }
.select-section {
  display: flex; align-items: center; gap: 10px; margin-bottom: 1.5rem;
  background: #f5f5f3; border-radius: 8px; padding: 12px 16px;
}
.select-section label { font-size: 13px; color: #666; font-weight: 500; white-space: nowrap; }
.form-control {
  flex: 1; border: 0.5px solid #ccc; border-radius: 8px; padding: 7px 10px; font-size: 13px;
}
.loading, .empty { text-align: center; padding: 3rem; color: #aaa; font-size: 13px; }
.overview-tree { display: flex; flex-direction: column; gap: 12px; }
</style>