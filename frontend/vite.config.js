import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // port: 5173,
    // proxy: {
    //   '/api': {
    //     target: 'http://localhost:8080',
    //     changeOrigin: true,
    //     rewrite: (path) => path
    //   }
    // }
    proxy: {
      '/api': {
        target: 'https://mydemocodes.onrender.com',  // ← 你的 Render 地址
        changeOrigin: true,
        secure: true,
      }
    }
  }
})
