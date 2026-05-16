package com.metadata.service.impl;

import com.metadata.common.codes.AppErrorCodes;
import com.metadata.entity.MetadataPhysicalDatabase;
import com.metadata.entity.MetadataTable;
import com.metadata.exception.BizException;
import com.metadata.mapper.MetadataBusinessSystemMapper;
import com.metadata.mapper.MetadataPhysicalDatabaseMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.mapper.MetadataTableRelationMapper;
import com.metadata.service.MetadataPhysicalDatabaseService;
import com.metadata.service.MetadataTableService;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.service.OperationLogService;
import com.metadata.service.constant.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MetadataPhysicalDatabaseServiceImpl implements MetadataPhysicalDatabaseService {

    @Autowired
    private MetadataPhysicalDatabaseMapper physicalDatabaseMapper;

    @Autowired
    private MySqlPhysicalCatalogService mySqlPhysicalCatalogService;

    @Autowired
    private MetadataBusinessSystemMapper businessSystemMapper;

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataTableRelationMapper relationMapper;

    @Autowired
    @Lazy
    private MetadataTableService tableService;

    @Autowired
    private OperationLogService logService;

    @Override
    public List<MetadataPhysicalDatabase> listAll(Boolean includeDisabled) {
        return physicalDatabaseMapper.selectAll(includeDisabled);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MetadataPhysicalDatabase row) {
        if (row.getCatalogName() == null || row.getCatalogName().trim().isEmpty()) {
            throw BizException.badRequest("库名不能为空");
        }
        String cat = row.getCatalogName().trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(cat)) {
            throw BizException.of(AppErrorCodes.PHYSICAL_DB_CATALOG_INVALID,
                    "库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        if (physicalDatabaseMapper.countByCatalogName(cat) > 0) {
            throw BizException.of(AppErrorCodes.PHYSICAL_DB_NAME_DUPLICATE, "该库名已存在");
        }
        normalizeDefaults(row);
        if (row.getSyncToInstance() != null && row.getSyncToInstance() == 1) {
            mySqlPhysicalCatalogService.ensureCatalogExists(cat, row.getCharsetName(), row.getCollationName());
        }
        row.setCatalogName(cat);
        physicalDatabaseMapper.insert(row);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "新增物理库: " + cat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MetadataPhysicalDatabase row) {
        if (row.getId() == null) {
            throw BizException.badRequest("ID不能为空");
        }
        MetadataPhysicalDatabase existing = physicalDatabaseMapper.selectById(row.getId());
        if (existing == null) {
            throw BizException.of(AppErrorCodes.PHYSICAL_DB_REGISTRATION_NOT_FOUND, "记录不存在");
        }
        normalizeDefaults(row);
        row.setCatalogName(existing.getCatalogName());
        if (row.getSyncToInstance() != null && row.getSyncToInstance() == 1) {
            mySqlPhysicalCatalogService.ensureCatalogExists(existing.getCatalogName().trim(), row.getCharsetName(), row.getCollationName());
        }
        physicalDatabaseMapper.update(row);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "更新物理库: " + existing.getCatalogName());
    }

    /**
     * 删除库配置：同一事务内解除业务系统默认库引用、删除表级引用该库的所有表元数据，最后删除本记录。
     * {@code dropOnInstance=true} 时在事务提交后执行 DROP DATABASE（DDL 与事务分离）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, boolean dropOnInstance) {
        MetadataPhysicalDatabase row = physicalDatabaseMapper.selectById(id);
        if (row == null) {
            throw BizException.of(AppErrorCodes.PHYSICAL_DB_REGISTRATION_NOT_FOUND, "记录不存在");
        }
        String cat = row.getCatalogName().trim();

        businessSystemMapper.clearDatabaseNameByDatabaseName(cat);

        List<MetadataTable> tables = tableMapper.selectByDatabaseName(cat);
        if (tables != null) {
            for (MetadataTable t : tables) {
                relationMapper.deleteByTableCodeEitherSide(t.getTableCode());
            }
            for (MetadataTable t : tables) {
                try {
                    tableService.delete(t.getId());
                } catch (BizException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw BizException.of(AppErrorCodes.PHYSICAL_DB_CASCADE_FAILED,
                            "级联删除表失败: " + t.getTableCode() + " — " + ex.getMessage(), ex);
                }
            }
        }

        physicalDatabaseMapper.deleteById(id);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE,
                "删除物理库配置" + (dropOnInstance ? "（含服务器库）" : "") + ": " + cat);

        if (dropOnInstance) {
            dropCatalogOnInstance(cat);
        }
    }

    private void dropCatalogOnInstance(String catalogName) {
        try {
            mySqlPhysicalCatalogService.dropCatalogIfExists(catalogName);
        } catch (BizException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw BizException.of(AppErrorCodes.PHYSICAL_DB_DROP_FAILED,
                    "元数据已删除，但删除服务器库失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void syncToInstance(Long id) {
        MetadataPhysicalDatabase row = physicalDatabaseMapper.selectById(id);
        if (row == null) {
            throw BizException.of(AppErrorCodes.PHYSICAL_DB_REGISTRATION_NOT_FOUND, "记录不存在");
        }
        normalizeDefaults(row);
        mySqlPhysicalCatalogService.ensureCatalogExists(row.getCatalogName().trim(), row.getCharsetName(), row.getCollationName());
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "同步物理库到实例: " + row.getCatalogName());
    }

    private void normalizeDefaults(MetadataPhysicalDatabase row) {
        if (row.getCharsetName() == null || row.getCharsetName().trim().isEmpty()) {
            row.setCharsetName("utf8mb4");
        }
        if (row.getCollationName() == null || row.getCollationName().trim().isEmpty()) {
            row.setCollationName("utf8mb4_unicode_ci");
        }
        if (row.getSyncToInstance() == null) {
            row.setSyncToInstance(1);
        }
        if (row.getIsEnabled() == null) {
            row.setIsEnabled(1);
        }
    }
}
