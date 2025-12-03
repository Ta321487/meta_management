package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataField;
import com.metadata.service.MetadataFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 字段控制器
 */
@RestController
@RequestMapping("/api/field")
@Tag(name = "字段管理", description = "元数据字段相关API")
public class MetadataFieldController {

    @Autowired
    private MetadataFieldService fieldService;

    /**
     * 新增字段
     */
    @PostMapping("/add")
    @Operation(summary = "新增字段", description = "添加新的元数据字段")
    public Result<?> add(@Parameter(description = "字段信息") @RequestBody MetadataField field) {
        try {
            fieldService.add(field);
            return Result.success();
        } catch (Exception e) {
            // 处理SQL唯一约束异常
            if (e.getMessage().contains("Duplicate entry") || e.getMessage().contains("uk_table_field")) {
                return Result.error("字段编码已存在");
            }
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新字段
     */
    @PostMapping("/update")
    @Operation(summary = "更新字段", description = "更新元数据字段信息")
    public Result<?> update(@Parameter(description = "字段信息") @RequestBody MetadataField field) {
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
    @Operation(summary = "删除字段", description = "根据ID删除元数据字段")
    public Result<?> delete(@Parameter(description = "包含id的参数") @RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            fieldService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 批量删除字段
     */
    @PostMapping("/batchDelete")
    @Operation(summary = "批量删除字段", description = "根据ID列表批量删除元数据字段")
    public Result<?> batchDelete(@Parameter(description = "包含ids列表的参数") @RequestBody Map<String, Object> params) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> intIds = (List<Integer>) params.get("ids");
            List<Long> ids = intIds.stream().map(Long::valueOf).collect(java.util.stream.Collectors.toList());
            fieldService.batchDelete(ids);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询表的所有字段（支持分页）
     */
    @GetMapping("/list/{tableCode}")
    @Operation(summary = "查询字段列表", description = "根据表编码查询字段列表，支持分页")
    public Result<?> listByTableCode(
            @Parameter(description = "表编码") @PathVariable String tableCode,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataField> pageResult = fieldService.pageByTableCode(tableCode, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataField> list = fieldService.listByTableCode(tableCode);
        return Result.success(list);
    }

    /**
     * 获取表的约束列表
     */
    @GetMapping("/constraint/list/{tableCode}")
    @Operation(summary = "获取约束列表", description = "根据表编码查询约束列表")
    public Result<?> getConstraints(
            @Parameter(description = "表编码") @PathVariable String tableCode) {
        try {
            List<Map<String, Object>> constraints = fieldService.getConstraints(tableCode);
            return Result.success(constraints);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除约束
     */
    @PostMapping("/constraint/delete")
    @Operation(summary = "删除约束", description = "删除表的约束")
    public Result<?> deleteConstraint(
            @Parameter(description = "包含id的参数") @RequestBody Map<String, Object> params) {
        try {
            fieldService.deleteConstraint(params);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

