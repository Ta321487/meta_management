// 生成模拟数据的工具函数（与元数据 validationRules / 表单组件同源）

import { resolveFieldOptionItems } from './fieldOptionUtils'

function generateRandomDigitString(length) {
  let result = ''
  for (let i = 0; i < length; i++) {
    result += Math.floor(Math.random() * 10).toString()
  }
  return result
}

function randomInt(min, max) {
  const lo = Math.min(min, max)
  const hi = Math.max(min, max)
  return lo + Math.floor(Math.random() * (hi - lo + 1))
}

function parseValidateRuleInput(field) {
  if (!field) return null
  if (field.validationRules && typeof field.validationRules === 'object') {
    return field.validationRules
  }
  const raw = fieldValidateRuleRaw(field)
  if (!raw) return null
  if (typeof raw === 'object') return raw
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function fieldValidateRuleRaw(field) {
  return field?.validateRule ?? field?.field?.validateRule
}

function parseValidateRuleRaw(field) {
  const raw = fieldValidateRuleRaw(field)
  if (!raw) return null
  if (typeof raw === 'object') return raw
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

function normalizeOptionValue(opt) {
  if (opt == null) return null
  if (typeof opt === 'object' && opt.value !== undefined && opt.value !== null) return opt.value
  if (typeof opt === 'object' && opt.label !== undefined) return opt.label
  return opt
}

function pickFromOptionsList(options) {
  if (!Array.isArray(options) || options.length === 0) return null
  const item = options[Math.floor(Math.random() * options.length)]
  const v = normalizeOptionValue(item)
  return v != null && v !== '' ? v : null
}

function pickInOrOptions(vr, rawJson) {
  if (vr?.hasOperator && String(vr.operator).toUpperCase() === 'IN' && Array.isArray(vr.values) && vr.values.length) {
    return pickFromOptionsList(vr.values)
  }
  if (vr?.hasOptions && Array.isArray(vr.options) && vr.options.length) {
    return pickFromOptionsList(vr.options)
  }
  if (rawJson) {
    if (rawJson.operator === 'IN' && Array.isArray(rawJson.values) && rawJson.values.length) {
      return pickFromOptionsList(rawJson.values)
    }
    if (Array.isArray(rawJson)) {
      const inRule = rawJson.find(r => r?.operator === 'IN' && r.values?.length)
      if (inRule) return pickFromOptionsList(inRule.values)
    }
    if (Array.isArray(rawJson.options) && rawJson.options.length) {
      return pickFromOptionsList(rawJson.options)
    }
  }
  return null
}

function pickFromEnumFieldType(fieldType) {
  if (!fieldType) return null
  const ft = String(fieldType).toLowerCase()
  if (!ft.startsWith('enum(') || !ft.endsWith(')')) return null
  const inner = ft.slice(5, -1)
  const values = inner
    .split(',')
    .map(s => s.trim().replace(/^['"]|['"]$/g, ''))
    .filter(Boolean)
  if (!values.length) return null
  return values[Math.floor(Math.random() * values.length)]
}

/** 与 ZIP/预览同源：从 validateRule + fieldType 解析选项后随机取存库 value */
function pickFromResolvedFieldOptions(field) {
  const items = resolveFieldOptionItems({
    validateRule: fieldValidateRuleRaw(field),
    fieldType: field?.fieldType || field?.field?.fieldType
  })
  if (!items.length) return null
  const item = items[Math.floor(Math.random() * items.length)]
  return item.value
}

function formComponentOf(field) {
  return field?.formComponent || field?.field?.formComponent || 'input'
}

function fieldTypeOf(field) {
  return field?.fieldType || field?.field?.fieldType || ''
}

function fieldNameOf(field) {
  return (field?.fieldName || field?.field?.fieldName || '').toLowerCase()
}

function labelOf(field) {
  return field?.label || field?.field?.label || field?.fieldName || ''
}

function isNumericFieldType(fieldType, formComponent) {
  if (formComponent === 'number') return true
  const t = String(fieldType).toLowerCase()
  if (t.startsWith('tinyint(1)')) return false
  return /int|bigint|smallint|decimal|numeric|float|double|tinyint/.test(t)
}

function isStringFieldType(fieldType) {
  const t = String(fieldType).toLowerCase()
  return t.includes('char') || t.includes('text') || t === 'varchar'
}

function generateAlphanumericString(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let s = ''
  for (let i = 0; i < length; i++) {
    s += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return s
}

const NAME_SUFFIXES = ['科技', '贸易', '实业', '集团', '商贸', '供应链', '客户', '门店', '中心']
const NAME_PREFIXES = ['华东', '华南', '北方', '鑫源', '恒通', '盛达', '启航', '卓越']

function generateChineseLikeName(targetLen) {
  const base = NAME_PREFIXES[Math.floor(Math.random() * NAME_PREFIXES.length)]
    + NAME_SUFFIXES[Math.floor(Math.random() * NAME_SUFFIXES.length)]
  if (base.length >= targetLen) return base.slice(0, targetLen)
  const pad = generateAlphanumericString(Math.max(0, targetLen - base.length))
  return (base + pad).slice(0, targetLen)
}

function generateStringForLengthRules(vr, field, dbLen) {
  const minLen = Math.max(1, Number(vr.minLength) || 1)
  const cap = Number(dbLen) || Number(vr.maxLength) || 255
  const maxLen = Math.min(Math.max(minLen, Number(vr.maxLength) || cap), cap)
  const targetLen = randomInt(minLen, maxLen)

  const name = fieldNameOf(field)
  const label = labelOf(field)

  if (name.includes('code') || label.includes('编码')) {
    return generateRandomDigitString(targetLen)
  }
  if (label.includes('名称') || label.includes('姓名') || name.includes('name')) {
    return generateChineseLikeName(targetLen)
  }
  if (name.includes('email') || name.includes('mail')) {
    const user = generateAlphanumericString(Math.min(10, targetLen - 5))
    const domain = 'example.com'
    const full = `${user}@${domain}`
    return full.length > targetLen ? full.slice(0, targetLen) : full.padEnd(targetLen, 'x').slice(0, targetLen)
  }
  return generateChineseLikeName(Math.min(targetLen, 32))
}

function tryPatternFromRules(vr, rawJson, field) {
  let pattern = ''
  if (vr?.hasPattern && vr.pattern) {
    pattern = vr.pattern
  } else if (rawJson?.pattern) {
    pattern = rawJson.pattern
  } else if (Array.isArray(rawJson)) {
    const regexRule = rawJson.find(rule => rule?.pattern)
    if (regexRule) pattern = regexRule.pattern
  }

  if (!pattern) return null

  const normalizedPattern = String(pattern).replace(/\\\\/g, '\\')
  const name = fieldNameOf(field)

  const digitMatch = normalizedPattern.match(/\\d\{(\d+)\}/)
  if (digitMatch) {
    return generateRandomDigitString(parseInt(digitMatch[1], 10))
  }
  if (normalizedPattern.includes('1[3-9]\\d{9}') || normalizedPattern.includes('1[3-9]')) {
    const prefixes = ['13', '14', '15', '16', '17', '18', '19']
    const prefix = prefixes[Math.floor(Math.random() * prefixes.length)]
    const suffix = Math.floor(100000000 + Math.random() * 900000000).toString()
    return `${prefix}${suffix}`
  }
  if (normalizedPattern.includes('@') && !name.includes('password') && !name.includes('pwd')) {
    const username = Math.random().toString(36).substr(2, 10)
    const domains = ['example.com', 'test.com', 'demo.com']
    return `${username}@${domains[Math.floor(Math.random() * domains.length)]}`
  }
  if (normalizedPattern === '^-?\\d+$' || /^-?\d+$/.test(normalizedPattern)) {
    return randomInt(1, 999)
  }
  if (normalizedPattern === '^-?\\d+(\\.\\d+)?$' || /^-?\d+(\.\d+)?$/.test(normalizedPattern)) {
    const fieldType = fieldTypeOf(field)
    return generateNumericInRange(0, 1000, fieldType)
  }
  return null
}

function generateNumericInRange(min, max, fieldType) {
  const defaultMin = 0
  const defaultMax = 1000
  const finalMin = min != null && min !== '' ? Number(min) : defaultMin
  const finalMax = max != null && max !== '' ? Number(max) : defaultMax
  const actualMin = Math.min(finalMin, finalMax)
  const actualMax = Math.max(finalMin, finalMax)
  const type = String(fieldType).toLowerCase()

  if (type.includes('decimal') || type.includes('numeric') || type.includes('float') || type.includes('double')) {
    const n = Math.random() * (actualMax - actualMin) + actualMin
    return Number.parseFloat(n.toFixed(4))
  }
  return Math.floor(Math.random() * (actualMax - actualMin + 1)) + actualMin
}

function extractNumericRange(vr, rawJson) {
  let min = null
  let max = null
  const apply = rule => {
    if (!rule || typeof rule !== 'object') return
    if (rule.min !== undefined && rule.min !== null && rule.min !== '') min = Number(rule.min)
    if (rule.max !== undefined && rule.max !== null && rule.max !== '') max = Number(rule.max)
  }
  if (vr?.hasRange) {
    if (vr.min != null) min = Number(vr.min)
    if (vr.max != null) max = Number(vr.max)
  }
  if (rawJson && (min == null && max == null)) {
    if (Array.isArray(rawJson)) rawJson.forEach(apply)
    else apply(rawJson)
  }
  if (min == null && max == null) return null
  return { min, max }
}

/** 与生成器 prepareFieldList 一致的列表属性名 */
export function resolveListFieldProp(field) {
  if (!field) return ''
  if (field.camelCaseName) return field.camelCaseName
  const raw = field.field?.fieldName || field.fieldName || ''
  if (!raw) return ''
  if (!raw.includes('_')) {
    return raw.charAt(0).toLowerCase() + raw.slice(1)
  }
  return raw
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part, i) => (i === 0 ? part : part.charAt(0).toUpperCase() + part.slice(1)))
    .join('')
}

export function generateMockValue(field) {
  const vr = parseValidateRuleInput(field)
  const rawJson = parseValidateRuleRaw(field)
  const fc = formComponentOf(field)
  const fieldType = fieldTypeOf(field)
  const fieldName = fieldNameOf(field)
  const label = labelOf(field)
  const dbLen = field.fieldLength ?? field.field?.fieldLength ?? 255

  // 1. 下拉 / IN / options（与 ZIP 同源解析，只写入存库 value）
  const fromResolved = pickFromResolvedFieldOptions(field)
  if (fromResolved != null && fromResolved !== '') {
    return fromResolved
  }
  const fromPick = pickInOrOptions(vr, rawJson)
  if (fromPick != null && fromPick !== '') {
    return fromPick
  }
  if (fc === 'select') {
    const enumVal = pickFromEnumFieldType(fieldType)
    if (enumVal != null && enumVal !== '') return enumVal
  }

  // 2. 数据库 ENUM 类型
  const fromEnumType = pickFromEnumFieldType(fieldType)
  if (fromEnumType != null && fromEnumType !== '') {
    return fromEnumType
  }

  // 3. 正则（\d{n}、手机号、邮箱等）
  const fromPattern = tryPatternFromRules(vr, rawJson, field)
  if (fromPattern != null && fromPattern !== '') {
    return fromPattern
  }

  // 4. 数值范围（仅数字类型；VARCHAR 上的 min/max 不当作数值）
  const range = extractNumericRange(vr, rawJson)
  if (range && isNumericFieldType(fieldType, fc)) {
    return generateNumericInRange(range.min, range.max, fieldType)
  }

  // 5. 字符串长度 minLength / maxLength
  if (isStringFieldType(fieldType) && vr?.hasLength) {
    return generateStringForLengthRules(vr, field, dbLen)
  }

  // 6. 字段名启发（无明确 options 时）
  if (fieldName.includes('password') || fieldName.includes('pwd') || fieldName.includes('密码')) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%'
    let password = ''
    for (let i = 0; i < 8; i++) password += chars.charAt(Math.floor(Math.random() * chars.length))
    return password
  }
  if (fieldName.includes('email') || fieldName.includes('mail')) {
    const username = Math.random().toString(36).substr(2, 10)
    return `${username}@example.com`
  }
  if (fieldName.includes('gender') || fieldName.includes('sex')) {
    return Math.random() > 0.5 ? '男' : '女'
  }
  if ((fieldName.includes('status') || label.includes('状态')) && fc !== 'select') {
    return ['启用', '停用', '待审核'][Math.floor(Math.random() * 3)]
  }
  if (fieldName.includes('phone') || fieldName.includes('mobile')) {
    const prefixes = ['13', '15', '18']
    return `${prefixes[Math.floor(Math.random() * prefixes.length)]}${Math.floor(100000000 + Math.random() * 900000000)}`
  }
  // 仅字符串类编码/单号字段走数字串；line_no、order_no 等数值列走上面的 range/类型分支
  if ((fieldName.includes('code') || fieldName.endsWith('_no')) && isStringFieldType(fieldType)) {
    const n = Math.min(Number(dbLen) || 10, 20)
    return generateRandomDigitString(n)
  }

  const type = String(fieldType).toLowerCase()
  if (!fieldType) return ''

  if (isStringFieldType(fieldType)) {
    const len = Math.min(Number(dbLen) || 20, 32)
    if (label.includes('名称') || fieldName.includes('name')) {
      return generateChineseLikeName(Math.max(2, Math.min(len, 16)))
    }
    const prefix = label ? `示例${String(label).slice(0, 4)}` : '模拟'
    const body = generateAlphanumericString(Math.max(1, len - prefix.length))
    return (prefix + body).slice(0, len)
  }

  if (type.includes('int') || type.includes('bigint') || type.includes('smallint')) {
    return Math.floor(Math.random() * 1000)
  }
  if (type.includes('decimal') || type.includes('numeric') || type.includes('float') || type.includes('double')) {
    return (Math.random() * 1000).toFixed(2)
  }
  if (type.includes('date') && !type.includes('time')) {
    return '2023-01-01'
  }
  if (type.includes('time') && !type.includes('date')) {
    return '12:00:00'
  }
  if (type.includes('datetime') || type.includes('timestamp')) {
    return '2023-01-01 12:00:00'
  }
  if (type.includes('bool') || type.includes('boolean') || type === 'bit' || type.startsWith('tinyint(1)')) {
    return Math.random() > 0.5 ? 1 : 0
  }

  return `示例${label}${Math.floor(Math.random() * 1000)}`
}

/** 将元数据/生成器字段转为 mock 生成器可用的扁平结构 */
export function toMockFieldShape(field) {
  if (!field) return {}
  const meta = field.field || field
  return {
    fieldName: resolveListFieldProp(field) || meta.fieldName || field.fieldName,
    fieldType: field.fieldType || meta.fieldType,
    fieldLength: meta.fieldLength ?? field.fieldLength,
    validateRule: meta.validateRule ?? field.validateRule ?? field.field?.validateRule,
    validationRules: field.validationRules || meta.validationRules,
    formComponent: meta.formComponent || field.formComponent,
    label: field.label || meta.label || field.label
  }
}

export function generateMockDataList(fields, count = 10) {
  const dataList = []
  for (let i = 0; i < count; i++) {
    const row = {}
    fields.forEach(field => {
      const mockField = toMockFieldShape(field)
      const propName = resolveListFieldProp(field) || getListFieldPropName(mockField) || mockField.fieldName
      if (!propName) return
      row[propName] = generateMockValue(mockField)
    })
    dataList.push(row)
  }
  return dataList
}

export function formatPreviewDate(dateStr) {
  if (!dateStr) return ''
  return dateStr
}

export function formatPreviewNumber(num) {
  if (num === undefined || num === null) return ''
  return num.toString()
}

export function getListFieldPropName(field) {
  if (!field) return ''
  if (field.camelCaseName) return field.camelCaseName
  const raw = field.field?.fieldName || field.fieldName || ''
  if (!raw) return ''
  if (raw.includes('_')) return resolveListFieldProp(field)
  return raw.replace(/^[A-Z]/, char => char.toLowerCase()) || raw
}

export function getFormFieldPropName(field) {
  if (!field) return ''
  return field.fieldName?.replace(/^[A-Z]/, char => char.toLowerCase()) || field.fieldName
}

export function getFormFieldOptions(field) {
  if (!field) return []
  const vr = parseValidateRuleInput(field)
  if (vr?.hasOperator && vr.operator === 'IN' && Array.isArray(vr.values)) {
    return vr.values.map(value =>
      typeof value === 'object' ? value : { label: value, value }
    )
  }
  if (vr?.hasOptions && Array.isArray(vr.options)) {
    return vr.options.map(o =>
      typeof o === 'object' ? o : { label: o, value: o }
    )
  }
  if (field.validateRule) {
    try {
      const validateRule = JSON.parse(field.validateRule)
      if (validateRule.operator === 'IN' && validateRule.values) {
        return validateRule.values.map(value => ({ label: value, value }))
      }
      if (Array.isArray(validateRule)) {
        for (const rule of validateRule) {
          if (rule.operator === 'IN' && rule.values) {
            return rule.values.map(value => ({ label: value, value }))
          }
        }
      }
    } catch (e) {
      console.error('Failed to parse validateRule:', e)
    }
  }
  if (field.enumValues) {
    try {
      return JSON.parse(field.enumValues)
    } catch (e) {
      console.error('Failed to parse enumValues:', e)
    }
  }
  return []
}

export function getRelatedDataOptions(relatedTableName) {
  if (!relatedTableName) {
    return []
  }
  const tableName = relatedTableName.toLowerCase()
  const options = []
  const count = 4

  if (tableName.includes('user') || tableName.includes('owner') || tableName.includes('person') || tableName.includes('member')) {
    const names = ['张三', '李四', '王五', '赵六']
    for (let i = 0; i < count; i++) options.push({ id: i + 1, name: names[i] })
  } else if (tableName.includes('type') || tableName.includes('category')) {
    for (let i = 0; i < count; i++) options.push({ id: i + 1, name: `类型${i + 1}` })
  } else if (tableName.includes('status') || tableName.includes('state')) {
    const statuses = ['启用', '禁用', '待审核', '已审核']
    for (let i = 0; i < count; i++) options.push({ id: i + 1, name: statuses[i] })
  } else {
    for (let i = 0; i < count; i++) {
      options.push({
        id: i + 1,
        name: `${relatedTableName.replace('_TABLE', '')}_${i + 1}`
      })
    }
  }
  return options
}
