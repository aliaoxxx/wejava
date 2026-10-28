import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    AutoImport({
      imports: ['vue', 'vue-router', 'pinia'],
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/auto-imports.d.ts'
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'src/types/components.d.ts'
    })
  ],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  css: {
    preprocessorOptions: {
      scss: { api: 'modern-compiler' }
    }
  },
  // 前端挂在前缀下：生产 /prod/，开发 /dev/
  base: mode === 'production' ? '/prod/' : '/dev/',
  server: {
    port: 3300,
    proxy: {
      // 后端 dev profile 的 context-path 就是 /dev/api，所以不做 rewrite
      '/dev/api': { target: 'http://127.0.0.1:8000', changeOrigin: true }
    }
  }
}))
