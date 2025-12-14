<template>
  <div class="node-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>功能节点管理</span>
          <div class="header-selectors">
          <el-select v-model="selectedBusinessCode" placeholder="请选择业务系统" style="width: 200px; margin-right: 10px" @change="() => { loadModules(); loadNodes(); }">
            <el-option
              v-for="businessSystem in businessSystems"
              :key="businessSystem.businessCode"
              :label="businessSystem.businessName"
              :value="businessSystem.businessCode"
            />
          </el-select>
          <el-select v-model="selectedModuleCode" placeholder="请选择模块" style="width: 200px; margin-right: 10px" @change="loadNodes">
            <el-option
              v-for="module in modules"
              :key="module.moduleCode"
              :label="module.moduleName"
              :value="module.moduleCode"
            />
          </el-select>
          <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedRows || selectedRows.length === 0">批量删除</el-button>
          <el-button type="primary" @click="handleAdd" :disabled="!selectedModuleCode">新增节点</el-button>
        </div>
        </div>
      </template>

      <el-table :data="nodeData" border style="width: 100%" v-loading="loading" ref="tableRef" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="nodeCode" label="节点编码" width="150" />
        <el-table-column prop="nodeName" label="节点名称" />
        <el-table-column prop="businessCode" label="业务系统" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.businessCode || '未关联' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nodeType" label="节点类型" width="150">
          <template #default="{ row }">
            {{ getNodeTypeName(row.nodeType) }}
          </template>
        </el-table-column>
        <el-table-column prop="relatedTableCode" label="关联表" width="150" />
        <el-table-column prop="routePath" label="路由路径" width="180" />
        <el-table-column prop="componentPath" label="组件路径" width="200" />
        <el-table-column prop="isMenuVisible" label="是否菜单显示" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isMenuVisible === 1 ? 'success' : 'info'">
              {{ row.isMenuVisible === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="icon" label="节点图标" width="120">
          <template #default="{ row }">
            <el-icon v-if="row.icon" :size="18">{{ row.icon }}</el-icon>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              :type="row.isEnabled === 1 ? 'warning' : 'success'" 
              size="small" 
              @click="handleToggleEnable(row)"
            >
              {{ row.isEnabled === 1 ? '禁用' : '启用' }}
            </el-button>
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="节点编码" prop="nodeCode" v-if="!form.id">
          <el-input v-model="form.nodeCode" placeholder="如：NODE_001" />
        </el-form-item>
        <el-form-item label="节点名称" prop="nodeName">
          <el-input v-model="form.nodeName" placeholder="请输入节点名称" />
        </el-form-item>
        <el-form-item label="节点类型" prop="nodeType">
          <el-select v-model="form.nodeType" placeholder="请选择" style="width: 100%">
            <el-option label="列表页" value="LIST_PAGE" />
            <el-option label="表单页" value="FORM_PAGE" />
            <el-option label="详情页" value="DETAIL_PAGE" />
            <el-option label="流程流转页" value="PROCESS_PAGE" />
            <el-option label="报表展示页" value="REPORT_PAGE" />
            <el-option label="批量导入页" value="BATCH_IMPORT_PAGE" />
            <el-option label="批量导出页" value="BATCH_EXPORT_PAGE" />
            <el-option label="自定义页面" value="CUSTOM_PAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务系统" prop="businessCode">
          <el-select v-model="form.businessCode" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="businessSystem in businessSystems"
              :key="businessSystem.businessCode"
              :label="businessSystem.businessName"
              :value="businessSystem.businessCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联表" prop="relatedTableCode">
          <el-select v-model="form.relatedTableCode" placeholder="请选择" clearable style="width: 100%">
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="跳转关系" prop="jumpRelation">
          <el-input v-model="form.jumpRelation" placeholder="如：NODE_001→NODE_002" />
        </el-form-item>
        <el-form-item label="路由路径" prop="routePath">
          <el-input v-model="form.routePath" placeholder="如：/list（相对于模块路由的子路径）" />
        </el-form-item>
        <el-form-item label="组件路径" prop="componentPath">
          <el-input v-model="form.componentPath" placeholder="如：List.vue（相对于模块组件路径的子路径）" />
        </el-form-item>
        <el-form-item label="是否在菜单显示" prop="isMenuVisible">
          <el-radio-group v-model="form.isMenuVisible">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="节点图标" prop="icon">
          <div class="icon-select-wrapper">
            <el-input v-model="form.icon" placeholder="点击选择图标" readonly @click="showIconSelector = true" />
            <el-button type="primary" size="small" @click="showIconSelector = true">选择图标</el-button>
          </div>
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
        <el-form-item label="是否启用" prop="isEnabled">
          <el-radio-group v-model="form.isEnabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModuleList, getNodeList, addNode, updateNode, deleteNode, batchDeleteNode, getTablesByModule, getBusinessSystemList } from '../api'
import IconSelector from '../components/IconSelector.vue'

export default {
  name: 'NodeManage',
  components: {
    IconSelector
  },
  setup() {
    const modules = ref([])
    const tables = ref([])
    const businessSystems = ref([])
    const nodeData = ref([])
    const selectedModuleCode = ref('')
    const selectedBusinessCode = ref('')
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增节点')
    const formRef = ref(null)
    const tableRef = ref(null)
    const selectedRows = ref([])
    const showIconSelector = ref(false)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const form = reactive({
      id: null,
      nodeCode: '',
      nodeName: '',
      moduleCode: '',
      nodeType: '',
      relatedTableCode: '',
      jumpRelation: '',
      routePath: '',
      componentPath: '',
      isMenuVisible: 1,
      icon: '',
      sort: 0,
      isEnabled: 1,
      businessCode: ''
    })
    const rules = {
      nodeCode: [{ required: true, message: '请输入节点编码', trigger: 'blur' }],
      nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
      nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }]
    }

    const loadModules = async () => {
      // 当未选择业务系统时，清空模块列表
      if (!selectedBusinessCode.value) {
        modules.value = []
        return
      }
      
      try {
        const params = {
          businessCode: selectedBusinessCode.value
        }
        const res = await getModuleList(params)
        if (res.code === 200) {
          modules.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载模块列表失败')
        modules.value = []
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

    const loadNodes = async () => {
      if (!selectedModuleCode.value) {
        nodeData.value = []
        pagination.total = 0
        // 清空表列表
        tables.value = []
        return
      }
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size
        }
        const res = await getNodeList(selectedModuleCode.value, params)
        if (res.code === 200) {
          if (res.data && res.data.records) {
            // 分页数据
            nodeData.value = res.data.records
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            nodeData.value = res.data || []
            pagination.total = Number(res.data?.length) || 0
          }
        }
        // 加载关联的表，传递业务系统编码
        const tableRes = await getTablesByModule(selectedModuleCode.value, {
          businessCode: selectedBusinessCode.value
        })
        if (tableRes.code === 200) {
          // 过滤掉禁用状态的表
          tables.value = tableRes.data.filter(table => table.isEnabled === 1)
        }
      } catch (error) {
        ElMessage.error('加载节点列表失败')
        nodeData.value = []
        pagination.total = 0
        tables.value = []
      } finally {
        loading.value = false
      }
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadNodes()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadNodes()
    }

    const handleAdd = () => {
      dialogTitle.value = '新增节点'
      
      // 计算当前节点列表中的最大排序号，新节点排序号为最大排序号+1
      let maxSort = 0
      if (nodeData.value && nodeData.value.length > 0) {
        // 找出当前所有节点中的最大排序号
        const existingSorts = nodeData.value
          .map(node => Number(node.sort) || 0)
          .filter(sort => !isNaN(sort))
          
        if (existingSorts.length > 0) {
          maxSort = Math.max(...existingSorts)
        }
      }
      
      Object.assign(form, {
        id: null,
        nodeCode: '',
        nodeName: '',
        moduleCode: selectedModuleCode.value,
        nodeType: '',
        relatedTableCode: '',
        jumpRelation: '',
        routePath: '',
        componentPath: '',
        isMenuVisible: 1,
        icon: '',
        sort: maxSort + 1,
        isEnabled: 1,
        businessCode: selectedBusinessCode.value
      })
      showIconSelector.value = false
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑节点'
      Object.assign(form, {
        id: row.id,
        nodeCode: row.nodeCode,
        nodeName: row.nodeName,
        moduleCode: row.moduleCode,
        nodeType: row.nodeType,
        relatedTableCode: row.relatedTableCode || '',
        jumpRelation: row.jumpRelation || '',
        routePath: row.routePath || '',
        componentPath: row.componentPath || '',
        isMenuVisible: typeof row.isMenuVisible === 'undefined' ? 1 : row.isMenuVisible,
        icon: row.icon || '',
        sort: row.sort,
        isEnabled: row.isEnabled,
        businessCode: row.businessCode || ''
      })
      showIconSelector.value = false
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateNode(form)
            } else {
              await addNode(form)
            }
            ElMessage.success('操作成功')
            dialogVisible.value = false
            loadNodes()
          } catch (error) {
        // 显示后端返回的具体错误信息，适配多种错误格式
        ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '操作失败')
      }
        }
      })
    }

    const handleToggleEnable = async (row) => {
      try {
        const newStatus = row.isEnabled === 1 ? 0 : 1
        const updateData = {
          id: row.id,
          nodeCode: row.nodeCode,
          moduleCode: row.moduleCode,
          isEnabled: newStatus
        }
        await updateNode(updateData)
        ElMessage.success('状态更新成功')
        loadNodes()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '状态更新失败')
      }
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该节点吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteNode(row.id)
          ElMessage.success('删除成功')
          loadNodes()
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

    // 批量删除功能节点
    const handleBatchDelete = () => {
      if (selectedRows.value.length === 0) {
        ElMessage.warning('请选择要删除的节点')
        return
      }

      ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个节点吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = selectedRows.value.map(row => row.id)
          await batchDeleteNode(ids)
          ElMessage.success('批量删除成功')
          loadNodes()
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

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    // 节点类型英文到中文的映射
    const getNodeTypeName = (nodeType) => {
      const nodeTypeMap = {
        'LIST_PAGE': '列表页',
        'FORM_PAGE': '表单页',
        'DETAIL_PAGE': '详情页',
        'PROCESS_PAGE': '流程流转页',
        'REPORT_PAGE': '报表展示页',
        'BATCH_IMPORT_PAGE': '批量导入页',
        'BATCH_EXPORT_PAGE': '批量导出页',
        'CUSTOM_PAGE': '自定义页面'
      }
      return nodeTypeMap[nodeType] || nodeType
    }

    onMounted(() => {
      loadModules()
      loadBusinessSystems()
    })

    // 监听模块变化，自动刷新节点列表
    watch([selectedModuleCode, modules], () => {
      if (selectedModuleCode.value) {
        loadNodes()
      }
    }, { deep: true })
    
    // 监听对话框中业务系统变化，重新加载表列表
    watch(() => form.businessCode, (newBusinessCode) => {
      if (dialogVisible.value && selectedModuleCode.value) {
        // 重新加载表列表，使用新的业务系统编码
        getTablesByModule(selectedModuleCode.value, {
          businessCode: newBusinessCode
        }).then(res => {
          if (res.code === 200) {
            // 过滤掉禁用状态的表
            tables.value = res.data.filter(table => table.isEnabled === 1)
          }
        }).catch(error => {
          ElMessage.error('加载表列表失败')
          tables.value = []
        })
      }
    })

    return {
      modules,
      tables,
      businessSystems,
      nodeData,
      selectedModuleCode,
      selectedBusinessCode,
      loading,
      dialogVisible,
      dialogTitle,
      formRef,
      tableRef,
      selectedRows,
      pagination,
      form,
      rules,
      showIconSelector,
      loadModules,
      loadNodes,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleToggleEnable,
      handleDelete,
      handleSelectionChange,
      handleBatchDelete,
      handleDialogClose,
      getNodeTypeName
    }
  }
}
</script>

<style scoped>
.node-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-selectors {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-select-wrapper {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>

