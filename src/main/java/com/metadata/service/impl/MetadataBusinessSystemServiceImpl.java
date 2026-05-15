package com.metadata.service.impl;

import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataModule;
import com.metadata.mapper.MetadataBusinessSystemMapper;
import com.metadata.mapper.MetadataModuleMapper;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.MetadataModuleService;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.util.SpringContextUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 业务系统服务实现
 */
@Service
public class MetadataBusinessSystemServiceImpl implements MetadataBusinessSystemService {

    @Autowired
    private MetadataBusinessSystemMapper businessSystemMapper;

    @Autowired
    private BusinessDataSourcePoolManager businessDataSourcePoolManager;

    @Autowired
    private MySqlPhysicalCatalogService mySqlPhysicalCatalogService;

    /**
     * 查询所有业务系统
     */
    @Override
    public List<MetadataBusinessSystem> listAll() {
        return businessSystemMapper.selectAll();
    }

    /**
     * 根据编码查询业务系统
     */
    @Override
    public MetadataBusinessSystem getByCode(String businessCode) {
        return businessSystemMapper.selectByCode(businessCode);
    }

    /**
     * 获取默认业务系统
     */
    @Override
    public MetadataBusinessSystem getDefault() {
        return businessSystemMapper.selectDefault();
    }

    /**
     * 新增业务系统
     */
    @Override
    @Transactional
    public void add(MetadataBusinessSystem businessSystem) {
        // 如果设置为默认系统，先将其他系统设置为非默认
        if (businessSystem.getIsDefault() != null && businessSystem.getIsDefault() == 1) {
            MetadataBusinessSystem defaultSystem = getDefault();
            if (defaultSystem != null) {
                defaultSystem.setIsDefault(0);
                businessSystemMapper.update(defaultSystem);
            }
        }
        if (businessSystem.getDatabaseName() != null && !businessSystem.getDatabaseName().trim().isEmpty()) {
            mySqlPhysicalCatalogService.ensureCatalogExists(businessSystem.getDatabaseName().trim());
        }
        businessSystemMapper.insert(businessSystem);
    }

    /**
     * 更新业务系统
     */
    @Override
    @Transactional
    public void update(MetadataBusinessSystem businessSystem) {
        MetadataBusinessSystem old = businessSystemMapper.selectByCode(businessSystem.getBusinessCode());
        // 如果设置为默认系统，先将其他系统设置为非默认
        if (businessSystem.getIsDefault() != null && businessSystem.getIsDefault() == 1) {
            MetadataBusinessSystem defaultSystem = getDefault();
            if (defaultSystem != null && !defaultSystem.getId().equals(businessSystem.getId())) {
                defaultSystem.setIsDefault(0);
                businessSystemMapper.update(defaultSystem);
            }
        }
        if (businessSystem.getDatabaseName() != null && !businessSystem.getDatabaseName().trim().isEmpty()) {
            mySqlPhysicalCatalogService.ensureCatalogExists(businessSystem.getDatabaseName().trim());
        }
        businessSystemMapper.update(businessSystem);
        if (old != null && old.getDatabaseName() != null && !old.getDatabaseName().trim().isEmpty()) {
            businessDataSourcePoolManager.evictCatalog(old.getDatabaseName());
        }
        if (businessSystem.getDatabaseName() != null && !businessSystem.getDatabaseName().trim().isEmpty()) {
            businessDataSourcePoolManager.evictCatalog(businessSystem.getDatabaseName());
        }
    }

    /**
     * 删除业务系统
     */
    @Override
    @Transactional
    public void delete(Long id) {
        MetadataBusinessSystem defaultRow = businessSystemMapper.selectByCode("DEFAULT");
        if (defaultRow != null && defaultRow.getId().equals(id)) {
            throw new RuntimeException("默认业务系统不能删除");
        }
        MetadataBusinessSystem toRemove = businessSystemMapper.selectById(id);
        businessSystemMapper.delete(id);
        if (toRemove != null && toRemove.getDatabaseName() != null && !toRemove.getDatabaseName().trim().isEmpty()) {
            businessDataSourcePoolManager.evictCatalog(toRemove.getDatabaseName());
        }
    }

    /**
     * 关联业务系统到模块
     */
    @Override
    @Transactional
    public void associateModules(String businessCode, List<String> moduleCodes) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            throw new RuntimeException("模块编码列表不能为空");
        }

        // 批量更新模块的业务系统
        MetadataModuleService moduleService = SpringContextUtil.getBean(MetadataModuleService.class);
        moduleService.batchUpdateModuleBusinessSystem(moduleCodes, businessCode);
    }

    /**
     * 解除业务系统与模块的关联
     */
    @Override
    @Transactional
    public void disassociateModules(String businessCode, List<String> moduleCodes) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            throw new RuntimeException("模块编码列表不能为空");
        }

        // 将模块的业务系统设置为默认值
        MetadataModuleService moduleService = SpringContextUtil.getBean(MetadataModuleService.class);
        moduleService.batchUpdateModuleBusinessSystem(moduleCodes, "DEFAULT");
    }

    /**
     * 获取业务系统关联的模块列表
     */
    @Override
    public List<String> getAssociatedModules(String businessCode) {
        MetadataModuleMapper moduleMapper = SpringContextUtil.getBean(MetadataModuleMapper.class);
        List<MetadataModule> modules = moduleMapper.selectByBusinessCode(businessCode);
        return modules.stream().map(MetadataModule::getModuleCode).collect(java.util.stream.Collectors.toList());
    }
}