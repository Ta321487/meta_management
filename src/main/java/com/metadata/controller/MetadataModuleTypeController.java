package com.metadata.controller;

import com.metadata.common.BatchDeleteRequest;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
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
    @Operation(summary = "查询模块类型列表", description = "查询模块类型列表，支持条件筛选与分页")
    public Result<?> list(
            @Parameter(description = "类型编码（模糊）") @RequestParam(required = false) String typeCode,
            @Parameter(description = "类型名称（模糊）") @RequestParam(required = false) String typeName,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataModuleType> pageResult = typeService.page(typeCode, typeName, pageRequest);
            return Result.success(pageResult);
        }
        List<MetadataModuleType> list = typeService.list(typeCode, typeName);
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

    @PostMapping("/batchDelete")
    @Operation(summary = "批量删除模块类型", description = "根据ID列表批量删除模块类型")
    public Result<?> batchDelete(@Parameter(description = "包含ids列表的参数") @RequestBody BatchDeleteRequest request) {
        typeService.batchDelete(request.getIds());
        return Result.success();
    }
}
