<template>
  <div class="sql-execute">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>SQL执行</span>
          <el-alert
            title="提示：支持执行INSERT、UPDATE、CREATE TABLE等语句，禁止执行DROP、TRUNCATE、DELETE等危险操作"
            type="warning"
            :closable="false"
            style="margin-top: 10px"
          />
        </div>
      </template>

      <div class="sql-editor-container">
        <div class="editor-header">
          <span>SQL语句</span>
          <div>
            <el-button type="primary" @click="handleExecute" :loading="executing">执行</el-button>
            <el-button @click="handleFormat">格式化</el-button>
            <el-button @click="handleClear">清空</el-button>
          </div>
        </div>
        <div class="editor-wrapper">
          <div ref="editorContainer" class="sql-editor"></div>
        </div>
      </div>

      <div class="result-container" v-if="result">
        <div class="result-header">
          <span>执行结果</span>
          <el-tag :type="result.success ? 'success' : 'danger'">
            {{ result.success ? '成功' : '失败' }}
          </el-tag>
        </div>
        
        <div class="result-message" :class="{ 'error': !result.success }">
          {{ result.message }}
        </div>

        <!-- 查询结果表格 -->
        <el-table
          v-if="result.success && result.data && result.data.length > 0"
          :data="result.data"
          border
          style="width: 100%; margin-top: 20px"
          max-height="400"
        >
          <el-table-column
            v-for="(value, key) in result.data[0]"
            :key="key"
            :prop="key"
            :label="key"
            show-overflow-tooltip
          />
        </el-table>

        <!-- 多条SQL执行结果 -->
        <div v-if="result.results && result.results.length > 0" class="multiple-results">
          <el-collapse v-model="activeCollapse">
            <el-collapse-item
              v-for="(item, index) in result.results"
              :key="index"
              :title="`SQL ${index + 1}: ${item.sql.substring(0, 50)}${item.sql.length > 50 ? '...' : ''}`"
              :name="index"
            >
              <div class="sql-result-item">
                <el-tag :type="item.success ? 'success' : 'danger'" style="margin-bottom: 10px">
                  {{ item.success ? '成功' : '失败' }}
                </el-tag>
                <div class="result-message" :class="{ 'error': !item.success }">
                  {{ item.message }}
                </div>
                <div v-if="item.affectedRows !== undefined" class="result-info">
                  影响行数: {{ item.affectedRows }}
                </div>
                <div v-if="item.rowCount !== undefined" class="result-info">
                  查询结果行数: {{ item.rowCount }}
                </div>
                <el-table
                  v-if="item.data && item.data.length > 0"
                  :data="item.data"
                  border
                  style="width: 100%; margin-top: 10px"
                  max-height="300"
                >
                  <el-table-column
                    v-for="(value, key) in item.data[0]"
                    :key="key"
                    :prop="key"
                    :label="key"
                    show-overflow-tooltip
                  />
                </el-table>
              </div>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 单条SQL执行结果信息 -->
        <div v-if="result.success && !result.data && result.affectedRows !== undefined" class="result-info">
          <el-icon><InfoFilled /></el-icon>
          影响行数: {{ result.affectedRows }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import * as monaco from 'monaco-editor'
import { executeSql, executeMultipleSql } from '../api'

export default {
  name: 'SqlExecute',
  components: {
    InfoFilled
  },
  setup() {
    const editorContainer = ref(null)
    let editor = null
    const executing = ref(false)
    const result = ref(null)
    const activeCollapse = ref([])

    // 初始化编辑器
    const initEditor = () => {
      if (!editorContainer.value) return

      editor = monaco.editor.create(editorContainer.value, {
        value: '',
        language: 'sql',
        theme: 'vs',
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        automaticLayout: true,
        lineNumbers: 'on',
        tabSize: 2,
        contextmenu: true,
        locale: 'zh-CN',
        scrollbar: {
          useShadows: false,
          verticalScrollbarSize: 10,
          horizontalScrollbarSize: 10
        },
        placeholder: '请输入SQL语句，多条SQL用分号(;)分隔\n例如：\nINSERT INTO metadata_table (table_code, table_name, pk_strategy, description) VALUES (\'TABLE_001\', \'测试表\', \'AUTO\', \'测试描述\');\nINSERT INTO metadata_field (field_code, table_code, field_name, field_type, label, is_required, form_component, sort) VALUES (\'FIELD_001\', \'TABLE_001\', \'name\', \'varchar(100)\', \'名称\', 1, \'input\', 1);'
      })
    }

    // 获取编辑器内容
    const getSqlText = () => {
      return editor ? editor.getValue() : ''
    }

    // 设置编辑器内容
    const setSqlText = (value) => {
      if (editor) {
        editor.setValue(value)
      }
    }

    // 执行SQL
    const handleExecute = async () => {
      const sqlText = getSqlText()
      if (!sqlText || !sqlText.trim()) {
        ElMessage.warning('请输入SQL语句')
        return
      }

      executing.value = true
      result.value = null

      try {
        // 判断是否包含多条SQL（有分号分隔）
        const sqlCount = sqlText.split(';').filter(s => s.trim().length > 0).length
        
        let response
        if (sqlCount > 1) {
          response = await executeMultipleSql({ sql: sqlText })
        } else {
          response = await executeSql({ sql: sqlText })
        }

        if (response.code === 200) {
          result.value = response.data
          if (result.value.success) {
            ElMessage.success('SQL执行成功')
            // 如果是多条SQL，展开所有结果
            if (result.value.results) {
              activeCollapse.value = result.value.results.map((_, index) => index)
            }
          } else {
            ElMessage.error('SQL执行失败: ' + result.value.message)
          }
        } else {
          ElMessage.error('执行失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('执行失败: ' + (error.message || '未知错误'))
        result.value = {
          success: false,
          message: error.message || '未知错误'
        }
      } finally {
        executing.value = false
      }
    }

    // 格式化SQL
    const handleFormat = () => {
      if (!editor) return

      try {
        const sqlText = getSqlText()
        if (!sqlText.trim()) {
          ElMessage.warning('请先输入SQL语句')
          return
        }

        // 使用monaco-editor的格式化功能
        // 直接触发格式化命令
        editor.getAction('editor.action.formatDocument').run()
        ElMessage.success('SQL格式化成功')
      } catch (error) {
        ElMessage.error('SQL格式化失败: ' + (error.message || '未知错误'))
      }
    }

    // 清空SQL
    const handleClear = () => {
      setSqlText('')
      result.value = null
      activeCollapse.value = []
    }

    // 生命周期钩子
    onMounted(() => {
      initEditor()
    })

    onBeforeUnmount(() => {
      if (editor) {
        editor.dispose()
      }
    })

    return {
      editorContainer,
      executing,
      result,
      activeCollapse,
      handleExecute,
      handleFormat,
      handleClear
    }
  }
}
</script>

<style scoped>
.sql-execute {
  height: 100%;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sql-editor-container {
  margin-bottom: 20px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: bold;
}

.editor-wrapper {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.sql-editor {
  width: 100%;
  min-height: 400px;
  font-family: 'Courier New', monospace;
}

.result-container {
  margin-top: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-weight: bold;
  font-size: 16px;
}

.result-message {
  padding: 10px;
  background-color: #f0f9ff;
  border-left: 4px solid #409eff;
  border-radius: 4px;
  margin-bottom: 10px;
}

.result-message.error {
  background-color: #fef0f0;
  border-left-color: #f56c6c;
  color: #f56c6c;
}

.result-info {
  margin-top: 10px;
  padding: 10px;
  background-color: #fff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.multiple-results {
  margin-top: 20px;
}

.sql-result-item {
  padding: 10px;
}

:deep(.el-collapse-item__header) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}
</style>

