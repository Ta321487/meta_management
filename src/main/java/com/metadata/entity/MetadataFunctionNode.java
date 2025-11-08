package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 功能节点实体
 */
@Data
public class MetadataFunctionNode {
    private Long id;
    private String nodeCode;
    private String nodeName;
    private String moduleCode;
    private String nodeType;
    private String relatedTableCode;
    private String jumpRelation;
    private Integer sort;
    private Integer isEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

