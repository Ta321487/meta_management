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
        <#if field.field.fieldName != "id">
        <el-form-item label="${field.field.label}" prop="${field.camelCaseName}">
          <#if field.field.formComponent == "input">
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
              <#else>
            <el-option label="选项1" value="1" />
              </#if>
            <#else>
            <el-option label="选项1" value="1" />
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
import { ${componentName}Api } from '../api'

export default {
  name: '${componentName}Form',
  setup() {
    const route = useRoute()
    const formRef = ref(null)
    const form = reactive({
<#list fields as field>
      ${field.camelCaseName}: <#if field.field.fieldType?contains("int")>null<#elseif field.field.fieldType?contains("date")>null<#else>''</#if>,
</#list>
    })
    
    const rules = {
<#list fields as field>
      <#if field.field.fieldName != "id" && (field.field.isRequired == 1 || (field.validationRules?? && field.validationRules.hasPattern!false))>
      ${field.camelCaseName}: [
        <#if field.field.isRequired == 1>
        { required: true, message: '请输入${field.field.label}', trigger: 'blur' }<#if (field.validationRules?? && field.validationRules.hasPattern!false)>,</#if>
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasPattern!false)>
        { 
          pattern: new RegExp('${escapeRegexPattern(field.validationRules.pattern!)}'), 
          message: '${escapeJsString(field.validationRules.patternMessage!"格式不正确")}', 
          trigger: 'blur' 
        }
        </#if>
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

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
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

    onMounted(() => {
      loadData()
    })

    return {
      formRef,
      form,
      rules,
      handleSubmit,
      handleReset
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

