package com.metadata.common.codes;

/**
 * 字段域接口文案与错误前缀，供字段相关 Service 等使用。
 */
public final class FieldMessages {

    private FieldMessages() {
    }

    public static final String FIELD_CODE_INVALID = "字段编码格式不正确";

    public static final String FIELD_NOT_FOUND = "字段不存在";

    public static final String TABLE_NOT_FOUND = "表不存在或未登记";

    public static final String ALTER_ADD_COLUMN_FAILED_PREFIX = "执行ALTER TABLE ADD COLUMN失败: ";

    public static final String ALTER_TABLE_OP_FAILED_PREFIX = "执行ALTER TABLE操作失败: ";

    /** 参数1：操作类型（如 ALTER_TABLE_MODIFY_COLUMN）；参数2：详情 */
    public static final String ALTER_NAMED_OP_FAILED_FMT = "执行%s失败: %s";

    public static final String ALTER_DROP_COLUMN_FAILED_PREFIX = "执行ALTER TABLE DROP COLUMN失败: ";

    public static final String PRIMARY_KEY_CANNOT_DELETE = "主键字段不允许删除";

    public static final String CANNOT_DELETE_LAST_FIELD = "不能删除表中最后一个字段";

    public static final String BATCH_DELETE_IDS_EMPTY = "删除ID列表不能为空";

    public static final String CONSTRAINT_INFO_INCOMPLETE = "缺少必要的约束信息";

    public static final String PRIMARY_CONSTRAINT_CANNOT_DROP = "主键约束不能被直接删除，若要修改主键请重新设计表结构";

    public static final String NN_COLUMN_NOT_FOUND = "未找到字段信息，无法删除非空约束";

    public static final String NN_COLUMN_TYPE_QUERY_FAILED = "查询字段类型失败，无法删除非空约束";

    public static final String DELETE_CONSTRAINT_FAILED_PREFIX = "删除约束失败: ";

    public static final String TABLE_CODES_EMPTY = "表编码列表不能为空";

    public static final String BATCH_STATUS_PARAM_EMPTY = "参数不能为空";

    public static final String PRIMARY_KEY_CANNOT_DISABLE = "主键字段不允许禁用";

    public static final String FIELD_MIGRATE_PRIMARY = "主键字段不允许迁移";

    public static final String FIELD_MIGRATE_SAME_TABLE = "目标表不能与源表相同";

    public static final String FIELD_MIGRATE_TARGET_FIELD_CODE_EXISTS = "目标表已存在相同字段编码";

    public static final String FIELD_MIGRATE_TARGET_FIELD_NAME_EXISTS = "目标表已存在相同物理列名";

    public static final String FIELD_MIGRATE_DATA_CATALOG_MISMATCH = "源表与目标表不在同一物理库，无法自动迁移数据，请取消「迁移数据」或先在同一库内调整表配置";

    public static final String FIELD_MIGRATE_TARGET_TABLE_NOT_FOUND = "目标表不存在或未登记";

    public static final String FIELD_MIGRATE_JOIN_INVALID = "关联列名格式不正确";

    public static String alterNamedOpFailed(String operationType, Object detail) {
        String d = detail != null ? detail.toString() : "";
        return String.format(ALTER_NAMED_OP_FAILED_FMT, operationType, d);
    }
}
