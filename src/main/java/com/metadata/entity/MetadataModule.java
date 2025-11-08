package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 抽象模块实体
 */
@Data
public class MetadataModule {
    private Long id;
    private String moduleCode;
    private String moduleName;
    private String moduleType;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

