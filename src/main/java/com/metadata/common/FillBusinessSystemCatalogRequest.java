package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 为业务系统补充默认物理库（并可回填表级物理库名为空的表）。
 */
@Data
@Schema(description = "补充业务系统默认物理库请求")
public class FillBusinessSystemCatalogRequest {

    @Schema(description = "业务系统编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessCode;

    @Schema(description = "要写入的 MySQL 物理库名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String databaseName;

    /**
     * 是否将本业务系统下「表级物理库名为空」的元数据表一并写入同一库名。
     * 未传时默认 true。
     */
    @Schema(description = "是否回填表级物理库名为空的表")
    private Boolean backfillEmptyTableCatalog;
}
