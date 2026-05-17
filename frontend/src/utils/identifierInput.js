/** 元数据技术标识：字母、数字、下划线，长度 1–50 */
export const METADATA_CODE_PATTERN = /^[A-Za-z0-9_]{1,50}$/

/** 模块类型编码：大写字母开头 */
export const MODULE_TYPE_CODE_PATTERN = /^[A-Z][A-Z0-9_]*$/

const WHITESPACE_RE = /[\s\u3000]+/g
const MULTI_UNDERSCORE_RE = /_+/g
const EDGE_UNDERSCORE_RE = /^_+|_+$/g

/**
 * 将首尾空白去除，连续空白（含全角空格）转为单个下划线，并去掉首尾下划线。
 */
export function normalizeWhitespaceToUnderscore(value) {
  if (value == null) return ''
  let s = String(value).trim()
  if (!s) return ''
  s = s.replace(WHITESPACE_RE, '_').replace(MULTI_UNDERSCORE_RE, '_').replace(EDGE_UNDERSCORE_RE, '')
  return s
}

/** 元数据编码（表/字段/模块等）：规范化后转大写 */
export function normalizeMetadataCode(value) {
  return normalizeWhitespaceToUnderscore(value).toUpperCase()
}

/** 物理列名：规范化，保留大小写 */
export function normalizePhysicalColumnName(value) {
  return normalizeWhitespaceToUnderscore(value)
}

/** 模块类型编码：大写且以字母开头 */
export function normalizeModuleTypeCode(value) {
  let s = normalizeMetadataCode(value)
  if (!s) return ''
  if (!/^[A-Z]/.test(s)) {
    s = 'T_' + s
  }
  return s
}

/**
 * 失焦时写回表单字段（仅当值发生变化时更新）
 * @param {object} model 表单 reactive 对象
 * @param {string} prop 字段名
 * @param {'code'|'column'|'moduleType'} mode
 * @returns {boolean} 是否发生了规范化
 */
export function applyIdentifierBlur(model, prop, mode = 'code') {
  if (!model || !prop) return false
  const raw = model[prop]
  if (raw == null || raw === '') return false
  const normalized =
    mode === 'column'
      ? normalizePhysicalColumnName(raw)
      : mode === 'moduleType'
        ? normalizeModuleTypeCode(raw)
        : normalizeMetadataCode(raw)
  if (normalized !== raw) {
    model[prop] = normalized
    return true
  }
  return false
}

/** 批量规范化表单中的多个编码字段 */
export function normalizeFormCodes(form, props) {
  if (!form || !props?.length) return
  for (const { key, mode = 'code' } of props) {
    applyIdentifierBlur(form, key, mode)
  }
}

export function metadataCodeRules(label = '编码') {
  return [
    { required: true, message: `请输入${label}`, trigger: 'blur' },
    {
      pattern: METADATA_CODE_PATTERN,
      message: `${label}只能包含字母、数字和下划线，长度1-50`,
      trigger: 'blur'
    }
  ]
}

export function physicalColumnRules() {
  return [
    { required: true, message: '请输入字段名称', trigger: 'blur' },
    {
      pattern: METADATA_CODE_PATTERN,
      message: '字段名称只能包含字母、数字和下划线，长度1-50',
      trigger: 'blur'
    }
  ]
}

export function moduleTypeCodeRules() {
  return [
    { required: true, message: '请输入类型编码', trigger: 'blur' },
    {
      pattern: MODULE_TYPE_CODE_PATTERN,
      message: '编码须为大写字母、数字或下划线，且以字母开头',
      trigger: 'blur'
    }
  ]
}
