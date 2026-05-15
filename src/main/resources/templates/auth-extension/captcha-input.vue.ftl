<#-- 字符型验证码输入组件 -->
<template>
  <div class="captcha-row">
    <el-input
      v-model="innerCode"
      placeholder="请输入验证码"
      maxlength="6"
      clearable
      @keyup.enter="$emit('enter')"
    >
      <template #prefix>
        <el-icon><Key /></el-icon>
      </template>
    </el-input>
    <div class="captcha-image-wrap" title="点击刷新验证码" @click="refresh">
      <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-image" />
      <span v-else class="captcha-placeholder">加载中</span>
    </div>
  </div>
</template>

<script>
import { ref, watch, onMounted } from 'vue'
import { Key } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'

export default {
  name: 'CaptchaInput',
  components: { Key },
  props: {
    captchaKey: { type: String, default: '' },
    captchaCode: { type: String, default: '' }
  },
  emits: ['update:captchaKey', 'update:captchaCode', 'enter'],
  setup(props, { emit }) {
    const innerCode = ref(props.captchaCode || '')
    const captchaImage = ref('')
    const loading = ref(false)

    watch(() => props.captchaCode, (v) => {
      if (v !== innerCode.value) innerCode.value = v || ''
    })

    watch(innerCode, (v) => emit('update:captchaCode', v))

    const refresh = async () => {
      if (loading.value) return
      loading.value = true
      try {
        const res = await getCaptcha()
        if (res.code === 200 && res.data) {
          emit('update:captchaKey', res.data.captchaKey || '')
          captchaImage.value = res.data.captchaImage || ''
          innerCode.value = ''
          emit('update:captchaCode', '')
        }
      } finally {
        loading.value = false
      }
    }

    onMounted(refresh)

    return { innerCode, captchaImage, refresh }
  }
}
</script>

<style scoped>
.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
}
.captcha-row .el-input {
  flex: 1;
}
.captcha-image-wrap {
  width: 120px;
  height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.captcha-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.captcha-placeholder {
  font-size: 12px;
  color: #909399;
}
</style>
