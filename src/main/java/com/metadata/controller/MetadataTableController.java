package com.metadata.controller;

import com.metadata.common.BatchDeleteRequest;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataTable;
import com.metadata.service.MetadataTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 表控制器
 */
@RestController
@RequestMapping("/api/table")
@Tag(name = "表管理", description = "元数据表相关API")
public class MetadataTableController {

    @Autowired
    private MetadataTableService tableService;

    /**
     * 新增表
     */
    @PostMapping("/add")
    @Operation(summary = "新增表", description = "添加新的元数据表")
    public Result<?> add(@Parameter(description = "表信息") @RequestBody MetadataTable table) {
        try {
            tableService.add(table);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新表
     */
    @PostMapping("/update")
    @Operation(summary = "更新表", description = "更新元数据表信息")
    public Result<?> update(@Parameter(description = "表信息") @RequestBody MetadataTable table) {
        try {
            tableService.update(table);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除表
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除表", description = "根据ID删除元数据表")
    public Result<?> delete(@Parameter(description = "表ID") @PathVariable Long id) {
        try {
            tableService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除表
     */
    @PostMapping("/batchDelete")
    @Operation(summary = "批量删除表", description = "根据ID列表批量删除元数据表")
    public Result<?> batchDelete(@Parameter(description = "包含ids列表的参数") @RequestBody BatchDeleteRequest request) {
        try {
            tableService.batchDelete(request.getIds());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询表详情
     */
    @GetMapping("/{tableCode}")
    @Operation(summary = "查询表详情", description = "根据表编码查询表详情")
    public Result<MetadataTable> getByCode(@Parameter(description = "表编码") @PathVariable String tableCode) {
        MetadataTable table = tableService.getByCode(tableCode);
        return Result.success(table);
    }

    /**
     * 查询所有表
     */
    @GetMapping("/list")
    @Operation(summary = "查询表列表", description = "查询表列表，支持分页和条件查询")
    public Result<?> list(
            @Parameter(description = "表名称") @RequestParam(required = false) String tableName,
            @Parameter(description = "业务系统") @RequestParam(required = false) String businessCode,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataTable> pageResult = tableService.page(tableName, businessCode, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataTable> list = tableService.list(tableName, businessCode);
        return Result.success(list);
    }

    /**
     * 根据模块编码查询表
     */
    @GetMapping("/listByModule/{moduleCode}")
    @Operation(summary = "根据模块查询表", description = "根据模块编码查询关联的表列表")
    public Result<List<MetadataTable>> listByModuleCode(@Parameter(description = "模块编码") @PathVariable String moduleCode) {
        List<MetadataTable> list = tableService.listByModuleCode(moduleCode);
        return Result.success(list);
    }
}

