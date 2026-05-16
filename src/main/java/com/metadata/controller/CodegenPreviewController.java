package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.impl.CodegenPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 业务系统预览：与 ZIP 生成物同源的 UI 规格 + 内存 Mock API
 */
@RestController
@RequestMapping("/api/codegen/preview")
@Tag(name = "代码生成预览", description = "业务系统级前后端预览（模拟下载 ZIP 后的效果）")
public class CodegenPreviewController {

    @Autowired
    private CodegenPreviewService codegenPreviewService;

    @GetMapping("/business/{businessCode}/spec")
    @Operation(summary = "获取业务系统预览规格", description = "路由、各表表单/列表字段、校验与业务规则（与生成模板同源）")
    public Result<Map<String, Object>> getBusinessPreviewSpec(
            @PathVariable String businessCode,
            @RequestParam(required = false) String packageName,
            @RequestParam(defaultValue = "false") boolean useInterface,
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        return Result.success(codegenPreviewService.buildBusinessPreviewSpec(
                businessCode, packageName, useInterface, captchaEnabled));
    }

    @PostMapping("/business/{businessCode}/mock/reset")
    @Operation(summary = "清空预览 Mock 数据")
    public Result<Void> resetMock(@PathVariable String businessCode) {
        codegenPreviewService.resetMockData(businessCode);
        return Result.success();
    }
}
