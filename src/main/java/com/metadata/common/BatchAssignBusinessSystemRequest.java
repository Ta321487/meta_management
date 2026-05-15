package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量分配业务系统请求类
 */
@Data
@Schema(description = "批量分配业务系统请求")
public class BatchAssignBusinessSystemRequest {

    /**
     * 待分配的表编码列表
     */
    @Schema(description = "表编码列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> tableCodes;

    /**
     * 目标业务系统编码
     */
    @Schema(description = "业务系统编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String businessCode;
}
