package com.metadata.service.impl;

import com.metadata.entity.MetadataPhysicalDatabase;
import com.metadata.mapper.MetadataPhysicalDatabaseMapper;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.service.OperationLogService;
import com.metadata.service.constant.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.metadata.util.JdbcCatalogUrlRewriter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 使用主数据源在实例上执行 CREATE DATABASE IF NOT EXISTS，字符集与排序规则与登记表或显式参数一致（白名单内）。
 */
@Service
public class MySqlPhysicalCatalogServiceImpl implements MySqlPhysicalCatalogService {

    private static final Pattern CATALOG_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_$]{1,64}$");

    /** 允许的 charset → collation（顺序首项为 charset 的默认排序规则） */
    private static final Map<String, List<String>> CHARSET_TO_COLLATIONS = new LinkedHashMap<>();

    static {
        CHARSET_TO_COLLATIONS.put("utf8mb4", List.of("utf8mb4_unicode_ci", "utf8mb4_general_ci", "utf8mb4_0900_ai_ci"));
        CHARSET_TO_COLLATIONS.put("utf8", List.of("utf8_unicode_ci", "utf8_general_ci"));
        CHARSET_TO_COLLATIONS.put("latin1", List.of("latin1_swedish_ci", "latin1_general_ci"));
    }

    private static final Set<String> SYSTEM_CATALOGS = Set.of(
            "information_schema", "mysql", "performance_schema", "sys"
    );

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OperationLogService logService;

    @Autowired
    private MetadataPhysicalDatabaseMapper physicalDatabaseMapper;

    @Override
    public boolean isValidCatalogName(String name) {
        if (name == null) {
            return false;
        }
        String t = name.trim();
        return CATALOG_NAME_PATTERN.matcher(t).matches();
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void ensureCatalogExists(String catalogName) {
        if (catalogName == null || catalogName.trim().isEmpty()) {
            return;
        }
        String cat = catalogName.trim();
        if (!isValidCatalogName(cat)) {
            throw new RuntimeException("物理库名非法：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        MetadataPhysicalDatabase reg = physicalDatabaseMapper.selectByCatalogName(cat);
        if (reg != null && StringUtils.hasText(reg.getCharsetName())) {
            runEnsure(cat, reg.getCharsetName(), reg.getCollationName());
        } else {
            runEnsure(cat, "utf8mb4", "utf8mb4_unicode_ci");
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void ensureCatalogExists(String catalogName, String charsetName, String collationName) {
        if (catalogName == null || catalogName.trim().isEmpty()) {
            return;
        }
        String cat = catalogName.trim();
        if (!isValidCatalogName(cat)) {
            throw new RuntimeException("物理库名非法：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        runEnsure(cat, charsetName, collationName);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void dropCatalogIfExists(String catalogName) {
        if (catalogName == null || catalogName.trim().isEmpty()) {
            return;
        }
        String cat = catalogName.trim();
        if (!isValidCatalogName(cat)) {
            throw new RuntimeException("物理库名非法：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        assertCatalogDroppable(cat);
        String escaped = cat.replace("`", "``");
        String ddl = "DROP DATABASE IF EXISTS `" + escaped + "`";
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "删除服务器物理库: " + cat);
        } catch (Exception e) {
            throw new RuntimeException("删除服务器物理库失败: " + e.getMessage(), e);
        }
    }

    private void assertCatalogDroppable(String catalogName) {
        String lower = catalogName.toLowerCase(Locale.ROOT);
        if (SYSTEM_CATALOGS.contains(lower)) {
            throw new RuntimeException("不允许删除系统库: " + catalogName);
        }
        if (JdbcCatalogUrlRewriter.sameCatalog(datasourceUrl, catalogName)) {
            throw new RuntimeException("不允许删除元数据管理库: " + catalogName);
        }
    }

    private void runEnsure(String catalogNameTrimmed, String charsetName, String collationName) {
        String charset = normalizeCharset(charsetName);
        String collation = normalizeCollation(charset, collationName);
        String escaped = catalogNameTrimmed.replace("`", "``");
        String ddl = "CREATE DATABASE IF NOT EXISTS `" + escaped + "` DEFAULT CHARACTER SET " + charset + " COLLATE " + collation;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE,
                    "确保物理库存在: " + catalogNameTrimmed + " (" + charset + "/" + collation + ")");
        } catch (Exception e) {
            throw new RuntimeException("创建物理库失败: " + e.getMessage(), e);
        }
    }

    private static String normalizeCharset(String input) {
        if (!StringUtils.hasText(input)) {
            return "utf8mb4";
        }
        String s = input.trim().toLowerCase(Locale.ROOT);
        if (CHARSET_TO_COLLATIONS.containsKey(s)) {
            return s;
        }
        return "utf8mb4";
    }

    private static String normalizeCollation(String charset, String input) {
        List<String> allowed = CHARSET_TO_COLLATIONS.get(charset);
        if (allowed == null || allowed.isEmpty()) {
            return "utf8mb4_unicode_ci";
        }
        if (StringUtils.hasText(input)) {
            String c = input.trim();
            for (String a : allowed) {
                if (a.equalsIgnoreCase(c)) {
                    return a;
                }
            }
        }
        return allowed.get(0);
    }
}
