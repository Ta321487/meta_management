/**
 * 与后端 AppErrorCodes 对齐的应用层业务码（非 HTTP 状态码）。
 * 分段：10xx 表、11xx 字段、12xx 业务系统、14xx 关联、15xx SQL、16xx 物理库登记、90xx 认证、98xx 通用、99xx 系统。
 */
export const RESULT_OK = 200

export const TABLE_NOT_FOUND = 1001

export const FIELD_CODE_INVALID = 1101
export const FIELD_NOT_FOUND = 1102
export const FIELD_CODE_DUPLICATE = 1103
export const FIELD_PRIMARY_CANNOT_DELETE = 1104
export const FIELD_LAST_COLUMN_CANNOT_DELETE = 1105
export const FIELD_BATCH_IDS_EMPTY = 1106
export const FIELD_CONSTRAINT_PARAM_INCOMPLETE = 1107
export const FIELD_PRIMARY_CONSTRAINT_CANNOT_DROP = 1108
export const FIELD_NN_COLUMN_NOT_FOUND = 1109
export const FIELD_NN_TYPE_QUERY_FAILED = 1110
export const FIELD_ALTER_DDL_FAILED = 1111
export const FIELD_DELETE_CONSTRAINT_FAILED = 1112
export const FIELD_BATCH_TABLE_CODES_EMPTY = 1113
export const FIELD_BATCH_STATUS_PARAM_INVALID = 1114
export const FIELD_PRIMARY_CANNOT_DISABLE = 1115

export const BIZ_SYSTEM_NOT_FOUND = 1201

export const MODULE_NOT_FOUND = 1301

export const RELATION_OPERATION_FAILED = 1401
export const RELATION_FK_SYNC_FAILED = 1402

export const SQL_TEXT_EMPTY = 1501

export const PHYSICAL_DB_REGISTRATION_NOT_FOUND = 1601
export const PHYSICAL_DB_CATALOG_INVALID = 1602
export const PHYSICAL_DB_NAME_DUPLICATE = 1603
export const PHYSICAL_DB_CASCADE_FAILED = 1604
export const PHYSICAL_DB_DROP_FAILED = 1605

/** 未登录或会话失效（JSON body 业务码，非浏览器「页面 404」） */
export const AUTH_SESSION_REQUIRED = 9001
export const AUTH_LOGIN_FAILED = 9002
export const AUTH_PASSWORD_INCORRECT = 9003

export const COMMON_BAD_REQUEST = 9801
export const COMMON_DB_CONSTRAINT = 9802
export const CODEGEN_FAILED = 9803

export const SYSTEM_INTERNAL = 9999

/** 兼容旧版：HTTP 401 响应体曾使用 401 */
export const LEGACY_UNAUTHORIZED_BODY_CODE = 401

/**
 * 是否应跳转登录页（与 axios 响应体 code 一致）
 * @param {number} code
 */
export function isAuthFailureCode(code) {
  return code === AUTH_SESSION_REQUIRED || code === LEGACY_UNAUTHORIZED_BODY_CODE
}
