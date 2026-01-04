<template>
  <el-tabs v-model="activeTab" type="border-card" class="nested-tabs">
    <el-tab-pane label="配置文件" name="applicationYml">
      <CodeContainer
        code-type="applicationYml"
        :code="codeMap.applicationYml"
        :refresh-tooltip="'刷新当前标签页的配置文件代码'"
        placeholder="请点击'生成'按钮生成配置文件代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="MyBatis配置" name="mybatisConfig">
      <CodeContainer
        code-type="mybatisConfig"
        :code="codeMap.mybatisConfig"
        :refresh-tooltip="'刷新当前标签页的MyBatis配置代码'"
        placeholder="请点击'生成'按钮生成MyBatis配置代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="pom.xml" name="pomXml">
      <CodeContainer
        code-type="pomXml"
        :code="codeMap.pomXml"
        :refresh-tooltip="'刷新当前标签页的pom.xml代码'"
        placeholder="请点击'生成'按钮生成pom.xml代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="Result类" name="result">
      <CodeContainer
        code-type="result"
        :code="codeMap.result"
        :refresh-tooltip="'刷新当前标签页的Result类代码'"
        placeholder="请点击'生成'按钮生成Result类代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="PageRequest类" name="pageRequest">
      <CodeContainer
        code-type="pageRequest"
        :code="codeMap.pageRequest"
        :refresh-tooltip="'刷新当前标签页的PageRequest类代码'"
        placeholder="请点击'生成'按钮生成PageRequest类代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="PageResult类" name="pageResult">
      <CodeContainer
        code-type="pageResult"
        :code="codeMap.pageResult"
        :refresh-tooltip="'刷新当前标签页的PageResult类代码'"
        placeholder="请点击'生成'按钮生成PageResult类代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
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
    default: 'applicationYml'
  },
  // 代码映射
  codeMap: {
    type: Object,
    default: () => ({})
  }
});

// Emits
const emit = defineEmits(['update:modelValue', 'refresh', 'copy']);

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
</script>

<style scoped>
.nested-tabs {
  margin-top: 10px;
}
</style>
