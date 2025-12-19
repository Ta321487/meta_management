<template>
  <div class="business-system-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>业务系统管理</span>
          <div>
            <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedRows || selectedRows.length === 0">批量删除</el-button>
            <el-button type="primary" @click="handleAdd">新增业务系统</el-button>
          </div>
        </div>
      </template>
        <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="业务系统名称">
          <el-input v-model="searchForm.businessName" placeholder="请输入业务系统名称" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%" ref="tableRef" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="businessCode" label="业务编码" width="150" />
        <el-table-column prop="businessName" label="业务系统名称" />
        <el-table-column prop="packageName" label="包名" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="isDefault" label="是否默认" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'success' : 'info'">
              {{ row.isDefault === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="success" size="small" @click="handleSetDefault(row)" :disabled="row.isDefault === 1">设为默认</el-button>
              <el-button type="info" size="small" @click="handleAssociateModules(row)">关联模块</el-button>
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
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="业务编码" prop="businessCode" v-if="!form.id">
          <el-input v-model="form.businessCode" placeholder="如：DEFAULT, PET_MANAGE, PARKING_MANAGE" />
        </el-form-item>
        <el-form-item label="业务系统名称" prop="businessName">
          <el-input v-model="form.businessName" placeholder="请输入业务系统名称" />
        </el-form-item>
        <el-form-item label="包名" prop="packageName">
          <el-input v-model="form.packageName" placeholder="如：com.example" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入业务系统描述" />
        </el-form-item>
        <el-form-item label="是否默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
    
    <!-- 关联模块对话框 -->
    <el-dialog
      close-on-click-modal="false"
      close-on-press-escape="false"
      v-model="associateDialogVisible"
      title="关联模块"
      width="600px"
    >
      <div class="associate-dialog-content">
        <el-form :model="form" label-width="120px">
          <el-form-item label="业务系统名称">
            <el-input v-model="form.businessName" readonly />
          </el-form-item>
        </el-form>
        <div class="module-selection">
          <el-select
            v-model="selectedModules"
            multiple
            filterable
            placeholder="请选择要关联的模块"
            style="width: 100%;"
            value-key="moduleCode"
          >
            <el-option
              v-for="module in allModules"
              :key="module.moduleCode"
              :label="module.moduleName"
              :value="module"
            />
          </el-select>
        </div>

      </div>
      <template #footer>
        <el-button @click="associateDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAssociateModules">保存关联</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBusinessSystemList, addBusinessSystem, updateBusinessSystem, deleteBusinessSystem, getModuleList, getAssociatedModules, associateModulesToBusinessSystem } from '../api'

export default {
  name: 'BusinessSystemManage',
  setup() {
    // 表格数据
    const tableData = ref([])
    // 选中的行
    const selectedRows = ref([])
    // 表格引用
    const tableRef = ref(null)
    // 加载状态
    const loading = ref(false)
    // 提交状态
    const submitting = ref(false)
    // 对话框显示
    const dialogVisible = ref(false)
    // 对话框标题
    const dialogTitle = ref('新增业务系统')
    // 表单引用
    const formRef = ref(null)
    
    // 搜索表单
    const searchForm = reactive({
      businessName: ''
    })
    
    // 分页参数
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    
    // 表单数据
    const form = reactive({
      id: null,
      businessCode: '',
      businessName: '',
      packageName: '',
      description: '',
      isDefault: 0
    })
    
    // 表单验证规则
    const rules = {
      businessCode: [{ required: true, message: '请输入业务编码', trigger: 'blur' }],
      businessName: [{ required: true, message: '请输入业务系统名称', trigger: 'blur' }]
    }
    
    // 加载业务系统列表
    const loadData = async () => {
      loading.value = true
      try {
        const res = await getBusinessSystemList()
        if (res.code === 200) {
          tableData.value = res.data
          pagination.total = res.data.length
        }
      } catch (error) {
        ElMessage.error('加载失败: ' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }
    
    // 搜索
    const handleSearch = () => {
      pagination.current = 1
      loadData()
    }
    
    // 重置
    const handleReset = () => {
      searchForm.businessName = ''
      pagination.current = 1
      loadData()
    }
    
    // 分页大小变化
    const handleSizeChange = (size) => {
      pagination.size = size
      pagination.current = 1
      loadData()
    }
    
    // 页码变化
    const handleCurrentChange = (current) => {
      pagination.current = current
      loadData()
    }
    
    // 选择行变化
    const handleSelectionChange = (rows) => {
      selectedRows.value = rows
    }
    
    // 新增业务系统
    const handleAdd = () => {
      dialogVisible.value = true
      dialogTitle.value = '新增业务系统'
      form.id = null
      form.businessCode = ''
      form.businessName = ''
      form.packageName = ''
      form.description = ''
      form.isDefault = 0
    }
    
    // 编辑业务系统
    const handleEdit = (row) => {
      dialogVisible.value = true
      dialogTitle.value = '编辑业务系统'
      form.id = row.id
      form.businessCode = row.businessCode
      form.businessName = row.businessName
      form.packageName = row.packageName || ''
      form.description = row.description
      form.isDefault = row.isDefault
    }
    
    // 设为默认
    const handleSetDefault = async (row) => {
      try {
        await updateBusinessSystem({
          id: row.id,
          businessName: row.businessName,
          description: row.description,
          isDefault: 1
        })
        ElMessage.success('设置成功')
        loadData()
      } catch (error) {
        ElMessage.error('设置失败: ' + (error.message || '未知错误'))
      }
    }
    
    // 删除业务系统
    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(`确定要删除业务系统「${row.businessName}」吗？`, '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteBusinessSystem({ id: row.id })
        ElMessage.success('删除成功')
        loadData()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败: ' + (error.message || '未知错误'))
        }
      }
    }
    
    // 批量删除
    const handleBatchDelete = async () => {
      try {
        await ElMessageBox.confirm(`确定要删除选中的${selectedRows.value.length}个业务系统吗？`, '批量删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        for (const row of selectedRows.value) {
          await deleteBusinessSystem({ id: row.id })
        }
        ElMessage.success('批量删除成功')
        loadData()
        selectedRows.value = []
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败: ' + (error.message || '未知错误'))
        }
      }
    }
    
    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      await formRef.value.validate(async (valid) => {
        if (valid) {
          submitting.value = true
          try {
            if (form.id) {
              // 更新
              await updateBusinessSystem(form)
              ElMessage.success('更新成功')
            } else {
              // 新增
              await addBusinessSystem(form)
              ElMessage.success('新增成功')
            }
            dialogVisible.value = false
            loadData()
          } catch (error) {
            ElMessage.error('操作失败: ' + (error.message || '未知错误'))
          } finally {
            submitting.value = false
          }
        }
      })
    }
    
    // 对话框关闭
    const handleDialogClose = () => {
      if (formRef.value) {
        formRef.value.resetFields()
      }
    }
    
    // 关联模块对话框
    const associateDialogVisible = ref(false)
    const allModules = ref([])
    const selectedModules = ref([])
    const currentBusinessCode = ref('')
    
    // 获取所有模块
    const getAllModules = async () => {
      try {
        const res = await getModuleList({})
        if (res.code === 200) {
          allModules.value = res.data
        }
      } catch (error) {
        ElMessage.error('获取模块列表失败: ' + (error.message || '未知错误'))
      }
    }
    
    // 获取已关联模块
    const fetchAssociatedModules = async (businessCode) => {
      try {
        const res = await getAssociatedModules(businessCode)
        if (res.code === 200) {
          return res.data
        }
        return []
      } catch (error) {
        ElMessage.error('获取已关联模块失败: ' + (error.message || '未知错误'))
        return []
      }
    }
    
    // 关联模块
    const handleAssociateModules = async (row) => {
      currentBusinessCode.value = row.businessCode
      form.businessName = row.businessName
      await getAllModules()
      const associatedModuleCodes = await fetchAssociatedModules(row.businessCode)
      selectedModules.value = allModules.value.filter(module => associatedModuleCodes.includes(module.moduleCode))
      associateDialogVisible.value = true
    }
    
    // 保存关联模块
    const saveAssociateModules = async () => {
      try {
        await associateModulesToBusinessSystem({
          businessCode: currentBusinessCode.value,
          moduleCodes: selectedModules.value.map(module => module.moduleCode)
        })
        ElMessage.success('关联成功')
        associateDialogVisible.value = false
      } catch (error) {
        ElMessage.error('关联失败: ' + (error.message || '未知错误'))
      }
    }
    
    // 初始化
    onMounted(() => {
      loadData()
    })
    
    return {
      tableData,
      selectedRows,
      tableRef,
      loading,
      submitting,
      dialogVisible,
      dialogTitle,
      formRef,
      searchForm,
      pagination,
      form,
      rules,
      handleSearch,
      handleReset,
      handleSizeChange,
      handleCurrentChange,
      handleSelectionChange,
      handleAdd,
      handleEdit,
      handleSetDefault,
      handleDelete,
      handleBatchDelete,
      handleSubmit,
      handleDialogClose,
      // 关联模块相关
      associateDialogVisible,
      allModules,
      selectedModules,
      handleAssociateModules,
      saveAssociateModules
    }
  }
}
</script>

<style scoped>
.business-system-manage {
  /* 移除多余的padding，使用el-card的默认样式 */
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
