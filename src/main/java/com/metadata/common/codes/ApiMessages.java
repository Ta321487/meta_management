package com.metadata.common.codes;

/**
 * 常用接口文案（中文），与 {@link com.metadata.common.Result} 及全局异常处理配合使用。
 */
public final class ApiMessages {

    private ApiMessages() {
    }

    public static final String OPERATION_SUCCESS = "操作成功";

    public static final String REQUEST_FAILED = "请求失败";

    public static final String BAD_REQUEST = "请求参数不正确";

    public static final String NOT_LOGGED_IN = "未登录";

    public static final String LOGIN_REQUIRED = "未登录，请先登录";

    public static final String LOGIN_FAILED = "用户名或密码错误";

    public static final String WRONG_OLD_PASSWORD = "原密码错误";

    public static final String INTERNAL_ERROR = "系统繁忙，请稍后再试";

    public static final String SQL_REQUIRED = "SQL语句不能为空";

    public static final String SQL_EXECUTE_FAILED = "执行失败";

    public static final String SQL_EXECUTE_ERROR_PREFIX = "SQL执行失败: ";

    public static final String SQL_ALTER_TABLE_ERROR_PREFIX = "执行ALTER TABLE语句失败: ";

    public static final String FIELD_CODE_EXISTS = "字段编码已存在";

    public static final String FK_SYNC_FAILED_PREFIX = "同步外键失败: ";

    public static String sqlExecuteError(String detail) {
        return SQL_EXECUTE_ERROR_PREFIX + detail;
    }

    public static String alterTableError(String detail) {
        return SQL_ALTER_TABLE_ERROR_PREFIX + detail;
    }
}
