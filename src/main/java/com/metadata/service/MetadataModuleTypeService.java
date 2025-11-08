package com.metadata.service;

import com.metadata.entity.MetadataModuleType;
import com.metadata.mapper.MetadataModuleTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模块类型服务
 */
@Service
public class MetadataModuleTypeService {

    @Autowired
    private MetadataModuleTypeMapper typeMapper;

    /**
     * 查询所有模块类型
     */
    public List<MetadataModuleType> listAll() {
        return typeMapper.selectAll();
    }

    /**
     * 根据编码查询
     */
    public MetadataModuleType getByCode(String typeCode) {
        return typeMapper.selectByCode(typeCode);
    }
}

