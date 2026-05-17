<template>
  <div class="admin-page ${componentName?lower_case}-detail">
    <div class="page-header">
      <div class="page-header-left row">
        <el-button link type="primary" class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon> 返回
        </el-button>
        <div>
          <h2 class="page-title">${menuTitle!table.tableName}</h2>
          <span class="page-desc">详情 · 只读查看</span>
        </div>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="handleEdit">编辑</el-button>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never" class="detail-card">
      <el-descriptions :column="2" border>
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
        <el-descriptions-item label="${field.field.label}" :span="${(field.field.formComponent == 'textarea')?then(2, 1)}">
          <#if field.isForeignKey!false>
          {{ ${field.camelCaseName}Display }}
          <#elseif field.field.formComponent == "select">
          {{ ${field.camelCaseName}Display }}
          <#else>
          {{ formatValue(record.${field.camelCaseName}) }}
          </#if>
        </el-descriptions-item>
        </#if>
</#list>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ${componentName}Api <#list fields as field><#if field.isForeignKey!false>, ${field.relatedTableClassName}Api</#if></#list> } from '../api'

export default {
  name: '${componentName}Detail',
  setup() {
    const route = useRoute()
    const router = useRouter()
    const loading = ref(false)
    const record = reactive({})
    const goBack = () => router.back()

    <#list fields as field>
    <#if field.isForeignKey!false>
    const ${field.relatedTableCamelCaseName}List = ref([])
    </#if>
    </#list>

    const formatValue = (val) => {
      if (val === null || val === undefined || val === '') return '—'
      if (typeof val === 'object') return JSON.stringify(val)
      return val
    }

    <#list fields as field>
    <#if field.isForeignKey!false>
    const ${field.camelCaseName}Display = computed(() => {
      const val = record.${field.camelCaseName}
      if (val === null || val === undefined || val === '') return '—'
      const list = ${field.relatedTableCamelCaseName}List.value
      const item = list.find(i => i.id === val || String(i.id) === String(val) || i.${field.relatedTableFieldName} === val || String(i.${field.relatedTableFieldName}) === String(val))
      return item != null ? String(item.${field.relatedTableFieldName}) : String(val)
    })
    <#elseif field.field.formComponent == "select">
    const ${field.camelCaseName}Display = computed(() => {
      const val = record.${field.camelCaseName}
      if (val === null || val === undefined || val === '') return '—'
      const options = [
        <#if (field.validationRules?? && field.validationRules.hasOptions!false) && field.validationRules.options?is_sequence>
        <#list field.validationRules.options as option>
        <#if option?is_string>
        { label: '${option?js_string}', value: '${option?js_string}' },
        <#else>
        { label: '${(option.label!option.value)?js_string}', value: '${(option.value!option.label)?js_string}' },
        </#if>
        </#list>
        </#if>
      ]
      const hit = options.find(o => o.value === val || String(o.value) === String(val))
      return hit ? hit.label : String(val)
    })
    </#if>
    </#list>

    const loadData = async () => {
      const id = route.params.id
      if (!id) {
        ElMessage.warning('缺少记录 ID')
        return
      }
      loading.value = true
      try {
        const res = await ${componentName}Api.getById(id)
        if (res.code === 200 && res.data) {
          Object.assign(record, res.data)
        } else {
          ElMessage.error(res.message || '加载数据失败')
        }
      } catch (error) {
        ElMessage.error('加载数据失败')
      } finally {
        loading.value = false
      }
    }

    const handleEdit = () => {
      const id = route.params.id
      if (!id) return
      router.push('${formRoutePrefix}' + id)
    }

    <#list fields as field>
    <#if field.isForeignKey!false>
    const load${field.relatedTableClassName}Data = async () => {
      try {
        const res = await ${field.relatedTableClassName}Api.list()
        const raw = res.data
        ${field.relatedTableCamelCaseName}List.value = Array.isArray(raw) ? raw : (raw && raw.records ? raw.records : [])
      } catch (e) {
        ${field.relatedTableCamelCaseName}List.value = []
      }
    }
    </#if>
    </#list>

    onMounted(async () => {
      <#list fields as field>
      <#if field.isForeignKey!false>
      await load${field.relatedTableClassName}Data()
      </#if>
      </#list>
      await loadData()
    })

    return {
      loading,
      record,
      goBack,
      handleEdit,
      formatValue
      <#list fields as field>
      <#if field.isForeignKey!false || (field.field.formComponent == "select")>
      , ${field.camelCaseName}Display
      </#if>
      </#list>
    }
  }
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}
.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn {
  padding-left: 0;
}
.detail-card {
  margin-top: 8px;
}
</style>
