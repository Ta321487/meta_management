<template>
  <div class="database-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>数据库管理</span>
          <el-button type="primary" @click="handleAdd">新增数据库</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="数据库名称">
          <el-input v-model="searchForm.databaseName" placeholder="请输入数据库名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="databaseName" label="数据库名称" />
        <el-table-column prop="tableCount" label="表数量" width="120" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleViewTables(row)">查看表</el-button>
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

    <!-- 新增数据库对话框 -->
    <el-dialog
      close-on-click-modal="false"
      close-on-press-escape="false"
      v-model="dialogVisible"
      title="新增数据库"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="数据库名称" prop="databaseName">
          <el-input v-model="form.databaseName" placeholder="请输入数据库名称，如：test_db" />
        </el-form-item>
        <el-alert
          title="提示：数据库名称只能包含字母、数字、下划线和连字符，且不能以数字开头"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        />
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 查看表对话框 -->
    <el-dialog
      close-on-click-modal="false"
      close-on-press-escape="false"
      v-model="tablesDialogVisible"
      :title="`数据库 ${currentDatabase} 中的表`"
      width="800px"
    >
      <el-table :data="tablesData" border style="width: 100%">
        <el-table-column prop="tableName" label="表名" />
        <el-table-column prop="tableType" label="类型" width="120" />
        <el-table-column prop="tableRows" label="行数" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
      <template #footer>
        <el-button @click="tablesDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDatabaseList, createDatabase, deleteDatabase, getTableListByDatabase } from '../api'

export default {
  name: 'DatabaseManage',
  setup() {
    const tableData = ref([])
    const dialogVisible = ref(false)
    const tablesDialogVisible = ref(false)
    const formRef = ref(null)
    const submitting = ref(false)
    const currentDatabase = ref('')
    const tablesData = ref([])
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const searchForm = reactive({
      databaseName: ''
    })
    const form = reactive({
      databaseName: ''
    })

    const rules = {
      databaseName: [
        { required: true, message: '请输入数据库名称', trigger: 'blur' },
        { pattern: /^[a-zA-Z_][a-zA-Z0-9_-]*$/, message: '数据库名称只能包含字母、数字、下划线和连字符，且不能以数字开头', trigger: 'blur' }
      ]
    }

    // 加载数据库列表
    const loadData = async () => {
      try {
        const response = await getDatabaseList()
        if (response.code === 200) {
          let databases = response.data.data || []
          
          // 如果提供了搜索条件，进行过滤
          if (searchForm.databaseName) {
            databases = databases.filter(db => 
              db.toLowerCase().includes(searchForm.databaseName.toLowerCase())
            )
          }
          
          // 转换为表格数据格式，并获取每个数据库的表数量
          const databaseList = await Promise.all(
            databases.map(async (dbName) => {
              let tableCount = 0
              try {
                const tablesResponse = await getTableListByDatabase(dbName)
                if (tablesResponse.code === 200 && tablesResponse.data && tablesResponse.data.success) {
                  // 后端返回格式：{ success: true, data: [...], count: ... }
                  tableCount = tablesResponse.data.count || (tablesResponse.data.data?.length || 0)
                }
              } catch (e) {
                // 忽略获取表数量失败的情况
              }
              return {
                databaseName: dbName,
                tableCount: tableCount
              }
            })
          )
          
          // 分页处理
          const start = (pagination.current - 1) * pagination.size
          const end = start + pagination.size
          tableData.value = databaseList.slice(start, end)
          pagination.total = databaseList.length
        } else {
          ElMessage.error('加载失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('加载失败: ' + (error.message || '未知错误'))
      }
    }

    // 搜索
    const handleSearch = () => {
      pagination.current = 1
      loadData()
    }

    // 重置
    const handleReset = () => {
      searchForm.databaseName = ''
      pagination.current = 1
      loadData()
    }

    // 新增
    const handleAdd = () => {
      dialogVisible.value = true
      form.databaseName = ''
    }

    // 提交
    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          submitting.value = true
          try {
            const response = await createDatabase({ databaseName: form.databaseName })
            if (response.code === 200) {
              ElMessage.success('创建数据库成功')
              dialogVisible.value = false
              loadData()
            } else {
              ElMessage.error('创建失败: ' + response.message)
            }
          } catch (error) {
            ElMessage.error('创建失败: ' + (error.message || '未知错误'))
          } finally {
            submitting.value = false
          }
        }
      })
    }

    // 删除
    const handleDelete = (row) => {
      ElMessageBox.confirm(
        `确定要删除数据库 "${row.databaseName}" 吗？此操作不可恢复！`,
        '警告',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(async () => {
        try {
          const response = await deleteDatabase({ databaseName: row.databaseName })
          if (response.code === 200) {
            ElMessage.success('删除成功')
            loadData()
          } else {
            ElMessage.error('删除失败: ' + response.message)
          }
        } catch (error) {
          ElMessage.error('删除失败: ' + (error.message || '未知错误'))
        }
      }).catch(() => {})
    }

    // 查看表
    const handleViewTables = async (row) => {
      currentDatabase.value = row.databaseName
      tablesDialogVisible.value = true
      try {
        const response = await getTableListByDatabase(row.databaseName)
        if (response.code === 200 && response.data && response.data.success) {
          // 后端返回格式：{ success: true, data: [{ tableName, tableType, tableRows, createTime }], count: ... }
          const tables = response.data.data || []
          tablesData.value = tables.map(table => ({
            tableName: table.tableName || '-',
            tableType: table.tableType || 'BASE TABLE',
            tableRows: table.tableRows || '0',
            createTime: table.createTime || '-'
          }))
        } else {
          ElMessage.error('加载表列表失败: ' + (response.data?.message || response.message || '未知错误'))
        }
      } catch (error) {
        ElMessage.error('加载表列表失败: ' + (error.message || '未知错误'))
      }
    }

    // 对话框关闭
    const handleDialogClose = () => {
      form.databaseName = ''
      formRef.value?.clearValidate()
    }

    // 分页
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      loadData()
    }

    const handleCurrentChange = (current) => {
      pagination.current = current
      loadData()
    }

    onMounted(() => {
      loadData()
    })

    return {
      tableData,
      dialogVisible,
      tablesDialogVisible,
      formRef,
      submitting,
      currentDatabase,
      tablesData,
      pagination,
      searchForm,
      form,
      rules,
      handleSearch,
      handleReset,
      handleAdd,
      handleSubmit,
      handleDelete,
      handleViewTables,
      handleDialogClose,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.database-manage {
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

