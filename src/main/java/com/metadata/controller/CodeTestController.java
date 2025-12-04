package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeGeneratorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码测试控制器
 */
@RestController
@RequestMapping("/api")
@Tag(name = "代码测试管理", description = "代码测试相关API")
public class CodeTestController {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    /**
     * 测试代码生成
     */
    @GetMapping("/codetest/test/{tableCode}")
    public Result<Map<String, Object>> testCode(@PathVariable String tableCode, 
                                               @RequestParam(defaultValue = "com.example") String packageName) {
        try {
            // 生成所有代码
            Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName);
            
            // 构造测试结果
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> testResults = new ArrayList<>();
            
            // 定义测试项配置
            Map<String, Map<String, String>> testConfigMap = new HashMap<>();
            testConfigMap.put("create_table.sql", Map.of("name", "建表SQL", "type", "数据库脚本"));
            testConfigMap.put("Entity.java", Map.of("name", "实体类", "type", "Java代码"));
            testConfigMap.put("Controller.java", Map.of("name", "控制器", "type", "Java代码"));
            testConfigMap.put("Service.java", Map.of("name", "服务层", "type", "Java代码"));
            testConfigMap.put("Mapper.java", Map.of("name", "数据访问层", "type", "Java代码"));
            testConfigMap.put("Mapper.xml", Map.of("name", "映射文件", "type", "XML配置"));
            testConfigMap.put("List.vue", Map.of("name", "列表页", "type", "Vue组件"));
            testConfigMap.put("Form.vue", Map.of("name", "表单页", "type", "Vue组件"));
            testConfigMap.put("routes.js", Map.of("name", "路由配置", "type", "前端配置"));
            
            // 验证生成的每种代码类型
            for (Map.Entry<String, String> entry : codeMap.entrySet()) {
                String codeType = entry.getKey();
                String code = entry.getValue();
                
                Map<String, Object> testResult = new HashMap<>();
                Map<String, String> testConfig = testConfigMap.getOrDefault(codeType, Map.of("name", codeType, "type", "未知类型"));
                
                // 基本信息
                testResult.put("name", testConfig.get("name"));
                testResult.put("type", testConfig.get("type"));
                
                // 验证代码是否生成成功
                boolean isSuccess = code != null && !code.trim().isEmpty();
                String status = isSuccess ? "success" : "danger";
                String message = isSuccess ? "生成成功" : "生成失败：代码为空";
                
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();
                
                if (!isSuccess) {
                    errors.add("代码内容为空");
                } else {
                    // 验证模板之间的逻辑关系
                    try {
                        validateTemplateLogic(codeType, code, codeMap);
                    } catch (Exception e) {
                        warnings.add(e.getMessage());
                        if (status.equals("success")) {
                            status = "warning";
                        }
                    }
                }
                
                testResult.put("status", status);
                testResult.put("message", message);
                testResult.put("errors", errors);
                testResult.put("warnings", warnings);
                testResult.put("apiTests", new ArrayList<>());
                
                testResults.add(testResult);
            }
            
            // 计算测试统计
            int total = testResults.size();
            int successCount = (int) testResults.stream().filter(r -> "success".equals(r.get("status"))).count();
            int failCount = (int) testResults.stream().filter(r -> "danger".equals(r.get("status"))).count();
            boolean allSuccess = successCount == total;
            
            result.put("testResults", testResults);
            result.put("success", allSuccess);
            result.put("message", allSuccess ? "所有代码生成测试通过！" : "部分代码生成测试失败，请检查");
            result.put("total", total);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("generatedCode", codeMap);
            
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 验证模板之间的逻辑关系
     */
    private void validateTemplateLogic(String codeType, String code, Map<String, String> codeMap) throws Exception {
        // 简化的模板逻辑验证，避免正则表达式
        if ("Controller.java".equals(codeType)) {
            String serviceCode = codeMap.get("Service.java");
            if (serviceCode != null && !serviceCode.trim().isEmpty()) {
                // 检查Controller中是否包含Service关键字
                if (!code.contains("Service")) {
                    throw new Exception("Controller中未正确引用Service类");
                }
            }
        }
        
        // 验证Service与Mapper之间的调用关系
        if ("Service.java".equals(codeType)) {
            String mapperCode = codeMap.get("Mapper.java");
            if (mapperCode != null && !mapperCode.trim().isEmpty()) {
                // 检查Service中是否包含Mapper关键字
                if (!code.contains("Mapper")) {
                    throw new Exception("Service中未正确引用Mapper接口");
                }
            }
        }
        
        // 验证Vue组件之间的路由关系
        if ("routes.js".equals(codeType)) {
            // 检查路由配置中是否包含了列表页和表单页的关键字
            String listVueCode = codeMap.get("List.vue");
            String formVueCode = codeMap.get("Form.vue");
            
            if (listVueCode != null && !listVueCode.trim().isEmpty()) {
                // 检查路由配置中是否包含列表相关关键字
                if (!code.contains("List") && !code.contains("list")) {
                    throw new Exception("路由配置中未包含列表页组件");
                }
            }
            
            if (formVueCode != null && !formVueCode.trim().isEmpty()) {
                // 检查路由配置中是否包含表单相关关键字
                if (!code.contains("Form") && !code.contains("form")) {
                    throw new Exception("路由配置中未包含表单页组件");
                }
            }
        }
    }
}