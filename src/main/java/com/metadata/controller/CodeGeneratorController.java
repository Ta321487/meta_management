package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String sql = codeGeneratorService.generateCreateTableSQL(tableCode, businessCode);
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
            @RequestParam(defaultValue = "com.example.entity") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateEntity(tableCode, packageName, businessCode);
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
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateController(tableCode, packageName, businessCode);
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
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateService(tableCode, packageName, businessCode);
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
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateMapper(tableCode, packageName, businessCode);
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
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateMapperXml(tableCode, packageName, businessCode);
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
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateVueList(tableCode, businessCode);
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
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateVueForm(tableCode, businessCode);
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
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName, businessCode);
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
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateRoutes(tableCode, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成业务系统下所有表的完整代码包
     */
    @GetMapping("/allByBusinessSystem/{businessCode}")
    @Operation(description = "生成业务系统下所有表的代码")
    public Result<Map<String, Map<String, String>>> generateAllByBusinessSystem(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            Map<String, Map<String, String>> codeMap = codeGeneratorService.generateAllByBusinessSystem(businessCode, packageName);
            return Result.success(codeMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成业务系统下所有表的建表SQL
     */
    @GetMapping("/sqlByBusinessSystem/{businessCode}")
    @Operation(description = "生成业务系统下所有表的SQL")
    public Result<Map<String, String>> generateAllSQLByBusinessSystem(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode) {
        try {
            Map<String, String> sqlMap = codeGeneratorService.generateAllSQLByBusinessSystem(businessCode);
            return Result.success(sqlMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


}

