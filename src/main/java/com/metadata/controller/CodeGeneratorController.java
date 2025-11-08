package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 代码生成控制器
 */
@RestController
@RequestMapping("/api/codegen")
public class CodeGeneratorController {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    /**
     * 生成建表SQL
     */
    @GetMapping("/sql/{tableCode}")
    public Result<String> generateSQL(@PathVariable String tableCode) {
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
    public Result<String> generateEntity(@PathVariable String tableCode, 
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
    public Result<String> generateController(@PathVariable String tableCode,
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
    public Result<String> generateService(@PathVariable String tableCode,
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
    public Result<String> generateMapper(@PathVariable String tableCode,
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
    public Result<String> generateMapperXml(@PathVariable String tableCode,
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
    public Result<String> generateVueList(@PathVariable String tableCode) {
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
    public Result<String> generateVueForm(@PathVariable String tableCode) {
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
    public Result<Map<String, String>> generateAll(@PathVariable String tableCode,
                                                    @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName);
            return Result.success(codeMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}

