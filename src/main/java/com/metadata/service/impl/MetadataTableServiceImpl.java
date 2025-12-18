package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataModuleTableMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.service.*;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 表服务实现
 */
@Service
public class MetadataTableServiceImpl implements MetadataTableService {

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataFieldMapper fieldMapper;

    @Autowired
    private MetadataModuleTableMapper moduleTableMapper;

    @Autowired
    private OperationLogService logService;

    @Autowired
    @Lazy
    private CodeGeneratorService codeGeneratorService;

    @Autowired
    private SqlExecuteService sqlExecuteService;

    @Autowired
    @Lazy
    private MetadataTableRelationService relationService;

    @Autowired
    private MetadataTableRelationMapper relationMapper;

    /**
     * 新增表
     */
    @Override
    @Transactional
    public void add(MetadataTable table) {
        if (!CodeValidator.isValidCode(table.getTableCode())) {
            throw new RuntimeException("表编码格式不正确");
        }
        if (tableMapper.countByCode(table.getTableCode()) > 0) {
            throw new RuntimeException("表编码已存在");
        }
        // 确保businessCode不为null，使用DEFAULT作为默认值
        if (table.getBusinessCode() == null || table.getBusinessCode().isEmpty()) {
            table.setBusinessCode("DEFAULT");
        }
        // 先创建元数据记录
        tableMapper.insert(table);
        
        // 检查是否有字段配置
        List<MetadataField> fields = fieldMapper.selectByTableCode(table.getTableCode());
        
        // 如果没有字段，根据主键策略自动创建一个主键字段
            if (fields.isEmpty()) {
                MetadataField primaryKeyField = new MetadataField();
                primaryKeyField.setFieldCode("ID");
                primaryKeyField.setTableCode(table.getTableCode());
                // 根据主键策略设置不同的字段名
                if ("UUID".equals(table.getPkStrategy())) {
                    primaryKeyField.setFieldName("uuid");
                    primaryKeyField.setLabel("主键UUID");
                } else {
                    primaryKeyField.setFieldName("id");
                    primaryKeyField.setLabel("主键ID");
                }
                primaryKeyField.setSort(0);
                
                // 根据主键策略设置字段类型和表单组件
                if ("AUTO".equals(table.getPkStrategy())) {
                    // 自增主键：不需要表单组件，但在数据库层面是必填的
                    primaryKeyField.setFieldType("BIGINT");
                    primaryKeyField.setFormComponent("primary_key"); // 自增字段使用primary_key表单组件
                    primaryKeyField.setIsRequired(1); // 主键在数据库层面必须是必填的
                } else if ("UUID".equals(table.getPkStrategy())) {
                    primaryKeyField.setFieldType("VARCHAR(36)");
                    primaryKeyField.setFormComponent("primary_key"); // UUID字段使用primary_key表单组件
                    primaryKeyField.setIsRequired(1);
                    // UUID默认值通过SQL模板设置，这里不需要额外设置
                } else {
                    // 默认使用 BIGINT
                    primaryKeyField.setFieldType("BIGINT");
                    primaryKeyField.setFormComponent("primary_key"); // 其他主键使用primary_key表单组件
                    primaryKeyField.setIsRequired(1);
                }   
                // 启用主键字段
                primaryKeyField.setIsEnabled(1);
                // 设置业务系统编码，与表保持一致
                primaryKeyField.setBusinessCode(table.getBusinessCode());
                
                // 插入主键字段
            fieldMapper.insert(primaryKeyField);
            logService.logSuccess("admin", "AUTO_CREATE_PK_FIELD", "自动创建主键字段: " + table.getTableCode());
        }
        
        // 生成并执行CREATE TABLE SQL
        try {
            String createTableSql = codeGeneratorService.generateCreateTableSQL(table.getTableCode());
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(createTableSql);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw new RuntimeException("创建数据库表失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "CREATE_TABLE_SQL", "执行CREATE TABLE SQL成功: " + table.getTableCode());
        } catch (Exception e) {
            // SQL执行失败，记录日志并抛出异常
            logService.logError("admin", "CREATE_TABLE_SQL", "执行CREATE TABLE SQL失败: " + table.getTableCode(), e.getMessage());
            throw new RuntimeException("创建数据库表失败: " + e.getMessage());
        }
        
        logService.logSuccess("admin", "ADD", "新增表：" + JSON.toJSONString(table));
    }

    /**
     * 更新表
     */
    @Override
    @Transactional
    public void update(MetadataTable table) {
        MetadataTable existing = tableMapper.selectById(table.getId());
        if (existing == null) {
            throw new RuntimeException("表不存在");
        }
        table.setTableCode(existing.getTableCode()); // 编码不可修改
        
        // 检查主键生成策略是否变更
        if (table.getPkStrategy() != null && !existing.getPkStrategy().equals(table.getPkStrategy())) {
            throw new RuntimeException("主键生成策略不允许修改，请删除表后重新创建");
        }
        
        // 检查业务系统是否变更
        boolean businessSystemChanged = table.getBusinessCode() != null && !table.getBusinessCode().equals(existing.getBusinessCode());
        
        // 更新元数据记录
        tableMapper.update(table);
        
        // 如果业务系统发生变化，更新表的所有字段的业务系统
        if (businessSystemChanged) {
            // 调用updateTableBusinessSystem方法更新表和字段的业务系统
            updateTableBusinessSystem(table.getTableCode(), table.getBusinessCode());
        }
        
        // 如果表名或描述发生变化，更新数据库表的注释（通过ALTER TABLE COMMENT）
        // 注意：这里只更新注释，不执行其他ALTER TABLE操作（如DROP COLUMN等危险操作）
        if (!existing.getTableName().equals(table.getTableName()) || 
            (existing.getDescription() != null && !existing.getDescription().equals(table.getDescription())) ||
            (table.getDescription() != null && !table.getDescription().equals(existing.getDescription()))) {
            try {
                String tableName = convertToTableName(table.getTableCode());
                String comment = table.getTableName();
                if (table.getDescription() != null && !table.getDescription().trim().isEmpty()) {
                    comment = table.getDescription();
                }
                // 生成ALTER TABLE COMMENT语句（安全操作，不包含DROP）
                String alterSql = String.format("ALTER TABLE `%s` COMMENT = '%s'", tableName, escapeSqlString(comment));
                Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql);
                if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                    // 更新注释失败不影响元数据更新，只记录日志
                    logService.logError("admin", "UPDATE_TABLE_COMMENT", "更新表注释失败: " + table.getTableCode(), 
                        sqlResult.get("message").toString());
                } else {
                    logService.logSuccess("admin", "UPDATE_TABLE_COMMENT", "更新表注释成功: " + table.getTableCode());
                }
            } catch (Exception e) {
                // 更新注释失败不影响元数据更新，只记录日志
                logService.logError("admin", "UPDATE_TABLE_COMMENT", "更新表注释失败: " + table.getTableCode(), e.getMessage());
            }
        }
        
        logService.logSuccess("admin", "EDIT", "更新表：" + JSON.toJSONString(table));
    }

    /**
     * 删除表
     */
    @Override
    @Transactional
    public void delete(Long id) {
        MetadataTable table = tableMapper.selectById(id);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        String tableCode = table.getTableCode();
        String tableName = convertToTableName(tableCode);
        
        // 先执行DROP TABLE删除实际数据库表
        try {
            Map<String, Object> dropResult = sqlExecuteService.executeDropTable(tableName);
            if (Boolean.TRUE.equals(dropResult.get("success"))) {
                logService.logSuccess("admin", "DROP_TABLE", "删除数据库表成功: " + tableCode);
            } else {
                // DROP TABLE失败不影响元数据删除，只记录日志
                logService.logError("admin", "DROP_TABLE", "删除数据库表失败: " + tableCode, 
                    dropResult.get("message").toString());
            }
        } catch (Exception e) {
            // DROP TABLE失败不影响元数据删除，只记录日志
            logService.logError("admin", "DROP_TABLE", "删除数据库表失败: " + tableCode, e.getMessage());
        }
        
        // 删除字段
        fieldMapper.deleteByTableCode(tableCode);
        // 删除模块关联
        moduleTableMapper.deleteByTableCode(tableCode);
        // 删除表元数据
        tableMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除表：" + tableCode);
    }

    /**
     * 批量删除表
     */
    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("删除ID列表不能为空");
        }
        // 为每个ID调用单个删除方法，确保物理结构删除和关联关系处理正确
        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 查询表详情
     */
    @Override
    public MetadataTable getByCode(String tableCode) {
        return tableMapper.selectByCode(tableCode);
    }

    /**
     * 查询所有表
     */
    @Override
    public List<MetadataTable> list(String tableName, String businessCode) {
        return tableMapper.selectAll(tableName, businessCode);
    }

    /**
     * 分页查询表
     */
    @Override
    public PageResult<MetadataTable> page(String tableName, String businessCode, PageRequest pageRequest) {
        Long total = tableMapper.count(tableName, businessCode);
        List<MetadataTable> records = tableMapper.selectPage(tableName, businessCode, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 根据模块编码查询表
     */
    @Override
    public List<MetadataTable> listByModuleCode(String moduleCode, String businessCode) {
        // 1. 获取模块直接关联的表
        List<MetadataTable> directTables = tableMapper.selectByModuleCode(moduleCode, businessCode);
        
        // 2. 如果没有直接关联的表，直接返回空列表
        if (directTables == null || directTables.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 创建一个Set用于存储所有相关表的编码，确保唯一性
        Set<String> tableCodeSet = new HashSet<>();
        List<MetadataTable> allTables = new ArrayList<>();
        
        // 4. 添加直接关联的表到结果列表和Set中
        for (MetadataTable table : directTables) {
            allTables.add(table);
            tableCodeSet.add(table.getTableCode());
        }
        
        // 5. 遍历直接关联的表，查找与它们有外键关联的其他表
        for (MetadataTable table : directTables) {
            String currentTableCode = table.getTableCode();
            
            // 获取当前表作为主表的所有关联关系（主表 -> 从表）
            List<MetadataTableRelation> mainRelations = relationService.listByMainTableCode(currentTableCode, businessCode);
            for (MetadataTableRelation relation : mainRelations) {
                String slaveTableCode = relation.getSlaveTableCode();
                if (!tableCodeSet.contains(slaveTableCode)) {
                    // 传递业务系统编码，确保只返回当前业务系统的表
                    MetadataTable slaveTable = tableMapper.selectByCode(slaveTableCode, businessCode);
                    if (slaveTable != null) {
                        allTables.add(slaveTable);
                        tableCodeSet.add(slaveTableCode);
                    }
                }
            }
            
            // 获取当前表作为从表的所有关联关系（从表 <- 主表）
            List<MetadataTableRelation> slaveRelations = relationService.listBySlaveTableCode(currentTableCode, businessCode);
            for (MetadataTableRelation relation : slaveRelations) {
                String mainTableCode = relation.getMainTableCode();
                if (!tableCodeSet.contains(mainTableCode)) {
                    // 传递业务系统编码，确保只返回当前业务系统的表
                    MetadataTable mainTable = tableMapper.selectByCode(mainTableCode, businessCode);
                    if (mainTable != null) {
                        allTables.add(mainTable);
                        tableCodeSet.add(mainTableCode);
                    }
                }
            }
        }
        
        return allTables;
    }

    /**
     * 更新表的业务系统
     */
    @Override
    @Transactional
    public void updateTableBusinessSystem(String tableCode, String businessCode) {
        // 获取表信息
        MetadataTable table = tableMapper.selectByCode(tableCode);
        if (table == null) {
            throw new RuntimeException("表不存在");
        }
        
        // 更新表的业务系统
        table.setBusinessCode(businessCode);
        tableMapper.update(table);
        
        // 更新表关联的所有字段的业务系统
        fieldMapper.updateFieldsBusinessSystemByTable(tableCode, businessCode);
        
        // 更新该表作为主表的所有关联关系的业务系统编码
        relationMapper.updateRelationBusinessSystemByMainTable(tableCode, businessCode);
        
        // 更新该表作为从表的所有关联关系的业务系统编码
        relationMapper.updateRelationBusinessSystemBySlaveTable(tableCode, businessCode);
        
        logService.logSuccess("admin", "EDIT", "更新表业务系统：" + tableCode + " -> " + businessCode);
    }

    /**
     * 批量更新表的业务系统
     */
    @Override
    @Transactional
    public void batchUpdateTableBusinessSystem(List<String> tableCodes, String businessCode) {
        if (tableCodes == null || tableCodes.isEmpty()) {
            throw new RuntimeException("表编码列表不能为空");
        }
        
        for (String tableCode : tableCodes) {
            updateTableBusinessSystem(tableCode, businessCode);
        }
    }

    /**
     * 批量获取表列表
     */
    @Override
    public List<MetadataTable> getTablesByCodes(List<String> tableCodes) {
        if (tableCodes == null || tableCodes.isEmpty()) {
            return List.of();
        }
        return tableMapper.selectByCodes(tableCodes);
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    private String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        return code.toLowerCase().replace("_TABLE", "");
    }

    /**
     * 工具方法：转义SQL字符串中的单引号
     */
    private String escapeSqlString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''");
    }
}