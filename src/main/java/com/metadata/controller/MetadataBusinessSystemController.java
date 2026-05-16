package com.metadata.controller;

import com.metadata.common.AssociateModulesRequest;
import com.metadata.common.FillBusinessSystemCatalogRequest;
import com.metadata.common.Result;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.service.MetadataBusinessSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        businessSystemService.add(businessSystem);
        return Result.success();

    }

    /**
     * 更新业务系统
     */
    @PostMapping("/update")
    @Operation(summary = "更新业务系统", description = "更新业务系统信息")
    public Result<?> update(@Parameter(description = "业务系统信息") @RequestBody MetadataBusinessSystem businessSystem) {
        businessSystemService.update(businessSystem);
        return Result.success();

    }

    /**
     * 补充默认物理库（可选事后补录），并可回填本系统下表级物理库名为空的表。
     */
    @PostMapping("/fillPhysicalCatalog")
    @Operation(summary = "补充业务系统物理库", description = "写入业务系统默认物理库；可选将表级物理库名为空的表一并写入同一库名")
    public Result<Map<String, Object>> fillPhysicalCatalog(
            @Parameter(description = "补充物理库参数") @RequestBody FillBusinessSystemCatalogRequest request) {
        int backfilled = businessSystemService.fillDefaultPhysicalCatalog(request);
        Map<String, Object> data = new HashMap<>(2);
        data.put("backfilledTableCount", backfilled);
        return Result.success(data);
    }

    /**
     * 删除业务系统
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除业务系统", description = "根据ID删除业务系统")
    public Result<?> delete(@Parameter(description = "业务系统ID") @PathVariable Long id) {
        businessSystemService.delete(id);
        return Result.success();

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
    @Operation(summary = "查询业务系统列表", description = "默认仅返回启用的业务系统；管理端传 includeDisabled=true 含停用")
    public Result<List<MetadataBusinessSystem>> list(
            @Parameter(description = "为 true 时包含已停用的业务系统（表管理等业务端勿传）")
            @RequestParam(required = false) Boolean includeDisabled) {
        List<MetadataBusinessSystem> list = businessSystemService.listAll(Boolean.TRUE.equals(includeDisabled));
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
        businessSystemService.associateModules(request.getBusinessCode(), request.getModuleCodes());
        return Result.success();

    }

    /**
     * 解除模块与业务系统的关联
     */
    @PostMapping("/disassociateModules")
    @Operation(summary = "解除模块关联", description = "解除多个模块与指定业务系统的关联")
    public Result<?> disassociateModules(@Parameter(description = "解除模块关联请求参数") @RequestBody AssociateModulesRequest request) {
        businessSystemService.disassociateModules(request.getBusinessCode(), request.getModuleCodes());
        return Result.success();

    }

    /**
     * 获取已关联模块
     */
    @GetMapping("/associatedModules/{businessCode}")
    @Operation(summary = "获取已关联模块", description = "获取指定业务系统已关联的模块列表")
    public Result<List<String>> getAssociatedModules(@Parameter(description = "业务系统编码") @PathVariable String businessCode) {
        List<String> moduleCodes = businessSystemService.getAssociatedModules(businessCode);
        return Result.success(moduleCodes);

    }
}
