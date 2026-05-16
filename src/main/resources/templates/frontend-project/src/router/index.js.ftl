import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/views/Layout.vue'
import Login from '@/views/Login.vue'
import generatedRoutes from './routes'

function firstMenuPath() {
  const item = generatedRoutes.find(r => r.meta && r.meta.isMenuVisible !== 0 && r.meta.isMenuVisible !== false)
  return item ? item.path : '/login'
}

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { public: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: firstMenuPath,
    children: generatedRoutes
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, from, next) => {
  const admin = sessionStorage.getItem('admin')
  if (to.meta?.public || to.path === '/login') {
    if (to.path === '/login' && admin) {
      next(firstMenuPath())
    } else {
      next()
    }
    return
  }
  if (!admin) {
    next('/login')
  } else {
    next()
  }
})

export default router
