<template>
  <div class="admin-page ${componentName?lower_case}-export">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">${menuTitle!table.tableName}</h2>
        <span class="page-desc">批量导出</span>
      </div>
    </div>

    <el-card shadow="never">
      <el-alert type="info" :closable="false" show-icon class="tip">
        按当前表结构导出全部数据为 CSV（分页拉取后合并下载）。
      </el-alert>
      <el-button type="primary" :loading="exporting" @click="exportAll">导出 CSV</el-button>
      <p v-if="lastCount >= 0" class="hint">上次导出 {{ lastCount }} 条</p>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ${componentName}Api } from '../api'

export default {
  name: '${componentName}Export',
  setup() {
    const exporting = ref(false)
    const lastCount = ref(-1)
    const headers = [
<#list exportFields as field>
      { key: '${field.camelCaseName}', label: '${field.field.label?js_string}' },
</#list>
    ]

    const exportAll = async () => {
      exporting.value = true
      try {
        const res = await ${componentName}Api.page({ page: 1, size: 10000 })
        const rows = (res.code === 200 && res.data && res.data.records) ? res.data.records : []
        const lines = [headers.map(h => h.label).join(',')]
        rows.forEach(r => {
          lines.push(headers.map(h => {
            const v = r[h.key]
            if (v == null) return ''
            const s = String(v).replace(/"/g, '""')
            return s.includes(',') ? '"' + s + '"' : s
          }).join(','))
        })
        const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8;' })
        const a = document.createElement('a')
        a.href = URL.createObjectURL(blob)
        a.download = '${componentName}_export_' + Date.now() + '.csv'
        a.click()
        URL.revokeObjectURL(a.href)
        lastCount.value = rows.length
        ElMessage.success('已导出 ' + rows.length + ' 条')
      } catch (e) {
        ElMessage.error('导出失败')
      } finally {
        exporting.value = false
      }
    }

    return { exporting, lastCount, exportAll }
  }
}
</script>

<style scoped>
.tip { margin-bottom: 16px; }
.hint { margin-top: 12px; color: #909399; font-size: 13px; }
</style>
