package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 表约束项
 * <p>物理库 information_schema 查询结果的标准化展示。</p>
 */
@Data
@Schema(description = "表约束项")
public class TableConstraintItem {

    /**
     * 约束名称
     */
    @Schema(description = "约束名称")
    private String constraintName;

    /**
     * 约束类型（中文，如主键约束、外键约束）
     */
    @Schema(description = "约束类型")
    private String constraintType;

    /**
     * 列名；表级约束时可能为空
     */
    @Schema(description = "列名")
    private String fieldName;

    /**
     * 约束级别：列级 / 表级
     */
    @Schema(description = "约束级别")
    private String constraintLevel;

    /**
     * 约束定义或说明
     */
    @Schema(description = "约束内容")
    private String constraintContent;

    /**
     * 元数据表编码
     */
    @Schema(description = "表编码")
    private String tableCode;

    /**
     * 关联字段主键；无对应字段时为空
     */
    @Schema(description = "字段 ID")
    private Long id;

    /**
     * 关联字段编码
     */
    @Schema(description = "字段编码")
    private String fieldCode;

    /**
     * 从服务层 Map 转换
     */
    public static TableConstraintItem fromMap(Map<String, Object> m) {
        TableConstraintItem item = new TableConstraintItem();
        if (m == null) {
            return item;
        }
        item.setConstraintName(str(m.get("constraintName")));
        item.setConstraintType(str(m.get("constraintType")));
        item.setFieldName(str(m.get("fieldName")));
        item.setConstraintLevel(str(m.get("constraintLevel")));
        item.setConstraintContent(str(m.get("constraintContent")));
        item.setTableCode(str(m.get("tableCode")));
        Object idObj = m.get("id");
        if (idObj instanceof Number) {
            item.setId(((Number) idObj).longValue());
        }
        item.setFieldCode(str(m.get("fieldCode")));
        return item;
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
