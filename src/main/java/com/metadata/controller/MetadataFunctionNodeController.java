package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.service.MetadataFunctionNodeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 功能节点控制器
 */
@RestController
@RequestMapping("/api/node")
@Tag(name = "功能节点管理", description = "功能节点相关API")
public class MetadataFunctionNodeController {

    @Autowired
    private MetadataFunctionNodeService nodeService;

    @PostMapping("/add")
    public Result<?> add(@RequestBody MetadataFunctionNode node) {
        try {
            nodeService.add(node);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataFunctionNode node) {
        try {
            nodeService.update(node);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            nodeService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list/{moduleCode}")
    public Result<?> listByModuleCode(@PathVariable String moduleCode,
                                      @RequestParam(required = false) Integer current,
                                      @RequestParam(required = false) Integer size) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataFunctionNode> pageResult = nodeService.pageByModuleCode(moduleCode, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataFunctionNode> list = nodeService.listByModuleCode(moduleCode);
        return Result.success(list);
    }

    @PostMapping("/updateSort")
    public Result<?> updateSort(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            Integer sort = Integer.valueOf(params.get("sort").toString());
            nodeService.updateSort(id, sort);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

