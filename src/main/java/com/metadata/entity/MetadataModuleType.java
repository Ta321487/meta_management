package com.metadata.entity;

import lombok.Data;

/**
 * 模块类型实体
 */
@Data
public class MetadataModuleType {
    private Long id;
    private String typeCode;
    private String typeName;
    private String defaultNodes;
    private String description;
}

