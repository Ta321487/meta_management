package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataBusinessRule;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataBusinessRuleMapper;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.service.CodeGeneratorService;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.MetadataTableService;
import com.metadata.service.OperationLogService;
import com.metadata.service.SqlExecuteService;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务规则服务实现
 */
@Service
public class MetadataBusinessRuleServiceImpl implements MetadataBusinessRuleService {

    @Autowired
    private MetadataBusinessRuleMapper ruleMapper;

    @Autowired
    private OperationLogService logService;

    @Autowired
    private MetadataTableService tableService;

    @Autowired
    @Lazy
    private MetadataFieldService fieldService;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    @Lazy
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private SqlExecuteService sqlExecuteService;

    /**
     * 验证规则中的字段是否正确
     */
    private void validateRuleFields(MetadataBusinessRule rule) {
        // 只处理VALIDATION_RULE类型的规则
        if (!"VALIDATION_RULE".equals(rule.getRuleType())) {
            return;
        }
        
        // 解析规则内容
        JSONObject ruleContent = JSONObject.parseObject(rule.getRuleContent());
        if (ruleContent == null) {
            return;
        }
        
        // 检查规则类型是否为unique或unique_combo
        String ruleType = ruleContent.getString("type");
        if (!"unique".equals(ruleType) && !"unique_combo".equals(ruleType)) {
            return;
        }
        
        // 提取字段列表
        List<String> fieldsToValidate = new ArrayList<>();
        if ("unique".equals(ruleType)) {
            // 单字段唯一
            String field = ruleContent.getString("field");
            if (field != null && !field.isEmpty()) {
                fieldsToValidate.add(field);
            }
        } else if ("unique_combo".equals(ruleType)) {
            // 组合字段唯一
            Object fieldsObj = ruleContent.get("fields");
            if (fieldsObj instanceof JSONArray) {
                JSONArray fieldsArray = (JSONArray) fieldsObj;
                for (Object fieldObj : fieldsArray) {
                    if (fieldObj instanceof String) {
                        fieldsToValidate.add((String) fieldObj);
                    }
                }
            }
        }
        
        if (fieldsToValidate.isEmpty()) {
            return;
        }
        
        // 获取模块关联的所有表
        List<MetadataTable> tables = tableService.listByModuleCode(rule.getModuleCode(), rule.getBusinessCode());
        if (tables.isEmpty()) {
            throw new RuntimeException("模块未关联任何表");
        }
        
        // 获取所有表的字段
        Set<String> allFieldNames = new HashSet<>();
        for (MetadataTable table : tables) {
            List<MetadataField> fields = fieldService.listByTableCode(table.getTableCode(), rule.getBusinessCode());
            for (MetadataField field : fields) {
                allFieldNames.add(field.getFieldName());
            }
        }
        
        // 验证字段是否存在
        for (String fieldName : fieldsToValidate) {
            if (!allFieldNames.contains(fieldName)) {
                throw new RuntimeException("字段名称不匹配：" + fieldName);
            }
        }
    }
    
    /**
     * 生成并执行ALTER TABLE语句来创建唯一索引
     */
    private void generateAndExecuteUniqueIndex(MetadataBusinessRule rule) {
        // 只处理VALIDATION_RULE类型的规则
        if (!"VALIDATION_RULE".equals(rule.getRuleType())) {
            return;
        }
        
        // 解析规则内容
        JSONObject ruleContent = JSONObject.parseObject(rule.getRuleContent());
        if (ruleContent == null) {
            return;
        }
        
        // 检查规则类型是否为unique或unique_combo
        String ruleType = ruleContent.getString("type");
        if (!"unique".equals(ruleType) && !"unique_combo".equals(ruleType)) {
            return;
        }
        
        // 提取字段列表
        List<String> uniqueFields = new ArrayList<>();
        if ("unique".equals(ruleType)) {
            // 单字段唯一
            String field = ruleContent.getString("field");
            if (field != null && !field.isEmpty()) {
                uniqueFields.add(field);
            }
        } else if ("unique_combo".equals(ruleType)) {
            // 组合字段唯一
            Object fieldsObj = ruleContent.get("fields");
            if (fieldsObj instanceof JSONArray) {
                JSONArray fieldsArray = (JSONArray) fieldsObj;
                for (Object fieldObj : fieldsArray) {
                    if (fieldObj instanceof String) {
                        uniqueFields.add((String) fieldObj);
                    }
                }
            }
        }
        
        if (uniqueFields.isEmpty()) {
            return;
        }
        
        try {
            // 获取规则关联的所有表
            List<MetadataTable> tables = tableService.listByModuleCode(rule.getModuleCode(), rule.getBusinessCode());
            if (tables.isEmpty()) {
                return;
            }
            
            // 遍历每个表，生成并执行ALTER TABLE语句
            for (MetadataTable table : tables) {
                String tableCode = table.getTableCode();
                // 获取表名
                String tableName = codeGeneratorService.convertToTableName(tableCode);
                
                // 检查字段是否都存在于当前表中
                List<MetadataField> tableFields = fieldMapper.selectByTableCode(tableCode, rule.getBusinessCode());
                Set<String> tableFieldNames = new HashSet<>();
                for (MetadataField field : tableFields) {
                    tableFieldNames.add(field.getFieldName());
                }
                
                boolean allFieldsExist = true;
                for (String uniqueField : uniqueFields) {
                    if (!tableFieldNames.contains(uniqueField)) {
                        allFieldsExist = false;
                        break;
                    }
                }
                
                if (!allFieldsExist) {
                    continue;
                }
                
                // 生成索引名称
                String indexName = "uk_" + tableName + "_" + String.join("_", uniqueFields);
                
                // 先检查索引是否已经存在，如果存在则先删除
                String checkIndexSql = "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND index_name = '" + indexName + "'";
                Map<String, Object> checkResult = sqlExecuteService.executeSql(checkIndexSql, false);
                if (checkResult != null && Boolean.TRUE.equals(checkResult.get("success"))) {
                    List<Map<String, Object>> rows = (List<Map<String, Object>>) checkResult.get("data");
                    if (rows != null && !rows.isEmpty()) {
                        // 获取第一行的结果，判断索引是否存在
                        Object countObj = rows.get(0).get("COUNT(*)");
                        if (countObj != null && Integer.parseInt(countObj.toString()) > 0) {
                            // 索引已经存在，先删除它
                            String dropIndexSql = "ALTER TABLE `" + tableName + "` DROP INDEX `" + indexName + "`";
                            sqlExecuteService.executeSql(dropIndexSql, true);
                        }
                    }
                }
                
                // 生成ALTER TABLE语句
                String alterSql = "ALTER TABLE `" + tableName + "` ADD UNIQUE KEY `" + indexName + "` (`" + String.join("`, `", uniqueFields) + "`)";
                
                // 执行SQL语句
                sqlExecuteService.executeSql(alterSql);
            }
        } catch (Exception e) {
            // 如果生成或执行SQL失败，只记录日志，不影响规则保存
            e.printStackTrace();
        }
    }
    
    /**
     * 新增规则
     */
    @Override
    @Transactional
    public void add(MetadataBusinessRule rule) {
        if (!CodeValidator.isValidCode(rule.getRuleCode())) {
            throw new RuntimeException("规则编码格式不正确");
        }
        
        // 验证规则中的字段
        validateRuleFields(rule);
        
        ruleMapper.insert(rule);
        logService.logSuccess("admin", "ADD", "新增业务规则：" + JSON.toJSONString(rule));
        
        // 生成并执行唯一索引
        generateAndExecuteUniqueIndex(rule);
    }

    /**
     * 更新规则
     */
    @Override
    @Transactional
    public void update(MetadataBusinessRule rule) {
        MetadataBusinessRule existing = ruleMapper.selectByCode(rule.getModuleCode(), rule.getRuleCode(), rule.getBusinessCode());
        if (existing == null) {
            throw new RuntimeException("规则不存在");
        }
        
        // 验证规则中的字段
        validateRuleFields(rule);
        
        rule.setId(existing.getId());
        ruleMapper.update(rule);
        logService.logSuccess("admin", "EDIT", "更新业务规则：" + JSON.toJSONString(rule));
        
        // 生成并执行唯一索引
        generateAndExecuteUniqueIndex(rule);
    }

    /**
     * 删除规则
     */
    @Override
    @Transactional
    public void delete(Long id) {
        ruleMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除业务规则ID：" + id);
    }

    /**
     * 查询模块的所有规则
     */
    @Override
    public List<MetadataBusinessRule> listByModuleCode(String moduleCode) {
        return ruleMapper.selectByModuleCode(moduleCode);
    }

    /**
     * 查询模块的所有规则（按业务系统）
     */
    @Override
    public List<MetadataBusinessRule> listByModuleCode(String moduleCode, String businessCode) {
        return ruleMapper.selectByModuleCode(moduleCode, businessCode);
    }

    /**
     * 分页查询模块的规则
     */
    @Override
    public PageResult<MetadataBusinessRule> pageByModuleCode(String moduleCode, PageRequest pageRequest) {
        Long total = ruleMapper.countByModuleCode(moduleCode);
        List<MetadataBusinessRule> records = ruleMapper.selectPageByModuleCode(moduleCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 分页查询模块的规则（按业务系统）
     */
    @Override
    public PageResult<MetadataBusinessRule> pageByModuleCode(String moduleCode, String businessCode, PageRequest pageRequest) {
        Long total = ruleMapper.countByModuleCode(moduleCode, businessCode);
        List<MetadataBusinessRule> records = ruleMapper.selectPageByModuleCode(moduleCode, businessCode, pageRequest);
        return new PageResult<>(total, records);
    }
}