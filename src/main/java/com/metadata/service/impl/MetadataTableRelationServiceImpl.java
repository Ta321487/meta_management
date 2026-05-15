package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.service.MetadataTableRelationService;
import com.metadata.service.OperationLogService;
import com.metadata.service.SqlExecuteService;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表关联关系服务实现
 */
@Service
public class MetadataTableRelationServiceImpl implements MetadataTableRelationService {

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

    @Autowired
    private BusinessCatalogResolver businessCatalogResolver;

    @Autowired
    private BusinessDataSourcePoolManager businessDataSourcePoolManager;

    /**
     * 校验主表和从表是否属于同一个业务系统
     *
     * @param mainTable  主表
     * @param slaveTable 从表
     */
    private void validateBusinessSystemConsistency(MetadataTable mainTable, MetadataTable slaveTable) {
        String mainBusinessCode = mainTable.getBusinessCode();
        String slaveBusinessCode = slaveTable.getBusinessCode();
        if (mainBusinessCode == null || mainBusinessCode.isEmpty() ||
                slaveBusinessCode == null || slaveBusinessCode.isEmpty()) {
            throw BizException.of(AppErrorCodes.RELATION_BUSINESS_SYSTEM_NOT_CONFIGURED, "主表或从表未配置业务系统");
        }
        if (!mainBusinessCode.equals(slaveBusinessCode)) {
            throw BizException.of(AppErrorCodes.RELATION_CROSS_BUSINESS_SYSTEM, "主表和从表不属于同一个业务系统");
        }
    }

    /**
     * 新增关联关系
     */
    @Override
    @Transactional
    public void add(MetadataTableRelation relation) {
        if (!CodeValidator.isValidCodes(relation.getRelationCode(), relation.getMainTableCode(),
                relation.getSlaveTableCode(), relation.getMainFieldCode(), relation.getSlaveFieldCode())) {
            throw BizException.of(AppErrorCodes.RELATION_CODE_INVALID, "编码格式不正确");
        }

        // 首先获取业务系统编码，如果relation中已有则使用，否则先从主表获取
        String businessCode = relation.getBusinessCode();
        if (businessCode == null || businessCode.isEmpty()) {
            // 先查询主表获取业务系统编码
            MetadataTable tempMainTable = tableMapper.selectByCode(relation.getMainTableCode());
            if (tempMainTable == null) {
                throw BizException.of(AppErrorCodes.RELATION_TABLE_NOT_FOUND, "主表不存在");
            }
            businessCode = tempMainTable.getBusinessCode();
        }

        // 校验表是否存在并检查启用状态
        MetadataTable mainTable = tableMapper.selectByCode(relation.getMainTableCode(), businessCode);
        if (mainTable == null) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_NOT_FOUND, "主表不存在");
        }
        if (mainTable.getIsEnabled() != 1) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_DISABLED, "主表未启用");
        }

        MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode(), businessCode);
        if (slaveTable == null) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_NOT_FOUND, "从表不存在");
        }
        if (slaveTable.getIsEnabled() != 1) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_DISABLED, "从表未启用");
        }

        // 校验字段是否存在
        if (fieldMapper.selectByCode(relation.getMainTableCode(), relation.getMainFieldCode()) == null) {
            throw BizException.of(AppErrorCodes.RELATION_FIELD_NOT_FOUND, "主表关联字段不存在");
        }
        if (fieldMapper.selectByCode(relation.getSlaveTableCode(), relation.getSlaveFieldCode()) == null) {
            throw BizException.of(AppErrorCodes.RELATION_FIELD_NOT_FOUND, "从表外键字段不存在");
        }

        // 校验主表和从表是否属于同一个业务系统
        validateBusinessSystemConsistency(mainTable, slaveTable);

        // 自动从表中获取业务系统编码
        relation.setBusinessCode(mainTable.getBusinessCode());

        // 检查关联编码是否已存在（使用正确的businessCode）
        if (relationMapper.countByCode(relation.getRelationCode(), relation.getBusinessCode()) > 0) {
            throw BizException.of(AppErrorCodes.RELATION_CODE_DUPLICATE, "关联编码已存在");
        }

        relationMapper.insert(relation);
        logService.logSuccess("admin", "ADD", "新增表关联关系：" + JSON.toJSONString(relation));
    }

    /**
     * 更新关联关系
     */
    @Override
    @Transactional
    public void update(MetadataTableRelation relation) {
        // 先获取业务系统编码，如果relation中已有则使用，否则先从现有关联中获取
        String businessCode = relation.getBusinessCode();
        MetadataTableRelation existing;

        if (businessCode == null || businessCode.isEmpty()) {
            // 先查询现有关联获取业务系统编码
            MetadataTable tempMainTable = tableMapper.selectByCode(relation.getMainTableCode());
            if (tempMainTable == null) {
                throw BizException.of(AppErrorCodes.RELATION_TABLE_NOT_FOUND, "主表不存在");
            }
            businessCode = tempMainTable.getBusinessCode();

            // 使用正确的businessCode查询关联关系
            existing = relationMapper.selectByCode(relation.getRelationCode(), businessCode);
        } else {
            // 直接使用提供的业务系统编码查询关联关系
            existing = relationMapper.selectByCode(relation.getRelationCode(), businessCode);
        }

        if (existing == null) {
            throw BizException.of(AppErrorCodes.RELATION_NOT_FOUND, "关联关系不存在");
        }

        // 检查关联关系是否发生变化（主表、从表、字段等）
        boolean relationChanged = !existing.getMainTableCode().equals(relation.getMainTableCode()) ||
                !existing.getSlaveTableCode().equals(relation.getSlaveTableCode()) ||
                !existing.getMainFieldCode().equals(relation.getMainFieldCode()) ||
                !existing.getSlaveFieldCode().equals(relation.getSlaveFieldCode());

        // 获取主表和从表信息，并检查启用状态
        MetadataTable mainTable = tableMapper.selectByCode(relation.getMainTableCode(), businessCode);
        if (mainTable == null) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_NOT_FOUND, "主表不存在");
        }
        if (mainTable.getIsEnabled() != 1) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_DISABLED, "主表未启用");
        }

        MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode(), businessCode);
        if (slaveTable == null) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_NOT_FOUND, "从表不存在");
        }
        if (slaveTable.getIsEnabled() != 1) {
            throw BizException.of(AppErrorCodes.RELATION_TABLE_DISABLED, "从表未启用");
        }

        // 如果关联关系发生变化，需要验证新的主表和从表的业务系统一致性
        if (relationChanged) {
            // 校验主表和从表是否属于同一个业务系统
            validateBusinessSystemConsistency(mainTable, slaveTable);

            try {
                // 1. 查找并删除旧的外键约束
                String oldSlaveTableName = convertToTableName(existing.getSlaveTableCode());
                MetadataField oldSlaveField = fieldMapper.selectByCode(
                        existing.getSlaveTableCode(), existing.getSlaveFieldCode());

                if (oldSlaveField != null) {
                    MetadataTable oldSlaveMeta = tableMapper.selectByCode(existing.getSlaveTableCode(), businessCode);
                    String oldSlaveCatalog = businessCatalogResolver.resolveCatalog(oldSlaveMeta);
                    // 查找旧的外键约束名称
                    String oldFkName = findForeignKeyName(oldSlaveTableName, oldSlaveField.getFieldName(), existing.getRelationCode(), oldSlaveCatalog);
                    if (oldFkName != null && !oldFkName.isEmpty()) {
                        // 删除旧的外键约束
                        String dropFkSql = "ALTER TABLE `" + oldSlaveTableName + "` DROP FOREIGN KEY `" + oldFkName + "`";
                        Map<String, Object> dropResult = sqlExecuteService.executeSql(dropFkSql, true, oldSlaveCatalog);
                        if (Boolean.TRUE.equals(dropResult.get("success"))) {
                            logService.logSuccess("admin", "DROP_FOREIGN_KEY", "删除旧外键约束: " + oldFkName);
                        } else {
                            logService.logError("admin", "DROP_FOREIGN_KEY", "删除旧外键约束失败: " + oldFkName,
                                    dropResult.get("message").toString());
                        }
                    }
                }

                // 2. 创建新的外键约束
                MetadataField mainField = fieldMapper.selectByCode(
                        relation.getMainTableCode(), relation.getMainFieldCode());
                MetadataField slaveField = fieldMapper.selectByCode(
                        relation.getSlaveTableCode(), relation.getSlaveFieldCode());

                if (mainTable != null && slaveTable != null && mainField != null && slaveField != null) {
                    MetadataTable newSlaveMeta = tableMapper.selectByCode(relation.getSlaveTableCode(), businessCode);
                    String newSlaveCatalog = businessCatalogResolver.resolveCatalog(newSlaveMeta);
                    String mainTableName = convertToTableName(relation.getMainTableCode());
                    String slaveTableName = convertToTableName(relation.getSlaveTableCode());
                    String newFkName = "fk_" + slaveTableName + "_" + slaveField.getFieldName();

                    String createFkSql = String.format(
                            "ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`) ",
                            slaveTableName, newFkName, slaveField.getFieldName(), mainTableName, mainField.getFieldName()
                    );

                    Map<String, Object> createResult = sqlExecuteService.executeSql(createFkSql, true, newSlaveCatalog);
                    if (Boolean.TRUE.equals(createResult.get("success"))) {
                        logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "创建新外键约束: " + newFkName);
                    } else {
                        logService.logError("admin", "CREATE_FOREIGN_KEY", "创建新外键约束失败: " + newFkName,
                                createResult.get("message").toString());
                    }
                }
            } catch (Exception e) {
                logService.logError("admin", "UPDATE_FOREIGN_KEY", "更新外键约束失败", e.getMessage());
            }
        }

        // 自动从表中获取业务系统编码，不允许手动修改
        if (mainTable != null) {
            relation.setBusinessCode(mainTable.getBusinessCode());
        }

        relation.setId(existing.getId());
        relationMapper.update(relation);
        logService.logSuccess("admin", "EDIT", "更新表关联关系：" + JSON.toJSONString(relation));
    }

    /**
     * 查找外键约束名称
     *
     * @param tableName    表名
     * @param columnName   字段名
     * @param relationCode 关联编码
     * @param targetCatalog 物理库名，空则主数据源
     */
    private String findForeignKeyName(String tableName, String columnName, String relationCode, String targetCatalog) {
        System.out.println("===== 开始查找外键约束名称 =====");
        System.out.println("表名: " + tableName);
        System.out.println("字段名: " + columnName);
        System.out.println("关联编码: " + relationCode);

        try (Connection connection = businessDataSourcePoolManager.getConnection(targetCatalog)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            String schema = connection.getSchema();

            System.out.println("Catalog: " + catalog);
            System.out.println("Schema: " + schema);

            try (ResultSet foreignKeys = metaData.getImportedKeys(catalog, schema, tableName)) {
                System.out.println("获取外键信息结果集");

                boolean foundAny = false;
                while (foreignKeys.next()) {
                    foundAny = true;
                    String fkName = foreignKeys.getString("FK_NAME");
                    String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                    String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                    String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");

                    System.out.println("找到外键: ");
                    System.out.println("  外键名称: " + fkName);
                    System.out.println("  外键字段: " + fkColumnName);
                    System.out.println("  主键表: " + pkTableName);
                    System.out.println("  主键字段: " + pkColumnName);

                    if (columnName.equalsIgnoreCase(fkColumnName)) {
                        System.out.println("外键字段匹配，返回外键名称: " + fkName);
                        return fkName;
                    }
                }

                if (!foundAny) {
                    System.out.println("未找到任何外键信息");
                }
            }
        } catch (Exception e) {
            System.out.println("查找外键约束名称失败: " + e.getMessage());
            e.printStackTrace();
            logService.logError("admin", "FIND_FOREIGN_KEY", "查找外键约束名称失败: " + tableName + "." + columnName, e.getMessage());
        }

        System.out.println("使用SQL查询再次检查外键是否存在");
        String checkFkSql = String.format(
                "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '%s' AND COLUMN_NAME = '%s' " +
                        "AND CONSTRAINT_NAME IS NOT NULL AND CONSTRAINT_NAME != 'PRIMARY'",
                tableName, columnName
        );
        System.out.println("执行SQL: " + checkFkSql);

        Map<String, Object> checkResult = sqlExecuteService.executeSql(checkFkSql, false, targetCatalog);
        System.out.println("SQL查询结果: " + checkResult);

        if (Boolean.TRUE.equals(checkResult.get("success"))) {
            List<Map<String, Object>> data = (List<Map<String, Object>>) checkResult.get("data");
            if (data != null && !data.isEmpty()) {
                System.out.println("SQL查询找到匹配的外键: " + data);
                return (String) data.get(0).get("CONSTRAINT_NAME");
            } else {
                System.out.println("SQL查询未找到匹配的外键");
            }
        }

        System.out.println("未找到匹配的外键约束，返回null");
        System.out.println("===== 结束查找外键约束名称 =====");
        return null;
    }

    /**
     * 删除关联关系
     */
    @Override
    @Transactional
    public void delete(Long id) {
        // 1. 先获取关联关系信息
        MetadataTableRelation relation = relationMapper.selectById(id);
        if (relation == null) {
            throw BizException.of(AppErrorCodes.RELATION_NOT_FOUND, "关联关系不存在");
        }

        // 2. 获取表和字段信息
        MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode(), relation.getBusinessCode());
        MetadataField slaveField = fieldMapper.selectByCode(relation.getSlaveTableCode(), relation.getSlaveFieldCode());

        if (slaveTable != null && slaveField != null) {
            // 3. 转换为实际表名
            String slaveTableName = convertToTableName(relation.getSlaveTableCode());
            String slaveCatalog = businessCatalogResolver.resolveCatalog(slaveTable);

            // 4. 查找外键名称
            String relationCode = relation.getRelationCode();
            String fkName = findForeignKeyName(slaveTableName, slaveField.getFieldName(), relationCode, slaveCatalog);

            // 5. 如果外键存在，删除物理外键
            if (fkName != null && !fkName.isEmpty()) {
                // 删除外键约束
                String dropFkSql = "ALTER TABLE `" + slaveTableName + "` DROP FOREIGN KEY `" + fkName + "`";
                Map<String, Object> dropResult = sqlExecuteService.executeSql(dropFkSql, true, slaveCatalog);
                if (Boolean.TRUE.equals(dropResult.get("success"))) {
                    logService.logSuccess("admin", "DROP_FOREIGN_KEY", "删除外键约束: " + fkName);
                } else {
                    logService.logError("admin", "DROP_FOREIGN_KEY", "删除外键约束失败: " + fkName,
                            dropResult.get("message").toString());
                }
            }
        }

        // 6. 删除关联关系记录
        relationMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除表关联关系ID：" + id);
    }

    /**
     * 查询主表的关联关系
     */
    @Override
    public List<MetadataTableRelation> listByMainTableCode(String mainTableCode) {
        return relationMapper.selectByMainTableCode(mainTableCode);
    }

    /**
     * 根据主表编码和业务系统编码查询关联关系
     */
    @Override
    public List<MetadataTableRelation> listByMainTableCode(String mainTableCode, String businessCode) {
        return relationMapper.selectByMainTableCode(mainTableCode, businessCode);
    }

    /**
     * 查询从表的关联关系
     */
    @Override
    public List<MetadataTableRelation> listBySlaveTableCode(String slaveTableCode) {
        return relationMapper.selectBySlaveTableCode(slaveTableCode);
    }

    /**
     * 根据从表编码和业务系统编码查询关联关系
     */
    @Override
    public List<MetadataTableRelation> listBySlaveTableCode(String slaveTableCode, String businessCode) {
        return relationMapper.selectBySlaveTableCode(slaveTableCode, businessCode);
    }

    /**
     * 查询所有关联关系
     */
    @Override
    public List<MetadataTableRelation> listAll() {
        return relationMapper.selectAll();
    }

    /**
     * 按业务系统查询所有关联关系
     */
    @Override
    public List<MetadataTableRelation> listAll(String businessCode) {
        return relationMapper.selectAll(businessCode);
    }

    /**
     * 分页查询关联关系
     */
    @Override
    public PageResult<MetadataTableRelation> page(PageRequest pageRequest) {
        Long total = relationMapper.count();
        List<MetadataTableRelation> records = relationMapper.selectPage(pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 按业务系统分页查询关联关系
     */
    @Override
    public PageResult<MetadataTableRelation> page(PageRequest pageRequest, String businessCode) {
        Long total = relationMapper.count(businessCode);
        List<MetadataTableRelation> records = relationMapper.selectPage(pageRequest, businessCode);
        return new PageResult<>(total, records);
    }

    /**
     * 创建外键约束
     *
     * @param relation 关联关系
     * @return 执行结果
     */
    @Override
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

        String businessCode = relation.getBusinessCode();
        if (businessCode == null || businessCode.isEmpty()) {
            MetadataTable temp = tableMapper.selectByCode(relation.getMainTableCode());
            if (temp != null) {
                businessCode = temp.getBusinessCode();
            }
        }

        MetadataTable mainTable = tableMapper.selectByCode(relation.getMainTableCode(), businessCode);
        if (mainTable == null) {
            result.put("success", false);
            result.put("message", "主表不存在");
            return result;
        }
        MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode(), businessCode);
        if (slaveTable == null) {
            result.put("success", false);
            result.put("message", "从表不存在");
            return result;
        }

        // 校验字段是否存在
        MetadataField mainField = fieldMapper.selectByCode(relation.getMainTableCode(), relation.getMainFieldCode());
        if (mainField == null) {
            result.put("success", false);
            result.put("message", "主表关联字段不存在");
            return result;
        }
        MetadataField slaveField = fieldMapper.selectByCode(relation.getSlaveTableCode(), relation.getSlaveFieldCode());
        if (slaveField == null) {
            result.put("success", false);
            result.put("message", "从表外键字段不存在");
            return result;
        }

        // 转换为实际表名
        String mainTableName = convertToTableName(relation.getMainTableCode());
        String slaveTableName = convertToTableName(relation.getSlaveTableCode());

        // 使用用户填写的关联编码作为外键名称
        String fkName = relation.getRelationCode();

        String slaveCatalog = businessCatalogResolver.resolveCatalog(slaveTable);

        // 检查外键是否已经存在
        String existingFkName = findForeignKeyName(slaveTableName, slaveField.getFieldName(), relation.getRelationCode(), slaveCatalog);

        if (existingFkName != null && !existingFkName.isEmpty()) {
            // 外键已经存在，直接返回成功
            result.put("success", true);
            result.put("message", "外键约束已存在: " + existingFkName);
            logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "外键约束已存在: " + existingFkName);
            return result;
        }

        // 生成创建外键的SQL
        String createFkSql = String.format(
                "ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`) ",
                slaveTableName, fkName, slaveField.getFieldName(), mainTableName, mainField.getFieldName()
        );

        // 执行SQL
        try {
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(createFkSql, true, slaveCatalog);

            if (Boolean.TRUE.equals(sqlResult.get("success"))) {
                logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "创建外键约束: " + fkName);
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
     * 根据主表、从表、主字段、从字段的组合来查找关联关系记录
     *
     * @param mainTableCode  主表编码
     * @param slaveTableCode 从表编码
     * @param mainFieldCode  主表关联字段编码
     * @param slaveFieldCode 从表外键字段编码
     * @param businessCode   业务系统编码
     * @return 关联关系对象列表
     */
    @Override
    public List<MetadataTableRelation> listByTablesAndFields(String mainTableCode, String slaveTableCode, String mainFieldCode, String slaveFieldCode, String businessCode) {
        return relationMapper.selectByTablesAndFields(mainTableCode, slaveTableCode, mainFieldCode, slaveFieldCode, businessCode);
    }

    /**
     * 根据主表、从表、主字段、从字段的组合来查找关联关系记录（默认业务系统）
     *
     * @param mainTableCode  主表编码
     * @param slaveTableCode 从表编码
     * @param mainFieldCode  主表关联字段编码
     * @param slaveFieldCode 从表外键字段编码
     * @return 关联关系对象列表
     */
    @Override
    public List<MetadataTableRelation> listByTablesAndFields(String mainTableCode, String slaveTableCode, String mainFieldCode, String slaveFieldCode) {
        return listByTablesAndFields(mainTableCode, slaveTableCode, mainFieldCode, slaveFieldCode, "DEFAULT");
    }

    /**
     * 同步元数据关联关系到数据库外键约束
     *
     * @param tableCode 表编码，如果为null或空字符串则同步所有表
     * @return 同步结果
     */
    @Override
    @Transactional
    public Map<String, Object> syncForeignKeys(String tableCode) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        int totalCreated = 0; // 总共创建的外键约束数
        List<String> messages = new ArrayList<>();

        System.out.println("===== 开始同步外键约束 =====");
        System.out.println("同步参数: tableCode = " + tableCode);

        try {
            // 获取所有关联关系记录
            List<MetadataTableRelation> relations;
            if (tableCode != null && !tableCode.trim().isEmpty()) {
                // 同步指定表的关联关系
                System.out.println("同步指定表的关联关系: " + tableCode);
                relations = relationMapper.selectBySlaveTableCode(tableCode);
            } else {
                // 同步所有关联关系
                System.out.println("同步所有关联关系");
                relations = relationMapper.selectAll();
            }

            System.out.println("共找到 " + relations.size() + " 条关联关系记录");

            for (MetadataTableRelation relation : relations) {
                System.out.println("\n处理关联关系: " + relation.getRelationCode());
                System.out.println("主表: " + relation.getMainTableCode() + ", 从表: " + relation.getSlaveTableCode());
                System.out.println("主字段: " + relation.getMainFieldCode() + ", 从字段: " + relation.getSlaveFieldCode());

                try {
                    String bc = relation.getBusinessCode();
                    MetadataTable mainTable = tableMapper.selectByCode(relation.getMainTableCode(), bc);
                    MetadataTable slaveTable = tableMapper.selectByCode(relation.getSlaveTableCode(), bc);
                    if (mainTable == null || slaveTable == null) {
                        System.out.println("表不存在，跳过关联关系");
                        failCount++;
                        messages.add("表不存在，跳过关联关系: " + relation.getRelationCode());
                        continue;
                    }

                    // 校验字段是否存在
                    MetadataField mainField = fieldMapper.selectByCode(relation.getMainTableCode(), relation.getMainFieldCode());
                    MetadataField slaveField = fieldMapper.selectByCode(relation.getSlaveTableCode(), relation.getSlaveFieldCode());
                    if (mainField == null || slaveField == null) {
                        System.out.println("字段不存在，跳过关联关系");
                        failCount++;
                        messages.add("字段不存在，跳过关联关系: " + relation.getRelationCode());
                        continue;
                    }

                    // 转换为实际表名
                    String mainTableName = convertToTableName(relation.getMainTableCode());
                    String slaveTableName = convertToTableName(relation.getSlaveTableCode());

                    System.out.println("转换后表名: 主表 = " + mainTableName + ", 从表 = " + slaveTableName);
                    System.out.println("转换后字段名: 主字段 = " + mainField.getFieldName() + ", 从字段 = " + slaveField.getFieldName());

                    // 使用用户填写的关联编码作为外键名称
                    String fkName = relation.getRelationCode();

                    String slaveCatalog = businessCatalogResolver.resolveCatalog(slaveTable);

                    // 检查外键是否已经存在
                    System.out.println("开始检查外键是否存在");
                    String existingFkName = findForeignKeyName(slaveTableName, slaveField.getFieldName(), relation.getRelationCode(), slaveCatalog);
                    System.out.println("外键检查结果: existingFkName = " + existingFkName);

                    if (existingFkName != null && !existingFkName.isEmpty()) {
                        // 外键已经存在，跳过
                        System.out.println("外键已存在，跳过");
                        continue;
                    }

                    // 生成创建外键的SQL
                    String createFkSql = String.format(
                            "ALTER TABLE `%s` ADD CONSTRAINT `%s` FOREIGN KEY (`%s`) REFERENCES `%s` (`%s`) ",
                            slaveTableName, fkName, slaveField.getFieldName(), mainTableName, mainField.getFieldName()
                    );

                    System.out.println("生成SQL: " + createFkSql);

                    // 执行SQL
                    Map<String, Object> sqlResult = sqlExecuteService.executeSql(createFkSql, true, slaveCatalog);

                    System.out.println("SQL执行结果: " + sqlResult);

                    if (Boolean.TRUE.equals(sqlResult.get("success"))) {
                        successCount++;
                        totalCreated++;
                        System.out.println("外键创建成功: " + fkName);
                        logService.logSuccess("admin", "CREATE_FOREIGN_KEY", "创建外键约束: " + fkName);
                    } else {
                        failCount++;
                        System.out.println("外键创建失败: " + sqlResult.get("message"));
                        messages.add("创建外键约束失败: " + relation.getRelationCode() + ", " + sqlResult.get("message"));
                    }
                } catch (Exception e) {
                    failCount++;
                    System.out.println("处理关联关系失败: " + e.getMessage());
                    e.printStackTrace();
                    messages.add("处理关联关系失败: " + relation.getRelationCode() + ", " + e.getMessage());
                    logService.logError("admin", "SYNC_FOREIGN_KEY", "处理关联关系失败: " + relation.getRelationCode(), e.getMessage());
                }
            }

            if (successCount > 0) {
                messages.add(0, "成功同步 " + successCount + " 个外键约束，共创建 " + totalCreated + " 个外键约束");
            }

            result.put("success", failCount == 0);
            result.put("message", String.join("; ", messages));
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCreated", totalCreated);

            System.out.println("===== 同步外键约束结束 =====");
            System.out.println("同步结果: " + result);
            System.out.println("成功数量: " + successCount);
            System.out.println("失败数量: " + failCount);
            System.out.println("总共创建: " + totalCreated);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "同步外键失败: " + e.getMessage());
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalCreated", totalCreated);

            System.out.println("===== 同步外键约束异常结束 =====");
            System.out.println("异常信息: " + e.getMessage());
            e.printStackTrace();

            logService.logError("admin", "SYNC_FOREIGN_KEY", "同步外键失败", e.getMessage());
        }

        return result;
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    private String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        System.out.println("表名转换前: " + code);
        String tableName = code.toLowerCase();
        System.out.println("表名转换后: " + tableName);
        return tableName;
    }
}