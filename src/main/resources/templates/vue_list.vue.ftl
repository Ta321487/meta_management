<#-- 辅助函数：转义正则表达式字符串 -->
<#function escapeRegexPattern pattern>
  <#return pattern?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>
<#function escapeJsString str>
  <#return str?replace("\\", "\\\\")?replace("'", "\\'")?replace("\"", "\\\"")?replace("\n", "\\n")?replace("\r", "\\r")?replace("\t", "\\t")>
</#function>

<template>
  <div class="admin-page ${componentName?lower_case}-list">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">${menuTitle!table.tableName}</h2>
        <span class="page-desc">${businessName!""} · 数据列表</span>
      </div>
      <div class="page-header-actions">
        <el-button type="danger" plain :disabled="multipleSelection.length === 0" @click="handleBatchDelete">批量删除</el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon style="vertical-align: middle; margin-right: 4px"><Plus /></el-icon>新增
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" :inline="true" class="search-form" @submit.prevent>
<#list fields as field>
        <#if field.field.formComponent != "primary_key" && field.field.formComponent != "textarea" && (field.field.formComponent == "input" || field.field.formComponent == "select" || field.field.formComponent == "datepicker" || field.field.formComponent == "date" || field.field.formComponent == "number")>
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
          <#elseif field.field.formComponent == "datepicker" || field.field.formComponent == "date">
          <el-date-picker v-model="searchForm.${field.camelCaseName}" type="date" placeholder="请选择${field.field.label}" clearable style="width: 180px" />
          <#elseif field.field.formComponent == "number">
          <el-input-number v-model="searchForm.${field.camelCaseName}" placeholder="请输入${field.field.label}" clearable style="width: 180px" />
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
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="tableData"
        border
        stripe
        highlight-current-row
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
<#list fields as field>
        <#if field.isForeignKey!false>
        <el-table-column label="${field.field.label}" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ ${field.camelCaseName}FkLabel(row) }}
          </template>
        </el-table-column>
        <#else>
        <el-table-column 
          prop="${field.camelCaseName}" 
          label="${field.field.label}"
          sortable="custom"
          @sort-change="(sort) => handleSortChange('${field.camelCaseName}', sort)"
        />
        </#if>
</#list>
        <el-table-column label="操作" width="${hasDetailPage?then(260, 200)}" fixed="right">
          <template #default="{ row }">
            <#if hasDetailPage>
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
            </#if>
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      class="dialog-form"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
<#list fields as field>
        <#if field.field.formComponent != "primary_key">
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
          <#elseif field.field.formComponent == "datepicker" || field.field.formComponent == "date">
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
import { Plus } from '@element-plus/icons-vue'
<#if hasDetailPage>
import { useRouter } from 'vue-router'
</#if>
import { ${componentName}Api<#list relatedLoadTargets as rt>, ${rt.relatedTableClassName}Api</#list> } from '../api'

export default {
  name: '${componentName}List',
  setup() {
    <#if hasDetailPage>
    const router = useRouter()
    </#if>
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
      <#if field.field.fieldName != "id" && field.field.formComponent != "textarea" && (field.field.formComponent == "input" || field.field.formComponent == "select" || field.field.formComponent == "datepicker" || field.field.formComponent == "date" || field.field.formComponent == "number")>
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

    <#list relatedLoadTargets as rt>
    const ${rt.relatedTableCamelCaseName}List = ref([])
    </#list>

    <#list relatedLoadTargets as rt>
    const load${rt.relatedTableClassName}Data = async () => {
      try {
        const res = await ${rt.relatedTableClassName}Api.list()
        const raw = res.data
        ${rt.relatedTableCamelCaseName}List.value = Array.isArray(raw) ? raw : (raw && raw.records ? raw.records : [])
      } catch (e) {
        ${rt.relatedTableCamelCaseName}List.value = []
      }
    }
    </#list>

    <#list fields as field>
    <#if field.isForeignKey!false>
    const ${field.camelCaseName}FkLabel = (row) => {
      const val = row.${field.camelCaseName}
      if (val === null || val === undefined || val === '') return ''
      const list = ${field.relatedTableCamelCaseName}List.value
      const item = list.find(i => i.id === val || String(i.id) === String(val) || i.${field.relatedTableFieldName} === val || String(i.${field.relatedTableFieldName}) === String(val))
      return item != null ? String(item.${field.relatedTableFieldName}) : String(val)
    }
    </#if>
    </#list>
    
    const rules = {
<#list fields as field>
      <#if field.field.fieldName != "id" && (field.field.isRequired == 1 || (field.validationRules?? && (field.validationRules.hasPattern!false || field.validationRules.hasLength!false || field.validationRules.hasRange!false)) || field.field.fieldType?contains("int") || field.field.fieldType?contains("decimal") || field.field.fieldType?contains("double") || field.field.fieldType?contains("float"))>
      ${field.camelCaseName}: [
        <#if field.field.isRequired == 1>
        { required: true, message: '请输入${field.field.label}', trigger: 'blur' },
        </#if>
        <#if field.field.fieldType?contains("int") || field.field.fieldType?contains("decimal") || field.field.fieldType?contains("double") || field.field.fieldType?contains("float")>
        { type: 'number', message: '请输入有效的数字', trigger: 'blur' },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasPattern!false)>
        { 
          pattern: new RegExp('${escapeRegexPattern(field.validationRules.pattern!)}'), 
          message: '${escapeJsString(field.validationRules.patternMessage!"格式不正确")}', 
          trigger: 'blur' 
        },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasLength!false)>
        { 
          <#if field.validationRules?exists && field.validationRules.minLength?exists>min: ${field.validationRules.minLength!0}, </#if>
          <#if field.validationRules?exists && field.validationRules.maxLength?exists>max: ${field.validationRules.maxLength!9999}, </#if>
          message: '${escapeJsString(field.validationRules.lengthMessage!"长度必须在${minLength}到${maxLength}之间")}', 
          trigger: 'blur' 
        },
        </#if>
        <#if (field.validationRules?? && field.validationRules.hasRange!false)>
        <#-- hasRange：元数据 min/max 为数值范围；字符串长度用 minLength/maxLength -->
        { 
          type: 'number',
          <#if field.validationRules?exists && field.validationRules.min?exists>min: ${field.validationRules.min!-99999999}, </#if>
          <#if field.validationRules?exists && field.validationRules.max?exists>max: ${field.validationRules.max!99999999}, </#if>
          message: '${escapeJsString(field.validationRules.rangeMessage!"数值必须在${min}到${max}之间")}', 
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

    <#if hasDetailPage>
    const handleView = (row) => {
      const id = row.${primaryKeyCamelCase}
      if (id === null || id === undefined || id === '') {
        ElMessage.warning('无法查看：缺少主键')
        return
      }
      router.push('${detailRoutePrefix}' + id)
    }
    </#if>

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.${primaryKeyCamelCase}) {
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
          await ${componentName}Api.delete({ id: row.${primaryKeyCamelCase} })
          ElMessage.success('删除成功')
          loadData()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      }).catch(() => {
        // 取消删除操作
      })
    }

    const handleBatchDelete = () => {
      if (multipleSelection.value.length === 0) {
        ElMessage.warning('请选择要删除的记录')
        return
      }
      
      ElMessageBox.confirm('确定要批量删除选中的记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = multipleSelection.value.map(row => row.${primaryKeyCamelCase})
          await ${componentName}Api.batchDelete({ ids })
          ElMessage.success('批量删除成功')
          loadData()
        } catch (error) {
          ElMessage.error('批量删除失败')
        }
      }).catch(() => {
        // 取消批量删除操作
      })
    }

    const handleSelectionChange = (selection) => {
      multipleSelection.value = selection
    }

    const handleDialogClose = () => {
      // 重置表单验证状态
      formRef.value?.resetFields()
    }

    onMounted(async () => {
      <#list relatedLoadTargets as rt>
      await load${rt.relatedTableClassName}Data()
      </#list>
      loadData()
    })

    return {
      tableData,
      dialogVisible,
      dialogTitle,
      formRef,
      multipleSelection,
      loading,
      pagination,
      searchForm,
      sortParams,
      form,
      rules,
      loadData,
      handleSearch,
      handleReset,
      handleSortChange,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      <#if hasDetailPage>handleView,</#if>
      handleSubmit,
      handleDelete,
      handleBatchDelete,
      handleSelectionChange,
      handleDialogClose<#list relatedLoadTargets as rt>,
      ${rt.relatedTableCamelCaseName}List</#list><#list fields as field><#if field.isForeignKey!false>,
      ${field.camelCaseName}FkLabel</#if></#list>
    }
  }
}
</script>

<style scoped>
/* 布局见 src/styles/admin.css */
</style>