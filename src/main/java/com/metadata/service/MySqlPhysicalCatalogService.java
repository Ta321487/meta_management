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
     * 目标库是否已在当前 MySQL 实例上存在（不创建）。
     */
    boolean catalogExistsOnInstance(String catalogName);

    /**
     * 若库在实例上不存在则抛出业务异常（不创建）。
     */
    void requireCatalogOnInstance(String catalogName);

    /**
     * 若库不存在则创建；字符集/排序规则优先从「库管理」登记表读取，无登记则 utf8mb4 / utf8mb4_unicode_ci。
     * 仅应由「库管理」保存时建库、显式同步、SQL 执行页 CREATE DATABASE 等入口调用。
     */
    void ensureCatalogExists(String catalogName);

    /**
     * 按指定字符集与排序规则建库（须为系统允许的组合），用于库配置尚未写入时的同步。
     */
    void ensureCatalogExists(String catalogName, String charsetName, String collationName);

    /**
     * 若库在实例上存在则删除（DROP DATABASE）；禁止删除元数据库与系统库。
     */
    void dropCatalogIfExists(String catalogName);
}
