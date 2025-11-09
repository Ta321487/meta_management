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
          <el-button type="success" @click="runTest" :disabled="!form.tableCode">测试代码</el-button>
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
              <div>
                <el-button type="primary" size="small" @click="handleGenerate('vueList')">生成</el-button>
                <el-button type="success" size="small" @click="handleCopy('vueList')">复制</el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  @click="handlePreviewList"
                  :disabled="!form.tableCode"
                >
                  预览样式
                </el-button>
              </div>
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
              <div>
                <el-button type="primary" size="small" @click="handleGenerate('vueForm')">生成</el-button>
                <el-button type="success" size="small" @click="handleCopy('vueForm')">复制</el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  @click="handlePreviewForm"
                  :disabled="!form.tableCode"
                >
                  预览样式
                </el-button>
              </div>
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

      <!-- 测试结果卡片 -->
      <el-card class="test-card" v-if="testResult" style="margin-top: 20px">
        <template #header>
          <div class="card-header">
            <span>代码测试结果</span>
            <div>
              <el-button 
                type="text" 
                size="small" 
                @click="toggleTestArea"
                style="margin-right: 10px; padding: 0">
                <el-icon style="vertical-align: middle">
                  <ArrowUp v-if="testAreaExpanded" />
                  <ArrowDown v-else />
                </el-icon>
                <span style="margin-left: 5px">{{ testAreaExpanded ? '折叠' : '展开' }}</span>
              </el-button>
              <el-button type="primary" size="small" @click="runTest">重新测试</el-button>
            </div>
          </div>
        </template>
        
        <div v-show="testAreaExpanded">
          <div v-if="testResult.success" class="test-success">
            <el-icon><Check /></el-icon>
            <span>所有测试通过！</span>
          </div>
          <div v-else class="test-error">
            <el-icon><Close /></el-icon>
            <span>部分测试失败</span>
          </div>

          <div style="margin-top: 10px; color: #909399; font-size: 14px">
            总计: {{ testResult.total }} | 
            通过: <span style="color: #67c23a">{{ testResult.successCount }}</span> | 
            失败: <span style="color: #f56c6c">{{ testResult.failCount }}</span>
          </div>

          <el-divider />

          <el-collapse v-model="activeTestItems">
          <el-collapse-item 
            v-for="(test, index) in testResult.testResults" 
            :key="index"
            :title="test.name + ' - ' + test.type"
            :name="index">
            <div class="test-detail">
              <el-tag :type="test.status === 'success' ? 'success' : test.status === 'warning' ? 'warning' : 'danger'" style="margin-bottom: 10px">
                {{ test.status === 'success' ? '通过' : test.status === 'warning' ? '警告' : '失败' }}
              </el-tag>
              <p style="margin: 10px 0">{{ test.message }}</p>
              
              <!-- 显示错误 -->
              <div v-if="test.errors && test.errors.length > 0" class="test-errors">
                <h4 style="color: #f56c6c; margin: 10px 0 5px 0">错误：</h4>
                <ul style="margin: 0; padding-left: 20px">
                  <li v-for="(error, i) in test.errors" :key="i" style="margin: 5px 0">{{ error }}</li>
                </ul>
              </div>
              
              <!-- 显示警告 -->
              <div v-if="test.warnings && test.warnings.length > 0" class="test-warnings">
                <h4 style="color: #e6a23c; margin: 10px 0 5px 0">警告：</h4>
                <ul style="margin: 0; padding-left: 20px">
                  <li v-for="(warning, i) in test.warnings" :key="i" style="margin: 5px 0">{{ warning }}</li>
                </ul>
              </div>
              
              <!-- API测试详情 -->
              <div v-if="test.apiTests" class="api-tests" style="margin-top: 15px">
                <h4 style="margin: 10px 0 5px 0">API接口测试：</h4>
                <el-table :data="test.apiTests" size="small" border style="margin-top: 10px">
                  <el-table-column prop="method" label="方法" width="80" />
                  <el-table-column prop="path" label="路径" />
                  <el-table-column prop="name" label="名称" />
                  <el-table-column label="状态" width="100">
                    <template #default="scope">
                      <el-tag :type="scope.row.status === 'success' ? 'success' : 'danger'">
                        {{ scope.row.status === 'success' ? '✓' : '✗' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
        </div>
      </el-card>

      <!-- 列表页预览对话框 -->
      <el-dialog
        v-model="listPreviewVisible"
        title="列表页样式预览"
        width="1200px"
        :close-on-click-modal="false"
      >
        <div v-if="listPreviewLoading" style="text-align: center; padding: 40px;">
          <el-icon class="is-loading"><Loading /></el-icon>
          <p>加载中...</p>
        </div>
        <div v-else-if="listPreviewFields.length === 0" style="text-align: center; padding: 40px; color: #909399;">
          <p>该表没有配置字段或字段信息加载失败</p>
        </div>
        <el-card v-else>
          <template #header>
            <div class="card-header">
              <span>{{ listPreviewTableName }}</span>
              <div>
                <el-button 
                  type="danger" 
                  :disabled="listPreviewMultipleSelection.length === 0" 
                  size="small" 
                  style="margin-right: 10px"
                  @click="handleListPreviewBatchDelete"
                >
                  批量删除
                </el-button>
                <el-button type="primary" size="small" @click="handleListPreviewAdd">新增</el-button>
              </div>
            </div>
          </template>
          
          <!-- 搜索表单 -->
          <el-form :model="listPreviewSearchForm" :inline="true" class="search-form" style="margin-bottom: 20px; padding: 20px; background-color: #f5f7fa; border-radius: 4px;">
            <el-form-item 
              v-for="field in listPreviewSearchFields" 
              :key="field.id"
              :label="field.label"
            >
              <el-input 
                v-if="field.formComponent === 'input'"
                v-model="listPreviewSearchForm[getListFieldPropName(field)]" 
                :placeholder="`请输入${field.label}`" 
                clearable 
                style="width: 180px"
              />
              <el-select 
                v-else-if="field.formComponent === 'select'"
                v-model="listPreviewSearchForm[getListFieldPropName(field)]" 
                :placeholder="`请选择${field.label}`" 
                clearable 
                style="width: 180px"
              >
                <el-option 
                  v-for="option in getFormFieldOptions(field)"
                  :key="option.value"
                  :label="option.label" 
                  :value="option.value" 
                />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="small" @click="handleListPreviewSearch">搜索</el-button>
              <el-button size="small" @click="handleListPreviewReset">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 数据表格 -->
          <el-table 
            :data="listPreviewTableData" 
            border 
            style="width: 100%"
            v-loading="listPreviewTableLoading"
            @selection-change="handleListPreviewSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column 
              v-for="field in listPreviewFields" 
              :key="field.id"
              :prop="getListFieldPropName(field)" 
              :label="field.label"
              sortable="custom"
              @sort-change="(sort) => handleListPreviewSortChange(getListFieldPropName(field), sort)"
            >
              <template #default="{ row }">
                <span v-if="field.fieldType?.includes('date') || field.fieldType?.includes('time')">
                  {{ formatPreviewDate(row[getListFieldPropName(field)]) }}
                </span>
                <span v-else-if="field.fieldType?.includes('decimal') || field.fieldType?.includes('numeric')">
                  {{ formatPreviewNumber(row[getListFieldPropName(field)]) }}
                </span>
                <span v-else>{{ row[getListFieldPropName(field)] }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="handleListPreviewEdit(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="handleListPreviewDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div style="margin-top: 20px; display: flex; justify-content: flex-end">
            <el-pagination
              v-model:current-page="listPreviewPagination.current"
              v-model:page-size="listPreviewPagination.size"
              :page-sizes="[10, 20, 50, 100]"
              :total="listPreviewPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleListPreviewSizeChange"
              @current-change="handleListPreviewCurrentChange"
            />
          </div>
        </el-card>

        <!-- 新增/编辑对话框 -->
        <el-dialog
          v-model="listPreviewDialogVisible"
          :title="listPreviewDialogTitle"
          width="600px"
          @close="handleListPreviewDialogClose"
        >
          <el-form 
            :model="listPreviewForm" 
            :rules="listPreviewFormRules" 
            ref="listPreviewFormRef" 
            label-width="100px"
          >
            <el-form-item 
              v-for="field in listPreviewFields" 
              :key="field.id"
              :label="field.label" 
              :prop="getListFieldPropName(field)"
              :required="field.isRequired === 1"
            >
              <!-- 输入框 -->
              <el-input 
                v-if="field.formComponent === 'input'"
                v-model="listPreviewForm[getListFieldPropName(field)]" 
                :placeholder="`请输入${field.label}`"
              />
              <!-- 下拉选择 -->
              <el-select 
                v-else-if="field.formComponent === 'select'"
                v-model="listPreviewForm[getListFieldPropName(field)]" 
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option 
                  v-for="option in getFormFieldOptions(field)"
                  :key="option.value"
                  :label="option.label" 
                  :value="option.value" 
                />
              </el-select>
              <!-- 日期选择器 -->
              <el-date-picker 
                v-else-if="field.formComponent === 'datepicker' || field.formComponent === 'date'"
                v-model="listPreviewForm[getListFieldPropName(field)]" 
                type="date" 
                placeholder="请选择日期" 
                style="width: 100%" 
              />
              <!-- 数字输入框 -->
              <el-input-number 
                v-else-if="field.formComponent === 'number'"
                v-model="listPreviewForm[getListFieldPropName(field)]" 
                style="width: 100%" 
              />
              <!-- 文本域 -->
              <el-input 
                v-else-if="field.formComponent === 'textarea'"
                v-model="listPreviewForm[getListFieldPropName(field)]" 
                type="textarea" 
                :rows="3"
              />
              <!-- 默认输入框 -->
              <el-input 
                v-else
                v-model="listPreviewForm[getListFieldPropName(field)]" 
                :placeholder="`请输入${field.label}`"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="listPreviewDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleListPreviewSubmit">确定</el-button>
          </template>
        </el-dialog>

        <template #footer>
          <el-button @click="listPreviewVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 表单预览对话框 -->
      <el-dialog
        v-model="formPreviewVisible"
        title="表单样式预览"
        width="900px"
        :close-on-click-modal="false"
      >
        <div v-if="formPreviewLoading" style="text-align: center; padding: 40px;">
          <el-icon class="is-loading"><Loading /></el-icon>
          <p>加载中...</p>
        </div>
        <div v-else-if="formPreviewFields.length === 0" style="text-align: center; padding: 40px; color: #909399;">
          <p>该表没有配置字段或字段信息加载失败</p>
        </div>
        <el-card v-else>
          <template #header>
            <div class="card-header">
              <span>{{ formPreviewTableName }}表单</span>
            </div>
          </template>
          <el-form 
            :model="formPreviewData" 
            :rules="formPreviewRules" 
            ref="formPreviewRef" 
            label-width="100px"
          >
            <el-form-item 
              v-for="field in formPreviewFields" 
              :key="field.id"
              :label="field.label" 
              :prop="getFormFieldPropName(field)"
              :required="field.isRequired === 1"
            >
              <!-- 输入框 -->
              <el-input 
                v-if="field.formComponent === 'input'"
                v-model="formPreviewData[getFormFieldPropName(field)]" 
                :placeholder="`请输入${field.label}`"
              />
              <!-- 下拉选择 -->
              <el-select 
                v-else-if="field.formComponent === 'select'"
                v-model="formPreviewData[getFormFieldPropName(field)]" 
                placeholder="请选择"
                style="width: 100%"
              >
                <el-option 
                  v-for="option in getFormFieldOptions(field)"
                  :key="option.value"
                  :label="option.label" 
                  :value="option.value" 
                />
              </el-select>
              <!-- 日期选择器 -->
              <el-date-picker 
                v-else-if="field.formComponent === 'datepicker' || field.formComponent === 'date'"
                v-model="formPreviewData[getFormFieldPropName(field)]" 
                type="date"
                placeholder="请选择日期"
                style="width: 100%"
              />
              <!-- 数字输入框 -->
              <el-input-number 
                v-else-if="field.formComponent === 'number'"
                v-model="formPreviewData[getFormFieldPropName(field)]" 
                style="width: 100%"
              />
              <!-- 文本域 -->
              <el-input 
                v-else-if="field.formComponent === 'textarea'"
                v-model="formPreviewData[getFormFieldPropName(field)]" 
                type="textarea" 
                :rows="3"
              />
              <!-- 默认输入框 -->
              <el-input 
                v-else
                v-model="formPreviewData[getFormFieldPropName(field)]" 
                :placeholder="`请输入${field.label}`"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary">保存</el-button>
              <el-button @click="handleFormPreviewReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
        <template #footer>
          <el-button @click="formPreviewVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Check, Close, ArrowUp, ArrowDown, Loading } from '@element-plus/icons-vue'
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
  generateAll,
  testCode,
  getFieldList,
  getTableByCode
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
    const testResult = ref(null)
    const activeTestItems = ref([])
    const testAreaExpanded = ref(true) // 测试区域默认展开

    // 列表页预览相关
    const listPreviewVisible = ref(false)
    const listPreviewLoading = ref(false)
    const listPreviewTableLoading = ref(false)
    const listPreviewFields = ref([])
    const listPreviewSearchFields = ref([])
    const listPreviewTableData = ref([])
    const listPreviewSearchForm = reactive({})
    const listPreviewMultipleSelection = ref([])
    const listPreviewPagination = reactive({
      current: 1,
      size: 10,
      total: 0
    })
    const listPreviewTableName = ref('')
    const listPreviewDialogVisible = ref(false)
    const listPreviewDialogTitle = ref('新增')
    const listPreviewForm = reactive({})
    const listPreviewFormRules = reactive({})
    const listPreviewFormRef = ref(null)
    const listPreviewSortParams = reactive({
      orderBy: '',
      orderDirection: 'DESC'
    })

    // 表单预览相关
    const formPreviewVisible = ref(false)
    const formPreviewLoading = ref(false)
    const formPreviewFields = ref([])
    const formPreviewData = reactive({})
    const formPreviewRules = reactive({})
    const formPreviewRef = ref(null)
    const formPreviewTableName = ref('')

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
          // 生成成功后自动运行测试
          await runTest()
        }
      } catch (error) {
        ElMessage.error('生成失败：' + (error.message || '未知错误'))
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
        const res = await testCode(form.tableCode, form.packageName)
        if (res.code === 200) {
          testResult.value = res.data
          // 默认展开所有测试项和测试区域
          activeTestItems.value = res.data.testResults.map((_, index) => index)
          testAreaExpanded.value = true
          
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

    // 切换测试区域折叠/展开
    const toggleTestArea = () => {
      testAreaExpanded.value = !testAreaExpanded.value
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

    // 预览列表页样式
    const handlePreviewList = async () => {
      if (!form.tableCode) {
        ElMessage.warning('请先选择表')
        return
      }
      
      listPreviewVisible.value = true
      listPreviewLoading.value = true
      listPreviewFields.value = []
      listPreviewSearchFields.value = []
      listPreviewTableData.value = []
      Object.keys(listPreviewSearchForm).forEach(key => delete listPreviewSearchForm[key])
      listPreviewMultipleSelection.value = []
      listPreviewPagination.current = 1
      listPreviewPagination.size = 10
      listPreviewPagination.total = 0
      
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
          const filteredFields = fields.filter(f => f.fieldName !== 'id' && f.fieldName !== 'ID')
          listPreviewFields.value = filteredFields
          
          // 搜索字段：只包含input和select类型的字段
          listPreviewSearchFields.value = filteredFields.filter(f => 
            f.formComponent === 'input' || f.formComponent === 'select'
          )
          
          // 初始化搜索表单
          listPreviewSearchFields.value.forEach(field => {
            const propName = getListFieldPropName(field)
            listPreviewSearchForm[propName] = ''
          })
          
          // 初始化表单校验规则
          filteredFields.forEach(field => {
            const propName = getListFieldPropName(field)
            const rules = []
            
            if (field.isRequired === 1) {
              rules.push({
                required: true,
                message: `请输入${field.label}`,
                trigger: 'blur'
              })
            }
            
            // 解析校验规则
            if (field.validateRule) {
              try {
                const validateRule = JSON.parse(field.validateRule)
                if (validateRule.pattern) {
                  rules.push({
                    pattern: new RegExp(validateRule.pattern),
                    message: validateRule.message || '格式不正确',
                    trigger: 'blur'
                  })
                }
              } catch (e) {
                // 忽略解析错误
              }
            }
            
            if (rules.length > 0) {
              listPreviewFormRules[propName] = rules
            }
          })

          // 生成示例数据（3条），为每条数据添加临时ID
          const mockData = generateMockTableData(filteredFields, 3)
          mockData.forEach((row, index) => {
            row._tempId = `temp_${Date.now()}_${index}`
          })
          listPreviewTableData.value = mockData
          listPreviewPagination.total = 3
        }
      } catch (error) {
        ElMessage.error('加载字段信息失败')
      } finally {
        listPreviewLoading.value = false
      }
    }

    // 获取列表字段属性名（转换为驼峰命名）
    const getListFieldPropName = (field) => {
      const name = field.fieldName || ''
      return name.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
    }

    // 生成模拟表格数据
    const generateMockTableData = (fields, count) => {
      const data = []
      for (let i = 1; i <= count; i++) {
        const row = {}
        fields.forEach(field => {
          const propName = getListFieldPropName(field)
          if (field.fieldType?.includes('int') || field.fieldType?.includes('decimal') || field.fieldType?.includes('numeric')) {
            row[propName] = i * 10
          } else if (field.fieldType?.includes('date') || field.fieldType?.includes('time')) {
            row[propName] = new Date().toISOString().split('T')[0]
          } else if (field.formComponent === 'select') {
            const options = getFormFieldOptions(field)
            row[propName] = options.length > 0 ? options[0].value : '选项' + i
          } else {
            row[propName] = `${field.label}示例${i}`
          }
        })
        data.push(row)
      }
      return data
    }

    // 列表预览选择变化
    const handleListPreviewSelectionChange = (selection) => {
      listPreviewMultipleSelection.value = selection
    }

    // 重置列表预览搜索表单
    const handleListPreviewReset = () => {
      Object.keys(listPreviewSearchForm).forEach(key => {
        listPreviewSearchForm[key] = ''
      })
      listPreviewSortParams.orderBy = ''
      listPreviewSortParams.orderDirection = 'DESC'
      listPreviewPagination.current = 1
      ElMessage.success('已重置搜索条件')
      // 模拟重新加载数据
      listPreviewTableLoading.value = true
      setTimeout(() => {
        listPreviewTableLoading.value = false
      }, 500)
    }

    // 列表预览搜索
    const handleListPreviewSearch = () => {
      listPreviewPagination.current = 1
      ElMessage.success('搜索功能（预览模式）')
      // 模拟搜索
      listPreviewTableLoading.value = true
      setTimeout(() => {
        listPreviewTableLoading.value = false
      }, 500)
    }

    // 列表预览排序变化
    const handleListPreviewSortChange = (prop, sort) => {
      if (sort.order) {
        listPreviewSortParams.orderBy = prop
        listPreviewSortParams.orderDirection = sort.order === 'ascending' ? 'ASC' : 'DESC'
        ElMessage.success(`按${prop}${sort.order === 'ascending' ? '升序' : '降序'}排序（预览模式）`)
      } else {
        listPreviewSortParams.orderBy = ''
        listPreviewSortParams.orderDirection = 'DESC'
      }
      listPreviewPagination.current = 1
      // 模拟重新加载
      listPreviewTableLoading.value = true
      setTimeout(() => {
        listPreviewTableLoading.value = false
      }, 300)
    }

    // 列表预览分页大小变化
    const handleListPreviewSizeChange = (val) => {
      listPreviewPagination.size = val
      listPreviewPagination.current = 1
      ElMessage.success(`每页显示${val}条（预览模式）`)
      // 模拟重新加载
      listPreviewTableLoading.value = true
      setTimeout(() => {
        listPreviewTableLoading.value = false
      }, 300)
    }

    // 列表预览当前页变化
    const handleListPreviewCurrentChange = (val) => {
      listPreviewPagination.current = val
      ElMessage.success(`跳转到第${val}页（预览模式）`)
      // 模拟重新加载
      listPreviewTableLoading.value = true
      setTimeout(() => {
        listPreviewTableLoading.value = false
      }, 300)
    }

    // 列表预览新增
    const handleListPreviewAdd = () => {
      listPreviewDialogTitle.value = '新增'
      // 清理表单数据
      Object.keys(listPreviewForm).forEach(key => {
        if (key !== '_tempId') {
          delete listPreviewForm[key]
        }
      })
      delete listPreviewForm._tempId
      // 初始化表单数据
      listPreviewFields.value.forEach(field => {
        const propName = getListFieldPropName(field)
        if (field.fieldType?.includes('int') || field.fieldType?.includes('decimal') || field.fieldType?.includes('numeric')) {
          listPreviewForm[propName] = null
        } else if (field.fieldType?.includes('date') || field.fieldType?.includes('time')) {
          listPreviewForm[propName] = null
        } else {
          listPreviewForm[propName] = ''
        }
      })
      listPreviewDialogVisible.value = true
    }

    // 列表预览编辑
    const handleListPreviewEdit = (row) => {
      listPreviewDialogTitle.value = '编辑'
      // 保存当前编辑行的临时ID
      listPreviewForm._tempId = row._tempId
      // 复制行数据到表单
      listPreviewFields.value.forEach(field => {
        const propName = getListFieldPropName(field)
        listPreviewForm[propName] = row[propName]
      })
      listPreviewDialogVisible.value = true
    }

    // 列表预览删除
    const handleListPreviewDelete = (row) => {
      ElMessageBox.confirm('确定要删除该记录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        ElMessage.success('删除成功（预览模式）')
        // 从表格数据中移除
        const index = listPreviewTableData.value.findIndex(item => item === row)
        if (index > -1) {
          listPreviewTableData.value.splice(index, 1)
          listPreviewPagination.total = Math.max(0, listPreviewPagination.total - 1)
        }
      }).catch(() => {
        // 取消删除
      })
    }

    // 列表预览批量删除
    const handleListPreviewBatchDelete = () => {
      if (listPreviewMultipleSelection.value.length === 0) {
        ElMessage.warning('请选择要删除的记录')
        return
      }
      ElMessageBox.confirm(`确定要删除选中的 ${listPreviewMultipleSelection.value.length} 条记录吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        ElMessage.success('批量删除成功（预览模式）')
        // 从表格数据中移除选中的项
        listPreviewMultipleSelection.value.forEach(selected => {
          const index = listPreviewTableData.value.findIndex(item => item === selected)
          if (index > -1) {
            listPreviewTableData.value.splice(index, 1)
          }
        })
        listPreviewPagination.total = Math.max(0, listPreviewPagination.total - listPreviewMultipleSelection.value.length)
        listPreviewMultipleSelection.value = []
      }).catch(() => {
        // 取消删除
      })
    }

    // 列表预览表单提交
    const handleListPreviewSubmit = () => {
      listPreviewFormRef.value?.validate((valid) => {
        if (valid) {
          ElMessage.success(`${listPreviewDialogTitle.value}成功（预览模式）`)
          listPreviewDialogVisible.value = false
          // 如果是新增，添加到表格数据
          if (listPreviewDialogTitle.value === '新增') {
            const newRow = { ...listPreviewForm }
            // 移除临时ID字段（如果存在）
            delete newRow._tempId
            // 添加新的临时ID
            newRow._tempId = `temp_${Date.now()}_${listPreviewTableData.value.length}`
            listPreviewTableData.value.push(newRow)
            listPreviewPagination.total = listPreviewTableData.value.length
          } else {
            // 如果是编辑，更新表格数据
            const index = listPreviewTableData.value.findIndex(item => item._tempId === listPreviewForm._tempId)
            if (index > -1) {
              const tempId = listPreviewForm._tempId
              Object.assign(listPreviewTableData.value[index], { ...listPreviewForm })
              listPreviewTableData.value[index]._tempId = tempId
            }
          }
          // 清理表单中的临时ID
          delete listPreviewForm._tempId
        }
      })
    }

    // 列表预览对话框关闭
    const handleListPreviewDialogClose = () => {
      listPreviewFormRef.value?.resetFields()
    }

    // 格式化预览日期
    const formatPreviewDate = (value) => {
      if (!value) return ''
      if (typeof value === 'string') return value
      if (value instanceof Date) {
        return value.toISOString().split('T')[0]
      }
      return value
    }

    // 格式化预览数字
    const formatPreviewNumber = (value) => {
      if (value === null || value === undefined) return ''
      return String(value)
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
      Object.keys(formPreviewData).forEach(key => delete formPreviewData[key])
      Object.keys(formPreviewRules).forEach(key => delete formPreviewRules[key])
      
      try {
        // 获取表信息
        const tableRes = await getTableByCode(form.tableCode)
        if (tableRes.code === 200 && tableRes.data) {
          formPreviewTableName.value = tableRes.data.tableName || ''
        }
        
        // 获取字段信息
        const fieldRes = await getFieldList(form.tableCode)
        if (fieldRes.code === 200) {
          const fields = Array.isArray(fieldRes.data) ? fieldRes.data : (fieldRes.data?.records || [])
          // 过滤掉主键字段
          formPreviewFields.value = fields.filter(f => f.fieldName !== 'id' && f.fieldName !== 'ID')
          
          // 初始化表单数据
          formPreviewFields.value.forEach(field => {
            const propName = getFormFieldPropName(field)
            if (field.fieldType?.includes('int') || field.fieldType?.includes('decimal') || field.fieldType?.includes('numeric')) {
              formPreviewData[propName] = null
            } else if (field.fieldType?.includes('date') || field.fieldType?.includes('time')) {
              formPreviewData[propName] = null
            } else {
              formPreviewData[propName] = ''
            }
          })
          
          // 初始化校验规则
          formPreviewFields.value.forEach(field => {
            const propName = getFormFieldPropName(field)
            const rules = []
            
            if (field.isRequired === 1) {
              rules.push({
                required: true,
                message: `请输入${field.label}`,
                trigger: 'blur'
              })
            }
            
            // 解析校验规则
            if (field.validateRule) {
              try {
                const validateRule = JSON.parse(field.validateRule)
                if (validateRule.pattern) {
                  rules.push({
                    pattern: new RegExp(validateRule.pattern),
                    message: validateRule.message || '格式不正确',
                    trigger: 'blur'
                  })
                }
              } catch (e) {
                // 忽略解析错误
              }
            }
            
            if (rules.length > 0) {
              formPreviewRules[propName] = rules
            }
          })
        }
      } catch (error) {
        ElMessage.error('加载字段信息失败')
      } finally {
        formPreviewLoading.value = false
      }
    }

    // 获取字段属性名（转换为驼峰命名）
    const getFormFieldPropName = (field) => {
      const name = field.fieldName || ''
      return name.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase())
    }

    // 获取字段选项（从校验规则中解析）
    const getFormFieldOptions = (field) => {
      if (!field.validateRule) {
        return [{ label: '选项1', value: '1' }]
      }
      
      try {
        const validateRule = JSON.parse(field.validateRule)
        if (validateRule.options && Array.isArray(validateRule.options)) {
          return validateRule.options.map(opt => {
            if (typeof opt === 'string') {
              return { label: opt, value: opt }
            } else {
              return { label: opt.label || opt.value, value: opt.value || opt }
            }
          })
        }
      } catch (e) {
        // 忽略解析错误
      }
      
      return [{ label: '选项1', value: '1' }]
    }

    // 重置预览表单
    const handleFormPreviewReset = () => {
      formPreviewRef.value?.resetFields()
    }

    onMounted(() => {
      loadTables()
    })

    return {
      tables,
      activeTab,
      form,
      codeMap,
      testResult,
      activeTestItems,
      testAreaExpanded,
      Check,
      Close,
      ArrowUp,
      ArrowDown,
      Loading,
      handleTableChange,
      handleGenerate,
      handleGenerateAll,
      handleCopy,
      runTest,
      toggleTestArea,
      listPreviewVisible,
      listPreviewLoading,
      listPreviewTableLoading,
      listPreviewFields,
      listPreviewSearchFields,
      listPreviewTableData,
      listPreviewSearchForm,
      listPreviewMultipleSelection,
      listPreviewPagination,
      listPreviewTableName,
      listPreviewDialogVisible,
      listPreviewDialogTitle,
      listPreviewForm,
      listPreviewFormRules,
      listPreviewFormRef,
      handlePreviewList,
      getListFieldPropName,
      handleListPreviewSelectionChange,
      handleListPreviewReset,
      handleListPreviewSearch,
      handleListPreviewSortChange,
      handleListPreviewSizeChange,
      handleListPreviewCurrentChange,
      handleListPreviewAdd,
      handleListPreviewEdit,
      handleListPreviewDelete,
      handleListPreviewBatchDelete,
      handleListPreviewSubmit,
      handleListPreviewDialogClose,
      formatPreviewDate,
      formatPreviewNumber,
      formPreviewVisible,
      formPreviewLoading,
      formPreviewFields,
      formPreviewData,
      formPreviewRules,
      formPreviewRef,
      formPreviewTableName,
      handlePreviewForm,
      getFormFieldPropName,
      getFormFieldOptions,
      handleFormPreviewReset
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

.test-card {
  margin-top: 20px;
}

.test-success {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #67c23a;
  font-size: 16px;
  font-weight: bold;
}

.test-error {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #f56c6c;
  font-size: 16px;
  font-weight: bold;
}

.test-detail {
  padding: 10px;
}

.test-errors {
  margin-top: 10px;
  color: #f56c6c;
}

.test-warnings {
  margin-top: 10px;
  color: #e6a23c;
}

.api-tests {
  margin-top: 15px;
}

/* 表单预览对话框样式 */
:deep(.el-dialog__body) {
  max-height: 75vh;
  overflow-y: auto;
}

.code-header > div {
  display: flex;
  gap: 8px;
}
</style>

