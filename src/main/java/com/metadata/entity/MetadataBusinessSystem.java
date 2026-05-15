package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 业务系统实体
 */
@Data
public class MetadataBusinessSystem {
    private Long id;
    private String businessCode;
    private String businessName;
    private String description;
    private Integer isDefault;
    private String packageName;
    /**
     * 该业务系统默认的物理库名（MySQL schema），物理 DDL 默认落在此库。
     */
    private String databaseName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}