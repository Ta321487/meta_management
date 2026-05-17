export function fc(field) {
  return field?.formComponent || field?.field?.formComponent || 'input'
}

export function fieldLabel(field) {
  return field?.label || field?.field?.label || field?.camelCaseName || ''
}

export function findStatusField(fields) {
  return (fields || []).find(f => {
    const name = (f.fieldName || f.field?.fieldName || '').toLowerCase()
    return /status|approve|process|flow/.test(name)
  }) || null
}

export function findFirstSelectField(fields) {
  return (fields || []).find(f => fc(f) === 'select') || null
}

export function nonPkFields(fields) {
  return (fields || []).filter(f => fc(f) !== 'primary_key')
}

function mapOptionEntry(o) {
  if (typeof o === 'string' || typeof o === 'number') return { label: String(o), value: o }
  return { label: o.label ?? o.value, value: o.value ?? o.label }
}

export function selectOptions(field) {
  const vr = field?.validationRules || {}
  if (vr.hasOptions && Array.isArray(vr.options) && vr.options.length) {
    return vr.options.map(mapOptionEntry)
  }
  if (vr.hasOperator && String(vr.operator).toUpperCase() === 'IN' && Array.isArray(vr.values) && vr.values.length) {
    return vr.values.map(mapOptionEntry)
  }
  return []
}

function fieldNameOf(field) {
  return (field?.fieldName || field?.field?.fieldName || '').toLowerCase()
}

/** 是否启用/软删等二值字段（无 options 时默认 0/1） */
export function isBinarySwitchFieldName(fieldName) {
  const n = String(fieldName || '').toLowerCase()
  return n === 'enabled' || n === 'is_enabled' || n === 'is_enable' || n === 'is_deleted'
}

/** 二值开关：表单/列表用 el-switch；多值状态用标签 */
export function resolveSwitchMeta(field) {
  let opts = selectOptions(field)
  if (opts.length === 0 && isBinarySwitchFieldName(fieldNameOf(field))) {
    opts = [
      { label: '否', value: 0 },
      { label: '是', value: 1 }
    ]
  }
  if (opts.length !== 2) return null
  const sorted = [...opts].sort((a, b) => {
    const na = Number(a.value)
    const nb = Number(b.value)
    if (!Number.isNaN(na) && !Number.isNaN(nb)) return na - nb
    return String(a.value).localeCompare(String(b.value))
  })
  return {
    inactiveValue: sorted[0].value,
    activeValue: sorted[1].value,
    inactiveLabel: sorted[0].label,
    activeLabel: sorted[1].label
  }
}

export function useSwitchDisplay(field) {
  if (resolveSwitchMeta(field)) return true
  const t = fc(field)
  return isBinarySwitchFieldName(fieldNameOf(field)) && (t === 'select' || t === 'number')
}

export function useTagDisplay(field) {
  if (useSwitchDisplay(field)) return false
  return selectOptions(field).length >= 2 && fc(field) === 'select'
}

export function labelForOptionValue(field, value) {
  if (value === null || value === undefined || value === '') return '—'
  const opts = selectOptions(field)
  const hit = opts.find(o => o.value === value || String(o.value) === String(value))
  return hit ? hit.label : String(value)
}

export function tagTypeForOptionValue(field, value) {
  const text = labelForOptionValue(field, value).toLowerCase()
  if (/草稿|draft|待/.test(text)) return 'info'
  if (/生效|确认|启用|正常|通过|是/.test(text)) return 'success'
  if (/作废|取消|禁用|删除|关闭|驳回|否/.test(text)) return 'danger'
  if (/警告|暂停/.test(text)) return 'warning'
  return 'info'
}
