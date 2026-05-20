<template>
  <div class="module-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模块管理</span>
          <div>
            <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedRows || selectedRows.length === 0">批量删除</el-button>
            <el-button
              :type="batchEnableToggle.type"
              :disabled="batchEnableToggle.disabled"
              @click="handleBatchToggleStatus(batchEnableToggle.targetStatus)"
            >
              {{ batchEnableToggle.label }}
            </el-button>
            <el-button type="primary" @click="handleAdd">新增模块</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="模块名称">
          <el-input v-model="searchForm.moduleName" placeholder="请输入模块名称" clearable @input="handleSearch" />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="searchForm.moduleType" placeholder="请选择" clearable style="width: 200px" @change="handleSearch">
            <el-option
              v-for="type in moduleTypes"
              :key="type.typeCode"
              :label="type.typeName"
              :value="type.typeCode"
            />
          </el-select>
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
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 200px" @change="handleSearch">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%" ref="tableRef" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="moduleCode" label="模块编码" width="150" />
        <el-table-column prop="moduleName" label="模块名称" />
        <el-table-column prop="businessCode" label="业务系统" width="150">
          <template #default="{ row }">
            <el-tag>{{ row.businessCode || '未关联' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="moduleType" label="模块类型" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="tableCodes" label="关联表" width="150">
          <template #default="{ row }">
            <el-tag v-for="tableCode in row.tableCodes" :key="tableCode" size="small" style="margin-right: 5px;">
              {{ tableCode }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="图标" width="120">
          <template #default="{ row }">
            <div class="icon-item">
              <el-icon :size="24"><component :is="row.icon" /></el-icon>
              <span class="icon-text">{{ row.icon }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="routePath" label="路由路径" width="150" />
        <el-table-column prop="componentPath" label="组件路径" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-space>
              <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button
                :type="row.status === 1 ? 'warning' : 'success'"
                size="small"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
              <el-button type="info" size="small" @click="handleRebuildNodes(row)">重建功能节点</el-button>
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
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :before-close="formGuard.handleBeforeClose"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模块编码" prop="moduleCode" v-if="!form.id">
          <el-input
            v-model="form.moduleCode"
            placeholder="如：MODULE_001"
            @blur="applyIdentifierBlur(form, 'moduleCode', 'code')"
          />
        </el-form-item>
        <el-form-item label="模块名称" prop="moduleName">
          <el-input v-model="form.moduleName" placeholder="请输入模块名称" />
        </el-form-item>
        <el-form-item label="模块类型" prop="moduleType">
          <el-select v-model="form.moduleType" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="type in moduleTypes"
              :key="type.typeCode"
              :label="type.typeName"
              :value="type.typeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="业务系统">
          <el-select v-model="form.businessCode" placeholder="请选择业务系统" style="width: 100%" @change="loadTables(form.businessCode)">
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
        <el-form-item label="关联表">
          <el-select
            v-model="form.tableCodes"
            multiple
            placeholder="请选择表"
            style="width: 100%"
          >
            <el-option
              v-for="table in allTables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
          <div class="el-form-item__help" style="color: #67c23a; margin-top: 8px;">
            提示：关联表可后续在表管理创建后回到此处关联，以保证功能正常。
          </div>
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="1" :step="1" placeholder="请输入排序号" />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <div class="icon-select-wrapper">
            <el-input v-model="form.icon" placeholder="点击选择图标" readonly @click="showIconSelector = true" />
            <el-button type="primary" size="small" @click="showIconSelector = true">选择图标</el-button>
          </div>
        </el-form-item>
        <el-form-item label="路由路径" prop="routePath">
          <el-input v-model="form.routePath" placeholder="如：/student" />
        </el-form-item>
        <el-form-item label="组件路径" prop="componentPath">
          <el-input v-model="form.componentPath" placeholder="如：views/student" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formGuard.requestCloseDialog">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
      
      <!-- 图标选择器 -->
      <IconSelector
        v-model="form.icon"
        v-model:visible="showIconSelector"
      />
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getModuleList,
  addModule,
  updateModule,
  deleteModule,
  batchDeleteModule,
  updateModuleStatus,
  batchUpdateModuleStatus,
  rebuildNodes,
  getModuleTypeList,
  getTableList,
  getTablesByModule,
  getBusinessSystemList
} from '../api'
import IconSelector from '../components/IconSelector.vue'
import { applyIdentifierBlur, metadataCodeRules, normalizeFormCodes } from '../utils/identifierInput'
import { useDialogFormGuard } from '../composables/useUnsavedFormGuard'
import { resolveBatchEnableToggle } from '../utils/batchEnableToggle'

export default {
  name: 'ModuleManage',
  setup() {
    const tableData = ref([])
    const moduleTypes = ref([])
    const allTables = ref([])
    const businessSystems = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增模块')
    const formRef = ref(null)
    const tableRef = ref(null)
    const selectedRows = ref([])
    const batchEnableToggle = computed(() => resolveBatchEnableToggle(selectedRows.value, 'status'))
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const searchForm = reactive({
      moduleName: '',
      moduleType: '',
      status: null,
      businessCode: ''
    })
    const form = reactive({
      id: null,
      moduleCode: '',
      moduleName: '',
      moduleType: '',
      businessCode: '',
      description: '',
      sort: 1,
      icon: '',
      routePath: '',
      componentPath: '',
      tableCodes: []
    })
    const showIconSelector = ref(false)
    const rules = {
      moduleCode: metadataCodeRules('模块编码'),
      moduleName: [{ required: true, message: '请输入模块名称', trigger: 'blur' }],
      moduleType: [{ required: true, message: '请选择模块类型', trigger: 'change' }],
      sort: [{ required: true, message: '请输入排序号', trigger: 'blur' }],
      routePath: [{ pattern: '^(/[a-zA-Z0-9_-]+)*$', message: '路由路径格式不正确', trigger: 'blur' }]
    }

    const formGuard = useDialogFormGuard(form, dialogVisible, {
      onReset: () => formRef.value?.resetFields()
    })

    const loadData = async () => {
      try {
        const params = {
          ...searchForm,
          current: pagination.current,
          size: pagination.size
        }
        const res = await getModuleList(params)
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
        // 显示后端返回的具体错误信息，适配多种错误格式
        ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '加载数据失败')
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

    const loadModuleTypes = async () => {
      try {
        const res = await getModuleTypeList()
        if (res.code === 200) {
          moduleTypes.value = res.data
        }
      } catch (error) {
        // 显示后端返回的具体错误信息，适配多种错误格式
        ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '加载模块类型失败')
      }
    }

    const loadTables = async (businessCode = '') => {
      try {
        const res = await getTableList({ businessCode })
        if (res.code === 200) {
          // 过滤只显示启用的表
          allTables.value = res.data.filter(table => table.isEnabled === 1)
        }
      } catch (error) {
        ElMessage.error('加载表列表失败')
      }
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

    const handleSearch = () => {
      pagination.current = 1
      loadData()
    }

    const handleReset = () => {
      searchForm.moduleName = ''
      searchForm.moduleType = ''
      searchForm.status = null
      searchForm.businessCode = ''
      pagination.current = 1
      loadData()
    }

    const handleAdd = async () => {
      dialogTitle.value = '新增模块'
      
      // 自动计算排序号：获取当前表格中最大的排序号并加1
      let maxSort = 0
      if (tableData.value && tableData.value.length > 0) {
        maxSort = Math.max(...tableData.value.map(item => item.sort || 0))
      }
      
      Object.assign(form, {
        id: null,
        moduleCode: '',
        moduleName: '',
        moduleType: '',
        businessCode: '',
        description: '',
        sort: maxSort + 1,
        icon: '',
        routePath: '',
        componentPath: '',
        tableCodes: []
      })
      // 刷新模块类型列表，确保显示最新的模块类型
      await loadModuleTypes()
      dialogVisible.value = true
    }

    const handleEdit = async (row) => {
      dialogTitle.value = '编辑模块'
      Object.assign(form, {
        id: row.id,
        moduleCode: row.moduleCode,
        moduleName: row.moduleName,
        moduleType: row.moduleType,
        businessCode: row.businessCode || '',
        description: row.description,
        sort: row.sort || 1,
        icon: row.icon || '',
        routePath: row.routePath || '',
        componentPath: row.componentPath || '',
        tableCodes: []
      })
      // 刷新模块类型列表，确保显示最新的模块类型
      await loadModuleTypes()
      // 加载当前业务系统的表数据
      loadTables(form.businessCode)
      // 加载关联的表
      getTablesByModule(row.moduleCode).then((res) => {
        if (res.code === 200 && res.data) {
          form.tableCodes = res.data.map((t) => t.tableCode)
        }
      }).catch(() => {
        form.tableCodes = []
      })
      dialogVisible.value = true
    }

    // 注意：这里应该使用getTablesByModule，但为了兼容，暂时使用getTableList

    const handleSubmit = async () => {
      if (!formRef.value) return
      if (!form.id) {
        normalizeFormCodes(form, [{ key: 'moduleCode', mode: 'code' }])
      }
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateModule(form)
            } else {
              await addModule(form)
            }
            ElMessage.success('操作成功')
            formGuard.markClean()
            dialogVisible.value = false
            loadData()
          } catch (error) {
            // 显示后端返回的具体错误信息，适配多种错误格式
            ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '操作失败')
          }
        }
      })
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该模块吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteModule(row.id)
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

    // 重建功能节点
    const handleRebuildNodes = (row) => {
      ElMessageBox.confirm('确定要重建该模块的功能节点吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await rebuildNodes(row.moduleCode)
          ElMessage.success('功能节点重建成功')
          loadData()
        } catch (error) {
          // 显示后端返回的具体错误信息，适配多种错误格式
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '功能节点重建失败')
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
        ElMessage.warning('请选择要删除的模块')
        return
      }
      
      ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个模块吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = selectedRows.value.map(row => row.id)
          await batchDeleteModule({ ids })
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
    const handleBatchToggleStatus = (status) => {
      if (selectedRows.value.length === 0) {
        ElMessage.warning('请选择要操作的模块')
        return
      }
      
      const actionText = status === 1 ? '启用' : '禁用'
      
      ElMessageBox.confirm(`确定要${actionText}选中的 ${selectedRows.value.length} 个模块吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = selectedRows.value.map(row => row.id)
          await batchUpdateModuleStatus(ids, status)
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

    const handleToggleStatus = async (row) => {
      ElMessageBox.confirm(`确定要${row.status === 1 ? '禁用' : '启用'}该模块吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await updateModuleStatus({
            id: row.id,
            status: row.status === 1 ? 0 : 1
          })
          ElMessage.success('操作成功')
          loadData()
        } catch (error) {
          // 显示后端返回的具体错误信息，适配多种错误格式
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '操作失败')
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
      loadModuleTypes()
      loadTables()
      loadBusinessSystems()
    })

    return {
      tableData,
      moduleTypes,
      allTables,
      businessSystems,
      dialogVisible,
      dialogTitle,
      formRef,
      tableRef,
      selectedRows,
      batchEnableToggle,
      searchForm,
      form,
      rules,
      formGuard,
      applyIdentifierBlur,
      pagination,
      showIconSelector,
      loadTables,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleRebuildNodes,
      handleBatchDelete,
      handleBatchToggleStatus,
      handleToggleStatus,
      handleDialogClose,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange
    }
  },
  components: {
    IconSelector
  }
}
</script>

<style scoped>
.module-manage {
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

.icon-select-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.icon-text {
  font-size: 12px;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100px;
}
</style>

