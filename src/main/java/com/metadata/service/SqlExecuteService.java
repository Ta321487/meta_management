package com.metadata.service;

import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL执行服务
 */
@Service
public class SqlExecuteService {

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

    /**
     * 执行SQL语句（内部方法，允许执行DROP TABLE等危险操作）
     * 仅供内部服务调用，不对外暴露
     * @param sql SQL语句
     * @param skipSafetyCheck 是否跳过安全检查
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeSqlInternal(String sql, boolean skipSafetyCheck) {
        Map<String, Object> result = new HashMap<>();
        
        if (sql == null || sql.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "SQL语句不能为空");
            return result;
        }

        // 移除SQL注释和多余空白
        sql = sql.trim();
        
        // 检查是否为危险操作（DROP、TRUNCATE等）
        String upperSql = sql.toUpperCase().trim();
        
        if (!skipSafetyCheck) {
            // 检查明显的危险操作
            if (upperSql.startsWith("DROP") || upperSql.startsWith("TRUNCATE") || 
                upperSql.startsWith("DELETE FROM") || upperSql.startsWith("SHOW") || 
                upperSql.startsWith("DESC") || upperSql.startsWith("DESCRIBE") ||
                upperSql.startsWith("ALTER TABLE") || upperSql.startsWith("RENAME TABLE") ||
                upperSql.startsWith("CREATE DATABASE") || upperSql.startsWith("DROP DATABASE") ||
                upperSql.startsWith("GRANT") || upperSql.startsWith("REVOKE") ||
                upperSql.startsWith("FLUSH") || upperSql.startsWith("RESET") ||
                upperSql.startsWith("LOAD DATA") || upperSql.startsWith("SELECT INTO OUTFILE")) {
                
                // 特殊处理ALTER TABLE，只允许添加字段等安全操作
                if (upperSql.startsWith("ALTER TABLE")) {
                    // 只允许ALTER TABLE中的ADD COLUMN、MODIFY COLUMN、CHANGE COLUMN、ADD INDEX、ADD CONSTRAINT等安全操作
                    // 禁止DROP、RENAME等危险操作
                    if (upperSql.contains(" DROP ") || upperSql.contains(" DROP,") || 
                        upperSql.contains(",DROP ") || upperSql.endsWith(" DROP") ||
                        upperSql.contains(" RENAME ") || upperSql.contains(" RENAME COLUMN") ||
                        upperSql.contains(" DROP COLUMN") || upperSql.contains(" DROP INDEX") ||
                        upperSql.contains(" DROP CONSTRAINT")) {
                        result.put("success", false);
                        result.put("message", "禁止执行ALTER TABLE中的DROP、RENAME等危险操作");
                        return result;
                    }
                } else {
                    // 其他危险操作直接禁止
                    result.put("success", false);
                    result.put("message", "禁止执行该操作，仅允许INSERT、UPDATE、SELECT、CREATE TABLE等低风险操作");
                    return result;
                }
            }
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 判断是否为查询语句，仅允许SELECT语句
            boolean isQuery = upperSql.startsWith("SELECT");
            
            if (isQuery) {
                // 查询语句使用JdbcTemplate执行
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                
                result.put("success", true);
                result.put("message", "执行成功");
                result.put("data", rows);
                result.put("rowCount", rows.size());
                
                logService.logSuccess("admin", "SQL_QUERY", "执行查询SQL: " + sql.substring(0, Math.min(100, sql.length())));
            } else {
                // 非查询语句（INSERT、UPDATE、CREATE等）
                int affectedRows = statement.executeUpdate(sql);
                
                result.put("success", true);
                result.put("message", "执行成功");
                result.put("affectedRows", affectedRows);
                
                // 如果是 CREATE TABLE 或 ALTER TABLE 语句，自动同步字段到元数据系统
                if (upperSql.startsWith("CREATE TABLE") || upperSql.startsWith("ALTER TABLE")) {
                    try {
                        String tableName = extractTableName(sql);
                        if (tableName != null) {
                            syncTableFields(tableName, connection);
                            // 同步外键关联关系
                            syncTableForeignKeys(tableName, connection);
                        }
                    } catch (Exception e) {
                        // 同步失败不影响 SQL 执行结果，只记录日志
                        String tableName = extractTableName(sql);
                        logService.logError("admin", "SYNC_FIELDS", "同步表字段失败: " + (tableName != null ? tableName : "未知表"), e.getMessage());
                    }
                }
                
                logService.logSuccess("admin", "SQL_EXECUTE", "执行SQL: " + sql.substring(0, Math.min(100, sql.length())) + "，影响行数: " + affectedRows);
            }
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "SQL执行失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
            
            logService.logError("admin", "SQL_EXECUTE", "执行SQL", e.getMessage());
        }
        
        return result;
    }

    /**
     * 执行SQL语句（对外接口，禁止危险操作）
     * @param sql SQL语句
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeSql(String sql) {
        return executeSqlInternal(sql, false);
    }
    
    /**
     * 执行SQL语句（支持跳过安全检查）
     * @param sql SQL语句
     * @param skipSafetyCheck 是否跳过安全检查
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeSql(String sql, boolean skipSafetyCheck) {
        return executeSqlInternal(sql, skipSafetyCheck);
    }

    /**
     * 执行DROP TABLE语句（仅供内部服务调用）
     * @param tableName 表名
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeDropTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "表名不能为空");
            return result;
        }
        
        // 转义表名，防止SQL注入
        String safeTableName = tableName.trim().replace("`", "").replace("'", "").replace("\"", "");
        String dropSql = "DROP TABLE IF EXISTS `" + safeTableName + "`";
        
        Map<String, Object> result = executeSqlInternal(dropSql, true);
        
        // 如果DROP TABLE执行成功，清理metadata_field表中的相关字段
        if ((Boolean) result.get("success")) {
            try {
                // 查找对应的表编码
                String tableCode = safeTableName.toUpperCase();
                MetadataTable table = tableMapper.selectByCode(tableCode);
                
                if (table != null) {
                    // 删除该表的所有字段
                    fieldMapper.deleteByTableCode(tableCode);
                    logService.logSuccess("admin", "SYNC_FIELDS", "删除表字段: " + tableCode);
                    
                    // 删除表记录
                    tableMapper.deleteById(table.getId());
                    logService.logSuccess("admin", "SYNC_TABLE", "删除表记录: " + tableCode);
                } else {
                    // 尝试通过表名查找
                    List<MetadataTable> tables = tableMapper.selectAll(null);
                    for (MetadataTable t : tables) {
                        if (safeTableName.equalsIgnoreCase(t.getTableName())) {
                            // 删除该表的所有字段
                            fieldMapper.deleteByTableCode(t.getTableCode());
                            logService.logSuccess("admin", "SYNC_FIELDS", "删除表字段: " + t.getTableCode());
                            
                            // 删除表记录
                            tableMapper.deleteById(t.getId());
                            logService.logSuccess("admin", "SYNC_TABLE", "删除表记录: " + t.getTableCode());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // 清理失败不影响DROP TABLE执行结果，只记录日志
                logService.logError("admin", "CLEAN_FIELDS", "清理表字段失败: " + safeTableName, e.getMessage());
            }
        }
        
        return result;
    }

    /**
     * 执行多条SQL语句（用分号分隔）
     * @param sqls SQL语句（用分号分隔）
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeMultipleSql(String sqls) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        
        if (sqls == null || sqls.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "SQL语句不能为空");
            return result;
        }

        // 按分号分割SQL语句
        String[] sqlArray = sqls.split(";");
        int successCount = 0;
        int failCount = 0;
        
        for (String sql : sqlArray) {
            sql = sql.trim();
            if (sql.isEmpty()) {
                continue;
            }
            
            Map<String, Object> singleResult = executeSql(sql);
            singleResult.put("sql", sql);
            results.add(singleResult);
            
            if ((Boolean) singleResult.get("success")) {
                successCount++;
            } else {
                failCount++;
            }
        }
        
        result.put("success", failCount == 0);
        result.put("message", String.format("共执行%d条SQL，成功%d条，失败%d条", 
            results.size(), successCount, failCount));
        result.put("results", results);
        result.put("totalCount", results.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        
        return result;
    }

    /**
     * 从 CREATE TABLE 或 ALTER TABLE 语句中提取表名
     */
    private String extractTableName(String sql) {
        // 匹配 CREATE TABLE `table_name` 或 CREATE TABLE table_name
        // 匹配 ALTER TABLE `table_name` 或 ALTER TABLE table_name
        Pattern pattern = Pattern.compile("(?:CREATE|ALTER)\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:`)?([a-zA-Z0-9_]+)(?:`)?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 同步数据库表字段到元数据系统
     */
    private void syncTableFields(String tableName, Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        
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
                try (java.sql.PreparedStatement pstmt = connection.prepareStatement(
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
                                String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
                                if ("YES".equalsIgnoreCase(isAutoIncrement)) {
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
                tableMapper.insert(table);
                tableCreated = true;
                
                logService.logSuccess("admin", "SYNC_TABLE", "自动创建表记录: " + tableCode);
            }
        } else {
            tableCode = table.getTableCode();
        }

        // 先获取主键信息，判断是否有自增主键
        String autoIncrementPkColumn = null;
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            if (primaryKeys.next()) {
                String pkColumnName = primaryKeys.getString("COLUMN_NAME");
                // 检查该主键列是否是自增的
                try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, pkColumnName)) {
                    if (columns.next()) {
                        String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
                        if ("YES".equalsIgnoreCase(isAutoIncrement)) {
                            autoIncrementPkColumn = pkColumnName;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 获取主键信息失败，不影响字段同步，只记录日志
            logService.logError("admin", "GET_PRIMARY_KEY", "获取主键信息失败: " + tableName, e.getMessage());
        }
        
        // 使用 DatabaseMetaData 获取表结构
        Map<String, MetadataField> physicalFields = new HashMap<>();
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, null)) {
            int sort = 0;
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                int nullable = columns.getInt("NULLABLE");
                String remarks = columns.getString("REMARKS");
                String isAutoIncrement = columns.getString("IS_AUTOINCREMENT");
                
                // 构建字段类型字符串
                String fieldType = columnType;
                if (columnSize > 0 && (columnType.equals("VARCHAR") || columnType.equals("CHAR") || 
                    columnType.equals("DECIMAL") || columnType.equals("NUMERIC"))) {
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
                field.setLabel(remarks != null && !remarks.isEmpty() ? remarks : columnName);
                
                // 判断是否是自增主键
                boolean isAutoIncrementPk = (autoIncrementPkColumn != null && 
                    autoIncrementPkColumn.equalsIgnoreCase(columnName)) ||
                    "YES".equalsIgnoreCase(isAutoIncrement);
                
                if (isAutoIncrementPk) {
                    // 自增主键：不需要表单组件，对用户来说不是必填
                    field.setFormComponent(""); // 自增字段不需要表单组件
                    field.setIsRequired(0); // 对用户来说不需要填写
                } else {
                    // 普通字段：根据字段类型设置表单组件和必填状态
                    field.setIsRequired(nullable == DatabaseMetaData.columnNoNulls ? 1 : 0);
                    field.setFormComponent(getDefaultFormComponent(fieldType));
                }
                
                field.setSort(sort++);
                field.setIsEnabled(1);
                
                physicalFields.put(fieldCode, field);
            }
        }
        
        // 获取表的CHECK约束
        Map<String, String> checkConstraints = new HashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // 查询表的CHECK约束
            String checkSql = "SELECT COLUMN_NAME, CHECK_CLAUSE FROM information_schema.CHECK_CONSTRAINTS cc " +
                             "JOIN information_schema.KEY_COLUMN_USAGE kcu ON cc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME " +
                             "WHERE kcu.TABLE_SCHEMA = '" + schema + "' AND kcu.TABLE_NAME = '" + tableName + "'";
            ResultSet rs = stmt.executeQuery(checkSql);
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String checkClause = rs.getString("CHECK_CLAUSE");
                checkConstraints.put(columnName, checkClause);
            }
        } catch (Exception e) {
            // 如果查询失败，尝试使用另一种方式查询
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                // 对于某些数据库，可能需要使用不同的方式查询CHECK约束
                String checkSql = "SHOW CREATE TABLE `" + tableName + "`";
                ResultSet rs = stmt.executeQuery(checkSql);
                if (rs.next()) {
                    String createTableSql = rs.getString(2);
                    // 解析CREATE TABLE语句，提取CHECK约束
                    java.util.regex.Pattern checkPattern = java.util.regex.Pattern.compile(
                        "CHECK\\s*\\(([^\\)]+)\\)\\s*(?:,|\\))", 
                        java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.MULTILINE
                    );
                    java.util.regex.Matcher checkMatcher = checkPattern.matcher(createTableSql);
                    while (checkMatcher.find()) {
                        String checkClause = checkMatcher.group(1);
                        // 提取字段名
                        java.util.regex.Pattern fieldPattern = java.util.regex.Pattern.compile(
                            "`?([a-zA-Z0-9_]+)`?\\s*(?:REGEXP|IN|BETWEEN)",
                            java.util.regex.Pattern.CASE_INSENSITIVE
                        );
                        java.util.regex.Matcher fieldMatcher = fieldPattern.matcher(checkClause);
                        if (fieldMatcher.find()) {
                            String columnName = fieldMatcher.group(1);
                            checkConstraints.put(columnName, "CHECK (" + checkClause + ")");
                        }
                    }
                }
            } catch (Exception ex) {
                // 如果两种方式都失败，记录日志并继续，不影响其他操作
                logService.logError("admin", "GET_CHECK_CONSTRAINTS", "获取表CHECK约束失败: " + tableName, ex.getMessage());
            }
        }
        
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
            if (checkConstraints.containsKey(columnName)) {
                String checkConstraint = checkConstraints.get(columnName);
                String validateRule = parseCheckConstraint(checkConstraint);
                if (validateRule != null) {
                    physicalField.setValidateRule(validateRule);
                }
            }
            
            if (existingFieldMap.containsKey(fieldCode)) {
                // 字段已存在，更新字段信息
                MetadataField existingField = existingFieldMap.get(fieldCode);
                
                // 更新字段信息（只更新物理表相关的字段，保留用户手动配置的其他字段）
                existingField.setFieldName(physicalField.getFieldName());
                existingField.setFieldType(physicalField.getFieldType());
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
                
                // 只有当validateRule为空时才设置，避免覆盖用户手动配置的校验规则
                if ((existingField.getValidateRule() == null || existingField.getValidateRule().isEmpty()) && 
                    physicalField.getValidateRule() != null && !physicalField.getValidateRule().isEmpty()) {
                    existingField.setValidateRule(physicalField.getValidateRule());
                }
                
                fieldMapper.update(existingField);
                logService.logSuccess("admin", "SYNC_FIELDS", "更新字段: " + tableCode + "." + fieldCode);
            } else {
                // 字段不存在，添加新字段
                fieldMapper.insert(physicalField);
                logService.logSuccess("admin", "SYNC_FIELDS", "添加字段: " + tableCode + "." + fieldCode);
            }
        }
        
        // 2. 删除物理表中不存在的字段
        for (MetadataField existingField : existingFields) {
            String fieldCode = existingField.getFieldCode();
            if (!physicalFields.containsKey(fieldCode)) {
                // 物理表中不存在该字段，删除元数据中的字段
                fieldMapper.deleteById(existingField.getId());
                logService.logSuccess("admin", "SYNC_FIELDS", "删除字段: " + tableCode + "." + fieldCode);
            }
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
     * 同步数据库表外键到元数据关联关系系统（公共方法，供外部调用）
     * @param tableCode 表编码，如果为null则同步所有表
     * @return 同步结果
     */
    @Transactional
    public Map<String, Object> syncForeignKeys(String tableCode) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int totalCreated = 0; // 总共创建的关联关系记录数
        List<String> messages = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            if (tableCode != null && !tableCode.trim().isEmpty()) {
                // 同步指定表
                // 尝试多种表名格式：先尝试去掉_TABLE，再尝试直接转小写
                String tableName1 = convertToTableName(tableCode);
                String tableName2 = tableCode.toLowerCase();
                
                try {
                    // 先尝试去掉_TABLE的格式
                    int created = syncTableForeignKeys(tableName1, connection);
                    successCount = 1;
                    totalCreated += created;
                    messages.add("表 " + tableCode + " 的外键同步成功，创建 " + created + " 条关联关系");
                } catch (Exception e1) {
                    // 如果失败，尝试直接转小写的格式
                    try {
                        int created = syncTableForeignKeys(tableName2, connection);
                        successCount = 1;
                        totalCreated += created;
                        messages.add("表 " + tableCode + " 的外键同步成功，创建 " + created + " 条关联关系");
                    } catch (Exception e2) {
                        // 两种格式都失败
                        failCount = 1;
                        messages.add("表 " + tableCode + " 的外键同步失败: " + e2.getMessage());
                        logService.logError("admin", "SYNC_FOREIGN_KEY", "同步外键失败: " + tableCode, e2.getMessage());
                    }
                }
            } else {
                // 同步所有表
                List<MetadataTable> tables = tableMapper.selectAll(null);
                for (MetadataTable table : tables) {
                    try {
                        // 尝试多种表名格式：先尝试去掉_TABLE，再尝试直接转小写
                        String tableName1 = convertToTableName(table.getTableCode());
                        String tableName2 = table.getTableCode().toLowerCase();
                        
                        // 先尝试去掉_TABLE的格式
                        try {
                            int created = syncTableForeignKeys(tableName1, connection);
                            successCount++;
                            totalCreated += created;
                        } catch (Exception e1) {
                            // 如果失败，尝试直接转小写的格式
                            try {
                                int created = syncTableForeignKeys(tableName2, connection);
                                successCount++;
                                totalCreated += created;
                            } catch (Exception e2) {
                                // 两种格式都失败
                                failCount++;
                                messages.add("表 " + table.getTableCode() + " 的外键同步失败: " + e2.getMessage());
                                logService.logError("admin", "SYNC_FOREIGN_KEY", "同步外键失败: " + table.getTableCode(), e2.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        failCount++;
                        messages.add("表 " + table.getTableCode() + " 的外键同步失败: " + e.getMessage());
                        logService.logError("admin", "SYNC_FOREIGN_KEY", "同步外键失败: " + table.getTableCode(), e.getMessage());
                    }
                }
                if (successCount > 0) {
                    messages.add(0, "成功同步 " + successCount + " 个表的外键，共创建 " + totalCreated + " 条关联关系");
                }
            }
            
            result.put("success", failCount == 0);
            result.put("message", String.join("; ", messages));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCreated", totalCreated); // 返回创建的记录数
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "同步外键失败: " + e.getMessage());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCreated", totalCreated);
            logService.logError("admin", "SYNC_FOREIGN_KEY", "同步外键失败", e.getMessage());
        }
        
        return result;
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    private String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        return code.toLowerCase().replace("_TABLE", "");
    }

    /**
     * 同步数据库表外键到元数据关联关系系统（内部方法）
     * @return 创建的关联关系记录数
     */
    private int syncTableForeignKeys(String tableName, Connection connection) throws Exception {
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
                
                // 检查关联关系是否已存在
                if (relationMapper.countByCode(relationCode) > 0) {
                    // 已存在，跳过
                    continue;
                }
                
                // 创建关联关系
                MetadataTableRelation relation = new MetadataTableRelation();
                relation.setRelationCode(relationCode);
                relation.setMainTableCode(mainTableCode);
                relation.setSlaveTableCode(slaveTableCode);
                relation.setMainFieldCode(mainField.getFieldCode());
                relation.setSlaveFieldCode(slaveField.getFieldCode());
                relation.setRelationType("ONE_TO_MANY"); // 默认一对多
                relation.setRelationName(mainTable.getTableName() + " -> " + table.getTableName());
                
                relationMapper.insert(relation);
                createdCount++;
                logService.logSuccess("admin", "SYNC_FOREIGN_KEY", "自动创建外键关联关系: " + relationCode);
            }
        } catch (Exception e) {
            // 同步外键失败不影响其他操作，只记录日志
            logService.logError("admin", "SYNC_FOREIGN_KEY", "同步外键关联关系失败: " + tableName, e.getMessage());
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
        String simpleCode = "REL_" + mainTableCode + "_" + slaveTableCode + "_" + 
                           mainFieldCode + "_" + slaveFieldCode;
        
        // 如果不超过50个字符，直接返回
        if (simpleCode.length() <= 50) {
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
            String hash = hexString.toString().substring(0, Math.min(42, hexString.length()));
            return "REL_" + hash.toUpperCase();
        } catch (Exception e) {
            // 如果哈希生成失败，使用截断的方式（不推荐，但作为后备方案）
            return simpleCode.substring(0, 50);
        }
    }
    
    /**
     * 解析CHECK约束字符串，转换为JSON格式的校验规则
     * @param checkConstraint CHECK约束字符串
     * @return JSON格式的校验规则
     */
    private String parseCheckConstraint(String checkConstraint) {
        if (checkConstraint == null || checkConstraint.trim().isEmpty()) {
            return null;
        }
        
        String constraint = checkConstraint.trim();
        
        // 正则表达式约束：CHECK (field REGEXP 'pattern')
        String regexPattern = "^CHECK \\s*\\([^\\)]*REGEXP \\s*'([^']+)'\\)";
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(regexPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = regex.matcher(constraint);
        if (matcher.find()) {
            String pattern = matcher.group(1);
            return "{\"pattern\": \"" + pattern.replace("\\", "\\\\") + ", \"message\": \"\"}";
        }
        
        // BETWEEN AND约束：CHECK (field BETWEEN min AND max)
        String betweenPattern = "^CHECK \\s*\\([^\\)]*BETWEEN \\s+([0-9]+) \\s+AND \\s+([0-9]+)\\)";
        regex = java.util.regex.Pattern.compile(betweenPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = regex.matcher(constraint);
        if (matcher.find()) {
            String min = matcher.group(1);
            String max = matcher.group(2);
            return "{\"min\": " + min + ", \"max\": " + max + ", \"message\": \"\"}";
        }
        
        // IN约束：CHECK (field IN ('value1', 'value2', ...))
        String inPattern = "^CHECK \\s*\\([^\\)]*IN \\s*\\(([^\\)]+)\\)\\)";
        regex = java.util.regex.Pattern.compile(inPattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = regex.matcher(constraint);
        if (matcher.find()) {
            String valuesStr = matcher.group(1);
            // 处理值列表，去除单引号和空格
            String[] values = valuesStr.split(",");
            StringBuilder options = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                String value = values[i].trim().replace("'", "");
                if (i > 0) {
                    options.append(", ");
                }
                options.append("\"").append(value).append("\"");
            }
            return "{\"options\": [" + options.toString() + "], \"message\": \"\"}";
        }
        
        return null;
    }
}

