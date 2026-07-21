import katex from 'katex'

// 将文本中的 LaTeX 公式渲染为 HTML（供 v-html 使用）。
// 支持块级公式 $$...$$ 与行内公式 $...$，并把换行转为 <br>。
// 用占位符保护 KaTeX 输出，防止后续 replace 破坏 HTML 结构。
export function renderLatex(text) {
  if (!text) return '<span style="color:var(--c-text3)">（空）</span>'

  const placeholders = []
  let counter = 0

  // 替换块级公式并保存到占位符
  text = text.replace(/\$\$([^$]+)\$\$/g, (match, m) => {
    try {
      const html = katex.renderToString(m, { displayMode: true })
      const placeholder = `__KATEX_PLACEHOLDER_${counter}__`
      placeholders.push(html)
      counter++
      return placeholder
    } catch {
      return match
    }
  })

  // 替换行内公式并保存到占位符
  text = text.replace(/\$([^$]+)\$/g, (match, m) => {
    try {
      const html = katex.renderToString(m, { displayMode: false })
      const placeholder = `__KATEX_PLACEHOLDER_${counter}__`
      placeholders.push(html)
      counter++
      return placeholder
    } catch {
      return match
    }
  })

  // 现在安全地处理换行符（不会影响 KaTeX 占位符）
  text = text.replace(/\n/g, '<br>')

  // 恢复所有 KaTeX 占位符
  placeholders.forEach((html, i) => {
    text = text.replace(`__KATEX_PLACEHOLDER_${i}__`, html)
  })

  return text
}

// 解析题目/步骤的 imageUrlsJson（JSON 数组的 base64 data URL），失败返回空数组。
export function parseImages(json) {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}
