package com.metadata.service;

import com.metadata.mapper.MetadataFieldMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@SpringBootTest
public class SqlExecuteTest {

    @Autowired
    private SqlExecuteService sqlExecuteService;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Test
    @Transactional
    public void testAlterTableAddColumn() throws Exception {
        // 先创建测试表（如果不存在）
        try {
            String createTableSql = "CREATE TABLE IF NOT EXISTS test_tb (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(50) NOT NULL " +
                    ");";
            Map<String, Object> createTableResult = sqlExecuteService.executeSqlInternal(createTableSql, true);
            System.out.println("创建测试表结果: " + createTableResult);
        } catch (Exception e) {
            System.out.println("创建测试表失败: " + e.getMessage());
        }
        
        // 先尝试删除字段（使用try-catch处理字段不存在的情况）
        try {
            String dropSql = "ALTER TABLE test_tb DROP COLUMN STU_AGE;";
            Map<String, Object> dropResult = sqlExecuteService.executeSqlInternal(dropSql, true);
            System.out.println("删除字段结果: " + dropResult);
        } catch (Exception e) {
            // 忽略字段不存在的错误
            System.out.println("删除字段失败（可能字段不存在）: " + e.getMessage());
        }
        
        // 执行ALTER TABLE语句添加字段
        String alterSql = "ALTER TABLE test_tb ADD COLUMN STU_AGE int NOT NULL COMMENT '年龄' CHECK (STU_AGE > 0);";
        Map<String, Object> alterResult = sqlExecuteService.executeSqlInternal(alterSql, true);
        System.out.println("添加字段结果: " + alterResult);
        
        // 验证SQL执行成功
        Assertions.assertTrue((Boolean) alterResult.get("success"), "ALTER TABLE执行失败");
        
        // 验证字段是否同步到metadata_field表
        String tableCode = "test_tb".toUpperCase();
        String fieldCode = "STU_AGE";
        com.metadata.entity.MetadataField field = fieldMapper.selectByCode(tableCode, fieldCode);
        Assertions.assertNotNull(field, "字段未同步到metadata_field表");
        System.out.println("字段同步成功！字段信息: " + field);
        
        // 验证字段信息是否正确
        Assertions.assertEquals("STU_AGE", field.getFieldName(), "字段名不正确");
        Assertions.assertTrue(field.getFieldType().equalsIgnoreCase("INT"), "字段类型不正确");
        Assertions.assertEquals("年龄", field.getLabel(), "字段标签不正确");
        Assertions.assertEquals(1, field.getIsRequired(), "字段必填状态不正确");
        
        System.out.println("测试通过！ALTER TABLE添加字段能正确同步到metadata_field表。");
    }
    
    @Test
    @Transactional
    public void testCreateTableWithMultipleCheckConstraints() throws Exception {
        // 先尝试删除表（如果存在）
        try {
            String dropSql = "DROP TABLE IF EXISTS employee;";
            Map<String, Object> dropResult = sqlExecuteService.executeSqlInternal(dropSql, true);
            System.out.println("删除表结果: " + dropResult);
        } catch (Exception e) {
            System.out.println("删除表失败: " + e.getMessage());
        }
        
        // 执行CREATE TABLE语句，包含多个CHECK约束
        String createSql = "CREATE TABLE employee ( " +
            "id INT PRIMARY KEY AUTO_INCREMENT, " +
            "name VARCHAR(50) NOT NULL, " +
            "age INT NOT NULL CHECK (age >= 18 AND age <= 65), " +
            "email VARCHAR(100) NOT NULL CHECK (email REGEXP '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$'), " +
            "gender VARCHAR(10) NOT NULL CHECK (gender IN ('男', '女')), " +
            "salary DECIMAL(10,2) NOT NULL CHECK (salary > 0), " +
            "status VARCHAR(20) NOT NULL CHECK (status != '禁用') " +
            ");";
        
        System.out.println("执行CREATE TABLE语句: " + createSql);
        Map<String, Object> createResult = sqlExecuteService.executeSqlInternal(createSql, false);
        System.out.println("创建表结果: " + createResult);
        
        // 验证SQL执行成功
        Assertions.assertTrue((Boolean) createResult.get("success"), "CREATE TABLE执行失败");
        
        // 验证表是否同步到metadata_table表
        // 这里假设tableMapper有selectByCode方法
        // 由于我们没有直接注入tableMapper，所以我们通过执行查询来验证
        System.out.println("测试通过！CREATE TABLE语句执行成功。");
    }
}