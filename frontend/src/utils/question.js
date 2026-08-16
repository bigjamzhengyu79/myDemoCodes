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

// ==================== 错误类型 ====================
//
// 原本只内联在 DoAssignmentView 里，错题本的列表与详情也要用，故提到这里。
//
// 【注意 null】只有教师手动批改（后端 AnswerService.grade）才会写 errorType，
// 自动判分的选择题/填空题一律留 null。所以必须把 null 和 'NONE' 一起当作「未分类」，
// 否则绝大多数题的错误类型徽章会是空白。

export const ERROR_TYPES = [
  { value: 'CONCEPT', label: '概念错误', badge: 'badge-red' },
  { value: 'CALC', label: '计算失误', badge: 'badge-amber' },
  { value: 'READING', label: '审题有误', badge: 'badge-purple' }
]

const ERROR_MAP = Object.fromEntries(ERROR_TYPES.map(t => [t.value, t]))

export function errorLabel(e) {
  if (!e || e === 'NONE') return '未分类'
  return ERROR_MAP[e]?.label || e
}

export function errorBadge(e) {
  if (!e || e === 'NONE') return 'badge-gray'
  return ERROR_MAP[e]?.badge || 'badge-gray'
}

// ==================== 掌握状态（错题本） ====================

export const MASTERY_TYPES = [
  { value: 'UNREVIEWED', label: '未复习', badge: 'badge-gray' },
  { value: 'REVIEWING', label: '复习中', badge: 'badge-amber' },
  { value: 'MASTERED', label: '已掌握', badge: 'badge-green' }
]

const MASTERY_MAP = Object.fromEntries(MASTERY_TYPES.map(t => [t.value, t]))

export function masteryLabel(m) { return MASTERY_MAP[m]?.label || '未复习' }

export function masteryBadge(m) { return MASTERY_MAP[m]?.badge || 'badge-gray' }

// 得分徽章配色：满分绿、部分分黄、零分红。
// 与 DoAssignmentView 的判分展示保持同一套阈值 —— 学生在两个页面看到的颜色语言必须一致。
// （DoAssignmentView 目前仍保留自己那份内联副本，待后续清理时改为引用这里。）
export function scoreColor(score, totalScore) {
  if (score == null) return 'badge-gray'
  if (score >= (totalScore || 0)) return 'badge-green'
  if (score > 0) return 'badge-amber'
  return 'badge-red'
}
