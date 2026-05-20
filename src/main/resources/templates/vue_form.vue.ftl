<#-- 辅助函数：转义正则表达式字符串 -->
<#function escapeRegexPattern pattern>
  <#return pattern?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>
<#function escapeJsString str>
  <#return str?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>

<template>
  <div class="admin-page ${componentName?lower_case}-form">
    <div class="page-header">
      <div class="page-header-left row">
        <el-button link type="primary" class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div>
          <h2 class="page-title">${menuTitle!table.tableName}</h2>
          <span class="page-desc">{{ isEdit ? '编辑' : '新增' }}记录</span>
        </div>
      </div>
    </div>

    <el-card shadow="never" class="form-card">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px" label-position="right">
        <el-row :gutter="24">
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
        <el-col :xs="24" :sm="24" :md="${(field.field.formComponent == 'textarea')?then(24, 12)}" :lg="${(field.field.formComponent == 'textarea')?then(24, 12)}">
        <el-form-item label="${field.field.label}" prop="${field.camelCaseName}">
          <#if field.isForeignKey!false>
          <el-select v-model="form.${field.camelCaseName}" placeholder="请选择${field.field.label}" style="width: 100%" filterable>
            <el-option
              v-for="item in ${field.relatedTableCamelCaseName}List"
              :key="item.id"
              :label="item.${field.relatedTableFieldName}"
              :value="item.${field.relatedTableFieldName}"
            />
          </el-select>
          <#elseif field.field.formComponent == "input">
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          <#elseif field.useSwitchDisplay!false>
          <el-switch
            v-model="form.${field.camelCaseName}"
            :active-value="${field.switchActiveValue}"
            :inactive-value="${field.switchInactiveValue}"
          />
          <#elseif field.field.formComponent == "select">
          <el-select v-model="form.${field.camelCaseName}" placeholder="请选择" style="width: 100%">
            <#if (field.validationRules?? && field.validationRules.hasOptions!false)>
              <#if field.validationRules.options?is_sequence>
                <#list field.validationRules.options as option>
                  <#if option?is_string>
            <el-option label="${option}" value="${option}" />
                  <#else>
            <el-option label="${option.label!option.value}" value="${option.value!option}" />
                  </#if>
                </#list>
              </#if>
            </#if>
          </el-select>
          <#elseif field.field.formComponent == "datepicker" || field.field.formComponent == "date">
          <el-date-picker v-model="form.${field.camelCaseName}" type="date" placeholder="请选择日期" style="width: 100%" />
          <#elseif field.field.formComponent == "number">
          <el-input-number
            v-model="form.${field.camelCaseName}"
            style="width: 100%"
            <#if field.numberInteger!false>
            :precision="0"
            :step="1"
            <#elseif field.numberPrecision??>
            :precision="${field.numberPrecision}"
            </#if>
          />
          <#elseif field.field.formComponent == "textarea">
          <el-input v-model="form.${field.camelCaseName}" type="textarea" :rows="4" />
          <#else>
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          </#if>
        </el-form-item>
        </el-col>
        </#if>
</#list>
        </el-row>
        <div class="form-actions">
          <el-button type="primary" @click="handleSubmit">保存</el-button>
          <el-button @click="goBack">取消</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ${componentName}Api <#list fields as field><#if field.isForeignKey!false>, ${field.relatedTableClassName}Api</#if></#list> } from '../api'

export default {
  name: '${componentName}Form',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const formRef = ref(null)
    const isEdit = computed(() => !!form.${primaryKeyCamelCase})
    const goBack = () => router.back()
    const form = reactive({
<#list fields as field>
      ${field.camelCaseName}: <#if field.field.fieldType?lower_case?contains("int")>null<#elseif field.field.fieldType?lower_case?contains("date")>null<#else>''</#if>,
</#list>
    })
    
    // 关联数据
    <#list fields as field>
    <#if field.isForeignKey!false>
    const ${field.relatedTableCamelCaseName}List = ref([])
    </#if>
    </#list>
    
    const rules = {
<#list fields as field>
      <#if field.field.formComponent != "primary_key" && (field.field.isRequired == 1 || (field.validationRules?? && (field.validationRules.hasPattern!false || field.validationRules.hasLength!false || field.validationRules.hasRange!false)) || field.field.fieldType?lower_case?contains("int") || field.field.fieldType?lower_case?contains("decimal") || field.field.fieldType?lower_case?contains("double") || field.field.fieldType?lower_case?contains("float"))>
      ${field.camelCaseName}: [
        <#if field.field.isRequired == 1>
        { required: true, message: '请输入${field.field.label}', trigger: 'blur' },
        </#if>
        <#if field.numericFormField!false>
        { validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') { callback(); return }
            const n = Number(value)
            if (Number.isNaN(n) || !Number.isFinite(n)) {
              callback(new Error('请输入有效的数字'))
            } else {
              callback()
            }
          }, trigger: 'blur' },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasPattern!false)>
        <#assign patternKind = field.validationRules.patternKind!"none">
        <#if patternKind == "integer">
        { validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') { callback(); return }
            const n = Number(value)
            if (Number.isNaN(n) || !Number.isFinite(n) || !Number.isInteger(n)) {
              callback(new Error('${escapeJsString(field.validationRules.patternMessage!"必须输入整数")}'))
            } else {
              callback()
            }
          }, trigger: 'blur' },
        <#elseif patternKind == "custom" || (patternKind != "builtinNumber" && patternKind != "integer")>
        { validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') { callback(); return }
            const re = new RegExp('${escapeJsString(field.validationRules.pattern!)}')
            if (re.test(String(value))) {
              callback()
            } else {
              callback(new Error('${escapeJsString(field.validationRules.patternMessage!"格式不正确")}'))
            }
          }, trigger: 'blur' },
        </#if>
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasLength!false) && !(field.numericFormField!false)>
        { 
          <#if field.validationRules?exists && field.validationRules.minLength?exists>min: ${field.validationRules.minLength!0}, </#if>
          <#if field.validationRules?exists && field.validationRules.maxLength?exists>max: ${field.validationRules.maxLength!9999}, </#if>
          message: '${escapeJsString(field.validationRules.lengthMessage!"长度必须在${minLength}到${maxLength}之间")}', 
          trigger: 'blur' 
        },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasRange!false)>
        <#-- hasRange：元数据 min/max 表示数值上下界；字符串长度请用 minLength/maxLength（hasLength） -->
        <#assign minValue = field.validationRules.min!-99999999>
        <#assign maxValue = field.validationRules.max!99999999>
        <#assign rangeMsg = field.validationRules.rangeMessage!field.validationRules.patternMessage!"数值超出范围">
        <#assign rangeMsg = rangeMsg?replace("${'$'}{min}", minValue?string)>
        <#assign rangeMsg = rangeMsg?replace("${'$'}{max}", maxValue?string)>
        { validator: (rule, value, callback) => {
            if (value === null || value === undefined || value === '') { callback(); return }
            const n = Number(value)
            if (Number.isNaN(n) || !Number.isFinite(n)) {
              callback(new Error('请输入有效的数字'))
              return
            }
            <#if field.validationRules?exists && field.validationRules.min?exists>
            if (n < ${minValue}) {
              callback(new Error('${escapeJsString(rangeMsg)}'))
              return
            }
            </#if>
            <#if field.validationRules?exists && field.validationRules.max?exists>
            if (n > ${maxValue}) {
              callback(new Error('${escapeJsString(rangeMsg)}'))
              return
            }
            </#if>
            callback()
          }, trigger: 'blur' },
        </#if>
        <#-- 检查是否有跨字段比较规则 -->
        <#if (field.validationRules?? && field.validationRules.hasCrossField!false)>
        <#assign crossField2 = field.validationRules.crossField2!''>
        <#if crossField2 != ''>
        <#assign compareFieldCamelCase = ''>
        <#list fields as f>
          <#if f.field.fieldName == crossField2>
            <#assign compareFieldCamelCase = f.camelCaseName>
            <#break>
          </#if>
        </#list>
        <#if compareFieldCamelCase != ''>
        <#assign crossFieldOperator = field.validationRules.crossFieldOperator!'>='>
        <#assign crossFieldCondition = field.validationRules.crossFieldCondition!''>
        <#assign crossFieldMessage = field.validationRules.crossFieldMessage!''>
        <#-- 确定错误消息：优先使用crossFieldMessage，如果为空或不存在，则使用crossFieldCondition或默认消息 -->
        <#if crossFieldMessage?has_content && crossFieldMessage?trim?has_content>
          <#assign finalMessage = crossFieldMessage>
        <#elseif crossFieldCondition?has_content && crossFieldCondition?trim?has_content>
          <#assign finalMessage = crossFieldCondition>
        <#else>
          <#assign finalMessage = '验证失败: ${field.field.fieldName} ${crossFieldOperator} ${crossField2}'>
        </#if>
        { 
          validator: (rule, value, callback) => {
            const compareValue = form.${compareFieldCamelCase};
            // 如果两个值都为空，跳过验证
            if (!value && !compareValue) {
              callback();
              return;
            }
            // 比较两个值
            let isValid = false;
            <#if crossFieldOperator == '>='>
            isValid = value >= compareValue;
            <#elseif crossFieldOperator == '<='>
            isValid = value <= compareValue;
            <#elseif crossFieldOperator == '>'>
            isValid = value > compareValue;
            <#elseif crossFieldOperator == '<'>
            isValid = value < compareValue;
            <#elseif crossFieldOperator == '=' || crossFieldOperator == '=='>
            isValid = value === compareValue;
            <#elseif crossFieldOperator == '!=' || crossFieldOperator == '<>'>
            isValid = value !== compareValue;
            </#if>
            if (isValid) {
              callback();
            } else {
              callback(new Error('${escapeJsString(finalMessage)}'));
            }
          }, 
          trigger: 'blur',
          message: '${escapeJsString(finalMessage)}'
        },
        </#if>
        </#if>
        </#if>
        <#-- 检查是否有针对该字段的唯一性规则 -->
        <#list businessRules as rule>
        <#if rule.ruleType == "unique">
        <#list rule.fields as ruleField>
        <#if ruleField.fieldName == field.field.fieldName>
        { 
          validator: async (rule, value, callback) => {
            if (!value) {
              callback();
              return;
            }
            try {
              const params = { ${ruleField.fieldName}: value };
              // 更新时排除自身
              if (form.${primaryKeyCamelCase}) {
                params.${primaryKeyCamelCase} = form.${primaryKeyCamelCase};
              }
              const res = await ${componentName}Api.checkUnique(params);
              if (res.code === 200 && !res.data) {
                callback();
              } else {
                callback(new Error('${rule.message}'));
              }
            } catch (error) {
              callback(new Error('验证失败'));
            }
          }, 
          trigger: 'blur' 
        },
        </#if>
        </#list>
        </#if>
        </#list>
      ]<#sep>,</#sep>
      </#if>
</#list>
    }

    const loadData = async () => {
      const id = route.params.id
      if (id) {
        try {
          const res = await ${componentName}Api.getById(id)
          if (res.code === 200 && res.data) {
            Object.assign(form, res.data)
<#list fields as field>
<#if field.numericFormField!false>
            if (form.${field.camelCaseName} != null && form.${field.camelCaseName} !== '') {
              const n_${field.camelCaseName} = Number(form.${field.camelCaseName})
              if (!Number.isNaN(n_${field.camelCaseName})) {
                <#if field.numberInteger!false>
                form.${field.camelCaseName} = Math.trunc(n_${field.camelCaseName})
                <#else>
                form.${field.camelCaseName} = n_${field.camelCaseName}
                </#if>
              }
            }
</#if>
</#list>
          }
        } catch (error) {
          ElMessage.error('加载数据失败')
        }
      }
    }
    
    // 加载关联数据
    <#list fields as field>
    <#if field.isForeignKey!false>
    const load${field.relatedTableClassName}Data = async () => {
      try {
        const res = await ${field.relatedTableClassName}Api.list()
        if (res.code === 200 && res.data) {
          ${field.relatedTableCamelCaseName}List.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载${field.relatedTableName}数据失败')
      }
    }
    </#if>
    </#list>

    // 验证组合字段唯一性
    const validateUniqueCombo = async () => {
      <#if businessRules?has_content>
      <#list businessRules as rule>
      <#if rule.ruleType == "unique_combo">
      try {
        const params = {
          <#list rule.fields as fieldName>
          <#list fields as field>
          <#if field.field.fieldName == fieldName.fieldName>
          ${fieldName.fieldName}: form.${field.camelCaseName},
          </#if>
          </#list>
          </#list>
        }
        // 更新时排除自身
        if (form.${primaryKeyCamelCase}) {
          params.${primaryKeyCamelCase} = form.${primaryKeyCamelCase};
        }
        const res = await ${componentName}Api.checkUniqueCombo(params)
        if (res.code === 200 && res.data) {
          ElMessage.error('${rule.message}')
          return false
        }
      } catch (error) {
        ElMessage.error('验证失败')
        return false
      }
      </#if>
      </#list>
      </#if>
      return true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          // 验证组合字段唯一性
          const isUniqueCombo = await validateUniqueCombo()
          if (!isUniqueCombo) {
            return
          }
          try {
            if (form.${primaryKeyCamelCase}) {
              await ${componentName}Api.update(form)
            } else {
              await ${componentName}Api.add(form)
            }
            ElMessage.success('保存成功')
          } catch (error) {
            ElMessage.error('保存失败')
          }
        }
      })
    }

    const handleReset = () => {
      formRef.value?.resetFields()
    }

    onMounted(async () => {
      await loadData()
      // 加载关联数据
      <#list fields as field>
      <#if field.isForeignKey!false>
      await load${field.relatedTableClassName}Data()
      </#if>
      </#list>
    })

    return {
      formRef,
      form,
      rules,
      isEdit,
      goBack,
      handleSubmit,
      handleReset
      <#list fields as field>
      <#if field.isForeignKey!false>
      , ${field.relatedTableCamelCaseName}List
      </#if>
      </#list>
    }
  }
}
</script>

<style scoped>
.page-header-left { display: flex; align-items: center; gap: 12px; }
.back-btn { padding-left: 0; }
</style>