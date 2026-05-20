package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.impl.CodegenPreviewMockStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预览用 Mock CRUD，路径与生成 Controller 一致（前缀为 /api/codegen/preview/mock）
 */
@RestController
@RequestMapping("/api/codegen/preview/mock/{businessCode}/{entityName}")
@Tag(name = "代码生成预览 Mock", description = "模拟生成后端的 REST 接口（内存存储）")
public class CodegenPreviewMockController {

    @GetMapping("/page")
    @Operation(summary = "分页查询（Mock）")
    public Result<Map<String, Object>> page(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(CodegenPreviewMockStore.page(businessCode, entityName, page, size));
    }

    @GetMapping("/list")
    @Operation(summary = "列表查询（Mock）")
    public Result<List<Map<String, Object>>> list(
            @PathVariable String businessCode,
            @PathVariable String entityName) {
        return Result.success(CodegenPreviewMockStore.list(businessCode, entityName));
    }

    @GetMapping("/{id}")
    @Operation(summary = "按 ID 查询（Mock）")
    public Result<Map<String, Object>> getById(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @PathVariable String id,
            @RequestParam(defaultValue = "id") String pkName) {
        Map<String, Object> row = CodegenPreviewMockStore.getById(businessCode, entityName, id, pkName);
        if (row == null) {
            return Result.error("记录不存在");
        }
        return Result.success(row);
    }

    @PostMapping("/add")
    public Result<Void> add(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestParam(defaultValue = "id") String pkName,
            @RequestBody Map<String, Object> body) {
        CodegenPreviewMockStore.add(businessCode, entityName, body, pkName);
        return Result.success();
    }

    @PostMapping("/update")
    public Result<Void> update(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestParam(defaultValue = "id") String pkName,
            @RequestBody Map<String, Object> body) {
        if (!CodegenPreviewMockStore.update(businessCode, entityName, body, pkName)) {
            return Result.error("更新失败");
        }
        return Result.success();
    }

    @PostMapping("/delete")
    public Result<Void> delete(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestParam(defaultValue = "id") String pkName,
            @RequestBody Map<String, Object> params) {
        Object id = params.get("id");
        if (id == null) {
            return Result.error("缺少 id");
        }
        CodegenPreviewMockStore.delete(businessCode, entityName, id.toString(), pkName);
        return Result.success();
    }

    @PostMapping("/batchDelete")
    public Result<Void> batchDelete(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestParam(defaultValue = "id") String pkName,
            @RequestBody Map<String, Object> params) {
        Object ids = params.get("ids");
        if (ids instanceof List<?> list) {
            CodegenPreviewMockStore.batchDelete(businessCode, entityName, list, pkName);
        }
        return Result.success();
    }

    @PostMapping("/checkUnique")
    @Operation(summary = "唯一性校验（Mock）", description = "data=true 表示已存在（与生成表单语义一致）")
    public Result<Boolean> checkUnique(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestBody Map<String, Object> params,
            @RequestParam(defaultValue = "id") String pkName) {
        boolean exists = CodegenPreviewMockStore.checkUnique(businessCode, entityName, params, pkName);
        return Result.success(exists);
    }

    @PostMapping("/checkUniqueCombo")
    public Result<Boolean> checkUniqueCombo(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestBody Map<String, Object> params,
            @RequestParam(defaultValue = "id") String pkName) {
        boolean exists = CodegenPreviewMockStore.checkUnique(businessCode, entityName, params, pkName);
        return Result.success(exists);
    }

    @PostMapping("/seed")
    @Operation(summary = "批量填充示例数据", description = "rows 由前端按字段类型生成；clearFirst=true 时先清空该表 Mock")
    public Result<Integer> seed(
            @PathVariable String businessCode,
            @PathVariable String entityName,
            @RequestBody Map<String, Object> body) {
        boolean clearFirst = Boolean.TRUE.equals(body.get("clearFirst"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.get("rows");
        int n = CodegenPreviewMockStore.seedRows(businessCode, entityName, rows, clearFirst);
        return Result.success(n);
    }
}
