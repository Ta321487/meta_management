package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成服务
 */
@Service
public class CodeGeneratorService {

    @Autowired
    private MetadataFieldService fieldService;

    @Autowired
    private MetadataTableService tableService;

    @Autowired
    private MetadataFunctionNodeMapper nodeMapper;

    private Configuration freemarkerConfig;

    public CodeGeneratorService() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        // 使用 ClassTemplateLoader 从类路径加载模板
        // 使用当前线程的类加载器，从类路径根目录开始查找，"/templates" 是相对于类路径根目录的路径
        ClassTemplateLoader templateLoader = new ClassTemplateLoader(
            Thread.currentThread().getContextClassLoader(), "/templates");
        freemarkerConfig.setTemplateLoader(templateLoader);

        freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    /**
     * 生成数据库建表SQL
     */
    public String generateCreateTableSQL(String tableCode) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        if (fields.isEmpty()) {
            throw new RuntimeException("表没有配置字段: " + tableCode);
        }

        // 为每个字段添加转换后的属性
        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("tableName", convertToTableName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("create_table.sql.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 准备字段列表，添加转换后的属性
     */
    private List<Map<String, Object>> prepareFieldList(List<MetadataField> fields) {
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (MetadataField field : fields) {
            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("field", field);
            fieldMap.put("fieldName", field.getFieldName());
            fieldMap.put("fieldType", field.getFieldType());
            fieldMap.put("label", field.getLabel());
            fieldMap.put("isRequired", field.getIsRequired());
            fieldMap.put("formComponent", field.getFormComponent());
            fieldMap.put("javaType", getJavaType(field.getFieldType()));
            fieldMap.put("camelCaseName", convertToCamelCase(field.getFieldName(), false));
            
            // 解析校验规则
            Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
            fieldMap.put("validationRules", validationRules);
            
            fieldList.add(fieldMap);
        }
        return fieldList;
    }

    /**
     * 解析校验规则JSON，提取正则表达式等信息
     */
    private Map<String, Object> parseValidationRule(String validateRule) {
        Map<String, Object> rules = new HashMap<>();
        // 初始化默认值，避免模板访问时出错
        rules.put("hasPattern", false);
        rules.put("hasOptions", false);
        
        if (validateRule == null || validateRule.trim().isEmpty()) {
            return rules;
        }
        
        try {
            JSONObject jsonObject = JSON.parseObject(validateRule);
            
            // 提取正则表达式
            if (jsonObject.containsKey("pattern")) {
                String pattern = jsonObject.getString("pattern");
                String message = jsonObject.getString("message");
                rules.put("hasPattern", true);
                rules.put("pattern", pattern);
                rules.put("patternMessage", message != null ? message : "格式不正确");
            } else {
                rules.put("hasPattern", false);
            }
            
            // 提取选项（用于下拉框）
            if (jsonObject.containsKey("options")) {
                Object options = jsonObject.get("options");
                rules.put("hasOptions", true);
                rules.put("options", options);
            } else {
                rules.put("hasOptions", false);
            }
            
            // 提取其他校验规则（如min、max等）
            if (jsonObject.containsKey("min")) {
                rules.put("min", jsonObject.get("min"));
            }
            if (jsonObject.containsKey("max")) {
                rules.put("max", jsonObject.get("max"));
            }
            if (jsonObject.containsKey("minLength")) {
                rules.put("minLength", jsonObject.get("minLength"));
            }
            if (jsonObject.containsKey("maxLength")) {
                rules.put("maxLength", jsonObject.get("maxLength"));
            }
            
        } catch (Exception e) {
            // JSON解析失败，忽略校验规则
            rules.put("hasPattern", false);
            rules.put("hasOptions", false);
        }
        
        return rules;
    }

    /**
     * 生成Java实体类
     */
    public String generateEntity(String tableCode, String packageName) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        if (fields.isEmpty()) {
            throw new RuntimeException("表没有配置字段: " + tableCode);
        }

        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("tableName", convertToTableName(table.getTableCode()));
        data.put("hasDate", hasDate(fields));
        data.put("hasDecimal", hasDecimal(fields));

        Template template = freemarkerConfig.getTemplate("entity.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Controller
     */
    public String generateController(String tableCode, String packageName) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("entityName", convertToEntityName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("controller.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Service
     */
    public String generateService(String tableCode, String packageName) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("entityName", convertToEntityName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("service.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Mapper接口
     */
    public String generateMapper(String tableCode, String packageName) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("entityName", convertToEntityName(table.getTableCode()));
        data.put("tableName", convertToTableName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("mapper.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Mapper XML
     */
    public String generateMapperXml(String tableCode, String packageName) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("entityName", convertToEntityName(table.getTableCode()));
        data.put("tableName", convertToTableName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("mapper.xml.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Vue列表页面
     */
    public String generateVueList(String tableCode) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        // 过滤掉主键字段，列表页通常不显示主键
        fields = fields.stream()
                .filter(f -> !f.getFieldName().equalsIgnoreCase("id"))
                .collect(Collectors.toList());

        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("componentName", convertToComponentName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("vue_list.vue.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Vue表单页面
     */
    public String generateVueForm(String tableCode) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<Map<String, Object>> fieldList = prepareFieldList(fields);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("componentName", convertToComponentName(table.getTableCode()));

        Template template = freemarkerConfig.getTemplate("vue_form.vue.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成前端路由配置（routes.js）
     * 会优先查找功能节点中配置的 routePath、componentPath、isMenuVisible 与 nodeType，以生成更贴合的路由，否则使用默认路径约定
     */
    public String generateRoutes(String tableCode) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        String componentName = convertToComponentName(table.getTableCode());
        String componentDir = "generated/" + componentName.toLowerCase();

        // 查询与该表相关的功能节点（若数据库中配置了 routePath，则优先使用）
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("componentName", componentName);
        data.put("componentDir", componentDir);
        data.put("nodes", nodes);

        Template template = freemarkerConfig.getTemplate("routes.js.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成完整的代码包（ZIP格式的JSON字符串，包含所有文件）
     */
    public Map<String, String> generateAll(String tableCode, String packageName) throws Exception {
        Map<String, String> codeMap = new HashMap<>();

        // 生成SQL
        codeMap.put("create_table.sql", generateCreateTableSQL(tableCode));

        // 生成Java代码
        codeMap.put("Entity.java", generateEntity(tableCode, packageName));
        codeMap.put("Controller.java", generateController(tableCode, packageName));
        codeMap.put("Service.java", generateService(tableCode, packageName));
        codeMap.put("Mapper.java", generateMapper(tableCode, packageName));
        codeMap.put("Mapper.xml", generateMapperXml(tableCode, packageName));

        // 生成Vue代码
        codeMap.put("List.vue", generateVueList(tableCode));
        codeMap.put("Form.vue", generateVueForm(tableCode));
        // 生成路由配置（供客户集成到前端）
        try {
            codeMap.put("routes.js", generateRoutes(tableCode));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("routes.js", "// 生成路由失败: " + e.getMessage());
        }

        return codeMap;
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
     * 工具方法：转换为组件名
     */
    private String convertToComponentName(String code) {
        return convertToCamelCase(code, true);
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    private String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        return code.toLowerCase().replace("_TABLE", "");
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

    /**
     * 获取Java类型
     */
    public String getJavaType(String fieldType) {
        if (fieldType == null) {
            return "String";
        }
        String type = fieldType.toLowerCase();
        if (type.contains("int") && !type.contains("bigint")) {
            return "Integer";
        } else if (type.contains("bigint")) {
            return "Long";
        } else if (type.contains("decimal") || type.contains("numeric") || type.contains("float") || type.contains("double")) {
            return "BigDecimal";
        } else if (type.contains("date") || type.contains("time")) {
            return "LocalDateTime";
        } else if (type.contains("boolean") || (type.contains("tinyint") && type.contains("1"))) {
            return "Boolean";
        } else {
            return "String";
        }
    }

    /**
     * 检查字段列表中是否有日期类型
     */
    public boolean hasDate(List<MetadataField> fields) {
        return fields.stream().anyMatch(f -> 
            f.getFieldType() != null && 
            (f.getFieldType().toLowerCase().contains("date") || f.getFieldType().toLowerCase().contains("time"))
        );
    }

    /**
     * 检查字段列表中是否有Decimal类型
     */
    public boolean hasDecimal(List<MetadataField> fields) {
        return fields.stream().anyMatch(f -> 
            f.getFieldType() != null && 
            (f.getFieldType().toLowerCase().contains("decimal") || 
             f.getFieldType().toLowerCase().contains("numeric") ||
             f.getFieldType().toLowerCase().contains("float") ||
             f.getFieldType().toLowerCase().contains("double"))
        );
    }
}

