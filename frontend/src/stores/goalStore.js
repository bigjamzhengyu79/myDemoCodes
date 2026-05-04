import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { goalApi } from '@/api/goalApi'

export const useGoalStore = defineStore('goal', () => {
  const goals = ref([])
  const stats = ref({ totalParent: 0, totalSub: 0, done: 0, late: 0, avgProgress: 0 })
  const loading = ref(false)
  const filterStatus = ref('ALL')
  const keyword = ref('')
  const expandedGoals = ref(new Set()) // 记录已展开的目标ID
  const loadedDepths = ref(new Map()) // 记录各目标已加载的深度

  const filteredGoals = computed(() => {
    return goals.value.filter(g => {
      const matchStatus = filterStatus.value === 'ALL' || g.status === filterStatus.value
      const kw = keyword.value.trim()
      const matchKw = !kw ||
        g.title.includes(kw) ||
        (g.owners || '').includes(kw) ||
        g.subGoals?.some(s => s.title.includes(kw))
      return matchStatus && matchKw
    })
  })

  async function fetchGoals() {
    loading.value = true
    try {
      goals.value = await goalApi.list()
      stats.value = await goalApi.stats()
      
      // 设置默认展开状态：有子目标的目标默认展开
      goals.value.forEach(goal => {
        if (goal.subGoals && goal.subGoals.length > 0) {
          expandedGoals.value.add(goal.id)
        }
      })
    } finally {
      loading.value = false
    }
  }

  async function createGoal(data) {
    const result = await goalApi.create(data)
    await fetchGoals()
    // 如果新创建的目标有子目标，设置为展开状态
    if (result.subGoals && result.subGoals.length > 0) {
      expandedGoals.value.add(result.id)
    }
    return result
  }

  async function updateGoal(id, data) {
    const result = await goalApi.update(id, data)
    await fetchGoals()
    // 如果更新后的目标有子目标，设置为展开状态
    if (result.subGoals && result.subGoals.length > 0) {
      expandedGoals.value.add(result.id)
    }
    return result
  }

  async function deleteGoal(id) {
    await goalApi.remove(id)
    await fetchGoals()
  }

  // 按需加载子目标
  async function loadSubGoals(goalId, depth = 2) {
    if (loadedDepths.value.get(goalId) >= depth) return // 已加载过
    try {
      const subs = await goalApi.loadSubGoals(goalId, depth)
      // 更新目标的子目标列表
      const goal = goals.value.find(g => g.id === goalId)
      if (goal) {
        goal.subGoals = subs
        loadedDepths.value.set(goalId, depth)
      }
    } catch (err) {
      console.error('加载子目标失败:', err)
    }
  }

  // 切换展开状态
  function toggleExpanded(goalId) {
    if (expandedGoals.value.has(goalId)) {
      expandedGoals.value.delete(goalId)
    } else {
      expandedGoals.value.add(goalId)
      // 展开时确保子目标已加载（如果还没有加载）
      const goal = goals.value.find(g => g.id === goalId)
      if (goal && (!goal.subGoals || goal.subGoals.length === 0)) {
        loadSubGoals(goalId, 2)
      }
    }
  }

  return {
    goals,
    stats,
    loading,
    filterStatus,
    keyword,
    filteredGoals,
    fetchGoals,
    createGoal,
    updateGoal,
    deleteGoal,
    expandedGoals,
    loadedDepths,
    loadSubGoals,
    toggleExpanded,
  }
})
