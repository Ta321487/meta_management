package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.service.MetadataFunctionNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 功能节点控制器
 */
@RestController
@RequestMapping("/api/node")
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
    public Result<List<MetadataFunctionNode>> listByModuleCode(@PathVariable String moduleCode) {
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

