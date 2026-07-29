import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    // 固定产物目录，禁止 --outDir 等方式产生 dist_fresh* 多副本
    outDir: 'dist',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'pinia', 'vue-router'],
          'vendor-ui': ['naive-ui'],
          'vendor-utils': ['axios', 'marked', 'highlight.js']
        }
      }
    }
  }
})
