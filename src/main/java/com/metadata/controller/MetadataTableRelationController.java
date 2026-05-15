package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.exception.BizException;
import com.metadata.service.MetadataTableRelationService;
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
    public Result<?> add(@RequestBody MetadataTableRelation relation) {
        relationService.add(relation);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataTableRelation relation) {
        relationService.update(relation);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        relationService.delete(id);
        return Result.success();
    }

    @GetMapping("/listByMain/{mainTableCode}")
    public Result<List<MetadataTableRelation>> listByMainTableCode(@PathVariable String mainTableCode) {
        List<MetadataTableRelation> list = relationService.listByMainTableCode(mainTableCode);
        return Result.success(list);
    }

    @GetMapping("/listBySlave/{slaveTableCode}")
    public Result<List<MetadataTableRelation>> listBySlaveTableCode(@PathVariable String slaveTableCode, @RequestParam(required = false) String businessCode) {
        List<MetadataTableRelation> list = relationService.listBySlaveTableCode(slaveTableCode, businessCode);
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result<?> listAll(@RequestParam(required = false) Integer current,
                             @RequestParam(required = false) Integer size,
                             @RequestParam(required = false) String businessCode) {
        // 如果传入了分页参数，使用分页查询
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
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataTableRelation> list;
        if (businessCode != null && !businessCode.isEmpty()) {
            list = relationService.listAll(businessCode);
        } else {
            list = relationService.listAll();
        }
        return Result.success(list);
    }

    @PostMapping("/createForeignKey")
    public Result<?> createForeignKey(@RequestBody MetadataTableRelation relation) {
        Map<String, Object> result = relationService.createForeignKey(relation);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result.get("message"));
        }
        throw BizException.of(AppErrorCodes.RELATION_OPERATION_FAILED, result.get("message").toString());
    }

    @PostMapping("/syncForeignKeys")
    public Result<?> syncForeignKeys(@RequestParam(required = false) String tableCode) {
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
