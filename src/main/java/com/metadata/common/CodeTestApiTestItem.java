package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 代码自测中的 API 探测项
 */
@Data
@Schema(description = "API 探测项")
public class CodeTestApiTestItem {

    /**
     * HTTP 方法，或 ROUTE / CALL 等
     */
    @Schema(description = "方法")
    private String method;

    /**
     * 请求路径或调用说明
     */
    @Schema(description = "路径")
    private String path;

    /**
     * 接口或关系名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 探测状态
     */
    @Schema(description = "状态")
    private String status;
}
