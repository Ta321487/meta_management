<template>
  <div class="operation-log">
    <el-card>
      <template #header>
        <span>操作日志</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="所属模块">
          <el-select v-model="searchForm.module" placeholder="全部模块" clearable style="width: 180px">
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
            <el-option label="登录与账号" value="operation-log" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operateType" placeholder="全部类型" clearable filterable style="width: 180px">
            <el-option
              v-for="item in operateTypeFilterOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="searchForm.operateTimeRange"
            type="daterange"
            unlink-panels
            range-separator="~"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            :shortcuts="operateTimeShortcuts"
            clearable
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" border stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="operateUser" label="操作人" width="100" show-overflow-tooltip />
        <el-table-column prop="operateType" label="操作类型" width="130" show-overflow-tooltip />
        <el-table-column prop="operateContent" label="操作内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="operateTime" label="操作时间" width="170" align="center" />
        <el-table-column prop="status" label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="错误信息" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="row.errorMsg">{{ row.errorMsg }}</span>
            <span v-else class="cell-placeholder">—</span>
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
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationLogList } from '../api'

/** 操作类型 → 中文（与后端 operate_type 字段一致） */
const OPERATE_TYPE_LABELS = {
  ADD: '新增',
  EDIT: '编辑',
  DELETE: '删除',
  BATCH_EDIT: '批量编辑',
  BATCH_DELETE: '批量删除',
  EXPORT: '导出',
  LOGIN: '登录',
  CHANGE_PASSWORD: '修改密码',
  ASSOCIATE: '关联',
  GENERATE_SQL: '生成SQL',
  SQL_EXECUTE: '执行SQL',
  SQL_QUERY: 'SQL查询',
  SYNC: '同步',
  SYNC_START: '开始同步',
  SYNC_COMPLETE: '同步完成',
  SYNC_FAILED: '同步失败',
  SYNC_EXCEPTION: '同步异常',
  SYNC_TABLE: '同步表',
  SYNC_TABLE_NAME: '同步表名',
  SYNC_TABLE_FIELDS_START: '开始同步表字段',
  SYNC_TABLE_FIELDS_END: '表字段同步完成',
  SYNC_TABLE_FIELDS: '同步表字段',
  SYNC_FIELDS: '同步字段',
  SYNC_FIELDS_END: '字段同步完成',
  CREATE_TABLE_SQL: '创建表SQL',
  DROP_TABLE: '删除表',
  TABLE_NAME_MATCH: '表名匹配',
  CREATE_FOREIGN_KEY: '创建外键',
  DROP_FOREIGN_KEY: '删除外键',
  SYNC_FOREIGN_KEY: '同步外键',
  FIND_FOREIGN_KEY: '查找外键',
  UPDATE_FOREIGN_KEY: '更新外键',
  ALTER_TABLE_ADD_COLUMN: '添加表字段',
  ALTER_TABLE_DROP_COLUMN: '删除表字段',
  ALTER_TABLE_OPERATION: '修改表结构',
  UPDATE_CHECK_CONSTRAINT: '更新检查约束',
  UPDATE_PRIMARY_KEY_FIELD: '更新主键字段',
  DELETE_CONSTRAINT: '删除约束',
  DELETE_BUSINESS_RULE: '删除业务规则',
  GET_CHECK_CONSTRAINTS: '获取检查约束',
  PARSE_CHECK_CONSTRAINT: '解析检查约束',
  GET_PRIMARY_KEY: '获取主键',
  SET_ENUM_TYPE: '设置枚举类型',
  SET_FORM_COMPONENT: '设置表单组件',
  SET_VALIDATE_RULE: '设置校验规则',
  AUTO_CREATE_PK_FIELD: '自动创建主键字段',
  UPDATE_TABLE_COMMENT: '更新表注释',
  CLEAN_FIELDS: '清理表字段',
  PARSE_ENUM: '解析枚举',
  EXTRACT_FIELD_NAME: '提取字段名'
}

/** 枚举片段兜底翻译（未在映射表中的组合类型） */
const TYPE_TOKEN_LABELS = {
  SYNC: '同步',
  TABLE: '表',
  FIELDS: '字段',
  FIELD: '字段',
  START: '开始',
  END: '完成',
  FAILED: '失败',
  EXCEPTION: '异常',
  SQL: 'SQL',
  EXECUTE: '执行',
  QUERY: '查询',
  FOREIGN: '外键',
  KEY: '键',
  CONSTRAINT: '约束',
  CHECK: '检查',
  PRIMARY: '主键',
  CREATE: '创建',
  DROP: '删除',
  ALTER: '修改',
  COLUMN: '字段',
  NAME: '名',
  MATCH: '匹配',
  COMPLETE: '完成',
  PARSE: '解析',
  GET: '获取',
  SET: '设置',
  ENUM: '枚举',
  FORM: '表单',
  COMPONENT: '组件',
  VALIDATE: '校验',
  RULE: '规则',
  AUTO: '自动',
  PK: '主键',
  UPDATE: '更新',
  DELETE: '删除',
  ADD: '新增',
  EDIT: '编辑',
  LOGIN: '登录',
  PASSWORD: '密码',
  CHANGE: '修改',
  BUSINESS: '业务',
  BATCH: '批量',
  GENERATE: '生成',
  EXPORT: '导出',
  ASSOCIATE: '关联'
}

const operateTypeFilterOptions = Object.entries(OPERATE_TYPE_LABELS).map(([value, label]) => ({
  value,
  label
}))

/** 按表格「操作时间」列筛选：选一个日期区间，查询 operate_time 落在区间内的记录 */
const operateTimeShortcuts = [
  {
    text: '今天',
    value: () => {
      const d = new Date()
      return [d, d]
    }
  },
  {
    text: '近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end]
    }
  },
  {
    text: '近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 29)
      return [start, end]
    }
  }
]

const buildOperateTimeQuery = (range) => {
  if (!range || range.length !== 2) return null
  const [from, to] = range
  return {
    startTime: `${from} 00:00:00`,
    endTime: `${to} 23:59:59`
  }
}

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
      operateTimeRange: null
    })

    const formatOperateType = (type) => {
      if (!type) return ''
      if (type === 'EXECUTE_SQL') return '执行SQL'
      if (OPERATE_TYPE_LABELS[type]) return OPERATE_TYPE_LABELS[type]
      if (/[\u4e00-\u9fa5]/.test(type)) return type
      const parts = type.split('_').filter(Boolean)
      if (!parts.length) return type
      const translated = parts.map((part) => TYPE_TOKEN_LABELS[part] || part)
      if (translated.some((p, i) => p !== parts[i])) {
        return translated.join('')
      }
      return type
    }

    const formatDateTime = (dateTime) => {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      if (Number.isNaN(date.getTime())) return String(dateTime)
      const year = date.getFullYear()
      const month = String(date.getMonth() + 1).padStart(2, '0')
      const day = String(date.getDate()).padStart(2, '0')
      const hours = String(date.getHours()).padStart(2, '0')
      const minutes = String(date.getMinutes()).padStart(2, '0')
      const seconds = String(date.getSeconds()).padStart(2, '0')
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
    }

    const formatOperateContent = (content) => {
      if (!content) return ''
      if (/[\u4e00-\u9fa5]/.test(content) && !/^[A-Z][A-Z0-9_]*[:：]/.test(content)) {
        return content
      }

      const prefixRules = [
        [/^CREATE_FOREIGN_KEY/i, '创建外键约束：'],
        [/^DROP FOREIGN KEY/i, '删除外键约束：'],
        [/^DROP_FOREIGN_KEY/i, '删除外键约束：'],
        [/^SYNC_FOREIGN_KEY/i, '同步外键关系：'],
        [/^FIND_FOREIGN_KEY/i, '查找外键约束：'],
        [/^UPDATE_FOREIGN_KEY/i, '更新外键约束：'],
        [/^SYNC_COMPLETE/i, '同步完成：'],
        [/^SYNC_TABLE_FIELDS_END/i, '表字段同步完成：'],
        [/^SYNC_TABLE_FIELDS_START/i, '开始同步表字段：'],
        [/^SYNC_TABLE_FIELDS/i, '同步表字段：'],
        [/^SYNC_FIELDS_END/i, '字段同步完成：'],
        [/^SYNC_FIELDS/i, '同步字段：'],
        [/^SYNC_TABLE_NAME/i, '同步表名：'],
        [/^SYNC_TABLE/i, '同步表：'],
        [/^SYNC_START/i, '开始同步：'],
        [/^SYNC_FAILED/i, '同步失败：'],
        [/^ALTER_TABLE_ADD_COLUMN/i, '添加表字段：'],
        [/^ALTER_TABLE_DROP_COLUMN/i, '删除表字段：'],
        [/^ALTER_TABLE_OPERATION/i, '修改表结构：'],
        [/^ALTER /i, '修改：'],
        [/^UPDATE_CHECK_CONSTRAINT/i, '更新检查约束：'],
        [/^DELETE_CONSTRAINT/i, '删除约束：'],
        [/^GET_CHECK_CONSTRAINTS/i, '获取检查约束：'],
        [/^PARSE_CHECK_CONSTRAINT/i, '解析检查约束：'],
        [/^TABLE_NAME_MATCH/i, '表名匹配：'],
        [/^DROP_TABLE/i, '删除表：'],
        [/^SET_ENUM_TYPE/i, '设置枚举类型：'],
        [/^SET_FORM_COMPONENT/i, '设置表单组件：'],
        [/^AUTO_CREATE_PK_FIELD/i, '自动创建主键字段：'],
        [/^SQL_EXECUTE/i, '执行SQL：']
      ]
      for (const [pattern, replacement] of prefixRules) {
        if (pattern.test(content)) {
          return content.replace(pattern, replacement)
        }
      }
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
        const operateTimeQuery = buildOperateTimeQuery(searchForm.operateTimeRange)
        if (operateTimeQuery) {
          params.startTime = operateTimeQuery.startTime
          params.endTime = operateTimeQuery.endTime
        }

        const res = await getOperationLogList(params)
        if (res.code === 200) {
          const mapRow = (item) => ({
            ...item,
            operateType: formatOperateType(item.operateType),
            operateContent: formatOperateContent(item.operateContent),
            operateTime: formatDateTime(item.operateTime)
          })
          if (res.data?.records) {
            tableData.value = res.data.records.map(mapRow)
            pagination.total = Number(res.data.total) || 0
          } else {
            tableData.value = (res.data || []).map(mapRow)
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
      searchForm.operateTimeRange = null
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
      operateTypeFilterOptions,
      operateTimeShortcuts,
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

.search-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.cell-placeholder {
  color: var(--el-text-color-placeholder);
}
</style>
