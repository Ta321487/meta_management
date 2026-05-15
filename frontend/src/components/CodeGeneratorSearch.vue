<template>
  <el-form :model="formData" label-width="100px" class="search-form-container">
    <el-row :gutter="20">
      <el-col :xs="24" :sm="8" :md="8" :lg="8">
        <el-form-item label="业务系统">
          <el-select v-model="formData.businessCode" placeholder="请选择业务系统" class="full-width" @change="handleBusinessSystemChange">
            <el-option
              v-for="business in businessSystems"
              :key="business.businessCode"
              :label="business.businessName"
              :value="business.businessCode"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8" :lg="8">
        <el-form-item label="选择表">
          <el-select v-model="formData.tableCode" placeholder="请选择表" class="full-width" @change="handleTableChange" clearable>
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="8" :md="8" :lg="8">
        <el-form-item label="包名">
          <el-input v-model="formData.packageName" placeholder="如：com.example" readonly>
            <template #suffix>
              <el-tooltip placement="top" content="如需修改请到业务系统配置模块进行修改">
                <el-icon class="info-icon" style="cursor: help;"><InfoFilled /></el-icon>
              </el-tooltip>
            </template>
          </el-input>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-form-item label="生成模式">
          <div class="mode-container">
            <el-switch 
              v-model="formData.useInterface" 
              active-text="接口 + 实现类" 
              inactive-text="传统 Service 类"
              @change="handleUseInterfaceChange"
            />
            <span class="mode-tip">（模式影响生成的代码架构分层）</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :span="24">
        <el-form-item label="认证扩展">
          <div class="mode-container">
            <el-switch
              v-model="formData.captchaEnabled"
              active-text="字符型验证码"
              inactive-text="不生成验证码"
              @change="handleCaptchaEnabledChange"
            />
            <span class="mode-tip">（勾选后生成 CaptchaService、AuthController、CaptchaInput.vue 等）</span>
          </div>
        </el-form-item>
      </el-col>
    </el-row>

    <div class="form-actions">
      <el-button type="primary" @click="handleGenerateCode">
        <el-icon style="margin-right: 4px"><Finished /></el-icon>
        生成代码
      </el-button>
      <el-button type="success" @click="handleRunTest" :disabled="!formData.tableCode">测试代码</el-button>
      <el-button type="info" plain @click="handleShowDeploymentGuide">部署指南</el-button>
    </div>
  </el-form>
</template>

<script setup>
import { computed, ref, watch } from 'vue';
import { InfoFilled, Finished } from '@element-plus/icons-vue';

const props = defineProps({
  form: {
    type: Object,
    default: () => ({
      businessCode: '',
      tableCode: '',
      packageName: '',
      useInterface: false,
      captchaEnabled: false
    })
  },
  businessSystems: { type: Array, default: () => [] },
  tables: { type: Array, default: () => [] }
});

const emit = defineEmits([
  'update:form',
  'business-system-change',
  'table-change',
  'generate-code',
  'run-test',
  'show-deployment-guide'
]);

const formData = ref({ ...props.form });

watch(() => props.form, (newForm) => {
  formData.value = { ...newForm };
}, { deep: true });

const handleBusinessSystemChange = (value) => {
  emit('update:form', { ...formData.value });
  emit('business-system-change', value);
};

const handleUseInterfaceChange = (value) => {
  emit('update:form', { ...formData.value });
};

const handleCaptchaEnabledChange = () => {
  emit('update:form', { ...formData.value });
};

const handleTableChange = (value) => {
  emit('update:form', { ...formData.value });
  emit('table-change', value);
};

const handleGenerateCode = () => emit('generate-code');
const handleRunTest = () => emit('run-test');
const handleShowDeploymentGuide = () => emit('show-deployment-guide');
</script>

<style scoped>
.search-form-container {
  padding: 20px;
  background-color: #ffffff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 20px;
}

.full-width {
  width: 100%;
}

.mode-container {
  display: flex;
  align-items: center;
  height: 32px;
}

.mode-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

.form-actions {
  display: flex;
  justify-content: flex-start;
  gap: 10px;
  margin-top: 10px;
  padding-top: 15px;
  border-top: 1px dashed #dcdfe6;
}

.info-icon {
  font-size: 16px;
  color: #909399;
}
</style>