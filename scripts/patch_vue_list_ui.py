# -*- coding: utf-8 -*-
from pathlib import Path

path = Path(r"d:\Java\meta-management\src\main\resources\templates\vue_list.vue.ftl")
s = path.read_text(encoding="utf-8")

header_old = """  <div class="${componentName?lower_case}-list">
    <el-card>
      <template #header>
        <motion class="card-header">"""

header_old = header_old.replace("<motion class=\"card-header\">", "<motion class=\"card-header\">")
# EXACT from file:
header_old = """  <div class="${componentName?lower_case}-list">
    <el-card>
      <template #header>
        <div class="card-header">"""

# read from file between markers
start = s.index('  <div class="${componentName?lower_case}-list">')
end = s.index('<#list fields as field>', start)
header_old = s[start:end]

header_new = """  <motion class="admin-page ${componentName?lower_case}-list">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">${table.tableName}</h2>
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
"""

header_new = header_new.replace("<motion class=", "<div class=")

s = s[:start] + header_new + s[end:]

# table block
s = s.replace(
    """      </el-form>

      <el-table 
        :data="tableData" 
        border 
        style="width: 100%"
        v-loading="loading"
        @selection-change="handleSelectionChange"
      >""",
    """      </el-form>
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
      >""",
    1,
)

s = s.replace(
    """            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>""",
    """            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>""",
)

s = s.replace(
    """      <div style="margin-top: 20px; display: flex; justify-content: flex-end;">""",
    """      <div class="table-footer">""",
)

s = s.replace(
    """    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >""",
    """    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="720px"
      class="dialog-form"
      destroy-on-close
      @close="handleDialogClose"
    >""",
)

s = s.replace(
    "import { ref, reactive, onMounted } from 'vue'\nimport { ElMessage, ElMessageBox } from 'element-plus'",
    "import { ref, reactive, onMounted } from 'vue'\nimport { ElMessage, ElMessageBox } from 'element-plus'\nimport { Plus } from '@element-plus/icons-vue'",
)

old_style = """<style scoped>
.${componentName?lower_case}-list {
  height: 100%;
  padding: 20px;
  background-color: #f5f7fa;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}
</style>"""

if old_style in s:
    s = s.replace(old_style, "<style scoped>\n/* 布局见 src/styles/admin.css */\n</style>")

path.write_text(s, encoding="utf-8")
print("patched vue_list")
