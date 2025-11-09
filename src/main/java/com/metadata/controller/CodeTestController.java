package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 代码测试控制器
 */
@RestController
@RequestMapping("/api/codetest")
public class CodeTestController {

    @Autowired
    private CodeTestService codeTestService;

    /**
     * 测试生成的代码
     */
    @GetMapping("/test/{tableCode}")
    public Result<Map<String, Object>> testCode(
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            Map<String, Object> result = codeTestService.testGeneratedCode(tableCode, packageName);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("测试失败: " + e.getMessage());
        }
    }
}

