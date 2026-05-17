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

export function selectOptions(field) {
  const vr = field?.validationRules || {}
  if (!vr.hasOptions || !vr.options) return []
  return (vr.options || []).map(o =>
    typeof o === 'string' ? { label: o, value: o } : { label: o.label ?? o.value, value: o.value ?? o.label }
  )
}
