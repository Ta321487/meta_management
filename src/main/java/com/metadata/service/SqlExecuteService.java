package com.metadata.service;

import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataTableMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
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

    /**
     * 执行SQL语句
     * @param sql SQL语句
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> executeSql(String sql) {
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
        if (upperSql.startsWith("DROP") || upperSql.startsWith("TRUNCATE") || 
            upperSql.startsWith("DELETE FROM") || upperSql.startsWith("ALTER TABLE DROP")) {
            result.put("success", false);
            result.put("message", "禁止执行DROP、TRUNCATE、DELETE等危险操作");
            return result;
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // 判断是否为查询语句
            boolean isQuery = upperSql.startsWith("SELECT") || upperSql.startsWith("SHOW") || 
                            upperSql.startsWith("DESC") || upperSql.startsWith("DESCRIBE");
            
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
                
                // 如果是 CREATE TABLE 语句，自动同步字段到元数据系统
                if (upperSql.startsWith("CREATE TABLE")) {
                    try {
                        String tableName = extractTableName(sql);
                        if (tableName != null) {
                            syncTableFields(tableName, connection);
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
     * 从 CREATE TABLE 语句中提取表名
     */
    private String extractTableName(String sql) {
        // 匹配 CREATE TABLE `table_name` 或 CREATE TABLE table_name
        Pattern pattern = Pattern.compile("CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:`)?([a-zA-Z0-9_]+)(?:`)?", Pattern.CASE_INSENSITIVE);
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
        // 查找对应的表编码（通过表名匹配，表名可能是表编码或实际表名）
        MetadataTable table = tableMapper.selectByCode(tableName);
        if (table == null) {
            // 如果表编码不存在，尝试通过表名查找
            List<MetadataTable> tables = tableMapper.selectAll(null);
            for (MetadataTable t : tables) {
                // 这里可以根据实际情况调整匹配逻辑
                // 如果表名就是表编码，或者有其他映射关系
                if (tableName.equalsIgnoreCase(t.getTableCode()) || 
                    tableName.equalsIgnoreCase(t.getTableName())) {
                    table = t;
                    break;
                }
            }
        }
        
        if (table == null) {
            // 如果找不到对应的表编码，无法同步字段
            return;
        }

        String tableCode = table.getTableCode();
        
        // 检查是否已有字段，如果有则跳过（避免重复同步）
        List<MetadataField> existingFields = fieldMapper.selectByTableCode(tableCode);
        if (!existingFields.isEmpty()) {
            // 已有字段，可以选择更新或跳过
            // 这里选择跳过，避免覆盖用户手动配置的字段
            return;
        }

        // 使用 DatabaseMetaData 获取表结构
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        
        try (ResultSet columns = metaData.getColumns(catalog, schema, tableName, null)) {
            int sort = 0;
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String columnType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                int nullable = columns.getInt("NULLABLE");
                String remarks = columns.getString("REMARKS");
                
                // 构建字段类型字符串
                String fieldType = columnType;
                if (columnSize > 0 && (columnType.equals("VARCHAR") || columnType.equals("CHAR") || 
                    columnType.equals("DECIMAL") || columnType.equals("NUMERIC"))) {
                    fieldType = columnType + "(" + columnSize + ")";
                }
                
                // 生成字段编码（使用列名的大写形式）
                String fieldCode = columnName.toUpperCase();
                
                // 检查字段是否已存在
                if (fieldMapper.countByCode(tableCode, fieldCode) > 0) {
                    continue;
                }
                
                // 创建字段对象
                MetadataField field = new MetadataField();
                field.setFieldCode(fieldCode);
                field.setTableCode(tableCode);
                field.setFieldName(columnName);
                field.setFieldType(fieldType);
                field.setLabel(remarks != null && !remarks.isEmpty() ? remarks : columnName);
                field.setIsRequired(nullable == DatabaseMetaData.columnNoNulls ? 1 : 0);
                field.setFormComponent(getDefaultFormComponent(fieldType));
                field.setSort(sort++);
                
                // 插入字段
                fieldMapper.insert(field);
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
            return "date";
        } else if (upperType.contains("TEXT")) {
            return "textarea";
        } else {
            return "input";
        }
    }
}

