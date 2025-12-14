package com.metadata.controller;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.Result;
import com.metadata.entity.MetadataBusinessRule;
import com.metadata.service.MetadataBusinessRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务规则控制器
 */
@RestController
@RequestMapping("/api/rule")
@Tag(name = "业务规则管理", description = "业务规则管理相关API")
public class MetadataBusinessRuleController {

    @Autowired
    private MetadataBusinessRuleService ruleService;

    @PostMapping("/add")
    @Operation(description = "添加业务规则")
    public Result<?> add(@Parameter(description = "业务规则信息") @RequestBody MetadataBusinessRule rule) {
        try {
            ruleService.add(rule);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    @Operation(description = "更新业务规则")
    public Result<?> update(@Parameter(description = "业务规则信息") @RequestBody MetadataBusinessRule rule) {
        try {
            ruleService.update(rule);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    @Operation(description = "根据id删除业务规则")
    public Result<?> delete(@Parameter(description = "业务规则信息") @PathVariable Long id) {
        try {
            ruleService.delete(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list/{moduleCode}")
    @Operation(description = "通过模块唯一代码分页查询")
    public Result<?> listByModuleCode(@Parameter(description = "模块唯一代码") @PathVariable String moduleCode,
                                      @Parameter(description = "当前页",required = true)
                                      @RequestParam(required = false) Integer current,
                                      @Parameter(description = "每页显示数量")
                                      @RequestParam(required = false) Integer size,
                                      @Parameter(description = "业务系统编码")
                                      @RequestParam(required = false) String businessCode) {
        // 如果传入了分页参数，使用分页查询
        if (current != null && size != null) {
            PageRequest pageRequest = new PageRequest();
            pageRequest.setCurrent(current);
            pageRequest.setSize(size);
            PageResult<MetadataBusinessRule> pageResult = ruleService.pageByModuleCode(moduleCode, businessCode, pageRequest);
            return Result.success(pageResult);
        }
        // 否则使用非分页查询（兼容旧接口）
        List<MetadataBusinessRule> list = ruleService.listByModuleCode(moduleCode, businessCode);
        return Result.success(list);
    }
}

