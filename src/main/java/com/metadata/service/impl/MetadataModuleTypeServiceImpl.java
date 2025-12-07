package com.metadata.service.impl;

import com.metadata.entity.MetadataModuleType;
import com.metadata.mapper.MetadataModuleTypeMapper;
import com.metadata.service.MetadataModuleTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模块类型服务实现
 */
@Service
public class MetadataModuleTypeServiceImpl implements MetadataModuleTypeService {

    @Autowired
    private MetadataModuleTypeMapper typeMapper;

    /**
     * 查询所有模块类型
     */
    @Override
    public List<MetadataModuleType> listAll() {
        return typeMapper.selectAll();
    }

    /**
     * 根据编码查询
     */
    @Override
    public MetadataModuleType getByCode(String typeCode) {
        return typeMapper.selectByCode(typeCode);
    }

    /**
     * 新增模块类型
     */
    @Override
    public void add(MetadataModuleType type) {
        // 验证必填字段
        if (type.getTypeCode() == null || type.getTypeCode().trim().isEmpty()) {
            throw new RuntimeException("模块类型编码不能为空");
        }
        if (type.getTypeName() == null || type.getTypeName().trim().isEmpty()) {
            throw new RuntimeException("模块类型名称不能为空");
        }
        
        // 验证typeCode唯一性
        MetadataModuleType existing = typeMapper.selectByCode(type.getTypeCode());
        if (existing != null) {
            throw new RuntimeException("模块类型编码已存在");
        }
        
        // 插入数据
        typeMapper.insert(type);
    }

    /**
     * 更新模块类型
     */
    @Override
    public void update(MetadataModuleType type) {
        // 验证id不能为空
        if (type.getId() == null) {
            throw new RuntimeException("模块类型ID不能为空");
        }
        
        // 验证必填字段
        if (type.getTypeCode() == null || type.getTypeCode().trim().isEmpty()) {
            throw new RuntimeException("模块类型编码不能为空");
        }
        if (type.getTypeName() == null || type.getTypeName().trim().isEmpty()) {
            throw new RuntimeException("模块类型名称不能为空");
        }
        
        // 验证模块类型是否存在
        MetadataModuleType existing = typeMapper.selectByCode(type.getTypeCode());
        if (existing != null && !existing.getId().equals(type.getId())) {
            throw new RuntimeException("模块类型编码已被其他记录使用");
        }
        
        // 更新数据
        typeMapper.update(type);
    }

    /**
     * 删除模块类型
     */
    @Override
    public void delete(Long id) {
        typeMapper.deleteById(id);
    }
}