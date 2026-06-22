<template>
  <span class="badge" :class="[badgeClass, { small }]">
    <span class="dot" :class="dotClass"></span>
    {{ labelMap[status] || status }}
  </span>
</template>

<script setup>
const props = defineProps({
  status: { type: String, required: true },
  small: { type: Boolean, default: false },
})

const badgeClass = `badge-${props.status.toLowerCase()}`
const dotClass = `dot-${props.status.toLowerCase()}`
const labelMap = {
  TODO: '未开始',
  IN_PROGRESS: '进行中',
  DONE: '已完成',
  LATE: '已延期',
}
</script>

<style scoped>
.badge {
  display: inline-flex; align-items: center; gap: 4px;
  border-radius: 20px; padding: 2px 9px; font-size: 11px; font-weight: 500; white-space: nowrap;
}
.badge.small { font-size: 10px; padding: 1px 7px; }
.badge-todo { background: #E1F5EE; color: #0F6E56; }
.badge-in_progress { background: #E6F1FB; color: #185FA5; }
.badge-done { background: #EAF3DE; color: #3B6D11; }
.badge-late { background: #FCEBEB; color: #A32D2D; }
.dot { width: 5px; height: 5px; border-radius: 50%; display: inline-block; }
.dot-todo { background: #1D9E75; }
.dot-in_progress { background: #378ADD; }
.dot-done { background: #639922; }
.dot-late { background: #E24B4A; }
</style>
