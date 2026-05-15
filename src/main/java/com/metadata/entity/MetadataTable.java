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
    private String businessCode;  // 业务系统编码
    /**
     * 物理库名（MySQL schema）。为空时使用业务系统上的默认库名。
     */
    private String databaseName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

