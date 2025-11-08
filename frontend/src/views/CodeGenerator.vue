<template>
  <div class="code-generator">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>代码生成器</span>
        </div>
      </template>

      <el-form :inline="true" :model="form" class="search-form">
        <el-form-item label="选择表">
          <el-select v-model="form.tableCode" placeholder="请选择表" style="width: 300px" @change="handleTableChange">
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="包名">
          <el-input v-model="form.packageName" placeholder="如：com.example" style="width: 300px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleGenerateAll">生成所有代码</el-button>
        </el-form-item>
      </el-form>

      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="SQL建表语句" name="sql">
          <div class="code-container">
            <div class="code-header">
              <span>create_table.sql</span>
              <el-button type="primary" size="small" @click="handleGenerate('sql')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('sql')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.sql"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Entity实体类" name="entity">
          <div class="code-container">
            <div class="code-header">
              <span>Entity.java</span>
              <el-button type="primary" size="small" @click="handleGenerate('entity')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('entity')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.entity"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Controller" name="controller">
          <div class="code-container">
            <div class="code-header">
              <span>Controller.java</span>
              <el-button type="primary" size="small" @click="handleGenerate('controller')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('controller')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.controller"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Service" name="service">
          <div class="code-container">
            <div class="code-header">
              <span>Service.java</span>
              <el-button type="primary" size="small" @click="handleGenerate('service')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('service')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.service"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Mapper接口" name="mapper">
          <div class="code-container">
            <div class="code-header">
              <span>Mapper.java</span>
              <el-button type="primary" size="small" @click="handleGenerate('mapper')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('mapper')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.mapper"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Mapper XML" name="mapperxml">
          <div class="code-container">
            <div class="code-header">
              <span>Mapper.xml</span>
              <el-button type="primary" size="small" @click="handleGenerate('mapperxml')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('mapperxml')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.mapperxml"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Vue列表页" name="vueList">
          <div class="code-container">
            <div class="code-header">
              <span>List.vue</span>
              <el-button type="primary" size="small" @click="handleGenerate('vueList')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('vueList')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.vueList"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="Vue表单页" name="vueForm">
          <div class="code-container">
            <div class="code-header">
              <span>Form.vue</span>
              <el-button type="primary" size="small" @click="handleGenerate('vueForm')">生成</el-button>
              <el-button type="success" size="small" @click="handleCopy('vueForm')">复制</el-button>
            </div>
            <el-input
              v-model="codeMap.vueForm"
              type="textarea"
              :rows="15"
              readonly
              class="code-textarea"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getTableList,
  generateSQL,
  generateEntity,
  generateController,
  generateService,
  generateMapper,
  generateMapperXml,
  generateVueList,
  generateVueForm,
  generateAll
} from '../api'

export default {
  name: 'CodeGenerator',
  setup() {
    const tables = ref([])
    const activeTab = ref('sql')
    const form = reactive({
      tableCode: '',
      packageName: 'com.example'
    })
    const codeMap = reactive({
      sql: '',
      entity: '',
      controller: '',
      service: '',
      mapper: '',
      mapperxml: '',
      vueList: '',
      vueForm: ''
    })

    const loadTables = async () => {
      try {
        const res = await getTableList({})
        if (res.code === 200) {
          tables.value = res.data
        }
      } catch (error) {
        ElMessage.error('加载表列表失败')
      }
    }

    const handleTableChange = () => {
      // 清空代码
      Object.keys(codeMap).forEach(key => {
        codeMap[key] = ''
      })
    }

    const handleGenerate = async (type) => {
      if (!form.tableCode) {
        ElMessage.warning('请先选择表')
        return
      }

      try {
        let res
        switch (type) {
          case 'sql':
            res = await generateSQL(form.tableCode)
            if (res.code === 200) {
              codeMap.sql = res.data
            }
            break
          case 'entity':
            res = await generateEntity(form.tableCode, form.packageName)
            if (res.code === 200) {
              codeMap.entity = res.data
            }
            break
          case 'controller':
            res = await generateController(form.tableCode, form.packageName)
            if (res.code === 200) {
              codeMap.controller = res.data
            }
            break
          case 'service':
            res = await generateService(form.tableCode, form.packageName)
            if (res.code === 200) {
              codeMap.service = res.data
            }
            break
          case 'mapper':
            res = await generateMapper(form.tableCode, form.packageName)
            if (res.code === 200) {
              codeMap.mapper = res.data
            }
            break
          case 'mapperxml':
            res = await generateMapperXml(form.tableCode, form.packageName)
            if (res.code === 200) {
              codeMap.mapperxml = res.data
            }
            break
          case 'vueList':
            res = await generateVueList(form.tableCode)
            if (res.code === 200) {
              codeMap.vueList = res.data
            }
            break
          case 'vueForm':
            res = await generateVueForm(form.tableCode)
            if (res.code === 200) {
              codeMap.vueForm = res.data
            }
            break
        }
        ElMessage.success('生成成功')
      } catch (error) {
        ElMessage.error('生成失败：' + (error.message || '未知错误'))
      }
    }

    const handleGenerateAll = async () => {
      if (!form.tableCode) {
        ElMessage.warning('请先选择表')
        return
      }

      try {
        const res = await generateAll(form.tableCode, form.packageName)
        if (res.code === 200 && res.data) {
          const data = res.data
          codeMap.sql = data['create_table.sql'] || ''
          codeMap.entity = data['Entity.java'] || ''
          codeMap.controller = data['Controller.java'] || ''
          codeMap.service = data['Service.java'] || ''
          codeMap.mapper = data['Mapper.java'] || ''
          codeMap.mapperxml = data['Mapper.xml'] || ''
          codeMap.vueList = data['List.vue'] || ''
          codeMap.vueForm = data['Form.vue'] || ''
          ElMessage.success('所有代码生成成功')
        }
      } catch (error) {
        ElMessage.error('生成失败：' + (error.message || '未知错误'))
      }
    }

    const handleCopy = (type) => {
      const text = codeMap[type]
      if (!text) {
        ElMessage.warning('请先生成代码')
        return
      }

      // 复制到剪贴板
      const textarea = document.createElement('textarea')
      textarea.value = text
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        ElMessage.success('复制成功')
      } catch (err) {
        ElMessage.error('复制失败')
      }
      document.body.removeChild(textarea)
    }

    onMounted(() => {
      loadTables()
    })

    return {
      tables,
      activeTab,
      form,
      codeMap,
      handleTableChange,
      handleGenerate,
      handleGenerateAll,
      handleCopy
    }
  }
}
</script>

<style scoped>
.code-generator {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.code-container {
  margin-top: 10px;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.code-header span {
  font-weight: bold;
}

.code-textarea {
  font-family: 'Courier New', monospace;
}

.code-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}
</style>

