package com.metadata.controller;

import com.metadata.common.AssociateModulesRequest;
import com.metadata.common.Result;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.service.MetadataBusinessSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务系统控制器
 */
@RestController
@RequestMapping("/api/businessSystem")
@Tag(name = "业务系统管理", description = "元数据业务系统相关API")
public class MetadataBusinessSystemController {

    @Autowired
    private MetadataBusinessSystemService businessSystemService;

    /**
     * 新增业务系统
     */
    @PostMapping("/add")
    @Operation(summary = "新增业务系统", description = "添加新的业务系统")
    public Result<?> add(@Parameter(description = "业务系统信息") @RequestBody MetadataBusinessSystem businessSystem) {
        try {
            businessSystemService.add(businessSystem);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新业务系统
     */
    @PostMapping("/update")
    @Operation(summary = "更新业务系统", description = "更新业务系统信息")
    public Result<?> update(@Parameter(description = "业务系统信息") @RequestBody MetadataBusinessSystem businessSystem) {
        try {
            businessSystemService.update(businessSystem);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除业务系统
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除业务系统", description = "根据ID删除业务系统")
    public Result<?> delete(@Parameter(description = "业务系统ID") @PathVariable Long id) {
        try {
            businessSystemService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询业务系统详情
     */
    @GetMapping("/{businessCode}")
    @Operation(summary = "查询业务系统详情", description = "根据业务系统编码查询详情")
    public Result<MetadataBusinessSystem> getByCode(@Parameter(description = "业务系统编码") @PathVariable String businessCode) {
        MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        return Result.success(businessSystem);
    }

    /**
     * 查询所有业务系统
     */
    @GetMapping("/list")
    @Operation(summary = "查询业务系统列表", description = "查询所有业务系统")
    public Result<List<MetadataBusinessSystem>> list() {
        List<MetadataBusinessSystem> list = businessSystemService.listAll();
        return Result.success(list);
    }

    /**
     * 获取默认业务系统
     */
    @GetMapping("/default")
    @Operation(summary = "获取默认业务系统", description = "获取默认的业务系统")
    public Result<MetadataBusinessSystem> getDefault() {
        MetadataBusinessSystem businessSystem = businessSystemService.getDefault();
        return Result.success(businessSystem);
    }

    /**
     * 关联模块到业务系统
     */
    @PostMapping("/associateModules")
    @Operation(summary = "关联模块到业务系统", description = "将多个模块关联到指定业务系统")
    public Result<?> associateModules(@Parameter(description = "关联模块请求参数") @RequestBody AssociateModulesRequest request) {
        try {
            businessSystemService.associateModules(request.getBusinessCode(), request.getModuleCodes());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 解除模块与业务系统的关联
     */
    @PostMapping("/disassociateModules")
    @Operation(summary = "解除模块关联", description = "解除多个模块与指定业务系统的关联")
    public Result<?> disassociateModules(@Parameter(description = "解除模块关联请求参数") @RequestBody AssociateModulesRequest request) {
        try {
            businessSystemService.disassociateModules(request.getBusinessCode(), request.getModuleCodes());
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取已关联模块
     */
    @GetMapping("/associatedModules/{businessCode}")
    @Operation(summary = "获取已关联模块", description = "获取指定业务系统已关联的模块列表")
    public Result<List<String>> getAssociatedModules(@Parameter(description = "业务系统编码") @PathVariable String businessCode) {
        try {
            List<String> moduleCodes = businessSystemService.getAssociatedModules(businessCode);
            return Result.success(moduleCodes);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
