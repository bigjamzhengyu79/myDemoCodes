<template>
  <div class="parent-card">
    <div class="parent-header" @click="expanded = !expanded">
      <span class="toggle">{{ expanded ? '▾' : '▸' }}</span>

      <div class="parent-info">
        <div class="title-row">
          <span class="title">{{ goal.title }}</span>
          <StatusBadge :status="goal.status" />
          <span class="sub-count">{{ goal.subGoals?.length || 0 }} 个子目标</span>
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

    <Transition name="slide">
      <div v-if="expanded" class="sub-list">
        <div v-if="!goal.subGoals?.length" class="sub-empty">
          暂无子目标，点击「+ 子目标」开始分解
        </div>

        <div
          v-for="(sub, idx) in goal.subGoals"
          :key="sub.id"
          class="sub-item"
        >
          <div class="connector">
            <div class="v-line top"></div>
            <div class="node-dot"></div>
            <div class="v-line bottom" :class="{ invisible: idx === goal.subGoals.length - 1 }"></div>
          </div>
          <div class="h-line"></div>

          <div class="sub-body">
            <div class="sub-title-row">
              <span class="sub-title">{{ sub.title }}</span>
              <StatusBadge :status="sub.status" small />
            </div>
            <div v-if="sub.description" class="sub-desc">{{ sub.description }}</div>
            <div class="sub-meta">
              <span>预计：{{ sub.plannedStart }} ~ {{ sub.plannedEnd }}</span>
              <span v-if="sub.actualStart">实际开始：{{ sub.actualStart }}</span>
              <span v-if="sub.actualEnd">实际完成：{{ sub.actualEnd }}</span>
              <span>实施者：{{ sub.owners || '—' }}</span>
            </div>
            <div class="progress-row" style="margin-top:6px">
              <div class="bar-bg" style="height:3px">
                <div class="bar-fill" :class="`fill-${sub.status.toLowerCase()}`"
                     :style="{ width: sub.progress + '%' }" style="height:100%"></div>
              </div>
              <span class="pct" style="font-size:10px">{{ sub.progress }}%</span>
            </div>
          </div>

          <div class="sub-actions">
            <button class="btn-icon" @click="$emit('editSub', { parentId: goal.id, sub })">编辑</button>
            <button class="btn-icon" @click="$emit('deleteSub', { parentId: goal.id, subId: sub.id })">删除</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import StatusBadge from './StatusBadge.vue'

const props = defineProps({
  goal: { type: Object, required: true },
})

defineEmits(['edit', 'delete', 'addSub', 'editSub', 'deleteSub'])

const expanded = ref(true)
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
.sub-list { border-top: 0.5px solid #eee; }
.sub-empty { padding: 10px 16px 10px 42px; font-size: 12px; color: #aaa; }
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
.sub-body { flex: 1; min-width: 0; }
.sub-title-row { display: flex; align-items: center; gap: 7px; margin-bottom: 4px; flex-wrap: wrap; }
.sub-title { font-size: 13px; color: #111; }
.sub-desc { font-size: 11px; color: #888; margin-bottom: 5px; line-height: 1.5; }
.sub-meta { display: flex; gap: 12px; font-size: 11px; color: #888; flex-wrap: wrap; }
.sub-actions { display: flex; gap: 4px; flex-shrink: 0; margin-top: 2px; }
.slide-enter-active, .slide-leave-active { transition: all .2s ease; overflow: hidden; }
.slide-enter-from, .slide-leave-to { max-height: 0; opacity: 0; }
.slide-enter-to, .slide-leave-from { max-height: 2000px; opacity: 1; }
</style>
