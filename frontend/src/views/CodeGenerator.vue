<template>
  <div class="code-generator">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>代码生成器</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <CodeGeneratorSearch
        :form="form"
        :business-systems="businessSystems"
        :tables="tables"
        @update:form="updateForm"
        @business-system-change="handleBusinessSystemChange"
        @table-change="handleTableChange"
        @generate-code="form.tableCode ? handleGenerateCurrentTable() : handleGenerateAllTables()"
        @run-test="runTest"
        @show-deployment-guide="deploymentGuideVisible = true"
      />

      <!-- 代码标签页容器 -->
      <CodeTabsContainer
        v-model="activeCollapse"
        v-model:active-tab="activeTab"
        :code-map="codeMap"
        :table-code="form.tableCode"
        :business-code="form.businessCode"
        :use-interface="form.useInterface"
        @refresh="handleGenerate"
        @copy="handleCopy"
        @download="handleDownload"
        @preview="handlePreview"
        @generate-integrated="handleGenerateIntegrated"
      />

      <!-- 测试结果卡片 -->
      <TestResult
        v-if="testResult"
        :test-result="testResult"
        @retest="runTest"
      />
    </el-card>

    <!-- 列表页预览 -->
    <ListPreview
      v-model:visible="listPreviewVisible"
      :table-name="listPreviewTableName"
      :fields="listPreviewFields"
      :loading="listPreviewLoading"
    />

    <!-- 表单预览 -->
    <FormPreview
      v-model:visible="formPreviewVisible"
      :table-name="formPreviewTableName"
      :fields="formPreviewFields"
      :loading="formPreviewLoading"
    />

    <!-- 部署指南 -->
    <DeploymentGuide
      v-model:visible="deploymentGuideVisible"
    />
    
    <!-- 登录页预览 -->
    <LoginPreview
      v-model:visible="loginPreviewVisible"
      :login-code="codeMap.login"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, ArrowUp, ArrowDown, Loading, InfoFilled } from '@element-plus/icons-vue'
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
  generateLoginPage,
  generateApi,
  generateRequestJs,
  generateEnvFile,
  generateCorsConfig,
  generateAll,
  generateRoutes,
  generateIntegratedRoutes,
  testCode,
  getFieldList,
  getTableByCode,
  getBusinessSystemList,
  generateAllByBusinessSystem,
  generateAllSQLByBusinessSystem,
  generateResult,
  generatePageRequest,
  generatePageResult,
  generateApplication,
  generateApplicationYml,
  generateMyBatisConfig,
  generatePomXml,
  generateAuth,
  generateCaptchaInput,
  generateAuthExtension,
  getRelationsBySlave
} from '../api'

// 导入子组件
import CodeGeneratorSearch from '../components/CodeGeneratorSearch.vue'
import CodeTabsContainer from '../components/CodeTabsContainer.vue'
import TestResult from '../components/TestResult.vue'
import ListPreview from '../components/ListPreview.vue'
import FormPreview from '../components/FormPreview.vue'
import LoginPreview from '../components/LoginPreview.vue'
import DeploymentGuide from '../components/DeploymentGuide.vue'

// 更新表单数据
const updateForm = (newForm) => {
  Object.assign(form, newForm);
};

// 表单数据
const form = reactive({
  businessCode: '',
  tableCode: '',
  packageName: 'com.example',
  useInterface: false,
  captchaEnabled: false
});

// 业务系统列表
const businessSystems = ref([])
// 表列表
const tables = ref([])
// 活动标签页
const activeTab = ref('sql')
// 活动折叠项
const activeCollapse = ref([])
// 代码映射
const codeMap = reactive({
  sql: '',
  entity: '',
  controller: '',
  service: '',
  serviceInterface: '',
  serviceImpl: '',
  mapper: '',
  mapperxml: '',
  application: '',
  applicationYml: '',
  mybatisConfig: '',
  corsConfig: '',
  pomXml: '',
  vueList: '',
  vueForm: '',
  login: '',
  routes: '',
  api: '',
  requestJs: '',
  auth: '',
  captchaInput: '',
  authController: '',
  captchaService: '',
  env: '',
  result: '',
  pageRequest: '',
  pageResult: ''
})

// 测试结果
const testResult = ref(null)

// 列表页预览相关
const listPreviewVisible = ref(false)
const listPreviewLoading = ref(false)
const listPreviewFields = ref([])
const listPreviewTableName = ref('')

// 表单预览相关
const formPreviewVisible = ref(false)
const formPreviewLoading = ref(false)
const formPreviewFields = ref([])
const formPreviewTableName = ref('')

// 部署指南
const deploymentGuideVisible = ref(false)

// 登录页预览相关
const loginPreviewVisible = ref(false)

// 加载业务系统列表
const loadBusinessSystems = async () => {
  try {
    const res = await getBusinessSystemList()
    if (res.code === 200) {
      businessSystems.value = res.data
    }
  } catch (error) {
    ElMessage.error('加载业务系统列表失败')
  }
}

// 加载表列表
const loadTables = async (businessCode = '') => {
  try {
    if (!businessCode) {
      tables.value = []
      return
    }
    ElMessage.info('正在加载表列表...')
    const res = await getTableList({ businessCode })
    if (res.code === 200) {
      const enabledTables = res.data.filter(table => table.isEnabled === 1)
      tables.value = enabledTables
      ElMessage.success(`成功加载${enabledTables.length}个表`)
    } else {
      ElMessage.error('加载表列表失败：' + res.message)
    }
  } catch (error) {
    ElMessage.error('加载表列表失败：' + (error.message || '未知错误'))
    console.error('加载表列表异常:', error)
  }
}

// 业务系统变化
const handleBusinessSystemChange = (businessCode) => {
  // 清空表选择和代码
  form.tableCode = ''
  Object.keys(codeMap).forEach(key => {
    codeMap[key] = ''
  })
  // 根据业务系统加载表
  loadTables(businessCode)
  // 自动填充包名
  const selectedSystem = businessSystems.value.find(system => system.businessCode === businessCode)
  if (selectedSystem && selectedSystem.packageName) {
    form.packageName = selectedSystem.packageName
  } else {
    form.packageName = 'com.example'
  }
}

// 表变化
const handleTableChange = async (tableCode) => {
  if (!tableCode) {
    Object.keys(codeMap).forEach(key => {
      codeMap[key] = ''
    })
    return
  }
  
  try {
    await handleGenerateCurrentTable()
  } catch (error) {
    ElMessage.error('切换表失败：' + (error.message || '未知错误'))
  }
}

// 复制代码
const handleCopy = async (codeType) => {
  try {
    const text = codeMap[codeType] || ''
    if (!text) {
      ElMessage.warning('没有可复制的代码')
      return
    }
    
    // 使用浏览器的剪贴板API
    await navigator.clipboard.writeText(text)
    ElMessage.success('复制成功')
  } catch (error) {
    ElMessage.error('复制失败：' + (error.message || '未知错误'))
  }
}

// 下载代码
const handleDownload = async (codeType, fileName) => {
  try {
    const text = codeMap[codeType] || ''
    if (!text) {
      ElMessage.warning('没有可下载的代码')
      return
    }
    
    // 创建下载链接
    const blob = new Blob([text], { type: 'text/plain' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName || `${codeType}.txt`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    
    ElMessage.success('下载成功')
  } catch (error) {
    ElMessage.error('下载失败：' + (error.message || '未知错误'))
  }
}

// 预览处理
const handlePreview = (codeType) => {
  if (codeType === 'vueList') {
    handlePreviewList()
  } else if (codeType === 'vueForm') {
    handlePreviewForm()
  } else if (codeType === 'login') {
    handlePreviewLogin()
  }
}

// 预览登录页
const handlePreviewLogin = () => {
  loginPreviewVisible.value = true
}

// 生成整合路由
const handleGenerateIntegrated = async () => {
  try {
    await handleGenerate('integratedRoutes')
    ElMessage.success('生成整合路由成功')
  } catch (error) {
    ElMessage.error('生成整合路由失败：' + (error.message || '未知错误'))
  }
}

// 生成代码
const handleGenerate = async (type) => {
  try {
    let res
    switch (type) {
      case 'sql':
        res = await generateSQL(form.tableCode, form.businessCode)
        if (res.code === 200) {
          codeMap.sql = res.data
        }
        break
      case 'entity':
        res = await generateEntity(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200) {
          codeMap.entity = res.data
        }
        break
      case 'controller':
        res = await generateController(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200) {
          codeMap.controller = res.data
        }
        break
      case 'service':
        res = await generateService(form.tableCode, form.packageName, form.businessCode, form.useInterface)
        if (res.code === 200) {
          codeMap.service = res.data
        }
        break
      case 'service-interface':
        res = await generateServiceInterface(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200) {
          codeMap.serviceInterface = res.data
        }
        break
      case 'service-impl':
        res = await generateServiceImpl(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200) {
          codeMap.serviceImpl = res.data
        }
        break
      case 'mapper':
        res = await generateMapper(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200) {
          codeMap.mapper = res.data
        }
        break
      case 'mapperxml':
        res = await generateMapperXml(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200) {
          codeMap.mapperxml = res.data
        }
        break
      case 'vueList':
        res = await generateVueList(form.tableCode, form.businessCode)
        if (res.code === 200) {
          codeMap.vueList = res.data
        }
        break
      case 'vueForm':
        res = await generateVueForm(form.tableCode, form.businessCode)
        if (res.code === 200) {
          codeMap.vueForm = res.data
        }
        break
      case 'routes':
        res = await generateRoutes(form.tableCode, form.businessCode)
        if (res.code === 200) {
          codeMap.routes = res.data
        }
        break
      case 'integratedRoutes':
        if (!form.businessCode) {
          ElMessage.warning('请先选择业务系统')
          return
        }
        res = await generateIntegratedRoutes(form.businessCode)
        if (res.code === 200) {
          codeMap.routes = res.data
        }
        break
      case 'result':
        let commonPackage1 = form.packageName + '.common'
        res = await generateResult(commonPackage1)
        if (res.code === 200) {
          codeMap.result = res.data
        }
        break
      case 'pageRequest':
        let commonPackage2 = form.packageName + '.common'
        res = await generatePageRequest(commonPackage2)
        if (res.code === 200) {
          codeMap.pageRequest = res.data
        }
        break
      case 'pageResult':
        let commonPackage3 = form.packageName + '.common'
        res = await generatePageResult(commonPackage3)
        if (res.code === 200) {
          codeMap.pageResult = res.data
        }
        break
      case 'application':
        let resApp = await generateApplication(form.packageName)
        if (resApp.code === 200) {
          codeMap.application = resApp.data
        }
        break
      case 'applicationYml':
        let resAppYml = await generateApplicationYml(form.packageName, form.captchaEnabled)
        if (resAppYml.code === 200) {
          codeMap.applicationYml = resAppYml.data
        }
        break
      case 'mybatisConfig':
        let resMyBatisConfig = await generateMyBatisConfig(form.packageName)
        if (resMyBatisConfig.code === 200) {
          codeMap.mybatisConfig = resMyBatisConfig.data
        }
        break
      case 'pomXml':
        let resPomXml = await generatePomXml(form.packageName)
        if (resPomXml.code === 200) {
          codeMap.pomXml = resPomXml.data
        }
        break
      case 'corsConfig':
        let resCorsConfig = await generateCorsConfig(form.packageName)
        if (resCorsConfig.code === 200) {
          codeMap.corsConfig = resCorsConfig.data
        }
        break
      case 'api':
        res = await generateApi(form.tableCode, form.businessCode)
        if (res.code === 200) {
          codeMap.api = res.data
        }
        break
      case 'requestJs':
        let resRequestJs = await generateRequestJs()
        if (resRequestJs.code === 200) {
          codeMap.requestJs = resRequestJs.data
        }
        break
      case 'auth':
        let resAuth = await generateAuth(form.captchaEnabled)
        if (resAuth.code === 200) {
          codeMap.auth = resAuth.data
        }
        break
      case 'captchaInput':
        let resCaptchaInput = await generateCaptchaInput()
        if (resCaptchaInput.code === 200) {
          codeMap.captchaInput = resCaptchaInput.data
        }
        break
      case 'authController':
        let resAuthExt = await generateAuthExtension(form.packageName, form.captchaEnabled)
        if (resAuthExt.code === 200 && resAuthExt.data) {
          codeMap.authController = resAuthExt.data['AuthController.java'] || ''
          codeMap.captchaService = resAuthExt.data['CaptchaService.java'] || ''
        }
        break
      case 'env':
        let resEnv = await generateEnvFile()
        if (resEnv.code === 200) {
          codeMap.env = resEnv.data
        }
        break
      case 'login':
        res = await generateLoginPage(form.businessCode, form.captchaEnabled)
        if (res.code === 200) {
          codeMap.login = res.data
        }
        break
    }
    ElMessage.success('生成成功')
  } catch (error) {
    ElMessage.error('生成失败：' + (error.message || '未知错误'))
  }
}

// 生成当前选中表的代码
const handleGenerateCurrentTable = async () => {
  if (!form.tableCode) {
    ElMessage.warning('请先选择表')
    return
  }

  try {
    const res = await generateAll(form.tableCode, form.packageName, form.businessCode, form.useInterface, form.captchaEnabled)
    if (res.code === 200 && res.data) {
      const data = res.data
      codeMap.sql = data['create_table.sql'] || ''
      codeMap.entity = data['Entity.java'] || ''
      codeMap.controller = data['Controller.java'] || ''
      codeMap.service = data['Service.java'] || ''
      codeMap.serviceInterface = data['ServiceInterface.java'] || ''
      codeMap.serviceImpl = data['ServiceImpl.java'] || ''
      codeMap.mapper = data['Mapper.java'] || ''
      codeMap.application = data['Application.java'] || ''
      codeMap.applicationYml = data['application.yml'] || ''
      codeMap.mybatisConfig = data['MyBatisConfig.java'] || ''
      codeMap.corsConfig = data['CorsConfig.java'] || ''
      codeMap.mapperxml = data['Mapper.xml'] || ''
      codeMap.vueList = data['List.vue'] || ''
      codeMap.vueForm = data['Form.vue'] || ''
      codeMap.login = data['Login.vue'] || ''
      codeMap.routes = data['routes.js'] || data['routes'] || ''
      codeMap.api = data['api.js'] || ''
      codeMap.requestJs = data['request.js'] || ''
      codeMap.env = data['.env'] || ''
      codeMap.result = data['Result.java'] || ''
      codeMap.pageRequest = data['PageRequest.java'] || ''
      codeMap.pageResult = data['PageResult.java'] || ''
      codeMap.pomXml = data['pom.xml'] || ''
      codeMap.auth = data['auth.js'] || codeMap.auth
      codeMap.captchaInput = data['CaptchaInput.vue'] || ''
      codeMap.authController = data['AuthController.java'] || ''
      codeMap.captchaService = data['CaptchaService.java'] || ''
      ElMessage.success('代码生成成功')
    }
  } catch (error) {
    ElMessage.error('生成失败：' + (error.message || '未知错误'))
  }
}

// 生成当前业务系统所有表的代码
const handleGenerateAllTables = async () => {
  if (!form.businessCode) {
    ElMessage.warning('请先选择业务系统')
    return
  }

  try {
    ElMessage.info('正在生成所有表代码，请稍候...')
    
    // 生成业务系统下所有表的SQL
    const sqlRes = await generateAllSQLByBusinessSystem(form.businessCode)
    if (sqlRes.code === 200 && sqlRes.data) {
      const sqlKeys = Object.keys(sqlRes.data)
      
      if (form.tableCode) {
        const targetSqlKey = form.tableCode + '.sql'
        if (targetSqlKey && sqlRes.data[targetSqlKey]) {
          codeMap.sql = sqlRes.data[targetSqlKey]
        }
      } else if (sqlKeys.length > 0) {
        let allSql = ''
        sqlKeys.forEach(sqlKey => {
          allSql += `-- ------------------------------\n`
          allSql += `-- ${sqlKey}\n`
          allSql += `-- ------------------------------\n`
          allSql += sqlRes.data[sqlKey]
          allSql += `\n\n`
        })
        codeMap.sql = allSql
      }
    } else {
      console.error('SQL generation failed:', sqlRes)
      codeMap.sql = '-- 生成SQL失败：' + (sqlRes.message || '未知错误')
    }
    
    // 只有选择了表，才生成其他代码
    if (form.tableCode) {
      const allCodeRes = await generateAllByBusinessSystem(form.businessCode, form.packageName, form.useInterface)
      if (allCodeRes.code === 200 && allCodeRes.data) {
        const tableCodes = Object.keys(allCodeRes.data)
        const targetTableCode = form.tableCode
        
        if (targetTableCode && allCodeRes.data[targetTableCode]) {
          const tableCodeMap = allCodeRes.data[targetTableCode]
          codeMap.entity = tableCodeMap['Entity.java'] || ''
          codeMap.controller = tableCodeMap['Controller.java'] || ''
          codeMap.service = tableCodeMap['Service.java'] || ''
          codeMap.serviceInterface = tableCodeMap['ServiceInterface.java'] || ''
          codeMap.serviceImpl = tableCodeMap['ServiceImpl.java'] || ''
          codeMap.mapper = tableCodeMap['Mapper.java'] || ''
          codeMap.application = tableCodeMap['Application.java'] || ''
          codeMap.applicationYml = tableCodeMap['application.yml'] || ''
          codeMap.mapperxml = tableCodeMap['Mapper.xml'] || ''
          codeMap.vueList = tableCodeMap['List.vue'] || ''
          codeMap.vueForm = tableCodeMap['Form.vue'] || ''
          codeMap.routes = tableCodeMap['routes.js'] || ''
          codeMap.result = tableCodeMap['Result.java'] || ''
          codeMap.pageRequest = tableCodeMap['PageRequest.java'] || ''
          codeMap.pageResult = tableCodeMap['PageResult.java'] || ''
          console.log('Generated code for table:', targetTableCode)
        }
      } else {
        console.error('Code generation failed:', allCodeRes)
        Object.keys(codeMap).forEach(key => {
          if (key !== 'sql') {
            codeMap[key] = ''
          }
        })
      }
    } else {
        Object.keys(codeMap).forEach(key => {
          if (key !== 'sql' && key !== 'result' && key !== 'pageRequest' && key !== 'pageResult') {
            codeMap[key] = ''
          }
        })
      }
    
    ElMessage.success('所有表代码生成成功')
    
  } catch (error) {
    console.error('Generate all tables error:', error)
    ElMessage.error('生成失败：' + (error.message || '未知错误'))
    Object.keys(codeMap).forEach(key => {
      if (key !== 'result' && key !== 'pageRequest' && key !== 'pageResult') {
        codeMap[key] = ''
      }
    })
  }
}

// 运行测试
const runTest = async () => {
  if (!form.tableCode) {
    ElMessage.warning('请先选择表')
    return
  }

  try {
    ElMessage.info('正在测试代码...')
    const res = await testCode(form.tableCode, form.packageName, form.businessCode, form.useInterface, form.captchaEnabled)
    if (res.code === 200) {
      testResult.value = res.data
      if (res.data.success) {
        ElMessage.success('所有测试通过！')
      } else {
        ElMessage.warning(res.data.message)
      }
    }
  } catch (error) {
    ElMessage.error('测试失败：' + (error.message || '未知错误'))
  }
}

// 预览列表页样式
const handlePreviewList = async () => {
  if (!form.tableCode) {
    ElMessage.warning('请先选择表')
    return
  }
  
  listPreviewVisible.value = true
  listPreviewLoading.value = true
  listPreviewFields.value = []
  listPreviewTableName.value = ''
  
  try {
    // 获取表信息
    const tableRes = await getTableByCode(form.tableCode)
    if (tableRes.code === 200 && tableRes.data) {
      listPreviewTableName.value = tableRes.data.tableName || ''
    }
    
    // 获取字段信息
    const fieldRes = await getFieldList(form.tableCode)
    if (fieldRes.code === 200) {
      const fields = Array.isArray(fieldRes.data) ? fieldRes.data : (fieldRes.data?.records || [])
      // 过滤掉主键字段
      listPreviewFields.value = fields.filter(f => f.formComponent !== 'primary_key')
    }
  } catch (error) {
    console.log('加载字段信息失败:', error)
    ElMessage.error('加载字段信息失败')
  } finally {
    listPreviewLoading.value = false
  }
}

// 预览表单样式
const handlePreviewForm = async () => {
  if (!form.tableCode) {
    ElMessage.warning('请先选择表')
    return
  }
  
  formPreviewVisible.value = true
  formPreviewLoading.value = true
  formPreviewFields.value = []
  formPreviewTableName.value = ''
  
  try {
    // 获取表信息
    const tableRes = await getTableByCode(form.tableCode)
    if (tableRes.code === 200 && tableRes.data) {
      formPreviewTableName.value = tableRes.data.tableName || ''
    }
    
    // 获取字段信息
    const fieldRes = await getFieldList(form.tableCode)
    if (fieldRes.code === 200) {
      let fields = Array.isArray(fieldRes.data) ? fieldRes.data : (fieldRes.data?.records || [])
      
      // 获取表关联关系
      const relationRes = await getRelationsBySlave(form.tableCode, form.businessCode)
      console.log('relationRes:', relationRes)
      if (relationRes.code === 200 && relationRes.data && relationRes.data.length > 0) {
        const relations = relationRes.data
        console.log('relations:', relations)
        // 标记外键字段
        fields = fields.map(field => {
          console.log('Processing field:', field.fieldName)
          const relation = relations.find(r => r.slaveFieldCode.toLowerCase() === field.fieldName.toLowerCase())
          console.log('Found relation:', relation)
          if (relation) {
            return {
              ...field,
              isForeignKey: true,
              relation,
              relatedTableName: relation.mainTableCode,
              relatedTableFieldName: relation.mainFieldCode,
              relatedTableClassName: relation.mainTableCode.replace(/_TABLE$/, '').split('_').map(word => word.charAt(0).toUpperCase() + word.slice(1)).join(''),
              relatedTableCamelCaseName: relation.mainTableCode.replace(/_TABLE$/, '').split('_').map((word, index) => index === 0 ? word.toLowerCase() : word.charAt(0).toUpperCase() + word.slice(1)).join('')
            }
          }
          return {
            ...field,
            isForeignKey: false
          }
        })
      } else {
        console.log('No relations found')
        // 没有关联关系，所有字段都不是外键
        fields = fields.map(field => ({
          ...field,
          isForeignKey: false
        }))
      }
      console.log('Final fields:', fields)
      
      formPreviewFields.value = fields
    }
  } catch (error) {
    console.log('加载字段信息失败:', error)
    ElMessage.error('加载字段信息失败')
  } finally {
    formPreviewLoading.value = false
  }
}

// 初始化
onMounted(() => {
  // 加载业务系统列表
  loadBusinessSystems()
})
</script>

<style scoped>
.code-generator {
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.code-actions {
  display: flex;
  gap: 8px;
}

.nested-tabs {
  margin-top: 20px;
}

.code-container {
  margin-bottom: 20px;
}

.code-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.5;
}

.test-card {
  margin-top: 20px;
}

.test-success {
  color: #67c23a;
  font-weight: 500;
}

.test-error {
  color: #f56c6c;
  font-weight: 500;
}

.search-form {
  margin-bottom: 20px;
}
</style>
