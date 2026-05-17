<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ listModel.menuTitle || listModel.tableName }}</h2>
        <span class="page-desc">统计报表 · 预览</span>
      </div>
      <div class="page-header-actions">
        <el-button @click="loadData">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="never"><el-statistic title="记录总数" :value="stats.total" /></el-card>
      </el-col>
      <el-col v-if="groupField" :span="16">
        <el-card shadow="never">
          <div class="card-title">{{ fieldLabel(groupField) }}分布</div>
          <el-table :data="stats.groups" size="small" border>
            <el-table-column prop="label" :label="fieldLabel(groupField)" />
            <el-table-column prop="count" label="数量" width="100" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <template #header>最近数�?/template>
      <el-table v-loading="loading" :data="recentRows" border stripe>
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          min-width="120"
          show-overflow-tooltip
        />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { createPreviewMockApi } from '../../utils/previewMockApi'
import { resolveListFieldProp } from '../../utils/mockDataGenerator'
import { fieldLabel, findFirstSelectField, nonPkFields } from '../../utils/previewFieldUtils'

const props = defineProps({
  listModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true }
})

const loading = ref(false)
const recentRows = ref([])
const stats = reactive({ total: 0, groups: [] })

const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.listModel.primaryKeyCamelCase)
)

const groupField = computed(() => findFirstSelectField(props.listModel.fields))

const columns = computed(() =>
  nonPkFields(props.listModel.fields)
    .map(f => ({
      prop: resolveListFieldProp(f),
      label: fieldLabel(f)
    }))
    .filter(c => c.prop)
)

async function loadData() {
  loading.value = true
  try {
    const pageRes = await api.value.page({ page: 1, size: 1 })
    if (pageRes.code === 200 && pageRes.data) stats.total = pageRes.data.total || 0

    const listRes = await api.value.page({ page: 1, size: 10 })
    if (listRes.code === 200 && listRes.data) recentRows.value = listRes.data.records || []

    if (groupField.value) {
      const key = resolveListFieldProp(groupField.value)
      const allRes = await api.value.list()
      const rows = Array.isArray(allRes.data) ? allRes.data : allRes.data?.records || []
      const map = {}
      rows.forEach(r => {
        const k = r[key] == null || r[key] === '' ? '未填�? : String(r[key])
        map[k] = (map[k] || 0) + 1
      })
      stats.groups = Object.keys(map).map(k => ({ label: k, count: map[k] }))
    } else {
      stats.groups = []
    }
  } catch (e) {
    ElMessage.error('加载报表失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.mockApiBase, loadData, { immediate: true })
defineExpose({ load: loadData })
</script>

<style scoped>
.stat-row { margin-bottom: 16px; }
.card-title { font-weight: 600; margin-bottom: 12px; }
.table-card { margin-top: 8px; }
</style>


