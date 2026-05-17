package com.metadata.service.impl;

import com.metadata.common.FillBusinessSystemCatalogRequest;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataModule;
import com.metadata.entity.MetadataPhysicalDatabase;
import com.metadata.mapper.MetadataPhysicalDatabaseMapper;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.mapper.MetadataBusinessSystemMapper;
import com.metadata.mapper.MetadataModuleMapper;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.util.CodeValidator;
import com.metadata.service.MetadataModuleService;
import com.metadata.service.MySqlPhysicalCatalogService;
import com.metadata.util.SpringContextUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataPhysicalDatabaseMapper physicalDatabaseMapper;

    /**
     * 业务系统/补充接口：库须在「库管理」登记且已在实例存在；不在此建库。
     */
    private void assertPhysicalCatalogReady(String catalog) {
        MetadataPhysicalDatabase reg = physicalDatabaseMapper.selectByCatalogName(catalog);
        if (reg == null) {
            throw BizException.badRequest("请先在「库管理」登记物理库「" + catalog + "」并建库（保存时建库或同步）");
        }
        if (reg.getIsEnabled() != null && reg.getIsEnabled() == 0) {
            throw BizException.badRequest("物理库登记已停用: " + catalog);
        }
        mySqlPhysicalCatalogService.requireCatalogOnInstance(catalog);
    }

    /**
     * 查询所有业务系统
     */
    @Override
    public List<MetadataBusinessSystem> listAll(Boolean includeDisabled) {
        return businessSystemMapper.selectAll(includeDisabled);
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
        if (StringUtils.hasText(businessSystem.getBusinessCode())) {
            businessSystem.setBusinessCode(CodeValidator.normalizeCode(businessSystem.getBusinessCode()));
            if (!CodeValidator.isValidCode(businessSystem.getBusinessCode())) {
                throw BizException.badRequest("业务编码只能包含字母、数字和下划线，长度1-50");
            }
        }
        if (!StringUtils.hasText(businessSystem.getDatabaseName())) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_PHYSICAL_CATALOG_REQUIRED,
                    "默认物理库不能为空，请先在「库管理」登记目标库并在本处选择");
        }
        String catalog = businessSystem.getDatabaseName().trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(catalog)) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_PHYSICAL_CATALOG_INVALID,
                    "物理库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        businessSystem.setDatabaseName(catalog);
        // 如果设置为默认系统，先将其他系统设置为非默认
        if (businessSystem.getIsDefault() != null && businessSystem.getIsDefault() == 1) {
            MetadataBusinessSystem defaultSystem = getDefault();
            if (defaultSystem != null) {
                defaultSystem.setIsDefault(0);
                businessSystemMapper.update(defaultSystem);
            }
        }
        assertPhysicalCatalogReady(catalog);
        if (businessSystem.getIsEnabled() == null) {
            businessSystem.setIsEnabled(1);
        }
        businessSystemMapper.insert(businessSystem);
    }

    /**
     * 更新业务系统
     */
    @Override
    @Transactional
    public void update(MetadataBusinessSystem businessSystem) {
        if (!StringUtils.hasText(businessSystem.getDatabaseName())) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_PHYSICAL_CATALOG_REQUIRED,
                    "默认物理库不能为空，请先在「库管理」登记目标库并在本处选择");
        }
        String catalog = businessSystem.getDatabaseName().trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(catalog)) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_PHYSICAL_CATALOG_INVALID,
                    "物理库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }
        businessSystem.setDatabaseName(catalog);
        MetadataBusinessSystem old = businessSystemMapper.selectByCode(businessSystem.getBusinessCode());
        if (businessSystem.getIsEnabled() == null && old != null) {
            businessSystem.setIsEnabled(old.getIsEnabled());
        }
        // 如果设置为默认系统，先将其他系统设置为非默认
        if (businessSystem.getIsDefault() != null && businessSystem.getIsDefault() == 1) {
            MetadataBusinessSystem defaultSystem = getDefault();
            if (defaultSystem != null && !defaultSystem.getId().equals(businessSystem.getId())) {
                defaultSystem.setIsDefault(0);
                businessSystemMapper.update(defaultSystem);
            }
        }
        assertPhysicalCatalogReady(catalog);
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
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_DEFAULT_CANNOT_DELETE, "默认业务系统不能删除");
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
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_MODULE_CODES_EMPTY, "模块编码列表不能为空");
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
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_MODULE_CODES_EMPTY, "模块编码列表不能为空");
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

    @Override
    @Transactional
    public int fillDefaultPhysicalCatalog(FillBusinessSystemCatalogRequest request) {
        if (request == null || !StringUtils.hasText(request.getBusinessCode())) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_NOT_FOUND, "业务系统编码不能为空");
        }
        String businessCode = request.getBusinessCode().trim();
        if (!StringUtils.hasText(request.getDatabaseName())) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_PHYSICAL_CATALOG_REQUIRED, "请选择要补充的物理库名称");
        }
        String catalog = request.getDatabaseName().trim();
        if (!mySqlPhysicalCatalogService.isValidCatalogName(catalog)) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_PHYSICAL_CATALOG_INVALID,
                    "物理库名不符合安全规则：仅允许字母、数字、下划线、美元符号，长度 1–64");
        }

        MetadataBusinessSystem bs = businessSystemMapper.selectByCode(businessCode);
        if (bs == null) {
            throw BizException.of(AppErrorCodes.BIZ_SYSTEM_NOT_FOUND, "业务系统不存在: " + businessCode);
        }

        String oldCat = bs.getDatabaseName() != null ? bs.getDatabaseName().trim() : null;

        assertPhysicalCatalogReady(catalog);
        bs.setDatabaseName(catalog);
        businessSystemMapper.update(bs);

        if (StringUtils.hasText(oldCat) && !oldCat.equalsIgnoreCase(catalog)) {
            businessDataSourcePoolManager.evictCatalog(oldCat);
        }
        businessDataSourcePoolManager.evictCatalog(catalog);

        boolean backfill = request.getBackfillEmptyTableCatalog() == null
                || Boolean.TRUE.equals(request.getBackfillEmptyTableCatalog());
        if (!backfill) {
            return 0;
        }
        return tableMapper.updateEmptyDatabaseNameByBusinessCode(businessCode, catalog);
    }
}