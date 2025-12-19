<template>
  <el-container class="layout-container" @click="hideContextMenu">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
      <div class="logo-wrapper">
        <div class="logo">
          <span v-show="!isCollapse">元数据管理</span>
          <span v-show="isCollapse">元</span>
        </div>
      </div>

      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        :collapse="isCollapse"
        :collapse-transition="false"
        class="sidebar-menu"
      >
        <el-menu-item index="/business-system">
          <el-icon><Setting /></el-icon>
          <template #title>业务系统管理</template>
        </el-menu-item>
        <el-menu-item index="/module-type">
          <el-icon><Document /></el-icon>
          <template #title>模块类型管理</template>
        </el-menu-item>
        <el-menu-item index="/module">
          <el-icon><Setting /></el-icon>
          <template #title>模块管理</template>
        </el-menu-item>
        <el-menu-item index="/table">
          <el-icon><Grid /></el-icon>
          <template #title>表管理</template>
        </el-menu-item>
        <el-menu-item index="/field">
          <el-icon><Document /></el-icon>
          <template #title>字段管理</template>
        </el-menu-item>
        <el-menu-item index="/node">
          <el-icon><Menu /></el-icon>
          <template #title>功能节点</template>
        </el-menu-item>
        <el-menu-item index="/relation">
          <el-icon><Connection /></el-icon>
          <template #title>表关联</template>
        </el-menu-item>
        <el-menu-item index="/rule">
          <el-icon><List /></el-icon>
          <template #title>业务规则</template>
        </el-menu-item>
        <el-menu-item index="/log">
          <el-icon><Document /></el-icon>
          <template #title>操作日志</template>
        </el-menu-item>
        <el-menu-item index="/codegen">
          <el-icon><Document /></el-icon>
          <template #title>代码生成</template>
        </el-menu-item>
        <el-menu-item index="/sql">
          <el-icon><Edit /></el-icon>
          <template #title>SQL执行</template>
        </el-menu-item>
      </el-menu>

      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="20">
          <component :is="isCollapse ? 'Expand' : 'Fold'" />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <el-header height="auto" class="header-wrapper">
        <div class="navbar">
          <div class="header-right">
            <el-dropdown @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                admin
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="tags-view-container">
          <el-tabs
            v-model="activeTab"
            type="card"
            closable
            @tab-remove="handleTabRemove"
            @tab-click="handleTabClick"
            class="menu-tabs"
          >
            <el-tab-pane
              v-for="tab in tabs"
              :key="tab.path"
              :name="tab.path"
            >
              <template #label>
                <span
                  class="tab-label"
                  @contextmenu.prevent.stop="onTabContextMenu($event, tab)"
                >
                  {{ tab.title }}
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>
          <div
            v-if="contextMenuVisible"
            class="tab-context-menu"
            :style="contextMenuStyle"
            @click.stop
          >
            <ul>
              <li @click="closeCurrentTab">关闭</li>
              <li @click="closeOtherTabs">关闭其他</li>
              <li @click="closeAllTabs">全部关闭</li>
            </ul>
          </div>
        </div>
      </el-header>

      <el-main class="app-main">
        <router-view v-slot="{ Component, route }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" :key="route.fullPath" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="400px"
      close-on-click-modal="false"
      close-on-press-escape="false"
    >
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-width="100px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout, changePassword } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Setting,
  Document,
  Grid,
  Menu,
  Connection,
  List,
  Edit,
  User,
  ArrowDown,
  Fold,
  Expand
} from '@element-plus/icons-vue'

export default {
  name: 'Layout',
  components: {
    Setting,
    Document,
    Grid,
    Menu,
    Connection,
    List,
    Edit,
    User,
    ArrowDown,
    Fold,
    Expand
  },
  setup() {
    const router = useRouter()
    const route = useRoute()

    const passwordDialogVisible = ref(false)
    const passwordFormRef = ref(null)
    const passwordForm = ref({
      oldPassword: '',
      newPassword: '',
      confirmPassword: ''
    })

    const isCollapse = ref(false)

    const menuTitleMap = {
      '/business-system': '业务系统管理',
      '/module-type': '模块类型管理',
      '/module': '模块管理',
      '/table': '表管理',
      '/field': '字段管理',
      '/node': '功能节点',
      '/relation': '表关联',
      '/rule': '业务规则',
      '/log': '操作日志',
      '/codegen': '代码生成',
      '/sql': 'SQL执行'
    }

    const activeMenu = computed(() => route.path)

    const tabs = ref([])
    const activeTab = ref(route.path === '/welcome' ? '' : route.path)
    const cachedViews = ref([])
    const contextMenuVisible = ref(false)
    const contextMenuStyle = ref({ left: '0px', top: '0px' })
    const contextMenuTabPath = ref('')

    const addTab = currentRoute => {
      const path = currentRoute.path
      if (path === '/welcome') {
        activeTab.value = ''
        return
      }

      const title =
        menuTitleMap[path] || currentRoute.meta?.title || currentRoute.name || path

      const matched = currentRoute.matched[currentRoute.matched.length - 1]
      const componentName = matched?.components?.default?.name

      const existing = tabs.value.find(tab => tab.path === path)
      if (!existing) {
        tabs.value.push({
          path,
          title,
          name: componentName
        })
      }

      if (componentName && !cachedViews.value.includes(componentName)) {
        cachedViews.value.push(componentName)
      }

      activeTab.value = path
    }

    const handleTabRemove = path => {
      const index = tabs.value.findIndex(tab => tab.path === path)
      if (index === -1) {
        return
      }

      const removedTab = tabs.value[index]

      if (removedTab.name) {
        const cacheIndex = cachedViews.value.indexOf(removedTab.name)
        if (cacheIndex > -1) {
          cachedViews.value.splice(cacheIndex, 1)
        }
      }

      tabs.value.splice(index, 1)

      if (activeTab.value === path) {
        if (tabs.value.length > 0) {
          const newIndex = index > 0 ? index - 1 : 0
          const newPath = tabs.value[newIndex].path
          activeTab.value = newPath
          router.push(newPath)
        } else {
          activeTab.value = ''
          router.push('/welcome')
        }
      }
    }

    const handleTabClick = pane => {
      if (pane?.props?.name) {
        activeTab.value = pane.props.name
      }
    }

    const onTabContextMenu = (event, tab) => {
      contextMenuTabPath.value = tab.path
      contextMenuStyle.value = {
        left: `${event.clientX}px`,
        top: `${event.clientY}px`
      }
      contextMenuVisible.value = true
    }

    const hideContextMenu = () => {
      contextMenuVisible.value = false
    }

    const closeCurrentTab = () => {
      if (!contextMenuTabPath.value) {
        return
      }
      handleTabRemove(contextMenuTabPath.value)
      contextMenuVisible.value = false
    }

    const closeOtherTabs = () => {
      if (!contextMenuTabPath.value) {
        return
      }
      const target = tabs.value.find(tab => tab.path === contextMenuTabPath.value)
      if (!target) {
        contextMenuVisible.value = false
        return
      }
      tabs.value = [target]
      cachedViews.value = target.name ? [target.name] : []
      activeTab.value = target.path
      router.push(target.path)
      contextMenuVisible.value = false
    }

    const closeAllTabs = () => {
      tabs.value = []
      cachedViews.value = []
      activeTab.value = ''
      router.push('/welcome')
      contextMenuVisible.value = false
    }

    watch(
      () => route.fullPath,
      () => {
        addTab(route)
      },
      { immediate: true }
    )

    watch(
      () => activeTab.value,
      newPath => {
        if (!newPath || newPath === route.path) {
          return
        }
        router.push(newPath)
      }
    )

    const toggleCollapse = () => {
      isCollapse.value = !isCollapse.value
    }

    const passwordRules = {
      oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
      newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
      confirmPassword: [
        { required: true, message: '请确认密码', trigger: 'blur' },
        {
          validator: (rule, value, callback) => {
            if (value !== passwordForm.value.newPassword) {
              callback(new Error('两次输入密码不一致'))
            } else {
              callback()
            }
          },
          trigger: 'blur'
        }
      ]
    }

    const handleCommand = command => {
      if (command === 'logout') {
        ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
          .then(async () => {
            await logout()
            sessionStorage.removeItem('admin')
            router.push('/login')
          })
          .catch(() => {})
      } else if (command === 'changePassword') {
        passwordDialogVisible.value = true
        passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      }
    }

    const handleChangePassword = async () => {
      await passwordFormRef.value.validate(async valid => {
        if (valid) {
          try {
            await changePassword({
              oldPassword: passwordForm.value.oldPassword,
              newPassword: passwordForm.value.newPassword
            })
            ElMessage.success('修改密码成功')
            passwordDialogVisible.value = false
          } catch (error) {
            ElMessage.error(error.message || '修改密码失败')
          }
        }
      })
    }

    return {
      activeMenu,
      passwordDialogVisible,
      passwordFormRef,
      passwordForm,
      passwordRules,
      handleCommand,
      handleChangePassword,
      isCollapse,
      toggleCollapse,
      tabs,
      activeTab,
      cachedViews,
      handleTabRemove,
      handleTabClick,
      contextMenuVisible,
      contextMenuStyle,
      onTabContextMenu,
      hideContextMenu,
      closeCurrentTab,
      closeOtherTabs,
      closeAllTabs
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  z-index: 10;
}

.logo-wrapper {
  height: 60px;
  line-height: 60px;
  background-color: #2b2f3a;
  text-align: center;
  overflow: hidden;
}

.logo {
  color: white;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  transition: all 0.3s;
}

.sidebar-menu {
  flex: 1;
  border-right: none !important;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-menu::-webkit-scrollbar {
  width: 6px;
}
.sidebar-menu::-webkit-scrollbar-thumb {
  background: #4a5a74;
  border-radius: 3px;
}
.sidebar-menu::-webkit-scrollbar-track {
  background: transparent;
}

.collapse-btn {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #263445;
  cursor: pointer;
  color: #bfcbd9;
  transition: all 0.3s;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

.collapse-btn:hover {
  background-color: #1f2d3d;
  color: #409EFF;
}

.header-wrapper {
  padding: 0;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 9;
}

.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}
.user-info:hover {
  color: #409EFF;
}

.tags-view-container {
  height: 34px;
  width: 100%;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.12), 0 0 3px 0 rgba(0, 0, 0, 0.04);
}

.menu-tabs {
  height: 34px;
}

.tags-view-container :deep(.el-tabs__header) {
  margin: 0;
  border-bottom: none;
}

.tags-view-container :deep(.el-tabs__nav) {
  border: none !important;
  border-radius: 0 !important;
}

.tags-view-container :deep(.el-tabs__item) {
  height: 34px;
  line-height: 34px;
  border: none !important;
  border-right: 1px solid #d8dce5 !important;
  color: #495060;
  background: #fff;
  padding: 0 15px !important;
  font-size: 12px;
  font-weight: normal;
}

.tags-view-container :deep(.el-tabs__item.is-active) {
  color: #409EFF;
  background-color: #eaf4ff;
  border-bottom: 2px solid #409EFF !important;
}

.tags-view-container :deep(.el-tabs__item:hover) {
  color: #409EFF;
  background-color: #f6f8fa;
}

.tab-label {
  display: inline-block;
  width: 100%;
}

.tab-context-menu {
  position: fixed;
  z-index: 9999;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 4px 0;
  font-size: 12px;
  color: #606266;
}

.tab-context-menu ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.tab-context-menu li {
  padding: 6px 16px;
  cursor: pointer;
  white-space: nowrap;
}

.tab-context-menu li:hover {
  background-color: #f5f7fa;
  color: #409EFF;
}

.app-main {
  min-height: calc(100vh - 84px);
  width: 100%;
  position: relative;
  overflow: auto;
  background-color: #f0f2f5;
  padding: 20px;
  box-sizing: border-box;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
