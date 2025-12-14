package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.CodeGeneratorService;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.MetadataTableService;
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
                List<com.metadata.entity.MetadataBusinessRule> rules = businessRuleService.listByModuleCode(moduleCode, businessCode);
                
                // 筛选出VALIDATION_RULE类型的规则
                for (com.metadata.entity.MetadataBusinessRule rule : rules) {
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
                                    if (fieldsObj instanceof com.alibaba.fastjson2.JSONArray) {
                                        com.alibaba.fastjson2.JSONArray fieldsArray = (com.alibaba.fastjson2.JSONArray) fieldsObj;
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
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在: " + tableCode);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("packageName", packageName);
        data.put("className", convertToClassName(table.getTableCode()));
        data.put("entityName", convertToEntityName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

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
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

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

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("componentName", componentName);
        data.put("componentDir", componentDir);
        data.put("nodes", nodes);
        data.put("businessCode", businessCode);
        data.put("businessName", getBusinessName(businessCode));

        Template template = freemarkerConfig.getTemplate("routes.js.ftl");
        StringWriter writer = new StringWriter();
        template.process(data, writer);
        return writer.toString();
    }

    /**
     * 生成完整的代码包（包含所有文件）
     */
    @Override
    public Map<String, String> generateAll(String tableCode, String packageName, String businessCode) throws Exception {
        Map<String, String> codeMap = new HashMap<>();

        // 生成SQL
        codeMap.put("create_table.sql", generateCreateTableSQL(tableCode, businessCode));

        // 生成Java代码
        codeMap.put("Entity.java", generateEntity(tableCode, packageName, businessCode));
        codeMap.put("Controller.java", generateController(tableCode, packageName, businessCode));
        codeMap.put("Service.java", generateService(tableCode, packageName, businessCode));
        codeMap.put("Mapper.java", generateMapper(tableCode, packageName, businessCode));
        codeMap.put("Mapper.xml", generateMapperXml(tableCode, packageName, businessCode));

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
        StringBuilder checkConstraint = new StringBuilder();
        
        // 处理正则表达式
        if (validationRules.containsKey("hasPattern") && (Boolean) validationRules.get("hasPattern")) {
            String pattern = (String) validationRules.get("pattern");
            checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` REGEXP '").append(pattern).append("')");
        }
        
        // 处理数值范围
        if (validationRules.containsKey("hasRange") && (Boolean) validationRules.get("hasRange")) {
            Number min = convertToNumber(validationRules.get("min"));
            Number max = convertToNumber(validationRules.get("max"));
            
            if (min != null && max != null) {
                checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` BETWEEN ").append(min).append(" AND ").append(max).append(")");
            } else if (min != null) {
                checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` >= ").append(min).append(")");
            } else if (max != null) {
                checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` <= ").append(max).append(")");
            }
        }
        
        // 处理枚举值
        if (validationRules.containsKey("hasOptions") && (Boolean) validationRules.get("hasOptions")) {
            Object options = validationRules.get("options");
            if (options instanceof com.alibaba.fastjson2.JSONArray) {
                com.alibaba.fastjson2.JSONArray optionsArray = (com.alibaba.fastjson2.JSONArray) options;
                if (!optionsArray.isEmpty()) {
                    checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` IN (");
                    for (int i = 0; i < optionsArray.size(); i++) {
                        if (i > 0) {
                            checkConstraint.append(", ");
                        }
                        Object option = optionsArray.get(i);
                        if (option instanceof String) {
                            checkConstraint.append("'").append(option).append("'");
                        } else {
                            checkConstraint.append(option);
                        }
                    }
                    checkConstraint.append(")");
                }
            }
        }
        
        // 处理操作符（IN、BETWEEN等）
        if (validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
            String operator = (String) validationRules.get("operator");
            
            // 处理IN操作符
            if ("IN".equalsIgnoreCase(operator)) {
                Object values = validationRules.get("values");
                if (values instanceof com.alibaba.fastjson2.JSONArray) {
                    com.alibaba.fastjson2.JSONArray valuesArray = (com.alibaba.fastjson2.JSONArray) values;
                    if (!valuesArray.isEmpty()) {
                        checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` IN (");
                        for (int i = 0; i < valuesArray.size(); i++) {
                            if (i > 0) {
                                checkConstraint.append(", ");
                            }
                            Object value = valuesArray.get(i);
                            if (value instanceof String) {
                                checkConstraint.append("'").append(value).append("'");
                            } else {
                                checkConstraint.append(value);
                            }
                        }
                        checkConstraint.append(")");
                    }
                }
            }
            
            // 处理BETWEEN操作符
            else if ("BETWEEN".equalsIgnoreCase(operator)) {
                Number min = convertToNumber(validationRules.get("min"));
                Number max = convertToNumber(validationRules.get("max"));
                if (min != null && max != null) {
                    checkConstraint.append("CHECK (`").append(field.getFieldName()).append("` BETWEEN ").append(min).append(" AND ").append(max).append(")");
                }
            }
        }
        
        return checkConstraint.length() > 0 ? checkConstraint.toString() : null;
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
}