import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    // 5173 常被别的本地项目占用，控制台固定走 5174
    port: 5174,
    host: '127.0.0.1',
    proxy: {
      // 三个都指到 server（默认 8080）：接口、上传的静态图、实时通道
      '/api': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8080', changeOrigin: true },
      '/socket.io': { target: 'http://127.0.0.1:8080', ws: true, changeOrigin: true }
    }
  },
  build: {
    chunkSizeWarningLimit: 1500
  }
})
