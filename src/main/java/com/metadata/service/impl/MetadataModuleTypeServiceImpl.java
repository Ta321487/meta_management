package com.metadata.service.impl;

import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.entity.MetadataModuleType;
import com.metadata.exception.BizException;
import com.metadata.mapper.MetadataModuleTypeMapper;
import com.metadata.service.MetadataModuleTypeService;
import com.metadata.util.CodeValidator;
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

    @Override
    public List<MetadataModuleType> listAll() {
        return typeMapper.selectByCondition(null, null);
    }

    @Override
    public List<MetadataModuleType> list(String typeCode, String typeName) {
        return typeMapper.selectByCondition(typeCode, typeName);
    }

    @Override
    public PageResult<MetadataModuleType> page(String typeCode, String typeName, PageRequest pageRequest) {
        Long total = typeMapper.count(typeCode, typeName);
        List<MetadataModuleType> records = typeMapper.selectPage(typeCode, typeName, pageRequest);
        return new PageResult<>(total, records);
    }

    @Override
    public MetadataModuleType getByCode(String typeCode) {
        return typeMapper.selectByCode(typeCode);
    }

    @Override
    public void add(MetadataModuleType type) {
        type.setTypeCode(CodeValidator.normalizeModuleTypeCode(type.getTypeCode()));
        if (type.getTypeCode() == null || type.getTypeCode().isEmpty()) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_CODE_REQUIRED, "模块类型编码不能为空");
        }
        if (!CodeValidator.isValidModuleTypeCode(type.getTypeCode())) {
            throw BizException.badRequest("模块类型编码须为大写字母、数字或下划线，且以字母开头");
        }
        if (type.getTypeName() == null || type.getTypeName().trim().isEmpty()) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_NAME_REQUIRED, "模块类型名称不能为空");
        }

        MetadataModuleType existing = typeMapper.selectByCode(type.getTypeCode());
        if (existing != null) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_CODE_DUPLICATE, "模块类型编码已存在");
        }

        typeMapper.insert(type);
    }

    @Override
    public void update(MetadataModuleType type) {
        if (type.getId() == null) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_ID_REQUIRED, "模块类型ID不能为空");
        }

        type.setTypeCode(CodeValidator.normalizeModuleTypeCode(type.getTypeCode()));
        if (type.getTypeCode() == null || type.getTypeCode().isEmpty()) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_CODE_REQUIRED, "模块类型编码不能为空");
        }
        if (!CodeValidator.isValidModuleTypeCode(type.getTypeCode())) {
            throw BizException.badRequest("模块类型编码须为大写字母、数字或下划线，且以字母开头");
        }
        if (type.getTypeName() == null || type.getTypeName().trim().isEmpty()) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_NAME_REQUIRED, "模块类型名称不能为空");
        }

        MetadataModuleType existing = typeMapper.selectByCode(type.getTypeCode());
        if (existing != null && !existing.getId().equals(type.getId())) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_CODE_CONFLICT, "模块类型编码已被其他记录使用");
        }

        typeMapper.update(type);
    }

    @Override
    public void delete(Long id) {
        typeMapper.deleteById(id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw BizException.of(AppErrorCodes.MODULE_TYPE_BATCH_IDS_EMPTY, "删除ID列表不能为空");
        }
        for (Long id : ids) {
            delete(id);
        }
    }
}
