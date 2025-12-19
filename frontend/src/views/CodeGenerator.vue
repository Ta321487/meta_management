<template>
  <div class="code-generator">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>代码生成器</span>
        </div>
      </template>

      <el-form :inline="true" :model="form" class="search-form">
        <el-form-item label="业务系统">
          <el-select v-model="form.businessCode" placeholder="请选择业务系统" style="width: 200px" @change="handleBusinessSystemChange">
            <el-option
              v-for="business in businessSystems"
              :key="business.businessCode"
              :label="business.businessName"
              :value="business.businessCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择表">
          <el-select v-model="form.tableCode" placeholder="请选择表" style="width: 300px" @change="handleTableChange" clearable>
            <el-option
              v-for="table in tables"
              :key="table.tableCode"
              :label="table.tableName"
              :value="table.tableCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="包名">
          <div style="display: flex; align-items: center; width: 300px;">
            <el-input v-model="form.packageName" placeholder="如：com.example" style="width: 270px" readonly />
            <el-tooltip placement="top" content="如需修改请到业务系统配置模块进行修改">
              <el-icon class="info-icon" style="margin-left: 5px; cursor: pointer; color: #909399;">
                <InfoFilled />
              </el-icon>
            </el-tooltip>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="form.tableCode ? handleGenerateCurrentTable() : handleGenerateAllTables()" :tooltip="'生成所有类型的代码'">
            生成代码
          </el-button>
          <el-button type="success" @click="runTest" :disabled="!form.tableCode">测试代码</el-button>
          <el-button type="info" @click="deploymentGuideVisible = true">部署指南</el-button>
        </el-form-item>
      </el-form>

      <el-collapse v-model="activeCollapse" accordion>
        <!-- Java相关 -->
        <el-collapse-item title="Java相关" name="java">
          <el-tabs v-model="activeTab" type="border-card" class="nested-tabs">
            <el-tab-pane label="SQL建表语句" name="sql">
              <div class="code-container" v-if="form.tableCode || codeMap.sql">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + '.sql') : 'all_tables.sql' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('sql')" :tooltip="'刷新当前标签页的SQL代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Entity实体类" name="entity">
              <div class="code-container" v-if="form.tableCode || codeMap.entity">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Entity.java') : 'Entity.java' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('entity')" :tooltip="'刷新当前标签页的实体类代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Controller" name="controller">
              <div class="code-container" v-if="form.tableCode || codeMap.controller">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Controller.java') : 'Controller.java' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('controller')" :tooltip="'刷新当前标签页的Controller代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Service" name="service">
              <div class="code-container" v-if="form.tableCode || codeMap.service">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Service.java') : 'Service.java' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('service')" :tooltip="'刷新当前标签页的Service代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Mapper接口" name="mapper">
              <div class="code-container" v-if="form.tableCode || codeMap.mapper">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Mapper.java') : 'Mapper.java' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('mapper')" :tooltip="'刷新当前标签页的Mapper接口代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Mapper.xml" name="mapperxml">
              <div class="code-container" v-if="form.tableCode || codeMap.mapperxml">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Mapper.xml') : 'Mapper.xml' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('mapperxml')" :tooltip="'刷新当前标签页的Mapper XML代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="启动类" name="application">
              <div class="code-container">
                <div class="code-header">
                  <span>Application.java</span>
                  <el-button type="primary" size="small" @click="handleGenerate('application')" :tooltip="'刷新当前标签页的启动类代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('application')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.application"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成启动类代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="CORS配置" name="corsConfig">
              <div class="code-container">
                <div class="code-header">
                  <span>CorsConfig.java</span>
                  <el-button type="primary" size="small" @click="handleGenerate('corsConfig')" :tooltip="'刷新当前标签页的CORS配置代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('corsConfig')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.corsConfig"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成CORS配置代码"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-collapse-item>

        <!-- 前端相关 -->
        <el-collapse-item title="前端相关" name="frontend">
          <el-tabs v-model="activeTab" type="border-card" class="nested-tabs">
            <el-tab-pane label="Vue列表页" name="vueList">
              <div class="code-container" v-if="form.tableCode || codeMap.vueList">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'List.vue') : 'List.vue' }}</span>
                  <div>
                    <el-button type="primary" size="small" @click="handleGenerate('vueList')" :tooltip="'刷新当前标签页的Vue列表页代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="Vue表单页" name="vueForm">
              <div class="code-container" v-if="form.tableCode || codeMap.vueForm">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Form.vue') : 'Form.vue' }}</span>
                  <div>
                    <el-button type="primary" size="small" @click="handleGenerate('vueForm')" :tooltip="'刷新当前标签页的Vue表单页代码'">刷新</el-button>
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
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="登录页" name="login">
              <div class="code-container">
                <div class="code-header">
                  <span>Login.vue</span>
                  <div>
                    <el-button type="primary" size="small" @click="handleGenerate('login')" :disabled="!form.businessCode" :tooltip="'刷新当前标签页的登录页代码'">刷新</el-button>
                    <el-button type="success" size="small" @click="handleCopy('login')">复制</el-button>
                    <el-button type="info" size="small" @click="handleDownload('login', 'Login.vue')">下载</el-button>
                  </div>
                </div>
                <el-input
                  v-model="codeMap.login"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成登录页代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="Routes" name="routes">
              <div class="code-container" v-if="form.tableCode || codeMap.routes || form.businessCode">
                <div class="code-header">
                  <span>routes.js</span>
                  <div>
                    <el-button type="primary" size="small" @click="handleGenerate('routes')" :disabled="!form.tableCode" :tooltip="'刷新当前标签页的路由配置代码'">刷新</el-button>
                    <el-button type="warning" size="small" @click="handleGenerate('integratedRoutes')" :disabled="!form.businessCode">生成整合路由</el-button>
                    <el-button type="success" size="small" @click="handleCopy('routes')">复制</el-button>
                    <el-button type="info" size="small" @click="handleDownload('routes', 'routes.js')">下载</el-button>
                  </div>
                </div>
                <el-input
                  v-model="codeMap.routes"
                  type="textarea"
                  :rows="10"
                  readonly
                  class="code-textarea"
                />
              </div>
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="API请求文件" name="api">
              <div class="code-container" v-if="form.tableCode || codeMap.api">
                <div class="code-header">
                  <span>{{ form.tableCode ? (form.tableCode.replace('_TABLE', '') + 'Api.js') : 'api.js' }}</span>
                  <el-button type="primary" size="small" @click="handleGenerate('api')" :disabled="!form.tableCode" :tooltip="'刷新当前标签页的API请求文件代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('api')">复制</el-button>
                  <el-button type="info" size="small" @click="handleDownload('api', 'api.js')">下载</el-button>
                </div>
                <el-input
                  v-model="codeMap.api"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成API请求文件代码"
                />
              </div>
              <div v-else style="text-align: center; padding: 40px; color: #909399;">
                <p>请先选择业务系统和表。如不选择表，将只生成SQL.</p>
              </div>
            </el-tab-pane>

            <el-tab-pane label="请求工具类" name="requestJs">
              <div class="code-container">
                <div class="code-header">
                  <span>request.js</span>
                  <el-button type="primary" size="small" @click="handleGenerate('requestJs')" :tooltip="'刷新当前标签页的请求工具类代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('requestJs')">复制</el-button>
                  <el-button type="info" size="small" @click="handleDownload('requestJs', 'request.js')">下载</el-button>
                </div>
                <el-input
                  v-model="codeMap.requestJs"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成请求工具类代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="认证API文件" name="auth">
              <div class="code-container">
                <div class="code-header">
                  <span>auth.js</span>
                  <el-button type="primary" size="small" @click="handleGenerate('auth')" :tooltip="'刷新当前标签页的认证API文件代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('auth')">复制</el-button>
                  <el-button type="info" size="small" @click="handleDownload('auth', 'auth.js')">下载</el-button>
                </div>
                <el-input
                  v-model="codeMap.auth"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成认证API文件代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label=".env配置" name="env">
              <div class="code-container">
                <div class="code-header">
                  <span>.env</span>
                  <el-button type="primary" size="small" @click="handleGenerate('env')" :tooltip="'刷新当前标签页的环境配置代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('env')">复制</el-button>
                  <el-button type="info" size="small" @click="handleDownload('env', '.env')">下载</el-button>
                </div>
                <el-input
                  v-model="codeMap.env"
                  type="textarea"
                  :rows="10"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成环境配置代码"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-collapse-item>

        <!-- 工具相关 -->
        <el-collapse-item title="工具相关" name="tools">
          <el-tabs v-model="activeTab" type="border-card" class="nested-tabs">
            <el-tab-pane label="配置文件" name="applicationYml">
              <div class="code-container">
                <div class="code-header">
                  <span>application.yml</span>
                  <el-button type="primary" size="small" @click="handleGenerate('applicationYml')" :tooltip="'刷新当前标签页的配置文件代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('applicationYml')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.applicationYml"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成配置文件代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="MyBatis配置" name="mybatisConfig">
              <div class="code-container">
                <div class="code-header">
                  <span>MyBatisConfig.java</span>
                  <el-button type="primary" size="small" @click="handleGenerate('mybatisConfig')" :tooltip="'刷新当前标签页的MyBatis配置代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('mybatisConfig')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.mybatisConfig"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成MyBatis配置代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="pom.xml" name="pomXml">
              <div class="code-container">
                <div class="code-header">
                  <span>pom.xml</span>
                  <el-button type="primary" size="small" @click="handleGenerate('pomXml')" :tooltip="'刷新当前标签页的pom.xml代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('pomXml')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.pomXml"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成pom.xml代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="Result类" name="result">
              <div class="code-container">
                <div class="code-header">
                  <span>Result.java</span>
                  <el-button type="primary" size="small" @click="handleGenerate('result')" :tooltip="'刷新当前标签页的Result类代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('result')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.result"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成Result类代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="PageRequest类" name="pageRequest">
              <div class="code-container">
                <div class="code-header">
                  <span>PageRequest.java</span>
                  <el-button type="primary" size="small" @click="handleGenerate('pageRequest')" :tooltip="'刷新当前标签页的PageRequest类代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('pageRequest')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.pageRequest"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成PageRequest类代码"
                />
              </div>
            </el-tab-pane>

            <el-tab-pane label="PageResult类" name="pageResult">
              <div class="code-container">
                <div class="code-header">
                  <span>PageResult.java</span>
                  <el-button type="primary" size="small" @click="handleGenerate('pageResult')" :tooltip="'刷新当前标签页的PageResult类代码'">刷新</el-button>
                  <el-button type="success" size="small" @click="handleCopy('pageResult')">复制</el-button>
                </div>
                <el-input
                  v-model="codeMap.pageResult"
                  type="textarea"
                  :rows="15"
                  readonly
                  class="code-textarea"
                  placeholder="请点击'生成'按钮生成PageResult类代码"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-collapse-item>
      </el-collapse>

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
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        v-model="listPreviewVisible"
        title="列表页样式预览"
        width="1200px"
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
          :close-on-click-modal="false"
          :close-on-press-escape="false"
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
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        v-model="formPreviewVisible"
        title="表单样式预览"
        width="900px"
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

      <!-- 部署指南对话框 -->
      <el-dialog
        v-model="deploymentGuideVisible"
        title="代码部署指南"
        width="1000px"
        :close-on-click-modal="true"
        :close-on-press-escape="true"
      >
        <el-tabs v-model="activeGuideTab" type="border-card">
          <!-- 后端部署指南 -->
          <el-tab-pane label="后端部署" name="backend">
            <div class="deployment-guide">
              <h3>1. 项目结构创建</h3>
              <div class="file-tree">
                <pre>
项目根目录/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/          <!-- 你的包名 -->
│   │   │           ├── common/       <!-- 通用工具类 -->
│   │   │           ├── config/        <!-- 配置类 -->
│   │   │           ├── controller/    <!-- 控制器 -->
│   │   │           ├── entity/        <!-- 实体类 -->
│   │   │           ├── mapper/        <!-- Mapper接口 -->
│   │   │           ├── service/       <!-- 服务类 -->
│   │   │           └── Application.java  <!-- 启动类 -->
│   │   └── resources/
│   │       ├── mapper/               <!-- Mapper XML文件 -->
│   │       └── application.yml       <!-- 配置文件 -->
│   └── test/                          <!-- 测试目录 -->
└── pom.xml                           <!-- Maven依赖配置 -->
                </pre>
              </div>

              <h3>2. 文件放置位置</h3>
              <table class="file-placement-table">
                <thead>
                  <tr>
                    <th>生成的文件</th>
                    <th>目标位置</th>
                    <th>说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Entity.java</td>
                    <td>src/main/java/your/package/entity/</td>
                    <td>数据实体类</td>
                  </tr>
                  <tr>
                    <td>Controller.java</td>
                    <td>src/main/java/your/package/controller/</td>
                    <td>API控制器</td>
                  </tr>
                  <tr>
                    <td>Service.java</td>
                    <td>src/main/java/your/package/service/</td>
                    <td>业务逻辑层</td>
                  </tr>
                  <tr>
                    <td>Mapper.java</td>
                    <td>src/main/java/your/package/mapper/</td>
                    <td>数据访问接口</td>
                  </tr>
                  <tr>
                    <td>Mapper.xml</td>
                    <td>src/main/resources/mapper/</td>
                    <td>MyBatis映射文件</td>
                  </tr>
                  <tr>
                    <td>Application.java</td>
                    <td>src/main/java/your/package/</td>
                    <td>Spring Boot启动类</td>
                  </tr>
                  <tr>
                    <td>Result.java</td>
                    <td>src/main/java/your/package/common/</td>
                    <td>统一返回结果类</td>
                  </tr>
                  <tr>
                    <td>PageRequest.java</td>
                    <td>src/main/java/your/package/common/</td>
                    <td>分页请求类</td>
                  </tr>
                  <tr>
                    <td>PageResult.java</td>
                    <td>src/main/java/your/package/common/</td>
                    <td>分页结果类</td>
                  </tr>
                  <tr>
                    <td>MyBatisConfig.java</td>
                    <td>src/main/java/your/package/config/</td>
                    <td>MyBatis配置类</td>
                  </tr>
                  <tr>
                    <td>application.yml</td>
                    <td>src/main/resources/application.yml</td>
                    <td>应用配置文件</td>
                  </tr>
                  <tr>
                    <td>pom.xml</td>
                    <td>pom.xml</td>
                    <td>Maven依赖配置</td>
                  </tr>
                </tbody>
              </table>

              <h3>3. 配置和运行</h3>
              <ol>
                <li>配置数据库连接：
                  <ul>
                    <li>修改application.yml中的数据库连接信息</li>
                    <li>确保数据库已创建，且用户名密码正确</li>
                  </ul>
                </li>
                <li>执行SQL脚本：
                  <ul>
                    <li>复制生成的SQL建表语句</li>
                    <li>在MySQL客户端中执行，创建数据库表</li>
                  </ul>
                </li>
                <li>运行后端项目：
                  <ul>
                    <li>找到Application.java文件</li>
                    <li>右键点击，选择"Run 'Application'"</li>
                    <li>等待项目启动成功</li>
                  </ul>
                </li>
              </ol>
            </div>
          </el-tab-pane>

          <!-- 前端部署指南 -->
          <el-tab-pane label="前端部署" name="frontend">
            <div class="deployment-guide">
              <h3>1. 项目结构创建</h3>
              <p>推荐在项目根目录下创建frontend目录作为前端项目根目录：</p>
              <div class="file-tree">
                <pre>
后端项目根目录/
├── frontend/                          <!-- 前端项目根目录 -->
│   ├── public/                        <!-- 静态资源 -->
│   ├── src/
│   │   ├── api/                       <!-- API请求 -->
│   │   ├── assets/                    <!-- 资源文件 -->
│   │   ├── components/                <!-- 组件 -->
│   │   ├── router/                    <!-- 路由配置 -->
│   │   ├── store/                     <!-- 状态管理 -->
│   │   ├── styles/                    <!-- 样式文件 -->
│   │   ├── utils/                     <!-- 工具函数 -->
│   │   ├── views/                     <!-- 页面组件 -->
│   │   ├── App.vue                    <!-- 根组件 -->
│   │   └── main.js                    <!-- 入口文件 -->
│   ├── .env                           <!-- 环境配置 -->
│   ├── .gitignore                     <!-- Git忽略文件 -->
│   ├── babel.config.js                <!-- Babel配置 -->
│   ├── package.json                   <!-- npm依赖 -->
│   ├── README.md                      <!-- 项目说明 -->
│   └── vue.config.js                  <!-- Vue配置 -->
└── 后端代码文件...
                </pre>
              </div>

              <h3>2. 文件放置位置</h3>
              <table class="file-placement-table">
                <thead>
                  <tr>
                    <th>生成的文件</th>
                    <th>目标位置</th>
                    <th>说明</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
          <td>List.vue</td>
          <td>frontend/src/views/</td>
          <td>列表页面组件</td>
        </tr>
        <tr>
          <td>Form.vue</td>
          <td>frontend/src/views/</td>
          <td>表单页面组件</td>
        </tr>
        <tr>
          <td>Login.vue</td>
          <td>frontend/src/views/</td>
          <td>登录页面组件</td>
        </tr>
        <tr>
          <td>routes.js</td>
          <td>frontend/src/router/</td>
          <td>路由配置</td>
        </tr>
        <tr>
          <td>auth.js</td>
          <td>frontend/src/api/</td>
          <td>认证相关API</td>
        </tr>
        <tr>
          <td>api.js</td>
          <td>frontend/src/api/</td>
          <td>业务模块API请求文件</td>
        </tr>
        <tr>
          <td>request.js</td>
          <td>frontend/src/utils/</td>
          <td>请求工具类，用于发送API请求</td>
        </tr>
        <tr>
          <td>index.js</td>
          <td>frontend/src/api/</td>
          <td>API统一导出文件，方便组件导入</td>
        </tr>
                </tbody>
              </table>

              <h3>3. 配置和运行</h3>
              <ol>
                <li>在项目根目录创建frontend目录</li>
                <li>进入frontend目录，初始化Vue项目：
                  <ul>
                    <li>执行命令：npm init vue@latest .</li>
                    <li>按照提示选择需要的功能</li>
                  </ul>
                </li>
                <li>安装前端依赖：
                  <ul>
                    <li>执行命令：npm install</li>
                  </ul>
                </li>
                <li>创建API工具类：
                  <ul>
                    <li>在frontend/src/api/目录下创建对应模块的API文件</li>
                    <li>配置API请求的baseURL和拦截器</li>
                  </ul>
                </li>
                <li>集成路由：
                  <ul>
                    <li>将生成的routes.js内容合并到frontend/src/router/目录下的路由配置中</li>
                    <li>确保路由路径与后端API路径匹配</li>
                  </ul>
                </li>
                <li>运行前端项目：
                  <ul>
                    <li>进入frontend目录</li>
                    <li>启动开发服务器：npm run serve</li>
                    <li>构建生产版本：npm run build</li>
                  </ul>
                </li>
              </ol>
            </div>
          </el-tab-pane>
        </el-tabs>
        
        <h3>3. 常见问题解决</h3>
        <ul class="common-problems">
          <li><strong>找不到类或包：</strong>检查包名是否正确，确保所有依赖已添加</li>
          <li><strong>数据库连接失败：</strong>检查application.yml中的数据库配置，确保数据库服务已启动</li>
          <li><strong>SQL语法错误：</strong>检查生成的SQL语句，确保与你的数据库版本兼容</li>
          <li><strong>前端访问后端API跨域：</strong>在后端添加CORS配置，或在前端使用代理</li>
          <li><strong>前端路由404：</strong>检查路由配置是否正确，确保组件路径存在</li>
        </ul>
        
        <template #footer>
          <el-button @click="deploymentGuideVisible = false">关闭</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script>
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
  generateAuth
} from '../api'

export default {
  name: 'CodeGenerator',
  setup() {
    const tables = ref([])
    const businessSystems = ref([])
    const activeTab = ref('sql')
    const activeCollapse = ref([]) // 用于跟踪当前展开的父菜单
    const form = reactive({
      businessCode: '',
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
      env: '',
      result: '',
      pageRequest: '',
      pageResult: ''
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

    // 部署指南对话框相关
    const deploymentGuideVisible = ref(false)
    const activeGuideTab = ref('backend')

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

    const loadTables = async (businessCode = '') => {
      try {
        if (!businessCode) {
          // 当businessCode为空时，清空表列表
          tables.value = []
          return
        }
        ElMessage.info('正在加载表列表...')
        const res = await getTableList({ businessCode })
      
        if (res.code === 200) {
        
          // 过滤掉禁用状态的表
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

    const handleBusinessSystemChange = () => {
      // 清空表选择和代码
      form.tableCode = ''
      Object.keys(codeMap).forEach(key => {
        codeMap[key] = ''
      })
      // 根据业务系统加载表
      loadTables(form.businessCode)
      // 自动填充包名
      const selectedSystem = businessSystems.value.find(system => system.businessCode === form.businessCode)
      if (selectedSystem && selectedSystem.packageName) {
        form.packageName = selectedSystem.packageName
      } else {
        form.packageName = 'com.example'
      }
    }

    // 切换表时更新代码显示
    const handleTableChange = async () => {
      if (!form.tableCode) {
        // 清空代码
        Object.keys(codeMap).forEach(key => {
          codeMap[key] = ''
        })
        return
      }
      
      try {
        // 生成当前选中表的代码
        await handleGenerateCurrentTable()
      } catch (error) {
        ElMessage.error('切换表失败：' + (error.message || '未知错误'))
      }
    }

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
            res = await generateService(form.tableCode, form.packageName, form.businessCode)
            if (res.code === 200) {
              codeMap.service = res.data
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
            // 生成启动类，不需要选择表
            let resApp = await generateApplication(form.packageName)
            if (resApp.code === 200) {
              codeMap.application = resApp.data
            }
            break
          case 'applicationYml':
            // 生成配置文件，不需要选择表
            let resAppYml = await generateApplicationYml(form.packageName)
            if (resAppYml.code === 200) {
              codeMap.applicationYml = resAppYml.data
            }
            break
          case 'mybatisConfig':
            // 生成MyBatis配置文件，不需要选择表
            let resMyBatisConfig = await generateMyBatisConfig(form.packageName)
            if (resMyBatisConfig.code === 200) {
              codeMap.mybatisConfig = resMyBatisConfig.data
            }
            break
          case 'pomXml':
            // 生成pom.xml配置文件，不需要选择表
            let resPomXml = await generatePomXml(form.packageName)
            if (resPomXml.code === 200) {
              codeMap.pomXml = resPomXml.data
            }
            break
          case 'corsConfig':
            // 生成CORS配置类，不需要选择表
            let resCorsConfig = await generateCorsConfig(form.packageName)
            if (resCorsConfig.code === 200) {
              codeMap.corsConfig = resCorsConfig.data
            }
            break
          case 'api':
            // 生成前端API请求文件
            res = await generateApi(form.tableCode, form.businessCode)
            if (res.code === 200) {
              codeMap.api = res.data
            }
            break
          case 'requestJs':
            // 生成前端请求工具类
            let resRequestJs = await generateRequestJs()
            if (resRequestJs.code === 200) {
              codeMap.requestJs = resRequestJs.data
            }
            break
          case 'auth':
            // 生成前端认证API文件
            let resAuth = await generateAuth()
            if (resAuth.code === 200) {
              codeMap.auth = resAuth.data
            }
            break
          case 'env':
            // 生成前端环境配置文件
            let resEnv = await generateEnvFile()
            if (resEnv.code === 200) {
              codeMap.env = resEnv.data
            }
            break
          case 'login':
            // 生成登录页
            res = await generateLoginPage(form.businessCode)
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
        const res = await generateAll(form.tableCode, form.packageName, form.businessCode)
        if (res.code === 200 && res.data) {
          const data = res.data
          codeMap.sql = data['create_table.sql'] || ''
          codeMap.entity = data['Entity.java'] || ''
          codeMap.controller = data['Controller.java'] || ''
          codeMap.service = data['Service.java'] || ''
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
          ElMessage.success('代码生成成功')
          // 移除自动运行测试，避免因testCode接口404导致生成失败提示
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
          // 获取所有SQL键
          const sqlKeys = Object.keys(sqlRes.data)
          
          if (form.tableCode) {
            // 如果有选中表，找对应的SQL
            const targetSqlKey = form.tableCode + '.sql'
            if (targetSqlKey && sqlRes.data[targetSqlKey]) {
              codeMap.sql = sqlRes.data[targetSqlKey]
            }
          } else if (sqlKeys.length > 0) {
            // 否则生成所有表的SQL，用换行分隔
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
          // 生成业务系统下所有表的完整代码包
          const allCodeRes = await generateAllByBusinessSystem(form.businessCode, form.packageName)
          if (allCodeRes.code === 200 && allCodeRes.data) {
            // 生成所有表的代码，但当前只显示当前选中表的代码
            const tableCodes = Object.keys(allCodeRes.data)
            const targetTableCode = form.tableCode
            
            if (targetTableCode && allCodeRes.data[targetTableCode]) {
              const tableCodeMap = allCodeRes.data[targetTableCode]
              // 更新所有代码类型
              codeMap.entity = tableCodeMap['Entity.java'] || ''
              codeMap.controller = tableCodeMap['Controller.java'] || ''
              codeMap.service = tableCodeMap['Service.java'] || ''
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
            // 清空非SQL代码，显示提示信息
            Object.keys(codeMap).forEach(key => {
              if (key !== 'sql') {
                codeMap[key] = ''
              }
            })
          }
          
          // 移除自动运行测试，避免因testCode接口404导致生成失败提示
        } else {
            // 没有选择表，清空非SQL代码，只保留SQL代码和通用类代码
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
        // 清空代码，显示提示信息，保留通用类代码
        Object.keys(codeMap).forEach(key => {
          if (key !== 'result' && key !== 'pageRequest' && key !== 'pageResult') {
            codeMap[key] = ''
          }
        })
      }
    }

    // 保留原有方法，兼容已有代码
    const handleGenerateAll = handleGenerateAllTables

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

    const handleDownload = (type, filename) => {
      const text = codeMap[type]
      if (!text) {
        ElMessage.warning('请先生成代码')
        return
      }
      const blob = new Blob([text], { type: 'text/javascript;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename || (type + '.js')
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
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
            
            // 内置正则表达式映射表，根据type值提供相应的正则表达式
            const builtInRegexMap = {
              email: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
              url: '^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*\\/?$',
              number: '^-?\\d+(\\.\\d+)?$',
              integer: '^-?\\d+$'
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
                } else if (validateRule.type && builtInRegexMap[validateRule.type]) {
                  // 处理带有type属性的约束
                  rules.push({
                    pattern: new RegExp(builtInRegexMap[validateRule.type]),
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
    // 内置正则表达式映射表，根据type值提供相应的正则表达式
    const builtInRegexMap = {
      email: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
      url: '^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*\\/?$',
      number: '^-?\\d+(\\.\\d+)?$',
      integer: '^-?\\d+$',
      phone: '^1[3-9]\\d{9}$',
      idcard: '^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$'
    }
    
    // 解析校验规则
    const parseValidationRule = (validateRule) => {
      const rules = {
        hasPattern: false,
        hasOptions: false,
        hasLength: false,
        hasRange: false,
        hasOperator: false,
        pattern: null,
        minLength: null,
        maxLength: null,
        min: null,
        max: null,
        options: [],
        operator: null,
        values: []
      }
      
      if (!validateRule) {
        return rules
      }
      
      try {
        const jsonRule = typeof validateRule === 'string' ? JSON.parse(validateRule) : validateRule
        
        // 提取正则表达式或type属性对应的内置正则表达式
        let pattern = null
        if (jsonRule.pattern) {
          pattern = jsonRule.pattern
        } else if (jsonRule.type && builtInRegexMap[jsonRule.type]) {
          pattern = builtInRegexMap[jsonRule.type]
        }
        
        if (pattern) {
          rules.hasPattern = true
          rules.pattern = pattern
        }
        
        // 提取长度限制
        if (jsonRule.minLength !== undefined || jsonRule.maxLength !== undefined) {
          rules.hasLength = true
          rules.minLength = parseInt(jsonRule.minLength) || null
          rules.maxLength = parseInt(jsonRule.maxLength) || null
        }
        
        // 提取数值范围
        if (jsonRule.min !== undefined || jsonRule.max !== undefined) {
          rules.hasRange = true
          rules.min = typeof jsonRule.min === 'number' ? jsonRule.min : parseFloat(jsonRule.min) || null
          rules.max = typeof jsonRule.max === 'number' ? jsonRule.max : parseFloat(jsonRule.max) || null
        }
        
        // 提取选项（用于下拉框）
        if (jsonRule.options) {
          rules.hasOptions = true
          rules.options = Array.isArray(jsonRule.options) ? jsonRule.options : []
        }
        
        // 提取操作符（IN、BETWEEN等）
        if (jsonRule.operator) {
          rules.hasOperator = true
          rules.operator = jsonRule.operator
          
          if (jsonRule.values) {
            rules.values = Array.isArray(jsonRule.values) ? jsonRule.values : []
          }
          
          if (jsonRule.operator === 'BETWEEN') {
            rules.hasRange = true
            rules.min = typeof jsonRule.min === 'number' ? jsonRule.min : parseFloat(jsonRule.min) || null
            rules.max = typeof jsonRule.max === 'number' ? jsonRule.max : parseFloat(jsonRule.max) || null
          }
        }
        
      } catch (e) {
        // 解析失败，返回默认规则
      }
      
      return rules
    }
    
    // 生成符合正则表达式的随机字符串
    const generateRandomStringByRegex = (pattern, fieldLabel) => {
      // 生成随机手机号
      const generateRandomPhone = () => {
        return '1' + [3, 5, 7, 8, 9][Math.floor(Math.random() * 5)] + Math.floor(Math.random() * 1000000000).toString().padStart(9, '0')
      }
      
      // 生成随机邮箱
      const generateRandomEmail = () => {
        const domains = ['example.com', 'test.com', 'demo.com', 'sample.com', 'mail.com']
        return `user${Math.floor(Math.random() * 10000)}@${domains[Math.floor(Math.random() * domains.length)]}`
      }
      
      // 生成随机URL
      const generateRandomUrl = () => {
        const domains = ['example.com', 'test.com', 'demo.com', 'sample.com', 'site.com']
        return `https://www.${domains[Math.floor(Math.random() * domains.length)]}/page${Math.floor(Math.random() * 1000)}`
      }
      
      // 生成随机身份证号
      const generateRandomIdCard = () => {
        return '110101' + (new Date().getFullYear() - Math.floor(Math.random() * 60)).toString() + Math.floor(Math.random() * 12 + 1).toString().padStart(2, '0') + Math.floor(Math.random() * 28 + 1).toString().padStart(2, '0') + Math.floor(Math.random() * 100000000).toString().padStart(8, '0') + (Math.floor(Math.random() * 10) === 9 ? 'X' : Math.floor(Math.random() * 10))
      }
      
      // 生成随机数字
      const generateRandomNum = () => {
        return Math.floor(Math.random() * 1000)
      }
      
      // 首先根据字段标签生成数据
      if (fieldLabel?.includes('手机')) {
        return generateRandomPhone()
      } else if (fieldLabel?.includes('邮箱')) {
        return generateRandomEmail()
      } else if (fieldLabel?.includes('URL') || fieldLabel?.includes('网址')) {
        return generateRandomUrl()
      } else if (fieldLabel?.includes('身份证')) {
        return generateRandomIdCard()
      }
      
      // 尝试根据正则表达式特征识别类型
      try {
        const regex = new RegExp(pattern)
        
        // 测试手机号正则
        if (regex.test('13800138001')) {
          return generateRandomPhone()
        }
        
        // 测试邮箱正则
        if (regex.test('test@example.com')) {
          return generateRandomEmail()
        }
        
        // 测试URL正则
        if (regex.test('https://www.example.com')) {
          return generateRandomUrl()
        }
        
        // 测试身份证正则
        if (regex.test('110101199001011234')) {
          return generateRandomIdCard()
        }
        
        // 测试数字正则
        if (regex.test('123') || regex.test('123.45')) {
          return generateRandomNum()
        }
      } catch (e) {
        // 忽略正则表达式错误
      }
      
      // 根据正则表达式字符串特征识别类型
      if (pattern.includes('@')) {
        return generateRandomEmail()
      } else if (pattern.includes('://')) {
        return generateRandomUrl()
      } else if (pattern.includes('1[3-9]') || pattern.includes('\\d{9}') || pattern.includes('\\d{11}')) {
        return generateRandomPhone()
      } else if (pattern.includes('18|19|20')) {
        return generateRandomIdCard()
      } else if (pattern.includes('\\d')) {
        return generateRandomNum()
      }
      
      // 默认生成随机字符串
      return fieldLabel + Math.floor(Math.random() * 1000)
    }
    
    // 生成指定长度的随机字符串
    const generateRandomString = (minLength = 5, maxLength = 20) => {
      const length = Math.floor(Math.random() * (maxLength - minLength + 1)) + minLength
      const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
      let result = ''
      for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length))
      }
      return result
    }
    
    // 生成指定范围内的随机数值
    const generateRandomNumber = (min = 0, max = 1000, isInteger = true) => {
      if (isInteger) {
        return Math.floor(Math.random() * (max - min + 1)) + min
      } else {
        return Math.random() * (max - min) + min
      }
    }
    
    // 生成模拟表数据
    const generateMockTableData = (fields, count) => {
      const data = []
      for (let i = 1; i <= count; i++) {
        const row = {}
        fields.forEach(field => {
          const propName = getListFieldPropName(field)
          const validationRules = parseValidationRule(field.validateRule)
          
          let mockValue
          
          // 处理选项列表（用于下拉框）
          if (validationRules.hasOptions && validationRules.options.length > 0) {
            const options = validationRules.options
            const randomOption = options[Math.floor(Math.random() * options.length)]
            mockValue = typeof randomOption === 'object' ? randomOption.value : randomOption
          } 
          // 处理操作符为IN的情况
          else if (validationRules.hasOperator && validationRules.operator === 'IN' && validationRules.values.length > 0) {
            mockValue = validationRules.values[Math.floor(Math.random() * validationRules.values.length)]
          } 
          // 处理日期时间类型
          else if (field.fieldType?.includes('date') || field.fieldType?.includes('time')) {
            mockValue = new Date().toISOString().split('T')[0]
          } 
          // 处理数值类型
          else if (field.fieldType?.includes('int') || field.fieldType?.includes('decimal') || field.fieldType?.includes('numeric')) {
            let min = 0
            let max = 1000
            let isInteger = field.fieldType?.includes('int')
            
            if (validationRules.hasRange) {
              if (validationRules.min !== null) {
                min = validationRules.min
              }
              if (validationRules.max !== null) {
                max = validationRules.max
              }
            }
            
            mockValue = generateRandomNumber(min, max, isInteger)
          } 
          // 首先根据字段标签或正则表达式生成数据
          else {
            // 优先使用正则表达式规则
            if (validationRules.hasPattern) {
              mockValue = generateRandomStringByRegex(validationRules.pattern, field.label)
            } 
            // 然后根据字段标签关键词生成数据
            else if (field.label?.includes('手机')) {
              mockValue = generateRandomStringByRegex(builtInRegexMap.phone, field.label)
            } 
            else if (field.label?.includes('邮箱')) {
              mockValue = generateRandomStringByRegex(builtInRegexMap.email, field.label)
            } 
            else if (field.label?.includes('URL') || field.label?.includes('网址')) {
              mockValue = generateRandomStringByRegex(builtInRegexMap.url, field.label)
            } 
            else if (field.label?.includes('身份证')) {
              mockValue = generateRandomStringByRegex(builtInRegexMap.idcard, field.label)
            } 
            // 处理文本类型（考虑长度限制）
            else {
              let minLength = 5
              let maxLength = 20
              
              if (validationRules.hasLength) {
                if (validationRules.minLength !== null) {
                  minLength = validationRules.minLength
                }
                if (validationRules.maxLength !== null) {
                  maxLength = validationRules.maxLength
                }
              }
              
              // 生成符合长度要求的随机字符串
              const randomStr = generateRandomString(minLength, maxLength)
              mockValue = randomStr
            }
          }
          
          row[propName] = mockValue
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
            
            // 内置正则表达式映射表，根据type值提供相应的正则表达式
            const builtInRegexMap = {
              email: '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$',
              url: '^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*\\/?$',
              number: '^-?\\d+(\\.\\d+)?$',
              integer: '^-?\\d+$'
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
                } else if (validateRule.type && builtInRegexMap[validateRule.type]) {
                  // 处理带有type属性的约束
                  rules.push({
                    pattern: new RegExp(builtInRegexMap[validateRule.type]),
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

    onMounted(async () => {
      await loadBusinessSystems()
      // 不自动加载表，只在选择业务系统后加载
    })

    return {
      tables,
      businessSystems,
      activeTab,
      activeCollapse,
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
      handleBusinessSystemChange,
      handleTableChange,
      handleGenerate,
      handleGenerateAll,
      handleGenerateCurrentTable,
      handleGenerateAllTables,
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
      handleFormPreviewReset,
      deploymentGuideVisible,
      activeGuideTab
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

/* 标签页样式 - 允许换行 */
:deep(.el-tabs__header) {
  flex-wrap: wrap;
  overflow: visible;
}

/* 嵌套标签页样式 */
.nested-tabs {
  margin-top: 10px;
}

.nested-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

:deep(.el-tabs__nav-wrap) {
  overflow: visible;
}

:deep(.el-tabs__nav-scroll) {
  overflow: visible;
}

:deep(.el-tabs__nav) {
  flex-wrap: wrap;
  overflow: visible;
  width: auto;
}

:deep(.el-tabs__item) {
  margin-bottom: 10px;
}

/* 部署指南样式 */
.deployment-guide {
  padding: 10px;
}

.deployment-guide h3 {
  margin: 20px 0 15px 0;
  color: #303133;
  font-size: 16px;
  font-weight: bold;
}

.deployment-guide ol {
  margin-left: 20px;
  padding-left: 10px;
}

.deployment-guide li {
  margin-bottom: 10px;
  line-height: 1.6;
}

.deployment-guide ul {
  margin-left: 20px;
  padding-left: 10px;
}

/* 文件树样式 */
.file-tree {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  margin: 15px 0;
  overflow-x: auto;
}

.file-tree pre {
  margin: 0;
  font-family: 'Monaco', 'Consolas', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.4;
  color: #303133;
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 文件放置表格样式 */
.file-placement-table {
  width: 100%;
  border-collapse: collapse;
  margin: 15px 0;
  font-size: 14px;
}

.file-placement-table th,
.file-placement-table td {
  padding: 10px;
  border: 1px solid #e4e7ed;
  text-align: left;
}

.file-placement-table th {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #303133;
}

.file-placement-table tr:nth-child(even) {
  background-color: #fafafa;
}

.file-placement-table tr:hover {
  background-color: #ecf5ff;
}

/* 常见问题样式 */
.common-problems {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.common-problems li {
  margin-bottom: 10px;
  line-height: 1.6;
}

.common-problems strong {
  color: #409eff;
}

/* 折叠面板样式 */
:deep(.el-collapse-item__header) {
  background-color: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 10px;
  font-weight: bold;
}

:deep(.el-collapse-item__header:hover) {
  background-color: #ecf5ff;
}

:deep(.el-collapse-item__content) {
  padding: 0;
  border: none;
  margin-bottom: 15px;
}

/* 折叠面板内容区样式 */
:deep(.el-collapse-item__content > .el-tabs) {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}
</style>

