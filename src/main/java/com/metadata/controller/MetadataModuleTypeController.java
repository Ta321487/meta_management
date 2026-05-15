package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataModuleType;
import com.metadata.service.MetadataModuleTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块类型控制器
 */
@RestController
@RequestMapping("/api/moduleType")
@Tag(name = "模块类型管理", description = "模块类型相关API")
public class MetadataModuleTypeController {

    @Autowired
    private MetadataModuleTypeService typeService;

    @GetMapping("/list")
    @Operation(summary = "查询模块类型列表", description = "查询全部模块类型")
    public Result<List<MetadataModuleType>> listAll() {
        List<MetadataModuleType> list = typeService.listAll();
        return Result.success(list);
    }

    @PostMapping("/add")
    @Operation(summary = "新增模块类型", description = "添加新的模块类型")
    public Result<?> add(@Parameter(description = "模块类型信息") @RequestBody MetadataModuleType type) {
        typeService.add(type);
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新模块类型", description = "修改模块类型信息")
    public Result<?> update(@Parameter(description = "模块类型信息") @RequestBody MetadataModuleType type) {
        typeService.update(type);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除模块类型", description = "根据主键 ID 删除")
    public Result<?> delete(@Parameter(description = "类型 ID") @PathVariable Long id) {
        typeService.delete(id);
        return Result.success();
    }
}
