<#-- 生成前端API请求文件 -->
<#-- data: className, apiName, tableCode, businessCode -->
import request from '@/utils/request'

export const ${className}Api = {
  page: (params) => request.get('/api/${businessCode}/${apiName}/page', { params }),
  list: (params) => request.get('/api/${businessCode}/${apiName}/list', { params }),
  getById: (id) => request.get('/api/' + '${businessCode}' + '/' + '${apiName}' + '/' + id),
  add: (data) => request.post('/api/${businessCode}/${apiName}/add', data),
  update: (data) => request.post('/api/${businessCode}/${apiName}/update', data),
  delete: (data) => request.post('/api/${businessCode}/${apiName}/delete', data),
  batchDelete: (data) => request.post('/api/${businessCode}/${apiName}/batchDelete', data)
}
