<template>
  <div class="latex-toolbar">
    <div class="flex gap-2" style="flex-wrap:wrap">
      <button class="btn btn-sm" v-for="sym in symbols" :key="sym.label"
              @click="insertSymbol(sym.latex)">{{ sym.label }}</button>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue'

// Props
const props = defineProps({
  targetId: {
    type: String,
    required: true
  }
})

// Symbols
const symbols = [
  { label: '分数', latex: '\\frac{}{}' },
  { label: '根号', latex: '\\sqrt{}' },
  { label: '上标', latex: '^{}' },
  { label: '下标', latex: '_{}' },
  { label: '积分', latex: '\\int_{}^{}' },
  { label: '求和', latex: '\\sum_{}^{}' },
  { label: '极限', latex: '\\lim_{}' },
  { label: '导数', latex: "f'(x)" },
  { label: '无穷', latex: '\\infty' },
  { label: '≥', latex: '\\geq' },
  { label: '≤', latex: '\\leq' },
]

// Insert symbol function
function insertSymbol(latex) {
  const el = document.getElementById(props.targetId)
  if (!el) return
  const start = el.selectionStart || 0
  const end = el.selectionEnd || 0
  const text = el.value || ''
  const newText = text.slice(0, start) + '$' + latex + '$' + text.slice(end)
  el.value = newText
  el.focus()
  el.setSelectionRange(start + latex.length + 2, start + latex.length + 2)
}
</script>

<style scoped>
.latex-toolbar {
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  padding: 8px 12px;
  background: var(--c-surface2);
}
</style>