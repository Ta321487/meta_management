<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <span v-show="!isCollapse" class="logo-text">${appTitle!"业务系统"}</span>
        <span v-show="isCollapse" class="logo-mini">管</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="isCollapse"
          :collapse-transition="false"
          background-color="#001529"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#fff"
        >
          <el-menu-item v-for="item in menuRoutes" :key="item.path" :index="item.path">
            <el-icon v-if="item.meta?.icon"><component :is="item.meta.icon" /></el-icon>
            <template #title>{{ item.meta?.title || item.name }}</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>
    <el-container class="main-wrap">
      <el-header class="app-header" height="50px">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <component :is="isCollapse ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span class="user-name">管理员</span>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/auth'
import generatedRoutes from '@/router/routes'

const route = useRoute()
const router = useRouter()
const isCollapse = ref(false)

const menuRoutes = computed(() =>
  generatedRoutes
    .filter(r => r.meta && r.meta.isMenuVisible !== 0 && r.meta.isMenuVisible !== false)
    .map(r => ({
      ...r,
      meta: { ...r.meta, title: r.meta.title || r.meta.nodeCode || r.name || r.path }
    }))
)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || route.name || '工作台')

const handleLogout = async () => {
  try { await logout() } catch (e) { /* ignore */ }
  sessionStorage.removeItem('admin')
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<style scoped>
.layout-container { height: 100vh; }
.sidebar {
  background: #001529;
  display: flex;
  flex-direction: column;
  box-shadow: 2px 0 8px rgba(0, 21, 41, 0.15);
  z-index: 10;
}
.logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}
.logo-mini { font-size: 18px; }
.menu-scroll { flex: 1; }
.main-wrap { min-width: 0; background: #f0f2f5; }
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.04);
}
.header-left { display: flex; align-items: center; gap: 12px; }
.collapse-btn { font-size: 20px; cursor: pointer; color: #606266; }
.collapse-btn:hover { color: #409eff; }
.header-right { display: flex; align-items: center; gap: 12px; }
.user-name { font-size: 14px; color: #606266; }
.app-main { padding: 16px; overflow: auto; }
:deep(.el-menu-item.is-active) { background-color: #1890ff !important; }
</style>
