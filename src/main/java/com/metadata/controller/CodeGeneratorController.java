package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成控制器
 */
@RestController
@RequestMapping("/api/codegen")
@Tag(name = "代码生成管理", description = "代码生成相关API")
public class CodeGeneratorController {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    /**
     * 生成建表SQL
     */
    @GetMapping("/sql/{tableCode}")
    @Operation(summary = "生成SQL语句")
    public Result<String> generateSQL(
            @Parameter(description = "表编码")
            @PathVariable String tableCode) {
        try {
            String sql = codeGeneratorService.generateCreateTableSQL(tableCode);
            return Result.success(sql);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成实体类
     */
    @GetMapping("/entity/{tableCode}")
    @Operation(summary = "生成实体类")
    public Result<String> generateEntity(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example.entity") String packageName) {
        try {
            String code = codeGeneratorService.generateEntity(tableCode, packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Controller
     */
    @GetMapping("/controller/{tableCode}")
    @Operation(summary = "生成Controller类")
    public Result<String> generateController(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            String code = codeGeneratorService.generateController(tableCode, packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Service
     */
    @GetMapping("/service/{tableCode}")
    @Operation(summary = "生成Service类")
    public Result<String> generateService(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            String code = codeGeneratorService.generateService(tableCode, packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Mapper接口
     */
    @GetMapping("/mapper/{tableCode}")
    @Operation(summary = "生成Mapper接口")
    public Result<String> generateMapper(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            String code = codeGeneratorService.generateMapper(tableCode, packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Mapper XML
     */
    @GetMapping("/mapperxml/{tableCode}")
    @Operation(summary = "生成MapperXML")
    public Result<String> generateMapperXml(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            String code = codeGeneratorService.generateMapperXml(tableCode, packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Vue列表页面
     */
    @GetMapping("/vue/list/{tableCode}")
    @Operation(summary = "生成Vue列表页")
    public Result<String> generateVueList(
            @Parameter(description = "表编码")
            @PathVariable String tableCode) {
        try {
            String code = codeGeneratorService.generateVueList(tableCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Vue表单页面
     */
    @GetMapping("/vue/form/{tableCode}")
    @Operation(summary = "生成Vue表单页")
    public Result<String> generateVueForm(
            @Parameter(description = "表编码")
            @PathVariable String tableCode) {
        try {
            String code = codeGeneratorService.generateVueForm(tableCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成所有代码
     */
    @GetMapping("/all/{tableCode}")
    @Operation(description = "一次性生成SQL、Controller、Service、Mapper、MapperXML、Vue列表、Vue表单")
    public Result<Map<String, String>> generateAll(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName);
            return Result.success(codeMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成路由配置（单独接口）
     */
    @GetMapping("/routes/{tableCode}")
    @Operation(description = "生成路由配置")
    public Result<String> generateRoutes(
            @Parameter(description = "表编码")
            @PathVariable String tableCode) {
        try {
            String code = codeGeneratorService.generateRoutes(tableCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


}

