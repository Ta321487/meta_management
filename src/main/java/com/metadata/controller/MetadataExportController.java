package com.metadata.controller;

import com.alibaba.fastjson2.JSONObject;
import com.metadata.common.Result;
import com.metadata.service.MetadataExportService;
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
    public Result<JSONObject> getModuleMetadata(@PathVariable String moduleCode) {
        try {
            JSONObject data = exportService.exportModule(moduleCode);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询表的所有字段配置
     */
    @GetMapping("/table/{tableCode}/fields")
    public Result<JSONObject> getTableFields(@PathVariable String tableCode) {
        try {
            JSONObject data = exportService.exportTable(tableCode);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出所有元数据
     */
    @GetMapping("/export/all")
    public Result<JSONObject> exportAll() {
        try {
            JSONObject data = exportService.exportAll();
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

