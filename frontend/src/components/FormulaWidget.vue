<script setup lang="ts">
/**
 * FormulaWidget
 *
 * 通过 <Teleport to="body"> 渲染到 body 顶层，
 * 使用 position: fixed 定位，出现在输入框的下方或上方。
 *
 * 定位逻辑：
 *   left = anchorRect.left （面板左边缘与输入框左边缘对齐）
 *   top 根据输入框位置决定：
 *     - 如果输入框在页面下半部分，则 top = anchorRect.top - GAP - panelHeight （上方）
 *     - 否则 top = anchorRect.bottom + GAP （下方）
 *
 * 边界保护：
 *   左右超出视口时夹回 MARGIN，确保面板完全可见。
 *   上下超出时调整位置，确保面板可见。
 */

import { inject, computed, ref, watch, nextTick } from 'vue'
import { FORMULA_KEY } from '@/composables/useFormulaContext'

const ctx = inject(FORMULA_KEY)

// ── 定位计算 ─────────────────────────────────────────────────────────────────

const PANEL_WIDTH = 480   // 与 CSS 中 .fw-wrap 的 width 保持一致
const GAP         = 8     // 按钮与面板之间的间隔
const MARGIN      = 12    // 视口边缘最小留白

const panelRef = ref<HTMLElement | null>(null)

const style = computed(() => {
  const rect = ctx?.anchorRect.value
  if (!rect) return { display: 'none' }

  // 面板左边缘对齐输入框左边缘
  let left = rect.left

  // 根据输入框位置决定面板在上方还是下方
  const panelH = panelRef.value?.offsetHeight ?? 420
  const isLowerHalf = rect.bottom > window.innerHeight / 2
  let top = isLowerHalf ? rect.top - GAP - panelH : rect.bottom + GAP

  // 边界保护：防止超出视口
  if (left < MARGIN) left = MARGIN
  if (left + PANEL_WIDTH > window.innerWidth - MARGIN) {
    left = window.innerWidth - MARGIN - PANEL_WIDTH
  }
  if (top < MARGIN) top = MARGIN
  if (top + panelH > window.innerHeight - MARGIN) {
    top = window.innerHeight - MARGIN - panelH
  }

  return {
    left: `${Math.round(left)}px`,
    top:  `${Math.round(top)}px`,
  }
})

// ── 面板出现时滚动位置可能影响 fixed 定位，监听滚动并在关闭时清理 ──────────────

function onScroll() {
  // 滚动时直接关闭浮层，避免位置错位
  if (ctx?.isVisible.value) ctx.closeWidget()
}

watch(
  () => ctx?.isVisible.value,
  (visible) => {
    if (visible) {
      window.addEventListener('scroll', onScroll, { passive: true })
      // 点击面板外侧关闭
      nextTick(() => {
        window.addEventListener('pointerdown', onOutsideClick)
      })
    } else {
      window.removeEventListener('scroll', onScroll)
      window.removeEventListener('pointerdown', onOutsideClick)
    }
  }
)

function onOutsideClick(e: PointerEvent) {
  if (!panelRef.value) return
  if (!panelRef.value.contains(e.target as Node)) {
    ctx?.closeWidget()
  }
}

// ── 状态 ──────────────────────────────────────────────────────────────────────

const activeLabel = computed(() => {
  const el = ctx?.activeEl.value
  if (!el) return null
  return el.dataset.label ?? '输入框'
})

const cursorDesc = computed(() => {
  if (!ctx?.activeEl.value) return null
  const { start, end } = ctx.savedSel.value
  if (start === end) return `光标 ${start}`
  return `选中 ${start}–${end}`
})

// ── 公式按钮数据 ──────────────────────────────────────────────────────────────

interface FormulaButton {
  label: string
  insert: string
  cursor?: number
  title?: string
}
interface FormulaGroup {
  name: string
  buttons: FormulaButton[]
}

const groups: FormulaGroup[] = [
  {
    name: '希腊字母',
    buttons: [
      { label: 'α', insert: 'α', title: 'Alpha' },
      { label: 'β', insert: 'β', title: 'Beta' },
      { label: 'γ', insert: 'γ', title: 'Gamma' },
      { label: 'δ', insert: 'δ', title: 'Delta' },
      { label: 'ε', insert: 'ε', title: 'Epsilon' },
      { label: 'θ', insert: 'θ', title: 'Theta' },
      { label: 'λ', insert: 'λ', title: 'Lambda' },
      { label: 'μ', insert: 'μ', title: 'Mu' },
      { label: 'π', insert: 'π', title: 'Pi' },
      { label: 'σ', insert: 'σ', title: 'Sigma (小写)' },
      { label: 'φ', insert: 'φ', title: 'Phi' },
      { label: 'ω', insert: 'ω', title: 'Omega (小写)' },
      { label: 'Σ', insert: 'Σ', title: 'Sigma (大写)' },
      { label: 'Δ', insert: 'Δ', title: 'Delta (大写)' },
      { label: 'Π', insert: 'Π', title: 'Pi (大写)' },
      { label: 'Ω', insert: 'Ω', title: 'Omega (大写)' },
    ],
  },
  {
    name: '运算符',
    buttons: [
      { label: '±',  insert: '±' },
      { label: '×',  insert: '×' },
      { label: '÷',  insert: '÷' },
      { label: '≈',  insert: '≈' },
      { label: '≠',  insert: '≠' },
      { label: '≤',  insert: '≤' },
      { label: '≥',  insert: '≥' },
      { label: '∞',  insert: '∞' },
      { label: '√',  insert: '√' },
      { label: '∛',  insert: '∛' },
      { label: '∂',  insert: '∂' },
      { label: '∫',  insert: '∫' },
      { label: '∬',  insert: '∬' },
      { label: '∮',  insert: '∮' },
      { label: '∇',  insert: '∇' },
      { label: '∑',  insert: '∑' },
      { label: '∏',  insert: '∏' },
      { label: '∈',  insert: '∈' },
      { label: '∉',  insert: '∉' },
      { label: '⊂',  insert: '⊂' },
      { label: '∪',  insert: '∪' },
      { label: '∩',  insert: '∩' },
    ],
  },
  {
    name: '上下标',
    buttons: [
      { label: '²',  insert: '²' },
      { label: '³',  insert: '³' },
      { label: '⁻¹', insert: '⁻¹' },
      { label: '⁰',  insert: '⁰' },
      { label: '½',  insert: '½' },
      { label: '⅓',  insert: '⅓' },
      { label: '¼',  insert: '¼' },
      { label: '₀',  insert: '₀' },
      { label: '₁',  insert: '₁' },
      { label: '₂',  insert: '₂' },
      { label: '₃',  insert: '₃' },
      { label: 'ₙ',  insert: 'ₙ' },
    ],
  },
  {
    name: '括号 / 结构',
    buttons: [
      { label: '()',  insert: '()',  cursor: -1 },
      { label: '[]',  insert: '[]',  cursor: -1 },
      { label: '{}',  insert: '{}',  cursor: -1 },
      { label: '⌊⌋',  insert: '⌊⌋',  cursor: -1 },
      { label: '⌈⌉',  insert: '⌈⌉',  cursor: -1 },
      { label: '|x|', insert: '||',  cursor: -1 },
      { label: '…',   insert: '…' },
      { label: '→',   insert: '→' },
      { label: '⇒',   insert: '⇒' },
      { label: '↔',   insert: '↔' },
    ],
  },
  {
    name: 'LaTeX 表現',
    buttons: [
      { label: '分数',  insert: '$\\frac{}{}$',  cursor: -2 },
      { label: '根号',  insert: '$\\sqrt{}$',  cursor: -2 },
      { label: '上标',  insert: '$^{}$',  cursor: -2 },
      { label: '下标',  insert: '$_{}$',  cursor: -2 },
      { label: '积分',  insert: '$\\int$' },
      { label: '求和', insert: '$\\sum$' },
      { label: '极限', insert: '$\\lim$' },
      { label: '导数', insert: '$f\'(x)$' },
      { label: '无穷', insert: '$\\infty$' },
      { label: '≥',  insert: '$\\geq$' },
      { label: '≤',  insert: '$\\leq$' },
    ],
  },
]

function handleInsert(btn: FormulaButton) {
  ctx?.insert(btn.insert, btn.cursor ?? 0)
}
</script>

<template>
  <!-- Teleport 到 body，脱离文档流，使 fixed 定位不受父级 transform/overflow 影响 -->
  <Teleport to="body">
    <Transition name="fw-pop">
      <div
        v-if="ctx?.isVisible.value"
        ref="panelRef"
        class="fw-wrap"
        :style="style"
      >
        <!-- 头部 -->
        <div class="fw-header">
          <svg class="fw-icon" viewBox="0 0 20 20" fill="none">
            <path d="M4 6h12M4 10h8M4 14h10" stroke="currentColor"
                  stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="fw-title">公式控件</span>

          <span v-if="activeLabel" class="fw-badge fw-badge--active">
            {{ activeLabel }}
          </span>
          <span v-else class="fw-badge fw-badge--idle">未选中</span>

          <span v-if="cursorDesc" class="fw-cursor-info">{{ cursorDesc }}</span>

          <!-- 关闭按钮 -->
          <button
            class="fw-close-btn"
            title="收起"
            @mousedown.prevent
            @click="ctx?.closeWidget()"
          >
            <svg viewBox="0 0 16 16" fill="none">
              <path d="M4 4l8 8M12 4l-8 8"
                    stroke="currentColor" stroke-width="1.5"
                    stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <!-- 按钮区 -->
        <div class="fw-body">
          <div v-for="group in groups" :key="group.name" class="fw-group">
            <p class="fw-group-name">{{ group.name }}</p>
            <div class="fw-btn-row">
              <button
                v-for="btn in group.buttons"
                :key="btn.insert"
                class="fw-btn"
                :title="btn.title"
                :disabled="!activeLabel"
                @mousedown.prevent
                @click="handleInsert(btn)"
              >
                {{ btn.label }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ── 浮层容器 ── */
.fw-wrap {
  position: fixed;
  z-index: 9999;
  width: 480px;
  max-width: calc(100vw - 24px);
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

/* ── 弹出动画：从按钮处向下展开 ── */
.fw-pop-enter-active {
  transition: opacity 0.18s ease, transform 0.22s cubic-bezier(0.34, 1.36, 0.64, 1);
}
.fw-pop-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.fw-pop-enter-from,
.fw-pop-leave-to {
  opacity: 0;
  transform: translateY(-6px) scale(0.97);
  transform-origin: top right;
}

/* ── 头部 ── */
.fw-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 12px;
  background: var(--color-gray-50);
  border-bottom: 1px solid var(--color-gray-200);
  flex-wrap: nowrap;
}

.fw-icon {
  width: 15px;
  height: 15px;
  color: var(--color-gray-600);
  flex-shrink: 0;
}

.fw-title {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-gray-600);
  white-space: nowrap;
}

.fw-badge {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 999px;
  white-space: nowrap;
}

.fw-badge--active {
  background: var(--color-primary-light);
  color: var(--color-primary);
}

.fw-badge--idle {
  background: var(--color-gray-100);
  color: var(--color-gray-400);
}

.fw-cursor-info {
  font-size: 11px;
  color: var(--color-gray-400);
  font-family: var(--font-mono);
  white-space: nowrap;
}

.fw-close-btn {
  margin-left: auto;
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--color-gray-400);
  cursor: pointer;
  padding: 0;
  transition: background 0.1s, color 0.1s;
}

.fw-close-btn svg {
  width: 11px;
  height: 11px;
}

.fw-close-btn:hover {
  background: var(--color-danger-light);
  border-color: #F09595;
  color: var(--color-danger);
}

/* ── 按钮区 ── */
.fw-body {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 52vh;
  overflow-y: auto;
}

.fw-group-name {
  font-size: 10px;
  font-weight: 500;
  color: var(--color-gray-400);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  margin-bottom: 5px;
}

.fw-btn-row {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.fw-btn {
  height: 30px;
  min-width: 34px;
  padding: 0 7px;
  font-size: 14px;
  font-family: var(--font-mono);
  color: var(--color-gray-900);
  background: white;
  border: 1px solid var(--color-gray-200);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.1s, border-color 0.1s, transform 0.07s;
  user-select: none;
  line-height: 1;
}

.fw-btn:hover:not(:disabled) {
  background: var(--color-primary-light);
  border-color: var(--color-primary-mid);
  color: var(--color-primary);
}

.fw-btn:active:not(:disabled) {
  transform: scale(0.91);
}

.fw-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
</style>
