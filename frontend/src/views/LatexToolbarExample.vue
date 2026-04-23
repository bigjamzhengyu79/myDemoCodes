<template>
  <div class="page" style="max-width: 800px; margin: 0 auto; padding: 20px;">
    <h1>LatexToolbar 使用示例</h1>

    <div style="margin-bottom: 20px;">
      <h3>示例1: 简单的textarea</h3>
      <LatexToolbar :target-id="'example1'" />
      <textarea id="example1" class="form-control" rows="4" placeholder="在这里输入LaTeX表达式..."></textarea>
    </div>

    <div style="margin-bottom: 20px;">
      <h3>示例2: input输入框</h3>
      <LatexToolbar :target-id="'example2'" />
      <input id="example2" type="text" class="form-control" placeholder="输入LaTeX表达式..." />
    </div>

    <div style="margin-bottom: 20px;">
      <h3>示例3: 内容编辑器</h3>
      <LatexToolbar :target-id="'example3'" />
      <div contenteditable="true" id="example3" class="content-editor" placeholder="可编辑内容区域..."></div>
    </div>

    <div style="margin-top: 40px; padding: 20px; background: var(--c-surface2); border-radius: var(--radius);">
      <h4>使用说明：</h4>
      <ul>
        <li>点击工具栏按钮可在指定ID的输入框中插入LaTeX符号</li>
        <li>支持textarea、input[type="text"]和contenteditable元素</li>
        <li>插入的符号会自动用$包裹</li>
        <li>插入后光标会定位到合适位置</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import LatexToolbar from '@/components/LatexToolbar.vue'
</script>

<style scoped>
.content-editor {
  border: 1px solid var(--c-border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  min-height: 80px;
  background: white;
  font-family: inherit;
}

.content-editor:empty::before {
  content: attr(placeholder);
  color: var(--c-text3);
  pointer-events: none;
}
</style>