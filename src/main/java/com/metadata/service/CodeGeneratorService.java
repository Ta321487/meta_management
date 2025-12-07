package com.metadata.service;

import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;

import java.util.List;
import java.util.Map;

/**
 * 代码生成服务接口
 */
public interface CodeGeneratorService {

    /**
     * 生成数据库建表SQL
     */
    String generateCreateTableSQL(String tableCode) throws Exception;

    /**
     * 生成Java实体类
     */
    String generateEntity(String tableCode, String packageName) throws Exception;

    /**
     * 生成Controller
     */
    String generateController(String tableCode, String packageName) throws Exception;

    /**
     * 生成Service
     */
    String generateService(String tableCode, String packageName) throws Exception;

    /**
     * 生成Mapper接口
     */
    String generateMapper(String tableCode, String packageName) throws Exception;

    /**
     * 生成Mapper XML
     */
    String generateMapperXml(String tableCode, String packageName) throws Exception;

    /**
     * 生成Vue列表页面
     */
    String generateVueList(String tableCode) throws Exception;

    /**
     * 生成Vue表单页面
     */
    String generateVueForm(String tableCode) throws Exception;

    /**
     * 生成前端路由配置（routes.js）
     */
    String generateRoutes(String tableCode) throws Exception;

    /**
     * 生成完整的代码包（包含所有文件）
     */
    Map<String, String> generateAll(String tableCode, String packageName) throws Exception;

    /**
     * 工具方法：转换为表名（下划线）
     */
    String convertToTableName(String code);

    /**
     * 获取Java类型
     */
    String getJavaType(String fieldType);

    /**
     * 检查字段列表中是否有日期类型
     */
    boolean hasDate(List<MetadataField> fields);

    /**
     * 检查字段列表中是否有Decimal类型
     */
    boolean hasDecimal(List<MetadataField> fields);
    
    /**
     * 生成CHECK约束
     */
    String generateCheckConstraint(MetadataField field);

    /**
     * 生成添加字段的ALTER TABLE语句
     */
    String generateAlterTableAddColumnSQL(String tableCode, MetadataField field) throws Exception;

    /**
     * 生成修改字段的ALTER TABLE语句
     */
    String generateAlterTableModifyColumnSQL(String tableCode, MetadataField field) throws Exception;
    
    /**
     * 生成修改字段名称和属性的ALTER TABLE语句
     */
    String generateAlterTableChangeColumnSQL(String tableCode, String oldFieldName, MetadataField field) throws Exception;

    /**
     * 生成删除字段的ALTER TABLE语句
     */
    String generateAlterTableDropColumnSQL(String tableCode, String fieldName) throws Exception;
}