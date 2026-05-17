<template>
  <div class="admin-page ${componentName?lower_case}-import">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">${menuTitle!table.tableName}</h2>
        <span class="page-desc">${importPageTitle!""}</span>
      </div>
    </div>

    <el-card shadow="never">
      <el-alert type="info" :closable="false" show-icon class="tip">
        下载 CSV 模板后填写，再上传导入。表头需与模板一致；逐条调用新增接口写入。
      </el-alert>
      <div class="actions">
        <el-button @click="downloadTemplate">下载模板</el-button>
        <el-upload :auto-upload="false" :show-file-list="false" accept=".csv" :on-change="onFileChange">
          <el-button type="primary">选择 CSV 并导入</el-button>
        </el-upload>
      </div>
      <el-table v-if="resultRows.length" :data="resultRows" border size="small" class="result-table">
        <el-table-column prop="line" label="行号" width="70" />
        <el-table-column prop="status" label="结果" width="90" />
        <el-table-column prop="message" label="说明" min-width="200" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ${componentName}Api } from '../api'

export default {
  name: '${componentName}Import',
  setup() {
    const resultRows = ref([])
    const headers = [
<#list importFields as field>
      '${field.camelCaseName}',
</#list>
    ]
    const headerLabels = [
<#list importFields as field>
      '${field.field.label?js_string}',
</#list>
    ]

    const downloadTemplate = () => {
      const csv = headerLabels.join(',') + '\n'
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = '${componentName}_import_template.csv'
      a.click()
      URL.revokeObjectURL(a.href)
    }

    const parseCsv = (text) => {
      const lines = text.split(/\r?\n/).filter(l => l.trim())
      if (lines.length < 2) return []
      const rows = []
      for (let i = 1; i < lines.length; i++) {
        const cols = lines[i].split(',').map(s => s.trim().replace(/^"|"$/g, ''))
        const row = {}
        headers.forEach((h, idx) => { row[h] = cols[idx] ?? '' })
        rows.push(row)
      }
      return rows
    }

    const onFileChange = async (file) => {
      const raw = await file.raw.text()
      const rows = parseCsv(raw)
      if (!rows.length) {
        ElMessage.warning('未解析到数据行')
        return
      }
      resultRows.value = []
      let ok = 0
      for (let i = 0; i < rows.length; i++) {
        const row = rows[i]
        try {
          const res = await ${componentName}Api.add(row)
          if (res.code === 200) {
            ok++
            resultRows.value.push({ line: i + 2, status: '成功', message: '' })
          } else {
            resultRows.value.push({ line: i + 2, status: '失败', message: res.message || '' })
          }
        } catch (e) {
          resultRows.value.push({ line: i + 2, status: '失败', message: e.message || '异常' })
        }
      }
      ElMessage.success('导入完成：成功 ' + ok + ' / ' + rows.length)
    }

    return { resultRows, downloadTemplate, onFileChange }
  }
}
</script>

<style scoped>
.tip { margin-bottom: 16px; }
.actions { display: flex; gap: 12px; margin-bottom: 16px; }
.result-table { margin-top: 16px; }
</style>
