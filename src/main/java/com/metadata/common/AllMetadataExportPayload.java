package com.metadata.common;

import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataModule;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 全量元数据导出响应类
 */
@Data
@Schema(description = "全量元数据导出内容")
public class AllMetadataExportPayload {

    /**
     * 全部模块
     */
    @Schema(description = "模块列表")
    private List<MetadataModule> modules;

    /**
     * 全部表
     */
    @Schema(description = "表列表")
    private List<MetadataTable> tables;

    /**
     * 各表字段，键为表编码
     */
    @Schema(description = "表编码 → 字段列表")
    private Map<String, List<MetadataField>> fields;

    /**
     * 表间关联关系
     */
    @Schema(description = "表关联关系列表")
    private List<MetadataTableRelation> relations;
}
