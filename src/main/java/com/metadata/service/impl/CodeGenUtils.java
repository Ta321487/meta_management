package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataField;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.exception.CodeGenException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成工具类，包含公共方法和工具函数
 */
public class CodeGenUtils {
    
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
     * Java关键字集合，用于避免生成的名称与关键字冲突
     */
    private static final Set<String> JAVA_KEYWORDS = new HashSet<String>() {
        {
            add("abstract"); add("assert"); add("boolean"); add("break"); add("byte");
            add("case"); add("catch"); add("char"); add("class"); add("const");
            add("continue"); add("default"); add("do"); add("double"); add("else");
            add("enum"); add("extends"); add("final"); add("finally"); add("float");
            add("for"); add("goto"); add("if"); add("implements"); add("import");
            add("instanceof"); add("int"); add("interface"); add("long"); add("native");
            add("new"); add("package"); add("private"); add("protected"); add("public");
            add("return"); add("short"); add("static"); add("strictfp"); add("super");
            add("switch"); add("synchronized"); add("this"); add("throw"); add("throws");
            add("transient"); add("try"); add("void"); add("volatile"); add("while");
            add("true"); add("false"); add("null");
        }
    };
    
    /**
     * 获取业务系统名称
     * @param businessCode 业务系统编码
     * @param businessSystemService 业务系统服务
     * @return 业务系统名称
     */
    public static String getBusinessName(String businessCode, MetadataBusinessSystemService businessSystemService) {
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
     * @param fields 字段列表
     * @return 转换后的字段列表
     */
    public static List<Map<String, Object>> prepareFieldList(List<MetadataField> fields) {
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
            
            // 处理enum类型，从validateRule中获取enum值
            if ("enum('')".equals(formattedFieldType)) {
                Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
                if (validationRules.containsKey("hasOptions") && (Boolean) validationRules.get("hasOptions")) {
                    Object options = validationRules.get("options");
                    if (options instanceof JSONArray) {
                        JSONArray optionsArray = (JSONArray) options;
                        if (!optionsArray.isEmpty()) {
                            StringBuilder enumValues = new StringBuilder();
                            for (int i = 0; i < optionsArray.size(); i++) {
                                if (i > 0) {
                                    enumValues.append(", ");
                                }
                                Object option = optionsArray.get(i);
                                if (option instanceof String) {
                                    enumValues.append("'").append(option).append("'");
                                } else {
                                    enumValues.append(option);
                                }
                            }
                            formattedFieldType = "enum(" + enumValues.toString() + ")";
                        }
                    }
                }
                
                // 处理操作符为IN的情况，从values字段获取enum值
                if ("enum('')".equals(formattedFieldType) && validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
                    String operator = (String) validationRules.get("operator");
                    if ("IN".equalsIgnoreCase(operator)) {
                        Object values = validationRules.get("values");
                        if (values instanceof JSONArray) {
                            JSONArray valuesArray = (JSONArray) values;
                            if (!valuesArray.isEmpty()) {
                                StringBuilder enumValues = new StringBuilder();
                                for (int i = 0; i < valuesArray.size(); i++) {
                                    if (i > 0) {
                                        enumValues.append(", ");
                                    }
                                    Object value = valuesArray.get(i);
                                    if (value instanceof String) {
                                        enumValues.append("'").append(value).append("'");
                                    } else {
                                        enumValues.append(value);
                                    }
                                }
                                formattedFieldType = "enum(" + enumValues.toString() + ")";
                            }
                        }
                    }
                }
            }
            
            fieldMap.put("fieldType", formattedFieldType);
            fieldMap.put("label", field.getLabel());
            fieldMap.put("isRequired", field.getIsRequired());
            fieldMap.put("formComponent", field.getFormComponent());
            fieldMap.put("javaType", getJavaType(field.getFieldType()));
            
            // 生成字段名并检查是否为关键字
            String camelCaseName = convertToCamelCase(field.getFieldName(), false);
            if (JAVA_KEYWORDS.contains(camelCaseName)) {
                camelCaseName = "_" + camelCaseName;
            }
            fieldMap.put("camelCaseName", camelCaseName);
            
            // 解析校验规则
            Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
            fieldMap.put("validationRules", validationRules);
            
            fieldList.add(fieldMap);
        }
        return fieldList;
    }
    
    /**
     * 格式化字段类型，确保生成有效的MySQL数据类型
     * @param fieldType 字段类型
     * @return 格式化后的字段类型
     */
    public static String formatFieldType(String fieldType) {
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
     * 解析校验规则JSON，提取正则表达式等信息
     * @param validateRule 校验规则JSON字符串
     * @return 解析后的校验规则
     */
    public static Map<String, Object> parseValidationRule(String validateRule) {
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
     * @param value 要转换的值
     * @return 转换后的数值
     */
    public static Number convertToNumber(Object value) {
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
     * 工具方法：转换为类名（大驼峰）
     * @param code 编码
     * @return 类名
     */
    public static String convertToClassName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的类名
        String processedCode = code.replace("_TABLE", "");
        String className = convertToCamelCase(processedCode, true);
        // 检查是否为Java关键字，若是则添加前缀
        if (JAVA_KEYWORDS.contains(className.toLowerCase())) {
            return "_" + className;
        }
        return className;
    }
    
    /**
     * 工具方法：转换为实体名（小驼峰）
     * @param code 编码
     * @return 实体名
     */
    public static String convertToEntityName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的实体名
        String processedCode = code.replace("_TABLE", "");
        String entityName = convertToCamelCase(processedCode, false);
        // 检查是否为Java关键字，若是则添加前缀
        if (JAVA_KEYWORDS.contains(entityName)) {
            return "_" + entityName;
        }
        return entityName;
    }
    
    /**
     * 工具方法：转换为组件名
     * @param code 编码
     * @return 组件名
     */
    public static String convertToComponentName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的组件名
        String processedCode = code.replace("_TABLE", "");
        return convertToCamelCase(processedCode, true);
    }
    
    /**
     * 工具方法：转换为表名（下划线）
     * @param code 编码
     * @return 表名
     */
    public static String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        return code.toLowerCase().replace("_TABLE", "");
    }
    
    /**
     * 工具方法：转换为驼峰命名
     * @param code 编码
     * @param firstUpper 首字母是否大写
     * @return 驼峰命名
     */
    public static String convertToCamelCase(String code, boolean firstUpper) {
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
        String result = sb.toString();
        // 检查是否为Java关键字，若是则添加前缀
        if (JAVA_KEYWORDS.contains(result)) {
            return "_" + result;
        }
        return result;
    }
    
    /**
     * 获取Java类型
     * @param fieldType 字段类型
     * @return Java类型
     */
    public static String getJavaType(String fieldType) {
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
     * @param fields 字段列表
     * @return 是否包含日期类型
     */
    public static boolean hasDate(List<MetadataField> fields) {
        return fields.stream().anyMatch(f -> 
            f.getFieldType() != null && 
            (f.getFieldType().toLowerCase().contains("date") || f.getFieldType().toLowerCase().contains("time"))
        );
    }
    
    /**
     * 检查字段列表中是否有Decimal类型
     * @param fields 字段列表
     * @return 是否包含Decimal类型
     */
    public static boolean hasDecimal(List<MetadataField> fields) {
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
     * @param field 字段信息
     * @return CHECK约束字符串
     */
    public static String generateCheckConstraint(MetadataField field) {
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
}