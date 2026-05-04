<template>
  <div class="parent-card">
    <div class="parent-header" @click="toggleExpanded">
      <span class="toggle" v-if="goal.subGoals?.length > 0">{{ isExpanded ? '▾' : '▸' }}</span>

      <div class="parent-info">
        <div class="title-row">
          <span class="title">{{ goal.title }}</span>
          <StatusBadge :status="goal.status" />
          <span class="sub-count" :class="{ 'has-subs': goal.subGoals?.length > 0 }">
            {{ goal.subGoals?.length || 0 }} 个子目标
          </span>
          <span class="depth-badge">层级 {{ goal.depth || 1 }}</span>
        </div>

        <div class="meta-row">
          <span>预计：{{ goal.plannedStart }} ~ {{ goal.plannedEnd }}</span>
          <span v-if="goal.actualStart">实际开始：{{ goal.actualStart }}</span>
          <span v-if="goal.actualEnd">实际完成：{{ goal.actualEnd }}</span>
          <span>实施者：{{ goal.owners || '—' }}</span>
        </div>

        <div class="progress-row">
          <div class="bar-bg">
            <div class="bar-fill" :class="`fill-${goal.status.toLowerCase()}`"
                 :style="{ width: goal.progress + '%' }"></div>
          </div>
          <span class="pct">{{ goal.progress }}%</span>
        </div>
      </div>

      <div class="actions" @click.stop>
        <button class="btn-sub" @click="$emit('addSub', goal)">+ 子目标</button>
        <button class="btn-icon" @click="$emit('edit', goal)">编辑</button>
        <button class="btn-icon" @click="$emit('delete', goal.id)">删除</button>
      </div>
    </div>

    <!-- 子目标列表（可展开/收起） -->
    <Transition name="slide">
      <div v-if="isExpanded && goal.subGoals?.length > 0" class="sub-list">
        <SubGoalItem
          v-for="(sub, idx) in goal.subGoals"
          :key="sub.id"
          :goal="sub"
          :parent-id="goal.id"
          :is-last="idx === goal.subGoals.length - 1"
          @add-sub="$emit('addSub', $event)"
          @edit-sub="$emit('editSub', $event)"
          @delete-sub="$emit('deleteSub', $event)"
        />
      </div>
    </Transition>

    <!-- 展开/折叠区域（用于查看更多详情或添加更多子目标） -->
    <Transition name="slide">
      <div v-if="isExpanded" class="expanded-section">
        <div v-if="!goal.subGoals?.length" class="sub-empty">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无子目标</div>
          <div class="empty-action">点击上方的「+ 子目标」按钮开始分解任务</div>
        </div>
        <div v-else class="expand-hint">
          子目标已显示在上面。如需添加更多子目标，请点击「+ 子目标」按钮。
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import StatusBadge from './StatusBadge.vue'
import SubGoalItem from './SubGoalItem.vue'
import { useGoalStore } from '@/stores/goalStore'

const props = defineProps({
  goal: { type: Object, required: true },
})

defineEmits(['edit', 'delete', 'addSub', 'editSub', 'deleteSub'])

const goalStore = useGoalStore()
const isExpanded = computed(() => goalStore.expandedGoals.has(props.goal.id))

function toggleExpanded() {
  goalStore.toggleExpanded(props.goal.id)
}

function toggleSubExpanded(subId) {
  goalStore.toggleExpanded(subId)
}
</script>

<style scoped>
.parent-card {
  background: #fff; border: 0.5px solid #ddd;
  border-radius: 12px; overflow: hidden;
}
.parent-header {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 14px 16px; cursor: pointer; user-select: none;
}
.parent-header:hover { background: #f8f8f7; }
.toggle { font-size: 12px; color: #999; margin-top: 2px; flex-shrink: 0; }
.parent-info { flex: 1; min-width: 0; }
.title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 6px; }
.title { font-size: 14px; font-weight: 500; color: #111; }
.sub-count {
  font-size: 11px; color: #999; background: #f2f2f0;
  border-radius: 10px; padding: 1px 7px; margin-left: 2px;
}
.sub-count.has-subs { color: #1D9E75; background: #E1F5EE; font-weight: 500; }
.depth-badge {
  font-size: 10px; color: #666; background: #e8f4fd;
  border-radius: 8px; padding: 1px 6px;
}
.meta-row {
  display: flex; gap: 14px; font-size: 12px;
  color: #888; flex-wrap: wrap; margin-bottom: 8px;
}
.progress-row { display: flex; align-items: center; gap: 8px; }
.bar-bg { flex: 1; height: 5px; background: #f0f0ee; border-radius: 3px; overflow: hidden; }
.bar-fill { height: 100%; border-radius: 3px; transition: width .3s; }
.fill-todo { background: #1D9E75; }
.fill-in_progress { background: #378ADD; }
.fill-done { background: #639922; }
.fill-late { background: #E24B4A; }
.pct { font-size: 11px; color: #888; min-width: 28px; text-align: right; }
.actions { display: flex; gap: 6px; flex-shrink: 0; margin-top: 2px; }
.btn-sub {
  border: 0.5px solid #1D9E75; color: #0F6E56; background: transparent;
  border-radius: 6px; padding: 3px 8px; font-size: 11px; cursor: pointer;
}
.btn-sub:hover { background: #E1F5EE; }
.btn-icon {
  border: 0.5px solid #e0e0e0; background: transparent;
  border-radius: 6px; padding: 3px 8px; font-size: 11px; cursor: pointer; color: #888;
}
.btn-icon:hover { background: #f5f5f3; color: #333; }
.btn-add-sub {
  border: 0.5px solid #1D9E75; color: #0F6E56; background: transparent;
  border-radius: 6px; padding: 2px 6px; font-size: 10px; cursor: pointer; font-weight: 500;
}
.btn-add-sub:hover { background: #E1F5EE; }
.sub-list { border-top: 0.5px solid #eee; }
.sub-empty { padding: 20px 16px; font-size: 12px; color: #aaa; text-align: center; }
.empty-icon { font-size: 24px; margin-bottom: 8px; }
.empty-text { font-weight: 500; margin-bottom: 4px; }
.empty-action { font-size: 11px; color: #888; }
.slide-enter-active, .slide-leave-active { transition: all .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 2000px; opacity: 1; }
.expanded-section { border-top: 0.5px solid #eee; padding: 10px 16px; background: #fafafa; }
.expand-hint { font-size: 12px; color: #666; text-align: center; }
</style>
