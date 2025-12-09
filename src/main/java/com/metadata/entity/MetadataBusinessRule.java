package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 业务规则实体
 */
@Data
public class MetadataBusinessRule {
    private Long id;
    private String ruleCode;
    private String moduleCode;
    private String ruleType;
    private String ruleContent;
    private String description;
    private String businessCode;  // 业务系统编码
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

