// 代码生成器通用工具函数

// 复制文本到剪贴板
export function copyToClipboard(text) {
  return new Promise((resolve, reject) => {
    if (navigator.clipboard && window.isSecureContext) {
      // 现代浏览器支持的API
      navigator.clipboard.writeText(text)
        .then(() => resolve())
        .catch(err => reject(err));
    } else {
      // 兼容性处理
      const textArea = document.createElement('textarea');
      textArea.value = text;
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      document.body.appendChild(textArea);
      textArea.focus();
      textArea.select();
      
      try {
        const successful = document.execCommand('copy');
        if (successful) {
          resolve();
        } else {
          reject(new Error('复制失败'));
        }
      } catch (err) {
        reject(err);
      } finally {
        document.body.removeChild(textArea);
      }
    }
  });
}

// 下载文件
export function downloadFile(content, filename, contentType = 'text/plain') {
  const blob = new Blob([content], { type: contentType });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

// 格式化日期
export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return '';
  
  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');
  
  return format
    .replace('YYYY', year)
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds);
}

// 获取文件名
export function getFileName(codeType, tableCode) {
  const baseName = tableCode ? tableCode.replace('_TABLE', '') : '';
  
  const fileNameMap = {
    sql: tableCode ? `${baseName}.sql` : 'all_tables.sql',
    entity: tableCode ? `${baseName}Entity.java` : 'Entity.java',
    controller: tableCode ? `${baseName}Controller.java` : 'Controller.java',
    service: tableCode ? `${baseName}Service.java` : 'Service.java',
    'service-interface': tableCode ? `${baseName}Service.java` : 'Service.java',
    'service-impl': tableCode ? `${baseName}ServiceImpl.java` : 'ServiceImpl.java',
    mapper: tableCode ? `${baseName}Mapper.java` : 'Mapper.java',
    mapperxml: tableCode ? `${baseName}Mapper.xml` : 'Mapper.xml',
    application: 'Application.java',
    corsConfig: 'CorsConfig.java',
    vueList: tableCode ? `${baseName}List.vue` : 'List.vue',
    vueForm: tableCode ? `${baseName}Form.vue` : 'Form.vue',
    login: 'Login.vue',
    routes: 'routes.js',
    api: tableCode ? `${baseName}Api.js` : 'api.js',
    requestJs: 'request.js',
    auth: 'auth.js',
    captchaInput: 'CaptchaInput.vue',
    authController: 'AuthController.java',
    captchaService: 'CaptchaService.java',
    env: '.env',
    applicationYml: 'application.yml',
    mybatisConfig: 'MyBatisConfig.java',
    pomXml: 'pom.xml',
    result: 'Result.java',
    pageRequest: 'PageRequest.java',
    pageResult: 'PageResult.java'
  };
  
  return fileNameMap[codeType] || `${codeType}.txt`;
}

// 获取文件内容类型
export function getFileContentType(filename) {
  const ext = filename.split('.').pop().toLowerCase();
  const contentTypeMap = {
    'js': 'text/javascript',
    'vue': 'text/plain',
    'java': 'text/plain',
    'xml': 'application/xml',
    'yml': 'text/yaml',
    'yaml': 'text/yaml',
    'sql': 'text/plain',
    'txt': 'text/plain',
    'md': 'text/markdown'
  };
  
  return contentTypeMap[ext] || 'text/plain';
}

/**
 * 将后端「单表完整代码包」写入页面 codeMap
 * @param {Object} codeMap 页面各 Tab 展示的源码对象
 * @param {Object} bundle 接口 data（TableGeneratedCodeBundle）
 */
export function applyGeneratedCodeBundle(codeMap, bundle) {
  if (!bundle || !codeMap) return
  codeMap.sql = bundle.createTableSql || ''
  codeMap.entity = bundle.entityJava || ''
  codeMap.controller = bundle.controllerJava || ''
  codeMap.service = bundle.serviceJava || ''
  codeMap.serviceInterface = bundle.serviceInterfaceJava || ''
  codeMap.serviceImpl = bundle.serviceImplJava || ''
  codeMap.mapper = bundle.mapperJava || ''
  codeMap.mapperxml = bundle.mapperXml || ''
  codeMap.result = bundle.resultJava || ''
  codeMap.pageRequest = bundle.pageRequestJava || ''
  codeMap.pageResult = bundle.pageResultJava || ''
  codeMap.vueList = bundle.listVue || ''
  codeMap.vueForm = bundle.formVue || ''
  codeMap.login = bundle.loginVue || ''
  codeMap.auth = bundle.authJs || codeMap.auth
  codeMap.captchaInput = bundle.captchaInputVue || ''
  codeMap.authController = bundle.authControllerJava || ''
  codeMap.captchaService = bundle.captchaServiceJava || ''
  codeMap.routes = bundle.routesJs || ''
  codeMap.api = bundle.apiJs || ''
  codeMap.requestJs = bundle.requestJs || ''
  codeMap.env = bundle.envFile || ''
  codeMap.application = bundle.applicationJava || ''
  codeMap.applicationYml = bundle.applicationYml || ''
  codeMap.mybatisConfig = bundle.myBatisConfigJava || ''
  codeMap.corsConfig = bundle.corsConfigJava || ''
  codeMap.pomXml = bundle.pomXml || ''
}

/**
 * 从业务系统代码包列表中按表编码取 files
 * @param {Array} tables data.tables
 * @param {string} tableCode 表编码
 */
export function findTableCodeBundle(tables, tableCode) {
  if (!tables || !tableCode) return null
  const entry = tables.find((t) => t.tableCode === tableCode)
  return entry ? entry.files : null
}

/**
 * 将认证扩展包写入 codeMap
 * @param {Object} codeMap 页面源码对象
 * @param {Object} ext 接口 data（AuthExtensionCodeBundle）
 */
export function applyAuthExtensionBundle(codeMap, ext) {
  if (!ext || !codeMap) return
  codeMap.authController = ext.authControllerJava || ''
  codeMap.captchaService = ext.captchaServiceJava || ''
}
