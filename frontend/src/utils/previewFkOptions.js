import request from './request'

const cache = new Map()

/**
 * 预览表单外键下拉：从关联表 Mock 列表加载（与 vue_form.ftl 的 label/value 字段一致）
 */
export async function loadPreviewFkOptions(businessCode, field) {
  const entity = field.relatedEntityName
  const labelField = field.relatedTableFieldName || 'id'
  if (!businessCode || !entity) {
    return []
  }
  const cacheKey = `${businessCode}:${entity}:${labelField}`
  if (cache.has(cacheKey)) {
    return cache.get(cacheKey)
  }
  try {
    const res = await request.get(
      `/codegen/preview/mock/${encodeURIComponent(businessCode)}/${encodeURIComponent(entity)}/list`
    )
    const list = res.code === 200 && Array.isArray(res.data) ? res.data : []
    const options = list.map((item, index) => {
      const raw = item[labelField] ?? item.id ?? index
      const value = raw
      const label = raw != null && raw !== '' ? String(raw) : `#${item.id ?? index + 1}`
      return { id: item.id ?? index, label, value }
    })
    cache.set(cacheKey, options)
    return options
  } catch {
    return []
  }
}

export function clearPreviewFkOptionsCache() {
  cache.clear()
}
