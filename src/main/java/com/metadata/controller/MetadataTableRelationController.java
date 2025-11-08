package com.metadata.controller;

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
    public Result<List<MetadataTableRelation>> listAll() {
        List<MetadataTableRelation> list = relationService.listAll();
        return Result.success(list);
    }
}

