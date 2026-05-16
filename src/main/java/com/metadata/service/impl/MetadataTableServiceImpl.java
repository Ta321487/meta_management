package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataPhysicalDatabase;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.mapper.MetadataFieldMapper;
import com.metadata.mapper.MetadataModuleTableMapper;
import com.metadata.mapper.MetadataPhysicalDatabaseMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.service.*;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    private MetadataBusinessSystemService businessSystemService;

    @Autowired
    private BusinessCatalogResolver businessCatalogResolver;

    @Autowired
    private MySqlPhysicalCatalogService mySqlPhysicalCatalogService;

    @Autowired
    @Lazy
    private MetadataTableRelationService relationService;

    @Autowired
    private MetadataTableRelationMapper relationMapper;

    @Autowired
    private MetadataPhysicalDatabaseMapper physicalDatabaseMapper;

    @Autowired
    private BusinessDataSourcePoolManager businessDataSourcePoolManager;

    @Autowired
    private MetadataSyncService metadataSyncService;

    /**
     * 新增表
     */
    @Override
    @Transactional
    public void add(MetadataTable table) {
        if (!CodeValidator.isValidCode(table.getTableCode())) {
            throw BizException.of(AppErrorCodes.TABLE_CODE_INVALID, "表编码格式不正确");
        }
        if (tableMapper.countByCode(table.getTableCode()) > 0) {
            throw BizException.of(AppErrorCodes.TABLE_CODE_DUPLICATE, "表编码已存在");
        }
        // 确保businessCode不为null，使用DEFAULT作为默认值
        if (table.getBusinessCode() == null || table.getBusinessCode().isEmpty()) {
            table.setBusinessCode("DEFAULT");
        }
        MetadataBusinessSystem businessSystem = businessSystemService.getByCode(table.getBusinessCode());
        if (businessSystem != null && businessSystem.getIsEnabled() != null && businessSystem.getIsEnabled() == 0) {
            throw BizException.badRequest("业务系统已停用，无法新增表");
        }
        // 表级未指定物理库时，继承业务系统默认库名
        if (businessSystem != null && (table.getDatabaseName() == null || table.getDatabaseName().trim().isEmpty())
                && businessSystem.getDatabaseName() != null && !businessSystem.getDatabaseName().trim().isEmpty()) {
            table.setDatabaseName(businessSystem.getDatabaseName().trim());
        }
        if (table.getDatabaseName() != null && StringUtils.hasText(table.getDatabaseName())) {
            MetadataPhysicalDatabase pdb = physicalDatabaseMapper.selectByCatalogName(table.getDatabaseName().trim());
            if (pdb != null && pdb.getIsEnabled() != null && pdb.getIsEnabled() == 0) {
                throw BizException.badRequest("物理库登记已停用，无法在该库下新增表");
            }
        }
        // 先创建元数据记录
        tableMapper.insert(table);

        ensureDefaultPrimaryKeyFieldIfMissing(table);

        // 生成并执行CREATE TABLE SQL
        try {
            String phyCatalog = businessCatalogResolver.resolveCatalog(table);
            if (phyCatalog != null && !phyCatalog.isEmpty()) {
                mySqlPhysicalCatalogService.requireCatalogOnInstance(phyCatalog);
            }
            String createTableSql = codeGeneratorService.generateCreateTableSQL(table.getTableCode(), table.getBusinessCode());
            Map<String, Object> sqlResult = sqlExecuteService.executeSql(createTableSql, false, phyCatalog);
            if (!Boolean.TRUE.equals(sqlResult.get("success"))) {
                throw BizException.of(AppErrorCodes.TABLE_CREATE_DDL_FAILED,
                        "创建数据库表失败: " + sqlResult.get("message"));
            }
            logService.logSuccess("admin", "CREATE_TABLE_SQL", "执行CREATE TABLE SQL成功: " + table.getTableCode());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            // SQL执行失败，记录日志并抛出异常
            logService.logError("admin", "CREATE_TABLE_SQL", "执行CREATE TABLE SQL失败: " + table.getTableCode(), e.getMessage());
            throw BizException.of(AppErrorCodes.TABLE_CREATE_DDL_FAILED, "创建数据库表失败: " + e.getMessage(), e);
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
            throw BizException.of(AppErrorCodes.TABLE_NOT_FOUND, "表不存在");
        }
        table.setTableCode(existing.getTableCode()); // 编码不可修改

        // 检查主键生成策略是否变更
        if (table.getPkStrategy() != null && !existing.getPkStrategy().equals(table.getPkStrategy())) {
            throw BizException.of(AppErrorCodes.TABLE_PK_STRATEGY_IMMUTABLE, "主键生成策略不允许修改，请删除表后重新创建");
        }

        // 检查业务系统是否变更
        boolean businessSystemChanged = table.getBusinessCode() != null && !table.getBusinessCode().equals(existing.getBusinessCode());

        // 检查表是否被禁用
        boolean isDisabled = table.getIsEnabled() != null && table.getIsEnabled() == 0 && existing.getIsEnabled() == 1;
        if (isDisabled) {
            // 查询关联的模块
            List<String> moduleCodes = moduleTableMapper.selectModuleCodesByTableCode(table.getTableCode());
            if (moduleCodes != null && !moduleCodes.isEmpty()) {
                // 记录日志，表被禁用且有关联模块
                logService.log("admin", "EDIT", "表被禁用且有关联模块: " + table.getTableCode() + ", 关联模块: " + String.join(",", moduleCodes), 0, "");
            }
        }

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
                String phyCatalog = businessCatalogResolver.resolveCatalog(existing);
                Map<String, Object> sqlResult = sqlExecuteService.executeSql(alterSql, false, phyCatalog);
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
            throw BizException.of(AppErrorCodes.TABLE_NOT_FOUND, "表不存在");
        }

        String tableCode = table.getTableCode();
        String tableName = convertToTableName(tableCode);
        String phyCatalog = businessCatalogResolver.resolveCatalog(table);

        // 先执行DROP TABLE删除实际数据库表
        try {
            Map<String, Object> dropResult = sqlExecuteService.executeDropTable(tableName, phyCatalog);
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
        // 删除表关联关系
        relationMapper.deleteByTableCodeEitherSide(tableCode);
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
            throw BizException.of(AppErrorCodes.TABLE_BATCH_IDS_EMPTY, "删除ID列表不能为空");
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
    public List<MetadataTable> list(String tableName, String businessCode, Boolean includeDisabled) {
        return tableMapper.selectAll(tableName, businessCode, includeDisabled);
    }

    /**
     * 分页查询表
     */
    @Override
    public PageResult<MetadataTable> page(String tableName, String businessCode, PageRequest pageRequest, Boolean includeDisabled) {
        Long total = tableMapper.count(tableName, businessCode, includeDisabled);
        List<MetadataTable> records = tableMapper.selectPage(tableName, businessCode, pageRequest, includeDisabled);
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
                    List<MetadataTable> slaveRows = tableMapper.selectByCodes(Collections.singletonList(slaveTableCode), businessCode, false);
                    MetadataTable slaveTable = slaveRows.isEmpty() ? null : slaveRows.get(0);
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
                    List<MetadataTable> mainRows = tableMapper.selectByCodes(Collections.singletonList(mainTableCode), businessCode, false);
                    MetadataTable mainTable = mainRows.isEmpty() ? null : mainRows.get(0);
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
            throw BizException.of(AppErrorCodes.TABLE_NOT_FOUND, "表不存在");
        }

        table.setBusinessCode(businessCode);
        if (StringUtils.hasText(businessCode)) {
            MetadataBusinessSystem bs = businessSystemService.getByCode(businessCode);
            if (bs != null && StringUtils.hasText(bs.getDatabaseName())
                    && !StringUtils.hasText(table.getDatabaseName())) {
                table.setDatabaseName(bs.getDatabaseName().trim());
            }
        }
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
            throw BizException.of(AppErrorCodes.TABLE_BATCH_CODES_EMPTY, "表编码列表不能为空");
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
        return tableMapper.selectByCodes(tableCodes, "", false);
    }

    /**
     * 批量更新表状态
     */
    @Override
    @Transactional
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            throw BizException.badRequest("参数不能为空");
        }
        for (Long id : ids) {
            MetadataTable table = tableMapper.selectById(id);
            if (table != null) {
                // 更新表状态
                table.setIsEnabled(status);
                tableMapper.update(table);
            }
        }
        logService.logSuccess("admin", "BATCH_EDIT", "批量更新表状态：ids=" + ids + ", status=" + status);
    }

    @Override
    public Map<String, Object> ensureMissingPhysicalTables(String businessCode) {
        if (!StringUtils.hasText(businessCode)) {
            throw BizException.of(AppErrorCodes.TABLE_PARAM_INVALID, "业务系统编码不能为空");
        }
        List<MetadataTable> tables = tableMapper.selectAll(null, businessCode, false);
        if (tables == null) {
            tables = Collections.emptyList();
        }
        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<Map<String, String>> failed = new ArrayList<>();

        for (MetadataTable table : tables) {
            if (table.getIsEnabled() != null && table.getIsEnabled() == 0) {
                skipped.add(table.getTableCode() + "（已禁用，跳过）");
                continue;
            }
            String catalog = businessCatalogResolver.resolveCatalog(table);
            if (!StringUtils.hasText(catalog)) {
                failed.add(singleFailRow(table.getTableCode(), "无法解析物理库，请检查业务系统默认库或表级物理库"));
                continue;
            }
            catalog = catalog.trim();
            if (!mySqlPhysicalCatalogService.isValidCatalogName(catalog)) {
                failed.add(singleFailRow(table.getTableCode(), "物理库名不合法: " + catalog));
                continue;
            }
            String physicalName = CodeGenUtils.convertToTableName(table.getTableCode());
            try {
                if (physicalTableExists(catalog, physicalName)) {
                    skipped.add(table.getTableCode() + "（物理表已存在）");
                    continue;
                }
                mySqlPhysicalCatalogService.requireCatalogOnInstance(catalog);
                ensureDefaultPrimaryKeyFieldIfMissing(table);
                String ddl = codeGeneratorService.generateCreateTableSQL(table.getTableCode(), businessCode);
                Map<String, Object> exec = sqlExecuteService.executeSql(ddl, false, catalog);
                if (Boolean.TRUE.equals(exec.get("success"))) {
                    created.add(table.getTableCode());
                    logService.logSuccess("admin", "ENSURE_PHYSICAL_TABLE", "缺失物理表已创建: " + table.getTableCode() + " @ " + catalog);
                } else {
                    Object msg = exec.get("message");
                    failed.add(singleFailRow(table.getTableCode(), msg != null ? msg.toString() : "DDL 执行失败"));
                }
            } catch (Exception e) {
                failed.add(singleFailRow(table.getTableCode(), e.getMessage()));
                logService.logError("admin", "ENSURE_PHYSICAL_TABLE", "创建物理表失败: " + table.getTableCode(), e.getMessage());
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("created", created);
        out.put("skipped", skipped);
        out.put("failed", failed);
        out.put("createdCount", created.size());
        out.put("skippedCount", skipped.size());
        out.put("failedCount", failed.size());
        return out;
    }

    @Override
    public Map<String, Object> importMissingMetadataTables(String businessCode) {
        if (!StringUtils.hasText(businessCode)) {
            throw BizException.of(AppErrorCodes.TABLE_PARAM_INVALID, "业务系统编码不能为空");
        }
        MetadataBusinessSystem bs = businessSystemService.getByCode(businessCode.trim());
        if (bs == null) {
            throw BizException.of(AppErrorCodes.TABLE_PARAM_INVALID, "业务系统不存在: " + businessCode);
        }
        String catalog = businessCatalogResolver.resolveCatalogForBusiness(bs);
        if (!StringUtils.hasText(catalog)) {
            throw BizException.of(AppErrorCodes.TABLE_PARAM_INVALID,
                    "请先在业务系统中配置默认物理库名，或在「库管理」登记该库");
        }
        catalog = catalog.trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(catalog)) {
            throw BizException.of(AppErrorCodes.TABLE_PARAM_INVALID, "物理库名不合法: " + catalog);
        }

        List<String> imported = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<Map<String, String>> failed = new ArrayList<>();

        try (Connection conn = businessDataSourcePoolManager.getConnection(catalog)) {
            List<String> physicalTables = listPhysicalTables(conn, catalog);
            for (String physicalName : physicalTables) {
                MetadataTable existing = findMetadataTableByPhysicalName(businessCode, physicalName);
                if (existing != null) {
                    Long fieldCount = fieldMapper.countByTableCode(existing.getTableCode(), businessCode, true);
                    if (fieldCount != null && fieldCount > 0) {
                        skipped.add(physicalName + "（元数据已登记）");
                        continue;
                    }
                    try {
                        metadataSyncService.syncTableFields(physicalName, conn, businessCode, catalog);
                        metadataSyncService.syncTableForeignKeys(physicalName, conn);
                        fieldMapper.updateFieldsBusinessSystemByTable(existing.getTableCode(), businessCode);
                        imported.add(physicalName + "（补字段）");
                        logService.logSuccess("admin", "IMPORT_METADATA_TABLE",
                                "补同步字段: " + physicalName + " @ " + catalog);
                    } catch (Exception e) {
                        failed.add(singleFailRow(physicalName, e.getMessage()));
                    }
                    continue;
                }
                try {
                    metadataSyncService.syncTableFields(physicalName, conn, businessCode, catalog);
                    metadataSyncService.syncTableForeignKeys(physicalName, conn);
                    imported.add(physicalName);
                    logService.logSuccess("admin", "IMPORT_METADATA_TABLE",
                            "从物理库导入元数据表: " + physicalName + " @ " + catalog);
                } catch (Exception e) {
                    failed.add(singleFailRow(physicalName, e.getMessage()));
                    logService.logError("admin", "IMPORT_METADATA_TABLE",
                            "导入失败: " + physicalName, e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw BizException.of(AppErrorCodes.TABLE_PARAM_INVALID, "连接物理库失败: " + e.getMessage());
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("imported", imported);
        out.put("skipped", skipped);
        out.put("failed", failed);
        out.put("importedCount", imported.size());
        out.put("skippedCount", skipped.size());
        out.put("failedCount", failed.size());
        out.put("catalog", catalog);
        return out;
    }

    private List<String> listPhysicalTables(Connection conn, String catalog) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT TABLE_NAME FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME")) {
            ps.setString(1, catalog);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                }
            }
        }
        return names;
    }

    private MetadataTable findMetadataTableByPhysicalName(String businessCode, String physicalTableName) {
        List<MetadataTable> tables = tableMapper.selectAll(null, businessCode, true);
        if (tables == null) {
            return null;
        }
        for (MetadataTable t : tables) {
            String physical = CodeGenUtils.convertToTableName(t.getTableCode());
            if (physicalTableName.equalsIgnoreCase(physical)
                    || physicalTableName.equalsIgnoreCase(t.getTableName())
                    || physicalTableName.equalsIgnoreCase(t.getTableCode())) {
                return t;
            }
        }
        return null;
    }

    /**
     * 若元数据表下没有任何字段，则按主键策略自动插入一条主键字段（与「新增表」行为一致），便于直接生成建表 SQL。
     */
    private void ensureDefaultPrimaryKeyFieldIfMissing(MetadataTable table) {
        List<MetadataField> fields = fieldMapper.selectByTableCode(table.getTableCode(), "", true);
        if (fields != null && !fields.isEmpty()) {
            return;
        }
        String pkStrategy = table.getPkStrategy();
        if (pkStrategy == null || pkStrategy.isEmpty()) {
            pkStrategy = "AUTO";
        }
        MetadataField primaryKeyField = new MetadataField();
        primaryKeyField.setFieldCode("ID");
        primaryKeyField.setTableCode(table.getTableCode());
        if ("UUID".equals(pkStrategy)) {
            primaryKeyField.setFieldName("uuid");
            primaryKeyField.setLabel("主键UUID");
        } else {
            primaryKeyField.setFieldName("id");
            primaryKeyField.setLabel("主键ID");
        }
        primaryKeyField.setSort(0);
        if ("AUTO".equals(pkStrategy)) {
            primaryKeyField.setFieldType("BIGINT");
            primaryKeyField.setFormComponent("primary_key");
            primaryKeyField.setIsRequired(1);
        } else if ("UUID".equals(pkStrategy)) {
            primaryKeyField.setFieldType("VARCHAR(36)");
            primaryKeyField.setFormComponent("primary_key");
            primaryKeyField.setIsRequired(1);
        } else {
            primaryKeyField.setFieldType("BIGINT");
            primaryKeyField.setFormComponent("primary_key");
            primaryKeyField.setIsRequired(1);
        }
        primaryKeyField.setIsEnabled(1);
        primaryKeyField.setInForm(0);
        String bc = table.getBusinessCode();
        if (bc == null || bc.isEmpty()) {
            bc = "DEFAULT";
        }
        primaryKeyField.setBusinessCode(bc);
        fieldMapper.insert(primaryKeyField);
        logService.logSuccess("admin", "AUTO_CREATE_PK_FIELD", "自动创建主键字段: " + table.getTableCode());
    }

    private static Map<String, String> singleFailRow(String tableCode, String message) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("tableCode", tableCode);
        m.put("message", message);
        return m;
    }

    private boolean physicalTableExists(String catalog, String physicalTableName) throws SQLException {
        try (Connection c = businessDataSourcePoolManager.getConnection(catalog)) {
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT 1 FROM information_schema.tables WHERE LOWER(table_schema) = LOWER(?) AND LOWER(table_name) = LOWER(?) LIMIT 1")) {
                ps.setString(1, catalog);
                ps.setString(2, physicalTableName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
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