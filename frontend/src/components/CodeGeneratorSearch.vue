<template>
  <el-form :inline="true" :model="formData" class="search-form">
    <el-form-item label="业务系统">
      <el-select v-model="formData.businessCode" placeholder="请选择业务系统" style="width: 200px" @change="handleBusinessSystemChange">
        <el-option
          v-for="business in businessSystems"
          :key="business.businessCode"
          :label="business.businessName"
          :value="business.businessCode"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="选择表">
      <el-select v-model="formData.tableCode" placeholder="请选择表" style="width: 300px" @change="handleTableChange" clearable>
        <el-option
          v-for="table in tables"
          :key="table.tableCode"
          :label="table.tableName"
          :value="table.tableCode"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="包名">
      <div style="display: flex; align-items: center; width: 300px;">
        <el-input v-model="formData.packageName" placeholder="如：com.example" style="width: 270px" readonly />
        <el-tooltip placement="top" content="如需修改请到业务系统配置模块进行修改">
          <el-icon class="info-icon" style="margin-left: 5px; cursor: pointer; color: #909399;">
            <InfoFilled />
          </el-icon>
        </el-tooltip>
      </div>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="handleGenerateCode" :tooltip="generateTooltip">
        生成代码
      </el-button>
      <el-button type="success" @click="handleRunTest" :disabled="!formData.tableCode">测试代码</el-button>
      <el-button type="info" @click="handleShowDeploymentGuide">部署指南</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { InfoFilled } from '@element-plus/icons-vue';

// Props
const props = defineProps({
  // 表单数据
  form: {
    type: Object,
    default: () => ({
      businessCode: '',
      tableCode: '',
      packageName: ''
    })
  },
  // 业务系统列表
  businessSystems: {
    type: Array,
    default: () => []
  },
  // 表列表
  tables: {
    type: Array,
    default: () => []
  }
});

// Emits
const emit = defineEmits([
  'update:form',
  'business-system-change',
  'table-change',
  'generate-code',
  'run-test',
  'show-deployment-guide'
]);

// Data
// 本地表单数据，避免直接修改props
const formData = ref({ ...props.form });

// Watch
// 监听props.form变化，更新本地表单数据
watch(() => props.form, (newForm) => {
  formData.value = { ...newForm };
}, { deep: true });

// Computed
// 生成按钮提示
const generateTooltip = computed(() => {
  return '生成所有类型的代码';
});

// Methods
// 处理业务系统变化
const handleBusinessSystemChange = (value) => {
  formData.value.businessCode = value;
  emit('update:form', { ...formData.value });
  emit('business-system-change', value);
};

// 处理表变化
const handleTableChange = (value) => {
  formData.value.tableCode = value;
  emit('update:form', { ...formData.value });
  emit('table-change', value);
};

// 处理生成代码
const handleGenerateCode = () => {
  emit('generate-code');
};

// 处理运行测试
const handleRunTest = () => {
  emit('run-test');
};

// 处理显示部署指南
const handleShowDeploymentGuide = () => {
  emit('show-deployment-guide');
};
</script>

<style scoped>
.search-form {
  margin-bottom: 20px;
}

.info-icon {
  cursor: pointer;
}
</style>
