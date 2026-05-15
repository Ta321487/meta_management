package com.metadata.service.impl;

import com.metadata.entity.MetadataPhysicalDatabase;
import com.metadata.mapper.MetadataBusinessSystemMapper;
import com.metadata.mapper.MetadataPhysicalDatabaseMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.service.MetadataPhysicalDatabaseService;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.service.OperationLogService;
import com.metadata.service.constant.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
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
    private OperationLogService logService;

    @Override
    public List<MetadataPhysicalDatabase> listAll() {
        return physicalDatabaseMapper.selectAll();
    }

    @Override
    @Transactional
    public void add(MetadataPhysicalDatabase row) {
        if (row.getCatalogName() == null || row.getCatalogName().trim().isEmpty()) {
            throw new RuntimeException("库名不能为空");
        }
        String cat = row.getCatalogName().trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(cat)) {
            throw new RuntimeException("库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        if (physicalDatabaseMapper.countByCatalogName(cat) > 0) {
            throw new RuntimeException("该库名已登记");
        }
        normalizeDefaults(row);
        if (row.getSyncToInstance() != null && row.getSyncToInstance() == 1) {
            mySqlPhysicalCatalogService.ensureCatalogExists(cat, row.getCharsetName(), row.getCollationName());
        }
        row.setCatalogName(cat);
        physicalDatabaseMapper.insert(row);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "登记物理库: " + cat);
    }

    @Override
    @Transactional
    public void update(MetadataPhysicalDatabase row) {
        if (row.getId() == null) {
            throw new RuntimeException("ID不能为空");
        }
        MetadataPhysicalDatabase existing = physicalDatabaseMapper.selectById(row.getId());
        if (existing == null) {
            throw new RuntimeException("记录不存在");
        }
        normalizeDefaults(row);
        row.setCatalogName(existing.getCatalogName());
        if (row.getSyncToInstance() != null && row.getSyncToInstance() == 1) {
            mySqlPhysicalCatalogService.ensureCatalogExists(existing.getCatalogName().trim(), row.getCharsetName(), row.getCollationName());
        }
        physicalDatabaseMapper.update(row);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "更新物理库登记: " + existing.getCatalogName());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MetadataPhysicalDatabase row = physicalDatabaseMapper.selectById(id);
        if (row == null) {
            throw new RuntimeException("记录不存在");
        }
        String cat = row.getCatalogName().trim();
        if (businessSystemMapper.countByDatabaseName(cat) > 0 || tableMapper.countByDatabaseName(cat) > 0) {
            throw new RuntimeException("该库仍被业务系统默认库或表级物理库引用，无法删除登记");
        }
        physicalDatabaseMapper.deleteById(id);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_SQL_EXECUTE, "删除物理库登记: " + cat);
    }

    @Override
    public void syncToInstance(Long id) {
        MetadataPhysicalDatabase row = physicalDatabaseMapper.selectById(id);
        if (row == null) {
            throw new RuntimeException("记录不存在");
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
