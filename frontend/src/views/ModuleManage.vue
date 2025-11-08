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
          <el-select v-model="searchForm.moduleType" placeholder="请选择" clearable>
            <el-option
              v-for="type in moduleTypes"
              :key="type.typeCode"
              :label="type.typeName"
              :value="type.typeCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
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
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
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

export default {
  name: 'ModuleManage',
  setup() {
    const tableData = ref([])
    const moduleTypes = ref([])
    const allTables = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增模块')
    const formRef = ref(null)
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
      tableCodes: []
    })
    const rules = {
      moduleCode: [{ required: true, message: '请输入模块编码', trigger: 'blur' }],
      moduleName: [{ required: true, message: '请输入模块名称', trigger: 'blur' }],
      moduleType: [{ required: true, message: '请选择模块类型', trigger: 'change' }]
    }

    const loadData = async () => {
      try {
        const res = await getModuleList(searchForm)
        if (res.code === 200) {
          tableData.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载数据失败')
      }
    }

    const loadModuleTypes = async () => {
      try {
        const res = await getModuleTypeList()
        if (res.code === 200) {
          moduleTypes.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载模块类型失败')
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
      loadData()
    }

    const handleReset = () => {
      searchForm.moduleName = ''
      searchForm.moduleType = ''
      searchForm.status = null
      loadData()
    }

    const handleAdd = () => {
      dialogTitle.value = '新增模块'
      Object.assign(form, {
        id: null,
        moduleCode: '',
        moduleName: '',
        moduleType: '',
        description: '',
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
            ElMessage.error('操作失败')
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
          ElMessage.error('删除失败')
        }
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
        ElMessage.error('操作失败')
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
      handleSearch,
      handleReset,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleToggleStatus,
      handleDialogClose
    }
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
</style>

