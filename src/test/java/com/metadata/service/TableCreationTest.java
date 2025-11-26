package com.metadata.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TableCreationTest {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    @Test
    public void testCreateTableSQL() throws Exception {
        // 测试生成建表SQL，确保模板语法正确
        String tableCode = "STUDENT_TABLE";
        String sql = codeGeneratorService.generateCreateTableSQL(tableCode);
        System.out.println("生成的SQL:");
        System.out.println(sql);
        
        // 验证SQL是否成功生成
        assert sql != null && !sql.isEmpty();
        assert sql.contains("CREATE TABLE");
        System.out.println("建表SQL生成成功！模板语法修复有效。");
    }
}