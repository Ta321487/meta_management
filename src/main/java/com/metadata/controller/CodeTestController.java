package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(description = "测试代码生成")
    public Result<Map<String, Object>> testCode(
            @Parameter(description = "表编码") @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @RequestParam(required = false) String businessCode,
            @RequestParam(defaultValue = "false") boolean useInterface,
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        // 生成所有代码（与代码生成页开关一致，便于测试验证码扩展包）
        Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName, businessCode, useInterface, captchaEnabled);

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
        testConfigMap.put("Application.java", Map.of("name", "启动类", "type", "Java代码"));
        testConfigMap.put("application.yml", Map.of("name", "配置文件", "type", "YAML配置"));
        testConfigMap.put("List.vue", Map.of("name", "列表页", "type", "Vue组件"));
        testConfigMap.put("Form.vue", Map.of("name", "表单页", "type", "Vue组件"));
        testConfigMap.put("routes.js", Map.of("name", "路由配置", "type", "前端配置"));
        testConfigMap.put("auth.js", Map.of("name", "认证 API", "type", "前端配置"));
        testConfigMap.put("CaptchaInput.vue", Map.of("name", "验证码组件", "type", "Vue组件"));
        testConfigMap.put("AuthController.java", Map.of("name", "认证控制器", "type", "Java代码"));
        testConfigMap.put("CaptchaService.java", Map.of("name", "验证码服务", "type", "Java代码"));
        testConfigMap.put("AuthService.java", Map.of("name", "认证服务接口", "type", "Java代码"));
        testConfigMap.put("AuthServiceImpl.java", Map.of("name", "认证服务实现", "type", "Java代码"));
        testConfigMap.put("LoginRequest.java", Map.of("name", "登录请求 DTO", "type", "Java代码"));
        testConfigMap.put("LoginInterceptor.java", Map.of("name", "登录拦截器", "type", "Java代码"));
        testConfigMap.put("InterceptorConfig.java", Map.of("name", "拦截器配置", "type", "Java代码"));

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
            
            // 处理API测试信息
            List<Map<String, String>> apiTests = new ArrayList<>();
            if (isSuccess) {
                apiTests = processApiTests(codeType, code, codeMap);
            }
            testResult.put("apiTests", apiTests);

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

    }

    /**
     * 处理API测试信息
     */
    private List<Map<String, String>> processApiTests(String codeType, String code, Map<String, String> codeMap) {
        List<Map<String, String>> apiTests = new ArrayList<>();
        
        // 根据不同的代码类型处理API测试信息
        switch (codeType) {
            case "Controller.java":
                // 从Controller代码中提取API接口信息
                apiTests = extractControllerApiInfo(code);
                break;
            case "Service.java":
                // Service被Controller调用
                Map<String, String> serviceApi = new HashMap<>();
                serviceApi.put("method", "CALL");
                serviceApi.put("path", "被Controller调用");
                serviceApi.put("name", "服务调用关系");
                serviceApi.put("status", "success");
                apiTests.add(serviceApi);
                break;
            case "Mapper.java":
                // Mapper被Service调用
                Map<String, String> mapperApi = new HashMap<>();
                mapperApi.put("method", "CALL");
                mapperApi.put("path", "被Service调用");
                mapperApi.put("name", "数据访问调用关系");
                mapperApi.put("status", "success");
                apiTests.add(mapperApi);
                break;
            case "Mapper.xml":
                // Mapper.xml被Mapper.java调用
                Map<String, String> mapperXmlApi = new HashMap<>();
                mapperXmlApi.put("method", "CALL");
                mapperXmlApi.put("path", "被Mapper.java调用");
                mapperXmlApi.put("name", "映射文件调用关系");
                mapperXmlApi.put("status", "success");
                apiTests.add(mapperXmlApi);
                break;
            case "List.vue":
                // Vue列表页可能调用的API
                Map<String, String> listVueApi = new HashMap<>();
                listVueApi.put("method", "GET");
                listVueApi.put("path", "/api/[businessCode]/[entityName]/list");
                listVueApi.put("name", "获取列表数据");
                listVueApi.put("status", "success");
                apiTests.add(listVueApi);
                break;
            case "Form.vue":
                // Vue表单页可能调用的API
                Map<String, String> formVueApi1 = new HashMap<>();
                formVueApi1.put("method", "POST");
                formVueApi1.put("path", "/api/[businessCode]/[entityName]/add");
                formVueApi1.put("name", "新增数据");
                formVueApi1.put("status", "success");
                apiTests.add(formVueApi1);
                
                Map<String, String> formVueApi2 = new HashMap<>();
                formVueApi2.put("method", "POST");
                formVueApi2.put("path", "/api/[businessCode]/[entityName]/update");
                formVueApi2.put("name", "更新数据");
                formVueApi2.put("status", "success");
                apiTests.add(formVueApi2);
                break;
            case "routes.js":
                // 路由配置定义的前端路由
                Map<String, String> routeApi = new HashMap<>();
                routeApi.put("method", "ROUTE");
                routeApi.put("path", "/[entityName]/list");
                routeApi.put("name", "列表页路由");
                routeApi.put("status", "success");
                apiTests.add(routeApi);
                
                Map<String, String> routeApi2 = new HashMap<>();
                routeApi2.put("method", "ROUTE");
                routeApi2.put("path", "/[entityName]/form");
                routeApi2.put("name", "表单页路由");
                routeApi2.put("status", "success");
                apiTests.add(routeApi2);
                break;
            default:
                // 其他类型可以不显示API测试信息或显示相关信息
                break;
        }
        
        return apiTests;
    }
    
    /**
     * 从Controller代码中提取API接口信息
     */
    private List<Map<String, String>> extractControllerApiInfo(String controllerCode) {
        List<Map<String, String>> apiTests = new ArrayList<>();
        
        // 提取类上的@RequestMapping注解，获取基础路径
        String basePath = "";
        int requestMappingIndex = controllerCode.indexOf("@RequestMapping");
        if (requestMappingIndex != -1) {
            int start = controllerCode.indexOf("(", requestMappingIndex);
            int end = controllerCode.indexOf(")", start);
            if (start != -1 && end != -1) {
                String requestMappingValue = controllerCode.substring(start + 1, end).trim();
                // 提取引号内的内容
                if (requestMappingValue.startsWith("\"")) {
                    int quoteStart = requestMappingValue.indexOf("\"");
                    int quoteEnd = requestMappingValue.indexOf("\"", quoteStart + 1);
                    if (quoteStart != -1 && quoteEnd != -1) {
                        basePath = requestMappingValue.substring(quoteStart + 1, quoteEnd);
                        // 移除路径中的DEFAULT，如果存在
                        if (basePath.contains("/DEFAULT/")) {
                            basePath = basePath.replace("/DEFAULT/", "/");
                        } else if (basePath.equals("/DEFAULT")) {
                            basePath = "";
                        }
                    }
                }
            }
        }
        
        // 提取各个方法的API信息
        // 首先找到所有的方法定义（以@开头的注解）
        String[] lines = controllerCode.split("\n");
        String currentMethod = "";
        String currentPath = "";
        String currentName = "";
        StringBuilder commentBuilder = new StringBuilder();
        boolean inComment = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // 处理注释
            if (line.startsWith("/**")) {
                // 开始注释块
                inComment = true;
                commentBuilder.setLength(0);
                // 提取注释内容（如果注释在同一行）
                if (line.length() > 3) {
                    String commentPart = line.substring(3).trim();
                    if (commentPart.endsWith("*/")) {
                        // 单行注释
                        commentPart = commentPart.substring(0, commentPart.length() - 2).trim();
                        if (!commentPart.isEmpty()) {
                            commentBuilder.append(commentPart);
                        }
                        inComment = false;
                    } else if (!commentPart.isEmpty()) {
                        commentBuilder.append(commentPart);
                    }
                }
            } else if (inComment) {
                // 在注释块内
                if (line.endsWith("*/")) {
                    // 结束注释块
                    String commentPart = line.substring(0, line.length() - 2).trim();
                    if (line.startsWith("* ")) {
                        commentPart = line.substring(2, line.length() - 2).trim();
                    }
                    if (!commentPart.isEmpty()) {
                        commentBuilder.append(commentPart);
                    }
                    inComment = false;
                } else {
                    // 注释行
                    String commentPart = line;
                    if (line.startsWith("* ")) {
                        commentPart = line.substring(2).trim();
                    } else if (line.startsWith("*")) {
                        commentPart = line.substring(1).trim();
                    }
                    if (!commentPart.isEmpty()) {
                        commentBuilder.append(commentPart);
                    }
                }
            }
            
            // 检查是否是HTTP方法注解
            if (line.startsWith("@GetMapping") || line.startsWith("@PostMapping") || 
                line.startsWith("@PutMapping") || line.startsWith("@DeleteMapping")) {
                
                // 提取HTTP方法
                if (line.startsWith("@GetMapping")) {
                    currentMethod = "GET";
                } else if (line.startsWith("@PostMapping")) {
                    currentMethod = "POST";
                } else if (line.startsWith("@PutMapping")) {
                    currentMethod = "PUT";
                } else if (line.startsWith("@DeleteMapping")) {
                    currentMethod = "DELETE";
                }
                
                // 提取方法路径
                int start = line.indexOf("(");
                int end = line.indexOf(")");
                if (start != -1 && end != -1) {
                    String pathValue = line.substring(start + 1, end).trim();
                    if (pathValue.startsWith("\"")) {
                        int quoteStart = pathValue.indexOf("\"");
                        int quoteEnd = pathValue.indexOf("\"", quoteStart + 1);
                        if (quoteStart != -1 && quoteEnd != -1) {
                            currentPath = pathValue.substring(quoteStart + 1, quoteEnd);
                        }
                    }
                }
                
                // 使用提取的注释作为接口名称
                currentName = commentBuilder.toString().trim();
                commentBuilder.setLength(0);
                
                // 组合完整路径
                String fullPath = basePath + currentPath;
                
                // 添加到API测试列表
                Map<String, String> apiInfo = new HashMap<>();
                apiInfo.put("method", currentMethod);
                apiInfo.put("path", fullPath);
                apiInfo.put("name", currentName.isEmpty() ? "未命名接口" : currentName);
                apiInfo.put("status", "success");
                apiTests.add(apiInfo);
            }
        }
        
        return apiTests;
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