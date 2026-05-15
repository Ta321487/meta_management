package com.metadata.service.impl;

import com.metadata.util.JdbcCatalogUrlRewriter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 MySQL catalog（库名）缓存业务库连接池；与主数据源共用账号参数，仅替换 URL 中的库名。
 */
@Component
public class BusinessDataSourcePoolManager {

    private final DataSource primaryDataSource;
    private final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    @Autowired
    public BusinessDataSourcePoolManager(DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    /**
     * @param catalog 业务库名；为空则使用主数据源
     */
    public Connection getConnection(String catalog) throws SQLException {
        if (!StringUtils.hasText(catalog)) {
            return primaryDataSource.getConnection();
        }
        String key = catalog.trim();
        if (!(primaryDataSource instanceof HikariDataSource)) {
            throw new IllegalStateException("主数据源须为 HikariDataSource 才能按库名派生业务连接");
        }
        HikariDataSource primary = (HikariDataSource) primaryDataSource;
        if (JdbcCatalogUrlRewriter.sameCatalog(primary.getJdbcUrl(), key)) {
            return primaryDataSource.getConnection();
        }
        return pools.computeIfAbsent(key, k -> createPool(primary, k)).getConnection();
    }

    private HikariDataSource createPool(HikariDataSource primary, String catalog) {
        String newUrl = JdbcCatalogUrlRewriter.replaceCatalog(primary.getJdbcUrl(), catalog);
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("biz-" + catalog);
        cfg.setJdbcUrl(newUrl);
        cfg.setUsername(primary.getUsername());
        cfg.setPassword(primary.getPassword());
        if (primary.getDriverClassName() != null) {
            cfg.setDriverClassName(primary.getDriverClassName());
        }
        cfg.setMaximumPoolSize(5);
        cfg.setMinimumIdle(0);
        cfg.setAutoCommit(true);
        return new HikariDataSource(cfg);
    }

    @PreDestroy
    public void shutdown() {
        pools.values().forEach(HikariDataSource::close);
        pools.clear();
    }

    /**
     * 业务系统修改库名后可调用，释放旧池（下次访问会重建）。
     */
    public void evictCatalog(String catalog) {
        if (!StringUtils.hasText(catalog)) {
            return;
        }
        HikariDataSource ds = pools.remove(catalog.trim());
        if (ds != null) {
            ds.close();
        }
    }
}
