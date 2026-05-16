<template>
  <div class="relation-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表关联关系管理</span>
          <div>
            <el-button type="success" @click="handleSyncForeignKeys" style="margin-right: 10px;">同步外键</el-button>
            <el-button type="primary" @click="handleAdd">新增关联</el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div style="margin-bottom: 15px; display: flex; align-items: center;">
        <el-select 
          v-model="selectedBusinessCode" 
          placeholder="请选择业务系统" 
          style="width: 200px; margin-right: 10px;"
          @change="handleFilter"
        >
          <el-option label="全部" value="" />
          <el-option 
            v-for="system in businessSystems" 
            :key="system.businessCode" 
            :label="system.businessName" 
            :value="system.businessCode" 
          />
        </el-select>
      </div>

      <el-table :data="relationData" border style="width: 100%">
        <el-table-column prop="relationCode" label="关联编码" width="150" />
        <el-table-column prop="relationName" label="关联名称" />
        <el-table-column prop="businessCode" label="业务系统" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.businessCode || '未关联' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="mainTableCode" label="主表" width="150" />
        <el-table-column prop="slaveTableCode" label="从表" width="150" />
        <el-table-column prop="mainFieldCode" label="主表字段" width="150" />
        <el-table-column prop="slaveFieldCode" label="从表字段" width="150" />
        <el-table-column prop="relationType" label="关联类型" width="120">
          <template #default="{ row }">
            <el-tag>
              {{ row.relationType === 'ONE_TO_ONE' ? '一对一' : 
                 row.relationType === 'ONE_TO_MANY' ? '一对多' : 
                 row.relationType === 'MANY_TO_ONE' ? '多对一' : '多对多' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
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
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="120px">
        <el-form-item label="业务系统" prop="businessCode">
          <el-select v-model="form.businessCode" placeholder="请选择业务系统" style="width: 100%" @change="handleBusinessCodeChange">
            <el-option
              v-for="system in businessSystems"
              :key="system.businessCode"
              :label="system.businessName"
              :value="system.businessCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联编码" prop="relationCode" v-if="!form.id">
          <el-input v-model="form.relationCode" placeholder="如：RELATION_001" />
        </el-form-item>
        <el-form-item label="关联名称" prop="relationName">
          <el-input v-model="form.relationName" placeholder="请输入关联名称" />
        </el-form-item>
        <el-form-item label="主表" prop="mainTableCode">
          <el-select v-model="form.mainTableCode" placeholder="请选择主表" style="width: 100%" @change="handleMainTableChange">
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主表关联字段" prop="mainFieldCode">
          <el-select v-model="form.mainFieldCode" placeholder="请选择字段" style="width: 100%">
            <el-option
              v-for="field in mainTableFields"
              :key="field.fieldCode"
              :label="field.fieldName"
              :value="field.fieldCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="从表" prop="slaveTableCode">
          <el-select v-model="form.slaveTableCode" placeholder="请选择从表" style="width: 100%" @change="handleSlaveTableChange">
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="从表外键字段" prop="slaveFieldCode">
          <el-select v-model="form.slaveFieldCode" placeholder="请选择字段" style="width: 100%">
            <el-option
              v-for="field in slaveTableFields"
              :key="field.fieldCode"
              :label="field.fieldName"
              :value="field.fieldCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联类型" prop="relationType">
          <el-select v-model="form.relationType" placeholder="请选择" style="width: 100%">
            <el-option label="一对一" value="ONE_TO_ONE" />
            <el-option label="一对多" value="ONE_TO_MANY" />
            <el-option label="多对一" value="MANY_TO_ONE" />
            <el-option label="多对多" value="MANY_TO_MANY" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联描述" prop="description">
          <el-input v-model="form.description" type="textarea" rows="3" placeholder="请输入关联关系的描述" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="创建外键约束">
          <el-checkbox v-model="form.createForeignKey">同时创建数据库外键约束</el-checkbox>
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
import { getTableList, getFieldList, addRelation, updateRelation, deleteRelation, getAllRelations, createForeignKey, syncForeignKeys, getBusinessSystemList } from '../api'

export default {
  name: 'RelationManage',
  setup() {
    const tables = ref([])
    const businessSystems = ref([])
    const relationData = ref([])
    const mainTableFields = ref([])
    const slaveTableFields = ref([])
    const dialogVisible = ref(false)
    const dialogTitle = ref('新增关联')
    const formRef = ref(null)
    const selectedBusinessCode = ref('') // 选中的业务系统编码
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const form = reactive({
      id: null,
      businessCode: '',
      relationCode: '',
      relationName: '',
      description: '',
      mainTableCode: '',
      slaveTableCode: '',
      mainFieldCode: '',
      slaveFieldCode: '',
      relationType: 'ONE_TO_MANY',
      createForeignKey: false
    })
    const rules = {
      businessCode: [{ required: true, message: '请选择业务系统', trigger: 'change' }],
      relationCode: [{ required: true, message: '请输入关联编码', trigger: 'blur' }],
      relationName: [{ required: true, message: '请输入关联名称', trigger: 'blur' }],
      mainTableCode: [{ required: true, message: '请选择主表', trigger: 'change' }],
      slaveTableCode: [{ required: true, message: '请选择从表', trigger: 'change' }],
      mainFieldCode: [{ required: true, message: '请选择主表字段', trigger: 'change' }],
      slaveFieldCode: [{ required: true, message: '请选择从表字段', trigger: 'change' }],
      relationType: [{ required: true, message: '请选择关联类型', trigger: 'change' }]
    }

    const loadTables = async (businessCode) => {
      try {
        // 使用传入的业务系统编码或表单中的业务系统编码
        const currentBusinessCode = businessCode || form.businessCode
        // 只有选择了业务系统，才加载表列表
        if (currentBusinessCode) {
          console.log('开始加载表列表，业务系统编码：', currentBusinessCode)
          const res = await getTableList({
            businessCode: currentBusinessCode,
            current: null, // 明确不使用分页
            size: null     // 明确不使用分页
          })
          console.log('加载表列表返回结果：', res)
          if (res.code === 200) {
            let tableList = []
            // 检查返回的数据格式，处理分页和非分页情况
            if (res.data && Array.isArray(res.data)) {
              // 非分页数据
              tableList = res.data
            } else if (res.data && res.data.records && Array.isArray(res.data.records)) {
              // 分页数据，取records字段
              tableList = res.data.records
            }
            // 过滤掉禁用状态的表
            tables.value = tableList.filter(table => table.isEnabled === 1)
            console.log('过滤后表列表：', tables.value)
          }
        } else {
          // 未选择业务系统时，清空表列表
          tables.value = []
        }
      } catch (error) {
        console.error('加载表列表失败：', error)
        ElMessage.error('加载表列表失败')
      }
    }

    // 处理业务系统选择变化
    const handleBusinessCodeChange = () => {
      // 清空之前选择的表和字段
      form.mainTableCode = ''
      form.slaveTableCode = ''
      form.mainFieldCode = ''
      form.slaveFieldCode = ''
      mainTableFields.value = []
      slaveTableFields.value = []
      // 加载对应业务系统下的表
      loadTables(form.businessCode)
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

    const loadRelations = async () => {
      try {
        const params = {
          current: pagination.current,
          size: pagination.size,
          businessCode: selectedBusinessCode.value || undefined
        }
        const res = await getAllRelations(params)
        if (res.code === 200) {
          if (res.data && res.data.records) {
            // 分页数据
            relationData.value = res.data.records
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            relationData.value = res.data || []
            pagination.total = Number(res.data?.length) || 0
          }
        }
      } catch (error) {
        ElMessage.error('加载关联关系失败')
        relationData.value = []
        pagination.total = 0
      }
    }

    const handleSizeChange = (val) => {
      pagination.size = val
      pagination.current = 1
      loadRelations()
    }

    const handleCurrentChange = (val) => {
      pagination.current = val
      loadRelations()
    }
    
    // 筛选关联关系
    const handleFilter = () => {
      pagination.current = 1 // 重置页码
      loadRelations() // 重新加载关联关系
      loadTables() // 重新加载表列表
    }
    
    // 重置筛选条件
    const handleReset = () => {
      selectedBusinessCode.value = '' // 重置选中的业务系统
      pagination.current = 1 // 重置页码
      loadRelations() // 重新加载关联关系
      loadTables() // 重新加载表列表
    }

    const handleMainTableChange = async (tableCode) => {
      if (tableCode) {
        try {
          const res = await getFieldList(tableCode)
          if (res.code === 200) {
            mainTableFields.value = res.data
          }
        } catch (error) {
          ElMessage.error('加载字段失败')
        }
      } else {
        mainTableFields.value = []
      }
    }

    const handleSlaveTableChange = async (tableCode) => {
      if (tableCode) {
        try {
          const res = await getFieldList(tableCode)
          if (res.code === 200) {
            slaveTableFields.value = res.data
          }
        } catch (error) {
          ElMessage.error('加载字段失败')
        }
      } else {
        slaveTableFields.value = []
      }
    }

    const handleAdd = () => {
      dialogTitle.value = '新增关联'
      Object.assign(form, {
        id: null,
        businessCode: '',
        relationCode: '',
        relationName: '',
        description: '',
        mainTableCode: '',
        slaveTableCode: '',
        mainFieldCode: '',
        slaveFieldCode: '',
        relationType: 'ONE_TO_MANY',
        createForeignKey: false
      })
      // 清空表列表和字段列表
      tables.value = []
      mainTableFields.value = []
      slaveTableFields.value = []
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      dialogTitle.value = '编辑关联'
      Object.assign(form, {
        id: row.id,
        businessCode: row.businessCode,
        relationCode: row.relationCode,
        relationName: row.relationName,
        description: row.description || '',
        mainTableCode: row.mainTableCode,
        slaveTableCode: row.slaveTableCode,
        mainFieldCode: row.mainFieldCode,
        slaveFieldCode: row.slaveFieldCode,
        relationType: row.relationType
      })
      // 加载对应业务系统下的表
      loadTables(row.businessCode)
      // 加载主表和从表的字段
      handleMainTableChange(row.mainTableCode)
      handleSlaveTableChange(row.slaveTableCode)
      dialogVisible.value = true
    }

    const handleSubmit = async () => {
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.id) {
              await updateRelation(form)
            } else {
              await addRelation(form)
              // 如果选择了创建外键约束，则创建外键
              if (form.createForeignKey) {
                try {
                  await createForeignKey(form)
                  ElMessage.success('关联关系和外键约束创建成功')
                } catch (error) {
                  ElMessage.warning('关联关系创建成功，但外键约束创建失败: ' + (error.response?.data?.message || error.message))
                }
              }
            }
            if (!form.createForeignKey || form.id) {
              ElMessage.success('操作成功')
            }
            dialogVisible.value = false
            loadRelations()
          } catch (error) {
            ElMessage.error('操作失败: ' + (error.response?.data?.message || error.message))
          }
        }
      })
    }


    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该关联关系吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteRelation(row.id)
          ElMessage.success('删除成功')
          loadRelations()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    const handleDialogClose = () => {
      formRef.value?.resetFields()
      tables.value = []
      mainTableFields.value = []
      slaveTableFields.value = []
    }

    const handleSyncForeignKeys = () => {
      ElMessageBox.confirm('确定要从元数据系统同步外键到业务系统吗？这将为您补充缺失的外键约束', '同步外键', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(async () => {
        try {
          const res = await syncForeignKeys(null)
          if (res.code === 200) {
            ElMessage.success(res.message || '同步外键成功')
            loadRelations()
          } else {
            ElMessage.warning(res.message || '同步外键完成，但部分表可能失败')
            loadRelations()
          }
        } catch (error) {
          ElMessage.error('同步外键失败: ' + (error.response?.data?.message || error.message))
        }
      }).catch(() => {
        // 处理用户取消操作，不做任何处理
      })
    }

    // 监听业务系统变化，重新加载表列表
    watch(selectedBusinessCode, () => {
      loadTables()
    })

    onMounted(() => {
      loadTables()
      loadRelations()
      loadBusinessSystems()
    })

    return {
      tables,
      businessSystems,
      relationData,
      mainTableFields,
      slaveTableFields,
      dialogVisible,
      dialogTitle,
      formRef,
      selectedBusinessCode,
      pagination,
      form,
      rules,
      handleBusinessCodeChange,
      handleMainTableChange,
      handleSlaveTableChange,
      handleSizeChange,
      handleCurrentChange,
      handleAdd,
      handleEdit,
      handleSubmit,
      handleDelete,
      handleDialogClose,
      handleSyncForeignKeys,
      handleFilter,
      handleReset
    }
  }
}
</script>

<style scoped>
.relation-manage {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

