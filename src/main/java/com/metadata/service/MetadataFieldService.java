package com.metadata.service;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataField;

import java.util.List;
import java.util.Map;

/**
 * 字段服务接口
 */
public interface MetadataFieldService {

    /**
     * 新增字段
     */
    void add(MetadataField field);

    /**
     * 更新字段
     */
    void update(MetadataField field);

    /**
     * 删除字段
     */
    void delete(Long id);

    /**
     * 批量删除字段
     */
    void batchDelete(List<Long> ids);

    /**
     * 查询表的所有字段
     */
    List<MetadataField> listByTableCode(String tableCode);

    /**
     * 分页查询表的字段
     */
    PageResult<MetadataField> pageByTableCode(String tableCode, PageRequest pageRequest);

    /**
     * 获取表的约束列表
     */
    List<Map<String, Object>> getConstraints(String tableCode);

    /**
     * 删除约束
     */
    void deleteConstraint(Map<String, Object> params);
}