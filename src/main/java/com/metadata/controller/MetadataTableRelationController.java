package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.service.MetadataTableRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表关联关系控制器
 */
@RestController
@RequestMapping("/api/relation")
public class MetadataTableRelationController {

    @Autowired
    private MetadataTableRelationService relationService;

    @PostMapping("/add")
    public Result<?> add(@RequestBody MetadataTableRelation relation) {
        try {
            relationService.add(relation);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataTableRelation relation) {
        try {
            relationService.update(relation);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            relationService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/listByMain/{mainTableCode}")
    public Result<List<MetadataTableRelation>> listByMainTableCode(@PathVariable String mainTableCode) {
        List<MetadataTableRelation> list = relationService.listByMainTableCode(mainTableCode);
        return Result.success(list);
    }

    @GetMapping("/listBySlave/{slaveTableCode}")
    public Result<List<MetadataTableRelation>> listBySlaveTableCode(@PathVariable String slaveTableCode) {
        List<MetadataTableRelation> list = relationService.listBySlaveTableCode(slaveTableCode);
        return Result.success(list);
    }

    @GetMapping("/list")
    public Result<?> listAll(@RequestParam(required = false) Integer current,
                             @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataTableRelation> pageResult = relationService.page(pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataTableRelation> list = relationService.listAll();
        return Result.success(list);
    }

    @PostMapping("/createForeignKey")
    public Result<?> createForeignKey(@RequestBody MetadataTableRelation relation) {
        try {
            Map<String, Object> result = relationService.createForeignKey(relation);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return Result.success(result.get("message"));
            } else {
                return Result.error(result.get("message").toString());
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/syncForeignKeys")
    public Result<?> syncForeignKeys(@RequestBody(required = false) Map<String, String> params) {
        try {
            String tableCode = params != null ? params.get("tableCode") : null;
            Map<String, Object> result = relationService.syncForeignKeys(tableCode);
            if (Boolean.TRUE.equals(result.get("success"))) {
                return Result.success(result.get("message"));
            } else {
                return Result.error(result.get("message").toString());
            }
        } catch (Exception e) {
            return Result.error("同步外键失败: " + e.getMessage());
        }
    }
}

