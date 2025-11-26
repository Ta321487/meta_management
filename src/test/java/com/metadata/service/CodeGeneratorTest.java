package com.metadata.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CodeGeneratorTest {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private CodeTestService codeTestService;

    @Test
    public void testCreateTableSQL() throws Exception {
        // 测试生成建表SQL
        String tableCode = "STUDENT_TABLE";
        String sql = codeGeneratorService.generateCreateTableSQL(tableCode);
        System.out.println("生成的SQL:");
        System.out.println(sql);
        
        // 验证SQL是否正确生成
        assert sql != null && !sql.isEmpty();
        assert sql.contains("CREATE TABLE");
        System.out.println("建表SQL生成成功！");
    }

    @Test
    public void testAllCodeGeneration() throws Exception {
        // 测试生成所有代码
        String tableCode = "STUDENT_TABLE";
        String packageName = "com.example";
        Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName);
        
        // 验证代码是否正确生成
        assert codeMap != null;
        assert codeMap.containsKey("create_table.sql");
        System.out.println("所有代码生成成功！生成的文件数量: " + codeMap.size());
        
        // 打印生成的SQL以验证
        System.out.println("生成的SQL:");
        System.out.println(codeMap.get("create_table.sql"));
    }

    @Test
    public void testCodeTestService() {
        // 使用CodeTestService测试生成的代码
        String tableCode = "STUDENT_TABLE";
        String packageName = "com.example";
        Map<String, Object> testResult = codeTestService.testGeneratedCode(tableCode, packageName);
        
        // 打印测试结果
        System.out.println("测试结果:");
        System.out.println("成功: " + testResult.get("success"));
        System.out.println("消息: " + testResult.get("message"));
        System.out.println("成功数量: " + testResult.get("successCount"));
        System.out.println("失败数量: " + testResult.get("failCount"));
        
        // 验证测试是否通过
        assert Boolean.TRUE.equals(testResult.get("success"));
        System.out.println("代码测试通过！");
    }
}