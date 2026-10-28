import { createApp } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { ElLoading } from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import '@/assets/styles/index.scss'
import App from '@/App.vue'
import router from '@/router'
import '@/router/guard'
import pinia from '@/store'
import { hasPermDirective } from '@/directives/hasPerm'
import { useThemeStore } from '@/store/modules/theme'

const app = createApp(App)

// 全局注册 Element Plus 图标，模板里可直接用 <HomeFilled /> 或 <component :is="'Setting'" />
for (const [name, component] of Object.entries(ElementPlusIcons)) {
  app.component(name, component)
}

app.use(pinia)
// 主题要在挂载前应用，避免闪白
useThemeStore().init()
app.use(router)
app.use(ElLoading)
app.directive('hasPerm', hasPermDirective)
app.mount('#app')
