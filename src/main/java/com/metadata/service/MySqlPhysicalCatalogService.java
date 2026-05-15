package com.metadata.service;

/**
 * 在元数据应用连接的 MySQL 实例上创建业务物理库（catalog）。
 */
public interface MySqlPhysicalCatalogService {

    /**
     * 库名是否允许用于自动建库（字母、数字、下划线、$，长度 1–64）。
     */
    boolean isValidCatalogName(String name);

    /**
     * 若库不存在则创建；字符集/排序规则优先从「库管理」登记表读取，无登记则 utf8mb4 / utf8mb4_unicode_ci。
     */
    void ensureCatalogExists(String catalogName);

    /**
     * 按指定字符集与排序规则建库（须为系统允许的组合），用于登记表尚未写入库时的同步。
     */
    void ensureCatalogExists(String catalogName, String charsetName, String collationName);
}
