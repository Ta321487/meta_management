package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private SqlExecuteService sqlExecuteService;
    
    @Autowired
    private DataSource dataSource;

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
        
        // 检查关联关系是否发生变化（主表、从表、字段等）
        boolean relationChanged = !existing.getMainTableCode().equals(relation.getMainTableCode()) ||
                                  !existing.getSlaveTableCode().equals(relation.getSlaveTableCode()) ||
                                  !existing.getMainFieldCode().equals(relation.getMainFieldCode()) ||
                                  !existing.getSlaveFieldCode().equals(relation.getSlaveFieldCode());
        
        // 如果关联关系发生变化，需要同步更新物理表的外键约束
        if (relationChanged) {
            try {
                // 1. 查找并删除旧的外键约束
                String oldSlaveTableName = convertToTableName(existing.getSlaveTableCode());
                com.metadata.entity.MetadataField oldSlaveField = fieldMapper.selectByCode(
                    existing.getSlaveTableCode(), existing.getSlaveFieldCode());
                
                if (oldSlaveField != null) {
                    String oldFkName = findForeignKeyName(oldSlaveTableName, oldSlaveField.getFieldName());
                    if (oldFkName != null && !oldFkName.isEmpty()) {
                        // 删除旧的外键约束
                        String dropFkSql = "ALTER TABLE `" + oldSlaveTableName + "` DROP FOREIGN KEY `" + oldFkName + "`";
                        Map<String, Object> dropResult = sqlExecuteService.executeSql(dropFkSql, true);
                        if (Boolean.TRUE.equals(dropResult.get("success"))) {
                            logService.logSuccess("admin", "DROP_FOREIGN_KEY", "删除旧外键约束: " + oldFkName);
                        } else {
                            logService.logError("admin", "DROP_FOREIGN_KEY", "删除旧外键约束失败: " + oldFkName, 
                                dropResult.get("message").toString());
                        }
                    }
                }
                
                // 2. 创建新的外键约束
                com.metadata.entity.MetadataTable mainTable = tableMapper.selectByCode(relation.getMainTableCode());
                com.metadata.entity.MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode());
                com.metadata.entity.MetadataField mainField = fieldMapper.selectByCode(
                    relation.getMainTableCode(), relation.getMainFieldCode());
                com.metadata.entity.MetadataField slaveField = fieldMapper.selectByCode(
                    relation.getSlaveTableCode(), relation.getSlaveFieldCode());
                
                if (mainTable != null && slaveTable != null && mainField != null && slaveField != null) {
                    String mainTableName = convertToTableName(relation.getMainTableCode());
                    String slaveTableName = convertToTableName(relation.getSlaveTableCode());
                    String newFkName = "fk_" + slaveTableName + "_" + slaveField.getFieldName();
                    
                    String createFkSql = String.format(
                        "ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`) ",
                        slaveTableName, newFkName, slaveField.getFieldName(), mainTableName, mainField.getFieldName()
                    );
                    
                    Map<String, Object> createResult = sqlExecuteService.executeSql(createFkSql, true);
                    if (Boolean.TRUE.equals(createResult.get("success"))) {
                        logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "创建新外键约束: " + newFkName);
                    } else {
                        logService.logError("admin", "CREATE_FOREIGN_KEY", "创建新外键约束失败: " + newFkName, 
                            createResult.get("message").toString());
                        // 外键创建失败不影响元数据更新，只记录日志
                    }
                }
            } catch (Exception e) {
                // 外键更新失败不影响元数据更新，只记录日志
                logService.logError("admin", "UPDATE_FOREIGN_KEY", "更新外键约束失败", e.getMessage());
            }
        }
        
        relation.setId(existing.getId());
        relationMapper.update(relation);
        logService.logSuccess("admin", "EDIT", "更新表关联关系：" + JSON.toJSONString(relation));
    }
    
    /**
     * 查找外键约束名称
     * @param tableName 表名
     * @param columnName 字段名
     * @return 外键约束名称，如果不存在则返回null
     */
    private String findForeignKeyName(String tableName, String columnName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();
            
            try (java.sql.ResultSet foreignKeys = metaData.getImportedKeys(catalog, schema, tableName)) {
                while (foreignKeys.next()) {
                    String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                    if (columnName.equalsIgnoreCase(fkColumnName)) {
                        String fkName = foreignKeys.getString("FK_NAME");
                        return fkName;
                    }
                }
            }
        } catch (Exception e) {
            logService.logError("admin", "FIND_FOREIGN_KEY", "查找外键约束名称失败: " + tableName + "." + columnName, e.getMessage());
        }
        return null;
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

    /**
     * 分页查询关联关系
     */
    public PageResult<MetadataTableRelation> page(PageRequest pageRequest) {
        Long total = relationMapper.count();
        List<MetadataTableRelation> records = relationMapper.selectPage(pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 创建外键约束
     * @param relation 关联关系
     * @return 执行结果
     */
    @Transactional
    public Map<String, Object> createForeignKey(MetadataTableRelation relation) {
        Map<String, Object> result = new HashMap<>();
        
        // 校验关联关系
        if (!CodeValidator.isValidCodes(relation.getMainTableCode(), relation.getSlaveTableCode(),
                relation.getMainFieldCode(), relation.getSlaveFieldCode())) {
            result.put("success", false);
            result.put("message", "编码格式不正确");
            return result;
        }
        
        // 校验表是否存在
        com.metadata.entity.MetadataTable mainTable = tableMapper.selectByCode(relation.getMainTableCode());
        if (mainTable == null) {
            result.put("success", false);
            result.put("message", "主表不存在");
            return result;
        }
        
        com.metadata.entity.MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode());
        if (slaveTable == null) {
            result.put("success", false);
            result.put("message", "从表不存在");
            return result;
        }
        
        // 校验字段是否存在
        com.metadata.entity.MetadataField mainField = fieldMapper.selectByCode(relation.getMainTableCode(), relation.getMainFieldCode());
        if (mainField == null) {
            result.put("success", false);
            result.put("message", "主表关联字段不存在");
            return result;
        }
        
        com.metadata.entity.MetadataField slaveField = fieldMapper.selectByCode(relation.getSlaveTableCode(), relation.getSlaveFieldCode());
        if (slaveField == null) {
            result.put("success", false);
            result.put("message", "从表外键字段不存在");
            return result;
        }
        
        // 转换为实际表名
        String mainTableName = convertToTableName(relation.getMainTableCode());
        String slaveTableName = convertToTableName(relation.getSlaveTableCode());
        
        // 生成外键名称
        String fkName = "fk_" + slaveTableName + "_" + slaveField.getFieldName();
        
        // 生成创建外键的SQL
        String createFkSql = String.format(
            "ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`)",
            slaveTableName, fkName, slaveField.getFieldName(), mainTableName, mainField.getFieldName()
        );
        
        // 执行SQL
        try {
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(createFkSql, true);
            if (Boolean.TRUE.equals(sqlResult.get("success"))) {
                // 如果关联关系不存在，自动创建
                if (relation.getRelationCode() == null || relation.getRelationCode().isEmpty()) {
                    String relationCode = "REL_" + relation.getMainTableCode() + "_" + relation.getSlaveTableCode() + "_" + 
                                         relation.getMainFieldCode() + "_" + relation.getSlaveFieldCode();
                    relation.setRelationCode(relationCode);
                }
                
                if (relationMapper.countByCode(relation.getRelationCode()) == 0) {
                    if (relation.getRelationName() == null || relation.getRelationName().isEmpty()) {
                        relation.setRelationName(mainTable.getTableName() + " -> " + slaveTable.getTableName());
                    }
                    if (relation.getRelationType() == null || relation.getRelationType().isEmpty()) {
                        relation.setRelationType("ONE_TO_MANY");
                    }
                    relationMapper.insert(relation);
                    logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "创建外键约束并关联关系: " + relation.getRelationCode());
                } else {
                    logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "创建外键约束: " + fkName);
                }
                
                result.put("success", true);
                result.put("message", "创建外键约束成功");
            } else {
                result.put("success", false);
                result.put("message", "创建外键约束失败: " + sqlResult.get("message"));
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建外键约束失败: " + e.getMessage());
            logService.logError("admin", "CREATE_FOREIGN_KEY", "创建外键约束失败", e.getMessage());
        }
        
        return result;
    }

    /**
     * 同步数据库外键到元数据关联关系系统
     * @param tableCode 表编码，如果为null或空字符串则同步所有表
     * @return 同步结果
     */
    @Transactional
    public Map<String, Object> syncForeignKeys(String tableCode) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "SqlExecuteService 中未定义 syncForeignKeys 方法，同步功能暂不可用");
        return result;
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    private String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        return code.toLowerCase().replace("_TABLE", "");
    }
}

