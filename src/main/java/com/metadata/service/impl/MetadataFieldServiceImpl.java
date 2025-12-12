package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.service.CodeGeneratorService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.OperationLogService;
import com.metadata.service.SqlExecuteService;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 字段服务实现
 */
@Service
public class MetadataFieldServiceImpl implements MetadataFieldService {

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private OperationLogService logService;

    @Autowired
    @Lazy
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private SqlExecuteService sqlExecuteService;

    @Autowired
    private MetadataTableRelationMapper relationMapper;
    
    @Autowired
    private MetadataTableMapper tableMapper;

    /**
     * 从校验规则中提取message字段
     */
    private String extractMessageFromValidateRule(String validateRule) {
        if (validateRule == null || validateRule.trim().isEmpty()) {
            return null;
        }
        try {
            JSONObject json = JSON.parseObject(validateRule);
            return json.getString("message");
        } catch (Exception e) {
            // 解析失败，忽略
            return null;
        }
    }

    /**
     * 将message字段合并到最新的校验规则中
     */
    private void mergeMessageIntoValidateRule(MetadataField field, MetadataField latestField, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        try {
            String mergedValidateRule;
            if (latestField.getValidateRule() != null && !latestField.getValidateRule().trim().isEmpty()) {
                // 最新的校验规则不为空，合并message字段
                JSONObject latestJson = JSON.parseObject(latestField.getValidateRule());
                latestJson.put("message", message);
                mergedValidateRule = latestJson.toJSONString();
            } else {
                // 最新的校验规则为空，直接设置为包含message字段的规则
                JSONObject messageJson = new JSONObject();
                messageJson.put("message", message);
                mergedValidateRule = messageJson.toJSONString();
            }

            // 更新合并后的校验规则到数据库
            MetadataField updatedField = new MetadataField();
            updatedField.setId(latestField.getId());
            updatedField.setValidateRule(mergedValidateRule);
            fieldMapper.update(updatedField);

            // 更新当前field对象的校验规则
            field.setValidateRule(mergedValidateRule);
        } catch (Exception e) {
            // 合并失败，忽略
        }
    }

    /**
     * 新增字段
     */
    @Override
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
        // 确保businessCode不为null，如果没有提供则从表中获取
        if (field.getBusinessCode() == null || field.getBusinessCode().isEmpty()) {
            // 从表中获取业务系统编码
            MetadataField existingField = fieldMapper.selectByTableCode(field.getTableCode()).stream().findFirst().orElse(null);
            if (existingField != null) {
                String existingBusinessCode = existingField.getBusinessCode();
                // 确保从现有字段获取的businessCode不为null或空字符串
                if (existingBusinessCode == null || existingBusinessCode.isEmpty()) {
                    field.setBusinessCode("DEFAULT");
                } else {
                    field.setBusinessCode(existingBusinessCode);
                }
            } else {
                // 如果没有现有字段，使用默认业务系统编码
                field.setBusinessCode("DEFAULT");
            }
        }

        // 保存原始校验规则的message字段
        String originalMessage = extractMessageFromValidateRule(field.getValidateRule());

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

        // 合并message字段到最新的校验规则中
        mergeMessageIntoValidateRule(field, latestField, originalMessage);
    }

    /**
     * 更新字段
     */
    @Override
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
        String oldMessage = extractMessageFromValidateRule(existing.getValidateRule());

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

        // 合并message字段到最新的校验规则中
        mergeMessageIntoValidateRule(field, latestField, oldMessage);
    }

    /**
     * 删除字段
     */
    @Override
    @Transactional
    public void delete(Long id) {
        MetadataField field = fieldMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段不存在");
        }

        // 增强主键字段判断：检查formComponent或字段名为id/uuid
        boolean isPrimaryKey = "primary_key".equals(field.getFormComponent()) || "id".equals(field.getFieldName()) || "uuid".equals(field.getFieldName());

        // 新增：检查字段是否是数据库中的主键约束字段
        if (!isPrimaryKey) {
            // 获取表的所有约束信息
            List<Map<String, Object>> constraints = getConstraints(field.getTableCode());
            for (Map<String, Object> constraint : constraints) {
                // 获取约束类型和字段名
                String constraintType = (String) constraint.get("constraintType");
                String columnName = (String) constraint.get("fieldName");

                // 检查是否是主键约束且字段名匹配
                if ("主键约束".equals(constraintType) && field.getFieldName().equals(columnName)) {
                    isPrimaryKey = true;
                    break;
                }
            }
        }

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
    @Override
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
    @Override
    public List<MetadataField> listByTableCode(String tableCode) {
        return fieldMapper.selectByTableCode(tableCode);
    }

    /**
     * 分页查询表的字段
     */
    @Override
    public PageResult<MetadataField> pageByTableCode(String tableCode, PageRequest pageRequest) {
        Long total = fieldMapper.countByTableCode(tableCode);
        List<MetadataField> records = fieldMapper.selectPageByTableCode(tableCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 获取表的约束列表
     */
    @Override
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

            // 设置表编码
            constraintItem.put("tableCode", tableCode);

            // 添加字段相关信息
            if (field != null) {
                constraintItem.put("id", field.getId());
                constraintItem.put("fieldCode", field.getFieldCode());
            }

            result.add(constraintItem);
        }

        return result;
    }

    /**
     * 删除约束
     */
    @Override
    @Transactional
    public void deleteConstraint(Map<String, Object> params) {
        // 检查参数中的id是否存在
        Object idObj = params.get("id");
        MetadataField field = null;
        String tableName = null;
        String constraintName = null;
        String fieldName = null;
        String tableCode = null;

        // 优先从参数中获取约束名
        constraintName = (String) params.get("constraintName");

        if (idObj != null) {
            // 如果有id，则通过id获取字段信息
            Long id = Long.valueOf(idObj.toString());
            field = fieldMapper.selectById(id);
            if (field == null) {
                throw new RuntimeException("字段不存在");
            }
            tableCode = field.getTableCode();
            tableName = codeGeneratorService.convertToTableName(tableCode);
            fieldName = field.getFieldName();

            // 如果没有约束名，则生成默认的检查约束名
            if (constraintName == null) {
                constraintName = "ck_" + tableName + "_" + fieldName;
            }
        } else {
            // 如果没有id，则尝试从参数中获取其他信息
            tableName = (String) params.get("tableName");
            tableCode = (String) params.get("tableCode");
            fieldName = (String) params.get("fieldName");

            // 如果参数中没有表名，则通过表编码转换
            if (tableName == null && tableCode != null) {
                tableName = codeGeneratorService.convertToTableName(tableCode);
            }

            // 如果没有约束名，则生成默认的检查约束名
            if (constraintName == null && tableName != null && fieldName != null) {
                constraintName = "ck_" + tableName + "_" + fieldName;
            }

            // 如果有tableCode和fieldName，则通过tableCode和fieldName获取字段信息
            if (tableCode != null && fieldName != null) {
                List<MetadataField> fields = fieldMapper.selectByTableCode(tableCode);
                if (fields != null && !fields.isEmpty()) {
                    for (MetadataField f : fields) {
                        if (fieldName.equals(f.getFieldName())) {
                            field = f;
                            break;
                        }
                    }
                }
            }
        }

        // 验证必要参数
        if (tableName == null || constraintName == null) {
            throw new RuntimeException("缺少必要的约束信息");
        }

        try {
            // 确定约束类型：先查询数据库获取实际约束类型
            String constraintType = null;
            // 直接拼接SQL，确保表名和约束名的安全性
            String querySql = "SELECT constraint_type FROM information_schema.table_constraints " +
                    "WHERE table_schema = DATABASE() AND table_name = '" + tableName + "' AND constraint_name = '" + constraintName + "'";
            
            // 执行查询获取约束类型
            Map<String, Object> typeResult = sqlExecuteService.executeSql(querySql, false);
            if (typeResult != null && Boolean.TRUE.equals(typeResult.get("success"))) {
                List<Map<String, Object>> rows = (List<Map<String, Object>>) typeResult.get("data");
                if (rows != null && !rows.isEmpty()) {
                    constraintType = (String) rows.get(0).get("CONSTRAINT_TYPE");
                }
            }
            
            // 如果查询失败或未获取到约束类型，则使用原有的约束名前缀判断逻辑
            if (constraintType == null) {
                if (constraintName.equalsIgnoreCase("PRIMARY")) {
                    constraintType = "PRIMARY KEY";
                } else if (constraintName.toUpperCase().startsWith("FK_")) {
                    constraintType = "FOREIGN KEY";
                } else if (constraintName.toUpperCase().startsWith("CK_")) {
                    constraintType = "CHECK";
                } else if (constraintName.toUpperCase().startsWith("UK_")) {
                    constraintType = "UNIQUE";
                } else {
                    // 默认为CHECK约束
                    constraintType = "CHECK";
                }
            }

            // 禁止删除主键约束
            if (constraintType.equals("PRIMARY KEY")) {
                throw new RuntimeException("主键约束不能被直接删除，若要修改主键请重新设计表结构");
            }

            // 构建删除约束的SQL语句
            String dropSql = "ALTER TABLE `" + tableName + "` DROP " + constraintType + " `" + constraintName + "`";

            // 删除数据库中的约束
            Map<String, Object> result = sqlExecuteService.executeSql(dropSql, true);

            // 如果有字段信息，则将validate_rule设置为空
            if (field != null) {
                // 查询完整的字段信息，避免更新时丢失其他字段值
                MetadataField fullField = fieldMapper.selectById(field.getId());
                if (fullField != null) {
                    MetadataField updateField = new MetadataField();
                    updateField.setId(fullField.getId());
                    updateField.setFieldCode(fullField.getFieldCode());
                    updateField.setTableCode(fullField.getTableCode());
                    updateField.setFieldName(fullField.getFieldName());
                    updateField.setFieldType(fullField.getFieldType());
                    updateField.setLabel(fullField.getLabel());
                    updateField.setIsRequired(fullField.getIsRequired());
                    updateField.setFormComponent(fullField.getFormComponent());
                    updateField.setValidateRule(null);
                    updateField.setSort(fullField.getSort());
                    updateField.setIsEnabled(fullField.getIsEnabled());
                    updateField.setBusinessCode(fullField.getBusinessCode());
                    fieldMapper.update(updateField);
                }
            }

            logService.logSuccess("admin", "DELETE_CONSTRAINT", "删除约束成功: " + constraintName);

            // 如果删除的是外键约束，同时删除对应的关联关系记录
            if (constraintType.equals("FOREIGN KEY")) {
                // 获取表的业务系统编码
                String businessCode = "DEFAULT";
                if (tableCode != null) {
                    // 如果有tableCode，直接查询表的业务系统编码
                    MetadataTable table = tableMapper.selectByCode(tableCode);
                    if (table != null) {
                        businessCode = table.getBusinessCode();
                    }
                } else if (tableName != null) {
                    // 否则，通过表名查询表的业务系统编码
                    List<MetadataTable> tables = tableMapper.selectAll(null);
                    for (MetadataTable t : tables) {
                        if (tableName.equalsIgnoreCase(codeGeneratorService.convertToTableName(t.getTableCode())) || 
                            tableName.equalsIgnoreCase(t.getTableCode())) {
                            businessCode = t.getBusinessCode();
                            break;
                        }
                    }
                }
                
                // 根据外键约束名（即关联编码）查找并删除对应的关联关系记录
                // 使用正确的业务系统编码
                MetadataTableRelation relation = relationMapper.selectByCode(constraintName, businessCode);
                if (relation != null) {
                    relationMapper.deleteById(relation.getId());
                    logService.logSuccess("admin", "DELETE", "删除关联关系: " + relation.getRelationCode());
                } else {
                    // 如果找不到，尝试使用所有业务系统编码查找
                    List<MetadataTableRelation> allRelations = relationMapper.selectAll();
                    for (MetadataTableRelation rel : allRelations) {
                        if (constraintName.equals(rel.getRelationCode())) {
                            relationMapper.deleteById(rel.getId());
                            logService.logSuccess("admin", "DELETE", "删除关联关系: " + rel.getRelationCode());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logService.logError("admin", "DELETE_CONSTRAINT", "删除约束失败", e.getMessage());
            throw new RuntimeException("删除约束失败: " + e.getMessage());
        }
    }

    /**
     * 更新字段的业务系统
     */
    @Override
    @Transactional
    public void updateFieldsBusinessSystemByTable(String tableCode, String businessCode) {
        fieldMapper.updateFieldsBusinessSystemByTable(tableCode, businessCode);
        logService.logSuccess("admin", "EDIT", "更新表字段业务系统：" + tableCode + " -> " + businessCode);
    }

    /**
     * 批量更新字段的业务系统
     */
    @Override
    @Transactional
    public void batchUpdateFieldsBusinessSystem(List<String> tableCodes, String businessCode) {
        if (tableCodes == null || tableCodes.isEmpty()) {
            throw new RuntimeException("表编码列表不能为空");
        }

        fieldMapper.batchUpdateFieldsBusinessSystem(tableCodes, businessCode);
        logService.logSuccess("admin", "EDIT", "批量更新表字段业务系统：" + tableCodes + " -> " + businessCode);
    }
}