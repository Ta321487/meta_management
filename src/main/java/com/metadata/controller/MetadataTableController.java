package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataTable;
import com.metadata.service.MetadataTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表控制器
 */
@RestController
@RequestMapping("/api/table")
public class MetadataTableController {

    @Autowired
    private MetadataTableService tableService;

    /**
     * 新增表
     */
    @PostMapping("/add")
    public Result<?> add(@RequestBody MetadataTable table) {
        try {
            tableService.add(table);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新表
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataTable table) {
        try {
            tableService.update(table);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除表
     */
    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            tableService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 查询表详情
     */
    @GetMapping("/{tableCode}")
    public Result<MetadataTable> getByCode(@PathVariable String tableCode) {
        MetadataTable table = tableService.getByCode(tableCode);
        return Result.success(table);
    }

    /**
     * 查询所有表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) String tableName,
                         @RequestParam(required = false) Integer current,
                         @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataTable> pageResult = tableService.page(tableName, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataTable> list = tableService.list(tableName);
        return Result.success(list);
    }

    /**
     * 根据模块编码查询表
     */
    @GetMapping("/listByModule/{moduleCode}")
    public Result<List<MetadataTable>> listByModuleCode(@PathVariable String moduleCode) {
        List<MetadataTable> list = tableService.listByModuleCode(moduleCode);
        return Result.success(list);
    }
}

