<template>
  <el-tabs v-model="activeTab" type="border-card" class="nested-tabs">
    <el-tab-pane label="Vue列表页" name="vueList">
      <CodeContainer
        code-type="vueList"
        :code="codeMap.vueList"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.vueList"
        :show-preview="true"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @preview="handlePreview"
      />
    </el-tab-pane>

    <el-tab-pane label="Vue表单页" name="vueForm">
      <CodeContainer
        code-type="vueForm"
        :code="codeMap.vueForm"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.vueForm"
        :show-preview="true"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @preview="handlePreview"
      />
    </el-tab-pane>

    <el-tab-pane label="登录页" name="login">
      <CodeContainer
        code-type="login"
        :code="codeMap.login"
        :business-code="businessCode"
        :disabled="!businessCode"
        :refresh-tooltip="'刷新当前标签页的登录页代码'"
        :show-download="true"
        :show-preview="true"
        placeholder="请点击'生成'按钮生成登录页代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
        @preview="handlePreview"
      />
    </el-tab-pane>

    <el-tab-pane label="Routes" name="routes">
      <CodeContainer
        code-type="routes"
        :code="codeMap.routes"
        :table-code="tableCode"
        :business-code="businessCode"
        :is-visible="!!tableCode || !!codeMap.routes || !!businessCode"
        :disabled="!tableCode"
        :refresh-tooltip="'刷新当前标签页的路由配置代码'"
        :show-download="true"
        :show-generate-integrated="true"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
        @generate-integrated="handleGenerateIntegrated"
      />
    </el-tab-pane>

    <el-tab-pane label="API请求文件" name="api">
      <CodeContainer
        code-type="api"
        :code="codeMap.api"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.api"
        :disabled="!tableCode"
        :refresh-tooltip="'刷新当前标签页的API请求文件代码'"
        :show-download="true"
        placeholder="请点击'生成'按钮生成API请求文件代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
      />
    </el-tab-pane>

    <el-tab-pane label="请求工具类" name="requestJs">
      <CodeContainer
        code-type="requestJs"
        :code="codeMap.requestJs"
        :show-download="true"
        placeholder="请点击'生成'按钮生成请求工具类代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
      />
    </el-tab-pane>

    <el-tab-pane label="认证API文件" name="auth">
      <CodeContainer
        code-type="auth"
        :code="codeMap.auth"
        :show-download="true"
        placeholder="请点击'生成'按钮生成认证API文件代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
      />
    </el-tab-pane>

    <el-tab-pane v-if="codeMap.captchaInput" label="验证码组件" name="captchaInput">
      <CodeContainer
        code-type="captchaInput"
        :code="codeMap.captchaInput"
        :show-download="true"
        placeholder="请勾选「字符型验证码」后生成"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
      />
    </el-tab-pane>

    <el-tab-pane label=".env配置" name="env">
      <CodeContainer
        code-type="env"
        :code="codeMap.env"
        :show-download="true"
        :rows="10"
        placeholder="请点击'生成'按钮生成环境配置代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
      />
    </el-tab-pane>
  </el-tabs>
</template>

<script setup>
import { ref, watch } from 'vue';
import CodeContainer from './CodeContainer.vue';

// Props
const props = defineProps({
  // 活动标签
  modelValue: {
    type: String,
    default: 'vueList'
  },
  // 代码映射
  codeMap: {
    type: Object,
    default: () => ({})
  },
  // 表代码
  tableCode: {
    type: String,
    default: ''
  },
  // 业务代码
  businessCode: {
    type: String,
    default: ''
  }
});

// Emits
const emit = defineEmits(['update:modelValue', 'refresh', 'copy', 'download', 'preview', 'generate-integrated']);

// Data
// 活动标签
const activeTab = ref(props.modelValue);

// Watch
// 监听标签变化
watch(() => activeTab.value, (newVal) => {
  emit('update:modelValue', newVal);
});

// 监听外部标签变化
watch(() => props.modelValue, (newVal) => {
  activeTab.value = newVal;
});

// Methods
// 处理刷新
const handleRefresh = (codeType) => {
  emit('refresh', codeType);
};

// 处理复制
const handleCopy = (codeType) => {
  emit('copy', codeType);
};

// 处理下载
const handleDownload = (codeType, fileName) => {
  emit('download', codeType, fileName);
};

// 处理预览
const handlePreview = (codeType) => {
  emit('preview', codeType);
};

// 处理生成整合路由
const handleGenerateIntegrated = () => {
  emit('generate-integrated');
};
</script>

<style scoped>
.nested-tabs {
  margin-top: 10px;
}
</style>
