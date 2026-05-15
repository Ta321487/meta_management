package com.metadata.common.codes;

/**
 * 四位应用层业务码（与 HTTP 状态解耦，避免如 404 被误解为「页面不存在」）。
 * <p>
 * 分段约定（千位/百位表意，后两位递增扩展）：
 * <ul>
 *   <li>200 — 成功（保留与历史接口一致）</li>
 *   <li>10xx — 表（元数据表 / 物理表相关）</li>
 *   <li>11xx — 字段</li>
 *   <li>12xx — 业务系统</li>
 *   <li>13xx — 模块（预留）</li>
 *   <li>14xx — 表关联 / 外键同步等</li>
 *   <li>15xx — SQL 执行工具</li>
 *   <li>90xx — 认证 / 会话</li>
 *   <li>98xx — 通用请求与数据约束</li>
 *   <li>99xx — 系统内部错误</li>
 * </ul>
 */
public final class AppErrorCodes {

    private AppErrorCodes() {
    }

    /** 成功 */
    public static final int SUCCESS = 200;

    // ---------- 表 10xx ----------
    /** 表在元数据中不存在或未登记 */
    public static final int TABLE_NOT_FOUND = 1001;

    // ---------- 字段 11xx ----------
    public static final int FIELD_CODE_INVALID = 1101;
    public static final int FIELD_NOT_FOUND = 1102;
    public static final int FIELD_CODE_DUPLICATE = 1103;
    /** 不允许删除主键字段 */
    public static final int FIELD_PRIMARY_CANNOT_DELETE = 1104;
    /** 不允许删除表中最后一个字段 */
    public static final int FIELD_LAST_COLUMN_CANNOT_DELETE = 1105;
    public static final int FIELD_BATCH_IDS_EMPTY = 1106;
    public static final int FIELD_CONSTRAINT_PARAM_INCOMPLETE = 1107;
    /** 不允许直接删除主键约束 */
    public static final int FIELD_PRIMARY_CONSTRAINT_CANNOT_DROP = 1108;
    public static final int FIELD_NN_COLUMN_NOT_FOUND = 1109;
    public static final int FIELD_NN_TYPE_QUERY_FAILED = 1110;
    /** 字段域 DDL（ADD/MODIFY/DROP COLUMN）执行失败 */
    public static final int FIELD_ALTER_DDL_FAILED = 1111;
    /** 删除检查/唯一等约束失败 */
    public static final int FIELD_DELETE_CONSTRAINT_FAILED = 1112;
    public static final int FIELD_BATCH_TABLE_CODES_EMPTY = 1113;
    public static final int FIELD_BATCH_STATUS_PARAM_INVALID = 1114;
    /** 不允许禁用主键字段 */
    public static final int FIELD_PRIMARY_CANNOT_DISABLE = 1115;

    // ---------- 业务系统 12xx（预留） ----------
    public static final int BIZ_SYSTEM_NOT_FOUND = 1201;

    // ---------- 模块 13xx（预留） ----------
    public static final int MODULE_NOT_FOUND = 1301;

    // ---------- 关联 / 外键 14xx ----------
    /** 关联服务返回业务失败（success=false） */
    public static final int RELATION_OPERATION_FAILED = 1401;
    /** 外键同步过程异常 */
    public static final int RELATION_FK_SYNC_FAILED = 1402;

    // ---------- SQL 工具 15xx ----------
    public static final int SQL_TEXT_EMPTY = 1501;

    // ---------- 认证 90xx ----------
    public static final int AUTH_SESSION_REQUIRED = 9001;
    public static final int AUTH_LOGIN_FAILED = 9002;
    public static final int AUTH_PASSWORD_INCORRECT = 9003;

    // ---------- 通用 98xx ----------
    public static final int COMMON_BAD_REQUEST = 9801;
    /** 数据库约束冲突（非字段唯一明确场景） */
    public static final int COMMON_DB_CONSTRAINT = 9802;
    /** 代码生成失败 */
    public static final int CODEGEN_FAILED = 9803;

    // ---------- 系统 99xx ----------
    public static final int SYSTEM_INTERNAL = 9999;

    public static boolean isSuccess(Integer code) {
        return code != null && code == SUCCESS;
    }
}
