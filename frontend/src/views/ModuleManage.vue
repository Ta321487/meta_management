<template>
  <div class="module-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模块管理</span>
          <el-button type="primary" @click="handleAdd">新增模块</el-button>
        </div>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="模块名称">
          <el-input v-model="searchForm.moduleName" placeholder="请输入模块名称" clearable />
        </el-form-item>
        <el-form-item label="模块类型">
          <el-select v-model="searchForm.moduleType" placeholder="请选择" clearable style="width: 200px">
            <el-option
              v-for="type in moduleTypes"
              :key="type.typeCode"
              :label="type.typeName"
              :value="type.typeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 200px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="moduleCode" label="模块编码" width="150" />
        <el-table-column prop="moduleName" label="模块名称" />
        <el-table-column prop="moduleType" label="模块类型" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="模块编码" prop="moduleCode" v-if="!form.id">
          <el-input v-model="form.moduleCode" placeholder="如：MODULE_001" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getModuleList,
  addModule,
  updateModule,
  deleteModule,
  updateModuleStatus,
  getModuleTypeList,
  getTableList,
  getTablesByModule
} from '../api'
import IconSelector from '../components/IconSelector.vue'

export default {
  name: 'ModuleManage',
  setup() {
    const tableData = ref([])
    const moduleTypes = ref([])
    const allTables = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增模块')
    const formRef = ref(null)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const searchForm = reactive({
      moduleName: '',
      moduleType: '',
      status: null
    })
    const form = reactive({
      id: null,
      moduleCode: '',
      moduleName: '',
      moduleType: '',
      description: '',
      sort: 1,
      icon: '',
      routePath: '',
      componentPath: '',
      tableCodes: []
    })
    const showIconSelector = ref(false)
    const rules = {
      moduleCode: [{ required: true, message: '请输入模块编码', trigger: 'blur' }],
      moduleName: [{ required: true, message: '请输入模块名称', trigger: 'blur' }],
      moduleType: [{ required: true, message: '请选择模块类型', trigger: 'change' }],
      sort: [{ required: true, message: '请输入排序号', trigger: 'blur' }],
      routePath: [{ pattern: '^(/[a-zA-Z0-9_-]+)*$', message: '路由路径格式不正确', trigger: 'blur' }]
    }

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

    const loadTables = async () => {
      try {
        const res = await getTableList({})
        if (res.code === 200) {
          allTables.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载表列表失败')
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
      pagination.current = 1
      loadData()
    }

    const handleAdd = () => {
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
        description: '',
        sort: maxSort + 1,
        icon: '',
        routePath: '',
        componentPath: '',
        tableCodes: []
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑模块'
      Object.assign(form, {
        id: row.id,
        moduleCode: row.moduleCode,
        moduleName: row.moduleName,
        moduleType: row.moduleType,
        description: row.description,
        sort: row.sort || 1,
        icon: row.icon || '',
        routePath: row.routePath || '',
        componentPath: row.componentPath || '',
        tableCodes: []
      })
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
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateModule(form)
            } else {
              await addModule(form)
            }
            ElMessage.success('操作成功')
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
          await deleteModule({ id: row.id })
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

    const handleToggleStatus = async (row) => {
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
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadData()
      loadModuleTypes()
      loadTables()
    })

    return {
      tableData,
      moduleTypes,
      allTables,
      dialogVisible,
      dialogTitle,
      formRef,
      searchForm,
      form,
      rules,
      pagination,
      showIconSelector,
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleToggleStatus,
      handleDialogClose,
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
</style>

