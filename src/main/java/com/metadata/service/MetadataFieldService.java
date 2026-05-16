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
     * 查询表的所有字段。includeDisabled 为 true 时不按上级启用链路过滤（字段管理页传 true）。
     */
    List<MetadataField> listByTableCode(String tableCode, String businessCode, Boolean includeDisabled);

    default List<MetadataField> listByTableCode(String tableCode) {
        return listByTableCode(tableCode, "", null);
    }

    default List<MetadataField> listByTableCode(String tableCode, String businessCode) {
        return listByTableCode(tableCode, businessCode, null);
    }

    /**
     * 分页查询表的字段
     */
    PageResult<MetadataField> pageByTableCode(String tableCode, String businessCode, PageRequest pageRequest, Boolean includeDisabled);

    default PageResult<MetadataField> pageByTableCode(String tableCode, PageRequest pageRequest, Boolean includeDisabled) {
        return pageByTableCode(tableCode, "", pageRequest, includeDisabled);
    }

    default PageResult<MetadataField> pageByTableCode(String tableCode, PageRequest pageRequest) {
        return pageByTableCode(tableCode, "", pageRequest, null);
    }

    /**
     * 获取表的约束列表
     */
    List<Map<String, Object>> getConstraints(String tableCode);

    /**
     * 删除约束
     */
    void deleteConstraint(Map<String, Object> params);

    /**
     * 更新字段的业务系统
     *
     * @param tableCode    表编码
     * @param businessCode 业务系统编码
     */
    void updateFieldsBusinessSystemByTable(String tableCode, String businessCode);

    /**
     * 批量更新字段的业务系统
     *
     * @param tableCodes   表编码列表
     * @param businessCode 业务系统编码
     */
    void batchUpdateFieldsBusinessSystem(List<String> tableCodes, String businessCode);

    /**
     * 批量更新字段状态
     *
     * @param ids    字段ID列表
     * @param status 目标状态，1-启用，0-禁用
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 查询业务库物理表上已存在的列名（小写比较由调用方处理）；表或库不存在时返回空列表。
     */
    List<String> listPhysicalColumnNames(String tableCode, String businessCode);

    /**
     * 从业务库物理表补登记尚未写入元数据的列（不修改物理表、不删除元数据字段）。
     */
    Map<String, Object> syncMissingFieldsFromPhysical(String tableCode, String businessCode);
}