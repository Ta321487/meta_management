<template>
  <div class="table-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表管理</span>
          <el-button type="primary" @click="handleAdd">新增表</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="表名称">
          <el-input v-model="searchForm.tableName" placeholder="请输入表名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="tableCode" label="表编码" width="150" />
        <el-table-column prop="tableName" label="表名称" />
        <el-table-column prop="pkStrategy" label="主键策略" width="120" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
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
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="表编码" prop="tableCode" v-if="!form.id">
          <el-input v-model="form.tableCode" placeholder="如：TABLE_001" />
        </el-form-item>
        <el-form-item label="表名称" prop="tableName">
          <el-input v-model="form.tableName" placeholder="请输入表名称" />
        </el-form-item>
        <el-form-item label="主键策略" prop="pkStrategy">
          <el-select v-model="form.pkStrategy" placeholder="请选择" style="width: 100%">
            <el-option label="自增(AUTO)" value="AUTO" />
            <el-option label="UUID" value="UUID" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
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
import { getTableList, addTable, updateTable, deleteTable } from '../api'

export default {
  name: 'TableManage',
  setup() {
    const tableData = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增表')
    const formRef = ref(null)
    const searchForm = reactive({
      tableName: ''
    })
    const form = reactive({
      id: null,
      tableCode: '',
      tableName: '',
      pkStrategy: 'AUTO',
      description: ''
    })
    const rules = {
      tableCode: [{ required: true, message: '请输入表编码', trigger: 'blur' }],
      tableName: [{ required: true, message: '请输入表名称', trigger: 'blur' }],
      pkStrategy: [{ required: true, message: '请选择主键策略', trigger: 'change' }]
    }

    const loadData = async () => {
      try {
        const res = await getTableList(searchForm)
        if (res.code === 200) {
          tableData.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载数据失败')
      }
    }

    const handleSearch = () => {
      loadData()
    }

    const handleReset = () => {
      searchForm.tableName = ''
      loadData()
    }

    const handleAdd = () => {
      dialogTitle.value = '新增表'
      Object.assign(form, {
        id: null,
        tableCode: '',
        tableName: '',
        pkStrategy: 'AUTO',
        description: ''
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑表'
      Object.assign(form, {
        id: row.id,
        tableCode: row.tableCode,
        tableName: row.tableName,
        pkStrategy: row.pkStrategy,
        description: row.description
      })
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateTable(form)
            } else {
              await addTable(form)
            }
            ElMessage.success('操作成功')
            dialogVisible.value = false
            loadData()
          } catch (error) {
            ElMessage.error('操作失败')
          }
        }
      })
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该表吗？删除后关联的字段也会被删除', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteTable({ id: row.id })
          ElMessage.success('删除成功')
          loadData()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadData()
    })

    return {
      tableData,
      dialogVisible,
      dialogTitle,
      formRef,
      searchForm,
      form,
      rules,
      handleSearch,
      handleReset,
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
.table-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>

