package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字段跨表迁移：元数据登记到目标表，可选物理列 ADD/DROP 与数据拷贝。
 */
@Data
@Schema(description = "字段迁移到其他表")
public class FieldMigrateRequest {

    @Schema(description = "源字段 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fieldId;

    @Schema(description = "目标表编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String targetTableCode;

    @Schema(description = "业务系统编码（可选，默认取源字段/目标表）")
    private String businessCode;

    @Schema(description = "是否拷贝物理数据（需源表与目标表在同一物理库）", defaultValue = "true")
    private Boolean migrateData;

    @Schema(description = "迁移成功后是否从源表删除字段（元数据 + DROP COLUMN）", defaultValue = "true")
    private Boolean removeFromSource;

    @Schema(description = "关联键：源表列名，默认 id")
    private String joinSourceField;

    @Schema(description = "关联键：目标表列名，默认与 joinSourceField 相同")
    private String joinTargetField;
}
