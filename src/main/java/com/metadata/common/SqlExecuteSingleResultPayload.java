package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 单条 SQL 执行结果
 */
@Data
@Schema(description = "单条 SQL 执行结果")
public class SqlExecuteSingleResultPayload {

    /**
     * 是否执行成功
     */
    @Schema(description = "是否执行成功")
    private Boolean success;

    /**
     * 提示或错误说明
     */
    @Schema(description = "提示信息")
    private String message;

    /**
     * 查询结果集（SELECT）；键为列名
     */
    @Schema(description = "查询结果集")
    private List<Map<String, Object>> data;

    /**
     * 查询返回行数
     */
    @Schema(description = "查询返回行数")
    private Integer rowCount;

    /**
     * 更新类语句影响行数
     */
    @Schema(description = "影响行数")
    private Integer affectedRows;

    /**
     * 错误类型简述
     */
    @Schema(description = "错误类型")
    private String error;

    /**
     * 数据库原始错误信息
     */
    @Schema(description = "原始错误信息")
    private String originalError;

    /**
     * 本条 SQL 文本（批量执行时返回）
     */
    @Schema(description = "SQL 文本")
    private String sql;

    /**
     * 从服务层 Map 结果转换
     */
    @SuppressWarnings("unchecked")
    public static SqlExecuteSingleResultPayload fromMap(Map<String, Object> m) {
        SqlExecuteSingleResultPayload p = new SqlExecuteSingleResultPayload();
        if (m == null) {
            return p;
        }
        p.setSuccess((Boolean) m.get("success"));
        p.setMessage((String) m.get("message"));
        Object dataObj = m.get("data");
        if (dataObj instanceof List) {
            p.setData((List<Map<String, Object>>) dataObj);
        }
        Object rc = m.get("rowCount");
        if (rc instanceof Number) {
            p.setRowCount(((Number) rc).intValue());
        }
        Object ar = m.get("affectedRows");
        if (ar instanceof Number) {
            p.setAffectedRows(((Number) ar).intValue());
        }
        p.setError((String) m.get("error"));
        p.setOriginalError((String) m.get("originalError"));
        p.setSql((String) m.get("sql"));
        return p;
    }
}
