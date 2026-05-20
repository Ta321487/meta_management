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

/**
 * 是否为「状态列」风格校验：IN + options（应用 TINYINT 模板，不应使用 MySQL ENUM 类型）
 */
const STATUS_FIELD_TYPES = ['TINYINT', 'INT', 'SMALLINT']

export function isStatusIntegerFieldType(baseFieldType) {
  return STATUS_FIELD_TYPES.includes(baseFieldType)
}

/**
 * 状态值文本 → IN.values + options（格式：0:草稿,1:生效 或 0,1,2）
 */
export function parseStatusEntriesString(str) {
  const s = String(str || '')
    .replace(/，/g, ',')
    .trim()
  if (!s) {
    return { values: [], options: [] }
  }
  const values = []
  const options = []
  for (const part of s.split(',').map(p => p.trim()).filter(Boolean)) {
    const colon = part.indexOf(':')
    if (colon > 0) {
      const value = coerceOptionValue(part.slice(0, colon).trim())
      const label = part.slice(colon + 1).trim()
      values.push(value)
      options.push({ value, label: label || String(value) })
    } else {
      const value = coerceOptionValue(part)
      values.push(value)
      options.push({ value, label: String(part) })
    }
  }
  return { values, options: mergeOptionsWithValues(values, options) }
}

/** 从校验规则还原为状态值文本 */
export function formatStatusEntriesFromValidateRule(raw) {
  const items = resolveFieldOptionItems({ validateRule: raw })
  if (items.length) {
    return items
      .map(o => {
        const v = o.value
        const label = o.label != null ? String(o.label) : String(v)
        if (String(v) === label) {
          return String(v)
        }
        return `${v}:${label}`
      })
      .join(',')
  }
  const obj = parseValidateRuleObject(raw)
  if (obj?.operator === 'IN' && Array.isArray(obj.values) && obj.values.length) {
    return obj.values.map(v => String(v)).join(',')
  }
  return ''
}

export function isStatusStyleValidateRule(raw) {
  const obj = parseValidateRuleObject(raw)
  if (!obj || obj.operator !== 'IN' || !Array.isArray(obj.values) || !obj.values.length) {
    return false
  }
  return Array.isArray(obj.options) && obj.options.length > 0
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
