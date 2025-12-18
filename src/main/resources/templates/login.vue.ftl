<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title-wrapper">
        <el-icon class="login-icon"><Data /></el-icon> 
        <h2>${businessName!"Metadata Management System"}</h2>
        <p class="subtitle">全面、高效、可信赖的数据资产管理</p>
      </div>
      
      <el-form 
        :model="loginForm" 
        :rules="rules" 
        ref="loginFormRef" 
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入用户名">
            <template #prefix>
              <el-icon><User /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码" 
            show-password 
            @keyup.enter="handleLogin" 
          >
            <template #prefix>
              <el-icon><Lock /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleLogin" 
            :loading="loading" 
            class="login-button"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-text">
        © 2025 ${businessName!"Metadata Management System"}
      </div>
    </div>
  </div>
</template>
<script>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api'
import { ElMessage } from 'element-plus'
import { User, Lock, Data } from '@element-plus/icons-vue' 

export default {
  name: 'Login',
  components: {
    User,
    Lock,
    Data 
  },
  setup() {
    const router = useRouter()
    const loginFormRef = ref(null)
    const loading = ref(false)
    const loginForm = reactive({
      username: 'admin',
      password: ''
    })
    const rules = {
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
    }

    const handleLogin = async () => {
      await loginFormRef.value.validate(async (valid) => {
        if (valid) {
          loading.value = true
          try {
            const res = await login(loginForm)
            if (res.code === 200) {
              sessionStorage.setItem('admin', JSON.stringify(res.data))
              ElMessage.success('登录成功')
              router.push('/')
            } else {
              ElMessage.error(res.message || '登录失败，请检查用户名和密码')
            }
          } catch (error) {
            ElMessage.error(error.message || '登录失败，网络或服务器错误')
          } finally {
            loading.value = false
          }
        }
      })
    }

    return {
      loginFormRef,
      loginForm,
      rules,
      loading,
      handleLogin
    }
  }
}
</script>

<style scoped>
/* 1. 全局容器美化 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #4c6a9a 0%, #2f497a 100%); /* 深蓝渐变 */
  position: fixed;
  top: 0;
  left: 0;
  overflow: hidden; /* 隐藏滚动条 */
}

/* 2. 登录卡片优化 */
.login-box {
  width: 380px; /* 略微缩小，更精致 */
  padding: 40px;
  background: #ffffff;
  border-radius: 12px; /* 更圆润的边角 */
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2); 
  z-index: 10; /* 确保在背景之上 */
  transition: transform 0.3s ease-in-out;
}

.login-box:hover {
  transform: translateY(-5px); /* 悬停微动效果 */
}

/* 3. 标题和副标题优化 */
.login-title-wrapper {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee; /* 分隔线 */
}

.login-box h2 {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-top: 10px;
  margin-bottom: 5px;
}

/* 图标样式 */
.login-icon {
  font-size: 40px;
  color: #4c6a9a; /* 与背景色系呼应的主题色 */
}

.subtitle {
  font-size: 14px;
  color: #999;
  margin-top: 5px;
}

/* 4. 表单和输入框优化 */
.login-form {
  margin-top: 20px;
}

/* 移除 Element Plus 默认的底部 margin，让表单项更紧凑 */
.login-form .el-form-item {
    margin-bottom: 25px;
}

/* 5. 登录按钮优化 */
.login-button {
  width: 100%;
  height: 45px; /* 增加高度，更容易点击 */
  font-size: 16px;
  /* 使用主题色 */
  background-color: #4c6a9a; 
  border-color: #4c6a9a;
  letter-spacing: 1px; /* 增加字母间距 */
  margin-top: 10px; /* 与输入框保持距离 */
}

.login-button:hover {
  background-color: #5c7baf;
  border-color: #5c7baf;
}

/* 6. 底部版权信息 */
.footer-text {
  text-align: center;
  margin-top: 30px;
  font-size: 12px;
  color: #aaa;
}
</style>