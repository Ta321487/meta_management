package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataTableRelation;

import java.util.List;
import java.util.Map;

/**
 * 表关联关系服务接口
 */
public interface MetadataTableRelationService {

    /**
     * 新增关联关系
     */
    void add(MetadataTableRelation relation);

    /**
     * 更新关联关系
     */
    void update(MetadataTableRelation relation);

    /**
     * 删除关联关系
     */
    void delete(Long id);

    /**
     * 查询主表的关联关系
     */
    List<MetadataTableRelation> listByMainTableCode(String mainTableCode);

    /**
     * 查询从表的关联关系
     */
    List<MetadataTableRelation> listBySlaveTableCode(String slaveTableCode);

    /**
     * 查询所有关联关系
     */
    List<MetadataTableRelation> listAll();
    
    /**
     * 按业务系统查询所有关联关系
     */
    List<MetadataTableRelation> listAll(String businessCode);

    /**
     * 分页查询关联关系
     */
    PageResult<MetadataTableRelation> page(PageRequest pageRequest);
    
    /**
     * 按业务系统分页查询关联关系
     */
    PageResult<MetadataTableRelation> page(PageRequest pageRequest, String businessCode);

    /**
     * 创建外键约束
     * @param relation 关联关系
     * @return 执行结果
     */
    Map<String, Object> createForeignKey(MetadataTableRelation relation);

    /**
     * 同步元数据关联关系到数据库外键约束
     * @param tableCode 表编码，如果为null或空字符串则同步所有表
     * @return 同步结果
     */
    Map<String, Object> syncForeignKeys(String tableCode);
    
    /**
     * 根据主表、从表、主字段、从字段的组合来查找关联关系记录
     * @param mainTableCode 主表编码
     * @param slaveTableCode 从表编码
     * @param mainFieldCode 主表关联字段编码
     * @param slaveFieldCode 从表外键字段编码
     * @param businessCode 业务系统编码
     * @return 关联关系对象列表
     */
    List<MetadataTableRelation> listByTablesAndFields(
            String mainTableCode,
            String slaveTableCode,
            String mainFieldCode,
            String slaveFieldCode,
            String businessCode
    );
    
    /**
     * 根据主表、从表、主字段、从字段的组合来查找关联关系记录（默认业务系统）
     * @param mainTableCode 主表编码
     * @param slaveTableCode 从表编码
     * @param mainFieldCode 主表关联字段编码
     * @param slaveFieldCode 从表外键字段编码
     * @return 关联关系对象列表
     */
    List<MetadataTableRelation> listByTablesAndFields(
            String mainTableCode,
            String slaveTableCode,
            String mainFieldCode,
            String slaveFieldCode
    );
}