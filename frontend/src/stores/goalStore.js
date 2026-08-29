import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { goalApi } from '@/api/goalApi'
import { useAuthStore } from '@/store/auth'

export const useGoalStore = defineStore('goal', () => {
  const goals = ref([])
  const stats = ref({ totalParent: 0, totalSub: 0, done: 0, late: 0, avgProgress: 0 })
  const loading = ref(false)
  const filterStatus = ref('ALL')
  const keyword = ref('')
  const expandedGoals = ref(new Set())
  const loadedDepths = ref(new Map())
  // 评论缓存：key=goalId
  const comments = ref({})
  // 学生概览缓存
  const studentOverviews = ref({})

  // 当前用户的角色
  const authStore = useAuthStore()
  const isTeacherView = computed(() => authStore.isTeacher() || authStore.isAdmin())
  const isStudentView = computed(() => !authStore.isTeacher() && !authStore.isAdmin())

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
      if (isStudentView.value) {
        // 学生只看分配给我的目标
        goals.value = await goalApi.listMy()
      } else {
        goals.value = await goalApi.list()
      }
      stats.value = await goalApi.stats()

      // 设置默认展开状态
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
    if (result.subGoals && result.subGoals.length > 0) {
      expandedGoals.value.add(result.id)
    }
    return result
  }

  async function updateGoal(id, data) {
    const result = await goalApi.update(id, data)
    await fetchGoals()
    if (result.subGoals && result.subGoals.length > 0) {
      expandedGoals.value.add(result.id)
    }
    return result
  }

  async function deleteGoal(id) {
    await goalApi.remove(id)
    await fetchGoals()
  }

  /**
   * 递归在 goals 树中查找指定 ID 的目标节点并更新其字段
   */
  function updateGoalNodeLocally(goalsList, goalId, updates) {
    for (const g of goalsList) {
      if (g.id === goalId) {
        Object.assign(g, updates)
        return true
      }
      if (g.subGoals && g.subGoals.length > 0) {
        if (updateGoalNodeLocally(g.subGoals, goalId, updates)) return true
      }
    }
    return false
  }

  function findGoalNode(goalsList, goalId) {
    for (const g of goalsList) {
      if (g.id === goalId) {
        return g
      }
      if (g.subGoals && g.subGoals.length > 0) {
        const found = findGoalNode(g.subGoals, goalId)
        if (found) return found
      }
    }
    return null
  }

  /**
   * 学生更新自己在某个目标上的个人进度（乐观更新，不刷新全量）
   */
  async function updateMyProgress(goalId, data) {
    // 先乐观更新本地数据
    const updates = {}
    if (data.progress !== undefined) {
      updates.studentProgress = data.progress
    }
    if (data.actualStart !== undefined) {
      updates.myActualStart = data.actualStart
    }
    if (data.actualEnd !== undefined) {
      updates.myActualEnd = data.actualEnd
    }
    updateGoalNodeLocally(goals.value, goalId, updates)

    // 再调用 API 持久化
    try {
      await goalApi.updateMyProgress(goalId, data)
    } catch (e) {
      // 超时不回滚，理由同 toggleCopyable：请求可能已在服务端生效。
      // 且冷启动时 fetchGoals 同样会超时，回滚本身也做不成。
      if (!e.isTimeout) {
        await fetchGoals()
      }
      throw e
    }
  }

  async function toggleCopyable(goalId, copyable) {
    const currentGoal = findGoalNode(goals.value, goalId)
    const previousCopyable = currentGoal?.copyable
    updateGoalNodeLocally(goals.value, goalId, { copyable })

    try {
      await goalApi.toggleCopyable(goalId, copyable)
    } catch (e) {
      // 超时不回滚：axios 超时只代表客户端不再等待，请求可能仍在服务端执行并成功。
      // 若照常回滚，界面会显示 OFF 而数据库是 ON，刷新后状态突变。
      // 保留乐观值，由调用方提示用户刷新确认。
      if (currentGoal && !e.isTimeout) {
        currentGoal.copyable = previousCopyable
      }
      throw e
    }
  }

  async function loadSubGoals(goalId, depth = 2) {
    if (loadedDepths.value.get(goalId) >= depth) return
    try {
      const subs = await goalApi.loadSubGoals(goalId, depth)
      const goal = goals.value.find(g => g.id === goalId)
      if (goal) {
        goal.subGoals = subs
        loadedDepths.value.set(goalId, depth)
      }
    } catch (err) {
      console.error('加载子目标失败:', err)
    }
  }

  function toggleExpanded(goalId) {
    if (expandedGoals.value.has(goalId)) {
      expandedGoals.value.delete(goalId)
    } else {
      expandedGoals.value.add(goalId)
      const goal = goals.value.find(g => g.id === goalId)
      if (goal && (!goal.subGoals || goal.subGoals.length === 0)) {
        loadSubGoals(goalId, 2)
      }
    }
  }

  // ====== 评论 ======

  async function fetchComments(goalId) {
    try {
      const data = await goalApi.getComments(goalId)
      comments.value[goalId] = data
      return data
    } catch (err) {
      console.error('获取评论失败:', err)
      return []
    }
  }

  async function addComment(goalId, data) {
    const result = await goalApi.addComment(goalId, data)
    // 刷新评论列表
    await fetchComments(goalId)
    return result
  }

  async function updateComment(goalId, commentId, data) {
    const result = await goalApi.updateComment(goalId, commentId, data)
    await fetchComments(goalId)
    return result
  }

  async function deleteComment(goalId, commentId) {
    await goalApi.deleteComment(goalId, commentId)
    await fetchComments(goalId)
  }

  // ====== 私密评论 ======

  async function fetchPrivateComments(goalId) {
    try {
      const data = await goalApi.getPrivateComments(goalId)
      comments.value['private_' + goalId] = data
      return data
    } catch (err) {
      console.error('获取私密评论失败:', err)
      return []
    }
  }

  async function addPrivateComment(goalId, data) {
    const result = await goalApi.addPrivateComment(goalId, data)
    await fetchPrivateComments(goalId)
    return result
  }

  async function deletePrivateComment(goalId, commentId) {
    await goalApi.deletePrivateComment(goalId, commentId)
    await fetchPrivateComments(goalId)
  }

  // ====== 学生概览（老师视角） ======

  async function fetchStudentOverview(goalId) {
    try {
      const data = await goalApi.getStudentOverview(goalId)
      studentOverviews.value[goalId] = data
      return data
    } catch (err) {
      console.error('获取学生概览失败:', err)
      return null
    }
  }

  // ====== 关联作业 ======

  async function updateAssignments(goalId, assignmentIds) {
    await goalApi.updateAssignments(goalId, assignmentIds)
    await fetchGoals()
  }

  return {
    goals,
    stats,
    loading,
    filterStatus,
    keyword,
    filteredGoals,
    isTeacherView,
    isStudentView,
    comments,
    studentOverviews,
    fetchGoals,
    createGoal,
    updateGoal,
    deleteGoal,
    updateMyProgress,
    toggleCopyable,
    expandedGoals,
    loadedDepths,
    loadSubGoals,
    toggleExpanded,
    // 评论
    fetchComments,
    addComment,
    updateComment,
    deleteComment,
    // 私密评论
    fetchPrivateComments,
    addPrivateComment,
    deletePrivateComment,
    // 学生概览
    fetchStudentOverview,
    // 关联作业
    updateAssignments,
  }
})