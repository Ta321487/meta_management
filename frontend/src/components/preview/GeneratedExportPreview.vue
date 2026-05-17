<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ listModel.menuTitle || listModel.tableName }}</h2>
        <span class="page-desc">批量导出 · 预览</span>
      </div>
    </div>

    <el-card shadow="never">
      <el-alert type="info" :closable="false" show-icon class="tip">
        从 Mock 拉取当前表数据并导出为 CSV（与 ZIP 导出页一致）。
      </el-alert>
      <el-button type="primary" :loading="exporting" @click="exportAll">导出 CSV</el-button>
      <p v-if="lastCount >= 0" class="hint">上次导出 {{ lastCount }} 条</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { createPreviewMockApi } from '../../utils/previewMockApi'
import { resolveListFieldProp } from '../../utils/mockDataGenerator'
import { fieldLabel, nonPkFields } from '../../utils/previewFieldUtils'

const props = defineProps({
  listModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true }
})

const exporting = ref(false)
const lastCount = ref(-1)

const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.listModel.primaryKeyCamelCase)
)

const exportHeaders = computed(() =>
  nonPkFields(props.listModel.fields).map(f => ({
    key: resolveListFieldProp(f),
    label: fieldLabel(f)
  }))
)

async function exportAll() {
  exporting.value = true
  try {
    const res = await api.value.page({ page: 1, size: 10000 })
    const rows = res.code === 200 && res.data?.records ? res.data.records : []
    const lines = [exportHeaders.value.map(h => h.label).join(',')]
    rows.forEach(r => {
      lines.push(
        exportHeaders.value
          .map(h => {
            const v = r[h.key]
            if (v == null) return ''
            const s = String(v).replace(/"/g, '""')
            return s.includes(',') ? `"${s}"` : s
          })
          .join(',')
      )
    })
    const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = `${props.listModel.entityName || 'export'}_${Date.now()}.csv`
    a.click()
    URL.revokeObjectURL(a.href)
    lastCount.value = rows.length
    ElMessage.success(`已导出 ${rows.length} 条`)
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

defineExpose({ load: () => {} })
</script>

<style scoped>
.tip { margin-bottom: 16px; }
.hint { margin-top: 12px; color: #909399; font-size: 13px; }
</style>
