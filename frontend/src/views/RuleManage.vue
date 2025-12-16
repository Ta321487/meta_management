<template>
  <div class="rule-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>业务规则管理</span>
          <div>
            <el-select v-model="selectedBusinessCode" placeholder="请选择业务系统" style="width: 200px; margin-right: 10px" @change="handleBusinessSystemChange">
              <el-option
                v-for="business in businessSystems"
                :key="business.businessCode"
                :label="business.businessName"
                :value="business.businessCode"
              />
            </el-select>
            <el-select v-model="selectedModuleCode" placeholder="请选择模块" style="width: 200px; margin-right: 10px" @change="loadRules">
              <el-option
                v-for="module in modules"
                :key="module.moduleCode"
                :label="module.moduleName"
                :value="module.moduleCode"
              />
            </el-select>
            <el-button type="primary" @click="handleAdd" :disabled="!selectedModuleCode">新增规则</el-button>
          </div>
        </div>
      </template>

      <el-table :data="ruleData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="ruleCode" label="规则编码" width="150" />
        <el-table-column prop="ruleType" label="规则类型" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="规则内容" min-width="200">
          <template #default="{ row }">
            <el-button type="text" @click="handlePreview(row)">预览</el-button>
          </template>
        </el-table-column>
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
      width="700px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="规则编码" prop="ruleCode" v-if="!form.id">
          <el-input v-model="form.ruleCode" placeholder="如：RULE_001" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择" style="width: 100%" @change="handleRuleTypeChange">
            <el-option label="验证规则" value="VALIDATION_RULE" />
            <el-option label="搜索规则" value="SEARCH_RULE" />
            <el-option label="显示规则" value="DISPLAY_RULE" />
            <el-option label="流程规则" value="PROCESS_RULE" />
            <el-option label="报表规则" value="REPORT_RULE" />
            <el-option label="批量操作规则" value="BATCH_RULE" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则内容" prop="ruleContent">
          <json-editor
            v-model="form.ruleContent"
            min-height="100px"
            max-height="400px"
            :show-test-and-example="true"
            :rule-type="form.ruleType"
            :options="{
              maxLines: 20,
              minLines: 1
            }"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 预览对话框 -->
  <el-dialog v-model="previewVisible" title="规则预览" width="600px" close-on-click-modal="false" close-on-press-escape="false">
      <pre>{{ previewContent }}</pre>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getModuleList, getRuleList, addRule, updateRule, deleteRule, getBusinessSystemList } from '../api'
import JsonEditor from '../components/JsonEditor'

export default {
  name: 'RuleManage',
  components: {
    JsonEditor
  },
  setup() {
    const businessSystems = ref([])
    const modules = ref([])
    const ruleData = ref([])
    const selectedBusinessCode = ref('')
    const selectedModuleCode = ref('')
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增规则')
    const previewVisible = ref(false)
    const previewContent = ref('')
    const formRef = ref(null)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const form = reactive({
      id: null,
      ruleCode: '',
      moduleCode: '',
      ruleType: '',
      ruleContent: '',
      description: '',
      businessCode: ''
    })
    const rules = {
      ruleCode: [{ required: true, message: '请输入规则编码', trigger: 'blur' }],
      ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
      ruleContent: [{ required: true, message: '请输入规则内容', trigger: 'blur' }]
    }

    const loadBusinessSystems = async () => {
      try {
        const res = await getBusinessSystemList()
        if (res.code === 200) {
          businessSystems.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载业务系统列表失败')
      }
    }

    const loadModules = async () => {
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
      }
    }

    const loadRules = async () => {
      if (!selectedModuleCode.value) {
        ruleData.value = []
        pagination.total = 0
        return
      }
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size,
          businessCode: selectedBusinessCode.value
        }
        const res = await getRuleList(selectedModuleCode.value, params)
        if (res.code === 200) {
          if (res.data && res.data.records) {
            // 分页数据
            ruleData.value = res.data.records
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            ruleData.value = res.data || []
            pagination.total = Number(res.data?.length) || 0
          }
        }
      } catch (error) {
        ElMessage.error('加载规则列表失败')
        ruleData.value = []
        pagination.total = 0
      } finally {
        loading.value = false
      }
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadRules()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadRules()
    }

    const handleAdd = () => {
      dialogTitle.value = '新增规则'
      Object.assign(form, {
        id: null,
        ruleCode: '',
        moduleCode: selectedModuleCode.value,
        ruleType: '',
        ruleContent: '',
        description: '',
        businessCode: selectedBusinessCode.value
      })
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑规则'
      Object.assign(form, {
        id: row.id,
        ruleCode: row.ruleCode,
        moduleCode: row.moduleCode,
        ruleType: row.ruleType,
        ruleContent: row.ruleContent,
        description: row.description || '',
        businessCode: row.businessCode || ''
      })
      dialogVisible.value = true
    }

    const handlePreview = (row) => {
      try {
        const content = JSON.parse(row.ruleContent)
        previewContent.value = JSON.stringify(content, null, 2)
      } catch (error) {
        previewContent.value = row.ruleContent
      }
      previewVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            // 处理规则内容：如果是{}，转换为null
            const submitForm = { ...form }
            if (submitForm.ruleContent === '{}' || submitForm.ruleContent === '{\n}') {
              submitForm.ruleContent = null
            }
            
            if (form.id) {
              await updateRule(submitForm)
            } else {
              await addRule(submitForm)
            }
            ElMessage.success('操作成功')
            dialogVisible.value = false
            loadRules()
          } catch (error) {
            // 显示后端返回的具体错误信息，适配多种错误格式
            ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '操作失败')
          }
        }
      })
    }

    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该规则吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteRule(row.id)
          ElMessage.success('删除成功')
          loadRules()
        } catch (error) {
          // 显示后端返回的具体错误信息，适配多种错误格式
          ElMessage.error(error.response?.data?.message || error.data?.message || error.message || '删除失败')
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
    }

    onMounted(() => {
      loadBusinessSystems()
    })

    // 监听业务系统变化，重新加载模块
    const handleBusinessSystemChange = () => {
      selectedModuleCode.value = ''
      ruleData.value = []
      pagination.total = 0
      loadModules()
    }
    
    // 监听规则类型变化，清空规则内容
    const handleRuleTypeChange = () => {
      form.ruleContent = ''
    }

    return {
      businessSystems,
      modules,
      ruleData,
      selectedBusinessCode,
      selectedModuleCode,
      loading,
      dialogVisible,
      dialogTitle,
      previewVisible,
      previewContent,
      formRef,
      pagination,
      form,
      rules,
      loadRules,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handlePreview,
      handleSubmit,
      handleDelete,
      handleDialogClose,
      handleBusinessSystemChange,
      handleRuleTypeChange
    }
  }
}
</script>

<style scoped>
.rule-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

pre {
  background-color: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}
</style>

