<template>
  <div class="physical-database-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库管理</span>
          <el-button type="primary" @click="handleAdd">新增库</el-button>
        </div>
      </template>
      <el-alert
        title="在这里维护要在数据库里使用的库名，方便和业务配置对上号。打开「保存时建库」后，点保存会尝试在服务器上创建这个库（已有则不会重复创建）。删除时会清理元数据里对该库名的引用；若选择同时删服务器库，将执行 DROP DATABASE，库内数据不可恢复。"
        type="info"
        :closable="false"
        style="margin-bottom: 16px"
      />
      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="catalogName" label="库名" width="160" />
        <el-table-column prop="displayName" label="展示名称" width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column prop="charsetName" label="字符集" width="110" />
        <el-table-column prop="collationName" label="排序规则" width="160" show-overflow-tooltip />
        <el-table-column label="保存时建库" width="110">
          <template #default="{ row }">
            <el-tag :type="row.syncToInstance === 1 ? 'success' : 'info'">{{ row.syncToInstance === 1 ? '开' : '关' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">{{ row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="success" size="small" @click="handleSync(row)">去服务器建库</el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :before-close="formGuard.handleBeforeClose"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="库名" prop="catalogName">
          <el-input
            v-model="form.catalogName"
            :disabled="!!form.id"
            placeholder="请输入库名，与数据库里一致，字母数字下划线等"
          />
        </el-form-item>
        <el-form-item label="展示名称" prop="displayName">
          <el-input v-model="form.displayName" placeholder="方便自己辨认，可不填" clearable />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填，记一点备注" />
        </el-form-item>
        <el-form-item label="字符集" prop="charsetName">
          <el-select v-model="form.charsetName" placeholder="请选择" style="width: 100%" @change="onCharsetChange">
            <el-option v-for="c in charsetOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序规则" prop="collationName">
          <el-select v-model="form.collationName" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in collationOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="保存时建库">
          <el-switch v-model="form.syncToInstance" :active-value="1" :inactive-value="0" />
          <div class="el-form-item__help" style="color: #909399; margin-top: 8px;">
            打开后，保存时会顺便在服务器上建这个库（没有才建，有就跳过）
          </div>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formGuard.requestCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPhysicalDatabaseList,
  addPhysicalDatabase,
  updatePhysicalDatabase,
  deletePhysicalDatabase,
  syncPhysicalDatabase
} from '../api'
import { useDialogFormGuard } from '../composables/useUnsavedFormGuard'

const CHARSET_OPTIONS = [
  { value: 'utf8mb4', label: 'utf8mb4（推荐）' },
  { value: 'utf8', label: 'utf8' },
  { value: 'latin1', label: 'latin1' }
]

const CHARSET_TO_COLLATIONS = {
  utf8mb4: [
    { value: 'utf8mb4_unicode_ci', label: 'utf8mb4_unicode_ci（推荐）' },
    { value: 'utf8mb4_general_ci', label: 'utf8mb4_general_ci' },
    { value: 'utf8mb4_0900_ai_ci', label: 'utf8mb4_0900_ai_ci（MySQL 8+）' }
  ],
  utf8: [
    { value: 'utf8_unicode_ci', label: 'utf8_unicode_ci' },
    { value: 'utf8_general_ci', label: 'utf8_general_ci' }
  ],
  latin1: [
    { value: 'latin1_swedish_ci', label: 'latin1_swedish_ci' },
    { value: 'latin1_general_ci', label: 'latin1_general_ci' }
  ]
}

export default {
  name: 'PhysicalDatabaseManage',
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const tableData = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增库')
    const formRef = ref(null)
    const form = reactive({
      id: null,
      catalogName: '',
      displayName: '',
      description: '',
      charsetName: 'utf8mb4',
      collationName: 'utf8mb4_unicode_ci',
      syncToInstance: 1,
      isEnabled: 1
    })
    const rules = {
      catalogName: [{ required: true, message: '请填写库名', trigger: 'blur' }]
    }

    const formGuard = useDialogFormGuard(form, dialogVisible, {
      onReset: () => formRef.value?.resetFields()
    })

    const charsetOptions = CHARSET_OPTIONS

    const collationOptions = computed(() => {
      const list = CHARSET_TO_COLLATIONS[form.charsetName]
      return list && list.length ? list : CHARSET_TO_COLLATIONS.utf8mb4
    })

    const normalizeCharsetCollation = () => {
      if (!CHARSET_TO_COLLATIONS[form.charsetName]) {
        form.charsetName = 'utf8mb4'
      }
      const allowed = collationOptions.value.map((o) => o.value)
      if (!allowed.includes(form.collationName)) {
        form.collationName = allowed[0] || 'utf8mb4_unicode_ci'
      }
    }

    const onCharsetChange = () => {
      normalizeCharsetCollation()
    }

    const loadData = async () => {
      loading.value = true
      try {
        const res = await getPhysicalDatabaseList({ includeDisabled: true })
        if (res.code === 200) {
          tableData.value = res.data || []
        }
      } catch (e) {
        ElMessage.error('加载列表失败，请稍后重试')
      } finally {
        loading.value = false
      }
    }

    const resetForm = () => {
      Object.assign(form, {
        id: null,
        catalogName: '',
        displayName: '',
        description: '',
        charsetName: 'utf8mb4',
        collationName: 'utf8mb4_unicode_ci',
        syncToInstance: 1,
        isEnabled: 1
      })
    }

    const handleAdd = () => {
      dialogTitle.value = '新增库'
      resetForm()
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑库'
      Object.assign(form, {
        id: row.id,
        catalogName: row.catalogName,
        displayName: row.displayName || '',
        description: row.description || '',
        charsetName: row.charsetName || 'utf8mb4',
        collationName: row.collationName || 'utf8mb4_unicode_ci',
        syncToInstance: row.syncToInstance ?? 1,
        isEnabled: row.isEnabled ?? 1
      })
      normalizeCharsetCollation()
      dialogVisible.value = true
    }

    const handleDialogClose = () => {
      if (formRef.value && typeof formRef.value.resetFields === 'function') {
        formRef.value.resetFields()
      }
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
      } catch {
        return
      }
      normalizeCharsetCollation()
      submitting.value = true
      try {
        if (form.id) {
          await updatePhysicalDatabase({ ...form })
          ElMessage.success('已保存')
        } else {
          await addPhysicalDatabase({ ...form })
          ElMessage.success('已添加')
        }
        formGuard.markClean()
        dialogVisible.value = false
        loadData()
      } catch (e) {
        ElMessage.error(e.message || '保存失败，请检查填写内容')
      } finally {
        submitting.value = false
      }
    }

    const handleDelete = async (row) => {
      const name = row.catalogName
      try {
        await ElMessageBox.confirm(
          `确定要删除库「${name}」吗？会清理元数据里对该库名的引用。`,
          '提示',
          { type: 'warning' }
        )
      } catch {
        return
      }

      let dropOnInstance = false
      try {
        await ElMessageBox.confirm(
          `是否在服务器上同时删除库「${name}」？库内数据不可恢复。`,
          '提示',
          {
            type: 'warning',
            confirmButtonText: '同时删服务器库',
            cancelButtonText: '仅删配置',
            distinguishCancelAndClose: true
          }
        )
        try {
          await ElMessageBox.confirm(
            `再次确认：将删除服务器上的库「${name}」，此操作不可恢复。`,
            '提示',
            { type: 'warning', confirmButtonText: '确定删除' }
          )
          dropOnInstance = true
        } catch {
          return
        }
      } catch (action) {
        if (action !== 'cancel') {
          return
        }
      }

      try {
        await deletePhysicalDatabase(row.id, dropOnInstance)
        ElMessage.success(dropOnInstance ? '已删除，服务器库已一并删除' : '删除成功')
        loadData()
      } catch (e) {
        ElMessage.error(e.message || '删除失败')
      }
    }

    const handleSync = async (row) => {
      try {
        await syncPhysicalDatabase(row.id)
        ElMessage.success('已在服务器上处理完成（没有则创建）')
      } catch (e) {
        ElMessage.error(e.message || '建库失败，请检查账号权限或库名是否合法')
      }
    }

    onMounted(() => {
      loadData()
    })

    return {
      loading,
      submitting,
      tableData,
      dialogVisible,
      dialogTitle,
      form,
      formRef,
      rules,
      formGuard,
      charsetOptions,
      collationOptions,
      onCharsetChange,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleSync,
      handleDialogClose
    }
  }
}
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
