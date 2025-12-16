package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.*;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.*;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成服务实现
 */
@Service
public class CodeGeneratorServiceImpl implements CodeGeneratorService {

    @Autowired
    private MetadataFieldService fieldService;

    @Autowired
    private MetadataTableService tableService;

    @Autowired
    private MetadataFunctionNodeMapper nodeMapper;
    
    @Autowired
    private MetadataBusinessSystemService businessSystemService;
    
    @Autowired
    private MetadataBusinessRuleService businessRuleService;

    private Configuration freemarkerConfig;

    public CodeGeneratorServiceImpl() {
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
    @Override
    public String generateCreateTableSQL(String tableCode, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
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
        
        // 生成CHECK约束列表
        List<String> checkConstraints = new ArrayList<>();
        String tableName = convertToTableName(table.getTableCode());
        for (MetadataField field : fields) {
            String checkConstraint = generateCheckConstraint(field);
            if (checkConstraint != null && !checkConstraint.isEmpty()) {
                String constraintName = "ck_" + tableName + "_" + field.getFieldName();
                checkConstraints.add(constraintName + " " + checkConstraint);
            }
        }
        
        // 生成UNIQUE约束列表
        List<String> uniqueConstraints = new ArrayList<>();
        try {
            // 获取表关联的模块编码列表（通过功能节点关联）
            List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode, businessCode);
            Set<String> moduleCodes = nodes.stream()
                .map(MetadataFunctionNode::getModuleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // 遍历每个关联的模块，获取业务规则
            for (String moduleCode : moduleCodes) {
                // 获取模块的所有业务规则
                List<MetadataBusinessRule> rules = businessRuleService.listByModuleCode(moduleCode, businessCode);
                
                // 筛选出VALIDATION_RULE类型的规则
                for (MetadataBusinessRule rule : rules) {
                    if ("VALIDATION_RULE".equals(rule.getRuleType())) {
                        // 解析规则内容
                        JSONObject ruleContent = JSONObject.parseObject(rule.getRuleContent());
                        if (ruleContent != null) {
                            String ruleType = ruleContent.getString("type");
                            if ("unique".equals(ruleType) || "unique_combo".equals(ruleType)) {
                                // 提取字段列表
                                List<String> uniqueFields = new ArrayList<>();
                                if ("unique".equals(ruleType)) {
                                    // 单字段唯一
                                    String field = ruleContent.getString("field");
                                    if (field != null && !field.isEmpty()) {
                                        uniqueFields.add(field);
                                    }
                                } else if ("unique_combo".equals(ruleType)) {
                                    // 组合字段唯一
                                    Object fieldsObj = ruleContent.get("fields");
                                    if (fieldsObj instanceof JSONArray) {
                                        JSONArray fieldsArray = (JSONArray) fieldsObj;
                                        for (Object fieldObj : fieldsArray) {
                                            if (fieldObj instanceof String) {
                                                uniqueFields.add((String) fieldObj);
                                            }
                                        }
                                    }
                                }
                                
                                // 生成UNIQUE约束
                                if (!uniqueFields.isEmpty()) {
                                    // 检查字段是否都存在于当前表中
                                    Set<String> tableFieldNames = fields.stream()
                                        .map(MetadataField::getFieldName)
                                        .collect(Collectors.toSet());
                                    
                                    boolean allFieldsExist = true;
                                    for (String uniqueField : uniqueFields) {
                                        if (!tableFieldNames.contains(uniqueField)) {
                                            allFieldsExist = false;
                                            break;
                                        }
                                    }
                                    
                                    if (allFieldsExist) {
                                        // 生成约束名称
                                        String constraintName = "uk_" + tableName + "_" + String.join("_", uniqueFields);
                                        // 生成约束SQL
                                        String constraintSql = "UNIQUE KEY `" + constraintName + "` (`" + String.join("`, `", uniqueFields) + "`)";
                                        uniqueConstraints.add(constraintSql);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取业务规则失败，不影响建表SQL生成
            e.printStackTrace();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("tableName", tableName);
        data.put("checkConstraints", checkConstraints);
        data.put("uniqueConstraints", uniqueConstraints);
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("create_table.sql.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }
    
    /**
     * 获取业务系统名称
     */
    private String getBusinessName(String businessCode) {
        if (businessCode == null || businessCode.trim().isEmpty()) {
            return "默认系统";
        }
        try {
            MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
            return businessSystem != null ? businessSystem.getBusinessName() : businessCode;
        } catch (Exception e) {
            return businessCode;
        }
    }
    
    /**
     * 获取表相关的业务规则
     */
    private List<Map<String, Object>> getTableBusinessRules(String tableCode, String businessCode) {
        List<Map<String, Object>> tableRules = new ArrayList<>();
        try {
            // 获取表关联的模块编码列表（通过功能节点关联）
            List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode, businessCode);
            Set<String> moduleCodes = nodes.stream()
                .map(MetadataFunctionNode::getModuleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            // 遍历每个关联的模块，获取业务规则
            for (String moduleCode : moduleCodes) {
                // 获取模块的所有业务规则
                List<MetadataBusinessRule> rules = businessRuleService.listByModuleCode(moduleCode, businessCode);
                
                // 筛选出VALIDATION_RULE类型的规则
                for (MetadataBusinessRule rule : rules) {
                    if ("VALIDATION_RULE".equals(rule.getRuleType())) {
                        // 解析规则内容
                        JSONObject ruleContent = JSONObject.parseObject(rule.getRuleContent());
                        if (ruleContent != null) {
                            String ruleType = ruleContent.getString("type");
                            if ("unique".equals(ruleType) || "unique_combo".equals(ruleType)) {
                                // 提取字段列表
                                List<String> ruleFields = new ArrayList<>();
                                if ("unique".equals(ruleType)) {
                                    // 单字段唯一
                                    String field = ruleContent.getString("field");
                                    if (field != null && !field.isEmpty()) {
                                        ruleFields.add(field);
                                    }
                                } else if ("unique_combo".equals(ruleType)) {
                                    // 组合字段唯一
                                    Object fieldsObj = ruleContent.get("fields");
                                    if (fieldsObj instanceof JSONArray) {
                                        JSONArray fieldsArray = (JSONArray) fieldsObj;
                                        for (Object fieldObj : fieldsArray) {
                                            if (fieldObj instanceof String) {
                                                ruleFields.add((String) fieldObj);
                                            }
                                        }
                                    }
                                }
                                
                                // 检查字段是否都存在于当前表中
                                List<MetadataField> tableFields = fieldService.listByTableCode(tableCode);
                                Set<String> tableFieldNames = tableFields.stream()
                                    .map(MetadataField::getFieldName)
                                    .collect(Collectors.toSet());
                                
                                boolean allFieldsExist = true;
                                for (String ruleField : ruleFields) {
                                    if (!tableFieldNames.contains(ruleField)) {
                                        allFieldsExist = false;
                                        break;
                                    }
                                }
                                
                                if (allFieldsExist && !ruleFields.isEmpty()) {
                                    // 将原始字段名转换为包含camelCaseName属性的field对象
                                    List<Map<String, Object>> fieldObjects = new ArrayList<>();
                                    for (String ruleField : ruleFields) {
                                        for (MetadataField tableField : tableFields) {
                                            if (tableField.getFieldName().equals(ruleField)) {
                                                Map<String, Object> fieldMap = new HashMap<>();
                                                fieldMap.put("fieldName", ruleField);
                                                fieldMap.put("camelCaseName", convertToCamelCase(ruleField, false));
                                                fieldObjects.add(fieldMap);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    Map<String, Object> ruleMap = new HashMap<>();
                                    ruleMap.put("ruleCode", rule.getRuleCode());
                                    ruleMap.put("ruleType", ruleType);
                                    ruleMap.put("fields", fieldObjects);
                                    ruleMap.put("message", ruleContent.getString("message"));
                                    ruleMap.put("description", rule.getDescription());
                                    tableRules.add(ruleMap);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取业务规则失败，不影响代码生成
            e.printStackTrace();
        }
        return tableRules;
    }

    /**
     * 准备字段列表，添加转换后的属性
     */
    private List<Map<String, Object>> prepareFieldList(List<MetadataField> fields) {
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (MetadataField field : fields) {
            // 只处理启用的字段
            if (field.getIsEnabled() != null && field.getIsEnabled() == 0) {
                continue;
            }
            
            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("field", field);
            fieldMap.put("fieldName", field.getFieldName());
            // 格式化字段类型，确保生成有效的MySQL数据类型
            String formattedFieldType = formatFieldType(field.getFieldType());
            fieldMap.put("fieldType", formattedFieldType);
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
     * 格式化字段类型，确保生成有效的MySQL数据类型
     */
    private String formatFieldType(String fieldType) {
        if (fieldType == null || fieldType.trim().isEmpty()) {
            return "varchar(255)";
        }
        
        String type = fieldType.trim().toLowerCase();
        
        // 处理常见的无效字段类型
        if (type.equals("var")) {
            return "varchar(255)";
        } else if (type.equals("varchar")) {
            return "varchar(255)";
        } else if (type.equals("char")) {
            return "char(1)";
        } else if (type.equals("int")) {
            return "int(11)";
        } else if (type.equals("bigint")) {
            return "bigint(20)";
        } else if (type.equals("tinyint")) {
            return "tinyint(4)";
        } else if (type.equals("smallint")) {
            return "smallint(6)";
        } else if (type.equals("mediumint")) {
            return "mediumint(9)";
        } else if (type.equals("float")) {
            return "float(10,2)";
        } else if (type.equals("double")) {
            return "double(16,2)";
        } else if (type.equals("decimal")) {
            return "decimal(18,2)";
        } else if (type.equals("date")) {
            return "date";
        } else if (type.equals("time")) {
            return "time";
        } else if (type.equals("datetime")) {
            return "datetime";
        } else if (type.equals("timestamp")) {
            return "timestamp";
        } else if (type.equals("text")) {
            return "text";
        } else if (type.equals("longtext")) {
            return "longtext";
        } else if (type.equals("mediumtext")) {
            return "mediumtext";
        } else if (type.equals("tinytext")) {
            return "tinytext";
        } else if (type.equals("blob")) {
            return "blob";
        } else if (type.equals("longblob")) {
            return "longblob";
        } else if (type.equals("mediumblob")) {
            return "mediumblob";
        } else if (type.equals("tinyblob")) {
            return "tinyblob";
        } else if (type.equals("enum")) {
            return "enum('')";
        } else if (type.equals("set")) {
            return "set('')";
        } else if (type.equals("boolean")) {
            return "tinyint(1)";
        }
        
        // 如果是已经包含括号的类型，直接返回
        if (type.contains("(")) {
            return fieldType;
        }
        
        // 默认返回varchar(255)
        return "varchar(255)";
    }

    /**
     * 内置正则表达式映射表，根据type值提供相应的正则表达式
     */
    private static final Map<String, String> BUILT_IN_REGEX_MAP = new HashMap<String, String>() {
        {
            put("email", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
            put("url", "^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*\\/?$");
            put("number", "^-?\\d+(\\.\\d+)?$");
            put("integer", "^-?\\d+$");
        }
    };
    
    /**
     * 解析校验规则JSON，提取正则表达式等信息
     */
    private Map<String, Object> parseValidationRule(String validateRule) {
        Map<String, Object> rules = new HashMap<>();
        // 初始化默认值，避免模板访问时出错
        rules.put("hasPattern", false);
        rules.put("hasOptions", false);
        rules.put("hasLength", false);
        rules.put("hasRange", false);
        rules.put("hasOperator", false);
        
        if (validateRule == null || validateRule.trim().isEmpty()) {
            return rules;
        }
        
        try {
            JSONObject jsonObject = JSON.parseObject(validateRule);
            
            // 提取正则表达式或type属性对应的内置正则表达式
            String pattern = null;
            if (jsonObject.containsKey("pattern")) {
                pattern = jsonObject.getString("pattern");
            } else if (jsonObject.containsKey("type")) {
                String type = jsonObject.getString("type");
                if (BUILT_IN_REGEX_MAP.containsKey(type)) {
                    pattern = BUILT_IN_REGEX_MAP.get(type);
                }
            }
            
            if (pattern != null) {
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
            
            // 提取长度限制
            boolean hasLength = false;
            if (jsonObject.containsKey("minLength") || jsonObject.containsKey("maxLength")) {
                if (jsonObject.containsKey("minLength")) {
                    rules.put("minLength", convertToNumber(jsonObject.get("minLength")));
                }
                if (jsonObject.containsKey("maxLength")) {
                    rules.put("maxLength", convertToNumber(jsonObject.get("maxLength")));
                }
                hasLength = true;
                String lengthMessage = jsonObject.getString("lengthMessage");
                rules.put("lengthMessage", lengthMessage != null ? lengthMessage : "长度必须在${minLength}到${maxLength}之间");
            }
            rules.put("hasLength", hasLength);
            
            // 提取数值范围
            boolean hasRange = false;
            if (jsonObject.containsKey("min") || jsonObject.containsKey("max")) {
                if (jsonObject.containsKey("min")) {
                    rules.put("min", convertToNumber(jsonObject.get("min")));
                }
                if (jsonObject.containsKey("max")) {
                    rules.put("max", convertToNumber(jsonObject.get("max")));
                }
                hasRange = true;
                String rangeMessage = jsonObject.getString("rangeMessage");
                rules.put("rangeMessage", rangeMessage != null ? rangeMessage : "数值必须在${min}到${max}之间");
            }
            rules.put("hasRange", hasRange);
            
            // 提取操作符（IN、BETWEEN等）
            if (jsonObject.containsKey("operator")) {
                String operator = jsonObject.getString("operator");
                rules.put("hasOperator", true);
                rules.put("operator", operator);
                
                String operatorMessage = jsonObject.getString("operatorMessage");
                rules.put("operatorMessage", operatorMessage != null ? operatorMessage : "值必须在指定范围内");
                
                // 提取IN操作符的values
                if ("IN".equalsIgnoreCase(operator) && jsonObject.containsKey("values")) {
                    Object values = jsonObject.get("values");
                    rules.put("values", values);
                }
                
                // 提取BETWEEN操作符的min和max
                if ("BETWEEN".equalsIgnoreCase(operator)) {
                    if (jsonObject.containsKey("min")) {
                        rules.put("min", convertToNumber(jsonObject.get("min")));
                    }
                    if (jsonObject.containsKey("max")) {
                        rules.put("max", convertToNumber(jsonObject.get("max")));
                    }
                }
            }
            
        } catch (Exception e) {
            // JSON解析失败，忽略校验规则
            rules.put("hasPattern", false);
            rules.put("hasOptions", false);
            rules.put("hasLength", false);
            rules.put("hasRange", false);
            rules.put("hasOperator", false);
        }
        
        return rules;
    }
    
    /**
     * 将对象转换为Number类型，如果是字符串则尝试解析为数值
     */
    private Number convertToNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof String) {
            String strValue = (String) value;
            try {
                if (strValue.contains(".")) {
                    return Double.parseDouble(strValue);
                } else {
                    return Long.parseLong(strValue);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 生成Java实体类
     */
    @Override
    public String generateEntity(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
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
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("entity.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Controller
     */
    @Override
    public String generateController(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
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
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("controller.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Service
     */
    @Override
    public String generateService(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<Map<String, Object>> fieldList = prepareFieldList(fields);
        // 获取表相关的业务规则
        List<Map<String, Object>> businessRules = getTableBusinessRules(tableCode, businessCode);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("entityName", convertToEntityName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));
        data.put("businessRules", businessRules);

        Template template = freemarkerConfig.getTemplate("service.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Mapper接口
     */
    @Override
    public String generateMapper(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
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
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("mapper.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Mapper XML
     */
    @Override
    public String generateMapperXml(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
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
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("mapper.xml.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Vue列表页面
     */
    @Override
    public String generateVueList(String tableCode, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
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
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("vue_list.vue.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成Vue表单页面
     */
    @Override
    public String generateVueForm(String tableCode, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<Map<String, Object>> fieldList = prepareFieldList(fields);
        // 获取表相关的业务规则
        List<Map<String, Object>> businessRules = getTableBusinessRules(tableCode, businessCode);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("componentName", convertToComponentName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));
        data.put("businessRules", businessRules);

        Template template = freemarkerConfig.getTemplate("vue_form.vue.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成前端路由配置（routes.js）
     */
    @Override
    public String generateRoutes(String tableCode, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        List<Map<String, Object>> routeList = generateTableRoutes(tableCode, businessCode);
        
        Map<String, Object> data = new HashMap<>();
        data.put("routes", routeList);
        data.put("businessCode", businessCode);
        
        Template template = freemarkerConfig.getTemplate("routes.js.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }
    
    /**
     * 生成单表的路由配置列表
     */
    private List<Map<String, Object>> generateTableRoutes(String tableCode, String businessCode) throws Exception {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        String componentName = convertToComponentName(table.getTableCode());
        String componentDir = "generated/" + componentName.toLowerCase();

        // 查询与该表相关的功能节点（若数据库中配置了 routePath，则优先使用）
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        
        // 过滤掉禁用的功能节点
        nodes = nodes.stream()
                .filter(node -> node.getIsEnabled() == null || node.getIsEnabled() == 1)
                .collect(Collectors.toList());
        
        List<Map<String, Object>> routeList = new ArrayList<>();
        
        if (!nodes.isEmpty()) {
            // 有配置的功能节点，生成对应的路由
            for (MetadataFunctionNode node : nodes) {
                // 对已配置路由信息或需要默认生成路由的节点生成路由
                if (node.getRoutePath() != null && !node.getRoutePath().isEmpty() || 
                    node.getJumpRelation() != null && !node.getJumpRelation().isEmpty() || 
                    (node.getNodeType() != null && (node.getNodeType().toUpperCase().contains("LIST") || 
                                                   node.getNodeType().toUpperCase().contains("FORM") || 
                                                   node.getNodeType().toUpperCase().contains("DETAIL")))) {
                    
                    Map<String, Object> route = new HashMap<>();
                    
                    // 生成路由路径
                    String pathValue = "";
                    if (node.getRoutePath() != null && !node.getRoutePath().isEmpty()) {
                        // 使用配置的 routePath
                        pathValue = node.getRoutePath();
                    } else if (node.getJumpRelation() != null && !node.getJumpRelation().isEmpty()) {
                        // 使用配置的 jumpRelation 作为路径
                        pathValue = node.getJumpRelation();
                    } else {
                        // 没有配置 routePath 和 jumpRelation，根据节点类型生成默认路径
                        // 使用组件目录作为基础路径
                        String basePath = componentName.toLowerCase();
                        if (node.getNodeType().toUpperCase().contains("LIST")) {
                            pathValue = "/" + businessCode + "/" + basePath + "/list";
                        } else if (node.getNodeType().toUpperCase().contains("FORM")) {
                            pathValue = "/" + businessCode + "/" + basePath + "/form/:id?";
                        } else if (node.getNodeType().toUpperCase().contains("DETAIL")) {
                            pathValue = "/" + businessCode + "/" + basePath + "/detail/:id?";
                        } else {
                            // 其他类型不生成默认路径
                            continue;
                        }
                    }
                    route.put("path", pathValue);
                    
                    // 生成路由名称
                    String nameValue = node.getNodeCode() != null && !node.getNodeCode().isEmpty() ? 
                                      node.getNodeCode() : componentName + "List";
                    route.put("name", nameValue);
                    
                    // 生成组件路径
                    String componentPath = "";
                    if (node.getComponentPath() != null && !node.getComponentPath().isEmpty()) {
                        // 如果组件路径以 views/ 开头，直接使用，否则使用默认路径
                        if (node.getComponentPath().startsWith("views/")) {
                            componentPath = "@/" + node.getComponentPath();
                        } else {
                            componentPath = "@/views/" + node.getComponentPath();
                        }
                    } else if (node.getNodeType().toUpperCase().contains("LIST")) {
                        // 没有配置 componentPath，使用默认路径
                        componentPath = "@/views/" + componentDir + "/List.vue";
                    } else if (node.getNodeType().toUpperCase().contains("FORM")) {
                        componentPath = "@/views/" + componentDir + "/Form.vue";
                    } else if (node.getNodeType().toUpperCase().contains("DETAIL")) {
                        componentPath = "@/views/" + componentDir + "/Form.vue";
                    } else {
                        componentPath = "@/views/" + componentDir + "/Form.vue";
                    }
                    route.put("component", "() => import('" + componentPath + "')");
                    
                    // 生成路由元信息
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("moduleCode", node.getModuleCode() != null ? node.getModuleCode() : "");
                    meta.put("nodeCode", node.getNodeCode() != null ? node.getNodeCode() : "");
                    meta.put("relatedTableCode", node.getRelatedTableCode() != null ? node.getRelatedTableCode() : tableCode);
                    meta.put("isMenuVisible", node.getIsMenuVisible() != null ? node.getIsMenuVisible() : 1);
                    meta.put("icon", node.getIcon() != null ? node.getIcon() : "");
                    meta.put("businessCode", businessCode);
                    route.put("meta", meta);
                    
                    routeList.add(route);
                }
            }
        } else {
            // 没有配置功能节点，生成默认的列表页和表单页路由
            // 生成列表页路由
            Map<String, Object> listRoute = new HashMap<>();
            listRoute.put("path", "/" + businessCode + "/" + componentName.toLowerCase() + "/list");
            listRoute.put("name", componentName + "List");
            listRoute.put("component", "() => import('@/views/" + componentDir + "/List.vue')");
            
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("relatedTableCode", tableCode);
            listMeta.put("isMenuVisible", 1);
            listMeta.put("businessCode", businessCode);
            listRoute.put("meta", listMeta);
            
            routeList.add(listRoute);
            
            // 生成表单页路由
            Map<String, Object> formRoute = new HashMap<>();
            formRoute.put("path", "/" + businessCode + "/" + componentName.toLowerCase() + "/form/:id?");
            formRoute.put("name", componentName + "Form");
            formRoute.put("component", "() => import('@/views/" + componentDir + "/Form.vue')");
            
            Map<String, Object> formMeta = new HashMap<>();
            formMeta.put("relatedTableCode", tableCode);
            formMeta.put("isMenuVisible", 0);
            formMeta.put("businessCode", businessCode);
            formRoute.put("meta", formMeta);
            
            routeList.add(formRoute);
        }
        
        return routeList;
    }
    
    /**
     * 生成业务系统下所有表的整合路由配置（routes.js）
     */
    @Override
    public String generateIntegratedRoutes(String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 获取业务系统下所有启用的表
        List<MetadataTable> tables = tableService.list(null, businessCode);
        List<MetadataTable> enabledTables = tables.stream()
                .filter(table -> table.getIsEnabled() != null && table.getIsEnabled() == 1)
                .collect(Collectors.toList());
        
        if (enabledTables.isEmpty()) {
            throw new RuntimeException("业务系统下没有启用的表: " + businessCode);
        }
        
        // 合并所有表的路由配置
        List<Map<String, Object>> allRoutes = new ArrayList<>();
        for (MetadataTable table : enabledTables) {
            List<Map<String, Object>> tableRoutes = generateTableRoutes(table.getTableCode(), businessCode);
            allRoutes.addAll(tableRoutes);
        }
        
        // 生成完整的routes.js文件
        Map<String, Object> data = new HashMap<>();
        data.put("routes", allRoutes);
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));
        
        Template template = freemarkerConfig.getTemplate("routes.integrated.js.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成完整的代码包（包含所有文件）
     */
    @Override
    public Map<String, String> generateAll(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        Map<String, String> codeMap = new HashMap<>();

        // 生成SQL
        codeMap.put("create_table.sql", generateCreateTableSQL(tableCode, businessCode));

        // 生成Java代码
        codeMap.put("Entity.java", generateEntity(tableCode, packageName, businessCode));
        codeMap.put("Controller.java", generateController(tableCode, packageName, businessCode));
        codeMap.put("Service.java", generateService(tableCode, packageName, businessCode));
        codeMap.put("Mapper.java", generateMapper(tableCode, packageName, businessCode));
        codeMap.put("Mapper.xml", generateMapperXml(tableCode, packageName, businessCode));

        // 生成通用类
        String commonPackage = packageName + ".common";
        codeMap.put("Result.java", generateResult(commonPackage));
        codeMap.put("PageRequest.java", generatePageRequest(commonPackage));
        codeMap.put("PageResult.java", generatePageResult(commonPackage));

        // 生成Vue代码
        codeMap.put("List.vue", generateVueList(tableCode, businessCode));
        codeMap.put("Form.vue", generateVueForm(tableCode, businessCode));
        // 生成路由配置（供客户集成到前端）
        try {
            codeMap.put("routes.js", generateRoutes(tableCode, businessCode));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("routes.js", "// 生成路由失败: " + e.getMessage());
        }

        // 生成Spring Boot启动类
        codeMap.put("Application.java", generateApplication(packageName));
        // 生成application.yml配置文件
        codeMap.put("application.yml", generateApplicationConfig(packageName));
        // 生成MyBatis配置类
        codeMap.put("MyBatisConfig.java", generateMyBatisConfig(packageName));
        // 生成pom.xml配置文件
        String groupId = packageName;
        String artifactId = packageName.substring(packageName.lastIndexOf(".") + 1);
        String name = artifactId.substring(0, 1).toUpperCase() + artifactId.substring(1);
        String description = name;
        codeMap.put("pom.xml", generatePomXml(groupId, artifactId, name, description));

        return codeMap;
    }
    
    /**
     * 生成业务系统下所有表的完整代码包
     */
    @Override
    public Map<String, Map<String, String>> generateAllByBusinessSystem(String businessCode, String packageName) throws Exception {
        Map<String, Map<String, String>> allCodeMap = new HashMap<>();
        
        // 获取业务系统下所有表
        List<MetadataTable> tables = tableService.list(null, businessCode);
        
        // 为每个表生成代码
        for (MetadataTable table : tables) {
            if (table.getIsEnabled() == 1) { // 只处理启用的表
                Map<String, String> codeMap = generateAll(table.getTableCode(), packageName, businessCode);
                allCodeMap.put(table.getTableCode(), codeMap);
            }
        }
        
        return allCodeMap;
    }
    
    /**
     * 生成业务系统下所有表的建表SQL
     */
    @Override
    public Map<String, String> generateAllSQLByBusinessSystem(String businessCode) throws Exception {
        Map<String, String> sqlMap = new HashMap<>();
        
        // 获取业务系统下所有表
        List<MetadataTable> tables = tableService.list(null, businessCode);
        
        // 为每个表生成SQL
        for (MetadataTable table : tables) {
            if (table.getIsEnabled() == 1) { // 只处理启用的表
                String sql = generateCreateTableSQL(table.getTableCode(), businessCode);
                sqlMap.put(table.getTableCode() + ".sql", sql);
            }
        }
        
        return sqlMap;
    }

    /**
     * 工具方法：转换为类名（大驼峰）
     */
    private String convertToClassName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的类名
        String processedCode = code.replace("_TABLE", "");
        return convertToCamelCase(processedCode, true);
    }

    /**
     * 工具方法：转换为实体名（小驼峰）
     */
    private String convertToEntityName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的实体名
        String processedCode = code.replace("_TABLE", "");
        return convertToCamelCase(processedCode, false);
    }

    /**
     * 工具方法：转换为组件名
     */
    private String convertToComponentName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的组件名
        String processedCode = code.replace("_TABLE", "");
        return convertToCamelCase(processedCode, true);
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    @Override
    public String convertToTableName(String code) {
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
    @Override
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
    @Override
    public boolean hasDate(List<MetadataField> fields) {
        return fields.stream().anyMatch(f -> 
            f.getFieldType() != null && 
            (f.getFieldType().toLowerCase().contains("date") || f.getFieldType().toLowerCase().contains("time"))
        );
    }

    /**
     * 检查字段列表中是否有Decimal类型
     */
    @Override
    public boolean hasDecimal(List<MetadataField> fields) {
        return fields.stream().anyMatch(f -> 
            f.getFieldType() != null && 
            (f.getFieldType().toLowerCase().contains("decimal") || 
             f.getFieldType().toLowerCase().contains("numeric") ||
             f.getFieldType().toLowerCase().contains("float") ||
             f.getFieldType().toLowerCase().contains("double"))
        );
    }
    
    /**
     * 生成CHECK约束
     */
    @Override
    public String generateCheckConstraint(MetadataField field) {
        Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
        List<String> conditions = new ArrayList<>();
        
        // 处理正则表达式
        if (validationRules.containsKey("hasPattern") && (Boolean) validationRules.get("hasPattern")) {
            String pattern = (String) validationRules.get("pattern");
            conditions.add("`" + field.getFieldName() + "` REGEXP '" + pattern + "'");
        }
        
        // 处理数值范围
        if (validationRules.containsKey("hasRange") && (Boolean) validationRules.get("hasRange")) {
            Number min = convertToNumber(validationRules.get("min"));
            Number max = convertToNumber(validationRules.get("max"));
            
            if (min != null && max != null) {
                conditions.add("`" + field.getFieldName() + "` BETWEEN " + min + " AND " + max);
            } else if (min != null) {
                conditions.add("`" + field.getFieldName() + "` >= " + min);
            } else if (max != null) {
                conditions.add("`" + field.getFieldName() + "` <= " + max);
            }
        }
        
        // 处理枚举值
        if (validationRules.containsKey("hasOptions") && (Boolean) validationRules.get("hasOptions")) {
            Object options = validationRules.get("options");
            if (options instanceof JSONArray) {
                JSONArray optionsArray = (JSONArray) options;
                if (!optionsArray.isEmpty()) {
                    StringBuilder inClause = new StringBuilder();
                    inClause.append("`").append(field.getFieldName()).append("` IN (");
                    for (int i = 0; i < optionsArray.size(); i++) {
                        if (i > 0) {
                            inClause.append(", ");
                        }
                        Object option = optionsArray.get(i);
                        if (option instanceof String) {
                            inClause.append("'").append(option).append("'");
                        } else {
                            inClause.append(option);
                        }
                    }
                    inClause.append(")");
                    conditions.add(inClause.toString());
                }
            }
        }
        
        // 处理操作符（IN、BETWEEN等）
        if (validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
            String operator = (String) validationRules.get("operator");
            
            // 处理IN操作符
            if ("IN".equalsIgnoreCase(operator)) {
                Object values = validationRules.get("values");
                if (values instanceof JSONArray) {
                    JSONArray valuesArray = (JSONArray) values;
                    if (!valuesArray.isEmpty()) {
                        StringBuilder inClause = new StringBuilder();
                        inClause.append("`").append(field.getFieldName()).append("` IN (");
                        for (int i = 0; i < valuesArray.size(); i++) {
                            if (i > 0) {
                                inClause.append(", ");
                            }
                            Object value = valuesArray.get(i);
                            if (value instanceof String) {
                                inClause.append("'").append(value).append("'");
                            } else {
                                inClause.append(value);
                            }
                        }
                        inClause.append(")");
                        conditions.add(inClause.toString());
                    }
                }
            }
            
            // 处理BETWEEN操作符
            else if ("BETWEEN".equalsIgnoreCase(operator)) {
                Number min = convertToNumber(validationRules.get("min"));
                Number max = convertToNumber(validationRules.get("max"));
                if (min != null && max != null) {
                    conditions.add("`" + field.getFieldName() + "` BETWEEN " + min + " AND " + max);
                }
            }
        }
        
        // 如果有条件，生成单个CHECK约束
        if (!conditions.isEmpty()) {
            return "CHECK (" + String.join(" AND ", conditions) + ")";
        }
        
        return null;
    }

    /**
     * 生成添加字段的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableAddColumnSQL(String tableCode, MetadataField field) throws Exception {
        String tableName = convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" ADD COLUMN `").append(field.getFieldName()).append("` ");
        
        // 处理字段类型和长度
        String fieldType = field.getFieldType();
        Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
        
        // 转换长度限制
        if (validationRules.containsKey("hasLength") && (Boolean) validationRules.get("hasLength")) {
            Number maxLengthNum = convertToNumber(validationRules.get("maxLength"));
            Integer maxLength = maxLengthNum != null ? maxLengthNum.intValue() : null;
            if (maxLength != null && fieldType.toLowerCase().contains("varchar")) {
                fieldType = "VARCHAR(" + maxLength + ")";
            }
        }
        
        sql.append(fieldType);
        
        // 添加NOT NULL约束
        if (field.getIsRequired() != null && field.getIsRequired() == 1) {
            sql.append(" NOT NULL");
        } else {
            sql.append(" NULL");
        }
        
        // 添加注释
        if (field.getLabel() != null && !field.getLabel().trim().isEmpty()) {
            sql.append(" COMMENT '").append(field.getLabel().replace("'", "''")).append("'");
        }
        
        // 添加CHECK约束
        String checkConstraint = generateCheckConstraint(field);
        if (checkConstraint != null && !checkConstraint.isEmpty()) {
            sql.append(", ADD CONSTRAINT ")
               .append("ck_").append(tableName).append("_")
               .append(field.getFieldName()).append(" ")
               .append(checkConstraint);
        }
        
        return sql.toString();
    }

    /**
     * 生成修改字段的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableModifyColumnSQL(String tableCode, MetadataField field) throws Exception {
        String tableName = convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" MODIFY COLUMN `").append(field.getFieldName()).append("` ");
        
        // 处理字段类型和长度
        String fieldType = field.getFieldType();
        Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
        
        // 转换长度限制
        if (validationRules.containsKey("hasLength") && (Boolean) validationRules.get("hasLength")) {
            Number maxLengthNum = convertToNumber(validationRules.get("maxLength"));
            Integer maxLength = maxLengthNum != null ? maxLengthNum.intValue() : null;
            if (maxLength != null && fieldType.toLowerCase().contains("varchar")) {
                fieldType = "VARCHAR(" + maxLength + ")";
            }
        }
        
        sql.append(fieldType);
        
        // 添加NOT NULL约束
        if (field.getIsRequired() != null && field.getIsRequired() == 1) {
            sql.append(" NOT NULL");
        } else {
            sql.append(" NULL");
        }
        
        // 添加注释
        if (field.getLabel() != null && !field.getLabel().trim().isEmpty()) {
            sql.append(" COMMENT '").append(field.getLabel().replace("'", "''")).append("'");
        }
        
        // 注意：修改字段时不再添加CHECK约束，因为这会导致重复约束名错误
        // 如果需要修改CHECK约束，请手动删除后重新添加
        
        return sql.toString();
    }
    
    /**
     * 生成修改字段名称和属性的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableChangeColumnSQL(String tableCode, String oldFieldName, MetadataField field) throws Exception {
        String tableName = convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" CHANGE COLUMN `").append(oldFieldName).append("` `").append(field.getFieldName()).append("` ");
        
        // 处理字段类型和长度
        String fieldType = field.getFieldType();
        Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
        
        // 转换长度限制
        if (validationRules.containsKey("hasLength") && (Boolean) validationRules.get("hasLength")) {
            Number maxLengthNum = convertToNumber(validationRules.get("maxLength"));
            Integer maxLength = maxLengthNum != null ? maxLengthNum.intValue() : null;
            if (maxLength != null && fieldType.toLowerCase().contains("varchar")) {
                fieldType = "VARCHAR(" + maxLength + ")";
            }
        }
        
        sql.append(fieldType);
        
        // 添加NOT NULL约束
        if (field.getIsRequired() != null && field.getIsRequired() == 1) {
            sql.append(" NOT NULL");
        } else {
            sql.append(" NULL");
        }
        
        // 添加注释
        if (field.getLabel() != null && !field.getLabel().trim().isEmpty()) {
            sql.append(" COMMENT '").append(field.getLabel().replace("'", "''")).append("'");
        }
        
        // 注意：修改字段时不再添加CHECK约束，因为这会导致重复约束名错误
        // 如果需要修改CHECK约束，请手动删除后重新添加
        
        return sql.toString();
    }

    /**
     * 生成删除字段的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableDropColumnSQL(String tableCode, String fieldName) throws Exception {
        String tableName = convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" DROP COLUMN `").append(fieldName).append("`");
        
        return sql.toString();
    }

    /**
     * 生成Result统一响应结果类
     */
    @Override
    public String generateResult(String packageName) throws Exception {
        return "package " + packageName + ";\n\n" +
                "import lombok.Data;\n" +
                "import java.io.Serializable;\n\n" +
                "/**\n" +
                " * 统一响应结果类\n" +
                " */\n" +
                "@Data\n" +
                "public class Result<T> implements Serializable {\n" +
                "    private Integer code;\n\n" +
                "    /**\n" +
                "     * 响应消息\n" +
                "     */\n" +
                "    private String message;\n\n" +
                "    /**\n" +
                "     * 响应数据\n" +
                "     */\n" +
                "    private T data;\n\n" +
                "    public Result() {\n" +
                "    }\n\n" +
                "    public Result(Integer code, String message, T data) {\n" +
                "        this.code = code;\n" +
                "        this.message = message;\n" +
                "        this.data = data;\n" +
                "    }\n\n" +
                "    public static <T> Result<T> success() {\n" +
                "        return new Result<>(200, \"操作成功\", null);\n" +
                "    }\n\n" +
                "    public static <T> Result<T> success(T data) {\n" +
                "        return new Result<>(200, \"操作成功\", data);\n" +
                "    }\n\n" +
                "    public static <T> Result<T> error(String message) {\n" +
                "        return new Result<>(500, message, null);\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成PageRequest分页请求类
     */
    @Override
    public String generatePageRequest(String packageName) throws Exception {
        return "package " + packageName + ";\n\n" +
                "import java.util.HashMap;\n" +
                "import java.util.Map;\n\n" +
                "/**\n" +
                " * 分页请求类\n" +
                " */\n" +
                "public class PageRequest {\n" +
                "    private Integer current = 1;\n" +
                "    private Integer size = 10;\n" +
                "    private String orderBy;\n" +
                "    private String orderDirection;\n" +
                "    private Map<String, Object> conditions;\n\n" +
                "    /**\n" +
                "     * 无参构造函数，初始化条件映射\n" +
                "     */\n" +
                "    public PageRequest() {\n" +
                "        this.conditions = new HashMap<>();\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取当前页码\n" +
                "     */\n" +
                "    public Integer getCurrent() {\n" +
                "        return current;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置当前页码\n" +
                "     */\n" +
                "    public void setCurrent(Integer current) {\n" +
                "        this.current = current;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取每页大小\n" +
                "     */\n" +
                "    public Integer getSize() {\n" +
                "        return size;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置每页大小\n" +
                "     */\n" +
                "    public void setSize(Integer size) {\n" +
                "        this.size = size;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取排序字段\n" +
                "     */\n" +
                "    public String getOrderBy() {\n" +
                "        return orderBy;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置排序字段\n" +
                "     */\n" +
                "    public void setOrderBy(String orderBy) {\n" +
                "        this.orderBy = orderBy;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取排序方向\n" +
                "     */\n" +
                "    public String getOrderDirection() {\n" +
                "        return orderDirection;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置排序方向\n" +
                "     */\n" +
                "    public void setOrderDirection(String orderDirection) {\n" +
                "        this.orderDirection = orderDirection;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取条件映射\n" +
                "     */\n" +
                "    public Map<String, Object> getConditions() {\n" +
                "        return conditions;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置条件映射\n" +
                "     */\n" +
                "    public void setConditions(Map<String, Object> conditions) {\n" +
                "        this.conditions = conditions;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取偏移量\n" +
                "     */\n" +
                "    public Integer getOffset() {\n" +
                "        return (current - 1) * size;\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成PageResult分页结果类
     */
    @Override
    public String generatePageResult(String packageName) throws Exception {
        return "package " + packageName + ";\n\n" +
                "import lombok.Data;\n" +
                "import java.util.List;\n\n" +
                "/**\n" +
                " * 分页结果类\n" +
                " */\n" +
                "@Data\n" +
                "public class PageResult<T> {\n" +
                "    private Long total;\n" +
                "    private List<T> records;\n\n" +
                "    public PageResult() {\n" +
                "    }\n\n" +
                "    public PageResult(Long total, List<T> records) {\n" +
                "        this.total = total;\n" +
                "        this.records = records;\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成Spring Boot启动类
     */
    @Override
    public String generateApplication(String packageName) throws Exception {
        return "package " + packageName + ";\n\n" +
                "import org.mybatis.spring.annotation.MapperScan;\n" +
                "import org.springframework.boot.SpringApplication;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
                "/**\n" +
                " * Spring Boot应用启动类\n" +
                " */\n" +
                "@SpringBootApplication\n" +
                "@MapperScan(\"" + packageName + ".mapper\")\n" +
                "public class Application {\n" +
                "    public static void main(String[] args) {\n" +
                "        SpringApplication.run(Application.class, args);\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成application.yml配置文件
     */
    @Override
    public String generateApplicationConfig(String packageName) throws Exception {
        return "# Spring Boot 应用配置\n" +
                "spring:\n" +
                "  application:\n" +
                "    name: application\n" +
                "  datasource:\n" +
                "    # 数据库连接配置\n" +
                "    url: jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true\n" +
                "    username: root\n" +
                "    password: your_password\n" +
                "    driver-class-name: com.mysql.cj.jdbc.Driver\n" +
                "\n" +
                "# 服务器配置\n" +
                "server:\n" +
                "  port: 8080\n" +
                "\n" +
                "# 日志配置\n" +
                "logging:\n" +
                "  level:\n" +
                "    root: INFO\n" +
                "    " + packageName + ": DEBUG\n";}
    
    /**
     * 生成MyBatis配置类
     */
    @Override
    public String generateMyBatisConfig(String packageName) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        
        Template template = freemarkerConfig.getTemplate("mybatis-config.java.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }
    
    /**
     * 生成pom.xml配置文件
     */
    @Override
    public String generatePomXml(String groupId, String artifactId, String name, String description) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("artifactId", artifactId);
        data.put("name", name);
        data.put("description", description);
        
        Template template = freemarkerConfig.getTemplate("pom.xml.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }
}