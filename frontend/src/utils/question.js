// 题目相关的纯展示辅助函数，供题库管理页与题目选择器共用。

export const QUESTION_TYPES = [
  { value: 'SINGLE_CHOICE', label: '单选', badge: 'badge-blue' },
  { value: 'FILL_BLANK', label: '填空', badge: 'badge-purple' },
  { value: 'OPEN_ENDED', label: '解答', badge: 'badge-amber' }
]

const TYPE_MAP = Object.fromEntries(QUESTION_TYPES.map(t => [t.value, t]))

// 去掉 LaTeX 片段，用于列表中的纯文本预览。
export function stripLatex(s) {
  return (s || '').replace(/\$[^$]*\$/g, '[公式]').replace(/\n/g, ' ')
}

export function typeLabel(t) { return TYPE_MAP[t]?.label || t }

export function typeBadge(t) { return TYPE_MAP[t]?.badge || 'badge-gray' }
