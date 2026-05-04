<template>
  <div class="sub-node">
    <div class="sub-item">
      <div class="connector">
        <div class="v-line top"></div>
        <div class="node-dot"></div>
        <div class="v-line bottom" :class="{ invisible: isLast }"></div>
      </div>
      <div class="h-line"></div>

      <!-- 子目标头部（可点击展开/收起） -->
      <div class="sub-header" @click="toggleExpanded">
        <span class="sub-toggle" v-if="goal.subGoals?.length > 0">{{ isExpanded ? '▾' : '▸' }}</span>

        <div class="sub-body">
          <div class="sub-title-row">
            <span class="sub-title">{{ goal.title }}</span>
            <StatusBadge :status="goal.status" small />
            <span class="sub-depth">层级 {{ goal.depth || 1 }}</span>
          </div>
          <div v-if="goal.description" class="sub-desc">{{ goal.description }}</div>
          <div class="sub-meta">
            <span>预计：{{ goal.plannedStart }} ~ {{ goal.plannedEnd }}</span>
            <span v-if="goal.actualStart">实际开始：{{ goal.actualStart }}</span>
            <span v-if="goal.actualEnd">实际完成：{{ goal.actualEnd }}</span>
            <span>实施者：{{ goal.owners || '—' }}</span>
          </div>
          <div class="progress-row" style="margin-top:6px">
            <div class="bar-bg" style="height:3px">
              <div class="bar-fill" :class="`fill-${goal.status.toLowerCase()}`"
                   :style="{ width: goal.progress + '%' }" style="height:100%"></div>
            </div>
            <span class="pct" style="font-size:10px">{{ goal.progress }}%</span>
          </div>
        </div>

        <div class="sub-actions" @click.stop>
          <button class="btn-icon btn-add-sub" @click="$emit('addSub', goal)">+ 子</button>
          <button class="btn-icon" @click="$emit('editSub', { parentId: parentId, sub: goal })">编辑</button>
          <button class="btn-icon" @click="$emit('deleteSub', { parentId: parentId, subId: goal.id })">删除</button>
        </div>
      </div>
    </div>

    <!-- 嵌套子目标列表（可展开/收起） -->
    <Transition name="slide">
      <div v-if="isExpanded && goal.subGoals?.length > 0" class="nested-sub-list">
        <SubGoalItem
          v-for="(subGoal, idx) in goal.subGoals"
          :key="subGoal.id"
          :goal="subGoal"
          :parent-id="goal.id"
          :is-last="idx === goal.subGoals.length - 1"
          @add-sub="$emit('addSub', $event)"
          @edit-sub="$emit('editSub', $event)"
          @delete-sub="$emit('deleteSub', $event)"
        />
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import StatusBadge from './StatusBadge.vue'
import SubGoalItem from './SubGoalItem.vue'
import { useGoalStore } from '@/stores/goalStore'

const props = defineProps({
  goal: { type: Object, required: true },
  parentId: { type: Number, required: true },
  isLast: { type: Boolean, default: false },
})

const emit = defineEmits(['addSub', 'editSub', 'deleteSub'])

const goalStore = useGoalStore()
const isExpanded = computed(() => goalStore.expandedGoals.has(props.goal.id))

function toggleExpanded() {
  goalStore.toggleExpanded(props.goal.id)
}
</script>

<style scoped>
.sub-node {
  width: 100%;
}
.sub-item {
  display: flex; align-items: flex-start; gap: 0;
  padding: 10px 16px; border-top: 0.5px solid #f0f0ee;
}
.sub-item:hover { background: #fafaf9; }
.connector {
  display: flex; flex-direction: column; align-items: center;
  width: 26px; flex-shrink: 0; align-self: stretch; padding-top: 2px;
}
.v-line { width: 1px; background: #ddd; flex: 1; min-height: 6px; }
.v-line.invisible { background: transparent; }
.node-dot {
  width: 8px; height: 8px; border-radius: 50%;
  border: 1.5px solid #ccc; background: #fff; flex-shrink: 0;
}
.h-line { width: 12px; height: 1px; background: #ddd; flex-shrink: 0; margin-top: 12px; }
.sub-header {
  flex: 1; min-width: 0; display: flex; align-items: flex-start; gap: 0;
  cursor: pointer; user-select: none; padding: 2px 0;
}
.sub-header:hover { background: #f5f5f3; border-radius: 6px; }
.sub-toggle {
  font-size: 12px; color: #999; margin-top: 2px; flex-shrink: 0;
  margin-right: 8px;
}
.sub-body { flex: 1; min-width: 0; }
.sub-title-row { display: flex; align-items: center; gap: 7px; margin-bottom: 4px; flex-wrap: wrap; }
.sub-title { font-size: 13px; color: #111; }
.sub-depth { font-size: 10px; color: #666; background: #f0f8ff; border-radius: 6px; padding: 1px 5px; }
.sub-desc { font-size: 11px; color: #888; margin-bottom: 5px; line-height: 1.5; }
.sub-meta { display: flex; gap: 12px; font-size: 11px; color: #888; flex-wrap: wrap; }
.sub-actions { display: flex; gap: 4px; flex-shrink: 0; margin-top: 2px; }
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
.nested-sub-list { margin-left: 26px; padding-left: 10px; border-left: 1px solid #eee; width: calc(100% - 26px); }
.slide-enter-active, .slide-leave-active { transition: max-height .2s ease, opacity .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 2000px; opacity: 1; }
</style>