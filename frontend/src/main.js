import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router/index.js'
// KaTeX 样式：utils/latex.js 用 katex.renderToString 生成的 DOM 依赖这份 CSS，
// 缺了它分式、根号、上下标全部塌成普通行内文本。
// 之前一直没引，靠 main.css 里的 .latex-block 字体兜底，公式实际是坏的。
import 'katex/dist/katex.min.css'
import './assets/main.css'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
