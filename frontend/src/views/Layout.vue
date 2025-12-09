<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '200px'" class="sidebar">
      <!-- Logo 区域 -->
      <div class="logo-wrapper">
        <div class="logo" :class="{ 'collapsed': isCollapse }">
          <span v-show="!isCollapse">元数据管理</span>
          <span v-show="isCollapse">元</span>
        </div>
      </div>

      <!-- 菜单区域 -->
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
        <!-- ... 菜单项保持不变 ... -->
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

      <!-- 底部折叠按钮 (优化核心) -->
      <div class="collapse-btn" @click="toggleCollapse">
        <el-icon :size="20">
          <component :is="isCollapse ? 'Expand' : 'Fold'" />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
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
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>

    <!-- 修改密码对话框保持不变 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px" close-on-click-modal="false" close-on-press-escape="false">
      <!-- 表单内容保持不变 -->
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
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
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { logout, changePassword } from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入 Fold 和 Expand 图标，更符合折叠语义
import {
  Setting, Document, Grid, Menu, Connection, List, Edit, User, ArrowDown,
  Fold, Expand
} from '@element-plus/icons-vue'

export default {
  name: 'Layout',
  components: {
    // 注册图标组件以便动态使用
    Fold, Expand
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

    const activeMenu = computed(() => route.path)

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

    const handleCommand = (command) => {
      if (command === 'logout') {
        ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          await logout()
          sessionStorage.removeItem('admin')
          router.push('/login')
        }).catch(() => {})
      } else if (command === 'changePassword') {
        passwordDialogVisible.value = true
        passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
      }
    }

    const handleChangePassword = async () => {
      await passwordFormRef.value.validate(async (valid) => {
        if (valid) {
          try {
            await changePassword({
              oldPassword: passwordForm.value.oldPassword,
              newPassword: passwordForm.value.newPassword
            })
            ElMessage.success('修改密码成功')
            passwordDialogVisible.value = false
          } catch (error) {
            ElMessage.error('修改密码失败')
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
      toggleCollapse
    }
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 侧边栏整体布局优化：使用 Flex 纵向布局 */
.sidebar {
  background-color: #304156;
  height: 100%;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  overflow: hidden; /* 防止折叠时内容溢出 */
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35); /* 增加右侧阴影，更有层次感 */
  z-index: 10;
}

/* Logo 区域优化 */
.logo-wrapper {
  height: 60px;
  line-height: 60px;
  background-color: #2b2f3a; /* 比侧边栏稍深，突出 Logo */
  text-align: center;
  overflow: hidden;
}

.logo {
  color: white;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap; /* 防止文字换行 */
  transition: all 0.3s;
}

/* 菜单区域优化：自动填满剩余空间，去掉右侧边框 */
.sidebar-menu {
  flex: 1;
  border-right: none !important; /* 去掉 Element Menu 默认的右边框 */
  overflow-y: auto; /* 菜单过多时允许滚动 */
  overflow-x: hidden;
}

/* 滚动条样式微调 (Webkit内核) */
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

/* 底部折叠按钮优化：改为底部通栏样式 */
.collapse-btn {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #263445; /* 底部深色背景 */
  cursor: pointer;
  color: #bfcbd9;
  transition: all 0.3s;
  border-top: 1px solid rgba(255, 255, 255, 0.05); /* 顶部微弱分割线 */
}

.collapse-btn:hover {
  background-color: #1f2d3d;
  color: #409EFF;
}

/* Header 样式 */
.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
  height: 60px;
  box-shadow: 0 1px 4px rgba(0,21,41,.08); /* 头部增加轻微阴影 */
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

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>