package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 多条 SQL 批量执行结果
 */
@Data
@Schema(description = "多条 SQL 批量执行结果")
public class SqlExecuteMultipleResultPayload {

    /**
     * 是否全部执行成功
     */
    @Schema(description = "是否全部成功")
    private Boolean success;

    /**
     * 汇总说明（共几条、成功几条等）
     */
    @Schema(description = "汇总说明")
    private String message;

    /**
     * 每条 SQL 的执行明细
     */
    @Schema(description = "各条执行结果")
    private List<SqlExecuteSingleResultPayload> results;

    /**
     * 实际执行的 SQL 条数
     */
    @Schema(description = "执行条数")
    private Integer totalCount;

    /**
     * 成功条数
     */
    @Schema(description = "成功条数")
    private Integer successCount;

    /**
     * 失败条数
     */
    @Schema(description = "失败条数")
    private Integer failCount;

    /**
     * 从服务层 Map 结果转换
     */
    @SuppressWarnings("unchecked")
    public static SqlExecuteMultipleResultPayload fromMap(Map<String, Object> m) {
        SqlExecuteMultipleResultPayload p = new SqlExecuteMultipleResultPayload();
        if (m == null) {
            return p;
        }
        p.setSuccess((Boolean) m.get("success"));
        p.setMessage((String) m.get("message"));
        Object resultsObj = m.get("results");
        if (resultsObj instanceof List) {
            List<Map<String, Object>> rawList = (List<Map<String, Object>>) resultsObj;
            List<SqlExecuteSingleResultPayload> items = new ArrayList<>();
            for (Map<String, Object> row : rawList) {
                items.add(SqlExecuteSingleResultPayload.fromMap(row));
            }
            p.setResults(items);
        }
        Object tc = m.get("totalCount");
        if (tc instanceof Number) {
            p.setTotalCount(((Number) tc).intValue());
        }
        Object sc = m.get("successCount");
        if (sc instanceof Number) {
            p.setSuccessCount(((Number) sc).intValue());
        }
        Object fc = m.get("failCount");
        if (fc instanceof Number) {
            p.setFailCount(((Number) fc).intValue());
        }
        return p;
    }
}
