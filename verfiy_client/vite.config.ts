import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
// Author: ktbtw
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true,
    port: 5173,
    strictPort: true,
    proxy: {
      // 代理到后端（Spring Boot 使用 context-path: /verfiy）
      '/verfiy': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
    },
  },
  optimizeDeps: {
    include: ['monaco-editor']
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('monaco-editor')) {
              return 'monaco-editor'
            }
            return 'vendor'
          }
        }
      }
    }
  }
})
