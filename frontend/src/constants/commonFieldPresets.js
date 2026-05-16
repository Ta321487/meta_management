/**
 * 按表一键添加的常用审计/软删字段（物理列名业界通用；fieldCode 随表编码生成前缀）。
 */

/** @typedef {{ key: string, fieldName: string, codeSuffix: string, label: string, fieldType: string, isRequired: number, inForm: number, formComponent: string, validateRule: string|null, sortStep: number, defaultSelected?: boolean }} PresetDef */

/** @type {PresetDef[]} */
export const COMMON_FIELD_PRESET_DEFS = [
  {
    key: 'create_time',
    fieldName: 'create_time',
    codeSuffix: 'F_CT',
    label: '创建时间',
    fieldType: 'DATETIME',
    isRequired: 0,
    inForm: 0,
    formComponent: 'none',
    validateRule: null,
    sortStep: 10,
    defaultSelected: true
  },
  {
    key: 'update_time',
    fieldName: 'update_time',
    codeSuffix: 'F_UT',
    label: '更新时间',
    fieldType: 'DATETIME',
    isRequired: 0,
    inForm: 0,
    formComponent: 'none',
    validateRule: null,
    sortStep: 20,
    defaultSelected: true
  },
  {
    key: 'is_deleted',
    fieldName: 'is_deleted',
    codeSuffix: 'F_DEL',
    label: '是否删除',
    fieldType: 'TINYINT',
    isRequired: 1,
    inForm: 0,
    formComponent: 'none',
    validateRule: JSON.stringify({
      type: 'number',
      min: 0,
      max: 1,
      message: '0否1是',
      trigger: 'blur'
    }),
    sortStep: 30,
    defaultSelected: true
  },
  {
    key: 'create_by',
    fieldName: 'create_by',
    codeSuffix: 'F_CBY',
    label: '创建人',
    fieldType: 'VARCHAR(64)',
    isRequired: 0,
    inForm: 0,
    formComponent: 'none',
    validateRule: null,
    sortStep: 40,
    defaultSelected: false
  },
  {
    key: 'update_by',
    fieldName: 'update_by',
    codeSuffix: 'F_UBY',
    label: '更新人',
    fieldType: 'VARCHAR(64)',
    isRequired: 0,
    inForm: 0,
    formComponent: 'none',
    validateRule: null,
    sortStep: 50,
    defaultSelected: false
  }
]

/**
 * 由表编码推导字段编码前缀（与模拟清单 MC、SO、SAL 等一致：各段首字母）。
 * @param {string} tableCode
 */
export function deriveTableFieldPrefix(tableCode) {
  if (!tableCode || typeof tableCode !== 'string') return 'T'
  const parts = tableCode.split('_').filter(Boolean)
  if (parts.length === 0) return 'T'
  const fromInitials = parts.map((p) => (p[0] || '').toUpperCase()).join('')
  if (fromInitials.length >= 2) return fromInitials.slice(0, 8)
  return parts[0].slice(0, 4).toUpperCase()
}

/**
 * @param {string} tableCode
 * @param {string} codeSuffix 如 F_CT
 */
export function buildPresetFieldCode(tableCode, codeSuffix) {
  const prefix = deriveTableFieldPrefix(tableCode)
  const suffix = codeSuffix.startsWith('_') ? codeSuffix.slice(1) : codeSuffix
  if (suffix.startsWith('F_')) {
    return `${prefix}_${suffix}`
  }
  return `${prefix}_F_${suffix}`
}

/**
 * @param {string} tableCode
 * @param {Array<{ fieldCode?: string, fieldName?: string }>} existingFields 元数据已有字段
 * @param {string[]} physicalColumnNames 业务库物理表已有列名
 */
export function buildCommonFieldPresetRows(tableCode, existingFields = [], physicalColumnNames = []) {
  const codes = new Set(
    (existingFields || []).map((f) => (f.fieldCode || '').toUpperCase()).filter(Boolean)
  )
  const names = new Set(
    (existingFields || []).map((f) => (f.fieldName || '').toLowerCase()).filter(Boolean)
  )
  const physical = new Set(
    (physicalColumnNames || []).map((n) => String(n).toLowerCase()).filter(Boolean)
  )

  return COMMON_FIELD_PRESET_DEFS.map((def) => {
    const fieldCode = buildPresetFieldCode(tableCode, def.codeSuffix)
    const nameKey = def.fieldName.toLowerCase()
    const metadataExists = codes.has(fieldCode.toUpperCase()) || names.has(nameKey)
    const physicalExists = physical.has(nameKey)

    let status
    let selectable
    if (metadataExists) {
      status = '已登记'
      selectable = false
    } else if (physicalExists) {
      status = '仅补元数据'
      selectable = true
    } else {
      status = '可添加'
      selectable = true
    }

    return {
      ...def,
      fieldCode,
      metadataExists,
      physicalExists,
      selectable,
      status
    }
  })
}

/**
 * @param {string} tableCode
 * @param {string} businessCode
 * @param {PresetDef & { fieldCode: string }} presetRow
 * @param {number} sort
 */
export function toAddFieldPayload(tableCode, businessCode, presetRow, sort) {
  const payload = {
    tableCode,
    businessCode: businessCode || 'DEFAULT',
    fieldCode: presetRow.fieldCode,
    fieldName: presetRow.fieldName,
    fieldType: presetRow.fieldType,
    label: presetRow.label,
    isRequired: presetRow.isRequired,
    inForm: presetRow.inForm,
    formComponent: presetRow.inForm === 0 ? 'none' : presetRow.formComponent,
    validateRule: presetRow.validateRule,
    sort,
    isEnabled: 1
  }
  if (payload.validateRule === '{}' || payload.validateRule === '{\n}') {
    payload.validateRule = null
  }
  return payload
}
