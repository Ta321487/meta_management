<template>
  <div class="operation-log">
    <el-card>
      <template #header>
        <span>操作日志</span>
      </template>

      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="操作类型">
          <el-select v-model="searchForm.operateType" placeholder="请选择" clearable style="width: 200px">
            <el-option label="新增" value="ADD" />
            <el-option label="编辑" value="EDIT" />
            <el-option label="删除" value="DELETE" />
            <el-option label="导出" value="EXPORT" />
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
    const searchForm = reactive({
      operateType: '',
      startTime: '',
      endTime: ''
    })

    // 格式化操作类型显示
    const formatOperateType = (type) => {
      const typeMap = {
        'ADD': '新增',
        'EDIT': '编辑',
        'DELETE': '删除',
        'EXPORT': '导出',
        'LOGIN': '登录',
        'CHANGE_PASSWORD': '修改密码'
      }
      return typeMap[type] || type
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

    const loadData = async () => {
      loading.value = true
      try {
        const params = {}
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
          // 格式化数据
          tableData.value = res.data.map(item => ({
            ...item,
            operateType: formatOperateType(item.operateType),
            operateTime: formatDateTime(item.operateTime)
          }))
        }
      } catch (error) {
        ElMessage.error('加载操作日志失败：' + (error.message || '未知错误'))
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      loadData()
    }

    const handleReset = () => {
      searchForm.operateType = ''
      searchForm.startTime = ''
      searchForm.endTime = ''
      loadData()
    }

    onMounted(() => {
      loadData()
    })

    return {
      tableData,
      loading,
      searchForm,
      handleSearch,
      handleReset
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

