<template>
  <div class="admin-page ${componentName?lower_case}-process">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">${menuTitle!table.tableName}</h2>
        <span class="page-desc">流程审批</span>
      </div>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="tableData" border stripe>
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
        <el-table-column prop="${field.camelCaseName}" label="${field.field.label}" min-width="120" show-overflow-tooltip />
        </#if>
</#list>
        <#if hasStatusField>
        <el-table-column prop="${statusFieldCamelCase}" label="流程状�? width="110" />
        </#if>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link @click="approve(row)">通过</el-button>
            <el-button type="danger" link @click="reject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ${componentName}Api } from '../api'

export default {
  name: '${componentName}Process',
  setup() {
    const loading = ref(false)
    const tableData = ref([])
    const pagination = reactive({ current: 1, size: 10, total: 0 })
    const pk = '${primaryKeyCamelCase}'
    <#if hasStatusField>
    const statusKey = '${statusFieldCamelCase}'
    const passVal = '已通过'
    const rejectVal = '已驳�?
    <#else>
    const localStatus = reactive({})
    </#if>

    const loadData = async () => {
      loading.value = true
      try {
        const res = await ${componentName}Api.page({ page: pagination.current, size: pagination.size })
        if (res.code === 200 && res.data) {
          tableData.value = res.data.records || []
          pagination.total = res.data.total || 0
        }
      } catch (e) {
        ElMessage.error('加载失败')
      } finally {
        loading.value = false
      }
    }

    const updateFlow = async (row, nextStatus) => {
      const payload = { ...row }
      <#if hasStatusField>
      payload[statusKey] = nextStatus
      <#else>
      localStatus[row[pk]] = nextStatus
      </#if>
      await ${componentName}Api.update(payload)
      ElMessage.success('�? + nextStatus)
      loadData()
    }

    const approve = (row) => {
      ElMessageBox.confirm('确认通过该记录？', '审批').then(() => updateFlow(row, <#if hasStatusField>passVal<#else>'已通过'</#if>)).catch(() => {})
    }
    const reject = (row) => {
      ElMessageBox.confirm('确认驳回该记录？', '审批').then(() => updateFlow(row, <#if hasStatusField>rejectVal<#else>'已驳�?</#if>)).catch(() => {})
    }

    onMounted(loadData)
    return { loading, tableData, pagination, loadData, approve, reject }
  }
}
</script>

<style scoped>
.table-footer { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>


