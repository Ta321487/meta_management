<template>
  <div class="welcome-container">
    <div class="welcome-content">
      <h1 class="welcome-title">欢迎使用通用元数据管理系统</h1>
      <p class="welcome-desc">请从左侧菜单选择功能进行操作</p>
    </div>
  </div>
</template>

<script>
import { ref } from 'vue'

export default {
  name: 'Welcome',
  setup() {
    const hasLogo = ref(false)
    // 检查是否有logo图片，这里简单处理，假设没有
    return { hasLogo }
  }
}
</script>

<style scoped>
.welcome-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.welcome-content {
  text-align: center;
}

.welcome-logo {
  width: 120px;
  margin-bottom: 20px;
}

.welcome-title {
  font-size: 32px;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 600;
}

.welcome-desc {
  font-size: 16px;
  color: #909399;
}
</style>
