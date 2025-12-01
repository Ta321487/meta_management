package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

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
    private Integer sort;  // 排序号
    private String icon;   // 图标
    private String routePath;  // 路由路径
    private String componentPath;  // 组件路径
    private List<String> tableCodes;  // 关联的表编码列表
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

