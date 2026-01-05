<template>
  <!-- 登录页预览对话框 -->
  <el-dialog
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    v-model="localVisible"
    title="登录页样式预览"
    width="600px"
  >
    <div v-if="!loginCode" style="text-align: center; padding: 40px; color: #909399;">
      <p>请先生成登录页代码</p>
    </div>
    <div v-else>
      <!-- 直接渲染登录页样式 -->
      <div class="login-preview-container">
        <div class="login-box">
          <div class="login-title-wrapper">
            <el-icon class="login-icon"><Data /></el-icon> 
            <h2>{{ businessName || '登录系统' }}</h2>
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
            © 2025 {{ businessName || '登录系统' }}
          </div>
        </div>
      </div>
    </div>
    <template #footer>
      <el-button @click="localVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script>
import { ref, reactive, watch } from 'vue';
import { Data, User, Lock } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

export default {
  name: 'LoginPreview',
  components: {
    Data,
    User,
    Lock
  },
  props: {
    // 可见性
    visible: {
      type: Boolean,
      default: false
    },
    // 生成的登录页代码
    loginCode: {
      type: String,
      default: ''
    }
  },
  emits: ['update:visible'],
  setup(props, { emit }) {
    // Data
    // 内部可见性
    const localVisible = ref(props.visible);
    // 表单引用
    const loginFormRef = ref(null);
    // 加载状态
    const loading = ref(false);
    // 登录表单数据
    const loginForm = reactive({
      username: 'admin',
      password: 'admin'
    });
    // 表单规则
    const rules = {
      username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
      password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
    };
    // 业务系统名称
    const businessName = ref('');

    // Watch
    // 监听外部可见性变化
    watch(() => props.visible, (newVal) => {
      localVisible.value = newVal;
      if (newVal) {
        // 从生成的代码中提取业务系统名称
        extractBusinessName();
      }
    });

    // 监听内部可见性变化
    watch(() => localVisible.value, (newVal) => {
      emit('update:visible', newVal);
      if (!newVal) {
        // 隐藏时重置表单
        Object.assign(loginForm, { username: '', password: '' });
        if (loginFormRef.value) {
          loginFormRef.value.resetFields();
        }
      }
    });

    // 监听登录页代码变化
    watch(() => props.loginCode, () => {
      if (localVisible.value) {
        extractBusinessName();
      }
    });

    // Methods
    // 从生成的代码中提取业务系统名称
    const extractBusinessName = () => {
      if (!props.loginCode) return;
      
      // 尝试从生成的代码中提取业务系统名称
      const businessNameMatch = props.loginCode.match(/<h2>([^<]+)<\/h2>/);
      if (businessNameMatch) {
        businessName.value = businessNameMatch[1].trim();
      } else {
        businessName.value = '';
      }
    };

    // 处理登录
    const handleLogin = () => {
      if (loginFormRef.value) {
        loginFormRef.value.validate(async (valid) => {
          if (valid) {
            loading.value = true;
            try {
              // 模拟登录过程
              await new Promise(resolve => setTimeout(resolve, 1000));
              console.log('登录成功:', loginForm);
              ElMessage.info('此处为模拟登录，实际代码将跳转至业务系统主页');
            } catch (error) {
              console.error('登录失败:', error);
            } finally {
              loading.value = false;
            }
          }
        });
      }
    };

    return {
      localVisible,
      loginFormRef,
      loading,
      loginForm,
      rules,
      businessName,
      handleLogin
    };
  }
};
</script>

<style scoped>
.login-preview-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  background: linear-gradient(135deg, #4c6a9a 0%, #2f497a 100%);
  padding: 20px;
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