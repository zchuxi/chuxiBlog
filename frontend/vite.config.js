import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { compression } from 'vite-plugin-compression2'

export default defineConfig({
  plugins: [
    vue(),
    // 注意参数名是 algorithms（复数）；写成 algorithm 会被静默忽略并回退到默认值。
    // 默认 include 只覆盖 html/xml/css/json/js/mjs/svg/yaml/toml，
    // Live2D 的 miku.moc3（9.07MiB）不在其中，一直是裸传；brotli 后仅 1.97MiB。
    compression({
      algorithms: ['gzip', 'brotliCompress'],
      include: /\.(html|xml|css|json|js|mjs|svg|yaml|yml|toml|moc3|can3|mtn)$/
    })
  ],
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
    sourcemap: false,
    cssCodeSplit: true,
    rollupOptions: {
      output: {
        assetFileNames: 'assets/[name]-[hash:8].[ext]',
        chunkFileNames: 'assets/[name]-[hash:8].js',
        entryFileNames: 'assets/[name]-[hash:8].js',
        manualChunks: {
          'vendor-vue': ['vue', 'pinia', 'vue-router'],
          'vendor-utils': ['axios', 'marked', 'highlight.js']
        }
      }
    }
  }
})
