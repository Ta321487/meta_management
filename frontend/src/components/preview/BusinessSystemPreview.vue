<template>
  <el-drawer
    v-model="visible"
    :title="`业务系统预览 — ${spec?.businessName || ''}`"
    size="92%"
    direction="rtl"
    destroy-on-close
  >
    <div v-if="loading" class="center">加载预览规格…</div>
    <div v-else-if="spec" class="preview-root">
      <el-alert type="info" show-icon :closable="false" class="tip">
        <template #title>模拟下载 ZIP 后的前端 + Mock 后端</template>
        路由与校验规则与生成代码同源；侧栏菜单与 ZIP 一致。填充/覆盖填充前会重新加载最新字段与校验规则，并按 validationRules（IN、长度、select 等）生成示例数据。数据在服务端内存，重启后清空。
      </el-alert>
      <el-row :gutter="12" class="toolbar">
        <el-col :span="16">
          <el-radio-group v-model="dataMode" size="small">
            <el-radio-button label="mock">Mock API（平台内）</el-radio-button>
            <el-radio-button label="zip" disabled>真实后端（需先启动 ZIP 工程）</el-radio-button>
          </el-radio-group>
        </el-col>
        <el-col :span="8" style="text-align: right">
          <el-button size="small" :loading="seeding" @click="fillSampleData(false)">填充示例数据</el-button>
          <el-button size="small" :loading="seeding" @click="fillSampleData(true)">覆盖填充</el-button>
          <el-button size="small" @click="resetMock">清空</el-button>
        </el-col>
      </el-row>
      <el-container class="main-layout">
        <el-aside width="220px" class="aside">
          <el-menu :default-active="activePath" @select="onMenuSelect">
            <el-menu-item
              v-for="r in menuRoutes"
              :key="r.path"
              :index="r.path"
            >
              {{ r.meta?.title || r.name }}
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main>
          <GeneratedListPreview
            v-if="view === 'list' && currentTable"
            ref="listRef"
            :list-model="currentTable.list"
            :mock-api-base="currentTable.mockApiBase"
            @add="openForm()"
            @view="openDetail($event)"
            @edit="openForm($event)"
          />
          <GeneratedFormPreview
            v-if="view === 'form' && currentTable"
            :form-model="currentTable.form"
            :mock-api-base="currentTable.mockApiBase"
            :record-id="editId"
            @saved="onFormSaved"
            @cancel="onFormCancel"
          />
          <GeneratedDetailPreview
            v-if="view === 'detail' && currentTable"
            :detail-model="currentTable.form"
            :mock-api-base="currentTable.mockApiBase"
            :record-id="detailId"
            @back="onDetailBack"
            @edit="openFormFromDetail"
          />
          <GeneratedReportPreview
            v-if="view === 'report' && currentTable"
            ref="reportRef"
            :list-model="currentTable.list"
            :mock-api-base="currentTable.mockApiBase"
          />
          <GeneratedProcessPreview
            v-if="view === 'process' && currentTable"
            ref="processRef"
            :form-model="currentTable.form"
            :mock-api-base="currentTable.mockApiBase"
          />
          <GeneratedImportPreview
            v-if="view === 'import' && currentTable"
            :form-model="currentTable.form"
            :mock-api-base="currentTable.mockApiBase"
            :batch-import="activePath.includes('batch-import')"
          />
          <GeneratedExportPreview
            v-if="view === 'export' && currentTable"
            :list-model="currentTable.list"
            :mock-api-base="currentTable.mockApiBase"
          />
          <el-empty v-if="!currentTable && !loading" description="请从左侧选择菜单" />
        </el-main>
      </el-container>
      <el-collapse class="api-collapse">
        <el-collapse-item title="后端接口清单（ZIP 解压后路径）" name="api">
          <el-table :data="spec.apiCatalog" size="small" border>
            <el-table-column prop="tableName" label="表" width="140" />
            <el-table-column prop="method" label="方法" width="70" />
            <el-table-column prop="path" label="ZIP 内路径" />
            <el-table-column prop="mockPath" label="预览 Mock 路径" />
          </el-table>
        </el-collapse-item>
      </el-collapse>
    </div>
  </el-drawer>
</template>

<script setup>
import '../../styles/admin.css'
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import { seedBusinessMock } from '../../utils/previewMockSeed'
import { clearPreviewFkOptionsCache } from '../../utils/previewFkOptions'
import GeneratedListPreview from './GeneratedListPreview.vue'
import GeneratedFormPreview from './GeneratedFormPreview.vue'
import GeneratedDetailPreview from './GeneratedDetailPreview.vue'
import GeneratedReportPreview from './GeneratedReportPreview.vue'
import GeneratedProcessPreview from './GeneratedProcessPreview.vue'
import GeneratedImportPreview from './GeneratedImportPreview.vue'
import GeneratedExportPreview from './GeneratedExportPreview.vue'

const props = defineProps({
  modelValue: Boolean,
  businessCode: String,
  packageName: String,
  useInterface: Boolean,
  captchaEnabled: Boolean
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: v => emit('update:modelValue', v)
})

const loading = ref(false)
const spec = ref(null)
const activePath = ref('')
const view = ref('list')
const editId = ref(null)
const detailId = ref(null)
const formFromDetail = ref(false)
const dataMode = ref('mock')
const listRef = ref(null)
const reportRef = ref(null)
const processRef = ref(null)
const seeding = ref(false)
const ROWS_PER_TABLE = 8

const menuRoutes = computed(() =>
  (spec.value?.routes || []).filter(r => {
    if (r.meta?.isMenuVisible === 0 || r.meta?.isMenuVisible === false) return false
    const p = r.path || ''
    if (p.includes('/form') || p.includes('/detail')) return false
    return true
  })
)

const currentTable = computed(() => {
  if (!spec.value?.tables?.length) return null
  const route = (spec.value.routes || []).find(r => r.path === activePath.value)
  const code = route?.meta?.relatedTableCode
  if (code) return spec.value.tables.find(t => t.tableCode === code) || spec.value.tables[0]
  return spec.value.tables[0]
})

function resolveViewFromRoute(route) {
  const path = route?.path || activePath.value || ''
  const nodeType = (route?.meta?.nodeType || '').toUpperCase()
  if (path.includes('/form') || nodeType === 'FORM_PAGE') return 'form'
  if (path.includes('/detail') || nodeType === 'DETAIL_PAGE') return 'detail'
  if (path.includes('/report') || nodeType.includes('REPORT')) return 'report'
  if (path.includes('/process') || nodeType.includes('PROCESS')) return 'process'
  if (path.includes('/import') || path.includes('/batch-import') || nodeType.includes('IMPORT')) return 'import'
  if (path.includes('/export') || nodeType.includes('EXPORT')) return 'export'
  return 'list'
}

function onMenuSelect(path) {
  activePath.value = path
  const route = (spec.value?.routes || []).find(r => r.path === path)
  view.value = resolveViewFromRoute(route)
  editId.value = null
  detailId.value = null
}

function resolveRowPk(row) {
  const pk = currentTable.value?.form?.primaryKeyCamelCase || 'id'
  return row ? (row[pk] ?? row.id) : null
}

function openForm(row) {
  formFromDetail.value = false
  view.value = 'form'
  editId.value = resolveRowPk(row)
  detailId.value = null
}

function openDetail(row) {
  formFromDetail.value = false
  view.value = 'detail'
  detailId.value = resolveRowPk(row)
  editId.value = null
}

function openFormFromDetail() {
  formFromDetail.value = true
  editId.value = detailId.value
  view.value = 'form'
}

function onFormSaved() {
  formFromDetail.value = false
  editId.value = null
  detailId.value = null
  const route = (spec.value?.routes || []).find(r => r.path === activePath.value)
  view.value = resolveViewFromRoute(route)
  refreshActiveView()
}

function onFormCancel() {
  if (formFromDetail.value && editId.value != null) {
    detailId.value = editId.value
    editId.value = null
    formFromDetail.value = false
    view.value = 'detail'
    return
  }
  formFromDetail.value = false
  const route = (spec.value?.routes || []).find(r => r.path === activePath.value)
  view.value = resolveViewFromRoute(route)
  editId.value = null
  detailId.value = null
}

function onDetailBack() {
  const route = (spec.value?.routes || []).find(r => r.path === activePath.value)
  view.value = resolveViewFromRoute(route)
  detailId.value = null
  refreshActiveView()
}

async function loadSpec() {
  if (!props.businessCode) return
  loading.value = true
  try {
    const res = await request.get(`/codegen/preview/business/${encodeURIComponent(props.businessCode)}/spec`, {
      params: {
        packageName: props.packageName || undefined,
        useInterface: props.useInterface,
        captchaEnabled: props.captchaEnabled
      }
    })
    if (res.code === 200) {
      spec.value = res.data
      const first = menuRoutes.value[0]
      if (first) {
        activePath.value = first.path
        view.value = resolveViewFromRoute(first)
      }
      await autoSeedIfEmpty()
    }
  } catch (e) {
    ElMessage.error(e.message || '加载预览失败')
  } finally {
    loading.value = false
  }
}

async function autoSeedIfEmpty() {
  if (!spec.value?.tables?.length) return
  try {
    const { total } = await seedBusinessMock(spec.value, {
      rowsPerTable: ROWS_PER_TABLE,
      clearFirst: false,
      onlyEmpty: true
    })
    if (total > 0) {
      ElMessage.success(`已自动填充示例数据（共 ${total} 条）`)
      await nextTick()
      refreshActiveView()
    }
  } catch (e) {
    console.warn('auto seed failed', e)
  }
}

async function refreshSpec() {
  if (!props.businessCode) return false
  try {
    const res = await request.get(`/codegen/preview/business/${encodeURIComponent(props.businessCode)}/spec`, {
      params: {
        packageName: props.packageName || undefined,
        useInterface: props.useInterface,
        captchaEnabled: props.captchaEnabled
      }
    })
    if (res.code === 200 && res.data) {
      spec.value = res.data
      return true
    }
  } catch (e) {
    console.warn('refresh spec failed', e)
  }
  return false
}

async function fillSampleData(clearFirst) {
  if (!spec.value) return
  await refreshSpec()
  if (clearFirst) {
    try {
      await ElMessageBox.confirm('将清空各表现有 Mock 数据并重新生成示例，是否继续？', '覆盖填充', {
        type: 'warning'
      })
    } catch {
      return
    }
  }
  seeding.value = true
  try {
    clearPreviewFkOptionsCache()
    const { total, details } = await seedBusinessMock(spec.value, {
      rowsPerTable: ROWS_PER_TABLE,
      clearFirst,
      onlyEmpty: !clearFirst
    })
    const skipped = details.filter(d => d.skipped).length
    ElMessage.success(
      clearFirst
        ? `已覆盖填充 ${total} 条示例数据`
        : `已填充 ${total} 条${skipped ? `（${skipped} 张表已有数据已跳过）` : ''}`
    )
    await nextTick()
    refreshActiveView()
  } catch (e) {
    ElMessage.error(e.message || '填充失败')
  } finally {
    seeding.value = false
  }
}

async function resetMock() {
  await request.post(`/codegen/preview/business/${encodeURIComponent(props.businessCode)}/mock/reset`)
  clearPreviewFkOptionsCache()
  ElMessage.success('已清空 Mock 数据')
  refreshActiveView()
}

function refreshActiveView() {
  if (view.value === 'list') listRef.value?.load?.()
  else if (view.value === 'report') reportRef.value?.load?.()
  else if (view.value === 'process') processRef.value?.load?.()
}

watch(
  () => [props.modelValue, props.businessCode],
  ([open]) => {
    if (open) loadSpec()
  }
)
</script>

<style scoped>
.preview-root {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 80px);
}
.tip {
  margin-bottom: 12px;
}
.toolbar {
  margin-bottom: 12px;
}
.main-layout {
  flex: 1;
  min-height: 0;
  border: 1px solid #ebeef5;
}
.aside {
  border-right: 1px solid #ebeef5;
}
.center {
  text-align: center;
  padding: 48px;
}
.api-collapse {
  margin-top: 12px;
}
</style>
