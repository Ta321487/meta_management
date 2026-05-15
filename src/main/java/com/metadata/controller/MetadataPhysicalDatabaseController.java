package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataPhysicalDatabase;
import com.metadata.service.MetadataPhysicalDatabaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/physicalDatabase")
@Tag(name = "物理库管理", description = "登记与管理 MySQL 业务库")
public class MetadataPhysicalDatabaseController {

    @Autowired
    private MetadataPhysicalDatabaseService physicalDatabaseService;

    @GetMapping("/list")
    @Operation(summary = "物理库列表")
    public Result<List<MetadataPhysicalDatabase>> list() {
        return Result.success(physicalDatabaseService.listAll());
    }

    @PostMapping("/add")
    @Operation(summary = "新增登记")
    public Result<?> add(@RequestBody MetadataPhysicalDatabase row) {
        physicalDatabaseService.add(row);
        return Result.success();

    }

    @PostMapping("/update")
    @Operation(summary = "更新登记")
    public Result<?> update(@RequestBody MetadataPhysicalDatabase row) {
        physicalDatabaseService.update(row);
        return Result.success();

    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除库")
    public Result<?> delete(
            @Parameter(description = "主键") @PathVariable Long id,
            @Parameter(description = "是否同时在服务器上 DROP DATABASE") @RequestParam(defaultValue = "false") boolean dropOnInstance) {
        physicalDatabaseService.delete(id, dropOnInstance);
        return Result.success();
    }

    @PostMapping("/sync/{id}")
    @Operation(summary = "在实例上创建库（若不存在）")
    public Result<?> sync(@PathVariable Long id) {
        physicalDatabaseService.syncToInstance(id);
        return Result.success();

    }
}
