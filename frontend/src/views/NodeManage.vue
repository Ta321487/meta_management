<template>
  <div class="node-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>功能节点管理</span>
          <div>
            <el-select v-model="selectedModuleCode" placeholder="请选择模块" style="width: 200px; margin-right: 10px" @change="loadNodes">
              <el-option
                v-for="module in modules"
                :key="module.moduleCode"
                :label="module.moduleName"
                :value="module.moduleCode"
              />
            </el-select>
            <el-button type="primary" @click="handleAdd" :disabled="!selectedModuleCode">新增节点</el-button>
          </div>
        </div>
      </template>

      <el-table :data="nodeData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="nodeCode" label="节点编码" width="150" />
        <el-table-column prop="nodeName" label="节点名称" />
        <el-table-column prop="nodeType" label="节点类型" width="150" />
        <el-table-column prop="relatedTableCode" label="关联表" width="150" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
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
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModuleList, getNodeList, addNode, updateNode, deleteNode, getTablesByModule } from '../api'

export default {
  name: 'NodeManage',
  setup() {
    const modules = ref([])
    const tables = ref([])
    const nodeData = ref([])
    const selectedModuleCode = ref('')
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增节点')
    const formRef = ref(null)
    const form = reactive({
      id: null,
      nodeCode: '',
      nodeName: '',
      moduleCode: '',
      nodeType: '',
      relatedTableCode: '',
      jumpRelation: '',
      sort: 0,
      isEnabled: 1
    })
    const rules = {
      nodeCode: [{ required: true, message: '请输入节点编码', trigger: 'blur' }],
      nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
      nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }]
    }

    const loadModules = async () => {
      try {
        const res = await getModuleList({})
        if (res.code === 200) {
          modules.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载模块列表失败')
      }
    }

    const loadNodes = async () => {
      if (!selectedModuleCode.value) {
        nodeData.value = []
        return
      }
      loading.value = true
      try {
        const res = await getNodeList(selectedModuleCode.value)
        if (res.code === 200) {
          nodeData.value = res.data
        }
        // 加载关联的表
        const tableRes = await getTablesByModule(selectedModuleCode.value)
        if (tableRes.code === 200) {
          tables.value = tableRes.data
        }
      } catch (error) {
        ElMessage.error('加载节点列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleAdd = () => {
      dialogTitle.value = '新增节点'
      Object.assign(form, {
        id: null,
        nodeCode: '',
        nodeName: '',
        moduleCode: selectedModuleCode.value,
        nodeType: '',
        relatedTableCode: '',
        jumpRelation: '',
        sort: 0,
        isEnabled: 1
      })
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
        sort: row.sort,
        isEnabled: row.isEnabled
      })
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
            ElMessage.error('操作失败')
          }
        }
      })
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该节点吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteNode({ id: row.id })
          ElMessage.success('删除成功')
          loadNodes()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadModules()
    })

    return {
      modules,
      tables,
      nodeData,
      selectedModuleCode,
      loading,
      dialogVisible,
      dialogTitle,
      formRef,
      form,
      rules,
      loadNodes,
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
.node-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

