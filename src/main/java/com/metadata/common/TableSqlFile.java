package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单表建表 SQL 项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "单表建表 SQL")
public class TableSqlFile {

    /**
     * 表编码
     */
    @Schema(description = "表编码", example = "USER_INFO")
    private String tableCode;

    /**
     * 建表 SQL 全文
     */
    @Schema(description = "建表 SQL")
    private String sql;
}
