package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataModuleTableMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表服务
 */
@Service
public class MetadataTableService {

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private MetadataModuleTableMapper moduleTableMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增表
     */
    @Transactional
    public void add(MetadataTable table) {
        if (!CodeValidator.isValidCode(table.getTableCode())) {
            throw new RuntimeException("表编码格式不正确");
        }
        if (tableMapper.countByCode(table.getTableCode()) > 0) {
            throw new RuntimeException("表编码已存在");
        }
        tableMapper.insert(table);
        logService.logSuccess("admin", "ADD", "新增表：" + JSON.toJSONString(table));
    }

    /**
     * 更新表
     */
    @Transactional
    public void update(MetadataTable table) {
        MetadataTable existing = tableMapper.selectById(table.getId());
        if (existing == null) {
            throw new RuntimeException("表不存在");
        }
        table.setTableCode(existing.getTableCode()); // 编码不可修改
        tableMapper.update(table);
        logService.logSuccess("admin", "EDIT", "更新表：" + JSON.toJSONString(table));
    }

    /**
     * 删除表
     */
    @Transactional
    public void delete(Long id) {
        MetadataTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        // 删除字段
        fieldMapper.deleteByTableCode(table.getTableCode());
        // 删除模块关联
        moduleTableMapper.deleteByTableCode(table.getTableCode());
        // 删除表
        tableMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除表：" + table.getTableCode());
    }

    /**
     * 查询表详情
     */
    public MetadataTable getByCode(String tableCode) {
        return tableMapper.selectByCode(tableCode);
    }

    /**
     * 查询所有表
     */
    public List<MetadataTable> list(String tableName) {
        return tableMapper.selectAll(tableName);
    }

    /**
     * 分页查询表
     */
    public PageResult<MetadataTable> page(String tableName, PageRequest pageRequest) {
        Long total = tableMapper.count(tableName);
        List<MetadataTable> records = tableMapper.selectPage(tableName, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 根据模块编码查询表
     */
    public List<MetadataTable> listByModuleCode(String moduleCode) {
        return tableMapper.selectByModuleCode(moduleCode);
    }
}

