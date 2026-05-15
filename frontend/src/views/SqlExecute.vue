<template>
  <div class="sql-execute">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>SQL执行</span>
          <el-alert
            title="提示：支持 SELECT/INSERT/UPDATE、CREATE TABLE、单行 CREATE DATABASE/SCHEMA（自动 utf8mb4）；禁止 DROP/TRUNCATE/DELETE 等危险操作"
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
            <el-button @click="showImportDialog = true">导入SQL</el-button>
          </div>
        </div>

        <!-- SQL导入对话框 -->
        <el-dialog
          v-model="showImportDialog"
          title="导入SQL文件"
          width="600px"
          center
        >
          <el-tabs v-model="activeTab">
            <!-- 本地文件上传 -->
            <el-tab-pane label="本地文件" name="local">
              <div class="upload-container">
                <el-upload
                  ref="uploadRef"
                  :auto-upload="false"
                  :on-change="handleFileChange"
                  accept=".sql"
                  :show-file-list="true"
                  :file-list="fileList"
                  drag
                  style="margin-bottom: 20px"
                >
                  <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                  <div class="el-upload__text">将SQL文件拖到此处，或<em>点击上传</em></div>
                  <template #tip>
                    <div class="el-upload__tip">
                      支持上传 .sql 格式文件，单文件大小不超过 10MB
                    </div>
                  </template>
                </el-upload>
                <div style="text-align: center;">
                  <el-button type="primary" @click="handleImportLocal" :loading="importLoading">开始导入</el-button>
                </div>
              </div>
            </el-tab-pane>
            
            <!-- 网络文件URL -->
            <el-tab-pane label="网络文件" name="remote">
              <div class="remote-container">
                <el-input
                  v-model="remoteUrl"
                  placeholder="请输入SQL文件URL地址"
                  type="textarea"
                  :rows="2"
                  style="margin-bottom: 20px"
                />
                <div style="text-align: center;">
                  <el-button type="primary" @click="handleImportRemote" :loading="importLoading">获取文件</el-button>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-dialog>
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
import { ElMessage, ElLoading } from 'element-plus'
import { InfoFilled, UploadFilled } from '@element-plus/icons-vue'
import * as monaco from 'monaco-editor'
import { executeSql, executeMultipleSql } from '../api'
import axios from 'axios'

export default {
  name: 'SqlExecute',
  components: {
    InfoFilled,
    UploadFilled
  },
  setup() {
    const editorContainer = ref(null)
    let editor = null
    const executing = ref(false)
    const result = ref(null)
    const activeCollapse = ref([])
    
    // 导入相关状态
    const showImportDialog = ref(false)
    const activeTab = ref('local')
    const uploadRef = ref(null)
    const fileList = ref([])
    const remoteUrl = ref('')
    const importLoading = ref(false)

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

    // 导入相关方法
    
    // 文件变更处理
    const handleFileChange = (file, files) => {
      fileList.value = [file]
    }

    // 本地文件导入
    const handleImportLocal = async () => {
      if (fileList.value.length === 0) {
        ElMessage.warning('请先选择一个SQL文件')
        return
      }

      importLoading.value = true
      
      try {
        const fileObj = fileList.value[0]
        // 获取原生File对象
        const file = fileObj.raw || fileObj
        if (!file) {
          ElMessage.error('获取文件对象失败')
          importLoading.value = false
          return
        }
        
        const reader = new FileReader()
        
        reader.onload = (e) => {
          const content = e.target.result
          setSqlText(content)
          showImportDialog.value = false
          ElMessage.success('SQL文件导入成功')
          importLoading.value = false
        }
        
        reader.onerror = () => {
          ElMessage.error('文件读取失败')
          importLoading.value = false
        }
        
        reader.readAsText(file, 'utf-8')
      } catch (error) {
        ElMessage.error('导入失败: ' + error.message)
        importLoading.value = false
      }
    }

    // 网络文件导入
    const handleImportRemote = async () => {
      if (!remoteUrl.value.trim()) {
        ElMessage.warning('请输入SQL文件URL')
        return
      }

      importLoading.value = true
      
      try {
        const response = await axios.get(remoteUrl.value, {
          responseType: 'text',
          timeout: 10000
        })
        
        setSqlText(response.data)
        showImportDialog.value = false
        ElMessage.success('网络SQL文件获取成功')
        importLoading.value = false
      } catch (error) {
        ElMessage.error('获取网络文件失败: ' + (error.message || '未知错误'))
        importLoading.value = false
      }
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
      // 导入相关
      showImportDialog,
      activeTab,
      uploadRef,
      fileList,
      remoteUrl,
      importLoading,
      handleExecute,
      handleFormat,
      handleClear,
      handleFileChange,
      handleImportLocal,
      handleImportRemote
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

