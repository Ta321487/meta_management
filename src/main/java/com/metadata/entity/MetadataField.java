package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 抽象字段实体
 */
@Data
public class MetadataField {
    private Long id;
    private String fieldCode;
    private String tableCode;
    private String fieldName;
    private String fieldType;
    private String label;
    private Integer isRequired;
    private String formComponent;
    /**
     * 1 参与新增/编辑表单；0 不参与（如业务编码由系统生成，仅列表展示），此时 form_component 存占位如 none。
     */
    private Integer inForm;
    private String validateRule;
    private Integer sort;
    private Integer isEnabled;
    private String businessCode;  // 业务系统编码
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

