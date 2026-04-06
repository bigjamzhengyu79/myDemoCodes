/**
 * useFormulaContext
 *
 * 设计目标：
 *   - 页面内多个 FormulaInput 共享一个 FormulaWidget
 *   - 追踪"当前活跃输入框"及其光标位置
 *   - 支持在光标处精确插入公式片段
 *   - 支持 v-model（通过派发 input 事件触发响应式更新）
 *   - 支持显示/隐藏公式控件面板，面板浮于触发按钮左下方
 *
 * 使用方式：
 *   父页面：const ctx = createFormulaContext(); provide(FORMULA_KEY, ctx)
 *   子组件：const ctx = inject(FORMULA_KEY)!; ctx.register(inputEl)
 *   控件：  const ctx = inject(FORMULA_KEY)!; ctx.insert('α')
 */

import { ref, readonly, type Ref, type InjectionKey } from 'vue'

// ─── 类型 ─────────────────────────────────────────────────────────────────────

export type FormulaTarget = HTMLInputElement | HTMLTextAreaElement

export interface SelectionRange {
  start: number
  end: number
}

/** 面板定位锚点：记录输入框在视口中的位置 */
export interface AnchorRect {
  left: number
  top: number
  bottom: number
  width: number
}

export interface FormulaContextValue {
  /** 当前聚焦的输入元素（只读） */
  activeEl: Readonly<Ref<FormulaTarget | null>>
  /** 当前保存的光标/选区位置（只读） */
  savedSel: Readonly<Ref<SelectionRange>>
  /** 公式控件面板是否可见（只读） */
  isVisible: Readonly<Ref<boolean>>
  /** 触发按钮的视口坐标，供 FormulaWidget 定位（只读） */
  anchorRect: Readonly<Ref<AnchorRect | null>>
  /**
   * 由各输入框在 onMounted 时调用，完成注册
   */
  register: (el: FormulaTarget) => void
  /**
   * 在当前活跃输入框的光标位置插入文本
   * @param text         要插入的字符串
   * @param cursorOffset 插入后光标偏移量（默认 0；传 -1 让光标落在末字符前）
   */
  insert: (text: string, cursorOffset?: number) => void
  /**
   * 切换公式控件面板的显示/隐藏
   * @param el          触发的输入框元素
   * @param inputRect   输入框的 getBoundingClientRect()，用于定位面板
   */
  toggleWidget: (el: FormulaTarget, inputRect: DOMRect) => void
  /** 直接关闭面板（头部关闭按钮调用） */
  closeWidget: () => void
}

export const FORMULA_KEY: InjectionKey<FormulaContextValue> = Symbol('formulaContext')

// ─── 工厂函数 ─────────────────────────────────────────────────────────────────

export function createFormulaContext(): FormulaContextValue {
  const activeEl   = ref<FormulaTarget | null>(null)
  const savedSel   = ref<SelectionRange>({ start: 0, end: 0 })
  const isVisible  = ref<boolean>(false)
  const anchorRect = ref<AnchorRect | null>(null)

  function syncSel(el: FormulaTarget): void {
    savedSel.value = {
      start: el.selectionStart ?? 0,
      end:   el.selectionEnd   ?? 0,
    }
  }

  function register(el: FormulaTarget): void {
    el.addEventListener('focus', () => {
      activeEl.value = el
      syncSel(el)
    })
    const syncIfActive = () => {
      if (activeEl.value === el) syncSel(el)
    }
    el.addEventListener('keyup',  syncIfActive)
    el.addEventListener('click',  syncIfActive)
    el.addEventListener('select', syncIfActive)
  }

  function insert(text: string, cursorOffset = 0): void {
    const el = activeEl.value
    if (!el) return

    const { start, end } = savedSel.value
    el.value = el.value.slice(0, start) + text + el.value.slice(end)
    el.dispatchEvent(new Event('input', { bubbles: true }))

    const newPos = start + text.length + cursorOffset
    el.focus()
    el.setSelectionRange(newPos, newPos)
    savedSel.value = { start: newPos, end: newPos }
  }

  function toggleWidget(el: FormulaTarget, inputRect: DOMRect): void {
    const isSameEl = activeEl.value === el

    // 更新锚点（输入框的视口坐标）
    anchorRect.value = {
      left:   inputRect.left,
      top:    inputRect.top,
      bottom: inputRect.bottom,
      width:  inputRect.width,
    }

    activeEl.value = el
    syncSel(el)

    // 同一框再次点击 → 收起；其他情况 → 展开
    isVisible.value = isSameEl ? !isVisible.value : true
  }

  function closeWidget(): void {
    isVisible.value = false
  }

  return {
    activeEl:   readonly(activeEl)   as Readonly<Ref<FormulaTarget | null>>,
    savedSel:   readonly(savedSel)   as Readonly<Ref<SelectionRange>>,
    isVisible:  readonly(isVisible)  as Readonly<Ref<boolean>>,
    anchorRect: readonly(anchorRect) as Readonly<Ref<AnchorRect | null>>,
    register,
    insert,
    toggleWidget,
    closeWidget,
  }
}
