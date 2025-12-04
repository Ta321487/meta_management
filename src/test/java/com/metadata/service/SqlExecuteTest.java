package com.metadata.service;

import com.metadata.mapper.MetadataFieldMapper;
import org.junit.jupiter.api.Test;
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
        assert (Boolean) alterResult.get("success");
        
        // 验证字段是否同步到metadata_field表
        String tableCode = "test_tb".toUpperCase();
        String fieldCode = "STU_AGE";
        com.metadata.entity.MetadataField field = fieldMapper.selectByCode(tableCode, fieldCode);
        assert field != null;
        System.out.println("字段同步成功！字段信息: " + field);
        
        // 验证字段信息是否正确
        assert field.getFieldName().equals("STU_AGE");
        assert field.getFieldType().equalsIgnoreCase("INT");
        assert field.getLabel().equals("年龄");
        assert field.getIsRequired() == 1;
        
        System.out.println("测试通过！ALTER TABLE添加字段能正确同步到metadata_field表。");
    }
}