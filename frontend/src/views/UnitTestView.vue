<template>
  <div class="page" style="max-width:1200px">
    <div class="flex gap-2 mb-4" style="align-items:center">
      <button class="btn btn-sm" @click="$router.back()">← 返回</button>
      <h1 style="font-size:20px;font-weight:600">前端函数单元测试</h1>
    </div>

    <!-- 测试用例列表 -->
    <div class="card mb-4">
      <div class="flex-between mb-3">
        <h2 style="font-size:16px;font-weight:500">测试用例</h2>
        <button class="btn btn-sm btn-primary" @click="runAllTests">运行所有测试</button>
      </div>

      <div class="test-cases">
        <div v-for="(testCase, index) in testCases" :key="index" class="test-case">
          <div class="test-case-header">
            <div class="flex gap-2" style="align-items:center">
              <span class="test-case-name">{{ testCase.name }}</span>
              <span :class="['test-status', testCase.status]">
                {{ testCase.status === 'pending' ? '待运行' :
                   testCase.status === 'running' ? '运行中' :
                   testCase.status === 'passed' ? '通过' : '失败' }}
              </span>
            </div>
            <button class="btn btn-xs" @click="runTest(index)">
              {{ testCase.status === 'running' ? '运行中...' : '运行' }}
            </button>
          </div>

          <div v-if="testCase.status === 'failed'" class="test-error">
            <strong>错误：</strong>{{ testCase.error }}
          </div>

          <div v-if="testCase.input !== undefined" class="test-input">
            <strong>输入：</strong>
            <pre>{{ JSON.stringify(testCase.input, null, 2) }}</pre>
          </div>

          <div v-if="testCase.expected !== undefined" class="test-expected">
            <strong>期望：</strong>
            <pre>{{ JSON.stringify(testCase.expected, null, 2) }}</pre>
          </div>

          <div v-if="testCase.actual !== undefined" class="test-actual">
            <strong>实际：</strong>
            <pre>{{ JSON.stringify(testCase.actual, null, 2) }}</pre>
          </div>

          <div v-if="testCase.output !== undefined" class="test-output">
            <strong>输出：</strong>
            <div v-html="testCase.output"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 自定义测试 -->
    <div class="card">
      <h2 style="font-size:16px;font-weight:500;margin-bottom:12px">自定义测试</h2>

      <div class="form-group">
        <label class="form-label">测试函数</label>
        <select v-model="selectedFunction" class="form-control">
          <option value="renderLatex">renderLatex</option>
          <option value="stripLatex">stripLatex</option>
          <option value="renderLatexWithImages">renderLatexWithImages</option>
        </select>
      </div>

      <div class="form-group">
        <label class="form-label">输入参数</label>
        <textarea v-model="customInput" class="form-control" rows="4"
                  placeholder="输入测试参数，JSON 格式"></textarea>
      </div>

      <button class="btn btn-primary" @click="runCustomTest" :disabled="!customInput.trim()">
        运行自定义测试
      </button>

      <div v-if="customResult" class="custom-result mt-3">
        <h3 style="font-size:14px;font-weight:500;margin-bottom:8px">测试结果</h3>
        <div v-html="customResult"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import katex from 'katex'

// 测试用例状态
const testCases = ref([])
const selectedFunction = ref('renderLatex')
const customInput = ref('')
const customResult = ref('')

// 导入需要测试的函数
function renderLatex(text) {
  if (!text) return '<span style="color:var(--c-text3)">预览...</span>'
  
  // 用占位符保护 KaTeX 输出，防止后续 replace 破坏 HTML 结构
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

function stripLatex(text) {
  return (text || '').replace(/\$[^$]*\$/g, '[公式]').replace(/\n/g, ' ')
}

function renderLatexWithImages(text, images) {
  const textHtml = renderLatex(text)
  if (!images?.length) return textHtml
  const imgHtml = images.map((img, i) =>
    `<div style="margin-top:10px">
       <div style="font-size:11px;color:var(--c-text3);margin-bottom:4px">图 ${i + 1}</div>
       <img src="${img.dataUrl}" style="max-width:100%;border-radius:4px;border:1px solid var(--c-border)" />
     </div>`
  ).join('')
  return textHtml + imgHtml
}

// 初始化测试用例
function initTestCases() {
  testCases.value = [
    {
      name: 'renderLatex - 平方根',
      function: 'renderLatex',
      input: '$\\sqrt{2}$',
      expected: 'katex', // KaTeX 输出中包含的标记
      expectedType: 'contains',
      status: 'pending'
    },
    {
      name: 'renderLatex - 空字符串',
      function: 'renderLatex',
      input: '',
      expected: '<span style="color:var(--c-text3)">预览...</span>',
      expectedType: 'exact',
      status: 'pending'
    },
    {
      name: 'renderLatex - 简单文本',
      function: 'renderLatex',
      input: 'Hello world',
      expected: 'Hello world',
      expectedType: 'exact',
      status: 'pending'
    },
    {
      name: 'renderLatex - 行内公式（检查KaTeX）',
      function: 'renderLatex',
      input: '质量为 $m$，速度为 $v$',
      expected: 'katex', // 检查是否包含 KaTeX 输出标记
      expectedType: 'contains',
      status: 'pending'
    },
    {
      name: 'renderLatex - 块级公式（检查KaTeX）',
      function: 'renderLatex',
      input: '牛顿第二定律：$$F = ma$$',
      expected: 'katex',
      expectedType: 'contains',
      status: 'pending'
    },
    {
      name: 'renderLatex - 换行符',
      function: 'renderLatex',
      input: '第一行\n第二行',
      expected: '<br>',
      expectedType: 'contains',
      status: 'pending'
    },
    {
      name: 'renderLatex - 复杂公式（检查KaTeX）',
      function: 'renderLatex',
      input: '$$\\int_{0}^{\\infty} e^{-x^2} dx = \\frac{\\sqrt{\\pi}}{2}$$',
      expected: 'katex',
      expectedType: 'contains',
      status: 'pending'
    },
    {
      name: 'stripLatex - 无公式',
      function: 'stripLatex',
      input: 'Hello world',
      expected: 'Hello world',
      expectedType: 'exact',
      status: 'pending'
    },
    {
      name: 'stripLatex - 包含公式',
      function: 'stripLatex',
      input: '质量 $m$ 速度 $v$ 公式 $$F=ma$$',
      expected: '质量 [公式] 速度 [公式] 公式 [公式]',
      expectedType: 'exact',
      status: 'pending'
    },
    {
      name: 'stripLatex - 换行符处理',
      function: 'stripLatex',
      input: '第一行\n第二行',
      expected: '第一行 第二行',
      expectedType: 'exact',
      status: 'pending'
    },
    {
      name: 'renderLatexWithImages - 无图片（检查KaTeX）',
      function: 'renderLatexWithImages',
      input: ['Hello $world$', []],
      expected: 'katex',
      expectedType: 'contains',
      status: 'pending'
    },
    {
      name: 'renderLatexWithImages - 带图片',
      function: 'renderLatexWithImages',
      input: ['公式：$a^2 + b^2 = c^2$', [{ dataUrl: 'data:image/png;base64,test' }]],
      expected: 'img',
      expectedType: 'contains',
      status: 'pending'
    }
  ]
}

// 运行单个测试
async function runTest(index) {
  const testCase = testCases.value[index]
  testCase.status = 'running'
  testCase.error = null
  testCase.actual = null
  testCase.output = null

  try {
    await new Promise(resolve => setTimeout(resolve, 100)) // 模拟异步

    let result
    switch (testCase.function) {
      case 'renderLatex':
        result = renderLatex(testCase.input)
        break
      case 'stripLatex':
        result = stripLatex(testCase.input)
        break
      case 'renderLatexWithImages':
        result = renderLatexWithImages(...testCase.input)
        break
      default:
        throw new Error('未知函数')
    }

    testCase.actual = result
    testCase.output = result

    // 根据 expectedType 进行不同的比较
    const expectedType = testCase.expectedType || 'exact'
    
    if (expectedType === 'contains') {
      // 检查结果是否包含期望的文本
      testCase.status = result.includes(testCase.expected) ? 'passed' : 'failed'
    } else if (expectedType === 'exact') {
      // 精确匹配
      testCase.status = result === testCase.expected ? 'passed' : 'failed'
    } else {
      testCase.status = 'failed'
      testCase.error = '未知的比较类型'
    }
  } catch (error) {
    testCase.status = 'failed'
    testCase.error = error.message
  }
}

// 运行所有测试
async function runAllTests() {
  for (let i = 0; i < testCases.value.length; i++) {
    await runTest(i)
  }
}

// 运行自定义测试
function runCustomTest() {
  try {
    const args = JSON.parse(customInput.value)
    let result

    switch (selectedFunction.value) {
      case 'renderLatex':
        result = renderLatex(args)
        break
      case 'stripLatex':
        result = stripLatex(args)
        break
      case 'renderLatexWithImages':
        result = renderLatexWithImages(...args)
        break
      default:
        throw new Error('未知函数')
    }

    customResult.value = result
  } catch (error) {
    customResult.value = `<span style="color:red">错误：${error.message}</span>`
  }
}

onMounted(() => {
  initTestCases()
})
</script>

<style scoped>
.test-cases {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.test-case {
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--c-surface);
}

.test-case-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.test-case-name {
  font-weight: 500;
  color: var(--c-text);
}

.test-status {
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.test-status.pending { background: var(--c-text3); color: white; }
.test-status.running { background: var(--c-primary); color: white; }
.test-status.passed { background: var(--c-success); color: white; }
.test-status.failed { background: var(--c-danger); color: white; }

.test-error {
  margin-top: 8px;
  padding: 8px;
  background: var(--c-danger-bg);
  color: var(--c-danger);
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.test-input, .test-expected, .test-actual, .test-output {
  margin-top: 8px;
}

.test-input pre, .test-expected pre, .test-actual pre {
  background: var(--c-surface2);
  padding: 8px;
  border-radius: var(--radius-sm);
  font-family: monospace;
  font-size: 12px;
  overflow-x: auto;
}

.test-output {
  background: var(--c-surface2);
  padding: 8px;
  border-radius: var(--radius-sm);
}

.custom-result {
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  padding: 12px;
  background: var(--c-surface);
}
</style>