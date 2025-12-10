package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataModuleType;
import com.metadata.service.MetadataModuleTypeService;
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
    public Result<List<MetadataModuleType>> listAll() {
        List<MetadataModuleType> list = typeService.listAll();
        return Result.success(list);
    }

    /**
     * 新增模块类型
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody MetadataModuleType type) {
        try {
            typeService.add(type);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新模块类型
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataModuleType type) {
        try {
            typeService.update(type);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除模块类型
     */
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(@PathVariable Long id) {
        try {
            typeService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

