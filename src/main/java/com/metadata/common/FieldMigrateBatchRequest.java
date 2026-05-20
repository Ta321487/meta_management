package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量字段跨表迁移：共用目标表与迁移选项，按字段 ID 逐条执行（每条独立事务）。
 */
@Data
@Schema(description = "批量字段迁移到其他表")
public class FieldMigrateBatchRequest {

    @Schema(description = "源字段 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> fieldIds;

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
