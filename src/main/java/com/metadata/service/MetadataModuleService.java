package com.metadata.service;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataModule;
import com.metadata.mapper.MetadataModuleMapper;
import com.metadata.mapper.MetadataModuleTableMapper;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模块服务
 */
@Service
public class MetadataModuleService {

    @Autowired
    private MetadataModuleMapper moduleMapper;

    @Autowired
    private MetadataModuleTableMapper moduleTableMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增模块
     */
    @Transactional
    public void add(MetadataModule module, List<String> tableCodes) {
        // 校验编码
        if (!CodeValidator.isValidCode(module.getModuleCode())) {
            throw new RuntimeException("模块编码格式不正确");
        }
        // 检查编码是否已存在
        if (moduleMapper.countByCode(module.getModuleCode()) > 0) {
            throw new RuntimeException("模块编码已存在");
        }
        // 插入模块
        module.setStatus(1);
        moduleMapper.insert(module);
        // 关联表
        if (tableCodes != null && !tableCodes.isEmpty()) {
            for (String tableCode : tableCodes) {
                if (CodeValidator.isValidCode(tableCode)) {
                    moduleTableMapper.insert(new com.metadata.entity.MetadataModuleTable() {{
                        setModuleCode(module.getModuleCode());
                        setTableCode(tableCode);
                    }});
                }
            }
        }
        logService.logSuccess("admin", "ADD", "新增模块：" + JSON.toJSONString(module));
    }

    /**
     * 更新模块
     */
    @Transactional
    public void update(MetadataModule module, List<String> tableCodes) {
        MetadataModule existing = moduleMapper.selectById(module.getId());
        if (existing == null) {
            throw new RuntimeException("模块不存在");
        }
        // 更新模块
        module.setModuleCode(existing.getModuleCode()); // 编码不可修改
        moduleMapper.update(module);
        // 更新关联表
        moduleTableMapper.deleteByModuleCode(existing.getModuleCode());
        if (tableCodes != null && !tableCodes.isEmpty()) {
            for (String tableCode : tableCodes) {
                if (CodeValidator.isValidCode(tableCode)) {
                    moduleTableMapper.insert(new com.metadata.entity.MetadataModuleTable() {{
                        setModuleCode(existing.getModuleCode());
                        setTableCode(tableCode);
                    }});
                }
            }
        }
        logService.logSuccess("admin", "EDIT", "更新模块：" + JSON.toJSONString(module));
    }

    /**
     * 删除模块
     */
    @Transactional
    public void delete(Long id) {
        MetadataModule module = moduleMapper.selectById(id);
        if (module == null) {
            throw new RuntimeException("模块不存在");
        }
        // 删除关联表关系
        moduleTableMapper.deleteByModuleCode(module.getModuleCode());
        // 删除模块
        moduleMapper.deleteById(id);
        logService.logSuccess("admin", "DELETE", "删除模块：" + module.getModuleCode());
    }

    /**
     * 查询模块详情
     */
    public MetadataModule getByCode(String moduleCode) {
        return moduleMapper.selectByCode(moduleCode);
    }

    /**
     * 查询所有模块
     */
    public List<MetadataModule> list(String moduleName, String moduleType, Integer status) {
        return moduleMapper.selectAll(moduleName, moduleType, status);
    }

    /**
     * 分页查询模块
     */
    public PageResult<MetadataModule> page(String moduleName, String moduleType, Integer status, PageRequest pageRequest) {
        Long total = moduleMapper.count(moduleName, moduleType, status);
        List<MetadataModule> records = moduleMapper.selectPage(moduleName, moduleType, status, pageRequest);
        return new PageResult<>(total, records);
    }

    /**
     * 启用/禁用模块
     */
    public void updateStatus(Long id, Integer status) {
        MetadataModule module = moduleMapper.selectById(id);
        if (module == null) {
            throw new RuntimeException("模块不存在");
        }
        module.setStatus(status);
        moduleMapper.update(module);
        logService.logSuccess("admin", "EDIT", "更新模块状态：" + module.getModuleCode() + " -> " + status);
    }
}

