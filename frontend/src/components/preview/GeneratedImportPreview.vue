<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ formModel.menuTitle || formModel.tableName }}</h2>
        <span class="page-desc">{{ batchImport ? '批量导入' : '数据导入' }} · 预览</span>
      </div>
    </div>

    <el-card shadow="never">
      <el-alert type="info" :closable="false" show-icon class="tip">
        下载 CSV 模板后填写并上传；将逐条调用 Mock 新增接口写入（与 ZIP 导入页逻辑一致）。
      </el-alert>
      <div class="actions">
        <el-button @click="downloadTemplate">下载模板</el-button>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".csv" :on-change="onFileChange">
          <el-button type="primary" :loading="importing">选择 CSV 并导入</el-button>
        </el-upload>
      </div>
      <el-table v-if="resultRows.length" :data="resultRows" border size="small" class="result-table">
        <el-table-column prop="line" label="行号" width="70" />
        <el-table-column prop="status" label="结果" width="90" />
        <el-table-column prop="message" label="说明" min-width="200" />
      </el-table>
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
  formModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true },
  batchImport: { type: Boolean, default: false }
})

const importing = ref(false)
const resultRows = ref([])

const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.formModel.primaryKeyCamelCase)
)

const importFields = computed(() => nonPkFields(props.formModel.fields))

const headers = computed(() =>
  importFields.value.map(f => ({
    key: resolveListFieldProp(f),
    label: fieldLabel(f)
  }))
)

function downloadTemplate() {
  const csv = headers.value.map(h => h.label).join(',') + '\n'
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `${props.formModel.entityName || 'data'}_import_template.csv`
  a.click()
  URL.revokeObjectURL(a.href)
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim())
  if (lines.length < 2) return []
  const rows = []
  for (let i = 1; i < lines.length; i++) {
    const cols = lines[i].split(',').map(s => s.trim().replace(/^"|"$/g, ''))
    const row = {}
    headers.value.forEach((h, idx) => {
      row[h.key] = cols[idx] ?? ''
    })
    rows.push(row)
  }
  return rows
}

async function onFileChange(file) {
  const raw = await file.raw.text()
  const rows = parseCsv(raw)
  if (!rows.length) {
    ElMessage.warning('未解析到数据行')
    return
  }
  importing.value = true
  resultRows.value = []
  let ok = 0
  try {
    for (let i = 0; i < rows.length; i++) {
      const row = rows[i]
      try {
        const res = await api.value.add(row)
        if (res.code === 200) {
          ok++
          resultRows.value.push({ line: i + 2, status: '成功', message: '' })
        } else {
          resultRows.value.push({ line: i + 2, status: '失败', message: res.message || '' })
        }
      } catch (e) {
        resultRows.value.push({ line: i + 2, status: '失败', message: e.message || '异常' })
      }
    }
    ElMessage.success(`导入完成：成功 ${ok} / ${rows.length}`)
  } finally {
    importing.value = false
  }
}

defineExpose({ reload: () => {} })
</script>

<style scoped>
.tip { margin-bottom: 16px; }
.actions { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.result-table { margin-top: 16px; }
</style>
