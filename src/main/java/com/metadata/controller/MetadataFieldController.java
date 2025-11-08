package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataField;
import com.metadata.service.MetadataFieldService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字段控制器
 */
@RestController
@RequestMapping("/api/field")
public class MetadataFieldController {

    @Autowired
    private MetadataFieldService fieldService;

    /**
     * 新增字段
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody MetadataField field) {
        try {
            fieldService.add(field);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新字段
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataField field) {
        try {
            fieldService.update(field);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除字段
     */
    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            fieldService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询表的所有字段
     */
    @GetMapping("/list/{tableCode}")
    public Result<List<MetadataField>> listByTableCode(@PathVariable String tableCode) {
        List<MetadataField> list = fieldService.listByTableCode(tableCode);
        return Result.success(list);
    }
}

