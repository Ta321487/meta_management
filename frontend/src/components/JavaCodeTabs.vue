<template>
  <el-tabs v-model="activeTab" type="border-card" class="nested-tabs">
    <el-tab-pane label="SQL建表语句" name="sql">
      <CodeContainer
        code-type="sql"
        :code="codeMap.sql"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.sql"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="Entity实体类" name="entity">
      <CodeContainer
        code-type="entity"
        :code="codeMap.entity"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.entity"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="Controller" name="controller">
      <CodeContainer
        code-type="controller"
        :code="codeMap.controller"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.controller"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="Service" name="service">
      <CodeContainer
        code-type="service"
        :code="codeMap.service"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.service"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="Mapper接口" name="mapper">
      <CodeContainer
        code-type="mapper"
        :code="codeMap.mapper"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.mapper"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="Mapper.xml" name="mapperxml">
      <CodeContainer
        code-type="mapperxml"
        :code="codeMap.mapperxml"
        :table-code="tableCode"
        :is-visible="!!tableCode || !!codeMap.mapperxml"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="启动类" name="application">
      <CodeContainer
        code-type="application"
        :code="codeMap.application"
        placeholder="请点击'生成'按钮生成启动类代码"
        @refresh="handleRefresh"
        @copy="handleCopy"
      />
    </el-tab-pane>

    <el-tab-pane label="CORS配置" name="corsConfig">
      <CodeContainer
        code-type="corsConfig"
        :code="codeMap.corsConfig"
        placeholder="请点击'生成'按钮生成CORS配置代码"
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
