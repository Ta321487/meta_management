package com.metadata.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.common.AllMetadataExportPayload;
import com.metadata.common.ModuleMetadataExportPayload;
import com.metadata.common.Result;
import com.metadata.common.TableMetadataExportPayload;
import com.metadata.service.MetadataExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 元数据导出控制器
 */
@RestController
@RequestMapping("/api/metadata")
@Tag(name = "元数据导出管理", description = "元数据导出相关API")
public class MetadataExportController {

    @Autowired
    private MetadataExportService exportService;

    /**
     * 查询模块详情及关联表/节点
     */
    @GetMapping("/module/{moduleCode}")
    @Operation(summary = "导出模块元数据", description = "查询模块信息及关联表、字段、功能节点、业务规则")
    public Result<ModuleMetadataExportPayload> getModuleMetadata(
            @Parameter(description = "模块编码") @PathVariable String moduleCode) {
        JSONObject data = exportService.exportModule(moduleCode);
        return Result.success(JSON.parseObject(data.toJSONString(), ModuleMetadataExportPayload.class));
    }

    /**
     * 查询表的所有字段配置
     */
    @GetMapping("/table/{tableCode}/fields")
    @Operation(summary = "导出单表元数据", description = "查询表信息及全部字段配置")
    public Result<TableMetadataExportPayload> getTableFields(
            @Parameter(description = "表编码") @PathVariable String tableCode) {
        JSONObject data = exportService.exportTable(tableCode);
        return Result.success(JSON.parseObject(data.toJSONString(), TableMetadataExportPayload.class));
    }

    /**
     * 导出所有元数据
     */
    @GetMapping("/export/all")
    @Operation(summary = "导出全量元数据", description = "导出全部模块、表、字段及表关联关系")
    public Result<AllMetadataExportPayload> exportAll() {
        JSONObject data = exportService.exportAll();
        return Result.success(JSON.parseObject(data.toJSONString(), AllMetadataExportPayload.class));
    }
}

