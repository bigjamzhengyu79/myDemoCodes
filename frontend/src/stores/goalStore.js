import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { goalApi } from '@/api/goalApi'

export const useGoalStore = defineStore('goal', () => {
  const goals = ref([])
  const stats = ref({ totalParent: 0, totalSub: 0, done: 0, late: 0, avgProgress: 0 })
  const loading = ref(false)
  const filterStatus = ref('ALL')
  const keyword = ref('')

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
    } finally {
      loading.value = false
    }
  }

  async function createGoal(data) {
    await goalApi.create(data)
    await fetchGoals()
  }

  async function updateGoal(id, data) {
    await goalApi.update(id, data)
    await fetchGoals()
  }

  async function deleteGoal(id) {
    await goalApi.remove(id)
    await fetchGoals()
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
  }
})
