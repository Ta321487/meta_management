<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">{{ listModel.menuTitle || listModel.tableName }}</h2>
        <span class="page-desc">数据列表 · 预览</span>
      </div>
      <div class="page-header-actions">
        <el-button type="primary" @click="$emit('add')">
          <el-icon style="margin-right: 4px"><Plus /></el-icon>新增
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="rows"
        border
        stripe
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column
          v-for="col in columns"
          :key="col.prop"
          :prop="col.prop"
          :label="col.label || col.prop"
          :min-width="col.switch ? 100 : 120"
          :align="col.switch ? 'center' : 'left'"
          :show-overflow-tooltip="!col.switch && !col.tag"
        >
          <template #default="{ row }">
            <el-switch
              v-if="col.switch"
              :model-value="row[col.prop]"
              :active-value="col.switch.activeValue"
              :inactive-value="col.switch.inactiveValue"
              disabled
            />
            <el-tag
              v-else-if="col.tag"
              :type="tagTypeForOptionValue(col.field, row[col.prop])"
              size="small"
              effect="light"
            >
              {{ labelForOptionValue(col.field, row[col.prop]) }}
            </el-tag>
            <span v-else>{{ formatCell(row[col.prop]) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="'操作'" :width="listModel.hasDetailPage ? 260 : 200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="listModel.hasDetailPage"
              type="primary"
              link
              size="small"
              @click="$emit('view', row)"
            >
              查看
            </el-button>
            <el-button type="primary" link size="small" @click="$emit('edit', row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="load"
          @size-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPreviewMockApi } from '../../utils/previewMockApi'
import { resolveListFieldProp } from '../../utils/mockDataGenerator'
import {
  labelForOptionValue,
  resolveSwitchMeta,
  tagTypeForOptionValue,
  useSwitchDisplay,
  useTagDisplay
} from '../../utils/previewFieldUtils'

const props = defineProps({
  listModel: { type: Object, required: true },
  mockApiBase: { type: String, required: true }
})

defineEmits(['add', 'view', 'edit'])

const loading = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const api = computed(() =>
  createPreviewMockApi(props.mockApiBase, props.listModel.primaryKeyCamelCase)
)

const columns = computed(() =>
  (props.listModel.fields || [])
    .filter(f => f?.formComponent !== 'primary_key')
    .map(f => {
      const prop = resolveListFieldProp(f)
      const sw = useSwitchDisplay(f) ? resolveSwitchMeta(f) : null
      return {
        prop,
        label: f.label || f.field?.label,
        field: f,
        switch: sw,
        tag: !sw && useTagDisplay(f)
      }
    })
    .filter(c => c.prop)
)

function formatCell(val) {
  if (val === null || val === undefined || val === '') return '—'
  if (typeof val === 'object') return JSON.stringify(val)
  return val
}

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

async function remove(row) {
  const pk = props.listModel.primaryKeyCamelCase
  await ElMessageBox.confirm('确定删除该记录吗？', '提示', { type: 'warning' })
  await api.value.delete({ id: row[pk] ?? row.id })
  ElMessage.success('已删除')
  load()
}

watch(() => props.mockApiBase, load, { immediate: true })

defineExpose({ load })
</script>
