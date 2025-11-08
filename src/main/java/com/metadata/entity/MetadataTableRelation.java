package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 表关联关系实体
 */
@Data
public class MetadataTableRelation {
    private Long id;
    private String relationCode;
    private String mainTableCode;
    private String slaveTableCode;
    private String mainFieldCode;
    private String slaveFieldCode;
    private String relationType;
    private String relationName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

