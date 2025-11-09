package com.metadata.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 代码测试服务
 * 用于验证生成的代码和模拟API接口测试
 */
@Service
public class CodeTestService {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    /**
     * 测试生成的代码
     * @param tableCode 表编码
     * @param packageName 包名
     * @return 测试结果
     */
    public Map<String, Object> testGeneratedCode(String tableCode, String packageName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> testResults = new ArrayList<>();

        try {
            // 1. 生成所有代码
            Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName);

            // 2. 验证SQL语法
            Map<String, Object> sqlTest = validateSQL(codeMap.get("create_table.sql"));
            testResults.add(sqlTest);

            // 3. 获取类名和实体名
            String className = convertToClassName(tableCode);
            String entityName = convertToEntityName(tableCode);

            // 4. 验证Java代码语法
            Map<String, Object> entityTest = validateJavaCode(
                codeMap.get("Entity.java"),
                className
            );
            testResults.add(entityTest);

            Map<String, Object> controllerTest = validateJavaCode(
                codeMap.get("Controller.java"),
                className + "Controller"
            );
            testResults.add(controllerTest);

            Map<String, Object> serviceTest = validateJavaCode(
                codeMap.get("Service.java"),
                className + "Service"
            );
            testResults.add(serviceTest);

            // 5. 模拟API接口测试
            Map<String, Object> apiTest = simulateAPITest(tableCode, packageName, codeMap, entityName);
            testResults.add(apiTest);

            // 6. 统计结果
            long successCount = testResults.stream()
                .filter(r -> "success".equals(r.get("status")))
                .count();
            long failCount = testResults.size() - successCount;

            result.put("success", failCount == 0);
            result.put("total", testResults.size());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("testResults", testResults);
            result.put("message", failCount == 0 ? "所有测试通过" : failCount + "个测试失败");

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "测试失败: " + e.getMessage());
            result.put("testResults", testResults);
        }

        return result;
    }

    /**
     * 验证SQL语法
     */
    private Map<String, Object> validateSQL(String sql) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "SQL语法验证");
        result.put("name", "create_table.sql");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (sql == null || sql.trim().isEmpty()) {
            errors.add("SQL为空");
        } else {
            String upperSql = sql.toUpperCase();
            
            // 检查必需的关键字
            if (!upperSql.contains("CREATE TABLE")) {
                errors.add("缺少CREATE TABLE语句");
            }
            if (!upperSql.contains("PRIMARY KEY")) {
                errors.add("缺少PRIMARY KEY定义");
            }
            
            // 检查表名
            if (!upperSql.contains("CREATE TABLE")) {
                errors.add("表名未定义");
            }

            // 警告检查
            if (!upperSql.contains("ENGINE")) {
                warnings.add("未指定存储引擎（建议使用InnoDB）");
            }
            if (!upperSql.contains("CHARSET") && !upperSql.contains("CHARACTER SET")) {
                warnings.add("未指定字符集（建议使用utf8mb4）");
            }
        }

        result.put("status", errors.isEmpty() ? "success" : "error");
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("message", errors.isEmpty() ? "SQL语法验证通过" : "发现" + errors.size() + "个错误");

        return result;
    }

    /**
     * 验证Java代码语法
     */
    private Map<String, Object> validateJavaCode(String code, String className) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "Java语法验证");
        result.put("name", className + ".java");

        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (code == null || code.trim().isEmpty()) {
            errors.add("代码为空");
            result.put("status", "error");
            result.put("errors", errors);
            result.put("warnings", warnings);
            result.put("message", "代码为空");
            return result;
        }

        // 基本语法检查
        if (!code.contains("package ")) {
            errors.add("缺少package声明");
        }
        if (!code.contains("public class " + className) && !code.contains("class " + className)) {
            warnings.add("类名可能不匹配: " + className);
        }

        // 检查导入
        if (code.contains("@RestController") && !code.contains("import org.springframework.web.bind.annotation.RestController")) {
            warnings.add("可能缺少RestController导入");
        }
        if (code.contains("@Service") && !code.contains("import org.springframework.stereotype.Service")) {
            warnings.add("可能缺少Service导入");
        }

        // 尝试编译检查（简化版，不实际编译）
        // 检查括号匹配
        long openBraces = code.chars().filter(ch -> ch == '{').count();
        long closeBraces = code.chars().filter(ch -> ch == '}').count();
        if (openBraces != closeBraces) {
            errors.add("大括号不匹配: 开括号" + openBraces + "个, 闭括号" + closeBraces + "个");
        }

        long openParens = code.chars().filter(ch -> ch == '(').count();
        long closeParens = code.chars().filter(ch -> ch == ')').count();
        if (openParens != closeParens) {
            errors.add("小括号不匹配: 开括号" + openParens + "个, 闭括号" + closeParens + "个");
        }

        result.put("status", errors.isEmpty() ? "success" : "error");
        result.put("errors", errors);
        result.put("warnings", warnings);
        result.put("message", errors.isEmpty() ? "Java语法验证通过" : "发现" + errors.size() + "个错误");

        return result;
    }

    /**
     * 模拟API接口测试
     */
    private Map<String, Object> simulateAPITest(String tableCode, String packageName, Map<String, String> codeMap, String entityName) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", "API接口模拟测试");
        result.put("name", "Controller API");

        List<Map<String, Object>> apiTests = new ArrayList<>();
        String controllerCode = codeMap.get("Controller.java");

        if (controllerCode == null || controllerCode.trim().isEmpty()) {
            result.put("status", "error");
            result.put("message", "Controller代码为空");
            result.put("apiTests", apiTests);
            return result;
        }

        // 解析Controller代码，提取API接口
        String basePath = "/api/" + entityName;

        // 测试各个API接口
        apiTests.add(testAPI("POST", basePath + "/add", "新增接口", controllerCode.contains("@PostMapping(\"/add\")")));
        apiTests.add(testAPI("POST", basePath + "/update", "更新接口", controllerCode.contains("@PostMapping(\"/update\")")));
        apiTests.add(testAPI("POST", basePath + "/delete", "删除接口", controllerCode.contains("@PostMapping(\"/delete\")")));
        apiTests.add(testAPI("POST", basePath + "/batchDelete", "批量删除接口", controllerCode.contains("@PostMapping(\"/batchDelete\")")));
        apiTests.add(testAPI("GET", basePath + "/{id}", "根据ID查询接口", controllerCode.contains("@GetMapping(\"/{id}\")")));
        apiTests.add(testAPI("GET", basePath + "/list", "列表查询接口", controllerCode.contains("@GetMapping(\"/list\")")));
        apiTests.add(testAPI("POST", basePath + "/page", "分页查询接口", controllerCode.contains("@PostMapping(\"/page\")")));
        apiTests.add(testAPI("POST", basePath + "/listByCondition", "条件查询接口", controllerCode.contains("@PostMapping(\"/listByCondition\")")));

        // 统计结果
        long successCount = apiTests.stream()
            .filter(t -> "success".equals(t.get("status")))
            .count();
        long failCount = apiTests.size() - successCount;

        result.put("status", failCount == 0 ? "success" : "warning");
        result.put("apiTests", apiTests);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("message", failCount == 0 ? "所有API接口定义正确" : failCount + "个API接口可能有问题");

        return result;
    }

    /**
     * 测试单个API接口
     */
    private Map<String, Object> testAPI(String method, String path, String name, boolean exists) {
        Map<String, Object> test = new HashMap<>();
        test.put("method", method);
        test.put("path", path);
        test.put("name", name);
        test.put("status", exists ? "success" : "error");
        test.put("message", exists ? "接口定义存在" : "接口定义不存在");
        return test;
    }

    /**
     * 工具方法：转换为类名（大驼峰）
     */
    private String convertToClassName(String code) {
        return convertToCamelCase(code, true);
    }

    /**
     * 工具方法：转换为实体名（小驼峰）
     */
    private String convertToEntityName(String code) {
        return convertToCamelCase(code, false);
    }

    /**
     * 工具方法：转换为驼峰命名
     */
    private String convertToCamelCase(String code, boolean firstUpper) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        String[] parts = code.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0 && !firstUpper) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        return sb.toString();
    }
}

