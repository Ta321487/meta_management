package com.metadata.service.impl;

import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataBusinessSystemMapper;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.service.OperationLogService;
import com.metadata.service.SqlExecuteService;
import com.metadata.service.MetadataSyncService;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.service.constant.SqlConstants;
import com.metadata.service.dto.SqlResult;
import com.metadata.service.strategy.SqlTypeStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL执行服务实现
 */
@Service
public class SqlExecuteServiceImpl implements SqlExecuteService {

    private static final Pattern DROP_DATABASE_OR_SCHEMA_PATTERN = Pattern.compile(
            "(?is)^\\s*DROP\\s+(?:DATABASE|SCHEMA)\\s+(?:IF\\s+EXISTS\\s+)?(?:`([^`]+)`|\"([^\"]+)\"|'([^']+)'|([a-zA-Z0-9_$]+))");

    private static final Pattern CREATE_DATABASE_OR_SCHEMA_PATTERN = Pattern.compile(
            "(?is)^\\s*CREATE\\s+(?:DATABASE|SCHEMA)(?:\\s+IF\\s+NOT\\s+EXISTS)?\\s+(?:`([^`]+)`|\"([^\"]+)\"|'([^']+)'|([a-zA-Z0-9_$]+))");


    @Autowired
    private DataSource dataSource;

    @Autowired
    private BusinessDataSourcePoolManager businessDataSourcePoolManager;

    @Autowired
    private BusinessCatalogResolver businessCatalogResolver;

    @Autowired
    private OperationLogService logService;

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataBusinessSystemMapper businessSystemMapper;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private MetadataTableRelationMapper relationMapper;

    @Autowired
    private SqlTypeStrategyFactory strategyFactory;

    @Autowired
    private MetadataSyncService metadataSyncService;

    @Autowired
    private MySqlPhysicalCatalogService mySqlPhysicalCatalogService;

    /**
     * 执行SQL语句（内部方法，允许执行DROP TABLE等危险操作）
     * 仅供内部服务调用，不对外暴露
     *
     * @param sql             SQL语句
     * @param skipSafetyCheck 是否跳过安全检查
     * @return 执行结果
     */
    private Map<String, Object> executeSqlInternal(String sql, boolean skipSafetyCheck, String targetCatalog) {
        Map<String, Object> result = new HashMap<>();

        if (sql == null || sql.trim().isEmpty()) {
            result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
            result.put(SqlConstants.RESULT_KEY_MESSAGE, "SQL语句不能为空");
            return result;
        }

        // 移除SQL注释和多余空白
        sql = sql.trim();

        // 移除多行注释 /* */
        sql = sql.replaceAll(SqlConstants.REGEX_MULTI_LINE_COMMENT, " ");
        // 移除单行注释 --
        Pattern lineCommentPattern = Pattern.compile(SqlConstants.REGEX_LINE_COMMENT, Pattern.MULTILINE);
        sql = lineCommentPattern.matcher(sql).replaceAll("");
        // 移除多余的空白字符
        sql = sql.replaceAll(SqlConstants.REGEX_EXCESSIVE_WHITESPACE, " ").trim();

        // 检查是否为危险操作（DROP、TRUNCATE等）
        String upperSql = sql.toUpperCase().trim();

        if (!skipSafetyCheck) {
            String createCatalog = tryParseCreateDatabaseOrSchemaName(sql);
            if (createCatalog != null) {
                if (!isSingleStatementAllowingTrailingSemicolon(sql)) {
                    result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                    result.put(SqlConstants.RESULT_KEY_MESSAGE, "CREATE DATABASE/SCHEMA 仅支持单条语句，不要使用分号连接多条 SQL");
                    return result;
                }
                if (!mySqlPhysicalCatalogService.isValidCatalogName(createCatalog)) {
                    result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                    result.put(SqlConstants.RESULT_KEY_MESSAGE, "库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
                    return result;
                }
                try {
                    mySqlPhysicalCatalogService.ensureCatalogExists(createCatalog);
                    result.put(SqlConstants.RESULT_KEY_SUCCESS, true);
                    result.put(SqlConstants.RESULT_KEY_MESSAGE, "物理库已就绪（不存在则已创建）: " + createCatalog.trim());
                } catch (BizException e) {
                    result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                    result.put(SqlConstants.RESULT_KEY_MESSAGE, e.getMessage());
                } catch (RuntimeException e) {
                    result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                    result.put(SqlConstants.RESULT_KEY_MESSAGE, e.getMessage());
                }
                return result;
            }
            if (upperSql.startsWith("CREATE DATABASE") || upperSql.startsWith("CREATE SCHEMA")) {
                result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                result.put(SqlConstants.RESULT_KEY_MESSAGE,
                        "CREATE DATABASE/SCHEMA 语法无法识别，请使用单行，例如：CREATE DATABASE IF NOT EXISTS mydb 或 `mydb`");
                return result;
            }
            // 检查明显的危险操作
            if (isDangerousSql(upperSql)) {
                // 特殊处理ALTER TABLE，只允许添加字段等安全操作
                if (upperSql.startsWith(SqlConstants.SQL_TYPE_ALTER + " TABLE")) {
                    // 只允许ALTER TABLE中的ADD COLUMN、MODIFY COLUMN、CHANGE COLUMN、ADD INDEX、ADD CONSTRAINT等安全操作
                    // 禁止DROP、RENAME等危险操作
                    if (containsDangerousAlterKeywords(upperSql)) {
                        result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                        result.put(SqlConstants.RESULT_KEY_MESSAGE, "禁止执行ALTER TABLE中的DROP、RENAME等危险操作");
                        return result;
                    }
                } else {
                    // 其他危险操作直接禁止
                    result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                    result.put(SqlConstants.RESULT_KEY_MESSAGE, "禁止执行该操作，仅允许INSERT、UPDATE、SELECT、CREATE TABLE等低风险操作");
                    return result;
                }
            }
        }

        String pendingDropCatalog = null;
        if (skipSafetyCheck) {
            pendingDropCatalog = tryParseDropDatabaseOrSchemaName(sql);
            if (pendingDropCatalog != null && metadataReferencesPhysicalCatalog(pendingDropCatalog)) {
                result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
                result.put(SqlConstants.RESULT_KEY_MESSAGE,
                        "该物理库仍被元数据引用（业务系统默认库名或表级库名），请先删除或调整对应业务系统/表元数据后再执行 DROP DATABASE / DROP SCHEMA。");
                return result;
            }
        }

        try (Connection connection = businessDataSourcePoolManager.getConnection(targetCatalog);
             Statement statement = connection.createStatement()) {

            // 使用策略模式执行SQL
            SqlResult sqlResult = executeSqlWithStrategy(sql, connection, statement, upperSql);

            // 转换为Map结果格式，保持接口兼容
            convertSqlResultToMap(sqlResult, result);

            if (Boolean.TRUE.equals(result.get(SqlConstants.RESULT_KEY_SUCCESS)) && pendingDropCatalog != null) {
                businessDataSourcePoolManager.evictCatalog(pendingDropCatalog);
            }

            // 如果是 CREATE TABLE 或 ALTER TABLE 语句，自动同步字段到元数据系统
            if (upperSql.startsWith(SqlConstants.SQL_TYPE_CREATE + " TABLE") || upperSql.startsWith(SqlConstants.SQL_TYPE_ALTER + " TABLE")) {
                try {
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_START, "开始同步元数据: " + sql.substring(0, Math.min(100, sql.length())));
                    String tableName = metadataSyncService.extractTableName(sql);
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_TABLE_NAME, "提取到表名: " + tableName);
                    if (tableName != null) {
                        metadataSyncService.syncTableFields(tableName, connection);
                        // 同步外键关联关系
                        metadataSyncService.syncTableForeignKeys(tableName, connection);
                        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_COMPLETE, "元数据同步完成: " + tableName);
                    } else {
                        logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FAILED, "表名提取失败，无法同步元数据", sql);
                    }
                } catch (Exception e) {
                    // 同步失败不影响 SQL 执行结果，只记录日志
                    String tableName = metadataSyncService.extractTableName(sql);
                    logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "同步表字段失败: " + (tableName != null ? tableName : "未知表"), e.getMessage() + "，SQL: " + sql);
                    logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_EXCEPTION, "同步异常详情", e.toString());
                }
            }

        } catch (Exception e) {
            handleSqlException(e, upperSql, result);
        }

        return result;
    }

    /**
     * 使用策略模式执行SQL语句
     */
    private SqlResult executeSqlWithStrategy(String sql, Connection connection, Statement statement, String upperSql) throws Exception {
        // 获取对应的SQL执行策略
        com.metadata.service.strategy.SqlStrategy strategy = strategyFactory.getStrategy(sql);
        if (strategy != null) {
            // 使用策略执行SQL
            return strategy.execute(sql, connection, statement);
        } else {
            // 如果没有找到对应的策略，使用默认的执行方式
            int affectedRows = statement.executeUpdate(sql);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE,
                    "执行SQL: " + sql.substring(0, Math.min(100, sql.length())) + "，影响行数: " + affectedRows);
            return SqlResult.successUpdate(affectedRows, "执行成功");
        }
    }

    /**
     * 将SqlResult转换为Map格式，保持接口兼容
     */
    private void convertSqlResultToMap(SqlResult sqlResult, Map<String, Object> result) {
        result.put(SqlConstants.RESULT_KEY_SUCCESS, sqlResult.isSuccess());
        result.put(SqlConstants.RESULT_KEY_MESSAGE, sqlResult.getMessage());

        if (sqlResult.isSuccess()) {
            if (sqlResult.getData() != null) {
                result.put(SqlConstants.RESULT_KEY_DATA, sqlResult.getData());
                result.put(SqlConstants.RESULT_KEY_ROW_COUNT, sqlResult.getRowCount());
            } else {
                result.put(SqlConstants.RESULT_KEY_AFFECTED_ROWS, sqlResult.getAffectedRows());
            }
        } else {
            result.put(SqlConstants.RESULT_KEY_ERROR, sqlResult.getError());
            result.put(SqlConstants.RESULT_KEY_ORIGINAL_ERROR, sqlResult.getOriginalError());
        }
    }

    /**
     * 处理SQL执行异常
     */
    private void handleSqlException(Exception e, String upperSql, Map<String, Object> result) {
        String errorMessage = e.getMessage();
        String simpleClassName = e.getClass().getSimpleName();

        // 改进错误信息，特别是对于ALTER TABLE语句和CHECK约束违反的情况
        if (upperSql.startsWith(SqlConstants.SQL_TYPE_ALTER + " TABLE")) {
            if (errorMessage.contains("Check constraint") || errorMessage.contains("check constraint")) {
                // CHECK约束违反
                errorMessage = "添加CHECK约束失败: 现有数据不符合约束条件。建议先添加允许NULL的字段，更新数据后再添加约束，或使用默认值确保符合约束。";
            } else if (errorMessage.contains("NOT NULL") || errorMessage.contains("not null")) {
                // NOT NULL约束违反
                errorMessage = "添加NOT NULL约束失败: 现有数据中存在NULL值。建议先添加允许NULL的字段，更新数据后再修改为NOT NULL，或使用默认值。";
            } else {
                // 其他ALTER TABLE错误
                errorMessage = ApiMessages.alterTableError(e.getMessage());
            }
        } else {
            // 其他SQL错误
            errorMessage = ApiMessages.sqlExecuteError(e.getMessage());
        }

        result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
        result.put(SqlConstants.RESULT_KEY_MESSAGE, errorMessage);
        result.put(SqlConstants.RESULT_KEY_ERROR, simpleClassName);
        result.put(SqlConstants.RESULT_KEY_ORIGINAL_ERROR, e.getMessage()); // 保留原始错误信息，便于调试

        logService.logError("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "执行SQL失败", e.getMessage());
    }

    /**
     * 检查SQL是否为危险操作
     */
    private boolean isDangerousSql(String upperSql) {
        for (String prefix : SqlConstants.DANGEROUS_SQL_PREFIXES) {
            if (upperSql.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查ALTER TABLE语句是否包含危险关键字
     */
    private boolean containsDangerousAlterKeywords(String upperSql) {
        for (String keyword : SqlConstants.DANGEROUS_ALTER_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 执行SQL语句（对外接口，禁止危险操作）
     *
     * @param sql SQL语句
     * @return 执行结果
     */
    @Override
    public Map<String, Object> executeSql(String sql) {
        return executeSqlInternal(sql, false, null);
    }

    /**
     * 执行SQL语句（支持跳过安全检查）
     *
     * @param sql             SQL语句
     * @param skipSafetyCheck 是否跳过安全检查
     * @return 执行结果
     */
    @Override
    public Map<String, Object> executeSql(String sql, boolean skipSafetyCheck) {
        return executeSqlInternal(sql, skipSafetyCheck, null);
    }

    @Override
    public Map<String, Object> executeSql(String sql, boolean skipSafetyCheck, String targetCatalog) {
        return executeSqlInternal(sql, skipSafetyCheck, targetCatalog);
    }

    /**
     * 执行DROP TABLE语句（仅供内部服务调用）
     *
     * @param tableName 表名
     * @return 执行结果
     */
    @Override
    @Transactional
    public Map<String, Object> executeDropTable(String tableName) {
        return executeDropTable(tableName, null);
    }

    @Override
    @Transactional
    public Map<String, Object> executeDropTable(String tableName, String targetCatalog) {
        if (tableName == null || tableName.trim().isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
            result.put(SqlConstants.RESULT_KEY_MESSAGE, "表名不能为空");
            return result;
        }

        // 转义表名，防止SQL注入
        String safeTableName = tableName.trim().replace("`", "").replace("'", "").replace("\"", "");
        String dropSql = "DROP TABLE IF EXISTS `" + safeTableName + "`";

        Map<String, Object> result = executeSqlInternal(dropSql, true, targetCatalog);

        // 如果DROP TABLE执行成功，清理metadata_field表中的相关字段
        if ((Boolean) result.get(SqlConstants.RESULT_KEY_SUCCESS)) {
            try {
                // 查找对应的表编码
                String tableCode = safeTableName.toUpperCase();
                MetadataTable table = tableMapper.selectByCode(tableCode);

                if (table != null) {
                    // 删除该表的所有字段
                    fieldMapper.deleteByTableCode(tableCode);
                    relationMapper.deleteByTableCodeEitherSide(tableCode);
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "删除表字段: " + tableCode);

                    // 删除表记录
                    tableMapper.deleteById(table.getId());
                    logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_TABLE, "删除表记录: " + tableCode);
                } else {
                    // 尝试通过表名查找
                    List<MetadataTable> tables = tableMapper.selectAll(null);
                    for (MetadataTable t : tables) {
                        if (safeTableName.equalsIgnoreCase(t.getTableName())) {
                            // 删除该表的所有字段
                            fieldMapper.deleteByTableCode(t.getTableCode());
                            relationMapper.deleteByTableCodeEitherSide(t.getTableCode());
                            logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_FIELDS, "删除表字段: " + t.getTableCode());

                            // 删除表记录
                            tableMapper.deleteById(t.getId());
                            logService.logSuccess("admin", SqlConstants.LOG_MODULE_SYNC_TABLE, "删除表记录: " + t.getTableCode());
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
     *
     * @param sqls SQL语句（用分号分隔）
     * @return 执行结果
     */
    @Override
    @Transactional
    public Map<String, Object> executeMultipleSql(String sqls) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();

        if (sqls == null || sqls.trim().isEmpty()) {
            result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
            result.put(SqlConstants.RESULT_KEY_MESSAGE, "SQL语句不能为空");
            return result;
        }

        // 按分号分割SQL语句
        String[] sqlArray = sqls.split(";\s*");
        int successCount = 0;
        int failCount = 0;

        for (String sql : sqlArray) {
            sql = sql.trim();
            if (sql.isEmpty()) {
                continue;
            }

            Map<String, Object> singleResult = executeSql(sql);
            singleResult.put(SqlConstants.RESULT_KEY_SQL, sql);
            results.add(singleResult);

            if ((Boolean) singleResult.get(SqlConstants.RESULT_KEY_SUCCESS)) {
                successCount++;
            } else {
                failCount++;
            }
        }

        result.put(SqlConstants.RESULT_KEY_SUCCESS, failCount == 0);
        result.put(SqlConstants.RESULT_KEY_MESSAGE, String.format("共执行%d条SQL，成功%d条，失败%d条",
                results.size(), successCount, failCount));
        result.put(SqlConstants.RESULT_KEY_RESULTS, results);
        result.put(SqlConstants.RESULT_KEY_TOTAL_COUNT, results.size());
        result.put(SqlConstants.RESULT_KEY_SUCCESS_COUNT, successCount);
        result.put(SqlConstants.RESULT_KEY_FAIL_COUNT, failCount);

        return result;
    }

    /**
     * 同步数据库表外键到元数据关联关系系统（公共方法，供外部调用）
     *
     * @param tableCode 表编码，如果为null则同步所有表
     * @return 同步结果
     */
    @Override
    @Transactional
    public Map<String, Object> syncForeignKeys(String tableCode) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int totalCreated = 0; // 总共创建的关联关系记录数
        List<String> messages = new ArrayList<>();

        try {
            if (tableCode != null && !tableCode.trim().isEmpty()) {
                MetadataTable meta = tableMapper.selectByCode(tableCode);
                String catalog = businessCatalogResolver.resolveCatalog(meta);
                try (Connection connection = businessDataSourcePoolManager.getConnection(catalog)) {
                    // 同步指定表
                    String tableName1 = metadataSyncService.convertToTableName(tableCode);
                    String tableName2 = tableCode.toLowerCase();

                    try {
                        int created = metadataSyncService.syncTableForeignKeys(tableName1, connection);
                        successCount = 1;
                        totalCreated += created;
                        messages.add("表 " + tableCode + " 的外键同步成功，创建 " + created + " 条关联关系");
                    } catch (Exception e1) {
                        try {
                            int created = metadataSyncService.syncTableForeignKeys(tableName2, connection);
                            successCount = 1;
                            totalCreated += created;
                            messages.add("表 " + tableCode + " 的外键同步成功，创建 " + created + " 条关联关系");
                        } catch (Exception e2) {
                            failCount = 1;
                            messages.add("表 " + tableCode + " 的外键同步失败: " + e2.getMessage());
                            logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FOREIGN_KEY, "同步外键失败: " + tableCode, e2.getMessage());
                        }
                    }
                }
            } else {
                List<MetadataTable> tables = tableMapper.selectAll(null, null);

                for (MetadataTable table : tables) {
                    String catalog = businessCatalogResolver.resolveCatalog(table);
                    try (Connection connection = businessDataSourcePoolManager.getConnection(catalog)) {
                        String tableName1 = metadataSyncService.convertToTableName(table.getTableCode());
                        String tableName2 = table.getTableCode().toLowerCase();

                        try {
                            int created = metadataSyncService.syncTableForeignKeys(tableName1, connection);
                            successCount++;
                            totalCreated += created;
                        } catch (Exception e1) {
                            try {
                                int created = metadataSyncService.syncTableForeignKeys(tableName2, connection);
                                successCount++;
                                totalCreated += created;
                            } catch (Exception e2) {
                                failCount++;
                                messages.add("表 " + table.getTableCode() + " 的外键同步失败: " + e2.getMessage());
                                logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FOREIGN_KEY, "同步外键失败: " + table.getTableCode(), e2.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        failCount++;
                        messages.add("表 " + table.getTableCode() + " 的外键同步失败: " + e.getMessage());
                        logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FOREIGN_KEY, "同步外键失败: " + table.getTableCode(), e.getMessage());
                    }
                }
                if (successCount > 0) {
                    messages.add(0, "成功同步 " + successCount + " 个表的外键，共创建 " + totalCreated + " 条关联关系");
                }
            }

            result.put(SqlConstants.RESULT_KEY_SUCCESS, failCount == 0);
            result.put(SqlConstants.RESULT_KEY_MESSAGE, String.join("; ", messages));
            result.put(SqlConstants.RESULT_KEY_SUCCESS_COUNT, successCount);
            result.put(SqlConstants.RESULT_KEY_FAIL_COUNT, failCount);
            result.put(SqlConstants.RESULT_KEY_TOTAL_CREATED, totalCreated); // 返回创建的记录数
        } catch (Exception e) {
            result.put(SqlConstants.RESULT_KEY_SUCCESS, false);
            result.put(SqlConstants.RESULT_KEY_MESSAGE, "同步外键失败: " + e.getMessage());
            result.put(SqlConstants.RESULT_KEY_SUCCESS_COUNT, successCount);
            result.put(SqlConstants.RESULT_KEY_FAIL_COUNT, failCount);
            result.put(SqlConstants.RESULT_KEY_TOTAL_CREATED, totalCreated);
            logService.logError("admin", SqlConstants.LOG_MODULE_SYNC_FOREIGN_KEY, "同步外键失败", e.getMessage());
        }

        return result;
    }

    private static String tryParseCreateDatabaseOrSchemaName(String sql) {
        Matcher m = CREATE_DATABASE_OR_SCHEMA_PATTERN.matcher(sql.trim());
        if (!m.find()) {
            return null;
        }
        for (int g = 1; g <= m.groupCount(); g++) {
            if (m.group(g) != null) {
                return m.group(g).trim();
            }
        }
        return null;
    }

    private static boolean isSingleStatementAllowingTrailingSemicolon(String sql) {
        String s = sql.trim().replaceAll(";\\s*$", "").trim();
        return !s.contains(";");
    }

    private static String tryParseDropDatabaseOrSchemaName(String sql) {
        Matcher m = DROP_DATABASE_OR_SCHEMA_PATTERN.matcher(sql.trim());
        if (!m.find()) {
            return null;
        }
        for (int g = 1; g <= m.groupCount(); g++) {
            if (m.group(g) != null) {
                return m.group(g).trim();
            }
        }
        return null;
    }

    private boolean metadataReferencesPhysicalCatalog(String catalog) {
        if (catalog == null || catalog.isEmpty()) {
            return false;
        }
        return businessSystemMapper.countByDatabaseName(catalog) > 0
                || tableMapper.countByDatabaseName(catalog) > 0;
    }
}