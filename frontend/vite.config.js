import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 这里的 proxy 【只在 npm run dev 生效】，生产构建根本不启 server。
// 生产走的是 .env.production 里的 VITE_API_BASE_URL（见 src/api/index.js），
// 前端直接请求该绝对地址，不经过任何代理 —— 所以不要再靠注释切换这段配置。
//
// 本地想连远程后端时，不要改这个文件，改 frontend/.env.local（已 gitignore）：
//   VITE_DEV_PROXY_TARGET=https://mydemocodes.onrender.com
// 缺省即本地后端。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target,
          changeOrigin: true,
          // 指向 https 远程后端时需要校验证书；本地 http 时该项无影响
          secure: target.startsWith('https'),
          rewrite: (path) => path
        }
      }
    }
  }
})
