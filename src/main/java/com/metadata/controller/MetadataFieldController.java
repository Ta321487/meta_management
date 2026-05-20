package com.metadata.controller;

import com.metadata.common.BatchDeleteRequest;
import com.metadata.common.DeleteConstraintRequest;
import com.metadata.common.FieldMigrateBatchRequest;
import com.metadata.common.FieldMigrateRequest;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.common.TableConstraintItem;
import com.metadata.entity.MetadataField;
import com.metadata.service.MetadataFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        fieldService.add(field);
        return Result.success();
    }

    /**
     * 更新字段
     */
    @PostMapping("/update")
    @Operation(summary = "更新字段", description = "更新元数据字段信息")
    public Result<?> update(@Parameter(description = "字段信息") @RequestBody MetadataField field) {
        fieldService.update(field);
        return Result.success();
    }

    /**
     * 删除字段
     */
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除字段", description = "根据ID删除元数据字段")
    public Result<?> delete(@Parameter(description = "字段ID") @PathVariable Long id) {
        fieldService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除字段
     */
    @PostMapping("/batchDelete")
    @Operation(summary = "批量删除字段", description = "根据ID列表批量删除元数据字段")
    public Result<?> batchDelete(@Parameter(description = "包含ids列表的参数") @RequestBody BatchDeleteRequest request) {
        fieldService.batchDelete(request.getIds());
        return Result.success();
    }

    /**
     * 查询表的所有字段（支持分页）
     */
    @GetMapping("/list/{tableCode}")
    @Operation(summary = "查询字段列表", description = "默认仅返回上级启用链路下的启用字段；字段管理传 includeDisabled=true 可查全部")
    public Result<?> listByTableCode(
            @Parameter(description = "表编码") @PathVariable String tableCode,
            @Parameter(description = "业务系统编码") @RequestParam(required = false) String businessCode,
            @Parameter(description = "为 true 时包含停用字段及停用上级下的字段（管理端）") @RequestParam(required = false) Boolean includeDisabled,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页大小") @RequestParam(required = false) Integer size) {
        boolean inc = Boolean.TRUE.equals(includeDisabled);
        String bc = businessCode != null ? businessCode : "";
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataField> pageResult = fieldService.pageByTableCode(tableCode, bc, pageRequest, inc);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataField> list = fieldService.listByTableCode(tableCode, bc, inc);
        return Result.success(list);
    }

    /**
     * 查询业务库物理表已有列名（用于常用字段预览、避免重复 ADD COLUMN）
     */
    @GetMapping("/physicalColumns/{tableCode}")
    @Operation(summary = "物理表列名", description = "返回业务库中该逻辑表对应物理表已存在的列名列表")
    public Result<List<String>> listPhysicalColumns(
            @Parameter(description = "表编码") @PathVariable String tableCode,
            @Parameter(description = "业务系统编码") @RequestParam(required = false) String businessCode) {
        return Result.success(fieldService.listPhysicalColumnNames(tableCode, businessCode));
    }

    /**
     * 从物理库补登记当前表在元数据中缺失的字段（不 ALTER 物理表）
     */
    @PostMapping("/syncFromPhysical/{tableCode}")
    @Operation(summary = "同步缺失字段", description = "读取业务库物理表结构，仅将尚未登记的列写入元数据")
    public Result<Map<String, Object>> syncFromPhysical(
            @Parameter(description = "表编码") @PathVariable String tableCode,
            @Parameter(description = "业务系统编码") @RequestParam(required = false) String businessCode) {
        return Result.success(fieldService.syncMissingFieldsFromPhysical(tableCode, businessCode));
    }

    @PostMapping("/migrate")
    @Operation(summary = "迁移字段到其他表", description = "目标表登记元数据并 ADD COLUMN（若缺列）；可选拷贝数据、删除源表字段")
    public Result<Map<String, Object>> migrateField(@RequestBody FieldMigrateRequest request) {
        return Result.success(fieldService.migrateField(request));
    }

    @PostMapping("/migrate/batch")
    @Operation(summary = "批量迁移字段到其他表", description = "共用目标表与迁移选项，按字段 ID 逐条迁移；每条独立事务，部分失败不回滚已成功项")
    public Result<Map<String, Object>> migrateFieldBatch(@RequestBody FieldMigrateBatchRequest request) {
        return Result.success(fieldService.migrateFieldBatch(request));
    }

    /**
     * 获取表的约束列表
     */
    @GetMapping("/constraint/list/{tableCode}")
    @Operation(summary = "获取约束列表", description = "根据表编码查询约束列表")
    public Result<List<TableConstraintItem>> getConstraints(
            @Parameter(description = "表编码") @PathVariable String tableCode) {
        List<Map<String, Object>> constraints = fieldService.getConstraints(tableCode);
        return Result.success(constraints.stream().map(TableConstraintItem::fromMap).collect(Collectors.toList()));
    }

    /**
     * 删除约束
     */
    @PostMapping("/constraint/delete")
    @Operation(summary = "删除约束", description = "删除表的约束")
    public Result<?> deleteConstraint(
            @Parameter(description = "删除约束请求参数") @RequestBody DeleteConstraintRequest request) {
        // 转换为Map以便兼容现有服务实现
        Map<String, Object> params = new HashMap<>();
        params.put("id", request.getId());
        params.put("constraintName", request.getConstraintName());
        params.put("tableName", request.getTableName());
        params.put("tableCode", request.getTableCode());
        params.put("fieldName", request.getFieldName());
        fieldService.deleteConstraint(params);
        return Result.success();
    }
    
    /**
     * 批量更新字段状态
     */
    @PostMapping("/batchUpdateStatus")
    @Operation(summary = "批量更新字段状态", description = "批量更新字段的启用/禁用状态")
    public Result<?> batchUpdateStatus(@Parameter(description = "包含ids列表的参数") @RequestBody BatchDeleteRequest request, 
                                  @Parameter(description = "目标状态，1-启用，0-禁用") @RequestParam Integer status) {
        fieldService.batchUpdateStatus(request.getIds(), status);
        return Result.success();
    }
}

