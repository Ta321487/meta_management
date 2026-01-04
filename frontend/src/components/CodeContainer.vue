<template>
  <div class="code-container" v-if="isVisible || localCode">
    <div class="code-header">
      <span>{{ fileName }}</span>
      <div v-if="showButtons">
        <el-button type="primary" size="small" @click="handleRefresh" :disabled="disabled" :tooltip="refreshTooltip">
          刷新
        </el-button>
        <el-button type="success" size="small" @click="handleCopy">复制</el-button>
        <el-button 
          type="warning" 
          size="small" 
          @click="handlePreview" 
          :disabled="!canPreview" 
          v-if="showPreview"
        >
          预览样式
        </el-button>
        <el-button 
          type="info" 
          size="small" 
          @click="handleDownload" 
          v-if="showDownload"
        >
          下载
        </el-button>
        <el-button 
          type="warning" 
          size="small" 
          @click="handleGenerateIntegrated" 
          :disabled="!canGenerateIntegrated" 
          v-if="showGenerateIntegrated"
        >
          生成整合路由
        </el-button>
      </div>
    </div>
    <el-input
      v-model="localCode"
      type="textarea"
      :rows="rows || 15"
      readonly
      class="code-textarea"
      :placeholder="placeholder"
    />
  </div>
  <div v-else style="text-align: center; padding: 40px; color: #909399;">
    <p>{{ emptyText }}</p>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { getFileName } from '../utils/codeGeneratorUtils';

// Props
const props = defineProps({
  // 代码类型
  codeType: {
    type: String,
    required: true
  },
  // 代码内容
  code: {
    type: String,
    default: ''
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
  },
  // 是否可见
  isVisible: {
    type: Boolean,
    default: true
  },
  // 是否显示按钮
  showButtons: {
    type: Boolean,
    default: true
  },
  // 是否显示预览按钮
  showPreview: {
    type: Boolean,
    default: false
  },
  // 是否显示下载按钮
  showDownload: {
    type: Boolean,
    default: false
  },
  // 是否显示生成整合路由按钮
  showGenerateIntegrated: {
    type: Boolean,
    default: false
  },
  // 是否禁用按钮
  disabled: {
    type: Boolean,
    default: false
  },
  // 行数
  rows: {
    type: Number,
    default: 15
  },
  // 占位符
  placeholder: {
    type: String,
    default: '请点击\'生成\'按钮生成代码'
  },
  // 空文本
  emptyText: {
    type: String,
    default: '请先选择业务系统和表。如不选择表，将只生成SQL.'
  },
  // 刷新按钮提示
  refreshTooltip: {
    type: String,
    default: '刷新当前标签页的代码'
  }
});

// Emits
const emit = defineEmits([
  'refresh',
  'copy',
  'download',
  'preview',
  'generate-integrated'
]);

// Data
// 本地代码内容
const localCode = ref(props.code);

// Computed
// 文件名
const fileName = computed(() => {
  return getFileName(props.codeType, props.tableCode);
});

// 是否可以预览
const canPreview = computed(() => {
  return !!props.tableCode;
});

// 是否可以生成整合路由
const canGenerateIntegrated = computed(() => {
  return !!props.businessCode;
});

// Watch
// 监听外部代码变化
watch(() => props.code, (newVal) => {
  localCode.value = newVal;
});

// Methods
// 处理刷新
const handleRefresh = () => {
  emit('refresh', props.codeType);
};

// 处理复制
const handleCopy = () => {
  emit('copy', props.codeType);
};

// 处理下载
const handleDownload = () => {
  emit('download', props.codeType, fileName.value);
};

// 处理预览
const handlePreview = () => {
  emit('preview', props.codeType);
};

// 处理生成整合路由
const handleGenerateIntegrated = () => {
  emit('generate-integrated');
};
</script>

<style scoped>
.code-container {
  margin-bottom: 20px;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 0 5px;
}

.code-header span {
  font-weight: bold;
  color: #333;
}

.code-header > div {
  display: flex;
  gap: 10px;
}

.code-textarea {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.5;
}
</style>
