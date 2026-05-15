package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 代码生成自测报告
 */
@Data
@Schema(description = "代码生成测试结果")
public class CodeTestReport {

    /**
     * 各产出物（SQL、Java、Vue 等）的检测明细
     */
    @Schema(description = "各产出物测试结果")
    private List<CodeTestItemResult> testResults;

    /**
     * 是否全部通过
     */
    @Schema(description = "是否全部通过")
    private Boolean success;

    /**
     * 汇总说明
     */
    @Schema(description = "汇总说明")
    private String message;

    /**
     * 检测项总数
     */
    @Schema(description = "检测项总数")
    private Integer total;

    /**
     * 成功项数
     */
    @Schema(description = "成功项数")
    private Integer successCount;

    /**
     * 失败项数
     */
    @Schema(description = "失败项数")
    private Integer failCount;

    /**
     * 本次生成的完整代码包
     */
    @Schema(description = "生成的完整代码包")
    private TableGeneratedCodeBundle generatedCode;
}
