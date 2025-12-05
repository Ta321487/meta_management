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
import com.alibaba.fastjson2.JSONObject;

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
        
        // 移除多行注释 /* */
        sql = sql.replaceAll("/\\*[\\s\\S]*?\\*/", " ");
        // 移除单行注释 --
        Pattern lineCommentPattern = Pattern.compile("--.*$\\n?", Pattern.MULTILINE);
        sql = lineCommentPattern.matcher(sql).replaceAll("");
        // 移除多余的空白字符
        sql = sql.replaceAll("\\s+", " ").trim();
        
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
                        logService.logSuccess("admin", "SYNC_START", "开始同步元数据: " + sql.substring(0, Math.min(100, sql.length())));
                        String tableName = extractTableName(sql);
                        logService.logSuccess("admin", "SYNC_TABLE_NAME", "提取到表名: " + tableName);
                        if (tableName != null) {
                            syncTableFields(tableName, connection);
                            // 同步外键关联关系
                            syncTableForeignKeys(tableName, connection);
                            logService.logSuccess("admin", "SYNC_COMPLETE", "元数据同步完成: " + tableName);
                        } else {
                            logService.logError("admin", "SYNC_FAILED", "表名提取失败，无法同步元数据", sql);
                        }
                    } catch (Exception e) {
                        // 同步失败不影响 SQL 执行结果，只记录日志
                        String tableName = extractTableName(sql);
                        logService.logError("admin", "SYNC_FIELDS", "同步表字段失败: " + (tableName != null ? tableName : "未知表"), e.getMessage() + "，SQL: " + sql);
                        logService.logError("admin", "SYNC_EXCEPTION", "同步异常详情", e.toString());
                    }
                }
                
                logService.logSuccess("admin", "SQL_EXECUTE", "执行SQL: " + sql.substring(0, Math.min(100, sql.length())) + "，影响行数: " + affectedRows);
            }
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            String simpleClassName = e.getClass().getSimpleName();
            
            // 改进错误信息，特别是对于ALTER TABLE语句和CHECK约束违反的情况
            if (upperSql.startsWith("ALTER TABLE")) {
                if (errorMessage.contains("Check constraint") || errorMessage.contains("check constraint")) {
                    // CHECK约束违反
                    errorMessage = "添加CHECK约束失败: 现有数据不符合约束条件。建议先添加允许NULL的字段，更新数据后再添加约束，或使用默认值确保符合约束。";
                } else if (errorMessage.contains("NOT NULL") || errorMessage.contains("not null")) {
                    // NOT NULL约束违反
                    errorMessage = "添加NOT NULL约束失败: 现有数据中存在NULL值。建议先添加允许NULL的字段，更新数据后再修改为NOT NULL，或使用默认值。";
                } else {
                    // 其他ALTER TABLE错误
                    errorMessage = "执行ALTER TABLE语句失败: " + e.getMessage();
                }
            } else {
                // 其他SQL错误
                errorMessage = "SQL执行失败: " + e.getMessage();
            }
            
            result.put("success", false);
            result.put("message", errorMessage);
            result.put("error", simpleClassName);
            result.put("originalError", e.getMessage()); // 保留原始错误信息，便于调试
            
            logService.logError("admin", "SQL_EXECUTE", "执行SQL失败", e.getMessage());
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
        Pattern pattern = Pattern.compile("(?:CREATE|ALTER)\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([^\s`]+)`?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);
        String matchedTableName = null;
        if (matcher.find()) {
            matchedTableName = matcher.group(1);
        }
        // 记录匹配结果
        logService.logSuccess("admin", "TABLE_NAME_MATCH", "表名匹配结果: 原始SQL=" + sql.substring(0, Math.min(50, sql.length())) + "..., 匹配到表名=" + matchedTableName);
        return matchedTableName;
    }

    /**
     * 同步数据库表字段到元数据系统
     */
    private void syncTableFields(String tableName, Connection connection) throws Exception {
        logService.logSuccess("admin", "SYNC_TABLE_FIELDS_START", "开始同步表字段: " + tableName);
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
                table.setIsEnabled(1); // 设置默认启用状态
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
        
        // 使用 SHOW COLUMNS 获取表结构（直接查询数据库，确保获取最新字段信息）
        Map<String, MetadataField> physicalFields = new HashMap<>();
        try (Statement stmt = connection.createStatement()) {
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
                Pattern typePattern = Pattern.compile("^(\\\\w+)(?:\\\\((\\\\d+)\\\\))?");
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
                
                // 判断是否是自增主键
                boolean isAutoIncrementPk = (autoIncrementPkColumn != null && 
                    autoIncrementPkColumn.equalsIgnoreCase(columnName)) ||
                    (extra != null && extra.contains("auto_increment"));
                
                if (isAutoIncrementPk) {
                    // 自增主键：不需要表单组件，对用户来说不是必填
                    field.setFormComponent(""); // 自增字段不需要表单组件
                    field.setIsRequired(0); // 对用户来说不需要填写
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
            logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "开始获取表CHECK约束: " + tableName);
            
            // 1. 直接使用SHOW CREATE TABLE获取表结构，这是最可靠的方式
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                String showCreateSql = "SHOW CREATE TABLE `" + tableName + "`";
                logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "执行SQL: " + showCreateSql);
                ResultSet rs = stmt.executeQuery(showCreateSql);
                if (rs.next()) {
                    String createTableSql = rs.getString(2);
                    logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "SHOW CREATE TABLE结果: " + createTableSql);
                    
                    // 解析CREATE TABLE语句，提取CHECK约束
                    // 匹配列级CHECK约束：column_name type CHECK (constraint)
                    Pattern columnCheckPattern = Pattern.compile(
                        "`?([a-zA-Z0-9_]+)`?\\s+[^,]+\\s+CHECK\\s*\\(([^\\)]+)\\)",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
                    );
                    Matcher columnCheckMatcher = columnCheckPattern.matcher(createTableSql);
                    int columnCheckCount = 0;
                    while (columnCheckMatcher.find()) {
                        String columnName = columnCheckMatcher.group(1);
                        String checkClause = columnCheckMatcher.group(2);
                        String fullConstraint = "CHECK (" + checkClause + ")";
                        checkConstraints.put(columnName.toUpperCase(), fullConstraint);
                        logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "提取列级CHECK约束: " + columnName + " -> " + fullConstraint);
                        columnCheckCount++;
                    }
                    logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "提取到" + columnCheckCount + "个列级CHECK约束");
                    
                    // 匹配表级CHECK约束：支持两种格式
                    // 格式1：CHECK (constraint)
                    // 格式2：CONSTRAINT constraint_name CHECK (...)
                    // 使用更健壮的正则表达式处理嵌套括号
                    String createTableStr = createTableSql;
                    int checkStartIndex = createTableStr.indexOf("CHECK");
                    int checkCount = 0;
                    
                    while (checkStartIndex != -1) {
                        // 找到CHECK关键字后的第一个左括号
                        int openParenIndex = createTableStr.indexOf("(", checkStartIndex);
                        if (openParenIndex == -1) {
                            break;
                        }
                        
                        // 计算匹配的右括号位置
                        int closeParenIndex = findMatchingCloseParen(createTableStr, openParenIndex);
                        if (closeParenIndex == -1) {
                            break;
                        }
                        
                        // 提取完整的CHECK约束
                        String fullCheckConstraint = createTableStr.substring(checkStartIndex, closeParenIndex + 1);
                        logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "提取到完整CHECK约束: " + fullCheckConstraint);
                        
                        // 提取CHECK约束子句（括号内的内容）
                        String checkClause = createTableStr.substring(openParenIndex + 1, closeParenIndex);
                        logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "提取到CHECK约束子句: " + checkClause);
                        
                        // 从约束条件中提取字段名
                        String columnName = extractFieldNameFromConstraint(checkClause);
                        
                        if (columnName != null && !columnName.isEmpty()) {
                            checkConstraints.put(columnName.toUpperCase(), fullCheckConstraint);
                            logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "提取表级CHECK约束: " + columnName + " -> " + fullCheckConstraint);
                            checkCount++;
                        } else {
                            logService.logError("admin", "GET_CHECK_CONSTRAINTS", "无法从表级约束中提取字段名", checkClause);
                        }
                        
                        // 继续查找下一个CHECK约束
                        checkStartIndex = createTableStr.indexOf("CHECK", closeParenIndex);
                    }
                    logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "提取到" + checkCount + "个表级CHECK约束");
                }
            } catch (Exception e) {
                logService.logError("admin", "GET_CHECK_CONSTRAINTS", "使用SHOW CREATE TABLE获取CHECK约束失败", e.getMessage());
                
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
                                     "AND tc.CONSTRAINT_TYPE = 'CHECK'";
                    logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "执行SQL: " + checkSql);
                    ResultSet rs = stmt.executeQuery(checkSql);
                    int count1 = 0;
                    while (rs.next()) {
                        // 移除未使用的变量 constraintName
                        String checkClause = rs.getString("CHECK_CLAUSE");
                        logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "从information_schema提取到CHECK约束子句: " + checkClause);
                        
                        // 从约束条件中提取字段名
                        String columnName = extractFieldNameFromConstraint(checkClause);
                        
                        if (columnName != null && !columnName.isEmpty()) {
                            checkConstraints.put(columnName.toUpperCase(), "CHECK (" + checkClause + ")");
                            logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "从information_schema提取CHECK约束: " + columnName + " -> " + checkClause);
                            count1++;
                        } else {
                            logService.logError("admin", "GET_CHECK_CONSTRAINTS", "无法从information_schema的CHECK约束中提取字段名", checkClause);
                        }
                    }
                    logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "从information_schema获取到" + count1 + "个CHECK约束");
                } catch (Exception ex) {
                    logService.logError("admin", "GET_CHECK_CONSTRAINTS", "从information_schema获取CHECK约束失败", ex.getMessage());
                }
            }
        } catch (Exception e) {
            // 最外层异常捕获，确保CHECK约束处理失败不会影响字段同步
            logService.logError("admin", "GET_CHECK_CONSTRAINTS", "处理CHECK约束时发生异常", e.getMessage());
        }
        
        logService.logSuccess("admin", "GET_CHECK_CONSTRAINTS", "总共获取到" + checkConstraints.size() + "个CHECK约束: " + checkConstraints);
        
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
                    logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析CHECK约束: " + checkConstraint);
                    String validateRule = parseCheckConstraint(checkConstraint);
                    if (validateRule != null) {
                        physicalField.setValidateRule(validateRule);
                        logService.logSuccess("admin", "SET_VALIDATE_RULE", "设置校验规则: " + columnName + " -> " + validateRule);
                        
                        // 检查是否为IN约束，如果是则设置字段类型为enum，表单组件为select
                        try {
                            com.alibaba.fastjson2.JSONObject validateJson = com.alibaba.fastjson2.JSON.parseObject(validateRule);
                            if ("IN".equals(validateJson.getString("operator"))) {
                                physicalField.setFieldType("enum");
                                physicalField.setFormComponent("select");
                                logService.logSuccess("admin", "SET_ENUM_TYPE", "设置字段为ENUM类型和select组件: " + columnName);
                            }
                        } catch (Exception e) {
                            // 解析校验规则失败，忽略
                        }
                    }
                } catch (Exception e) {
                    // 解析CHECK约束失败，记录日志但不影响字段同步
                    logService.logError("admin", "PARSE_CHECK_CONSTRAINT", "解析CHECK约束失败: " + columnName, e.getMessage());
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
                
                // 更新校验规则：无论现有规则是否为空，都同步物理表的CHECK约束
                // 这样可以确保物理表的CHECK约束能正确同步到元数据系统
                if (physicalField.getValidateRule() != null && !physicalField.getValidateRule().isEmpty()) {
                    String oldRule = existingField.getValidateRule();
                    String newRule = physicalField.getValidateRule();
                    
                    // 合并原始message字段和处理IN约束
                    if (oldRule != null && !oldRule.isEmpty()) {
                        try {
                            com.alibaba.fastjson2.JSONObject oldJson = com.alibaba.fastjson2.JSON.parseObject(oldRule);
                            com.alibaba.fastjson2.JSONObject newJson = com.alibaba.fastjson2.JSON.parseObject(newRule);
                            
                            // 如果原始规则有message字段，保留它
                            if (oldJson.containsKey("message")) {
                                String message = oldJson.getString("message");
                                if (message != null && !message.isEmpty()) {
                                    newJson.put("message", message);
                                }
                            }
                            
                            // 如果是IN约束，确保options与values一致
                            if ("IN".equals(newJson.getString("operator"))) {
                                // 使用values作为options，确保一致性
                                com.alibaba.fastjson2.JSONArray values = newJson.getJSONArray("values");
                                newJson.put("options", values);
                            }
                            
                            newRule = newJson.toJSONString();
                        } catch (Exception e) {
                            // 解析失败，使用新规则
                        }
                    }
                    
                    existingField.setValidateRule(newRule);
                    
                    // 检查是否为IN约束，如果是则设置字段类型为enum，表单组件为select
                    try {
                        com.alibaba.fastjson2.JSONObject validateJson = com.alibaba.fastjson2.JSON.parseObject(newRule);
                        if ("IN".equals(validateJson.getString("operator"))) {
                            existingField.setFieldType("enum");
                            existingField.setFormComponent("select");
                            logService.logSuccess("admin", "SET_ENUM_TYPE", "更新字段为ENUM类型和select组件: " + tableCode + "." + fieldCode);
                        }
                    } catch (Exception e) {
                        // 解析校验规则失败，忽略
                    }
                    
                    if (oldRule != null && !oldRule.isEmpty() && !oldRule.equals(newRule)) {
                        logService.logSuccess("admin", "SET_VALIDATE_RULE", "覆盖现有校验规则: " + 
                            tableCode + "." + fieldCode + " -> 旧规则: " + oldRule + ", 新规则: " + newRule);
                    } else {
                        logService.logSuccess("admin", "SET_VALIDATE_RULE", "设置校验规则: " + 
                            tableCode + "." + fieldCode + " -> " + newRule);
                    }
                } else {
                    // 如果解析后的校验规则为空，清空现有校验规则
                    if (existingField.getValidateRule() != null && !existingField.getValidateRule().isEmpty()) {
                        existingField.setValidateRule(null);
                        logService.logSuccess("admin", "SET_VALIDATE_RULE", "清空校验规则: " + tableCode + "." + fieldCode);
                    }
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
        logService.logSuccess("admin", "SYNC_TABLE_FIELDS_END", "表字段同步完成: " + tableName);
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
            logService.logError("admin", "PARSE_CHECK_CONSTRAINT", "CHECK约束为空", "");
            return null;
        }
        
        String constraint = checkConstraint.trim();
        logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "开始解析CHECK约束: " + constraint);
        
        // 提取字段名
        String fieldName = extractFieldNameFromConstraint(constraint);
        logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "提取到字段名: " + fieldName);
        
        // 1. 正则表达式约束：支持 regexp_like 语法，如 regexp_like(`name`,_utf8mb4'^[A-Za-z]+$')
        Pattern regexPattern = Pattern.compile(
            "regexp_like\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*,\\s*(?:_utf8mb4)?'([^']+)'\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regexMatcher = regexPattern.matcher(constraint);
        if (regexMatcher.find()) {
            String pattern = regexMatcher.group(2);
            if (pattern != null) {
                // 生成正则约束JSON
                String json = String.format("{\"pattern\":\"%s\",\"message\":\"\"}", 
                    pattern.replace("\\", "\\\\"));
                logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为正则约束: " + json);
                return json;
            }
        }
        
        // 2. 支持直接使用 REGEXP 关键字的格式，如 STU_CODE REGEXP '^\\d{10}$'
        Pattern regExpKeywordPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+REGEXP\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regExpKeywordMatcher = regExpKeywordPattern.matcher(constraint);
        if (regExpKeywordMatcher.find()) {
            String pattern = regExpKeywordMatcher.group(2);
            if (pattern != null) {
                // 生成正则约束JSON
                String json = String.format("{\"pattern\":\"%s\",\"message\":\"\"}", 
                    pattern.replace("\\", "\\\\"));
                logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为REGEXP关键字正则约束: " + json);
                return json;
            }
        }
        
        // 3. BETWEEN AND约束：支持 ((`age` between 18 and 60)) 格式，匹配小写的between和and
        Pattern betweenPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+between\\s+([0-9]+)\\s+and\\s+([0-9]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher betweenMatcher = betweenPattern.matcher(constraint);
        if (betweenMatcher.find()) {
            String min = betweenMatcher.group(2);
            String max = betweenMatcher.group(3);
            // 生成BETWEEN约束JSON
            String json = String.format("{\"min\":\"%s\",\"max\":\"%s\",\"message\":\"\"}",
                min, max);
            logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为BETWEEN约束: " + json);
            return json;
        }
        
        // 4. IN约束：支持 ((`status` in (_gbk'active',_gbk'inactive',_gbk'pending'))) 格式，匹配小写的in
        Pattern inPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+in\\s*\\(([^\\)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher inMatcher = inPattern.matcher(constraint);
        if (inMatcher.find()) {
            String valuesStr = inMatcher.group(2);
            logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "提取IN值列表: " + valuesStr);
            // 解析IN值列表，处理字符集前缀如_gbk'active'
            List<String> valuesList = parseInValues(valuesStr);
            logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析后的值列表: " + valuesList);
            if (!valuesList.isEmpty()) {
                // 生成IN约束JSON，包含options字段
                StringBuilder valuesJson = new StringBuilder();
                for (int i = 0; i < valuesList.size(); i++) {
                    if (i > 0) {
                        valuesJson.append(",");
                    }
                    valuesJson.append("\"").append(valuesList.get(i)).append("\"");
                }
                String json = String.format("{\"operator\":\"IN\",\"values\":[%s],\"options\":[%s]}",
                    valuesJson.toString(), valuesJson.toString());
                logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为IN约束: " + json);
                return json;
            }
        }
        
        // 5. 数值范围约束：支持 >、<、>=、<=，如 ((`age` >= 18)) 或 ((`salary` < 10000))
        Pattern rangePattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s*([><]=?|<=?|>=?)\\s*([0-9]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher rangeMatcher = rangePattern.matcher(constraint);
        if (rangeMatcher.find()) {
            String operator = rangeMatcher.group(2);
            String value = rangeMatcher.group(3);
            com.alibaba.fastjson2.JSONObject jsonObj = new JSONObject();
            jsonObj.put("message", "");
            
            int intValue = Integer.parseInt(value);
            
            // 处理各种运算符
            switch (operator) {
                case ">":
                case ">=" :
                    jsonObj.put("min", intValue);
                    break;
                case "<":
                case "<=" :
                    jsonObj.put("max", intValue);
                    break;
                default:
                    // 未知运算符，记录日志
                    logService.logError("admin", "PARSE_CHECK_CONSTRAINT", "未知的数值范围运算符", operator);
                    return null;
            }
            
            String json = jsonObj.toJSONString();
            logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为数值范围约束: " + json);
            return json;
        }
        
        // 6. 等于/不等于约束：支持 =、!=，如 ((`status` = 'active')) 或 ((`type` != 'admin'))
        Pattern equalPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s*(!?=)\\s*'?([^']+)'?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher equalMatcher = equalPattern.matcher(constraint);
        if (equalMatcher.find()) {
            String operator = equalMatcher.group(2);
            String value = equalMatcher.group(3);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("message", "");
            
            if ("=".equals(operator)) {
                jsonObj.put("operator", "=");
                jsonObj.put("value", value);
            } else if ("!=".equals(operator)) {
                jsonObj.put("operator", "!");
                jsonObj.put("value", value);
            }
            
            String json = jsonObj.toJSONString();
            logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为等于/不等于约束: " + json);
            return json;
        }
        
        // 7. LIKE/RLIKE约束：支持 LIKE 'pattern' 或 RLIKE 'pattern'
        Pattern likePattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+(LIKE|RLIKE)\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        Matcher likeMatcher = likePattern.matcher(constraint);
        if (likeMatcher.find()) {
            String likeOperator = likeMatcher.group(2);
            String pattern = likeMatcher.group(3);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("message", "");
            
            if ("RLIKE".equalsIgnoreCase(likeOperator)) {
                // RLIKE等价于正则表达式
                jsonObj.put("pattern", pattern);
            } else if ("LIKE".equalsIgnoreCase(likeOperator)) {
                // LIKE转换为正则表达式
                String regexPatternStr = pattern.replace("%", ".*");
                jsonObj.put("pattern", regexPatternStr);
            }
            
            String json = jsonObj.toJSONString();
            logService.logSuccess("admin", "PARSE_CHECK_CONSTRAINT", "解析为LIKE/RLIKE约束: " + json);
            return json;
        }
        
        logService.logError("admin", "PARSE_CHECK_CONSTRAINT", "无法解析CHECK约束", constraint);
        return null;
    }
    
    /**
     * 从CHECK约束中提取字段名
     * @param constraint CHECK约束字符串
     * @return 字段名
     */
    private String extractFieldNameFromConstraint(String constraint) {
        if (constraint == null || constraint.trim().isEmpty()) {
            return "";
        }
        
        String trimmedConstraint = constraint.trim();
        
        // 1. 首先尝试匹配regexp_like函数格式：regexp_like(`field`, '_utf8mb4^[A-Za-z]+$')
        Pattern regexpLikePattern = Pattern.compile(
            "regexp_like\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*,",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regexpLikeMatcher = regexpLikePattern.matcher(trimmedConstraint);
        if (regexpLikeMatcher.find()) {
            return regexpLikeMatcher.group(1);
        }
        
        // 2. 尝试匹配带括号的字段格式：((`field` between 18 and 60)) 或 ((`field` in (value1, value2)))
        Pattern parenthesisFieldPattern = Pattern.compile(
            "\\(\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*",
            Pattern.CASE_INSENSITIVE
        );
        Matcher parenthesisFieldMatcher = parenthesisFieldPattern.matcher(trimmedConstraint);
        if (parenthesisFieldMatcher.find()) {
            return parenthesisFieldMatcher.group(1);
        }
        
        // 3. 尝试匹配简单字段格式：`field` between 18 and 60 或 `field` in (value1, value2)
        Pattern simpleFieldPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+(?:between|in|REGEXP|IN|BETWEEN|=|>|<|>=|<=|!=|LIKE|RLIKE)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher simpleFieldMatcher = simpleFieldPattern.matcher(trimmedConstraint);
        if (simpleFieldMatcher.find()) {
            return simpleFieldMatcher.group(1);
        }
        
        logService.logError("admin", "EXTRACT_FIELD_NAME", "无法从约束中提取字段名", constraint);
        return "";
    }
    
    /**
     * 查找匹配的右括号位置
     * @param str 输入字符串
     * @param openParenIndex 左括号位置
     * @return 匹配的右括号位置，未找到返回-1
     */
    private int findMatchingCloseParen(String str, int openParenIndex) {
        if (openParenIndex < 0 || openParenIndex >= str.length() || str.charAt(openParenIndex) != '(') {
            return -1;
        }
        
        int parenCount = 1;
        for (int i = openParenIndex + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
                if (parenCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 解析IN约束中的值列表
     * @param valuesStr 值列表字符串
     * @return 解析后的值列表
     */
    private List<String> parseInValues(String valuesStr) {
        List<String> values = new ArrayList<>();
        
        // 去除前后空格
        valuesStr = valuesStr.trim();
        if (valuesStr.isEmpty()) {
            return values;
        }
        
        // 使用状态机解析值列表，处理引号和逗号
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\'';
        
        for (char c : valuesStr.toCharArray()) {
            if (c == '\'' || c == '"') {
                // 处理引号
                if (inQuotes) {
                    if (c == quoteChar) {
                        // 引号结束
                        inQuotes = false;
                    } else {
                        // 引号内的其他引号，作为普通字符处理
                        currentValue.append(c);
                    }
                } else {
                    // 引号开始
                    inQuotes = true;
                    quoteChar = c;
                }
            } else if (c == ',' && !inQuotes) {
                // 逗号分隔符，且不在引号内
                String value = currentValue.toString().trim();
                if (!value.isEmpty()) {
                    values.add(value);
                }
                currentValue.setLength(0);
            } else if (!Character.isWhitespace(c) || inQuotes) {
                // 普通字符，或引号内的空格
                currentValue.append(c);
            }
        }
        
        // 添加最后一个值
        String lastValue = currentValue.toString().trim();
        if (!lastValue.isEmpty()) {
            values.add(lastValue);
        }
        
        // 处理每个值，去除可能的_utf8mb4前缀
        List<String> processedValues = new ArrayList<>();
        for (String value : values) {
            // 去除_utf8mb4前缀
            value = value.replaceFirst("^_utf8mb4", "");
            // 去除前后引号
            if ((value.startsWith("'") && value.endsWith("'")) ||
                (value.startsWith("\"") && value.endsWith("\""))) {
                value = value.substring(1, value.length() - 1);
            }
            processedValues.add(value);
        }
        
        return processedValues;
    }
}

