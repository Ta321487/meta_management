<template>
  <!-- 表单预览对话框 -->
  <el-dialog
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    v-model="localVisible"
    title="表单样式预览"
    width="900px"
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
          <span>{{ tableName }}表单</span>
        </div>
      </template>
      <el-form 
        :model="formData" 
        :rules="formRules" 
        ref="formRef" 
        label-width="100px"
      >
        <el-form-item 
          v-for="field in filteredFields" 
          :key="field.id"
          :label="field.label" 
          :prop="getFormFieldPropName(field)"
          :required="field.isRequired === 1"
        >
          <!-- 输入框 -->
          <el-input 
            v-if="field.formComponent === 'input' && !['INT', 'NUMBER', 'DECIMAL'].includes(field.fieldType)"
            v-model="formData[getFormFieldPropName(field)]" 
            :placeholder="`请输入${field.label}`"
          />
          <!-- 数字输入框（针对数字类型字段） -->
          <el-input-number 
            v-else-if="field.formComponent === 'input' && ['INT', 'NUMBER', 'DECIMAL'].includes(field.fieldType)"
            v-model="formData[getFormFieldPropName(field)]" 
            style="width: 100%"
          />
          <!-- 下拉选择 -->
          <el-select 
            v-else-if="field.formComponent === 'select'"
            v-model="formData[getFormFieldPropName(field)]" 
            :placeholder="`请选择${field.label}`"
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
            v-model="formData[getFormFieldPropName(field)]" 
            type="date"
            placeholder="请选择日期"
            style="width: 100%"
          />
          <!-- 数字输入框 -->
          <el-input-number 
            v-else-if="field.formComponent === 'number'"
            v-model="formData[getFormFieldPropName(field)]" 
            style="width: 100%"
          />
          <!-- 文本域 -->
          <el-input 
            v-else-if="field.formComponent === 'textarea'"
            v-model="formData[getFormFieldPropName(field)]" 
            type="textarea" 
            :rows="3"
          />
          <!-- 默认输入框 -->
          <el-input 
            v-else
            v-model="formData[getFormFieldPropName(field)]" 
            :placeholder="`请输入${field.label}`"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <template #footer>
      <el-button @click="localVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { getFormFieldPropName, getFormFieldOptions } from '../utils/mockDataGenerator';

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
const emit = defineEmits(['update:visible']);

// Data
// 表单引用
const formRef = ref(null);
// 表单数据
const formData = ref({});
// 本地可见性变量
const localVisible = ref(props.visible);

// Watch
// 监听外部可见性变化
watch(() => props.visible, (newVal) => {
  localVisible.value = newVal;
});

// 监听内部可见性变化
watch(() => localVisible.value, (newVal) => {
  emit('update:visible', newVal);
});

// Methods
// 处理提交
const handleSubmit = () => {
  if (formRef.value) {
    formRef.value.validate((valid) => {
      if (valid) {
        // 模拟保存
        console.log('表单数据:', formData.value);
      }
    });
  }
};

// 处理重置
const handleReset = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  formData.value = {};
};

// 计算属性：过滤后的字段列表（移除主键字段和无效字段）
const filteredFields = computed(() => {
  return props.fields.filter(field => {
    return field && 
           field.formComponent !== 'primary_key' && 
           field.fieldName !== 'id' && 
           field.fieldName !== 'uuid';
  });
});

// 内置正则表达式映射表，根据type值提供相应的正则表达式
const builtInRegexMap = {
  email: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
  url: '^(https?:\\/\\/)?(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3})(?:[/\\w .-]*)*\\/?$',
  number: '^-?\\d+(\\.\\d+)?$',
  integer: '^-?\\d+$'
};

// 表单规则
const formRules = computed(() => {
  const rules = {};
  props.fields.forEach(field => {
    const propName = getFormFieldPropName(field);
    
    // 初始化该字段的规则数组
    if (!rules[propName]) {
      rules[propName] = [];
    }
    
    // 必填验证规则
    if (field.isRequired === 1) {
      const isSelect = field.formComponent === 'select';
      const message = isSelect ? `请选择${field.label}` : `请输入${field.label}`;
      rules[propName].push({ required: true, message, trigger: 'blur' });
    }
    
    // 解析并添加自定义验证规则
    if (field.validateRule) {
      try {
        let customRules = JSON.parse(field.validateRule);
        // 如果是数组，直接处理；如果是对象，包装成数组
        const rulesToAdd = Array.isArray(customRules) ? customRules : [customRules];
        
        // 处理各种验证规则
        rulesToAdd.forEach((rule, index) => {
          if (rule.pattern) {
            console.log(`原始正则: ${rule.pattern}`);
            console.log(`正则类型: ${typeof rule.pattern}`);
            console.log(`正则字符编码: ${JSON.stringify(rule.pattern)}`);
            
            // 修复转义问题：JSON中的"^\d{10}$"解析后是"^\d{10}$"
            // 我们需要将其转换为"^\d{10}$"，这样new RegExp才能正确解析
            let pattern = new Function(`return '${rule.pattern}'`)();
            console.log(`修复后正则: ${pattern}`);
            console.log(`修复后字符编码: ${JSON.stringify(pattern)}`);
            
            // 测试正则匹配
            const testValue = '1010101010';
            const ipTestValue = '192.168.31.155';
            
            // 检测是否为URL相关的pattern，如果是，使用builtInRegexMap.url
            if (pattern.includes('https?') || pattern.includes('http?')) {
              console.log('检测到URL pattern，使用builtInRegexMap.url');
              pattern = builtInRegexMap.url;
            }
            
            const regex = new RegExp(pattern);
            console.log(`正则对象: ${regex}`);
            console.log(`测试值: ${testValue}，匹配结果: ${regex.test(testValue)}`);
            console.log(`IP测试值: ${ipTestValue}，匹配结果: ${regex.test(ipTestValue)}`);
            
            rule.pattern = pattern;
          } else if (rule.type) {
            console.log(`type约束: ${rule.type}`);
            
            // 检查是否为内置正则表达式类型
            if (builtInRegexMap[rule.type]) {
              console.log(`将type:${rule.type}转换为正则表达式验证`);
              // 使用内置正则表达式映射表中的对应正则表达式
              rulesToAdd[index] = {
                pattern: builtInRegexMap[rule.type],
                message: rule.message || `请输入正确的${rule.type}格式`,
                trigger: rule.trigger || 'blur'
              };
            }
          } else if (rule.operator === 'IN' && rule.values) {
            // 处理IN约束
            console.log(`IN约束: ${JSON.stringify(rule)}`);
            console.log(`可选项: ${rule.values}`);
            
            // 移除原有的IN规则，替换为自定义验证器
            rulesToAdd[index] = {
              validator: (rule, value, callback) => {
                // 检查值是否在values数组中
                const isMatch = rule.values.some(v => String(v) === String(value));
                if (isMatch) {
                  callback();
                } else {
                  callback(new Error(rule.message || '请选择有效值'));
                }
              },
              trigger: rule.trigger || 'blur',
              values: rule.values,
              message: rule.message
            };
          } else if (rule.min !== undefined || rule.max !== undefined) {
            // 处理min/max约束
            console.log(`处理min/max约束: ${JSON.stringify(rule)}`);
            console.log(`字段名: ${field.fieldName}, 字段类型: ${field.fieldType}, 表单组件: ${field.formComponent}`);
            
            // 构建min/max规则
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
            rulesToAdd[index] = minMaxRule;
          }
        });
        
        rules[propName] = [...rules[propName], ...rulesToAdd];
      } catch (e) {
        console.error('Failed to parse validateRule:', e);
      }
    }
  });
  return rules;
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
