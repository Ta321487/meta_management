package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataBusinessRule;
import com.metadata.service.MetadataBusinessRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 业务规则控制器
 */
@RestController
@RequestMapping("/api/rule")
public class MetadataBusinessRuleController {

    @Autowired
    private MetadataBusinessRuleService ruleService;

    @PostMapping("/add")
    public Result<?> add(@RequestBody MetadataBusinessRule rule) {
        try {
            ruleService.add(rule);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody MetadataBusinessRule rule) {
        try {
            ruleService.update(rule);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/delete")
    public Result<?> delete(@RequestBody Map<String, Object> params) {
        try {
            Long id = Long.valueOf(params.get("id").toString());
            ruleService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list/{moduleCode}")
    public Result<List<MetadataBusinessRule>> listByModuleCode(@PathVariable String moduleCode) {
        List<MetadataBusinessRule> list = ruleService.listByModuleCode(moduleCode);
        return Result.success(list);
    }
}

