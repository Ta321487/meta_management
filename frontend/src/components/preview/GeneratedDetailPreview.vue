<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left row">
        <el-button link type="primary" class="back-btn" @click="$emit('back')">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
        <div>
          <h2 class="page-title">{{ detailModel.menuTitle || detailModel.tableName }}</h2>
          <span class="page-desc">详情 · 只读查看</span>
        </div>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="$emit('edit')">编辑</el-button>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never" class="detail-card">
      <el-descriptions :column="2" border>
        <el-descriptions-item
          v-for="field in detailFields"
          :key="field.camelCaseName"
          :label="fieldLabel(field)"
          :span="descSpan(field)"
        >
          {{ formatFieldValue(field, record[field.camelCaseName]) }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { createPreviewMockApi } from '../../utils/previewMockApi'

const props = defineProps({
  detailModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true },
  recordId: { type: [String, Number], required: true }
})

defineEmits(['back', 'edit'])

const loading = ref(false)
const record = ref({})

const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.detailModel.primaryKeyCamelCase)
)

const detailFields = computed(() =>
  (props.detailModel.fields || []).filter(f => fc(f) !== 'primary_key')
)

function fc(field) {
  return field.formComponent || field.field?.formComponent || 'input'
}

function fieldLabel(field) {
  return field.label || field.field?.label || field.camelCaseName
}

function descSpan(field) {
  return fc(field) === 'textarea' ? 2 : 1
}

function selectOptions(field) {
  const vr = field.validationRules || {}
  if (!vr.hasOptions || !vr.options) return []
  return (vr.options || []).map(o =>
    typeof o === 'string' ? { label: o, value: o } : { label: o.label ?? o.value, value: o.value ?? o.label }
  )
}

function formatFieldValue(field, value) {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'object') return JSON.stringify(value)
  if (field.isForeignKey) {
    return `关联记录 #${value}`
  }
  if (fc(field) === 'select') {
    const opts = selectOptions(field)
    const hit = opts.find(o => o.value === value || String(o.value) === String(value))
    if (hit) return hit.label
  }
  if (fc(field) === 'datepicker' || fc(field) === 'date') {
    if (value instanceof Date) return value.toISOString().slice(0, 10)
  }
  return value
}

async function loadRecord() {
  if (!props.recordId) return
  loading.value = true
  try {
    const res = await api.value.getById(props.recordId)
    if (res.code === 200 && res.data) record.value = { ...res.data }
    else record.value = {}
  } finally {
    loading.value = false
  }
}

watch(() => [props.recordId, props.mockApiBase], loadRecord, { immediate: true })
</script>

<style scoped>
.row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.back-btn {
  margin-right: 4px;
}
.detail-card {
  margin-top: 8px;
}
</style>
