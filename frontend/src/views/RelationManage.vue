<template>
  <div class="relation-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表关联关系管理</span>
          <el-button type="primary" @click="handleAdd">新增关联</el-button>
        </div>
      </template>

      <el-table :data="relationData" border style="width: 100%">
        <el-table-column prop="relationCode" label="关联编码" width="150" />
        <el-table-column prop="relationName" label="关联名称" />
        <el-table-column prop="mainTableCode" label="主表" width="150" />
        <el-table-column prop="slaveTableCode" label="从表" width="150" />
        <el-table-column prop="mainFieldCode" label="主表字段" width="150" />
        <el-table-column prop="slaveFieldCode" label="从表字段" width="150" />
        <el-table-column prop="relationType" label="关联类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.relationType === 'ONE_TO_ONE' ? '一对一' : '一对多' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="关联编码" prop="relationCode" v-if="!form.id">
          <el-input v-model="form.relationCode" placeholder="如：RELATION_001" />
        </el-form-item>
        <el-form-item label="关联名称" prop="relationName">
          <el-input v-model="form.relationName" placeholder="请输入关联名称" />
        </el-form-item>
        <el-form-item label="主表" prop="mainTableCode">
          <el-select v-model="form.mainTableCode" placeholder="请选择主表" style="width: 100%" @change="handleMainTableChange">
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主表关联字段" prop="mainFieldCode">
          <el-select v-model="form.mainFieldCode" placeholder="请选择字段" style="width: 100%">
            <el-option
              v-for="field in mainTableFields"
              :key="field.fieldCode"
              :label="field.fieldName"
              :value="field.fieldCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="从表" prop="slaveTableCode">
          <el-select v-model="form.slaveTableCode" placeholder="请选择从表" style="width: 100%" @change="handleSlaveTableChange">
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="从表外键字段" prop="slaveFieldCode">
          <el-select v-model="form.slaveFieldCode" placeholder="请选择字段" style="width: 100%">
            <el-option
              v-for="field in slaveTableFields"
              :key="field.fieldCode"
              :label="field.fieldName"
              :value="field.fieldCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联类型" prop="relationType">
          <el-select v-model="form.relationType" placeholder="请选择" style="width: 100%">
            <el-option label="一对一" value="ONE_TO_ONE" />
            <el-option label="一对多" value="ONE_TO_MANY" />
          </el-select>
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
import { getTableList, getFieldList, addRelation, updateRelation, deleteRelation, getAllRelations } from '../api'

export default {
  name: 'RelationManage',
  setup() {
    const tables = ref([])
    const relationData = ref([])
    const mainTableFields = ref([])
    const slaveTableFields = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增关联')
    const formRef = ref(null)
    const form = reactive({
      id: null,
      relationCode: '',
      relationName: '',
      mainTableCode: '',
      slaveTableCode: '',
      mainFieldCode: '',
      slaveFieldCode: '',
      relationType: 'ONE_TO_MANY'
    })
    const rules = {
      relationCode: [{ required: true, message: '请输入关联编码', trigger: 'blur' }],
      relationName: [{ required: true, message: '请输入关联名称', trigger: 'blur' }],
      mainTableCode: [{ required: true, message: '请选择主表', trigger: 'change' }],
      slaveTableCode: [{ required: true, message: '请选择从表', trigger: 'change' }],
      mainFieldCode: [{ required: true, message: '请选择主表字段', trigger: 'change' }],
      slaveFieldCode: [{ required: true, message: '请选择从表字段', trigger: 'change' }],
      relationType: [{ required: true, message: '请选择关联类型', trigger: 'change' }]
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

    const loadRelations = async () => {
      try {
        const res = await getAllRelations()
        if (res.code === 200) {
          relationData.value = res.data || []
        }
      } catch (error) {
        ElMessage.error('加载关联关系失败')
        relationData.value = []
      }
    }

    const handleMainTableChange = async (tableCode) => {
      if (tableCode) {
        try {
          const res = await getFieldList(tableCode)
          if (res.code === 200) {
            mainTableFields.value = res.data
          }
        } catch (error) {
          ElMessage.error('加载字段失败')
        }
      } else {
        mainTableFields.value = []
      }
    }

    const handleSlaveTableChange = async (tableCode) => {
      if (tableCode) {
        try {
          const res = await getFieldList(tableCode)
          if (res.code === 200) {
            slaveTableFields.value = res.data
          }
        } catch (error) {
          ElMessage.error('加载字段失败')
        }
      } else {
        slaveTableFields.value = []
      }
    }

    const handleAdd = () => {
      dialogTitle.value = '新增关联'
      Object.assign(form, {
        id: null,
        relationCode: '',
        relationName: '',
        mainTableCode: '',
        slaveTableCode: '',
        mainFieldCode: '',
        slaveFieldCode: '',
        relationType: 'ONE_TO_MANY'
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑关联'
      Object.assign(form, {
        id: row.id,
        relationCode: row.relationCode,
        relationName: row.relationName,
        mainTableCode: row.mainTableCode,
        slaveTableCode: row.slaveTableCode,
        mainFieldCode: row.mainFieldCode,
        slaveFieldCode: row.slaveFieldCode,
        relationType: row.relationType
      })
      handleMainTableChange(row.mainTableCode)
      handleSlaveTableChange(row.slaveTableCode)
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateRelation(form)
            } else {
              await addRelation(form)
            }
            ElMessage.success('操作成功')
            dialogVisible.value = false
            loadRelations()
          } catch (error) {
            ElMessage.error('操作失败')
          }
        }
      })
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该关联关系吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteRelation({ id: row.id })
          ElMessage.success('删除成功')
          loadRelations()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
      mainTableFields.value = []
      slaveTableFields.value = []
    }

    onMounted(() => {
      loadTables()
      loadRelations()
    })

    return {
      tables,
      relationData,
      mainTableFields,
      slaveTableFields,
      dialogVisible,
      dialogTitle,
      formRef,
      form,
      rules,
      handleMainTableChange,
      handleSlaveTableChange,
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
.relation-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

