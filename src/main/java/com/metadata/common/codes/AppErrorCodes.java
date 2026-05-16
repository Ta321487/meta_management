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
 *   <li>16xx — 物理库登记</li>
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
    public static final int TABLE_CODE_INVALID = 1002;
    public static final int TABLE_CODE_DUPLICATE = 1003;
    public static final int TABLE_CREATE_DDL_FAILED = 1004;
    public static final int TABLE_PK_STRATEGY_IMMUTABLE = 1005;
    public static final int TABLE_BATCH_IDS_EMPTY = 1006;
    public static final int TABLE_BATCH_CODES_EMPTY = 1007;
    public static final int TABLE_PARAM_INVALID = 1008;

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

    // ---------- 业务系统 12xx ----------
    public static final int BIZ_SYSTEM_NOT_FOUND = 1201;
    public static final int BIZ_SYSTEM_DEFAULT_CANNOT_DELETE = 1202;
    public static final int BIZ_SYSTEM_MODULE_CODES_EMPTY = 1203;
    /** 补充物理库时未选择库名 */
    public static final int BIZ_SYSTEM_PHYSICAL_CATALOG_REQUIRED = 1204;
    /** 物理库名不符合安全规则 */
    public static final int BIZ_SYSTEM_PHYSICAL_CATALOG_INVALID = 1205;

    // ---------- 模块 13xx ----------
    public static final int MODULE_NOT_FOUND = 1301;
    public static final int MODULE_CODE_INVALID = 1302;
    public static final int MODULE_CODE_DUPLICATE = 1303;
    public static final int MODULE_OBJECT_REQUIRED = 1304;
    public static final int MODULE_BATCH_IDS_EMPTY = 1305;
    public static final int MODULE_CODE_LIST_EMPTY = 1306;
    public static final int MODULE_TYPE_CODE_REQUIRED = 1311;
    public static final int MODULE_TYPE_NAME_REQUIRED = 1312;
    public static final int MODULE_TYPE_CODE_DUPLICATE = 1313;
    public static final int MODULE_TYPE_ID_REQUIRED = 1314;
    public static final int MODULE_TYPE_CODE_CONFLICT = 1315;
    public static final int MODULE_TYPE_BATCH_IDS_EMPTY = 1316;

    // ---------- 关联 / 外键 14xx ----------
    /** 关联服务返回业务失败（success=false） */
    public static final int RELATION_OPERATION_FAILED = 1401;
    /** 外键同步过程异常 */
    public static final int RELATION_FK_SYNC_FAILED = 1402;
    public static final int RELATION_NOT_FOUND = 1403;
    public static final int RELATION_CODE_DUPLICATE = 1404;
    public static final int RELATION_CODE_INVALID = 1405;
    public static final int RELATION_BUSINESS_SYSTEM_NOT_CONFIGURED = 1406;
    public static final int RELATION_CROSS_BUSINESS_SYSTEM = 1407;
    public static final int RELATION_TABLE_NOT_FOUND = 1408;
    public static final int RELATION_TABLE_DISABLED = 1409;
    public static final int RELATION_FIELD_NOT_FOUND = 1410;

    // ---------- SQL 工具 15xx ----------
    public static final int SQL_TEXT_EMPTY = 1501;
    public static final int SQL_EXECUTE_FAILED = 1502;
    public static final int SQL_TARGET_CATALOG_REQUIRED = 1503;

    // ---------- 物理库登记 16xx ----------
    /** 物理库登记记录不存在 */
    public static final int PHYSICAL_DB_REGISTRATION_NOT_FOUND = 1601;
    /** 物理库名非法或校验失败 */
    public static final int PHYSICAL_DB_CATALOG_INVALID = 1602;
    /** 库名已登记 */
    public static final int PHYSICAL_DB_NAME_DUPLICATE = 1603;
    /** 级联删除表或元数据时失败（事务将回滚） */
    public static final int PHYSICAL_DB_CASCADE_FAILED = 1604;

    /** 在实例上删除物理库失败 */
    public static final int PHYSICAL_DB_DROP_FAILED = 1605;
    public static final int PHYSICAL_DB_SYSTEM_CATALOG_PROTECTED = 1606;
    public static final int PHYSICAL_DB_METADATA_CATALOG_PROTECTED = 1607;
    public static final int PHYSICAL_DB_CREATE_FAILED = 1608;
    /** 物理库在 MySQL 实例上不存在（未自动建库） */
    public static final int PHYSICAL_DB_CATALOG_NOT_ON_INSTANCE = 1609;

    // ---------- 功能节点 17xx ----------
    public static final int FUNCTION_NODE_CODE_INVALID = 1701;
    public static final int FUNCTION_NODE_NOT_FOUND = 1702;

    // ---------- 业务规则 18xx ----------
    public static final int BUSINESS_RULE_CODE_INVALID = 1801;
    public static final int BUSINESS_RULE_NOT_FOUND = 1802;
    public static final int BUSINESS_RULE_MODULE_NO_TABLE = 1803;
    public static final int BUSINESS_RULE_FIELD_MISMATCH = 1804;

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
