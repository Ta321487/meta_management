package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.exception.BizException;
import com.metadata.service.MetadataTableRelationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表关联关系控制器
 */
@RestController
@RequestMapping("/api/relation")
@Tag(name = "表关联关系管理", description = "表关联关系相关API")
public class MetadataTableRelationController {

    @Autowired
    private MetadataTableRelationService relationService;

    @PostMapping("/add")
    @Operation(summary = "新增表关联", description = "在元数据中新增主从表关联关系")
    public Result<?> add(@Parameter(description = "关联关系信息") @RequestBody MetadataTableRelation relation) {
        relationService.add(relation);
        return Result.success();
    }

    @PostMapping("/update")
    @Operation(summary = "更新表关联", description = "修改表关联关系配置")
    public Result<?> update(@Parameter(description = "关联关系信息") @RequestBody MetadataTableRelation relation) {
        relationService.update(relation);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除表关联", description = "根据主键 ID 删除关联关系")
    public Result<?> delete(@Parameter(description = "关联 ID") @PathVariable Long id) {
        relationService.delete(id);
        return Result.success();
    }

    @GetMapping("/listByMain/{mainTableCode}")
    @Operation(summary = "按主表查询关联", description = "查询指定主表下的全部关联关系")
    public Result<List<MetadataTableRelation>> listByMainTableCode(
            @Parameter(description = "主表编码") @PathVariable String mainTableCode) {
        List<MetadataTableRelation> list = relationService.listByMainTableCode(mainTableCode);
        return Result.success(list);
    }

    @GetMapping("/listBySlave/{slaveTableCode}")
    @Operation(summary = "按从表查询关联", description = "查询指定从表上的关联关系，可按业务系统过滤")
    public Result<List<MetadataTableRelation>> listBySlaveTableCode(
            @Parameter(description = "从表编码") @PathVariable String slaveTableCode,
            @Parameter(description = "业务系统编码") @RequestParam(required = false) String businessCode) {
        List<MetadataTableRelation> list = relationService.listBySlaveTableCode(slaveTableCode, businessCode);
        return Result.success(list);
    }

    @GetMapping("/list")
    @Operation(summary = "查询关联列表", description = "查询全部关联；传入 current、size 时分页，可按 businessCode 过滤")
    public Result<?> listAll(
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer size,
            @Parameter(description = "业务系统编码") @RequestParam(required = false) String businessCode) {
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataTableRelation> pageResult;
            if (businessCode != null && !businessCode.isEmpty()) {
                pageResult = relationService.page(pageRequest, businessCode);
            } else {
                pageResult = relationService.page(pageRequest);
            }
            return Result.success(pageResult);
        }
        List<MetadataTableRelation> list;
        if (businessCode != null && !businessCode.isEmpty()) {
            list = relationService.listAll(businessCode);
        } else {
            list = relationService.listAll();
        }
        return Result.success(list);
    }

    @PostMapping("/createForeignKey")
    @Operation(summary = "创建外键", description = "根据关联配置在物理库创建外键约束")
    public Result<?> createForeignKey(@Parameter(description = "关联关系信息") @RequestBody MetadataTableRelation relation) {
        Map<String, Object> result = relationService.createForeignKey(relation);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result.get("message"));
        }
        throw BizException.of(AppErrorCodes.RELATION_OPERATION_FAILED, result.get("message").toString());
    }

    @PostMapping("/syncForeignKeys")
    @Operation(summary = "同步外键", description = "从物理库同步外键到元数据；tableCode 为空时同步全部表")
    public Result<?> syncForeignKeys(
            @Parameter(description = "表编码，可选") @RequestParam(required = false) String tableCode) {
        try {
            Map<String, Object> result = relationService.syncForeignKeys(tableCode);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return Result.success(result.get("message"));
            }
            throw BizException.of(AppErrorCodes.RELATION_OPERATION_FAILED, result.get("message").toString());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            String detail = e.getMessage() != null ? e.getMessage() : "";
            throw BizException.of(AppErrorCodes.RELATION_FK_SYNC_FAILED, ApiMessages.FK_SYNC_FAILED_PREFIX + detail, e);
        }
    }
}
