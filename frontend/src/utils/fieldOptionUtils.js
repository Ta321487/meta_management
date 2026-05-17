/**
 * 字段下拉选项解析（列表「选项」列、预览、Mock 同源）
 */

export function parseEnumLiteralsFromFieldType(fieldType) {
  if (!fieldType) return []
  const m = String(fieldType).trim().match(/^ENUM\s*\((.*)\)\s*$/i)
  if (!m) return []
  return m[1]
    .split(',')
    .map(s => s.trim().replace(/^['"]|['"]$/g, ''))
    .filter(s => s.length > 0)
}

export function coerceOptionValue(raw) {
  if (raw === null || raw === undefined) return raw
  const s = String(raw).trim()
  if (/^-?\d+$/.test(s)) return Number(s)
  return raw
}

export function normalizeOptionItem(o) {
  if (o == null) return null
  if (typeof o === 'object' && !Array.isArray(o)) {
    const value = coerceOptionValue(o.value ?? o.label)
    const label = o.label ?? o.value
    if (value == null && label == null) return null
    return { value, label: label ?? value }
  }
  const v = coerceOptionValue(o)
  return { value: v, label: String(o) }
}

export function parseValidateRuleObject(raw) {
  if (raw == null) return null
  if (typeof raw === 'object' && !Array.isArray(raw)) return raw
  const str = String(raw).trim()
  if (!str || str === '{}') return null
  try {
    return JSON.parse(str)
  } catch {
    return null
  }
}

/** 将 IN.values 与已有 options 合并，保留 label */
export function mergeOptionsWithValues(values, prevOptions = []) {
  const prev = Array.isArray(prevOptions) ? prevOptions : []
  return values.map((v, index) => {
    const value = coerceOptionValue(v)
    const hit =
      prev.find(o => o != null && (o.value === value || String(o.value) === String(value))) ||
      prev[index]
    if (hit && typeof hit === 'object') {
      const label = hit.label ?? hit.value
      return { value, label: label ?? value }
    }
    return { value, label: String(v) }
  })
}

/**
 * @param {{ validateRule?: string|object, fieldType?: string }} field
 * @returns {{ value, label }[]}
 */
export function resolveFieldOptionItems(field) {
  if (!field) return []
  const obj = parseValidateRuleObject(field.validateRule)
  if (obj) {
    if (Array.isArray(obj.options) && obj.options.length) {
      return obj.options.map(normalizeOptionItem).filter(Boolean)
    }
    if (obj.operator === 'IN' && Array.isArray(obj.values) && obj.values.length) {
      return mergeOptionsWithValues(obj.values, obj.options || [])
    }
  }
  const literals = parseEnumLiteralsFromFieldType(field.fieldType)
  if (literals.length) {
    return literals.map(v => {
      const value = coerceOptionValue(v)
      return { value, label: String(v) }
    })
  }
  return []
}
