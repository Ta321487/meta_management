package com.metadata.common;

import com.metadata.entity.MetadataBusinessRule;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataModule;
import com.metadata.entity.MetadataTable;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模块元数据导出响应类
 */
@Data
@Schema(description = "模块元数据导出内容")
public class ModuleMetadataExportPayload {

    /**
     * 模块基本信息
     */
    @Schema(description = "模块信息")
    private MetadataModule module;

    /**
     * 模块关联的表列表
     */
    @Schema(description = "关联表列表")
    private List<MetadataTable> tables;

    /**
     * 各表字段，键为表编码
     */
    @Schema(description = "表编码 → 字段列表")
    private Map<String, List<MetadataField>> fields;

    /**
     * 模块下功能节点
     */
    @Schema(description = "功能节点列表")
    private List<MetadataFunctionNode> nodes;

    /**
     * 模块下业务规则
     */
    @Schema(description = "业务规则列表")
    private List<MetadataBusinessRule> rules;
}
