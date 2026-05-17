<template>
  <div class="admin-page ${componentName?lower_case}-report">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">${menuTitle!table.tableName}</h2>
        <span class="page-desc">统计报表</span>
      </div>
      <div class="page-header-actions">
        <el-button @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="never"><el-statistic title="记录总数" :value="stats.total" /></el-card>
      </el-col>
      <#if groupFieldCamelCase?? && groupFieldCamelCase?has_content>
      <el-col :span="16">
        <el-card shadow="never">
          <div class="card-title">${groupFieldLabel!""}分布</div>
          <el-table :data="stats.groups" size="small" border>
            <el-table-column prop="label" label="${groupFieldLabel!""}" />
            <el-table-column prop="count" label="数量" width="100" />
          </el-table>
        </el-card>
      </el-col>
      </#if>
    </el-row>

    <el-card shadow="never" class="table-card">
      <template #header>最近数�?/template>
      <el-table v-loading="loading" :data="recentRows" border stripe>
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
        <el-table-column prop="${field.camelCaseName}" label="${field.field.label}" min-width="120" show-overflow-tooltip />
        </#if>
</#list>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ${componentName}Api } from '../api'

export default {
  name: '${componentName}Report',
  setup() {
    const loading = ref(false)
    const recentRows = ref([])
    const stats = reactive({ total: 0, groups: [] })

    const loadData = async () => {
      loading.value = true
      try {
        const pageRes = await ${componentName}Api.page({ page: 1, size: 1 })
        if (pageRes.code === 200 && pageRes.data) {
          stats.total = pageRes.data.total || 0
        }
        const listRes = await ${componentName}Api.page({ page: 1, size: 10 })
        if (listRes.code === 200 && listRes.data) {
          recentRows.value = listRes.data.records || []
        }
        <#if groupFieldCamelCase?? && groupFieldCamelCase?has_content>
        const allRes = await ${componentName}Api.list()
        const rows = Array.isArray(allRes.data) ? allRes.data : (allRes.data?.records || [])
        const map = {}
        rows.forEach(r => {
          const k = r.${groupFieldCamelCase} == null || r.${groupFieldCamelCase} === '' ? '未填�? : String(r.${groupFieldCamelCase})
          map[k] = (map[k] || 0) + 1
        })
        stats.groups = Object.keys(map).map(k => ({ label: k, count: map[k] }))
        </#if>
      } catch (e) {
        ElMessage.error('加载报表数据失败')
      } finally {
        loading.value = false
      }
    }

    onMounted(loadData)
    return { loading, recentRows, stats, loadData }
  }
}
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.card-title { font-weight: 600; margin-bottom: 12px; }
.table-card { margin-top: 8px; }
</style>


