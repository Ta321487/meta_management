package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.entity.MetadataField;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 字段服务
 */
@Service
public class MetadataFieldService {

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增字段
     */
    @Transactional
    public void add(MetadataField field) {
        if (!CodeValidator.isValidCode(field.getFieldCode())) {
            throw new RuntimeException("字段编码格式不正确");
        }
        if (fieldMapper.countByCode(field.getTableCode(), field.getFieldCode()) > 0) {
            throw new RuntimeException("字段编码已存在");
        }
        fieldMapper.insert(field);
        logService.logSuccess("admin", "ADD", "新增字段：" + JSON.toJSONString(field));
    }

    /**
     * 更新字段
     */
    @Transactional
    public void update(MetadataField field) {
        MetadataField existing = fieldMapper.selectByCode(field.getTableCode(), field.getFieldCode());
        if (existing == null) {
            throw new RuntimeException("字段不存在");
        }
        field.setId(existing.getId());
        field.setFieldCode(existing.getFieldCode()); // 编码不可修改
        field.setTableCode(existing.getTableCode()); // 表编码不可修改
        fieldMapper.update(field);
        logService.logSuccess("admin", "EDIT", "更新字段：" + JSON.toJSONString(field));
    }

    /**
     * 删除字段
     */
    @Transactional
    public void delete(Long id) {
        MetadataField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段不存在");
        }
        fieldMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除字段：" + JSON.toJSONString(field));
    }

    /**
     * 查询表的所有字段
     */
    public List<MetadataField> listByTableCode(String tableCode) {
        return fieldMapper.selectByTableCode(tableCode);
    }
}

