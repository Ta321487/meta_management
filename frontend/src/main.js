import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const app = createApp(App)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 处理 ResizeObserver 错误（Element Plus 常见问题，通常无害）
const resizeObserverLoopErrRe = /ResizeObserver loop/
const resizeObserverLimitErrRe = /ResizeObserver loop limit exceeded/

// 捕获所有错误，包括 ResizeObserver 错误
const originalError = window.console.error
window.console.error = function(...args) {
  if (args.length > 0) {
    // 增强错误信息处理，确保能正确处理 Event 对象
    let message = ''
    if (args[0] instanceof Event) {
      message = `Event: ${args[0].type}`
    } else if (args[0] instanceof Error) {
      message = args[0].message
    } else {
      message = args[0]?.toString() || ''
    }
    if (resizeObserverLoopErrRe.test(message) || resizeObserverLimitErrRe.test(message)) {
      return // 忽略 ResizeObserver 错误
    }
  }
  originalError.apply(console, args)
}

// 处理未捕获的错误
window.addEventListener('error', (e) => {
  const message = e.message || ''
  if (resizeObserverLoopErrRe.test(message) || resizeObserverLimitErrRe.test(message)) {
    e.stopImmediatePropagation()
    e.preventDefault()
    return false
  }
}, true) // 使用捕获阶段，更早捕获错误

// 处理未处理的 Promise 拒绝
window.addEventListener('unhandledrejection', (e) => {
  const message = e.reason?.message || e.reason?.toString() || ''
  if (resizeObserverLoopErrRe.test(message) || resizeObserverLimitErrRe.test(message)) {
    e.preventDefault()
    return false
  }
})

app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})
app.mount('#app')

