package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 代码自测单项结果
 */
@Data
@Schema(description = "单个产出物测试结果")
public class CodeTestItemResult {

    /**
     * 产出物名称（如「实体类」「控制器」）
     */
    @Schema(description = "产出物名称")
    private String name;

    /**
     * 类型（如 Java 代码、Vue 组件）
     */
    @Schema(description = "类型")
    private String type;

    /**
     * 状态：success / warning / danger
     */
    @Schema(description = "状态")
    private String status;

    /**
     * 检测说明
     */
    @Schema(description = "说明")
    private String message;

    /**
     * 错误列表
     */
    @Schema(description = "错误信息")
    private List<String> errors;

    /**
     * 警告列表
     */
    @Schema(description = "警告信息")
    private List<String> warnings;

    /**
     * 从源码解析出的接口或调用关系
     */
    @Schema(description = "API 探测项")
    private List<CodeTestApiTestItem> apiTests;
}
