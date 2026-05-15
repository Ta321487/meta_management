package com.metadata.common;

import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单表元数据导出响应类
 */
@Data
@Schema(description = "单表元数据导出内容")
public class TableMetadataExportPayload {

    /**
     * 表基本信息
     */
    @Schema(description = "表信息")
    private MetadataTable table;

    /**
     * 表下全部字段
     */
    @Schema(description = "字段列表")
    private List<MetadataField> fields;
}
