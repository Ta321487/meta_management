package com.metadata.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物理库登记（MySQL catalog 元数据）
 */
@Data
public class MetadataPhysicalDatabase {
    private Long id;
    /** MySQL 库名 */
    private String catalogName;
    private String displayName;
    private String description;
    private String charsetName;
    private String collationName;
    /** 保存时是否在实例上 CREATE DATABASE IF NOT EXISTS */
    private Integer syncToInstance;
    private Integer isEnabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
