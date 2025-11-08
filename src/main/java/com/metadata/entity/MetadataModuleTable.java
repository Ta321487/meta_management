package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模块与表关联实体
 */
@Data
public class MetadataModuleTable {
    private Long id;
    private String moduleCode;
    private String tableCode;
    private LocalDateTime createTime;
}

