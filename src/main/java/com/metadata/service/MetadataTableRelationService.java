package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 表关联关系服务
 */
@Service
public class MetadataTableRelationService {

    @Autowired
    private MetadataTableRelationMapper relationMapper;

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增关联关系
     */
    @Transactional
    public void add(MetadataTableRelation relation) {
        if (!CodeValidator.isValidCodes(relation.getRelationCode(), relation.getMainTableCode(),
                relation.getSlaveTableCode(), relation.getMainFieldCode(), relation.getSlaveFieldCode())) {
            throw new RuntimeException("编码格式不正确");
        }
        // 校验表是否存在
        if (tableMapper.selectByCode(relation.getMainTableCode()) == null) {
            throw new RuntimeException("主表不存在");
        }
        if (tableMapper.selectByCode(relation.getSlaveTableCode()) == null) {
            throw new RuntimeException("从表不存在");
        }
        // 校验字段是否存在
        if (fieldMapper.selectByCode(relation.getMainTableCode(), relation.getMainFieldCode()) == null) {
            throw new RuntimeException("主表关联字段不存在");
        }
        if (fieldMapper.selectByCode(relation.getSlaveTableCode(), relation.getSlaveFieldCode()) == null) {
            throw new RuntimeException("从表外键字段不存在");
        }
        if (relationMapper.countByCode(relation.getRelationCode()) > 0) {
            throw new RuntimeException("关联编码已存在");
        }
        relationMapper.insert(relation);
        logService.logSuccess("admin", "ADD", "新增表关联关系：" + JSON.toJSONString(relation));
    }

    /**
     * 更新关联关系
     */
    @Transactional
    public void update(MetadataTableRelation relation) {
        MetadataTableRelation existing = relationMapper.selectByCode(relation.getRelationCode());
        if (existing == null) {
            throw new RuntimeException("关联关系不存在");
        }
        relation.setId(existing.getId());
        relationMapper.update(relation);
        logService.logSuccess("admin", "EDIT", "更新表关联关系：" + JSON.toJSONString(relation));
    }

    /**
     * 删除关联关系
     */
    @Transactional
    public void delete(Long id) {
        relationMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除表关联关系ID：" + id);
    }

    /**
     * 查询主表的关联关系
     */
    public List<MetadataTableRelation> listByMainTableCode(String mainTableCode) {
        return relationMapper.selectByMainTableCode(mainTableCode);
    }

    /**
     * 查询从表的关联关系
     */
    public List<MetadataTableRelation> listBySlaveTableCode(String slaveTableCode) {
        return relationMapper.selectBySlaveTableCode(slaveTableCode);
    }

    /**
     * 查询所有关联关系
     */
    public List<MetadataTableRelation> listAll() {
        return relationMapper.selectAll();
    }
}

