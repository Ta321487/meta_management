<template>
  <!-- 列表页预览对话框 -->
  <el-dialog
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    v-model="localVisible"
    title="列表页样式预览"
    width="1200px"
  >
    <div v-if="loading" style="text-align: center; padding: 40px;">
      <el-icon class="is-loading"><Loading /></el-icon>
      <p>加载中...</p>
    </div>
    <div v-else-if="fields.length === 0" style="text-align: center; padding: 40px; color: #909399;">
      <p>该表没有配置字段或字段信息加载失败</p>
    </div>
    <el-card v-else>
      <template #header>
        <div class="card-header">
          <span>{{ tableName }}</span>
          <div>
            <el-button 
              type="danger" 
              :disabled="multipleSelection.length === 0" 
              size="small" 
              style="margin-right: 10px"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
            <el-button type="primary" size="small" @click="handleAdd">新增</el-button>
          </div>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form" style="margin-bottom: 20px; padding: 20px; background-color: #f5f7fa; border-radius: 4px;">
        <el-form-item 
          v-for="field in searchFields" 
          :key="field.id"
          :label="field.label"
        >
          <el-input 
            v-if="field.formComponent === 'input'"
            v-model="searchForm[getListFieldPropName(field)]" 
            :placeholder="`请输入${field.label}`" 
            clearable 
            style="width: 180px"
          />
          <el-select 
            v-else-if="field.formComponent === 'select'"
            v-model="searchForm[getListFieldPropName(field)]" 
            :placeholder="`请选择${field.label}`" 
            clearable 
            style="width: 180px"
          >
            <el-option 
              v-for="option in getFormFieldOptions(field)"
              :key="option.value"
              :label="option.label" 
              :value="option.value" 
            />
          </el-select>
          <el-date-picker 
            v-else-if="field.formComponent === 'datepicker' || field.formComponent === 'date'"
            v-model="searchForm[getListFieldPropName(field)]" 
            type="date" 
            :placeholder="`请选择${field.label}`" 
            clearable 
            style="width: 180px"
          />
          <el-input-number 
            v-else-if="field.formComponent === 'number'"
            v-model="searchForm[getListFieldPropName(field)]" 
            :placeholder="`请输入${field.label}`" 
            clearable 
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="handleSearch">搜索</el-button>
          <el-button size="small" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table 
        :data="tableData" 
        border 
        style="width: 100%"
        v-loading="tableLoading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column 
          v-for="field in filteredFields" 
          :key="field.id"
          :prop="getListFieldPropName(field)" 
          :label="field.label"
          sortable="custom"
          @sort-change="(sort) => handleSortChange(getListFieldPropName(field), sort)"
        >
          <template #default="{ row }">
            <span v-if="field.fieldType?.includes('date') || field.fieldType?.includes('time')">
              {{ formatPreviewDate(row[getListFieldPropName(field)]) }}
            </span>
            <span v-else-if="field.fieldType?.includes('decimal') || field.fieldType?.includes('numeric')">
              {{ formatPreviewNumber(row[getListFieldPropName(field)]) }}
            </span>
            <span v-else>{{ row[getListFieldPropName(field)] }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      v-model="innerDialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form 
        :model="formData" 
        :rules="formRules" 
        ref="formRef" 
        label-width="auto"
        label-position="top"
      >
        <el-form-item 
          v-for="field in filteredFields" 
          :key="field.id"
          :label="field.label" 
          :prop="getListFieldPropName(field)"
          :required="field.isRequired === 1"
        >
          <!-- 主键字段不显示输入组件 -->
          <template v-if="field.formComponent === 'primary_key'">
            <el-input 
              v-model="formData[getListFieldPropName(field)]" 
              placeholder="主键自动生成" 
              readonly
            />
          </template>
          <!-- 数字输入框（针对数字类型字段，无论表单组件设置是什么） -->
          <template v-else-if="['INT', 'NUMBER', 'DECIMAL', 'BIGINT', 'TINYINT', 'NUMERIC'].includes(field.fieldType)">
            <el-input-number 
              v-model="formData[getListFieldPropName(field)]" 
              style="width: 100%"
            />
          </template>
          <!-- 日期选择器（针对日期时间类型字段，无论表单组件设置是什么） -->
          <template v-else-if="field.fieldType?.includes('date') || field.fieldType?.includes('time')">
            <el-date-picker 
              v-model="formData[getListFieldPropName(field)]" 
              type="datetime" 
              placeholder="请选择日期时间" 
              style="width: 100%" 
            />
          </template>
          <!-- 长文本类型（针对文本类型字段，无论表单组件设置是什么） -->
          <template v-else-if="field.fieldType?.includes('text') && field.fieldType?.length > 4">
            <el-input 
              v-model="formData[getListFieldPropName(field)]" 
              type="textarea" 
              :rows="3"
            />
          </template>
          <!-- 枚举类型（针对枚举类型字段，无论表单组件设置是什么） -->
          <template v-else-if="field.fieldType?.includes('enum')">
            <el-select 
              v-model="formData[getListFieldPropName(field)]" 
              placeholder="请选择" 
              style="width: 100%"
            >
              <el-option 
                v-for="option in getFormFieldOptions(field)"
                :key="option.value"
                :label="option.label" 
                :value="option.value" 
              />
            </el-select>
          </template>
          <!-- 布尔类型（针对布尔类型字段，无论表单组件设置是什么） -->
          <template v-else-if="field.fieldType?.includes('boolean')">
            <el-select 
              v-model="formData[getListFieldPropName(field)]" 
              placeholder="请选择" 
              style="width: 100%"
            >
              <el-option label="是" value="1" />
              <el-option label="否" value="0" />
            </el-select>
          </template>
          <!-- 按表单组件类型渲染（兼容旧配置） -->
          <template v-else>
            <!-- 输入框 -->
            <el-input 
              v-if="field.formComponent === 'input'"
              v-model="formData[getListFieldPropName(field)]" 
              :placeholder="`请输入${field.label}`"
            />
            <!-- 下拉选择 -->
            <el-select 
              v-else-if="field.formComponent === 'select'"
              v-model="formData[getListFieldPropName(field)]" 
              placeholder="请选择" 
              style="width: 100%"
            >
              <el-option 
                v-for="option in getFormFieldOptions(field)"
                :key="option.value"
                :label="option.label" 
                :value="option.value" 
              />
            </el-select>
            <!-- 日期选择器 -->
            <el-date-picker 
              v-else-if="field.formComponent === 'datepicker' || field.formComponent === 'date'"
              v-model="formData[getListFieldPropName(field)]" 
              type="date" 
              placeholder="请选择日期" 
              style="width: 100%" 
            />
            <!-- 数字输入框 -->
            <el-input-number 
              v-else-if="field.formComponent === 'number'"
              v-model="formData[getListFieldPropName(field)]" 
              style="width: 100%" 
            />
            <!-- 文本域 -->
            <el-input 
              v-else-if="field.formComponent === 'textarea'"
              v-model="formData[getListFieldPropName(field)]" 
              type="textarea" 
              :rows="3"
            />
            <!-- 默认输入框 -->
            <el-input 
              v-else
              v-model="formData[getListFieldPropName(field)]" 
              :placeholder="`请输入${field.label}`"
            />
          </template>
        </el-form-item>
      </el-form>
      <template #footer>
    <el-button @click="innerDialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmit">确定</el-button>
  </template>
</el-dialog>

    <template #footer>
      <el-button @click="localVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import {
  generateMockDataList,
  formatPreviewDate,
  formatPreviewNumber,
  getListFieldPropName,
  getFormFieldOptions
} from '../utils/mockDataGenerator';
import { processPatternRule } from '../utils/regexUtils';

// Props
const props = defineProps({
  // 可见性
  visible: {
    type: Boolean,
    default: false
  },
  // 加载状态
  loading: {
    type: Boolean,
    default: false
  },
  // 字段列表
  fields: {
    type: Array,
    default: () => []
  },
  // 表名
  tableName: {
    type: String,
    default: ''
  }
});

// Emits
const emit = defineEmits(['update:visible', 'update:loading']);

// Data
// 表单引用
const formRef = ref(null);
// 表格加载状态
const tableLoading = ref(false);
// 多选数据
const multipleSelection = ref([]);
// 表格数据
const tableData = ref([]);
// 搜索表单
const searchForm = ref({});
// 表单数据
const formData = ref({});
// 内部编辑对话框可见性
const innerDialogVisible = ref(false);
// 主对话框可见性
const localVisible = ref(props.visible);
// 表单对话框标题
const dialogTitle = ref('新增');
// 当前编辑行
const currentEditRow = ref(null);
// 分页信息
const pagination = ref({
  current: 1,
  size: 10,
  total: 100
});

// Watch
// 监听外部可见性变化
watch(() => props.visible, (newVal) => {
  localVisible.value = newVal;
});

// 监听内部可见性变化
watch(() => localVisible.value, (newVal) => {
  emit('update:visible', newVal);
});

// Computed
// 过滤后的字段列表（移除主键字段和无效字段）
const filteredFields = computed(() => {
  return props.fields.filter(field => {
    return field && 
           field.formComponent !== 'primary_key' && 
           field.formComponent !== 'primary_key' && 
           field.fieldName !== 'uuid' &&
           field.fieldName !== 'create_time' &&
           field.fieldName !== 'update_time';
  });
});

// 搜索字段（过滤掉长文本和大字段，且不包含主键字段和文本域）
const searchFields = computed(() => {
  return filteredFields.value.filter(field => {
    const type = (field.fieldType || '').toLowerCase();
    const formComponent = field.formComponent || '';
    
    // 排除文本域类型
    if (formComponent === 'textarea') {
      return false;
    }
    
    // 包含所有适合搜索的组件类型
    return (formComponent === 'input' || 
            formComponent === 'select' || 
            formComponent === 'datepicker' || 
            formComponent === 'date' || 
            formComponent === 'number') &&
           // 过滤掉长文本类型，除了varchar
           (!type.includes('text') || type.includes('varchar'));
  }).slice(0, 4); // 最多显示4个搜索字段
});

// Watch
// 监听可见性变化，生成模拟数据
watch(() => props.visible, (newVal) => {
  if (newVal && props.fields.length > 0) {
    generateTableData();
  }
});

// 监听fields变化，重新生成模拟数据
watch(() => props.fields, (newVal) => {
  if (newVal && newVal.length > 0) {
    generateTableData();
  }
}, { deep: true });

// 组件挂载时生成模拟数据
onMounted(() => {
  if (props.visible && props.fields.length > 0) {
    generateTableData();
  }
});

// Methods
// 生成表格数据
const generateTableData = () => {
  tableLoading.value = true;
  setTimeout(() => {
    tableData.value = generateMockDataList(props.fields, pagination.value.size);
    tableLoading.value = false;
  }, 500);
};

// 处理选择变化
const handleSelectionChange = (selection) => {
  multipleSelection.value = selection;
};

// 处理排序变化
const handleSortChange = (field, sort) => {
  // 模拟排序
  console.log('排序字段:', field, '排序方式:', sort.order);
};

// 处理分页大小变化
const handleSizeChange = (size) => {
  pagination.value.size = size;
  generateTableData();
};

// 处理当前页变化
const handleCurrentChange = (current) => {
  pagination.value.current = current;
  generateTableData();
};

// 处理搜索
const handleSearch = () => {
  // 模拟搜索
  console.log('搜索条件:', searchForm.value);
};

// 处理重置
const handleReset = () => {
  searchForm.value = {};
};

// 处理新增
const handleAdd = () => {
  dialogTitle.value = '新增';
  formData.value = {};
  currentEditRow.value = null;
  innerDialogVisible.value = true;
};

// 处理编辑
const handleEdit = (row) => {
  dialogTitle.value = '编辑';
  formData.value = { ...row };
  currentEditRow.value = row;
  innerDialogVisible.value = true;
};

// 处理删除
const handleDelete = (row) => {
  // 模拟删除
  const index = tableData.value.findIndex(item => item === row);
  if (index > -1) {
    tableData.value.splice(index, 1);
  }
};

// 处理批量删除
const handleBatchDelete = () => {
  // 模拟批量删除
  multipleSelection.value.forEach(item => {
    const index = tableData.value.findIndex(row => row === item);
    if (index > -1) {
      tableData.value.splice(index, 1);
    }
  });
  multipleSelection.value = [];
};

// 处理对话框关闭
const handleDialogClose = () => {
  formData.value = {};
  currentEditRow.value = null;
};

// 处理表单提交
const handleSubmit = () => {
  if (formRef.value) {
    formRef.value.validate((valid) => {
      if (valid) {
        if (currentEditRow.value) {
          // 编辑模式
          Object.assign(currentEditRow.value, formData.value);
        } else {
          // 新增模式
          tableData.value.unshift({ ...formData.value });
        }
        innerDialogVisible.value = false;
      }
    });
  }
};

// 表单规则
const formRules = computed(() => {
  const rules = {};
  props.fields.forEach(field => {
    const propName = getListFieldPropName(field);
    
    // 初始化该字段的规则数组
    if (!rules[propName]) {
      rules[propName] = [];
    }
    
    // 必填验证规则
    if (field.isRequired === 1) {
      rules[propName].push({ required: true, message: `请输入${field.label}`, trigger: 'blur' });
    }
    
    // 解析并添加自定义验证规则
    if (field.validateRule) {
      try {
        const validateRule = JSON.parse(field.validateRule);
        let rulesToAdd = [];
        
        // 处理单个对象形式的约束
        const extractRulesFromRule = (rule) => {
          // 处理pattern约束
          if (rule.pattern) {
            // 使用公共工具函数处理pattern规则
            const processedRule = processPatternRule(rule);
            if (processedRule === null) {
              // 规则无效，跳过该规则
              return;
            }
            rulesToAdd.push(processedRule);
          }
          // 处理min/max约束
          else if (rule.min !== undefined || rule.max !== undefined) {
            console.log(`处理min/max约束: ${JSON.stringify(rule)}`);
            console.log(`字段名: ${field.fieldName}, 字段类型: ${field.fieldType}, 表单组件: ${field.formComponent}`);
            
            const minMaxRule = {
              message: rule.message || '',
              trigger: rule.trigger || 'blur'
            };
            
            // 添加min约束
            if (rule.min !== undefined) {
              minMaxRule.min = Number(rule.min);
              console.log(`添加min约束: ${minMaxRule.min}`);
            }
            
            // 添加max约束
            if (rule.max !== undefined) {
              minMaxRule.max = Number(rule.max);
              console.log(`添加max约束: ${minMaxRule.max}`);
            }
            
            // 如果是数字类型，添加type: 'number'
            const isNumberInput = field.formComponent === 'number' || ['INT', 'NUMBER', 'DECIMAL'].includes(field.fieldType);
            console.log(`是否为数字输入框: ${isNumberInput}`);
            
            if (isNumberInput) {
              minMaxRule.type = 'number';
              console.log(`添加type: 'number'约束`);
            }
            
            console.log(`最终minMaxRule: ${JSON.stringify(minMaxRule)}`);
            rulesToAdd.push(minMaxRule);
          } else {
            // 其他约束，直接添加
            rulesToAdd.push(rule);
          }
        };
        
        // 处理单个对象
        if (typeof validateRule === 'object' && !Array.isArray(validateRule)) {
          extractRulesFromRule(validateRule);
        }
        // 处理数组形式
        else if (Array.isArray(validateRule)) {
          for (const rule of validateRule) {
            extractRulesFromRule(rule);
          }
        }
        
        // 添加规则
        if (rulesToAdd.length > 0) {
          rules[propName] = [...rules[propName], ...rulesToAdd];
        }
      } catch (e) {
        console.error('Failed to parse validateRule:', e);
      }
    }
  });
  return rules;
});

// 初始化搜索表单
onMounted(() => {
  if (props.fields.length > 0) {
    generateTableData();
  }
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>
