<template>
  <div class="field-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字段管理</span>
          <div>
            <el-select v-model="selectedTableCode" placeholder="请选择表" style="width: 200px; margin-right: 10px" @change="loadFields">
              <el-option
                v-for="table in tables"
                :key="table.tableCode"
                :label="table.tableName"
                :value="table.tableCode"
              />
            </el-select>
            <el-button type="primary" @click="handleAdd" :disabled="!selectedTableCode">新增字段</el-button>
          </div>
        </div>
      </template>

      <el-table :data="fieldData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="fieldCode" label="字段编码" width="150" />
        <el-table-column prop="fieldName" label="字段名称" />
        <el-table-column prop="fieldType" label="字段类型" width="150" />
        <el-table-column prop="label" label="显示名" width="120" />
        <el-table-column prop="isRequired" label="必填" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRequired === 1 ? 'success' : 'info'">
              {{ row.isRequired === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="formComponent" label="表单组件" width="120" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total || 0"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      close-on-click-modal="false"
      close-on-press-escape="false"
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="字段编码" prop="fieldCode" v-if="!form.id">
          <el-input v-model="form.fieldCode" placeholder="如：FIELD_001" />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="请输入字段名称" />
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-input v-model="form.fieldType" placeholder="如：varchar(100)" />
        </el-form-item>
        <el-form-item label="显示名" prop="label">
          <el-input v-model="form.label" placeholder="请输入显示名" />
        </el-form-item>
        <el-form-item label="是否必填" prop="isRequired">
          <el-radio-group v-model="form.isRequired">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="表单组件" prop="formComponent">
          <el-select v-model="form.formComponent" placeholder="请选择" style="width: 100%">
            <el-option label="输入框" value="input" />
            <el-option label="下拉框" value="select" />
            <el-option label="日期选择器" value="datepicker" />
            <el-option label="数字输入框" value="number" />
            <el-option label="文本域" value="textarea" />
          </el-select>
        </el-form-item>
        <el-form-item label="校验规则" prop="validateRule">
          <el-input v-model="form.validateRule" type="textarea" :rows="2" placeholder="JSON格式" />
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTableList, getFieldList, addField, updateField, deleteField } from '../api'

export default {
  name: 'FieldManage',
  setup() {
    const tables = ref([])
    const fieldData = ref([])
    const selectedTableCode = ref('')
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增字段')
    const formRef = ref(null)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const form = reactive({
      id: null,
      fieldCode: '',
      tableCode: '',
      fieldName: '',
      fieldType: '',
      label: '',
      isRequired: 0,
      formComponent: 'input',
      validateRule: '',
      sort: 0
    })
    const rules = {
      fieldCode: [{ required: true, message: '请输入字段编码', trigger: 'blur' }],
      fieldName: [{ required: true, message: '请输入字段名称', trigger: 'blur' }],
      fieldType: [{ required: true, message: '请输入字段类型', trigger: 'blur' }],
      label: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
      formComponent: [{ required: true, message: '请选择表单组件', trigger: 'change' }]
    }

    const loadTables = async () => {
      try {
        const res = await getTableList({})
        if (res.code === 200) {
          tables.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载表列表失败')
      }
    }

    const loadFields = async () => {
      if (!selectedTableCode.value) {
        fieldData.value = []
        pagination.total = 0
        return
      }
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size
        }
        const res = await getFieldList(selectedTableCode.value, params)
        if (res.code === 200) {
          if (res.data && res.data.records) {
            // 分页数据
            fieldData.value = res.data.records
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            fieldData.value = res.data || []
            pagination.total = Number(res.data?.length) || 0
          }
        }
      } catch (error) {
        ElMessage.error('加载字段列表失败')
        fieldData.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadFields()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadFields()
    }

    const handleAdd = () => {
      dialogTitle.value = '新增字段'
      Object.assign(form, {
        id: null,
        fieldCode: '',
        tableCode: selectedTableCode.value,
        fieldName: '',
        fieldType: '',
        label: '',
        isRequired: 0,
        formComponent: 'input',
        validateRule: '',
        sort: 0
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑字段'
      Object.assign(form, {
        id: row.id,
        fieldCode: row.fieldCode,
        tableCode: row.tableCode,
        fieldName: row.fieldName,
        fieldType: row.fieldType,
        label: row.label,
        isRequired: row.isRequired,
        formComponent: row.formComponent,
        validateRule: row.validateRule || '',
        sort: row.sort
      })
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateField(form)
            } else {
              await addField(form)
            }
            ElMessage.success('操作成功')
            dialogVisible.value = false
            loadFields()
          } catch (error) {
            ElMessage.error('操作失败')
          }
        }
      })
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该字段吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteField({ id: row.id })
          ElMessage.success('删除成功')
          loadFields()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadTables()
    })

    return {
      tables,
      fieldData,
      selectedTableCode,
      loading,
      dialogVisible,
      dialogTitle,
      formRef,
      pagination,
      form,
      rules,
      loadFields,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleDialogClose
    }
  }
}
</script>

<style scoped>
.field-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

