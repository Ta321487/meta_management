<template>

  <div class="page-module-type">

    <el-card>

      <template #header>

        <div class="card-header">

          <span>模块类型管理</span>

          <div>

            <el-button type="danger" @click="handleBatchDelete" :disabled="!selectedRows || selectedRows.length === 0">批量删除</el-button>

            <el-button type="primary" @click="handleAdd">新增类型</el-button>

          </div>

        </div>
      </template>

      <el-alert
        class="type-guide-alert"
        type="info"
        :closable="false"
        show-icon
        title="模块类型需在本页手动新增保存，系统启动时不会自动写入数据库。"
        description="为模块挂上类型后，「模块管理」中「重建功能节点」将按所选默认节点生成页面骨架。下方可参考常用类型组合与各节点用途，也可一键预填后保存。"
      />

      <el-collapse v-model="guideCollapse" class="type-guide-collapse">
        <el-collapse-item name="templates">
          <template #title>
            {{ collapsePanelTitle('参考类型模板', 'templates') }}
          </template>
          <div v-for="item in typeTemplates" :key="item.typeCode" class="template-card">
            <div class="template-head">
              <span class="template-title">{{ item.typeName }}</span>
              <el-tag size="small" type="info">{{ item.typeCode }}</el-tag>
              <el-button type="primary" link size="small" @click="applyTemplate(item)">按此模板新增</el-button>
            </div>
            <p class="template-desc">{{ item.description }}</p>
            <p class="template-nodes">
              <span class="label">建议默认节点：</span>
              <el-tag v-for="code in item.defaultNodes" :key="code" size="small" class="node-tag">
                {{ nodeTypeMap[code] || code }}
              </el-tag>
            </p>
          </div>
        </el-collapse-item>
        <el-collapse-item name="nodes">
          <template #title>
            {{ collapsePanelTitle('功能节点类型说明', 'nodes') }}
          </template>
          <el-table :data="nodeTypeHelp" border size="small" class="node-help-table">
            <el-table-column prop="label" label="节点" width="120" />
            <el-table-column prop="code" label="编码" width="160" />
            <el-table-column prop="usage" label="典型用途" show-overflow-tooltip />
          </el-table>
        </el-collapse-item>
      </el-collapse>

      <el-form :inline="true" :model="searchForm" class="search-form">

        <el-form-item label="类型编码">

          <el-input v-model="searchForm.typeCode" placeholder="请输入类型编码" clearable @input="handleSearch" />

        </el-form-item>

        <el-form-item label="类型名称">

          <el-input v-model="searchForm.typeName" placeholder="请输入类型名称" clearable @input="handleSearch" />

        </el-form-item>

      </el-form>



      <el-table :data="tableData" border style="width: 100%" ref="tableRef" @selection-change="handleSelectionChange">

        <el-table-column type="selection" width="55" />

        <el-table-column prop="typeCode" label="类型编码" width="180" />

        <el-table-column prop="typeName" label="类型名称" />

        <el-table-column prop="defaultNodes" label="默认节点" :formatter="formatDefaultNodes" />

        <el-table-column prop="description" label="描述" show-overflow-tooltip />

        <el-table-column label="操作" width="180" fixed="right">

          <template #default="{ row }">

            <el-space>

              <el-button size="small" @click="handleEdit(row)">编辑</el-button>

              <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>

            </el-space>

          </template>

        </el-table-column>

      </el-table>



      <div class="pagination-wrap">

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



    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="560px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="类型编码" prop="typeCode">
          <el-input
            v-model="form.typeCode"
            :disabled="!!form.id"
            placeholder="如：DATA_MANAGE"
          />
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="form.typeName" placeholder="如：数据管理型" />
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

import { ref, reactive, onMounted, nextTick } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import { getModuleTypeList, addModuleType, updateModuleType, deleteModuleType, batchDeleteModuleType } from '../api'



export default {

  name: 'ModuleTypeManage',

  setup() {

    const tableData = ref([])

    const tableRef = ref(null)

    const selectedRows = ref([])

    const dialogVisible = ref(false)

    const dialogTitle = ref('新增类型')

    const formRef = ref(null)

    const form = reactive({ id: null, typeCode: '', typeName: '', defaultNodes: [], description: '' })

    const rules = {
      typeCode: [
        { required: true, message: '请输入类型编码', trigger: 'blur' },
        { pattern: /^[A-Z][A-Z0-9_]*$/, message: '编码须为大写字母、数字或下划线，且以字母开头', trigger: 'blur' }
      ],
      typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
      defaultNodes: [
        { type: 'array', required: true, min: 1, message: '请至少选择一个默认节点', trigger: 'change' }
      ]
    }



    const searchForm = reactive({

      typeCode: '',

      typeName: ''

    })



    const pagination = reactive({

      current: 1,

      size: 10,

      total: 0

    })



    const guideCollapse = ref(['templates'])

    const collapsePanelTitle = (label, name) => {
      const expanded = guideCollapse.value.includes(name)
      return `${label}（点击${expanded ? '收起' : '展开'}）`
    }

    const nodeTypeMap = {
      LIST_PAGE: '列表页',
      FORM_PAGE: '表单页',
      DETAIL_PAGE: '详情页',
      IMPORT_PAGE: '导入页',
      PROCESS_PAGE: '流程页',
      REPORT_PAGE: '报表页',
      BATCH_IMPORT_PAGE: '批量导入页',
      BATCH_EXPORT_PAGE: '批量导出页',
      CUSTOM_PAGE: '自定义页面'
    }

    const typeTemplates = [
      {
        typeCode: 'DATA_MANAGE',
        typeName: '数据管理型',
        defaultNodes: ['LIST_PAGE', 'FORM_PAGE', 'DETAIL_PAGE'],
        description: '适用于主数据、档案类模块：列表查询与维护，配套新增/编辑表单与只读详情。'
      },
      {
        typeCode: 'PROCESS_APPROVE',
        typeName: '流程审批型',
        defaultNodes: ['LIST_PAGE', 'FORM_PAGE', 'PROCESS_PAGE'],
        description: '适用于请假、报销、订单审批等：在列表与表单基础上增加流程流转/审批页。'
      },
      {
        typeCode: 'STAT_REPORT',
        typeName: '统计报表型',
        defaultNodes: ['LIST_PAGE', 'REPORT_PAGE'],
        description: '适用于经营看板、统计分析：列表作入口或维度筛选，报表页展示图表与汇总。'
      },
      {
        typeCode: 'BATCH_OPERATE',
        typeName: '批量操作型',
        defaultNodes: ['LIST_PAGE', 'FORM_PAGE', 'DETAIL_PAGE', 'IMPORT_PAGE'],
        description: '适用于需批量导入或大批量维护的数据：在标准 CRUD 节点上增加导入页。'
      }
    ]

    const nodeTypeHelp = [
      { code: 'LIST_PAGE', label: '列表页', usage: '数据列表、查询、分页；重建功能节点时通常作为模块入口' },
      { code: 'FORM_PAGE', label: '表单页', usage: '新增、编辑单条记录；与列表页联动打开' },
      { code: 'DETAIL_PAGE', label: '详情页', usage: '只读查看单条记录详情' },
      { code: 'IMPORT_PAGE', label: '导入页', usage: '文件/模板导入单批数据（与批量导入页区分场景时可二选一）' },
      { code: 'PROCESS_PAGE', label: '流程页', usage: '审批、流转、待办处理等流程类界面' },
      { code: 'REPORT_PAGE', label: '报表页', usage: '统计图表、汇总报表展示' },
      { code: 'BATCH_IMPORT_PAGE', label: '批量导入页', usage: '大批量数据导入、导入结果反馈' },
      { code: 'BATCH_EXPORT_PAGE', label: '批量导出页', usage: '按条件导出 Excel 等批量下载' },
      { code: 'CUSTOM_PAGE', label: '自定义页面', usage: '非标准 CRUD/报表的定制页面，需自行配置路由与组件' }
    ]



    const formatDefaultNodes = (row, column, cellValue) => {

      if (!cellValue || !Array.isArray(cellValue) || cellValue.length === 0) {

        return ''

      }

      return cellValue.map(node => nodeTypeMap[node] || node).join('，')

    }



    const loadData = async () => {

      try {

        const params = {

          ...searchForm,

          current: pagination.current,

          size: pagination.size

        }

        const res = await getModuleTypeList(params)

        if (res.code === 200) {

          if (res.data && res.data.records) {

            tableData.value = res.data.records

            pagination.total = Number(res.data.total) || 0

          } else {

            tableData.value = res.data || []

            pagination.total = Number(res.data?.length) || 0

          }

        }

      } catch (e) {

        ElMessage.error(e.response?.data?.message || e.data?.message || e.message || '加载失败')

        tableData.value = []

        pagination.total = 0

      }

    }



    const handleSearch = () => {

      pagination.current = 1

      loadData()

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



    const handleSelectionChange = (rows) => {

      selectedRows.value = rows

    }



    const resetForm = () => {
      Object.assign(form, { id: null, typeCode: '', typeName: '', defaultNodes: [], description: '' })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    const openAddDialog = () => {
      dialogVisible.value = true
      nextTick(() => {
        formRef.value?.clearValidate()
      })
    }

    const handleAdd = () => {
      dialogTitle.value = '新增类型'
      resetForm()
      openAddDialog()
    }

    const applyTemplate = (template) => {
      dialogTitle.value = '新增类型'
      Object.assign(form, {
        id: null,
        typeCode: template.typeCode,
        typeName: template.typeName,
        defaultNodes: [...template.defaultNodes],
        description: template.description
      })
      openAddDialog()
    }



    const handleEdit = (row) => {
      dialogTitle.value = '编辑类型'
      Object.assign(form, {
        id: row.id,
        typeCode: row.typeCode,
        typeName: row.typeName,
        defaultNodes: Array.isArray(row.defaultNodes) ? [...row.defaultNodes] : [],
        description: row.description || ''
      })
      dialogVisible.value = true
      nextTick(() => {
        if (formRef.value) {
          formRef.value.clearValidate()
        }
      })
    }



    const handleDelete = (row) => {

      ElMessageBox.confirm('确定要删除该模块类型吗？', '提示', {

        confirmButtonText: '确定',

        cancelButtonText: '取消',

        type: 'warning'

      }).then(async () => {

        try {

          await deleteModuleType(row.id)

          ElMessage.success('删除成功')

          loadData()

        } catch (e) {

          ElMessage.error(e.response?.data?.message || e.data?.message || e.message || '删除失败')

        }

      }).catch(() => {})

    }



    const handleBatchDelete = async () => {

      if (!selectedRows.value || selectedRows.value.length === 0) {

        return

      }

      try {

        await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 个模块类型吗？`, '批量删除确认', {

          confirmButtonText: '确定',

          cancelButtonText: '取消',

          type: 'warning'

        })

        const ids = selectedRows.value.map(row => row.id)

        await batchDeleteModuleType({ ids })

        ElMessage.success('批量删除成功')

        selectedRows.value = []

        if (tableRef.value) {

          tableRef.value.clearSelection()

        }

        loadData()

      } catch (e) {

        if (e !== 'cancel') {

          ElMessage.error(e.response?.data?.message || e.data?.message || e.message || '批量删除失败')

        }

      }

    }



    const handleSubmit = async () => {
      if (!formRef.value) return
      await formRef.value.validate(async (valid) => {
        if (!valid) return
        try {
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
          ElMessage.error(e.response?.data?.message || e.data?.message || e.message || '保存失败')
        }
      })
    }



    onMounted(() => {

      loadData()

    })



    return {

      tableData,

      tableRef,

      selectedRows,

      searchForm,

      pagination,

      dialogVisible,

      dialogTitle,

      form,

      formRef,

      rules,
      guideCollapse,
      collapsePanelTitle,
      typeTemplates,
      nodeTypeHelp,
      nodeTypeMap,

      handleAdd,
      applyTemplate,

      handleDialogClose,

      handleEdit,

      handleDelete,

      handleBatchDelete,

      handleSubmit,

      handleSearch,

      handleSizeChange,

      handleCurrentChange,

      handleSelectionChange,

      formatDefaultNodes

    }

  }

}

</script>



<style scoped>

.card-header { display: flex; justify-content: space-between; align-items: center; }
.type-guide-alert { margin-bottom: 16px; }
.type-guide-collapse { margin-bottom: 16px; }
.template-card {
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.template-card:last-child { border-bottom: none; }
.template-head { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 6px; }
.template-title { font-weight: 600; color: var(--el-text-color-primary); }
.template-desc { margin: 0 0 8px; font-size: 13px; color: var(--el-text-color-regular); line-height: 1.5; }
.template-nodes { margin: 0; font-size: 13px; color: var(--el-text-color-regular); }
.template-nodes .label { margin-right: 8px; }
.node-tag { margin-right: 6px; margin-bottom: 4px; }
.node-help-table { margin-top: 4px; }
.search-form { margin-bottom: 16px; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }

</style>

