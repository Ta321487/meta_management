package com.metadata.service.constant;

/**
 * SQL执行相关常量
 */
public class SqlConstants {

    // SQL类型
    public static final String SQL_TYPE_SELECT = "SELECT";
    public static final String SQL_TYPE_INSERT = "INSERT";
    public static final String SQL_TYPE_UPDATE = "UPDATE";
    public static final String SQL_TYPE_DELETE = "DELETE";
    public static final String SQL_TYPE_CREATE = "CREATE";
    public static final String SQL_TYPE_ALTER = "ALTER";
    public static final String SQL_TYPE_DROP = "DROP";
    public static final String SQL_TYPE_TRUNCATE = "TRUNCATE";
    public static final String SQL_TYPE_SHOW = "SHOW";
    public static final String SQL_TYPE_DESC = "DESC";
    public static final String SQL_TYPE_DESCRIBE = "DESCRIBE";
    public static final String SQL_TYPE_RENAME = "RENAME";
    public static final String SQL_TYPE_GRANT = "GRANT";
    public static final String SQL_TYPE_REVOKE = "REVOKE";
    public static final String SQL_TYPE_FLUSH = "FLUSH";
    public static final String SQL_TYPE_RESET = "RESET";
    public static final String SQL_TYPE_LOAD = "LOAD";
    
    // 安全检查相关
    public static final String[] DANGEROUS_SQL_PREFIXES = {
        "DROP", "TRUNCATE", "DELETE FROM", "SHOW", "DESC", "DESCRIBE",
        "ALTER TABLE", "RENAME TABLE", "CREATE DATABASE", "DROP DATABASE",
        "GRANT", "REVOKE", "FLUSH", "RESET", "LOAD DATA", "SELECT INTO OUTFILE"
    };
    
    public static final String[] DANGEROUS_ALTER_KEYWORDS = {
        " DROP ", " DROP,", ",DROP ", " DROP",
        " RENAME ", " RENAME COLUMN",
        " DROP COLUMN", " DROP INDEX", " DROP CONSTRAINT"
    };
    
    // CHECK约束相关
    public static final String CHECK_CONSTRAINT_PREFIX = "CHECK";
    public static final String CONSTRAINT_TYPE_CHECK = "CHECK";
    
    // 正则表达式
    public static final String REGEX_LINE_COMMENT = "--.*$\\n?";
    public static final String REGEX_MULTI_LINE_COMMENT = "/\\*[\\s\\S]*?\\*/";
    public static final String REGEX_EXCESSIVE_WHITESPACE = "\\s+";
    
    // 元数据同步相关
    public static final String AUTO_INCREMENT = "auto_increment";
    public static final String COLUMN_IS_AUTOINCREMENT = "IS_AUTOINCREMENT";
    public static final String YES = "YES";
    
    // 校验规则相关
    public static final String VALIDATE_OPERATOR_IN = "IN";
    public static final String VALIDATE_OPERATOR_EQUAL = "=";
    public static final String VALIDATE_OPERATOR_NOT_EQUAL = "!";
    
    // 字段类型相关
    public static final String FIELD_TYPE_ENUM = "enum";
    public static final String FORM_COMPONENT_SELECT = "select";
    
    // 关联关系相关
    public static final String RELATION_TYPE_ONE_TO_MANY = "ONE_TO_MANY";
    public static final String RELATION_CODE_PREFIX = "REL_";
    public static final int MAX_RELATION_CODE_LENGTH = 50;
    
    // 日志相关
    public static final String LOG_MODULE_SQL_QUERY = "SQL_QUERY";
    public static final String LOG_MODULE_SQL_EXECUTE = "SQL_EXECUTE";
    public static final String LOG_MODULE_SYNC_START = "SYNC_START";
    public static final String LOG_MODULE_SYNC_TABLE_NAME = "SYNC_TABLE_NAME";
    public static final String LOG_MODULE_SYNC_COMPLETE = "SYNC_COMPLETE";
    public static final String LOG_MODULE_SYNC_FAILED = "SYNC_FAILED";
    public static final String LOG_MODULE_SYNC_FIELDS = "SYNC_FIELDS";
    public static final String LOG_MODULE_SYNC_EXCEPTION = "SYNC_EXCEPTION";
    public static final String LOG_MODULE_TABLE_NAME_MATCH = "TABLE_NAME_MATCH";
    public static final String LOG_MODULE_SYNC_TABLE_FIELDS_START = "SYNC_TABLE_FIELDS_START";
    public static final String LOG_MODULE_SYNC_TABLE_FIELDS_END = "SYNC_TABLE_FIELDS_END";
    public static final String LOG_MODULE_SYNC_TABLE = "SYNC_TABLE";
    public static final String LOG_MODULE_GET_PRIMARY_KEY = "GET_PRIMARY_KEY";
    public static final String LOG_MODULE_GET_CHECK_CONSTRAINTS = "GET_CHECK_CONSTRAINTS";
    public static final String LOG_MODULE_PARSE_CHECK_CONSTRAINT = "PARSE_CHECK_CONSTRAINT";
    public static final String LOG_MODULE_SET_VALIDATE_RULE = "SET_VALIDATE_RULE";
    public static final String LOG_MODULE_SET_ENUM_TYPE = "SET_ENUM_TYPE";
    public static final String LOG_MODULE_SYNC_FOREIGN_KEY = "SYNC_FOREIGN_KEY";
    public static final String LOG_MODULE_EXTRACT_FIELD_NAME = "EXTRACT_FIELD_NAME";
    
    // 结果相关
    public static final String RESULT_KEY_SUCCESS = "success";
    public static final String RESULT_KEY_MESSAGE = "message";
    public static final String RESULT_KEY_DATA = "data";
    public static final String RESULT_KEY_AFFECTED_ROWS = "affectedRows";
    public static final String RESULT_KEY_ROW_COUNT = "rowCount";
    public static final String RESULT_KEY_ERROR = "error";
    public static final String RESULT_KEY_ORIGINAL_ERROR = "originalError";
    public static final String RESULT_KEY_SQL = "sql";
    public static final String RESULT_KEY_RESULTS = "results";
    public static final String RESULT_KEY_TOTAL_COUNT = "totalCount";
    public static final String RESULT_KEY_SUCCESS_COUNT = "successCount";
    public static final String RESULT_KEY_FAIL_COUNT = "failCount";
    public static final String RESULT_KEY_TOTAL_CREATED = "totalCreated";
}
