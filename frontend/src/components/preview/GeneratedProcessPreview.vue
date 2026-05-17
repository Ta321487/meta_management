<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ formModel.menuTitle || formModel.tableName }}</h2>
        <span class="page-desc">ćľç¨ĺŽĄćš Âˇ é˘č§</span>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="rows" border stripe>
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label"
          min-width="120"
          show-overflow-tooltip
        />
        <el-table-column v-if="statusField" :prop="statusProp" label="ćľç¨çść" width="110" />
        <el-table-column label="ćä˝" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link size="small" @click="approve(row)">éčż</el-button>
            <el-button type="danger" link size="small" @click="reject(row)">éŠłĺ</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPreviewMockApi } from '../../utils/previewMockApi'
import { resolveListFieldProp } from '../../utils/mockDataGenerator'
import { fieldLabel, findStatusField, nonPkFields } from '../../utils/previewFieldUtils'

const props = defineProps({
  formModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true }
})

const loading = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.formModel.primaryKeyCamelCase)
)

const statusField = computed(() => findStatusField(props.formModel.fields))
const statusProp = computed(() => (statusField.value ? resolveListFieldProp(statusField.value) : ''))

const columns = computed(() =>
  nonPkFields(props.formModel.fields)
    .filter(f => f !== statusField.value)
    .map(f => ({ prop: resolveListFieldProp(f), label: fieldLabel(f) }))
    .filter(c => c.prop)
)

async function load() {
  loading.value = true
  try {
    const res = await api.value.page({ page: page.value, size: size.value })
    if (res.code === 200 && res.data) {
      rows.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

async function updateFlow(row, nextStatus) {
  const payload = { ...row }
  if (statusProp.value) payload[statusProp.value] = nextStatus
  await api.value.update(payload)
  ElMessage.success('ĺˇ˛' + nextStatus)
  load()
}

function approve(row) {
  ElMessageBox.confirm('çĄŽčŽ¤éčżďź', 'ĺŽĄćš', { type: 'info' })
    .then(() => updateFlow(row, 'ĺˇ˛éčż'))
    .catch(() => {})
}

function reject(row) {
  ElMessageBox.confirm('çĄŽčŽ¤éŠłĺďź', 'ĺŽĄćš', { type: 'warning' })
    .then(() => updateFlow(row, 'ĺˇ˛éŠłĺ'))
    .catch(() => {})
}

watch(() => props.mockApiBase, load, { immediate: true })
defineExpose({ load })
</script>

<style scoped>
.table-card { margin-top: 8px; }
.table-footer { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
