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
})
