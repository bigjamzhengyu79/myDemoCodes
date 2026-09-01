<template>
  <div class="page">
    <div class="top-bar">
      <div>
        <h1 class="page-title">目标模板库</h1>
        <p class="page-sub">所有老师标记为「可复制」的目标都在这里，可展开查看完整结构后再使用</p>
      </div>
      <button class="btn-back" @click="$router.push('/goals')">← 返回目标管理</button>
    </div>

    <div class="toolbar">
      <input v-model="keyword" class="search" placeholder="搜索模板名称、作者…" />
      <label class="mine-toggle">
        <input type="checkbox" v-model="hideMine" />
        只看其他老师的模板
      </label>
      <span class="count">共 {{ filtered.length }} 个模板</span>
    </div>

    <div v-if="loading" class="state">加载中…</div>

    <div v-else-if="!templates.length" class="state empty">
      <div class="empty-icon">📋</div>
      <div class="empty-text">还没有可复制的模板</div>
      <div class="empty-hint">在目标管理页把某个父目标的「📋」按钮切换为 ON，它就会出现在这里，供所有老师复用。</div>
    </div>

    <div v-else-if="!filtered.length" class="state empty">
      <div class="empty-text">没有匹配的模板</div>
    </div>

    <div v-else class="card-list">
      <div v-for="t in filtered" :key="t.id" class="tpl-card">
        <div class="tpl-head" @click="toggle(t.id)">
          <span class="toggle">{{ expanded.has(t.id) ? '▾' : '▸' }}</span>
          <div class="tpl-main">
            <div class="tpl-title-row">
              <span class="tpl-title">{{ t.title }}</span>
              <span class="author-badge" :class="{ mine: isMine(t) }">{{ authorOf(t) }}</span>
            </div>
            <div class="tpl-meta">
              <span>{{ countSubGoals(t) }} 个子目标</span>
              <span>{{ t.assignmentIds?.length || 0 }} 个关联作业</span>
              <span v-if="t.owners">实施者：{{ t.owners }}</span>
            </div>
            <p v-if="t.description" class="tpl-desc">{{ t.description }}</p>
          </div>
          <button class="btn-use" @click.stop="use(t)" :disabled="copyingId === t.id">
            {{ copyingId === t.id ? '复制中…' : '使用此模板' }}
          </button>
        </div>

        <Transition name="slide">
          <div v-if="expanded.has(t.id)" class="tpl-body">
            <div v-if="t.assignmentTitles?.length" class="section">
              <div class="section-label">📝 原作者关联的作业（仅供参考，不会复制）</div>
              <div class="assignment-tags">
                <span v-for="(name, i) in t.assignmentTitles" :key="i" class="tag">{{ name }}</span>
              </div>
            </div>

            <div class="section">
              <div class="section-label">🗂 目标结构</div>
              <div v-if="!t.subGoals?.length" class="no-subs">该模板没有子目标</div>
              <ul v-else class="tree">
                <GoalTemplateNode v-for="s in t.subGoals" :key="s.id" :node="s" />
              </ul>
            </div>

            <div class="copy-note">
              复制后只带来<strong>标题、描述与子目标结构</strong>；
              学生、班级与关联作业都不会继承 —— 它们属于原作者，需要你自己指定。
            </div>
          </div>
        </Transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { goalApi } from '@/api/goalApi'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

const templates = ref([])
const loading = ref(true)
const keyword = ref('')
const hideMine = ref(false)
const expanded = ref(new Set())
const copyingId = ref(null)

/**
 * 递归渲染子目标树。
 * 用函数式组件而不是单独的 .vue 文件：这棵树只在本页出现，
 * 结构也简单（标题 + 层级缩进），单独开文件反而更难找。
 */
const GoalTemplateNode = (props) => {
  const node = props.node
  return h('li', { class: 'tree-node' }, [
    h('span', { class: 'node-title' }, node.title),
    node.assignmentIds?.length
      ? h('span', { class: 'node-tag' }, `${node.assignmentIds.length} 个作业`)
      : null,
    node.subGoals?.length
      ? h('ul', { class: 'tree' },
          node.subGoals.map(s => h(GoalTemplateNode, { node: s, key: s.id })))
      : null,
  ])
}

function isMine(t) {
  return t.managerId && t.managerId === authStore.user?.id
}

function authorOf(t) {
  if (isMine(t)) return '我创建的'
  return t.managerName || '未知作者'
}

/** 递归统计整棵树的子目标数（不含根节点自身） */
function countSubGoals(t) {
  if (!t.subGoals?.length) return 0
  return t.subGoals.reduce((sum, s) => sum + 1 + countSubGoals(s), 0)
}

const filtered = computed(() => {
  let list = templates.value
  if (hideMine.value) {
    list = list.filter(t => !isMine(t))
  }
  const kw = keyword.value.trim()
  if (kw) {
    list = list.filter(t =>
      t.title?.includes(kw) ||
      t.description?.includes(kw) ||
      authorOf(t).includes(kw)
    )
  }
  return list
})

function toggle(id) {
  // Set 是响应式的，但直接 add/delete 不会触发模板更新，需换新引用
  const next = new Set(expanded.value)
  next.has(id) ? next.delete(id) : next.add(id)
  expanded.value = next
}

async function fetchTemplates() {
  loading.value = true
  try {
    templates.value = await goalApi.listCopyable()
  } catch (e) {
    alert(e.isTimeout ? e.message : '加载模板失败：' + (e.message || '未知错误'))
    templates.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 直接复制该模板，成功后回到目标管理页。
 * 复制品归当前用户所有，学生/班级/作业均为空，需用户自行补充 —— 提示语必须说清楚。
 */
async function use(t) {
  if (!confirm(`确定使用模板「${t.title}」？\n\n将为你创建一份副本（含完整子目标结构）。\n学生、班级与关联作业不会复制，需要你在目标管理页自行指定。`)) {
    return
  }
  copyingId.value = t.id
  try {
    await goalApi.copyGoal(t.id)
    alert('已创建副本，请在目标管理页为其指定学生、班级与关联作业。')
    router.push('/goals')
  } catch (e) {
    // 超时的结果未知，不能说成失败（同 GoalCard 的处理）
    alert(e.isTimeout ? e.message : '复制失败：' + (e.message || '未知错误'))
  } finally {
    copyingId.value = null
  }
}

onMounted(fetchTemplates)
</script>

<style scoped>
.page { max-width: 900px; margin: 0 auto; padding: 2rem 1rem; font-family: sans-serif; }
.top-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 1.25rem; }
.page-title { font-size: 20px; font-weight: 500; color: #111; }
.page-sub { font-size: 13px; color: #888; margin-top: 2px; }
.btn-back {
  background: transparent; border: 0.5px solid #ddd; border-radius: 8px;
  padding: 8px 14px; font-size: 13px; color: #555; cursor: pointer;
}
.btn-back:hover { background: #f5f5f3; }

.toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 1rem; }
.search {
  flex: 1; border: 0.5px solid #ddd; border-radius: 8px;
  padding: 8px 12px; font-size: 13px; outline: none;
}
.search:focus { border-color: #1D9E75; }
.mine-toggle { font-size: 12px; color: #666; display: flex; align-items: center; gap: 5px; cursor: pointer; }
.count { font-size: 12px; color: #999; white-space: nowrap; }

.state { text-align: center; padding: 48px 16px; color: #999; font-size: 13px; }
.empty-icon { font-size: 28px; margin-bottom: 10px; }
.empty-text { font-weight: 500; color: #666; margin-bottom: 6px; }
.empty-hint { font-size: 12px; color: #aaa; max-width: 420px; margin: 0 auto; line-height: 1.6; }

.card-list { display: flex; flex-direction: column; gap: 10px; }
.tpl-card { background: #fff; border: 0.5px solid #ddd; border-radius: 12px; overflow: hidden; }
.tpl-head { display: flex; align-items: flex-start; gap: 10px; padding: 14px 16px; cursor: pointer; }
.tpl-head:hover { background: #fafaf9; }
.toggle { color: #aaa; font-size: 12px; padding-top: 3px; }
.tpl-main { flex: 1; min-width: 0; }
.tpl-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tpl-title { font-size: 14px; font-weight: 500; color: #111; }
.author-badge {
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
  background: #EEF2FF; color: #4338CA;
}
.author-badge.mine { background: #E1F5EE; color: #0F6E56; }
.tpl-meta { display: flex; gap: 12px; font-size: 12px; color: #888; margin-top: 5px; flex-wrap: wrap; }
.tpl-desc {
  font-size: 12px; color: #777; margin-top: 6px; line-height: 1.5;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.btn-use {
  background: #1D9E75; color: #fff; border: none; border-radius: 8px;
  padding: 7px 14px; font-size: 12px; font-weight: 500; cursor: pointer; white-space: nowrap;
}
.btn-use:hover:not(:disabled) { background: #0F6E56; }
.btn-use:disabled { background: #bbb; cursor: default; }

.tpl-body { border-top: 0.5px solid #eee; padding: 14px 16px; background: #fbfbfa; }
.section { margin-bottom: 14px; }
.section-label { font-size: 12px; font-weight: 500; color: #555; margin-bottom: 6px; }
.assignment-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag { font-size: 11px; background: #fff; border: 0.5px solid #ddd; border-radius: 6px; padding: 3px 8px; color: #555; }
.no-subs { font-size: 12px; color: #aaa; }

.tree { list-style: none; padding-left: 16px; margin: 0; }
.tree-node { font-size: 12px; color: #444; padding: 3px 0; position: relative; }
.tree-node::before {
  content: '·'; position: absolute; left: -11px; color: #bbb;
}
.node-title { color: #333; }
.node-tag { font-size: 10px; color: #888; margin-left: 6px; }

.copy-note {
  font-size: 11px; color: #92400E; background: #FEF3C7;
  border-radius: 6px; padding: 8px 10px; line-height: 1.6;
}

.slide-enter-active, .slide-leave-active { transition: all .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { opacity: 1; max-height: 1200px; }
</style>
