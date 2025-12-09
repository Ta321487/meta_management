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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}