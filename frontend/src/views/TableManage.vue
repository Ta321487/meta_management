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
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
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
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        const res = await getTableList(params)
        if (res.code === 200) {
          if (res.data && res.data.records) {
            // 分页数据
            tableData.value = res.data.records
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            tableData.value = res.data || []
            pagination.total = Number(res.data?.length) || 0
          }
        }
      } catch (error) {
        ElMessage.error('加载数据失败')
        tableData.value = []
        pagination.total = 0
      }
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadData()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadData()
    }

    const handleSearch = () => {
      pagination.current = 1
      loadData()
    }

    const handleReset = () => {
      searchForm.tableName = ''
      pagination.current = 1
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
      pagination,
      searchForm,
      form,
      rules,
      handleSearch,
      handleReset,
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

