import request from './request'

/** 与生成物 API 路径一致，仅前缀为预览 Mock */
export function createPreviewMockApi(mockApiBase, primaryKeyCamelCase) {
  const base = mockApiBase.startsWith('/') ? mockApiBase : `/${mockApiBase}`
  return {
    primaryKeyCamelCase,
    page: (params) => request.get(`${base}/page`, { params }),
    list: (params) => request.get(`${base}/list`, { params }),
    getById: (id) => request.get(`${base}/${id}`),
    add: (data) => request.post(`${base}/add`, data),
    update: (data) => request.post(`${base}/update`, data),
    delete: (data) => request.post(`${base}/delete`, data),
    batchDelete: (data) => request.post(`${base}/batchDelete`, data),
    checkUnique: (data) => request.post(`${base}/checkUnique`, data),
    checkUniqueCombo: (data) => request.post(`${base}/checkUniqueCombo`, data)
  }
}
