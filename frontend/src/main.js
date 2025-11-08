import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 处理 ResizeObserver 错误（Element Plus 常见问题，通常无害）
const resizeObserverLoopErrRe = /ResizeObserver loop/
window.addEventListener('error', (e) => {
  if (resizeObserverLoopErrRe.test(e.message)) {
    e.stopImmediatePropagation()
    return false
  }
})
window.addEventListener('unhandledrejection', (e) => {
  if (resizeObserverLoopErrRe.test(e.reason?.message || e.reason || '')) {
    e.preventDefault()
  }
})

app.use(router)
app.use(ElementPlus)
app.mount('#app')

