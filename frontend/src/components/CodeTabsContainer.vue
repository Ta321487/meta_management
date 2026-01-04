<template>
  <el-collapse v-model="activeCollapse" accordion>
    <!-- Java相关 -->
    <el-collapse-item title="Java相关" name="java">
      <JavaCodeTabs
        v-model="localActiveTab"
        :code-map="codeMap"
        :table-code="tableCode"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-collapse-item>

    <!-- 前端相关 -->
    <el-collapse-item title="前端相关" name="frontend">
      <FrontendCodeTabs
        v-model="localActiveTab"
        :code-map="codeMap"
        :table-code="tableCode"
        :business-code="businessCode"
        @refresh="handleRefresh"
        @copy="handleCopy"
        @download="handleDownload"
        @preview="handlePreview"
        @generate-integrated="handleGenerateIntegrated"
      />
    </el-collapse-item>

    <!-- 工具相关 -->
    <el-collapse-item title="工具相关" name="tools">
      <ToolsCodeTabs
        v-model="localActiveTab"
        :code-map="codeMap"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-collapse-item>
  </el-collapse>
</template>

<script setup>
import { ref, watch } from 'vue';
import JavaCodeTabs from './JavaCodeTabs.vue';
import FrontendCodeTabs from './FrontendCodeTabs.vue';
import ToolsCodeTabs from './ToolsCodeTabs.vue';

// Props
const props = defineProps({
  // 激活的折叠面板
  modelValue: {
    type: String,
    default: 'java'
  },
  // 激活的标签页
  activeTab: {
    type: String,
    default: 'sql'
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
const emit = defineEmits(['update:modelValue', 'update:activeTab', 'refresh', 'copy', 'download', 'preview', 'generate-integrated']);

// Data
// 激活的折叠面板
const activeCollapse = ref(props.modelValue);
// 本地激活标签
const localActiveTab = ref(props.activeTab);

// Watch
// 监听折叠面板变化
watch(() => activeCollapse.value, (newVal) => {
  emit('update:modelValue', newVal);
});

// 监听外部折叠面板变化
watch(() => props.modelValue, (newVal) => {
  activeCollapse.value = newVal;
});

// 监听本地激活标签变化
watch(() => localActiveTab.value, (newVal) => {
  emit('update:activeTab', newVal);
});

// 监听外部激活标签变化
watch(() => props.activeTab, (newVal) => {
  localActiveTab.value = newVal;
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
/* 可以在这里添加自定义样式 */
</style>
