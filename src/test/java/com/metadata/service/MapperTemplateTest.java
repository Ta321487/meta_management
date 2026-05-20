package com.metadata.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class MapperTemplateTest {

    @Test
    public void testMapperTemplateLoading() throws Exception {
        // 初始化Freemarker配置
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        
        // 创建测试数据模型
        Map<String, Object> dataModel = new HashMap<>();
        
        // 添加包名、类名和表名
        dataModel.put("packageName", "com.test");
        dataModel.put("className", "TestEntity");
        dataModel.put("tableName", "test_table");
        dataModel.put("businessName", "测试业务系统");
        
        // 创建表信息
        Map<String, Object> table = new HashMap<>();
        table.put("tableName", "test_table");
        table.put("tableCode", "TEST");
        table.put("description", "测试表描述");
        table.put("pkStrategy", "UUID");
        
        dataModel.put("table", table);
        
        // 创建字段列表
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> field1 = new HashMap<>();
        Map<String, Object> fieldInfo1 = new HashMap<>();
        fieldInfo1.put("fieldName", "id");
        fieldInfo1.put("fieldType", "BIGINT");
        fieldInfo1.put("isRequired", 1);
        fieldInfo1.put("label", "主键");
        fieldInfo1.put("formComponent", "primary_key");
        field1.put("field", fieldInfo1);
        field1.put("camelCaseName", "id");
        field1.put("field_has_next", true);
        fields.add(field1);
        
        Map<String, Object> field2 = new HashMap<>();
        Map<String, Object> fieldInfo2 = new HashMap<>();
        fieldInfo2.put("fieldName", "name");
        fieldInfo2.put("fieldType", "VARCHAR(100)");
        fieldInfo2.put("isRequired", 1);
        fieldInfo2.put("label", "名称");
        fieldInfo2.put("formComponent", "input");
        field2.put("field", fieldInfo2);
        field2.put("camelCaseName", "name");
        field2.put("field_has_next", false);
        fields.add(field2);
        
        dataModel.put("fields", fields);
        
        // 加载模板并处理
        Template template = cfg.getTemplate("mapper.xml.ftl");
        StringWriter writer = new StringWriter();
        
        try {
            template.process(dataModel, writer);
            String output = writer.toString();
            System.out.println("Mapper template processed successfully!");
            System.out.println("Output: " + output);
        } catch (TemplateException e) {
            System.err.println("Template processing error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}