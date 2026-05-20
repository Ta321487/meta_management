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
public class TemplateLoadingTest {

    @Test
    public void testTemplateLoading() throws Exception {
        // 初始化Freemarker配置
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(this.getClass(), "/templates");
        cfg.setDefaultEncoding("UTF-8");
        
        // 创建测试数据模型
        Map<String, Object> dataModel = new HashMap<>();
        
        // 创建表信息
        Map<String, Object> table = new HashMap<>();
        table.put("tableName", "test_table");
        table.put("tableCode", "TEST");
        table.put("description", "测试表描述");
        table.put("pkStrategy", "AUTO");
        
        dataModel.put("table", table);
        dataModel.put("tableName", table.get("tableName"));
        
        // 创建字段列表
        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> field = new HashMap<>();
        field.put("fieldName", "id");
        field.put("fieldType", "BIGINT");
        field.put("isRequired", 1);
        field.put("label", "主键");
        field.put("formComponent", "primary_key");
        field.put("field_has_next", false); // 标记是否有下一个字段
        
        fields.add(field);
        dataModel.put("fields", fields);
        
        // 加载模板并处理
        Template template = cfg.getTemplate("create_table.sql.ftl");
        StringWriter writer = new StringWriter();
        
        try {
            template.process(dataModel, writer);
            String output = writer.toString();
            System.out.println("Template processed successfully!");
            System.out.println("Output: " + output);
        } catch (TemplateException e) {
            System.err.println("Template processing error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}