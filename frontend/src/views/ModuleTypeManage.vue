<template>
  <div class="page-module-type">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>模块类型管理</span>
          <el-button type="primary" @click="handleAdd">新增类型</el-button>
        </div>
      </template>
      <el-table :data="tableData" border style="width: 100%">
        <el-table-column prop="typeCode" label="类型编码" width="180" />
        <el-table-column prop="typeName" label="类型名称" />
        <el-table-column prop="defaultNodes" label="默认节点" :formatter="formatDefaultNodes" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-space>
            <el-button size="mini" @click="handleEdit(row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(row)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
      </el-table>
    </el-card>

  <el-dialog :title="dialogTitle" v-model="dialogVisible" close-on-click-modal="false" close-on-press-escape="false">
      <el-form :model="form" ref="formRef" label-width="120px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input v-model="form.typeCode" :disabled="form.id"></el-input>
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName"></el-input>
        </el-form-item>
        <el-form-item label="默认节点" prop="defaultNodes">
          <el-select v-model="form.defaultNodes" multiple placeholder="请选择默认节点" style="width: 100%">
            <el-option label="列表页" value="LIST_PAGE" />
            <el-option label="表单页" value="FORM_PAGE" />
            <el-option label="详情页" value="DETAIL_PAGE" />
            <el-option label="导入页" value="IMPORT_PAGE" />
            <el-option label="流程页" value="PROCESS_PAGE" />
            <el-option label="报表页" value="REPORT_PAGE" />
            <el-option label="批量导入页" value="BATCH_IMPORT_PAGE" />
            <el-option label="批量导出页" value="BATCH_EXPORT_PAGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" :rows="3"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModuleTypeList, addModuleType, updateModuleType, deleteModuleType } from '../api'

export default {
  name: 'ModuleTypeManage',
  setup() {
    const tableData = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增类型')
    const formRef = ref(null)
    const form = reactive({ id: null, typeCode: '', typeName: '', defaultNodes: '', description: '' })
    
    // 节点类型映射配置
    const nodeTypeMap = {
      'LIST_PAGE': '列表页',
      'FORM_PAGE': '表单页', 
      'DETAIL_PAGE': '详情页',
      'IMPORT_PAGE': '导入页',
      'PROCESS_PAGE': '流程页',
      'REPORT_PAGE': '报表页',
      'BATCH_IMPORT_PAGE': '批量导入页',
      'BATCH_EXPORT_PAGE': '批量导出页'
    }
    
    // 格式化默认节点显示为中文
    const formatDefaultNodes = (row, column, cellValue) => {
      if (!cellValue || !Array.isArray(cellValue) || cellValue.length === 0) {
        return ''
      }
      return cellValue.map(node => nodeTypeMap[node] || node).join('，')
    }

    const loadData = async () => {
      try {
        const res = await getModuleTypeList()
        if (res.code === 200) {
          tableData.value = res.data || []
        }
      } catch (e) {
        ElMessage.error('加载失败')
      }
    }

    const handleAdd = () => {
      dialogTitle.value = '新增类型'
      Object.assign(form, { id: null, typeCode: '', typeName: '', defaultNodes: '', description: '' })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑类型'
      Object.assign(form, { id: row.id, typeCode: row.typeCode, typeName: row.typeName, defaultNodes: row.defaultNodes, description: row.description })
      dialogVisible.value = true
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该模块类型吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteModuleType({ id: row.id })
          ElMessage.success('删除成功')
          loadData()
        } catch (e) {
          ElMessage.error('删除失败')
        }
      }).catch(() => {
        // 用户取消删除操作，不执行任何操作
      })
    }

    const handleSubmit = async () => {
      try {
        if (!form.typeCode || !form.typeName) {
          ElMessage.warning('请填写编码和名称')
          return
        }
        if (form.id) {
          await updateModuleType(form)
          ElMessage.success('更新成功')
        } else {
          await addModuleType(form)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadData()
      } catch (e) {
        ElMessage.error('保存失败')
      }
    }

    onMounted(() => {
      loadData()
    })

    return { tableData, dialogVisible, dialogTitle, form, formRef, handleAdd, handleEdit, handleDelete, handleSubmit, formatDefaultNodes }
  }
}
</script>

<style scoped>
.card-header { display:flex; justify-content:space-between; align-items:center; }
</style>
