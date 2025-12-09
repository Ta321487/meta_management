import request from '../utils/request'

// 认证相关
export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
export const changePassword = (data) => request.post('/auth/changePassword', data)

// 模块相关
export const getModuleList = (params) => request.get('/module/list', { params })
export const getModuleByCode = (moduleCode) => request.get(`/module/${moduleCode}`)
export const addModule = (data) => request.post('/module/add', data)
export const updateModule = (data) => request.post('/module/update', data)
export const deleteModule = (data) => request.post('/module/delete', data)
export const batchDeleteModule = (data) => request.post('/module/batchDelete', data)
export const updateModuleStatus = (data) => request.post('/module/updateStatus', data)

// 表相关
export const getTableList = (params) => request.get('/table/list', { params })
export const getTableByCode = (tableCode) => request.get(`/table/${tableCode}`)
export const addTable = (data) => request.post('/table/add', data)
export const updateTable = (data) => request.post('/table/update', data)
export const deleteTable = (data) => request.post('/table/delete', data)
export const batchDeleteTable = (data) => request.post('/table/batchDelete', data)
export const getTablesByModule = (moduleCode) => request.get(`/table/listByModule/${moduleCode}`)

// 字段相关
export const getFieldList = (tableCode, params) => request.get(`/field/list/${tableCode}`, { params })
export const addField = (data) => request.post('/field/add', data)
export const updateField = (data) => request.post('/field/update', data)
export const deleteField = (data) => request.post('/field/delete', data)
export const batchDeleteField = (data) => request.post('/field/batchDelete', data)
export const getConstraintList = (tableCode) => request.get(`/field/constraint/list/${tableCode}`)
export const deleteConstraint = (data) => request.post('/field/constraint/delete', data)

// 功能节点相关
export const getNodeList = (moduleCode, params) => request.get(`/node/list/${moduleCode}`, { params })
export const addNode = (data) => request.post('/node/add', data)
export const updateNode = (data) => request.post('/node/update', data)
export const deleteNode = (data) => request.post('/node/delete', data)
export const updateNodeSort = (data) => request.post('/node/updateSort', data)

// 业务规则相关
export const getRuleList = (moduleCode, params) => request.get(`/rule/list/${moduleCode}`, { params })
export const addRule = (data) => request.post('/rule/add', data)
export const updateRule = (data) => request.post('/rule/update', data)
export const deleteRule = (data) => request.post('/rule/delete', data)

// 表关联关系相关
export const getRelationsByMain = (mainTableCode) => request.get(`/relation/listByMain/${mainTableCode}`)
export const getRelationsBySlave = (slaveTableCode) => request.get(`/relation/listBySlave/${slaveTableCode}`)
export const getAllRelations = (params) => request.get('/relation/list', { params })
export const addRelation = (data) => request.post('/relation/add', data)
export const updateRelation = (data) => request.post('/relation/update', data)
export const deleteRelation = (data) => request.post('/relation/delete', data)
export const createForeignKey = (data) => request.post('/relation/createForeignKey', data)
export const syncForeignKeys = (tableCode) => request.post('/relation/syncForeignKeys', tableCode ? { tableCode } : {})

// 代码生成相关
export const generateSQL = (tableCode) => request.get(`/codegen/sql/${tableCode}`)
export const generateEntity = (tableCode, packageName) => request.get(`/codegen/entity/${tableCode}`, { params: { packageName } })
export const generateController = (tableCode, packageName) => request.get(`/codegen/controller/${tableCode}`, { params: { packageName } })
export const generateService = (tableCode, packageName) => request.get(`/codegen/service/${tableCode}`, { params: { packageName } })
export const generateMapper = (tableCode, packageName) => request.get(`/codegen/mapper/${tableCode}`, { params: { packageName } })
export const generateMapperXml = (tableCode, packageName) => request.get(`/codegen/mapperxml/${tableCode}`, { params: { packageName } })
export const generateVueList = (tableCode) => request.get(`/codegen/vue/list/${tableCode}`)
export const generateVueForm = (tableCode) => request.get(`/codegen/vue/form/${tableCode}`)
export const generateAll = (tableCode, packageName) => request.get(`/codegen/all/${tableCode}`, { params: { packageName } })
export const generateRoutes = (tableCode) => request.get(`/codegen/routes/${tableCode}`)

// 代码测试相关
export const testCode = (tableCode, packageName) => request.get(`/codetest/test/${tableCode}`, { params: { packageName } })

// 模块类型相关
export const getModuleTypeList = () => request.get('/moduleType/list')
export const addModuleType = (data) => request.post('/moduleType/add', data)
export const updateModuleType = (data) => request.post('/moduleType/update', data)
export const deleteModuleType = (data) => request.post('/moduleType/delete', data)

// 元数据导出相关
export const getModuleMetadata = (moduleCode) => request.get(`/metadata/module/${moduleCode}`)
export const getTableFields = (tableCode) => request.get(`/metadata/table/${tableCode}/fields`)
export const exportAllMetadata = () => request.get('/metadata/export/all')

// 操作日志相关
export const getOperationLogList = (params) => request.get('/operationLog/list', { params })

// SQL执行相关
export const executeSql = (data) => request.post('/sql/execute', data)
export const executeMultipleSql = (data) => request.post('/sql/executeMultiple', data)

// 业务系统相关
export const getBusinessSystemList = () => request.get('/businessSystem/list')
export const getBusinessSystemByCode = (businessCode) => request.get(`/businessSystem/${businessCode}`)
export const getDefaultBusinessSystem = () => request.get('/businessSystem/default')
export const addBusinessSystem = (data) => request.post('/businessSystem/add', data)
export const updateBusinessSystem = (data) => request.post('/businessSystem/update', data)
export const deleteBusinessSystem = (data) => request.post('/businessSystem/delete', data)
export const associateModulesToBusinessSystem = (data) => request.post('/businessSystem/associateModules', data)
export const disassociateModulesFromBusinessSystem = (data) => request.post('/businessSystem/disassociateModules', data)
export const getAssociatedModules = (businessCode) => request.get(`/businessSystem/associatedModules/${businessCode}`)

