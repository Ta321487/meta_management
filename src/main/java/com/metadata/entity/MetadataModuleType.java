package com.metadata.entity;

import lombok.Data;

import java.util.List;

/**
 * 模块类型实体
 */
@Data
public class MetadataModuleType {
    private Long id;
    private String typeCode;
    private String typeName;
    
    private List<String> defaultNodes;
    
    private String description;
}

