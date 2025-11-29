package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 抽象表实体
 */
@Data
public class MetadataTable {
    private Long id;
    private String tableCode;
    private String tableName;
    private String pkStrategy;
    private String description;
    private Integer isEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

