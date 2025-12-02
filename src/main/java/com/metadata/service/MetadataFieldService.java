package com.metadata.service;

import com.alibaba.fastjson2.JSON;
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
        
        // 如果validate_rule变化，添加新的CHECK约束
        if (validateRuleChanged) {
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
}

