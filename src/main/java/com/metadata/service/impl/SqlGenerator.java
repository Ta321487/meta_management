package com.metadata.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataBusinessRule;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.MetadataTableService;
import com.metadata.service.exception.CodeGenException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL生成模块，负责SQL生成相关逻辑
 */
public class SqlGenerator {
    
    private MetadataTableService tableService;
    private MetadataFieldService fieldService;
    private MetadataBusinessSystemService businessSystemService;
    private MetadataFunctionNodeMapper nodeMapper;
    private MetadataBusinessRuleService businessRuleService;
    private TemplateManager templateManager;
    
    /**
     * 构造方法
     * @param tableService 表服务
     * @param fieldService 字段服务
     * @param businessSystemService 业务系统服务
     * @param nodeMapper 功能节点Mapper
     * @param businessRuleService 业务规则服务
     */
    public SqlGenerator(MetadataTableService tableService,
                        MetadataFieldService fieldService,
                        MetadataBusinessSystemService businessSystemService,
                        MetadataFunctionNodeMapper nodeMapper,
                        MetadataBusinessRuleService businessRuleService) {
        this.tableService = tableService;
        this.fieldService = fieldService;
        this.businessSystemService = businessSystemService;
        this.nodeMapper = nodeMapper;
        this.businessRuleService = businessRuleService;
        this.templateManager = TemplateManager.getInstance();
    }
    
    /**
     * 生成建表SQL
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return 建表SQL
     * @throws CodeGenException 代码生成异常
     */
    public String generateCreateTableSQL(String tableCode, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        if (fields.isEmpty()) {
            throw new CodeGenException("FIELD_NOT_FOUND", "表没有配置字段: " + tableCode);
        }

        // 为每个字段添加转换后的属性
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields);
        
        // 生成CHECK约束列表
        List<String> checkConstraints = new ArrayList<>();
        String tableName = CodeGenUtils.convertToTableName(table.getTableCode());
        for (MetadataField field : fields) {
            String checkConstraint = CodeGenUtils.generateCheckConstraint(field);
            if (checkConstraint != null && !checkConstraint.isEmpty()) {
                String constraintName = "ck_" + tableName + "_" + field.getFieldName();
                checkConstraints.add(constraintName + " " + checkConstraint);
            }
        }
        
        // 生成UNIQUE约束列表
        List<String> uniqueConstraints = generateUniqueConstraints(tableCode, businessCode, fields, tableName);
        
        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("tableName", tableName);
        data.put("checkConstraints", checkConstraints);
        data.put("uniqueConstraints", uniqueConstraints);
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));

        return templateManager.processTemplate("create_table.sql.ftl", data);
    }
    
    /**
     * 生成UNIQUE约束列表
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @param fields 字段列表
     * @param tableName 表名
     * @return UNIQUE约束列表
     */
    private List<String> generateUniqueConstraints(String tableCode, String businessCode, List<MetadataField> fields, String tableName) {
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
        return uniqueConstraints;
    }
    
    /**
     * 生成添加字段的ALTER TABLE语句
     * @param tableCode 表编码
     * @param field 字段信息
     * @return ALTER TABLE语句
     * @throws CodeGenException 代码生成异常
     */
    public String generateAlterTableAddColumnSQL(String tableCode, MetadataField field) throws CodeGenException {
        String tableName = CodeGenUtils.convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" ADD COLUMN `").append(field.getFieldName()).append("` ");
        
        // 处理字段类型和长度
        String fieldType = field.getFieldType();
        Map<String, Object> validationRules = CodeGenUtils.parseValidationRule(field.getValidateRule());
        
        // 转换长度限制
        if (validationRules.containsKey("hasLength") && (Boolean) validationRules.get("hasLength")) {
            Number maxLengthNum = CodeGenUtils.convertToNumber(validationRules.get("maxLength"));
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
        String checkConstraint = CodeGenUtils.generateCheckConstraint(field);
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
     * @param tableCode 表编码
     * @param field 字段信息
     * @return ALTER TABLE语句
     * @throws CodeGenException 代码生成异常
     */
    public String generateAlterTableModifyColumnSQL(String tableCode, MetadataField field) throws CodeGenException {
        String tableName = CodeGenUtils.convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" MODIFY COLUMN `").append(field.getFieldName()).append("` ");
        
        // 处理字段类型和长度
        String fieldType = field.getFieldType();
        Map<String, Object> validationRules = CodeGenUtils.parseValidationRule(field.getValidateRule());
        
        // 处理ENUM类型：如果字段类型是ENUM，需要从validate_rule中提取values生成完整的ENUM定义
        if (fieldType != null && fieldType.toUpperCase().equals("ENUM")) {
            if (validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
                String operator = (String) validationRules.get("operator");
                if ("IN".equalsIgnoreCase(operator) && validationRules.containsKey("values")) {
                    Object valuesObj = validationRules.get("values");
                    if (valuesObj instanceof JSONArray) {
                        JSONArray valuesArray = (JSONArray) valuesObj;
                        StringBuilder enumDef = new StringBuilder("ENUM(");
                        for (int i = 0; i < valuesArray.size(); i++) {
                            if (i > 0) {
                                enumDef.append(",");
                            }
                            String value = valuesArray.getString(i);
                            // 转义单引号
                            value = value.replace("'", "''");
                            enumDef.append("'").append(value).append("'");
                        }
                        enumDef.append(")");
                        fieldType = enumDef.toString();
                    }
                }
            }
            // 如果无法从validate_rule中提取values，保持原样（可能是数据库已有完整定义）
        }
        
        // 转换长度限制：仅当fieldType不包含括号（即没有指定长度）时，才考虑使用validationRules中的maxLength
        if (fieldType != null && fieldType.indexOf('(') == -1 && validationRules.containsKey("hasLength") && (Boolean) validationRules.get("hasLength")) {
            Number maxLengthNum = CodeGenUtils.convertToNumber(validationRules.get("maxLength"));
            Integer maxLength = maxLengthNum != null ? maxLengthNum.intValue() : null;
            if (maxLength != null && fieldType.toLowerCase().contains("varchar")) {
                fieldType = "VARCHAR(" + maxLength + ")";
            }
        }
        
        // 确保fieldType不为null
        if (fieldType == null) {
            fieldType = "VARCHAR(255)"; // 默认类型
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
        
        return sql.toString();
    }
    
    /**
     * 生成修改字段名称和属性的ALTER TABLE语句
     * @param tableCode 表编码
     * @param oldFieldName 旧字段名
     * @param field 字段信息
     * @return ALTER TABLE语句
     * @throws CodeGenException 代码生成异常
     */
    public String generateAlterTableChangeColumnSQL(String tableCode, String oldFieldName, MetadataField field) throws CodeGenException {
        String tableName = CodeGenUtils.convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" CHANGE COLUMN `").append(oldFieldName).append("` `").append(field.getFieldName()).append("` ");
        
        // 处理字段类型和长度
        String fieldType = field.getFieldType();
        Map<String, Object> validationRules = CodeGenUtils.parseValidationRule(field.getValidateRule());
        
        // 处理ENUM类型：如果字段类型是ENUM，需要从validate_rule中提取values生成完整的ENUM定义
        if (fieldType != null && fieldType.toUpperCase().equals("ENUM")) {
            if (validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
                String operator = (String) validationRules.get("operator");
                if ("IN".equalsIgnoreCase(operator) && validationRules.containsKey("values")) {
                    Object valuesObj = validationRules.get("values");
                    if (valuesObj instanceof JSONArray) {
                        JSONArray valuesArray = (JSONArray) valuesObj;
                        StringBuilder enumDef = new StringBuilder("ENUM(");
                        for (int i = 0; i < valuesArray.size(); i++) {
                            if (i > 0) {
                                enumDef.append(",");
                            }
                            String value = valuesArray.getString(i);
                            // 转义单引号
                            value = value.replace("'", "''");
                            enumDef.append("'").append(value).append("'");
                        }
                        enumDef.append(")");
                        fieldType = enumDef.toString();
                    }
                }
            }
            // 如果无法从validate_rule中提取values，保持原样（可能是数据库已有完整定义）
        }
        
        // 转换长度限制：仅当fieldType不包含括号（即没有指定长度）时，才考虑使用validationRules中的maxLength
        if (fieldType != null && fieldType.indexOf('(') == -1 && validationRules.containsKey("hasLength") && (Boolean) validationRules.get("hasLength")) {
            Number maxLengthNum = CodeGenUtils.convertToNumber(validationRules.get("maxLength"));
            Integer maxLength = maxLengthNum != null ? maxLengthNum.intValue() : null;
            if (maxLength != null && fieldType.toLowerCase().contains("varchar")) {
                fieldType = "VARCHAR(" + maxLength + ")";
            }
        }
        
        // 确保fieldType不为null
        if (fieldType == null) {
            fieldType = "VARCHAR(255)"; // 默认类型
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
        
        return sql.toString();
    }
    
    /**
     * 生成删除字段的ALTER TABLE语句
     * @param tableCode 表编码
     * @param fieldName 字段名
     * @return ALTER TABLE语句
     * @throws CodeGenException 代码生成异常
     */
    public String generateAlterTableDropColumnSQL(String tableCode, String fieldName) throws CodeGenException {
        String tableName = CodeGenUtils.convertToTableName(tableCode);
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        sql.append(" DROP COLUMN `").append(fieldName).append("`");
        
        return sql.toString();
    }
    
    /**
     * 生成业务系统下所有表的建表SQL
     * @param businessCode 业务系统编码
     * @return 业务系统下所有表的建表SQL
     * @throws CodeGenException 代码生成异常
     */
    public Map<String, String> generateAllSQLByBusinessSystem(String businessCode) throws CodeGenException {
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
}