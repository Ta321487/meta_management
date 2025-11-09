<#-- 辅助函数：转义正则表达式字符串 -->
<#function escapeRegexPattern pattern>
  <#return pattern?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>
<#function escapeJsString str>
  <#return str?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>

<template>
  <div class="${componentName?lower_case}-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>${table.tableName}</span>
          <div>
            <el-button type="danger" :disabled="multipleSelection.length === 0" @click="handleBatchDelete" style="margin-right: 10px">
              批量删除
            </el-button>
            <el-button type="primary" @click="handleAdd">新增</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form">
<#list fields as field>
        <#if field.field.fieldName != "id" && (field.field.formComponent == "input" || field.field.formComponent == "select")>
        <el-form-item label="${field.field.label}">
          <#if field.field.formComponent == "select">
          <el-select v-model="searchForm.${field.camelCaseName}" placeholder="请选择${field.field.label}" clearable style="width: 180px">
            <#if (field.validationRules?? && field.validationRules.hasOptions!false)>
              <#if field.validationRules.options?is_sequence>
                <#list field.validationRules.options as option>
                  <#if option?is_string>
            <el-option label="${option}" value="${option}" />
                  <#else>
            <el-option label="${option.label!option.value}" value="${option.value!option}" />
                  </#if>
                </#list>
              </#if>
            </#if>
          </el-select>
          <#else>
          <el-input v-model="searchForm.${field.camelCaseName}" placeholder="请输入${field.field.label}" clearable style="width: 180px" />
          </#if>
        </el-form-item>
        </#if>
</#list>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table 
        :data="tableData" 
        border 
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
<#list fields as field>
        <el-table-column 
          prop="${field.camelCaseName}" 
          label="${field.field.label}"
          sortable="custom"
          @sort-change="(sort) => handleSortChange('${field.camelCaseName}', sort)"
        />
</#list>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
<#list fields as field>
        <#if field.field.fieldName != "id">
        <el-form-item label="${field.field.label}" prop="${field.camelCaseName}">
          <#if field.field.formComponent == "input">
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          <#elseif field.field.formComponent == "select">
          <el-select v-model="form.${field.camelCaseName}" placeholder="请选择" style="width: 100%">
            <#if (field.validationRules?? && field.validationRules.hasOptions!false)>
              <#if field.validationRules.options?is_sequence>
                <#list field.validationRules.options as option>
                  <#if option?is_string>
            <el-option label="${option}" value="${option}" />
                  <#else>
            <el-option label="${option.label!option.value}" value="${option.value!option}" />
                  </#if>
                </#list>
              <#else>
            <el-option label="选项1" value="1" />
              </#if>
            <#else>
            <el-option label="选项1" value="1" />
            </#if>
          </el-select>
          <#elseif field.field.formComponent == "datepicker">
          <el-date-picker v-model="form.${field.camelCaseName}" type="date" placeholder="请选择日期" style="width: 100%" />
          <#elseif field.field.formComponent == "number">
          <el-input-number v-model="form.${field.camelCaseName}" style="width: 100%" />
          <#elseif field.field.formComponent == "textarea">
          <el-input v-model="form.${field.camelCaseName}" type="textarea" :rows="3" />
          <#else>
          <el-input v-model="form.${field.camelCaseName}" placeholder="请输入${field.field.label}" />
          </#if>
        </el-form-item>
        </#if>
</#list>
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
import { ${componentName}Api } from '../api'

export default {
  name: '${componentName}List',
  setup() {
    const tableData = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增')
    const formRef = ref(null)
    const multipleSelection = ref([])
    const loading = ref(false)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const searchForm = reactive({
<#list fields as field>
      <#if field.field.fieldName != "id" && (field.field.formComponent == "input" || field.field.formComponent == "select")>
      ${field.camelCaseName}: '',
      </#if>
</#list>
    })
    const sortParams = reactive({
      orderBy: '',
      orderDirection: 'DESC'
    })
    const form = reactive({
<#list fields as field>
      ${field.camelCaseName}: <#if field.field.fieldType?contains("int")>null<#elseif field.field.fieldType?contains("date")>null<#else>''</#if>,
</#list>
    })
    
    const rules = {
<#list fields as field>
      <#if field.field.fieldName != "id" && (field.field.isRequired == 1 || (field.validationRules?? && field.validationRules.hasPattern!false))>
      ${field.camelCaseName}: [
        <#if field.field.isRequired == 1>
        { required: true, message: '请输入${field.field.label}', trigger: 'blur' }<#if (field.validationRules?? && field.validationRules.hasPattern!false)>,</#if>
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasPattern!false)>
        { 
          pattern: new RegExp('${escapeRegexPattern(field.validationRules.pattern!)}'), 
          message: '${escapeJsString(field.validationRules.patternMessage!"格式不正确")}', 
          trigger: 'blur' 
        }
        </#if>
      ]<#sep>,</#sep>
      </#if>
</#list>
    }

    const loadData = async () => {
      loading.value = true
      try {
        // 构建查询条件
        const conditions = {}
        Object.keys(searchForm).forEach(key => {
          if (searchForm[key] !== null && searchForm[key] !== '' && searchForm[key] !== undefined) {
            conditions[key] = searchForm[key]
          }
        })
        
        const params = {
          current: pagination.current,
          size: pagination.size,
          conditions: Object.keys(conditions).length > 0 ? conditions : null
        }
        
        // 添加排序参数
        if (sortParams.orderBy) {
          params.orderBy = sortParams.orderBy
          params.orderDirection = sortParams.orderDirection
        }
        
        const res = await ${componentName}Api.page(params)
        if (res.code === 200) {
          tableData.value = res.data.records || []
          pagination.total = res.data.total || 0
        } else {
          ElMessage.error(res.message || '加载数据失败')
        }
      } catch (error) {
        ElMessage.error('加载数据失败：' + (error.message || '未知错误'))
        tableData.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }
    
    const handleSearch = () => {
      pagination.current = 1
      loadData()
    }
    
    const handleReset = () => {
      Object.keys(searchForm).forEach(key => {
        searchForm[key] = ''
      })
      sortParams.orderBy = ''
      sortParams.orderDirection = 'DESC'
      pagination.current = 1
      loadData()
    }
    
    const handleSortChange = (prop, sort) => {
      if (sort.order) {
        sortParams.orderBy = prop
        sortParams.orderDirection = sort.order === 'ascending' ? 'ASC' : 'DESC'
      } else {
        sortParams.orderBy = ''
        sortParams.orderDirection = 'DESC'
      }
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

    const handleAdd = () => {
      dialogTitle.value = '新增'
      Object.assign(form, {
<#list fields as field>
        ${field.camelCaseName}: <#if field.field.fieldType?contains("int")>null<#elseif field.field.fieldType?contains("date")>null<#else>''</#if>,
</#list>
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑'
      Object.assign(form, row)
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await ${componentName}Api.update(form)
            } else {
              await ${componentName}Api.add(form)
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
      ElMessageBox.confirm('确定要删除该记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await ${componentName}Api.delete({ id: row.id })
          ElMessage.success('删除成功')
          loadData()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    const handleSelectionChange = (selection) => {
      multipleSelection.value = selection
    }

    const handleBatchDelete = () => {
      if (multipleSelection.value.length === 0) {
        ElMessage.warning('请选择要删除的记录')
        return
      }
      ElMessageBox.confirm('确定要删除选中的 ' + multipleSelection.value.length + ' 条记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = multipleSelection.value.map(item => item.id)
          await ${componentName}Api.batchDelete({ ids })
          ElMessage.success('批量删除成功')
          multipleSelection.value = []
          loadData()
        } catch (error) {
          ElMessage.error('批量删除失败')
        }
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadData()
    })

    return {
      tableData,
      dialogVisible,
      dialogTitle,
      formRef,
      form,
      rules,
      pagination,
      multipleSelection,
      loading,
      searchForm,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleBatchDelete,
      handleSelectionChange,
      handleDialogClose,
      handleSizeChange,
      handleCurrentChange,
      handleSearch,
      handleReset,
      handleSortChange
    }
  }
}
</script>

<style scoped>
.${componentName?lower_case}-list {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}
</style>

