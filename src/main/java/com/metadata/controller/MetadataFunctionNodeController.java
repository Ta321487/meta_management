package com.metadata.controller;

import com.metadata.common.BatchDeleteRequest;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.common.UpdateSortRequest;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataModule;
import com.metadata.service.MetadataFunctionNodeService;
import com.metadata.service.MetadataModuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 功能节点控制器
 */
@RestController
@RequestMapping("/api/node")
@Tag(name = "功能节点管理", description = "功能节点相关API")
public class MetadataFunctionNodeController {

    @Autowired
    private MetadataFunctionNodeService nodeService;
    
    @Autowired
    private MetadataModuleService moduleService;

    @PostMapping("/add")
    @Operation(summary = "新增功能节点", description = "在指定模块下新增菜单/页面等功能节点")
    public Result<?> add(@Parameter(description = "功能节点信息") @RequestBody MetadataFunctionNode node) {
        nodeService.add(node);
        return Result.success();

    }

    @PostMapping("/update")
    @Operation(summary = "更新功能节点", description = "修改功能节点名称、路由、排序等信息")
    public Result<?> update(@Parameter(description = "功能节点信息") @RequestBody MetadataFunctionNode node) {
        nodeService.update(node);
        return Result.success();

    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除功能节点", description = "根据主键 ID 删除功能节点")
    public Result<?> delete(@Parameter(description = "节点 ID") @PathVariable Long id) {
        nodeService.delete(id);
        return Result.success();

    }

    @PostMapping("/batchDelete")
    @Operation(summary = "批量删除功能节点", description = "根据 ID 列表批量删除")
    public Result<?> batchDelete(@Parameter(description = "包含 ids 列表") @RequestBody BatchDeleteRequest request) {
        nodeService.batchDelete(request.getIds());
        return Result.success();

    }

    @GetMapping("/list/{moduleCode}")
    @Operation(summary = "查询功能节点列表", description = "按模块编码查询节点；传入 current、size 时分页返回")
    public Result<?> listByModuleCode(
            @Parameter(description = "模块编码") @PathVariable String moduleCode,
            @Parameter(description = "当前页码") @RequestParam(required = false) Integer current,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer size) {
        // 获取模块信息，用于获取业务系统编码
        MetadataModule module = moduleService.getByCode(moduleCode);
        String businessCode = "DEFAULT"; // 默认业务系统
        
        // 如果模块存在，使用模块的业务系统编码
        if (module != null && module.getBusinessCode() != null) {
            businessCode = module.getBusinessCode();
        }
        
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            // 使用带业务系统参数的分页查询方法
            PageResult<MetadataFunctionNode> pageResult = nodeService.pageByModuleCodeAndBusinessCode(moduleCode, businessCode, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        // 使用带业务系统参数的查询方法
        List<MetadataFunctionNode> list = nodeService.listByModuleCodeAndBusinessCode(moduleCode, businessCode);
        return Result.success(list);
    }

    @PostMapping("/updateSort")
    @Operation(summary = "更新节点排序", description = "调整单个功能节点的 sort 值")
    public Result<?> updateSort(@Parameter(description = "节点 ID 与排序值") @RequestBody UpdateSortRequest request) {
        nodeService.updateSort(request.getId(), request.getSort());
        return Result.success();

    }
}

