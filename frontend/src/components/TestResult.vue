<template>
  <el-card class="test-card" v-if="testResult" style="margin-top: 20px">
    <template #header>
      <div class="card-header">
        <span>代码测试结果</span>
        <div>
          <el-button 
            type="text" 
            size="small" 
            @click="toggleTestArea"
            style="margin-right: 10px; padding: 0">
            <el-icon style="vertical-align: middle">
              <ArrowUp v-if="testAreaExpanded" /> 
              <ArrowDown v-else /> 
            </el-icon>
            <span style="margin-left: 5px">{{ testAreaExpanded ? '折叠' : '展开' }}</span>
          </el-button>
          <el-button type="primary" size="small" @click="handleRerunTest">重新测试</el-button>
        </div>
      </div>
    </template>
    
    <div v-show="testAreaExpanded">
      <div v-if="testResult.success" class="test-success">
        <el-icon><Check /></el-icon>
        <span>所有测试通过！</span>
      </div>
      <div v-else class="test-error">
        <el-icon><Close /></el-icon>
        <span>部分测试失败</span>
      </div>

      <div style="margin-top: 10px; color: #909399; font-size: 14px">
        总计: {{ testResult.total }} | 
        通过: <span style="color: #67c23a">{{ testResult.successCount }}</span> | 
        失败: <span style="color: #f56c6c">{{ testResult.failCount }}</span>
      </div>

      <el-divider />

      <el-collapse v-model="activeTestItems">
        <el-collapse-item 
          v-for="(test, index) in testResult.testResults" 
          :key="index"
          :title="test.name + ' - ' + test.type"
          :name="index">
          <div class="test-detail">
            <el-tag :type="test.status === 'success' ? 'success' : test.status === 'warning' ? 'warning' : 'danger'" style="margin-bottom: 10px">
              {{ test.status === 'success' ? '通过' : test.status === 'warning' ? '警告' : '失败' }}
            </el-tag>
            <p style="margin: 10px 0">{{ test.message }}</p>
            
            <!-- 显示错误 -->
            <div v-if="test.errors && test.errors.length > 0" class="test-errors">
              <h4 style="color: #f56c6c; margin: 10px 0 5px 0">错误：</h4>
              <ul style="margin: 0; padding-left: 20px">
                <li v-for="(error, i) in test.errors" :key="i" style="margin: 5px 0">{{ error }}</li>
              </ul>
            </div>
            
            <!-- 显示警告 -->
            <div v-if="test.warnings && test.warnings.length > 0" class="test-warnings">
              <h4 style="color: #e6a23c; margin: 10px 0 5px 0">警告：</h4>
              <ul style="margin: 0; padding-left: 20px">
                <li v-for="(warning, i) in test.warnings" :key="i" style="margin: 5px 0">{{ warning }}</li>
              </ul>
            </div>
            
            <!-- API测试详情 -->
            <div v-if="test.apiTests" class="api-tests" style="margin-top: 15px">
              <h4 style="margin: 10px 0 5px 0">API接口测试：</h4>
              <el-table :data="test.apiTests" size="small" border style="margin-top: 10px">
                <el-table-column prop="method" label="方法" width="80" />
                <el-table-column prop="path" label="路径" />
                <el-table-column prop="name" label="名称" />
                <el-table-column label="状态" width="100">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'">
                      {{ scope.row.status === 'success' ? '✓' : '✗' }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </el-card>
</template>

<script setup>
import { ref } from 'vue';
import { ArrowUp, ArrowDown, Check, Close } from '@element-plus/icons-vue';

// Props
const props = defineProps({
  // 测试结果
  testResult: {
    type: Object,
    default: null
  }
});

// Emits
const emit = defineEmits(['rerun-test']);

// Data
// 测试区域展开状态
const testAreaExpanded = ref(true);
// 激活的测试项
const activeTestItems = ref([]);

// Methods
// 切换测试区域展开/折叠
const toggleTestArea = () => {
  testAreaExpanded.value = !testAreaExpanded.value;
};

// 处理重新测试
const handleRerunTest = () => {
  emit('rerun-test');
};
</script>

<style scoped>
.test-card {
  margin-top: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-success {
  display: flex;
  align-items: center;
  color: #67c23a;
  margin-bottom: 10px;
}

.test-success .el-icon {
  margin-right: 5px;
  font-size: 18px;
}

.test-error {
  display: flex;
  align-items: center;
  color: #f56c6c;
  margin-bottom: 10px;
}

.test-error .el-icon {
  margin-right: 5px;
  font-size: 18px;
}

.test-detail {
  padding: 10px 0;
}

.test-errors, .test-warnings {
  margin-top: 10px;
}

.test-errors h4, .test-warnings h4 {
  margin: 10px 0 5px 0;
  font-size: 14px;
}

.test-errors ul, .test-warnings ul {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
}

.test-errors li, .test-warnings li {
  margin: 5px 0;
}

.api-tests h4 {
  margin: 10px 0 5px 0;
  font-size: 14px;
}
</style>
