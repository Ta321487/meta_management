<template>
  <div class="operation-log">
    <el-card>
      <template #header>
        <span>操作日志</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="所属模块">
          <el-select v-model="searchForm.module" placeholder="请选择" clearable style="width: 200px">
            <el-option label="业务系统管理" value="business-system" />
            <el-option label="模块类型管理" value="module-type" />
            <el-option label="模块管理" value="module" />
            <el-option label="表管理" value="table" />
            <el-option label="字段管理" value="field" />
            <el-option label="功能节点" value="node" />
            <el-option label="表关联" value="relation" />
            <el-option label="业务规则" value="rule" />
            <el-option label="代码生成" value="codegen" />
            <el-option label="SQL执行" value="sql" />
            <el-option label="操作日志" value="operation-log" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operateType" placeholder="请选择" clearable style="width: 200px">
            <el-option label="新增" value="ADD" />
            <el-option label="编辑" value="EDIT" />
            <el-option label="删除" value="DELETE" />
            <el-option label="导出" value="EXPORT" />
            <el-option label="登录" value="LOGIN" />
            <el-option label="修改密码" value="CHANGE_PASSWORD" />
            <el-option label="关联" value="ASSOCIATE" />
            <el-option label="同步" value="SYNC" />
            <el-option label="批量删除" value="BATCH_DELETE" />
            <el-option label="生成SQL" value="GENERATE_SQL" />
            <el-option label="执行SQL" value="EXECUTE_SQL" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="searchForm.startTime" type="datetime" placeholder="选择开始时间" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="searchForm.endTime" type="datetime" placeholder="选择结束时间" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border style="width: 100%" v-loading="loading">
        <el-table-column prop="operateUser" label="操作人" width="120" />
        <el-table-column prop="operateType" label="操作类型" width="100" />
        <el-table-column prop="operateContent" label="操作内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="operateTime" label="操作时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" show-overflow-tooltip />
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
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogList } from '../api'

export default {
  name: 'OperationLog',
  setup() {
    const tableData = ref([])
    const loading = ref(false)
    const pagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const searchForm = reactive({
      module: '',
      operateType: '',
      startTime: '',
      endTime: ''
    })

    // 操作类型映射表
    const operateTypeMap = {
      // 基础操作
      'ADD': '新增',
      'EDIT': '编辑',
      'DELETE': '删除',
      'EXPORT': '导出',
      'LOGIN': '登录',
      'CHANGE_PASSWORD': '修改密码',
      'ASSOCIATE': '关联',
      'BATCH_DELETE': '批量删除',
      'GENERATE_SQL': '生成SQL',
      'EXECUTE_SQL': '执行SQL',
      // 外键操作
      'CREATE_FOREIGN_KEY': '创建外键',
      'DROP_FOREIGN_KEY': '删除外键',
      'SYNC_FOREIGN_KEY': '同步外键',
      'FIND_FOREIGN_KEY': '查找外键',
      'UPDATE_FOREIGN_KEY': '更新外键',
      // 同步操作
      'SYNC': '同步',
      'SYNC_COMPLETE': '同步完成',
      'SYNC_TABLE_FIELDS': '同步表字段',
      'SYNC_TABLE_FIELDS_END': '表字段同步完成',
      'SYNC_FIELDS': '同步字段',
      'SYNC_FIELDS_END': '字段同步完成',
      'SYNC_TABLE_NAME': '同步表名',
      // 字段操作
      'SET_ENUM_TYPE': '设置枚举类型',
      'SET_FORM_COMPONENT': '设置表单组件',
      'AUTO_CREATE_PK_FIELD': '自动创建主键字段',
      // 表操作
      'ALTER_TABLE_ADD_COLUMN': '添加表字段',
      'ALTER_TABLE_DROP_COLUMN': '删除表字段',
      'ALTER_TABLE_OPERATION': '修改表操作',
      'DROP_TABLE': '删除表',
      'TABLE_NAME_MATCH': '表名匹配',
      // 约束操作
      'UPDATE_CHECK_CONSTRAINT': '更新检查约束',
      'DELETE_CONSTRAINT': '删除约束',
      'GET_CHECK_CONSTRAINTS': '获取检查约束',
      'PARSE_CHECK_CONSTRAINT': '解析检查约束',
      // SQL操作
      'SQL_EXECUTE': '执行SQL'
    }

    // 格式化操作类型显示
    const formatOperateType = (type) => {
      return operateTypeMap[type] || type
    }

    // 格式化时间显示
    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }

    // 格式化日期时间为字符串
    const formatDateToString = (date) => {
      if (!date) return ''
      const d = new Date(date)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      const hours = String(d.getHours()).padStart(2, '0')
      const minutes = String(d.getMinutes()).padStart(2, '0')
      const seconds = String(d.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }
    
    // 格式化操作内容显示
    const formatOperateContent = (content) => {
      if (!content) return ''
      
      // 处理外键操作
      if (content.includes('FOREIGN_KEY') || content.includes('外键')) {
        if (content.startsWith('CREATE_FOREIGN_KEY')) {
          return content.replace(/^CREATE_FOREIGN_KEY/i, '创建外键约束:')
        } else if (content.startsWith('DROP FOREIGN KEY')) {
          return content.replace(/^DROP FOREIGN KEY/i, '删除外键约束:')
        } else if (content.startsWith('SYNC_FOREIGN_KEY')) {
          return content.replace(/^SYNC_FOREIGN_KEY/i, '同步外键关系:')
        } else if (content.startsWith('FIND_FOREIGN_KEY')) {
          return content.replace(/^FIND_FOREIGN_KEY/i, '查找外键约束:')
        } else if (content.startsWith('UPDATE_FOREIGN_KEY')) {
          return content.replace(/^UPDATE_FOREIGN_KEY/i, '更新外键约束:')
        }
      }
      
      // 处理同步操作
      if (content.includes('SYNC_') || content.includes('同步')) {
        if (content.startsWith('SYNC_COMPLETE')) {
          return content.replace(/^SYNC_COMPLETE/i, '同步完成:')
        } else if (content.startsWith('SYNC_TABLE_FIELDS')) {
          return content.replace(/^SYNC_TABLE_FIELDS/i, '同步表字段:')
        } else if (content.startsWith('SYNC_TABLE_FIELDS_END')) {
          return content.replace(/^SYNC_TABLE_FIELDS_END/i, '表字段同步完成:')
        } else if (content.startsWith('SYNC_FIELDS')) {
          return content.replace(/^SYNC_FIELDS/i, '同步字段:')
        } else if (content.startsWith('SYNC_FIELDS_END')) {
          return content.replace(/^SYNC_FIELDS_END/i, '字段同步完成:')
        } else if (content.startsWith('SYNC_TABLE_NAME')) {
          return content.replace(/^SYNC_TABLE_NAME/i, '同步表名:')
        } else if (content.startsWith('SYNC_')) {
          return content.replace(/^SYNC_/i, '同步:').replace(/_/g, ' ')
        }
      }
      
      // 处理ALTER TABLE操作
      if (content.includes('ALTER TABLE') || content.includes('ALTER_TABLE')) {
        if (content.startsWith('ALTER_TABLE_ADD_COLUMN')) {
          return content.replace(/^ALTER_TABLE_ADD_COLUMN/i, '添加表字段:')
        } else if (content.startsWith('ALTER_TABLE_DROP_COLUMN')) {
          return content.replace(/^ALTER_TABLE_DROP_COLUMN/i, '删除表字段:')
        } else if (content.startsWith('ALTER_TABLE_OPERATION')) {
          return content.replace(/^ALTER_TABLE_OPERATION/i, '修改表操作:')
        } else if (content.startsWith('ALTER ')) {
          return content.replace(/^ALTER /i, '修改:')
        }
      }
      
      // 处理约束操作
      if (content.includes('CONSTRAINT') || content.includes('约束')) {
        if (content.startsWith('UPDATE_CHECK_CONSTRAINT')) {
          return content.replace(/^UPDATE_CHECK_CONSTRAINT/i, '更新检查约束:')
        } else if (content.startsWith('DELETE_CONSTRAINT')) {
          return content.replace(/^DELETE_CONSTRAINT/i, '删除约束:')
        } else if (content.startsWith('GET_CHECK_CONSTRAINTS')) {
          return content.replace(/^GET_CHECK_CONSTRAINTS/i, '获取检查约束:')
        } else if (content.startsWith('PARSE_CHECK_CONSTRAINT')) {
          return content.replace(/^PARSE_CHECK_CONSTRAINT/i, '解析检查约束:')
        }
      }
      
      // 处理表操作
      if (content.startsWith('TABLE_NAME_MATCH')) {
        return content.replace(/^TABLE_NAME_MATCH/i, '表名匹配:')
      } else if (content.startsWith('DROP_TABLE')) {
        return content.replace(/^DROP_TABLE/i, '删除表:')
      }
      
      // 处理字段操作
      if (content.startsWith('SET_ENUM_TYPE')) {
        return content.replace(/^SET_ENUM_TYPE/i, '设置枚举类型:')
      } else if (content.startsWith('SET_FORM_COMPONENT')) {
        return content.replace(/^SET_FORM_COMPONENT/i, '设置表单组件:')
      } else if (content.startsWith('AUTO_CREATE_PK_FIELD')) {
        return content.replace(/^AUTO_CREATE_PK_FIELD/i, '自动创建主键字段:')
      }
      
      // 处理SQL操作
      if (content.startsWith('SQL_EXECUTE')) {
        return content.replace(/^SQL_EXECUTE/i, '执行SQL:')
      }
      
      // 处理表操作
      if (content.startsWith('TABLE_')) {
        return content.replace(/^TABLE_/i, '表:').replace(/_/g, ' ')
      }
      
      // 处理字段操作
      if (content.startsWith('FIELD_')) {
        return content.replace(/^FIELD_/i, '字段:').replace(/_/g, ' ')
      }
      
      // 处理其他操作
      return content
    }

    const loadData = async () => {
      loading.value = true
      try {
        const params = {
          current: pagination.current,
          size: pagination.size
        }
        if (searchForm.module) {
          params.module = searchForm.module
        }
        if (searchForm.operateType) {
          params.operateType = searchForm.operateType
        }
        if (searchForm.startTime) {
          params.startTime = formatDateToString(searchForm.startTime)
        }
        if (searchForm.endTime) {
          params.endTime = formatDateToString(searchForm.endTime)
        }
        
        const res = await getOperationLogList(params)
        if (res.code === 200) {
          if (res.data && res.data.records) {
            // 分页数据
            tableData.value = res.data.records.map(item => ({
              ...item,
              operateType: formatOperateType(item.operateType),
              operateContent: formatOperateContent(item.operateContent),
              operateTime: formatDateTime(item.operateTime)
            }))
            pagination.total = Number(res.data.total) || 0
          } else {
            // 兼容旧接口（非分页数据）
            tableData.value = (res.data || []).map(item => ({
              ...item,
              operateType: formatOperateType(item.operateType),
              operateContent: formatOperateContent(item.operateContent),
              operateTime: formatDateTime(item.operateTime)
            }))
            pagination.total = Number(res.data?.length) || 0
          }
        }
      } catch (error) {
        ElMessage.error('加载操作日志失败：' + (error.message || '未知错误'))
        tableData.value = []
        pagination.total = 0
      } finally {
        loading.value = false
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

    const handleSearch = () => {
      pagination.current = 1
      loadData()
    }

    const handleReset = () => {
      searchForm.module = ''
      searchForm.operateType = ''
      searchForm.startTime = ''
      searchForm.endTime = ''
      pagination.current = 1
      loadData()
    }

    onMounted(() => {
      loadData()
    })

    return {
      tableData,
      loading,
      searchForm,
      pagination,
      handleSearch,
      handleReset,
      handleSizeChange,
      handleCurrentChange
    }
  }
}
</script>

<style scoped>
.operation-log {
  height: 100%;
}

.search-form {
  margin-bottom: 20px;
}
</style>

