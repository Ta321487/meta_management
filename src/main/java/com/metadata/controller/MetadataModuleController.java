package com.metadata.controller;

import com.metadata.common.BatchDeleteRequest;
import com.metadata.common.ModuleRequest;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.common.UpdateStatusRequest;
import com.metadata.entity.MetadataModule;
import com.metadata.service.MetadataModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模块控制器
 */
@RestController
@RequestMapping("/api/module")
@Tag(name = "模块管理", description = "元数据模块相关API")
public class MetadataModuleController {

    @Autowired
    private MetadataModuleService moduleService;

    /**
     * 新增模块
     */
    @PostMapping("/add")
    @Operation(summary = "新增模块", description = "添加新的元数据模块")
    public Result<?> add(@Parameter(description = "模块请求信息") @RequestBody ModuleRequest request) {
        moduleService.add(request.toMetadataModule(), request.getTableCodes());
        return Result.success();

    }

    /**
     * 更新模块
     */
    @PostMapping("/update")
    @Operation(summary = "更新模块", description = "更新元数据模块信息")
    public Result<?> update(@Parameter(description = "模块请求信息") @RequestBody ModuleRequest request) {
        moduleService.update(request.toMetadataModule(), request.getTableCodes());
        return Result.success();

    }

    /**
     * 删除模块
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除模块", description = "根据ID删除元数据模块")
    public Result<?> delete(@Parameter(description = "模块ID") @PathVariable Long id) {
        moduleService.delete(id);
        return Result.success();

    }

    /**
     * 批量删除模块
     */
    @PostMapping("/batchDelete")
    @Operation(summary = "批量删除模块", description = "根据ID列表批量删除元数据模块")
    public Result<?> batchDelete(@Parameter(description = "包含ids列表的参数") @RequestBody BatchDeleteRequest request) {
        moduleService.batchDelete(request.getIds());
        return Result.success();

    }

    /**
     * 查询模块详情
     */
    @GetMapping("/{moduleCode}")
    @Operation(summary = "查询模块详情", description = "根据模块编码查询模块详情")
    public Result<MetadataModule> getByCode(@Parameter(description = "模块编码") @PathVariable String moduleCode) {
        MetadataModule module = moduleService.getByCode(moduleCode);
        return Result.success(module);
    }

    /**
     * 查询所有模块
     */
    @GetMapping("/list")
    @Operation(summary = "查询模块列表", description = "查询模块列表，支持分页和条件查询")
    public Result<?> list(
            @Parameter(description = "模块名称") @RequestParam(required = false) String moduleName,
            @Parameter(description = "模块类型") @RequestParam(required = false) String moduleType,
            @Parameter(description = "业务系统") @RequestParam(required = false) String businessCode,
            @Parameter(description = "模块状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataModule> pageResult = moduleService.page(moduleName, moduleType, businessCode, status, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataModule> list = moduleService.list(moduleName, moduleType, businessCode, status);
        return Result.success(list);
    }

    /**
     * 更新模块状态
     */
    @PostMapping("/updateStatus")
    @Operation(summary = "更新模块状态", description = "更新模块的启用/禁用状态")
    public Result<?> updateStatus(@Parameter(description = "包含id和status的参数") @RequestBody UpdateStatusRequest request) {
        moduleService.updateStatus(request.getId(), request.getStatus());
        return Result.success();

    }
    
    /**
     * 批量更新模块状态
     */
    @PostMapping("/batchUpdateStatus")
    @Operation(summary = "批量更新模块状态", description = "批量更新模块的启用/禁用状态")
    public Result<?> batchUpdateStatus(@Parameter(description = "包含ids列表和status的参数") @RequestBody BatchDeleteRequest request, 
                                      @Parameter(description = "目标状态，1-启用，0-禁用") @RequestParam Integer status) {
        moduleService.batchUpdateStatus(request.getIds(), status);
        return Result.success();

    }

    /**
     * 重建模块功能节点
     */
    @PostMapping("/rebuildNodes/{moduleCode}")
    @Operation(summary = "重建模块功能节点", description = "根据模块类型的默认节点重新生成模块的功能节点")
    public Result<?> rebuildNodes(@Parameter(description = "模块编码") @PathVariable String moduleCode) {
        moduleService.rebuildNodes(moduleCode);
        return Result.success();

    }
}

