<#-- 辅助函数：转义正则表达式字符串 -->
<#function escapeRegexPattern pattern>
  <#return pattern?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>
<#function escapeJsString str>
  <#return str?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>

<template>
  <div class="${componentName?lower_case}-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>${table.tableName}表单</span>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
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
          <el-input-number v-model="form.${field.camelCaseName}" style="width: 100%" />
          <#elseif field.field.formComponent == "textarea">
          <el-input v-model="form.${field.camelCaseName}" type="textarea" :rows="3" />
          <#else>
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          </#if>
        </el-form-item>
        </#if>
</#list>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit">保存</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { ${componentName}Api <#list fields as field><#if field.isForeignKey!false>, ${field.relatedTableClassName}Api</#if></#list> } from '../api'

export default {
  name: '${componentName}Form',
  setup() {
    const route = useRoute()
    const formRef = ref(null)
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
        <#if field.field.fieldType?lower_case?contains("int") || field.field.fieldType?lower_case?contains("decimal") || field.field.fieldType?lower_case?contains("double") || field.field.fieldType?lower_case?contains("float")>
        { type: 'number', message: '请输入有效的数字', trigger: 'blur' },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasPattern!false)>
        { 
          pattern: /${field.validationRules.pattern!}/, 
          message: '${escapeJsString(field.validationRules.patternMessage!"格式不正确")}', 
          trigger: 'blur' 
        },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasLength!false)>
        { 
          <#if field.validationRules?exists && field.validationRules.minLength?exists>min: ${field.validationRules.minLength!0}, </#if>
          <#if field.validationRules?exists && field.validationRules.maxLength?exists>max: ${field.validationRules.maxLength!9999}, </#if>
          message: '${escapeJsString(field.validationRules.lengthMessage!"长度必须在${minLength}到${maxLength}之间")}', 
          trigger: 'blur' 
        },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasRange!false)>
        <!-- hasRange: ${field.validationRules.hasRange?c}, min: ${field.validationRules.min!''}, max: ${field.validationRules.max!''} -->
        { 
          type: 'number',
          <#assign minValue = field.validationRules.min!-99999999>
          <#assign maxValue = field.validationRules.max!99999999>
          <#if field.validationRules?exists && field.validationRules.min?exists>min: ${minValue}, </#if>
          <#if field.validationRules?exists && field.validationRules.max?exists>max: ${maxValue}, </#if>
          <#assign message = field.validationRules.rangeMessage!"数值必须在${minValue}到${maxValue}之间">
          <#assign message = message?replace("${'$'}{min}", minValue?string)>
          <#assign message = message?replace("${'$'}{max}", maxValue?string)>
          message: '${escapeJsString(message)}', 
          trigger: 'blur' 
        }
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
.${componentName?lower_case}-form {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>