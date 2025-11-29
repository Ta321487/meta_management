<template>
  <div class="field-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字段管理</span>
          <div>
            <el-select v-model="selectedTableCode" placeholder="请选择表" style="width: 200px; margin-right: 10px" @change="loadFields">
              <el-option
                v-for="table in tables"
                :key="table.tableCode"
                :label="table.tableName"
                :value="table.tableCode"
              />
            </el-select>
            <el-button type="primary" @click="handleAdd" :disabled="!selectedTableCode">新增字段</el-button>
          </div>
        </div>
      </template>

      <el-table :data="fieldData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="fieldCode" label="字段编码" width="150" />
        <el-table-column prop="fieldName" label="字段名称" />
        <el-table-column prop="fieldType" label="字段类型" width="150" />
        <el-table-column prop="label" label="显示名" width="120" />
        <el-table-column prop="isRequired" label="必填" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isRequired === 1 ? 'success' : 'info'">
              {{ row.isRequired === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="formComponent" label="表单组件" width="120" />
        <el-table-column prop="isEnabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled === 1 ? 'success' : 'danger'">
              {{ row.isEnabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="200" fixed="right">
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
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="字段编码" prop="fieldCode" v-if="!form.id">
          <el-input v-model="form.fieldCode" placeholder="如：FIELD_001" />
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="form.fieldName" placeholder="请输入字段名称" />
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-input v-model="form.fieldType" placeholder="如：varchar(100)" />
        </el-form-item>
        <el-form-item label="显示名" prop="label">
          <el-input v-model="form.label" placeholder="请输入显示名" />
        </el-form-item>
        <el-form-item label="是否必填" prop="isRequired">
          <el-radio-group v-model="form.isRequired">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="表单组件" prop="formComponent">
          <el-select v-model="form.formComponent" placeholder="请选择" style="width: 100%">
            <el-option label="主键字段" value="primary_key" />
            <el-option label="输入框" value="input" />
            <el-option label="下拉框" value="select" />
            <el-option label="日期选择器" value="datepicker" />
            <el-option label="数字输入框" value="number" />
            <el-option label="文本域" value="textarea" />
          </el-select>
        </el-form-item>
        <el-form-item label="校验规则" prop="validateRule">
          <json-editor
            v-model="form.validateRule"
            min-height="60px"
            max-height="200px"
            :options="{
              maxLines: 10,
              minLines: 1
            }"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTableList, getFieldList, addField, updateField, deleteField } from '../api'
import JsonEditor from '../components/JsonEditor'

export default {
  name: 'FieldManage',
  components: {
    JsonEditor
  },
  setup() {
    const tables = ref([])
    const fieldData = ref([])
    const selectedTableCode = ref('')
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增字段')
    const formRef = ref(null)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const form = reactive({
      id: null,
      fieldCode: '',
      tableCode: '',
      fieldName: '',
      fieldType: '',
      label: '',
      isRequired: 0,
      formComponent: 'input',
      validateRule: '',
      sort: 0,
      isEnabled: 1
    })
    
    // 监听字段名称变化，当字段名为'id'或'uuid'时自动设置排序号为0和表单组件为primary_key
    watch(() => form.fieldName, (newValue) => {
      if (newValue === 'id' || newValue === 'uuid') {
        form.sort = 0
        form.formComponent = 'primary_key'
      }
    })
    const rules = {
      fieldCode: [{ required: true, message: '请输入字段编码', trigger: 'blur' }],
      fieldName: [{ required: true, message: '请输入字段名称', trigger: 'blur' }],
      fieldType: [{ required: true, message: '请输入字段类型', trigger: 'blur' }],
      label: [{ required: true, message: '请输入显示名', trigger: 'blur' }],
      formComponent: [{ required: true, message: '请选择表单组件', trigger: 'change' }]
    }

    const loadTables = async () => {
      try {
        const res = await getTableList({})
        if (res.code === 200) {
          // 过滤掉禁用状态的表
          tables.value = res.data.filter(table => table.isEnabled === 1)
        }
      } catch (error) {
        ElMessage.error('加载表列表失败')
      }
    }

    const loadFields = async () => {
      if (!selectedTableCode.value) {
        fieldData.value = []
        pagination.total = 0
        return
      }
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size
        }
        const res = await getFieldList(selectedTableCode.value, params)
        if (res.code === 200) {
          let fields = []
          if (res.data && res.data.records) {
            // 分页数据
            fields = res.data.records
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            fields = res.data || []
            pagination.total = Number(res.data?.length) || 0
          }
          
          // 处理主键字段，确保其必填状态正确显示在表格中
          fieldData.value = fields.map(field => {
            if (field.fieldName === 'id') {
              return { ...field, isRequired: 1 }
            }
            return field
          })
        }
      } catch (error) {
        ElMessage.error('加载字段列表失败')
        fieldData.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadFields()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadFields()
    }

    const handleAdd = () => {
      dialogTitle.value = '新增字段'
      
      // 默认为非主键字段的排序号
      let newSort = 1
      if (fieldData.value && fieldData.value.length > 0) {
        // 找出当前所有字段中的最大排序号
        const existingSorts = fieldData.value
          .map(field => Number(field.sort) || 0)
          .filter(sort => !isNaN(sort))
          
        if (existingSorts.length > 0) {
          newSort = Math.max(...existingSorts) + 1
        }
      }
      
      Object.assign(form, {
        id: null,
        fieldCode: '',
        tableCode: selectedTableCode.value,
        fieldName: '',
        fieldType: '',
        label: '',
        isRequired: 0,
        formComponent: 'input',
        validateRule: '',
        sort: newSort // 默认为计算的排序号
      })
      
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑字段'
      Object.assign(form, {
        id: row.id,
        fieldCode: row.fieldCode,
        tableCode: row.tableCode,
        fieldName: row.fieldName,
        fieldType: row.fieldType,
        label: row.label,
        isRequired: row.isRequired,
        formComponent: row.formComponent,
        validateRule: row.validateRule || '',
        sort: row.sort
      })
      
      // 对于主键字段（字段名为id或uuid），确保设置为必填且设置默认表单组件
      if (row.fieldName === 'id' || row.fieldName === 'uuid') {
        form.isRequired = 1 // 主键字段强制设置为必填
        if (!row.formComponent) {
          form.formComponent = 'primary_key' // 主键字段使用primary_key表单组件
        }
      }
      
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      // 检查是否设置了主键字段（仅基于formComponent判断）
      const isSettingPrimaryKey = form.formComponent === 'primary_key'
      
      // 检查当前是否正在将主键字段修改为非主键字段
      if (form.id) {
        // 获取当前字段的原始数据
        const originalField = fieldData.value.find(field => field.id === form.id)
        const wasPrimaryKey = originalField && originalField.formComponent === 'primary_key'
        
        // 如果是将主键字段修改为非主键字段，检查是否还有其他主键字段
        if (wasPrimaryKey && !isSettingPrimaryKey) {
          // 检查是否还有其他主键字段
          const hasOtherPrimaryKey = fieldData.value.some(field => 
            field.id !== form.id && // 排除当前字段
            field.formComponent === 'primary_key'
          )
          
          if (!hasOtherPrimaryKey) {
            ElMessage.error('当前表必须有且只有一个主键字段，无法将唯一的主键字段修改为非主键字段')
            return
          }
        }
      }
      
      // 如果是设置主键字段，检查当前表是否已经存在主键字段
      if (isSettingPrimaryKey) {
        // 查找当前表中已有的主键字段
        const existingPrimaryKey = fieldData.value.find(field => 
          field.formComponent === 'primary_key' && 
          field.id !== form.id // 排除当前正在编辑的字段
        )
        
        if (existingPrimaryKey) {
          ElMessage.error('当前表已经存在主键字段，每个表只能有一个主键字段')
          return
        }
      }
      
      // 特殊处理主键字段：如果是主键字段（字段名为id）且表单组件为空，则跳过表单组件验证
      let isValid = true
      let errorMessage = ''
      
      if (form.fieldName === 'id') {
        // 对主键字段进行简化验证，跳过表单组件验证
        if (!form.fieldCode) {
          isValid = false
          errorMessage = '请输入字段编码'
        } else if (!form.fieldName) {
          isValid = false
          errorMessage = '请输入字段名称'
        } else if (!form.fieldType) {
          isValid = false
          errorMessage = '请输入字段类型'
        } else if (!form.label) {
          isValid = false
          errorMessage = '请输入显示名'
        }
        
        if (!isValid) {
          ElMessage.error(errorMessage)
          return
        }
      } else {
        // 非主键字段使用正常的表单验证
        await formRef.value.validate(async (valid) => {
          if (valid) {
            try {
              // 处理校验规则：如果是{}，转换为null
              const submitForm = { ...form }
              if (submitForm.validateRule === '{}' || submitForm.validateRule === '{\n}') {
                submitForm.validateRule = null
              }
              
              if (form.id) {
                await updateField(submitForm)
              } else {
                await addField(submitForm)
              }
              ElMessage.success('操作成功')
              dialogVisible.value = false
              loadFields()
            } catch (error) {
              // 显示后端返回的具体错误消息，如果没有则显示通用错误
              const errorMsg = error.response?.data?.message || '操作失败'
              ElMessage.error(errorMsg)
            }
          }
        })
        return
      }
      
      // 主键字段的保存逻辑
      try {
        // 确保主键字段的排序号为0
        const submitForm = { ...form }
        if (form.fieldName === 'id') {
          submitForm.sort = 0
        }
        
        // 处理校验规则：如果是{}，转换为null
        if (submitForm.validateRule === '{}' || submitForm.validateRule === '{\n}') {
          submitForm.validateRule = null
        }
        
        if (form.id) {
          await updateField(submitForm)
        } else {
          await addField(submitForm)
        }
        ElMessage.success('操作成功')
        dialogVisible.value = false
        loadFields()
      } catch (error) {
        // 显示后端返回的具体错误消息，适配多种错误格式
        const errorMsg = error.response?.data?.message || error.data?.message || error.message || '操作失败'
        ElMessage.error(errorMsg)
      }
    }

    const handleDelete = (row) => {
      // 检查是否为唯一的主键字段（仅基于formComponent判断）
      const isPrimaryKey = row.formComponent === 'primary_key'
      
      if (isPrimaryKey) {
        // 检查是否还有其他主键字段
        const hasOtherPrimaryKey = fieldData.value.some(field => 
          field.id !== row.id && // 排除当前字段
          field.formComponent === 'primary_key'
        )
        
        if (!hasOtherPrimaryKey) {
          ElMessage.error('当前表必须有且只有一个主键字段，无法删除唯一的主键字段')
          return
        }
      }
      
      ElMessageBox.confirm('确定要删除该字段吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteField({ id: row.id })
          ElMessage.success('删除成功')
          loadFields()
        } catch (error) {
          // 显示后端返回的具体错误消息，适配多种错误格式
          const errorMsg = error.response?.data?.message || error.data?.message || error.message || '删除失败'
          ElMessage.error(errorMsg)
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleToggleEnable = (row) => {
      const newStatus = row.isEnabled === 1 ? 0 : 1
      const statusText = newStatus === 1 ? '启用' : '禁用'
      
      ElMessageBox.confirm(`确定要${statusText}该字段吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await updateField({
            ...row,
            isEnabled: newStatus
          })
          ElMessage.success(`${statusText}成功`)
          loadFields()
        } catch (error) {
          // 显示后端返回的具体错误消息，适配多种错误格式
          const errorMsg = error.response?.data?.message || error.data?.message || error.message || `${statusText}失败`
          ElMessage.error(errorMsg)
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadTables()
    })

    return {
      tables,
      fieldData,
      selectedTableCode,
      loading,
      dialogVisible,
      dialogTitle,
      formRef,
      pagination,
      form,
      rules,
      loadFields,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleToggleEnable,
      handleDelete,
      handleDialogClose
    }
  }
}
</script>

<style scoped>
.field-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

