package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataField;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 字段服务
 */
@Service
public class MetadataFieldService {

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private OperationLogService logService;

    @Autowired
    @Lazy
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private SqlExecuteService sqlExecuteService;

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
        // 设置isEnabled默认值
        if (field.getIsEnabled() == null) {
            field.setIsEnabled(1);
        }
        
        // 保存原始校验规则的message字段
        String originalMessage = null;
        if (field.getValidateRule() != null && !field.getValidateRule().trim().isEmpty()) {
            try {
                JSONObject originalJson = JSON.parseObject(field.getValidateRule());
                if (originalJson.containsKey("message")) {
                    originalMessage = originalJson.getString("message");
                }
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }
        
        fieldMapper.insert(field);
        logService.logSuccess("admin", "ADD", "新增字段：" + JSON.toJSONString(field));
        
        // 生成并执行ALTER TABLE ADD COLUMN语句
        try {
            String alterSql = codeGeneratorService.generateAlterTableAddColumnSQL(field.getTableCode(), field);
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, true);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("执行ALTER TABLE ADD COLUMN失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "ALTER_TABLE_ADD_COLUMN", "执行ALTER TABLE ADD COLUMN成功: " + alterSql.substring(0, Math.min(100, alterSql.length())));
        } catch (Exception e) {
            logService.logError("admin", "ALTER_TABLE_ADD_COLUMN", "执行ALTER TABLE ADD COLUMN失败", e.getMessage());
            throw new RuntimeException("执行ALTER TABLE ADD COLUMN失败: " + e.getMessage());
        }
        
        // 重新从数据库中获取最新的字段信息（包括syncTableFields更新后的信息）
        MetadataField latestField = fieldMapper.selectByCode(field.getTableCode(), field.getFieldCode());
        
        // 如果原始校验规则有message字段，且最新的校验规则不为空，合并message字段
        if (originalMessage != null && !originalMessage.isEmpty() && latestField.getValidateRule() != null && !latestField.getValidateRule().trim().isEmpty()) {
            try {
                JSONObject latestJson = JSON.parseObject(latestField.getValidateRule());
                latestJson.put("message", originalMessage);
                String mergedValidateRule = latestJson.toJSONString();
                
                // 更新合并后的校验规则到数据库
                MetadataField updatedField = new MetadataField();
                updatedField.setId(latestField.getId());
                updatedField.setValidateRule(mergedValidateRule);
                fieldMapper.update(updatedField);
                
                // 更新当前field对象的校验规则，确保后续操作使用合并后的规则
                field.setValidateRule(mergedValidateRule);
            } catch (Exception e) {
                // 合并失败，忽略
            }
        } else if (originalMessage != null && !originalMessage.isEmpty()) {
            // 如果最新的校验规则为空，直接设置为包含message字段的规则
            try {
                JSONObject messageJson = new JSONObject();
                messageJson.put("message", originalMessage);
                String messageValidateRule = messageJson.toJSONString();
                
                // 更新包含message字段的校验规则到数据库
                MetadataField updatedField = new MetadataField();
                updatedField.setId(latestField.getId());
                updatedField.setValidateRule(messageValidateRule);
                fieldMapper.update(updatedField);
                
                // 更新当前field对象的校验规则
                field.setValidateRule(messageValidateRule);
            } catch (Exception e) {
                // 合并失败，忽略
            }
        }
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
        
        // 保存原有校验规则的message字段
        String oldMessage = null;
        if (existing.getValidateRule() != null && !existing.getValidateRule().trim().isEmpty()) {
            try {
                JSONObject oldJson = JSON.parseObject(existing.getValidateRule());
                if (oldJson.containsKey("message")) {
                    oldMessage = oldJson.getString("message");
                }
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }
        
        // 检查字段名称是否变化以及validate_rule是否变化
        boolean fieldNameChanged = !Objects.equals(existing.getFieldName(), field.getFieldName());
        boolean validateRuleChanged = !Objects.equals(existing.getValidateRule(), field.getValidateRule());
        
        // 如果字段名称变化或validate_rule变化，需要先处理CHECK约束
        if (fieldNameChanged || validateRuleChanged) {
            try {
                String tableName = codeGeneratorService.convertToTableName(field.getTableCode());
                
                // 生成旧约束名
                String oldConstraintName = "ck_" + tableName + "_" + existing.getFieldName();
                
                // 先删除旧的CHECK约束（如果存在）
                String dropOldSql = "ALTER TABLE `" + tableName + "` DROP CHECK `" + oldConstraintName + "`";
                // 执行删除旧约束的SQL语句（忽略失败，因为约束可能不存在）
                sqlExecuteService.executeSql(dropOldSql, true);
            } catch (Exception e) {
                // 记录错误日志，但不影响主流程
                logService.logError("admin", "UPDATE_CHECK_CONSTRAINT", "删除旧CHECK约束失败", e.getMessage());
            }
        }
        
        // 生成并执行ALTER TABLE语句
        try {
            String alterSql;
            String operationType;
            
            // 检查字段名称是否发生了变化
            if (fieldNameChanged) {
                // 字段名称发生了变化，执行CHANGE COLUMN语句
                alterSql = codeGeneratorService.generateAlterTableChangeColumnSQL(field.getTableCode(), existing.getFieldName(), field);
                operationType = "ALTER_TABLE_CHANGE_COLUMN";
            } else {
                // 字段名称没有变化，执行MODIFY COLUMN语句
                alterSql = codeGeneratorService.generateAlterTableModifyColumnSQL(field.getTableCode(), field);
                operationType = "ALTER_TABLE_MODIFY_COLUMN";
            }
            
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, true);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("执行" + operationType + "失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", operationType, "执行" + operationType + "成功: " + alterSql.substring(0, Math.min(100, alterSql.length())));
        } catch (Exception e) {
            logService.logError("admin", "ALTER_TABLE_OPERATION", "执行ALTER TABLE操作失败", e.getMessage());
            throw new RuntimeException("执行ALTER TABLE操作失败: " + e.getMessage());
        }
        
        // 如果字段名称变化或validate_rule变化，添加新的CHECK约束
        if (fieldNameChanged || validateRuleChanged) {
            try {
                String tableName = codeGeneratorService.convertToTableName(field.getTableCode());
                
                // 生成新的约束名
                String newConstraintName = "ck_" + tableName + "_" + field.getFieldName();
                
                // 生成删除新CHECK约束的SQL语句（以防万一，确保没有重复约束）
                String dropNewSql = "ALTER TABLE `" + tableName + "` DROP CHECK `" + newConstraintName + "`";
                // 执行删除新约束的SQL语句（忽略失败，因为约束可能不存在）
                sqlExecuteService.executeSql(dropNewSql, true);
                
                // 生成新的CHECK约束
                String checkConstraint = codeGeneratorService.generateCheckConstraint(field);
                
                // 如果有新的CHECK约束，执行添加新CHECK约束的SQL语句
                if (checkConstraint != null && !checkConstraint.isEmpty()) {
                    String addSql = "ALTER TABLE `" + tableName + "` ADD CONSTRAINT `" + newConstraintName + "` " + checkConstraint;
                    Map<String, Object> addResult = sqlExecuteService.executeSql(addSql, true);
                    if (Boolean.TRUE.equals(addResult.get("success"))) {
                        logService.logSuccess("admin", "UPDATE_CHECK_CONSTRAINT", "更新CHECK约束成功: " + newConstraintName);
                    } else {
                        logService.logError("admin", "UPDATE_CHECK_CONSTRAINT", "更新CHECK约束失败", String.valueOf(addResult.get("message")));
                    }
                }
            } catch (Exception e) {
                // 记录错误日志，但不影响主流程
                logService.logError("admin", "UPDATE_CHECK_CONSTRAINT", "更新CHECK约束失败", e.getMessage());
            }
        }
        
        // 更新数据库记录（将ALTER TABLE和CHECK约束操作放在前面，确保syncTableFields先执行）
        fieldMapper.update(field);
        logService.logSuccess("admin", "EDIT", "更新字段：" + JSON.toJSONString(field));
        
        // 重新从数据库中获取最新的字段信息（包括syncTableFields更新后的信息）
        MetadataField latestField = fieldMapper.selectByCode(field.getTableCode(), field.getFieldCode());
        
        // 如果原有校验规则有message字段，且最新的校验规则不为空，合并message字段
        if (oldMessage != null && !oldMessage.isEmpty() && latestField.getValidateRule() != null && !latestField.getValidateRule().trim().isEmpty()) {
            try {
                JSONObject latestJson = JSON.parseObject(latestField.getValidateRule());
                latestJson.put("message", oldMessage);
                String mergedValidateRule = latestJson.toJSONString();
                
                // 更新合并后的校验规则到数据库
                MetadataField updatedField = new MetadataField();
                updatedField.setId(existing.getId());
                updatedField.setValidateRule(mergedValidateRule);
                fieldMapper.update(updatedField);
                
                // 更新当前field对象的校验规则，确保后续操作使用合并后的规则
                field.setValidateRule(mergedValidateRule);
            } catch (Exception e) {
                // 合并失败，忽略
            }
        } else if (oldMessage != null && !oldMessage.isEmpty()) {
            // 如果最新的校验规则为空，直接设置为包含message字段的规则
            try {
                JSONObject messageJson = new JSONObject();
                messageJson.put("message", oldMessage);
                String messageValidateRule = messageJson.toJSONString();
                
                // 更新包含message字段的校验规则到数据库
                MetadataField updatedField = new MetadataField();
                updatedField.setId(existing.getId());
                updatedField.setValidateRule(messageValidateRule);
                fieldMapper.update(updatedField);
                
                // 更新当前field对象的校验规则
                field.setValidateRule(messageValidateRule);
            } catch (Exception e) {
                // 合并失败，忽略
            }
        }
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
        
        // 增强主键字段判断：检查formComponent或字段名为id/uuid
        boolean isPrimaryKey = "primary_key".equals(field.getFormComponent()) || "id".equals(field.getFieldName()) || "uuid".equals(field.getFieldName());
        if (isPrimaryKey) {
            throw new RuntimeException("主键字段不允许删除");
        }
        
        // 检查表中字段数量，不能删除最后一个字段
        Long fieldCount = fieldMapper.countByTableCode(field.getTableCode());
        if (fieldCount <= 1) {
            throw new RuntimeException("不能删除表中最后一个字段");
        }
        
        // 生成并执行ALTER TABLE DROP COLUMN语句
        try {
            String alterSql = codeGeneratorService.generateAlterTableDropColumnSQL(field.getTableCode(), field.getFieldName());
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, true);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("执行ALTER TABLE DROP COLUMN失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "ALTER_TABLE_DROP_COLUMN", "执行ALTER TABLE DROP COLUMN成功: " + alterSql.substring(0, Math.min(100, alterSql.length())));
        } catch (Exception e) {
            logService.logError("admin", "ALTER_TABLE_DROP_COLUMN", "执行ALTER TABLE DROP COLUMN失败", e.getMessage());
            throw new RuntimeException("执行ALTER TABLE DROP COLUMN失败: " + e.getMessage());
        }
        
        fieldMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除字段：" + JSON.toJSONString(field));
    }

    /**
     * 批量删除字段
     */
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("删除ID列表不能为空");
        }
        // 为每个ID调用单个删除方法，确保物理结构删除和日志记录正确
        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 查询表的所有字段
     */
    public List<MetadataField> listByTableCode(String tableCode) {
        return fieldMapper.selectByTableCode(tableCode);
    }

    /**
     * 分页查询表的字段
     */
    public PageResult<MetadataField> pageByTableCode(String tableCode, PageRequest pageRequest) {
        Long total = fieldMapper.countByTableCode(tableCode);
        List<MetadataField> records = fieldMapper.selectPageByTableCode(tableCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 获取表的约束列表
     */
    public List<Map<String, Object>> getConstraints(String tableCode) {
        List<MetadataField> fields = fieldMapper.selectByTableCode(tableCode);
        
        // 获取表名
        String tableName = codeGeneratorService.convertToTableName(tableCode);
        
        // 获取所有类型约束
        List<Map<String, Object>> allConstraints = fieldMapper.selectAllConstraints(tableName);
        
        // 将字段按字段名分组，便于查询
        Map<String, MetadataField> fieldMap = new java.util.HashMap<>();
        for (MetadataField field : fields) {
            fieldMap.put(field.getFieldName(), field);
        }
        
        // 构建约束结果列表
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        // 处理所有类型约束
        for (Map<String, Object> constraint : allConstraints) {
            // 获取值时使用大写键，与SQL返回的列名一致
            String constraintType = (String) constraint.get("CONSTRAINT_TYPE");
            String columnName = (String) constraint.get("COLUMN_NAME");
            String constraintName = (String) constraint.get("CONSTRAINT_NAME");
            String constraintLevel = (String) constraint.get("constraint_level");
            String constraintContent = (String) constraint.get("constraint_content");
            
            // 确保约束类型不为null
            if (constraintType == null) {
                constraintType = "UNKNOWN";
            }
            
            // 确保约束级别不为null
            if (constraintLevel == null) {
                constraintLevel = "COLUMN";
            }
            
            // 确保约束内容不为null
            if (constraintContent == null) {
                constraintContent = constraintName;
            }
            
            // 将约束级别转换为中文显示
            String constraintLevelCn = "COLUMN".equals(constraintLevel) ? "列级" : "表级";
            
            // 构建约束对象
            Map<String, Object> constraintItem = new java.util.HashMap<>();
            
            // 设置约束类型中文名称
            String constraintTypeCn = switch (constraintType) {
                case "PRIMARY KEY" -> "主键约束";
                case "FOREIGN KEY" -> "外键约束";
                case "UNIQUE" -> "唯一约束";
                case "CHECK" -> "检查约束";
                case "DEFAULT" -> "默认约束";
                default -> constraintType;
            };
            
            // 查找对应的字段
            MetadataField field = columnName != null ? fieldMap.get(columnName) : null;
            
            // 设置约束基本信息
            constraintItem.put("constraintName", constraintName);
            constraintItem.put("constraintType", constraintTypeCn);
            constraintItem.put("fieldName", columnName != null ? columnName : "");
            constraintItem.put("constraintLevel", constraintLevelCn);
            
            // 设置约束内容
            constraintItem.put("constraintContent", constraintContent);
            
            // 添加字段相关信息
            if (field != null) {
                constraintItem.put("id", field.getId());
                constraintItem.put("fieldCode", field.getFieldCode());
                constraintItem.put("tableCode", field.getTableCode());
            }
            
            result.add(constraintItem);
        }
        
        return result;
    }

    /**
     * 删除约束
     */
    @Transactional
    public void deleteConstraint(Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        MetadataField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段不存在");
        }
        
        try {
            String tableName = codeGeneratorService.convertToTableName(field.getTableCode());
            // 生成约束名
            String constraintName = "ck_" + tableName + "_" + field.getFieldName();
            
            // 删除数据库中的CHECK约束
            String dropSql = "ALTER TABLE `" + tableName + "` DROP CHECK `" + constraintName + "`";
            sqlExecuteService.executeSql(dropSql, true);
            
            // 将validate_rule设置为空
            MetadataField updateField = new MetadataField();
            updateField.setId(id);
            updateField.setValidateRule(null);
            fieldMapper.update(updateField);
            
            logService.logSuccess("admin", "DELETE_CONSTRAINT", "删除约束成功: " + constraintName);
        } catch (Exception e) {
            logService.logError("admin", "DELETE_CONSTRAINT", "删除约束失败", e.getMessage());
            throw new RuntimeException("删除约束失败: " + e.getMessage());
        }
    }
}

