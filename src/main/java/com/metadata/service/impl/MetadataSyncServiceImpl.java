package com.metadata.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.service.CheckConstraintParser;
import com.metadata.service.MetadataSyncService;
import com.metadata.service.OperationLogService;
import com.metadata.service.constant.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 元数据同步服务实现
 * 负责将数据库表结构、字段和外键同步到元数据系统
 */
@Service
public class MetadataSyncServiceImpl implements MetadataSyncService {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private OperationLogService logService;
    
    @Autowired
    private MetadataTableMapper tableMapper;
    
    @Autowired
    private MetadataFieldMapper fieldMapper;
    
    @Autowired
    private MetadataTableRelationMapper relationMapper;
    
    @Autowired
    private CheckConstraintParser checkConstraintParser;
    
    /**
     * 从 CREATE TABLE 或 ALTER TABLE 语句中提取表名
     */
    @Override
    public String extractTableName(String sql) {
        // 匹配 CREATE TABLE `table_name` 或 CREATE TABLE table_name
        // 匹配 ALTER TABLE `table_name` 或 ALTER TABLE table_name
        Pattern pattern = Pattern.compile("(?:CREATE|ALTER)\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?`?([^\\s`]+)`?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        String matchedTableName = null;
        if (matcher.find()) {
            matchedTableName = matcher.group(1);
        }
        // 记录匹配结果
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_TABLE_NAME_MATCH, 
            "表名匹配结果: 原始SQL=" + sql.substring(0, Math.min(50, sql.length())) + "..., 匹配到表名=" + matchedTableName);
        return matchedTableName;
    }
    
    /**
     * 同步数据库表字段到元数据系统
     */
    @Override
    public void syncTableFields(String tableName, Connection connection) throws Exception {
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_TABLE_FIELDS_START, "开始同步表字段: " + tableName);
        // 使用新的连接，确保能看到最新的表结构
        try (Connection newConnection = dataSource.getConnection()) {
            DatabaseMetaData metaData = newConnection.getMetaData();
            String catalog = newConnection.getCatalog();
            String schema = newConnection.getSchema();
        
        // 查找对应的表编码（通过表名匹配，表名可能是表编码或实际表名）
        MetadataTable table = tableMapper.selectByCode(tableName.toUpperCase());
        if (table == null) {
            // 如果表编码不存在，尝试通过表名查找
            List<MetadataTable> tables = tableMapper.selectAll(null);
            for (MetadataTable t : tables) {
                // 如果表名就是表编码，或者有其他映射关系
                if (tableName.equalsIgnoreCase(t.getTableCode()) || 
                    tableName.equalsIgnoreCase(t.getTableName())) {
                    table = t;
                    break;
                }
            }
        }
        
        String tableCode;
        boolean tableCreated = false;
        
        if (table == null) {
            // 如果找不到对应的表记录，自动创建表记录
            // 使用表名的大写形式作为表编码
            tableCode = tableName.toUpperCase();
            
            // 检查表编码是否已存在（防止重复）
            if (tableMapper.countByCode(tableCode) > 0) {
                // 如果已存在，直接使用
                table = tableMapper.selectByCode(tableCode);
            } else {
                // 从数据库获取表的注释信息和主键策略
                String tableComment = "";
                String pkStrategy = "NONE";
                
                // 通过查询 information_schema 获取表注释（MySQL）
                // 使用PreparedStatement防止SQL注入，表名使用反引号包裹
                try (PreparedStatement pstmt = connection.prepareStatement(
                         "SELECT TABLE_COMMENT FROM information_schema.TABLES " +
                         "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?")) {
                    pstmt.setString(1, tableName);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            tableComment = rs.getString("TABLE_COMMENT");
                            if (tableComment == null) {
                                tableComment = "";
                            }
                        }
                    }
                } catch (Exception e) {
                    // 如果查询失败，使用表名作为默认值
                    tableComment = "";
                }
                
                // 检查是否有自增主键
                try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName)) {
                    if (primaryKeys.next()) {
                        String pkColumnName = primaryKeys.getString("COLUMN_NAME");
                        // 检查该主键列是否是自增的
                        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, pkColumnName)) {
                            if (columns.next()) {
                                String isAutoIncrement = columns.getString(SqlConstants.COLUMN_IS_AUTOINCREMENT);
                                if (SqlConstants.YES.equalsIgnoreCase(isAutoIncrement)) {
                                    pkStrategy = "AUTO";
                                }
                            }
                        }
                    }
                }
                
                // 创建表记录
                table = new MetadataTable();
                table.setTableCode(tableCode);
                // 如果表注释为空，使用表名作为表名称
                table.setTableName(tableComment != null && !tableComment.isEmpty() ? tableComment : tableName);
                table.setPkStrategy(pkStrategy);
                table.setDescription(tableComment);
                table.setIsEnabled(1); // 设置默认启用状态
                table.setBusinessCode("DEFAULT"); // 设置默认业务系统编码
                tableMapper.insert(table);
                tableCreated = true;
                
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_TABLE, "自动创建表记录: " + tableCode);
            }
        } else {
            tableCode = table.getTableCode();
        }

        // 先获取主键信息，判断是否有自增主键以及所有主键字段
        String autoIncrementPkColumn = null;
        Set<String> primaryKeyColumns = new HashSet<>();
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (primaryKeys.next()) {
                String pkColumnName = primaryKeys.getString("COLUMN_NAME");
                primaryKeyColumns.add(pkColumnName); // 记录所有主键字段
                // 检查该主键列是否是自增的
                try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, pkColumnName)) {
                    if (columns.next()) {
                        String isAutoIncrement = columns.getString(SqlConstants.COLUMN_IS_AUTOINCREMENT);
                        if (SqlConstants.YES.equalsIgnoreCase(isAutoIncrement)) {
                            autoIncrementPkColumn = pkColumnName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 获取主键信息失败，不影响字段同步，只记录日志
            logService.logError("admin", SqlConstants.LOG_MODULE_GET_PRIMARY_KEY, "获取主键信息失败: " + tableName, e.getMessage());
        }
        
        // 使用 SHOW COLUMNS 获取表结构（直接查询数据库，确保获取最新字段信息）
        Map<String, MetadataField> physicalFields = new HashMap<>();
        try (Statement stmt = newConnection.createStatement()) {
            String columnsSql = "SHOW FULL COLUMNS FROM `" + tableName + "`";
            ResultSet columns = stmt.executeQuery(columnsSql);
            int sort = 0;
            while (columns.next()) {
                String columnName = columns.getString("Field");
                String columnTypeFull = columns.getString("Type");
                String nullableStr = columns.getString("Null");
                String extra = columns.getString("Extra");
                String comments = columns.getString("Comment");
                
                // 解析字段类型和大小
                String columnType = columnTypeFull;
                int columnSize = 0;
                // 提取字段类型（如 VARCHAR(10) -> VARCHAR）
                Pattern typePattern = Pattern.compile("^(\\w+)(?:\\((\\d+)\\))?");
                Matcher typeMatcher = typePattern.matcher(columnTypeFull);
                if (typeMatcher.find()) {
                    columnType = typeMatcher.group(1);
                    if (typeMatcher.group(2) != null) {
                        columnSize = Integer.parseInt(typeMatcher.group(2));
                    }
                }
                
                // 构建字段类型字符串
                String fieldType = columnType;
                if (columnSize > 0 && (columnType.equalsIgnoreCase("VARCHAR") || columnType.equalsIgnoreCase("CHAR") || 
                    columnType.equalsIgnoreCase("DECIMAL") || columnType.equalsIgnoreCase("NUMERIC"))) {
                    fieldType = columnType + "(" + columnSize + ")";
                }
                
                // 生成字段编码（使用列名的大写形式）
                String fieldCode = columnName.toUpperCase();
                
                // 创建字段对象
                MetadataField field = new MetadataField();
                field.setFieldCode(fieldCode);
                field.setTableCode(tableCode);
                field.setFieldName(columnName);
                field.setFieldType(fieldType);
                field.setLabel(comments != null && !comments.isEmpty() ? comments : columnName);
                field.setBusinessCode(table.getBusinessCode()); // 使用表的业务系统编码
                
                // 判断是否是自增主键
                boolean isAutoIncrementPk = (autoIncrementPkColumn != null && 
                    autoIncrementPkColumn.equalsIgnoreCase(columnName)) ||
                    (extra != null && extra.contains(SqlConstants.AUTO_INCREMENT));
                
                // 判断是否是主键字段
                boolean isPrimaryKey = primaryKeyColumns.contains(columnName);
                
                if (isPrimaryKey) {
                    // 所有主键字段：使用primary_key表单组件
                    field.setFormComponent("primary_key"); // 主键字段使用primary_key表单组件
                    // 自增主键对用户来说不是必填，普通主键可能需要必填
                    field.setIsRequired(isAutoIncrementPk ? 0 : ("NO".equalsIgnoreCase(nullableStr) ? 1 : 0));
                } else {
                    // 普通字段：根据字段类型设置表单组件和必填状态
                    field.setIsRequired("NO".equalsIgnoreCase(nullableStr) ? 1 : 0);
                    field.setFormComponent(getDefaultFormComponent(fieldType));
                }
                
                field.setSort(sort++);
                field.setIsEnabled(1);
                
                physicalFields.put(fieldCode, field);
            }
        }
        
        // 获取表的CHECK约束
        Map<String, String> checkConstraints = new HashMap<>();
        try {
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "开始获取表CHECK约束: " + tableName);
            
            // 1. 直接使用SHOW CREATE TABLE获取表结构，这是最可靠的方式
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                String showCreateSql = "SHOW CREATE TABLE `" + tableName + "`";
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "执行SQL: " + showCreateSql);
                ResultSet rs = stmt.executeQuery(showCreateSql);
                if (rs.next()) {
                    String createTableSql = rs.getString(2);
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "SHOW CREATE TABLE结果: " + createTableSql);
                    
                    // 解析CREATE TABLE语句，提取CHECK约束
                    checkConstraints = checkConstraintParser.extractCheckConstraints(createTableSql);
                }
            } catch (Exception e) {
                logService.logError("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "使用SHOW CREATE TABLE获取CHECK约束失败", e.getMessage());
                
                // 2. 尝试从information_schema.CHECK_CONSTRAINTS表查询（MySQL 8.0+）
                try (Connection conn = dataSource.getConnection();
                     Statement stmt = conn.createStatement()) {
                    // MySQL 8.0中CHECK_CONSTRAINTS表的查询方式，需要关联TABLE_CONSTRAINTS表获取TABLE_NAME
                    String checkSql = "SELECT cc.CONSTRAINT_NAME, cc.CHECK_CLAUSE " +
                                     "FROM information_schema.CHECK_CONSTRAINTS cc " +
                                     "JOIN information_schema.TABLE_CONSTRAINTS tc " +
                                     "ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA " +
                                     "AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME " +
                                     "WHERE cc.CONSTRAINT_SCHEMA = '" + schema + "' " +
                                     "AND tc.TABLE_NAME = '" + tableName + "' " +
                                     "AND tc.CONSTRAINT_TYPE = '" + SqlConstants.CONSTRAINT_TYPE_CHECK + "'";
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "执行SQL: " + checkSql);
                    ResultSet rs = stmt.executeQuery(checkSql);
                    while (rs.next()) {
                        // 移除未使用的变量 constraintName
                        String checkClause = rs.getString("CHECK_CLAUSE");
                        logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "从information_schema提取到CHECK约束子句: " + checkClause);
                        
                        // 从约束条件中提取字段名
                        String columnName = checkConstraintParser.extractFieldNameFromConstraint(checkClause);
                        
                        if (columnName != null && !columnName.isEmpty()) {
                            checkConstraints.put(columnName.toUpperCase(), "CHECK (" + checkClause + ")");
                            logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "从information_schema提取CHECK约束: " + columnName + " -> " + checkClause);
                        } else {
                            logService.logError("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "无法从information_schema的CHECK约束中提取字段名", checkClause);
                        }
                    }
                } catch (Exception ex) {
                    logService.logError("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "从information_schema获取CHECK约束失败", ex.getMessage());
                }
            }
        } catch (Exception e) {
            // 最外层异常捕获，确保CHECK约束处理失败不会影响字段同步
            logService.logError("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "处理CHECK约束时发生异常", e.getMessage());
        }
        
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "总共获取到" + checkConstraints.size() + "个CHECK约束: " + checkConstraints);
        
        // 获取现有字段
        List<MetadataField> existingFields = fieldMapper.selectByTableCode(tableCode);
        Map<String, MetadataField> existingFieldMap = new HashMap<>();
        for (MetadataField field : existingFields) {
            existingFieldMap.put(field.getFieldCode(), field);
        }
        
        // 1. 更新或添加字段
        for (Map.Entry<String, MetadataField> entry : physicalFields.entrySet()) {
            String fieldCode = entry.getKey();
            MetadataField physicalField = entry.getValue();
            String columnName = physicalField.getFieldName();
            
            // 检查是否有对应的CHECK约束
            if (checkConstraints.containsKey(columnName.toUpperCase())) {
                try {
                    String checkConstraint = checkConstraints.get(columnName.toUpperCase());
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析CHECK约束: " + checkConstraint);
                    String validateRule = checkConstraintParser.parseCheckConstraint(checkConstraint);
                    if (validateRule != null) {
                        physicalField.setValidateRule(validateRule);
                        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SET_VALIDATE_RULE, "设置校验规则: " + columnName + " -> " + validateRule);
                        
                        // 检查是否为IN约束，如果是则设置表单组件为下拉框并转换为ENUM类型
                        if (validateRule.contains("\"operator\":\"IN\"")) {
                            physicalField.setFormComponent("select");
                            logService.logSuccess("admin", SqlConstants.LOG_MODULE_SET_FORM_COMPONENT, "设置表单组件为下拉框: " + columnName + " -> select");
                            
                            // 解析validateRule JSON，提取values数组，转换为ENUM类型
                            try {
                                JSONObject jsonObj = JSONObject.parseObject(validateRule);
                                if (jsonObj.containsKey("values")) {
                                    JSONArray valuesArray = jsonObj.getJSONArray("values");
                                    if (valuesArray != null && !valuesArray.isEmpty()) {
                                        // 简化ENUM类型表示，只使用ENUM而不包含具体的值，避免超过字段长度限制
                                        String enumType = "ENUM";
                                        physicalField.setFieldType(enumType);
                                        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SET_ENUM_TYPE, "设置字段类型为ENUM: " + columnName + " -> " + enumType);
                                    }
                                }
                            } catch (Exception e) {
                                // JSON解析失败，记录日志但不影响字段同步
                                logService.logError("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析IN约束JSON失败: " + columnName, e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    // 解析CHECK约束失败，记录日志但不影响字段同步
                    logService.logError("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析CHECK约束失败: " + columnName, e.getMessage());
                }
            }
            
            if (existingFieldMap.containsKey(fieldCode)) {
                // 字段已存在，更新字段信息
                MetadataField existingField = existingFieldMap.get(fieldCode);
                
                // 更新字段信息（只更新物理表相关的字段，保留用户手动配置的其他字段）
                existingField.setFieldName(physicalField.getFieldName());
                // 对于字段类型，如果是IN约束转换的ENUM类型，则更新；否则尊重用户手动配置
                if (physicalField.getFieldType().equalsIgnoreCase("ENUM") && physicalField.getValidateRule() != null && physicalField.getValidateRule().contains("\"operator\":\"IN\"")) {
                    existingField.setFieldType(physicalField.getFieldType());
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_SET_ENUM_TYPE, "更新字段类型为ENUM: " + columnName + " -> " + physicalField.getFieldType());
                } else {
                    // 保留用户手动配置的字段类型
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "保留用户手动配置的字段类型: " + columnName + " -> " + existingField.getFieldType());
                }
                existingField.setIsRequired(physicalField.getIsRequired());
                existingField.setSort(physicalField.getSort());
                
                // 只有当字段备注不为空时才更新label，避免覆盖用户手动设置的label
                if (physicalField.getLabel() != null && !physicalField.getLabel().isEmpty() && 
                    !physicalField.getLabel().equals(physicalField.getFieldName())) {
                    existingField.setLabel(physicalField.getLabel());
                }
                
                // 只有当表单组件为空时才设置默认值，保留用户手动配置的表单组件
                if ((existingField.getFormComponent() == null || existingField.getFormComponent().isEmpty()) && 
                    physicalField.getFormComponent() != null && !physicalField.getFormComponent().isEmpty()) {
                    existingField.setFormComponent(physicalField.getFormComponent());
                }
                
                // 更新校验规则：无论现有规则是否为空，都同步物理表的CHECK约束
                if (physicalField.getValidateRule() != null && !physicalField.getValidateRule().isEmpty()) {
                    existingField.setValidateRule(physicalField.getValidateRule());
                } else {
                    // 如果解析后的校验规则为空，清空现有校验规则
                    if (existingField.getValidateRule() != null && !existingField.getValidateRule().isEmpty()) {
                        existingField.setValidateRule(null);
                        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SET_VALIDATE_RULE, "清空校验规则: " + tableCode + "." + fieldCode);
                    }
                }
                
                fieldMapper.update(existingField);
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "更新字段: " + tableCode + "." + fieldCode);
            } else {
                // 字段不存在，添加新字段
                fieldMapper.insert(physicalField);
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "添加字段: " + tableCode + "." + fieldCode);
            }
        }
        
        // 2. 删除物理表中不存在的字段
        // 注意：仅删除已启用的字段，避免删除刚刚插入的未启用字段
        for (MetadataField existingField : existingFields) {
            String fieldCode = existingField.getFieldCode();
            if (!physicalFields.containsKey(fieldCode)) {
                // 物理表中不存在该字段，删除元数据中的字段
                fieldMapper.deleteById(existingField.getId());
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "删除字段: " + tableCode + "." + fieldCode);
            }
        }
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_TABLE_FIELDS_END, "表字段同步完成: " + tableName);
        }
    }
    
    /**
     * 根据字段类型获取默认的表单组件
     */
    private String getDefaultFormComponent(String fieldType) {
        String upperType = fieldType.toUpperCase();
        if (upperType.contains("INT") || upperType.contains("BIGINT") || 
            upperType.contains("DECIMAL") || upperType.contains("NUMERIC") || 
            upperType.contains("FLOAT") || upperType.contains("DOUBLE")) {
            return "number";
        } else if (upperType.contains("DATE") || upperType.contains("TIME")) {
            return "datepicker";
        } else if (upperType.contains("TEXT")) {
            return "textarea";
        } else {
            return "input";
        }
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
     * 同步数据库表外键到元数据关联关系系统（内部方法）
     * @return 创建的关联关系记录数
     */
    @Override
    public int syncTableForeignKeys(String tableName, Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        
        // 查找对应的表编码
        MetadataTable table = tableMapper.selectByCode(tableName.toUpperCase());
        if (table == null) {
            // 如果表编码不存在，尝试通过表名查找
            List<MetadataTable> tables = tableMapper.selectAll(null);
            for (MetadataTable t : tables) {
                if (tableName.equalsIgnoreCase(t.getTableCode()) || 
                    tableName.equalsIgnoreCase(t.getTableName())) {
                    table = t;
                    break;
                }
            }
        }
        
        if (table == null) {
            // 如果找不到表记录，无法同步外键
            return 0;
        }
        
        String slaveTableCode = table.getTableCode();
        int createdCount = 0;
        
        // 获取该表的所有外键
        try (ResultSet foreignKeys = metaData.getImportedKeys(catalog, schema, tableName)) {
            while (foreignKeys.next()) {
                String pkTableName = foreignKeys.getString("PKTABLE_NAME"); // 主表名
                String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME"); // 主表字段名
                String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME"); // 从表外键字段名
                
                // 查找主表编码
                MetadataTable mainTable = tableMapper.selectByCode(pkTableName.toUpperCase());
                if (mainTable == null) {
                    // 尝试通过表名查找
                    List<MetadataTable> tables = tableMapper.selectAll(null);
                    for (MetadataTable t : tables) {
                        if (pkTableName.equalsIgnoreCase(t.getTableCode()) || 
                            pkTableName.equalsIgnoreCase(t.getTableName())) {
                            mainTable = t;
                            break;
                        }
                    }
                }
                
                if (mainTable == null) {
                    // 主表不存在，跳过
                    continue;
                }
                
                String mainTableCode = mainTable.getTableCode();
                
                // 查找主表字段编码
                MetadataField mainField = fieldMapper.selectByCode(mainTableCode, pkColumnName.toUpperCase());
                if (mainField == null) {
                    // 尝试通过字段名查找
                    List<MetadataField> fields = fieldMapper.selectByTableCode(mainTableCode);
                    for (MetadataField f : fields) {
                        if (pkColumnName.equalsIgnoreCase(f.getFieldName())) {
                            mainField = f;
                            break;
                        }
                    }
                }
                
                if (mainField == null) {
                    // 主表字段不存在，跳过
                    continue;
                }
                
                // 查找从表字段编码
                MetadataField slaveField = fieldMapper.selectByCode(slaveTableCode, fkColumnName.toUpperCase());
                if (slaveField == null) {
                    // 尝试通过字段名查找
                    List<MetadataField> fields = fieldMapper.selectByTableCode(slaveTableCode);
                    for (MetadataField f : fields) {
                        if (fkColumnName.equalsIgnoreCase(f.getFieldName())) {
                            slaveField = f;
                            break;
                        }
                    }
                }
                
                if (slaveField == null) {
                    // 从表字段不存在，跳过
                    continue;
                }
                
                // 生成关联编码（确保不超过50个字符）
                String relationCode = generateRelationCode(mainTableCode, slaveTableCode, 
                                                          mainField.getFieldCode(), slaveField.getFieldCode());
                
                // 获取业务系统编码（从从表获取）
                String businessCode = table.getBusinessCode();
                
                // 检查关联关系是否已存在（在同一业务系统内）
                if (relationMapper.countByCode(relationCode, businessCode) > 0) {
                    // 已存在，跳过
                    continue;
                }
                
                // 检查是否已经存在由用户手动创建的关联关系记录（根据主表、从表、主字段、从字段的组合）
                System.out.println("检查是否已存在关联关系记录: 主表 = " + mainTableCode + ", 从表 = " + slaveTableCode + ", 主字段 = " + mainField.getFieldCode() + ", 从字段 = " + slaveField.getFieldCode());
                List<MetadataTableRelation> existingRelations = relationMapper.selectByTablesAndFields(
                        mainTableCode, slaveTableCode, mainField.getFieldCode(), slaveField.getFieldCode(), businessCode);
                if (existingRelations != null && !existingRelations.isEmpty()) {
                    // 已存在由用户手动创建的关联关系记录，跳过自动生成
                    System.out.println("已存在关联关系记录，跳过自动生成: " + existingRelations);
                    continue;
                }
                
                // 创建关联关系
                MetadataTableRelation relation = new MetadataTableRelation();
                relation.setRelationCode(relationCode);
                relation.setMainTableCode(mainTableCode);
                relation.setSlaveTableCode(slaveTableCode);
                relation.setMainFieldCode(mainField.getFieldCode());
                relation.setSlaveFieldCode(slaveField.getFieldCode());
                relation.setRelationType(SqlConstants.RELATION_TYPE_ONE_TO_MANY); // 默认一对多
                relation.setRelationName(mainTable.getTableName() + " -> " + table.getTableName());
                relation.setBusinessCode(businessCode); // 设置业务系统编码
                
                relationMapper.insert(relation);
                createdCount++;
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FOREIGN_KEY, "自动创建外键关联关系: " + relationCode);
            }
        } catch (Exception e) {
            // 同步外键失败不影响其他操作，只记录日志
            logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FOREIGN_KEY, "同步外键关联关系失败: " + tableName, e.getMessage());
            throw e; // 重新抛出异常，让调用者知道失败
        }
        
        return createdCount;
    }
    
    /**
     * 生成关联编码（确保不超过50个字符）
     * @param mainTableCode 主表编码
     * @param slaveTableCode 从表编码
     * @param mainFieldCode 主表字段编码
     * @param slaveFieldCode 从表字段编码
     * @return 关联编码（最长50个字符）
     */
    private String generateRelationCode(String mainTableCode, String slaveTableCode, 
                                        String mainFieldCode, String slaveFieldCode) {
        // 先尝试生成简洁的编码
        String simpleCode = SqlConstants.RELATION_CODE_PREFIX + mainTableCode + "_" + slaveTableCode + "_" + 
                           mainFieldCode + "_" + slaveFieldCode;
        
        // 如果不超过50个字符，直接返回
        if (simpleCode.length() <= SqlConstants.MAX_RELATION_CODE_LENGTH) {
            return simpleCode;
        }
        
        // 如果超过50个字符，使用哈希值生成短编码
        try {
            String fullCode = mainTableCode + "_" + slaveTableCode + "_" + 
                             mainFieldCode + "_" + slaveFieldCode;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(fullCode.getBytes("UTF-8"));
            
            // 将哈希值转换为十六进制字符串，取前42个字符，加上"REL_"前缀
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // 取前42个字符，加上"REL_"前缀，总共46个字符
            String hash = hexString.substring(0, Math.min(42, hexString.length()));
            return SqlConstants.RELATION_CODE_PREFIX + hash.toUpperCase();
        } catch (Exception e) {
            // 如果哈希生成失败，使用截断的方式（不推荐，但作为后备方案）
            return simpleCode.substring(0, SqlConstants.MAX_RELATION_CODE_LENGTH);
        }
    }
}