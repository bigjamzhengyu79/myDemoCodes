<script setup lang="ts">
/**
 * FormulaInput
 *
 * 右侧切换按钮点击时，将自身 el 的 getBoundingClientRect()
 * 一起传给 toggleWidget，让 FormulaWidget 能定位到输入框的下方或上方。
 */

import { ref, onMounted, inject, computed } from 'vue'
import { FORMULA_KEY } from '@/composables/useFormulaContext'

const props = withDefaults(defineProps<{
  modelValue: string
  label: string
  placeholder?: string
  multiline?: boolean
  rows?: number
  id?: string
}>(), {
  placeholder: '',
  multiline: false,
  rows: 3,
  id: undefined,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const formulaCtx = inject(FORMULA_KEY)

const inputRef    = ref<HTMLInputElement | null>(null)
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const toggleBtnRef = ref<HTMLButtonElement | null>(null)

const isActive = computed(() => {
  if (!formulaCtx?.activeEl.value) return false
  return (
    formulaCtx.activeEl.value === inputRef.value ||
    formulaCtx.activeEl.value === textareaRef.value
  )
})

const btnActive = computed(
  () => isActive.value && (formulaCtx?.isVisible.value ?? false)
)

onMounted(() => {
  const el = props.multiline ? textareaRef.value : inputRef.value
  if (el && formulaCtx) formulaCtx.register(el)
})

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
}

function onToggle() {
  const el  = props.multiline ? textareaRef.value : inputRef.value
  const btn = toggleBtnRef.value
  if (!el || !btn || !formulaCtx) return

  // 将输入框的视口坐标传给 context，供 Widget 定位
  formulaCtx.toggleWidget(el, el.getBoundingClientRect())

  // 面板展开后让输入框重新获得焦点
  if (formulaCtx.isVisible.value) el.focus()
}
</script>

<template>
  <div class="formula-input-wrap">
    <label class="fi-label">{{ label }}</label>

    <div class="fi-field" :class="{ 'is-active': isActive }">
      <input
        v-if="!multiline"
        ref="inputRef"
        :id="id"
        class="fi-input"
        type="text"
        :value="modelValue"
        :placeholder="placeholder"
        @input="onInput"
      />

      <textarea
        v-else
        ref="textareaRef"
        :id="id"
        class="fi-input fi-textarea"
        :value="modelValue"
        :placeholder="placeholder"
        :rows="rows"
        @input="onInput"
      />

      <!--
        @mousedown.prevent：阻止 mousedown 默认的焦点转移。
        焦点由 onToggle 内部显式调用 el.focus() 管理。
        传入输入框的 rect 用于定位面板。
      -->
      <button
        ref="toggleBtnRef"
        class="fi-toggle-btn"
        :class="{ 'is-on': btnActive }"
        :title="btnActive ? '收起公式控件' : '展开公式控件'"
        @mousedown.prevent
        @click="onToggle"
      >
        <!-- Σ 形图标 -->
        <svg viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path
            d="M13 3H5L9.5 9 5 15h8"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.formula-input-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.fi-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-gray-600);
  user-select: none;
}

.fi-field {
  position: relative;
  transition: box-shadow 0.15s;
  border-radius: var(--radius-md);
}

.fi-field.is-active {
  box-shadow: 0 0 0 2px var(--color-primary-mid);
}

.fi-input {
  width: 100%;
  padding: 8px 38px 8px 12px;
  font-size: 14px;
  font-family: var(--font-mono);
  color: var(--color-gray-900);
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-md);
  outline: none;
  transition: border-color 0.15s;
  resize: vertical;
}

.fi-input:focus {
  border-color: var(--color-primary);
}

.fi-textarea {
  line-height: 1.7;
  min-height: 80px;
}

/* ── 切换按钮 ── */
.fi-toggle-btn {
  position: absolute;
  top: 50%;
  right: 6px;
  transform: translateY(-50%);
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-sm);
  background: white;
  color: var(--color-gray-400);
  cursor: pointer;
  transition: background 0.15s, color 0.15s, border-color 0.15s;
  padding: 0;
}

.fi-toggle-btn svg {
  width: 14px;
  height: 14px;
}

.fi-toggle-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary-mid);
  color: var(--color-primary);
}

.fi-toggle-btn.is-on {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: white;
}

.fi-toggle-btn.is-on:hover {
  background: var(--color-primary);
  color: white;
}

/* textarea 模式按钮贴顶 */
.fi-textarea ~ .fi-toggle-btn {
  top: 10px;
  transform: none;
  background-color:lawngreen;
}
</style>
