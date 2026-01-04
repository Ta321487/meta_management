<template>
  <div class="table-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表管理</span>
          <div>
            <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedRows || selectedRows.length === 0">批量删除</el-button>
            <el-button 
              :type="selectedRows.every(row => row.isEnabled === 1) ? 'warning' : 'success'" 
              @click="handleBatchToggleEnable(0)" 
              :disabled="!selectedRows || selectedRows.length === 0 || selectedRows.every(row => row.isEnabled === 0)"
            >
              批量禁用
            </el-button>
            <el-button 
              type="success" 
              @click="handleBatchToggleEnable(1)" 
              :disabled="!selectedRows || selectedRows.length === 0 || selectedRows.every(row => row.isEnabled === 1)"
            >
              批量启用
            </el-button>
            <el-button type="primary" @click="handleAdd">新增表</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="表名称">
          <el-input v-model="searchForm.tableName" placeholder="请输入表名称" clearable @input="handleSearch" />
        </el-form-item>
        <el-form-item label="业务系统">
          <el-select v-model="searchForm.businessCode" placeholder="请选择" clearable style="width: 200px" @change="handleSearch">
            <el-option
              v-for="system in businessSystems"
              :key="system.businessCode"
              :label="system.businessName"
              :value="system.businessCode"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%" ref="tableRef" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="tableCode" label="表编码" width="150" />
        <el-table-column prop="tableName" label="表名称" />
        <el-table-column prop="businessCode" label="业务系统" width="150">
          <template #default="{ row }">
            <el-tag>{{ row.businessCode || '未关联' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pkStrategy" label="主键策略" width="120" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button 
                :type="row.isEnabled === 1 ? 'warning' : 'success'" 
                size="small" 
                @click="handleToggleEnable(row)"
              >
                {{ row.isEnabled === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </el-space>
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
        <el-form-item label="业务系统">
          <el-select v-model="form.businessCode" placeholder="请选择业务系统" style="width: 100%">
            <el-option
              v-for="system in businessSystems"
              :key="system.businessCode"
              :label="system.businessName"
              :value="system.businessCode"
            />
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
import { getTableList, addTable, updateTable, deleteTable, batchDeleteTable, batchUpdateTableStatus, getBusinessSystemList } from '../api'

export default {
  name: 'TableManage',
  setup() {
    const tableData = ref([])
    const businessSystems = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增表')
    const formRef = ref(null)
    const tableRef = ref(null)
    const selectedRows = ref([])
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const searchForm = reactive({
      tableName: '',
      businessCode: ''
    })
    const form = reactive({
      id: null,
      tableCode: '',
      tableName: '',
      pkStrategy: 'AUTO',
      description: '',
      businessCode: '',
      isEnabled: 1
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

    // 加载业务系统列表
    const loadBusinessSystems = async () => {
      try {
        const res = await getBusinessSystemList({})
        if (res.code === 200) {
          businessSystems.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载业务系统列表失败')
      }
    }

    const handleReset = () => {
      searchForm.tableName = ''
      searchForm.businessCode = ''
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
        description: '',
        businessCode: ''
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
        description: row.description,
        businessCode: row.businessCode || ''
      })
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      try {
        // 先验证表单
        await formRef.value.validate()
        
        // 检查是否修改了主键生成策略
        if (form.id) {
          const originalTable = tableData.value.find(t => t.id === form.id)
          if (originalTable && originalTable.pkStrategy !== form.pkStrategy) {
            // 直接提示用户主键策略不可修改，而不是让用户确认后再被后端拒绝
            ElMessage.warning('主键生成策略不允许修改，请删除表后重新创建')
            // 恢复原始主键策略值
            form.pkStrategy = originalTable.pkStrategy
            return
          }
        }
        
        // 提交表单
        if (form.id) {
          await updateTable(form)
          ElMessage.success('操作成功')
        } else {
          await addTable(form)
          ElMessage.success('表创建成功，可前往模块管理关联该表')
        }
        dialogVisible.value = false
        loadData()
      } catch (error) {
        // 只处理实际错误，表单验证失败和取消操作的错误已经被单独处理
        if (!(error.message === 'cancel' || error.toString().includes('取消'))) {
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '操作失败')
        }
      }
    }

    const handleToggleEnable = async (row) => {
      try {
        const newStatus = row.isEnabled === 1 ? 0 : 1
        const statusText = newStatus === 1 ? '启用' : '禁用'
        
        // 弹出确认框
        await ElMessageBox.confirm(`确定要${statusText}该表吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 传递完整的表信息，包括主键生成策略，避免后端检查时出现null值
        const updateData = {
          ...row,
          isEnabled: newStatus
        }
        await updateTable(updateData)
        ElMessage.success(`${statusText}成功`)
        loadData()
      } catch (error) {
        // 如果用户取消操作，不显示错误信息
        // Element Plus 的取消操作会抛出一个带有 name 属性为 'cancel' 的错误对象
        if (!(error.name === 'cancel' || error.toString().includes('cancel'))) {
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '状态更新失败')
        }
      }
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该表吗？删除后关联的字段也会被删除', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteTable(row.id)
          ElMessage.success('删除成功')
          loadData()
        } catch (error) {
          // 显示后端返回的具体错误信息，适配多种错误格式
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '删除失败')
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    // 处理选中行变化
    const handleSelectionChange = (selection) => {
      selectedRows.value = selection
    }

    // 批量删除
    const handleBatchDelete = () => {
      if (selectedRows.value.length === 0) {
        ElMessage.warning('请选择要删除的表')
        return
      }
      
      ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个表吗？删除后关联的字段也会被删除`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = selectedRows.value.map(row => row.id)
          await batchDeleteTable({ ids })
          ElMessage.success('批量删除成功')
          loadData()
          // 清空选中状态
          selectedRows.value = []
        } catch (error) {
          // 显示后端返回的具体错误信息，适配多种错误格式
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '批量删除失败')
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }
    
    // 批量启用/禁用
    const handleBatchToggleEnable = (status) => {
      if (selectedRows.value.length === 0) {
        ElMessage.warning('请选择要操作的表')
        return
      }
      
      const actionText = status === 1 ? '启用' : '禁用'
      
      ElMessageBox.confirm(`确定要${actionText}选中的 ${selectedRows.value.length} 个表吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = selectedRows.value.map(row => row.id)
          await batchUpdateTableStatus(ids, status)
          ElMessage.success(`批量${actionText}成功`)
          loadData()
          // 清空选中状态
          selectedRows.value = []
        } catch (error) {
          // 显示后端返回的具体错误信息，适配多种错误格式
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || `批量${actionText}失败`)
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadData()
      loadBusinessSystems()
    })

    return {
      tableData,
      businessSystems,
      dialogVisible,
      dialogTitle,
      formRef,
      tableRef,
      selectedRows,
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
      handleToggleEnable,
      handleDelete,
      handleBatchDelete,
      handleBatchToggleEnable,
      handleSelectionChange,
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

