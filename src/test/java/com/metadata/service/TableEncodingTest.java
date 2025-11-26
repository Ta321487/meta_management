package com.metadata.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class TableEncodingTest {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Test
    public void testTableEncodingFormat() throws Exception {
        // 创建一个临时的表配置文件
        Map<String, Object> dataModel = new HashMap<>();
        Map<String, Object> table = new HashMap<>();
        table.put("tableName", "test_table");
        table.put("tableCode", "TEST");
        table.put("description", "测试表");
        table.put("pkStrategy", "AUTO");
        
        // 直接验证模板文件内容
        File templateFile = new File("src/main/resources/templates/create_table.sql.ftl");
        assert templateFile.exists() : "Template file not found";
        
        // 读取模板内容并验证编码格式设置
        java.nio.file.Path path = java.nio.file.Paths.get("src/main/resources/templates/create_table.sql.ftl");
        String templateContent = new String(java.nio.file.Files.readAllBytes(path));
        
        System.out.println("Template content verification:");
        System.out.println("Contains correct encoding: " + templateContent.contains("DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"));
        
        // 验证模板中包含正确的编码格式和校对规则
        assert templateContent.contains("DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci") : "Template does not contain correct encoding format";
        System.out.println("Table encoding format test passed!");
    }
}