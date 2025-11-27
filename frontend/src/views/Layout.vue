<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="sidebar">
      <div class="logo">元数据管理</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/module-type">
          <el-icon><Document /></el-icon>
          <span>模块类型管理</span>
        </el-menu-item>
        <el-menu-item index="/module">
          <el-icon><Setting /></el-icon>
          <span>模块管理</span>
        </el-menu-item>
        <el-menu-item index="/table">
          <el-icon><Grid /></el-icon>
          <span>表管理</span>
        </el-menu-item>
        <el-menu-item index="/field">
          <el-icon><Document /></el-icon>
          <span>字段管理</span>
        </el-menu-item>
        <el-menu-item index="/node">
          <el-icon><Menu /></el-icon>
          <span>功能节点</span>
        </el-menu-item>
        <el-menu-item index="/relation">
          <el-icon><Connection /></el-icon>
          <span>表关联</span>
        </el-menu-item>
        <el-menu-item index="/rule">
          <el-icon><List /></el-icon>
          <span>业务规则</span>
        </el-menu-item>
        <el-menu-item index="/log">
          <el-icon><Document /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="/codegen">
          <el-icon><Document /></el-icon>
          <span>代码生成</span>
        </el-menu-item>
        <el-menu-item index="/sql">
          <el-icon><Edit /></el-icon>
          <span>SQL执行</span>
        </el-menu-item>
      </el-menu>
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

    <!-- 修改密码对话框 -->
  <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px" close-on-click-modal="false" close-on-press-escape="false">
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

export default {
  name: 'Layout',
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

    const activeMenu = computed(() => route.path)

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
        }).catch(() => {
          // 处理用户取消退出登录操作，不做任何处理
        })
      } else if (command === 'changePassword') {
        passwordDialogVisible.value = true
        passwordForm.value = {
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
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
      handleChangePassword
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
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 0 20px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>

