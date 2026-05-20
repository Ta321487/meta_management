import request from './request'

/** 与生成物 API 路径一致，仅前缀为预览 Mock */
export function createPreviewMockApi(mockApiBase, primaryKeyCamelCase) {
  const base = mockApiBase.startsWith('/') ? mockApiBase : `/${mockApiBase}`
  const pkName = primaryKeyCamelCase || 'id'
  const pkParams = { params: { pkName } }
  return {
    primaryKeyCamelCase: pkName,
    page: (params) => request.get(`${base}/page`, { params }),
    list: (params) => request.get(`${base}/list`, { params }),
    getById: (id) => request.get(`${base}/${id}`, pkParams),
    add: (data) => request.post(`${base}/add`, data, pkParams),
    update: (data) => request.post(`${base}/update`, data, pkParams),
    delete: (data) => request.post(`${base}/delete`, data, pkParams),
    batchDelete: (data) => request.post(`${base}/batchDelete`, data, pkParams),
    checkUnique: (data) => request.post(`${base}/checkUnique`, data, pkParams),
    checkUniqueCombo: (data) => request.post(`${base}/checkUniqueCombo`, data, pkParams)
  }
}
