package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.*;
import com.metadata.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 元数据导出服务
 */
@Service
public class MetadataExportService {

    @Autowired
    private MetadataModuleMapper moduleMapper;

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private MetadataFunctionNodeMapper nodeMapper;

    @Autowired
    private MetadataBusinessRuleMapper ruleMapper;

    @Autowired
    private MetadataTableRelationMapper relationMapper;

    @Autowired
    private MetadataModuleTableMapper moduleTableMapper;

    /**
     * 导出模块元数据
     */
    public JSONObject exportModule(String moduleCode) {
        MetadataModule module = moduleMapper.selectByCode(moduleCode);
        if (module == null) {
            throw new RuntimeException("模块不存在");
        }

        JSONObject result = new JSONObject();
        result.put("module", module);

        // 关联的表
        List<String> tableCodes = moduleTableMapper.selectTableCodesByModuleCode(moduleCode);
        List<MetadataTable> tables = tableCodes.stream()
                .map(tableMapper::selectByCode)
                .collect(Collectors.toList());
        result.put("tables", tables);

        // 表的字段
        Map<String, List<MetadataField>> fieldsMap = new HashMap<>();
        for (String tableCode : tableCodes) {
            List<MetadataField> fields = fieldMapper.selectByTableCode(tableCode);
            fieldsMap.put(tableCode, fields);
        }
        result.put("fields", fieldsMap);

        // 功能节点
        List<MetadataFunctionNode> nodes = nodeMapper.selectByModuleCode(moduleCode);
        result.put("nodes", nodes);

        // 业务规则
        List<MetadataBusinessRule> rules = ruleMapper.selectByModuleCode(moduleCode);
        result.put("rules", rules);

        return result;
    }

    /**
     * 导出表元数据
     */
    public JSONObject exportTable(String tableCode) {
        MetadataTable table = tableMapper.selectByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }

        JSONObject result = new JSONObject();
        result.put("table", table);

        // 字段
        List<MetadataField> fields = fieldMapper.selectByTableCode(tableCode);
        result.put("fields", fields);

        return result;
    }

    /**
     * 导出所有元数据
     */
    public JSONObject exportAll() {
        JSONObject result = new JSONObject();

        // 所有模块
        List<MetadataModule> modules = moduleMapper.selectAll(null, null, null);
        result.put("modules", modules);

        // 所有表
        List<MetadataTable> tables = tableMapper.selectAll(null);
        result.put("tables", tables);

        // 所有字段
        Map<String, List<MetadataField>> fieldsMap = new HashMap<>();
        for (MetadataTable table : tables) {
            List<MetadataField> fields = fieldMapper.selectByTableCode(table.getTableCode());
            fieldsMap.put(table.getTableCode(), fields);
        }
        result.put("fields", fieldsMap);

        // 所有关联关系
        List<MetadataTableRelation> relations = relationMapper.selectAll();
        result.put("relations", relations);

        return result;
    }
}

