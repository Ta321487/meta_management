package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.metadata.common.PageRequest;
import com.metadata.common.PageResult;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataModule;
import com.metadata.entity.MetadataModuleType;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.mapper.MetadataModuleMapper;
import com.metadata.mapper.MetadataModuleTableMapper;
import com.metadata.service.MetadataModuleService;
import com.metadata.service.MetadataModuleTypeService;
import com.metadata.service.MetadataTableService;
import com.metadata.service.OperationLogService;
import com.metadata.util.CodeValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模块服务实现
 */
@Service
public class MetadataModuleServiceImpl implements MetadataModuleService {

    @Autowired
    private MetadataModuleMapper moduleMapper;

    @Autowired
    private MetadataModuleTableMapper moduleTableMapper;

    @Autowired
    private MetadataModuleTypeService moduleTypeService;

    @Autowired
    private MetadataFunctionNodeMapper functionNodeMapper;

    @Autowired
    private MetadataTableService tableService;

    @Autowired
    private OperationLogService logService;

    /**
     * 新增模块
     */
    @Override
    @Transactional
    public void add(MetadataModule module, List<String> tableCodes) {
        // 校验模块对象是否为null
        if (module == null) {
            throw new RuntimeException("模块对象不能为空");
        }
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
        // 根据模块类型自动创建功能节点
        createDefaultFunctionNodes(module, tableCodes);
        logService.logSuccess("admin", "ADD", "新增模块：" + JSON.toJSONString(module));
    }

    /**
     * 更新模块
     */
    @Override
    @Transactional
    public void update(MetadataModule module, List<String> tableCodes) {
        MetadataModule existing = moduleMapper.selectById(module.getId());
        if (existing == null) {
            throw new RuntimeException("模块不存在");
        }
        // 更新模块
        module.setModuleCode(existing.getModuleCode()); // 编码不可修改
        // 如果 status 为 null，使用现有记录的 status
        if (module.getStatus() == null) {
            module.setStatus(existing.getStatus());
        }
        moduleMapper.update(module);
        // 更新关联表
        moduleTableMapper.deleteByModuleCode(existing.getModuleCode());
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
        // 调用创建默认功能节点方法，生成缺失的功能节点
        createDefaultFunctionNodes(module, tableCodes);
        logService.logSuccess("admin", "EDIT", "更新模块：" + JSON.toJSONString(module));
    }

    /**
     * 删除模块
     */
    @Override
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
     * 批量删除模块
     */
    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("删除ID列表不能为空");
        }
        // 为每个ID调用单个删除方法，确保关联关系和日志记录正确
        for (Long id : ids) {
            delete(id);
        }
    }

    /**
     * 查询模块详情
     */
    @Override
    public MetadataModule getByCode(String moduleCode) {
        return moduleMapper.selectByCode(moduleCode, "");
    }

    /**
     * 查询所有模块
     */
    @Override
    public List<MetadataModule> list(String moduleName, String moduleType, String businessCode, Integer status) {
        List<MetadataModule> modules = moduleMapper.selectAll(moduleName, moduleType, status, businessCode);
        // 为每个模块添加关联的表编码列表
        return setTableCodesForModules(modules);
    }

    /**
     * 分页查询模块
     */
    @Override
    public PageResult<MetadataModule> page(String moduleName, String moduleType, String businessCode, Integer status, PageRequest pageRequest) {
        Long total = moduleMapper.count(moduleName, moduleType, status, businessCode);
        List<MetadataModule> records = moduleMapper.selectPage(moduleName, moduleType, status, businessCode, pageRequest);
        // 为每个模块添加关联的表编码列表
        records = setTableCodesForModules(records);
        return new PageResult<>(total, records);
    }

    /**
     * 为模块列表设置关联的表编码列表
     */
    private List<MetadataModule> setTableCodesForModules(List<MetadataModule> modules) {
        for (MetadataModule module : modules) {
            // 查询模块关联的表编码列表
            List<String> tableCodes = moduleTableMapper.selectTableCodesByModuleCode(module.getModuleCode());
            module.setTableCodes(tableCodes);
        }
        return modules;
    }

    /**
     * 启用/禁用模块
     */
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        MetadataModule module = moduleMapper.selectById(id);
        if (module == null) {
            throw new RuntimeException("模块不存在");
        }

        // 更新模块状态
        module.setStatus(status);
        moduleMapper.update(module);

        // 同步更新模块下所有功能节点的状态
        List<MetadataFunctionNode> nodes = functionNodeMapper.selectByModuleCode(module.getModuleCode(), module.getBusinessCode());
        if (nodes != null && !nodes.isEmpty()) {
            for (MetadataFunctionNode node : nodes) {
                node.setIsEnabled(status);
                functionNodeMapper.update(node);
            }
        }

        logService.logSuccess("admin", "EDIT", "更新模块状态：" + module.getModuleCode() + " -> " + status + ", 同步更新功能节点数：" + (nodes != null ? nodes.size() : 0));
    }

    @Override
    @Transactional
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty() || status == null) {
            throw new RuntimeException("参数不能为空");
        }
        int totalNodesUpdated = 0;
        for (Long id : ids) {
            MetadataModule module = moduleMapper.selectById(id);
            if (module != null) {
                // 更新模块状态
                module.setStatus(status);
                moduleMapper.update(module);

                // 同步更新模块下所有功能节点的状态
                List<MetadataFunctionNode> nodes = functionNodeMapper.selectByModuleCode(module.getModuleCode(), module.getBusinessCode());
                if (nodes != null && !nodes.isEmpty()) {
                    for (MetadataFunctionNode node : nodes) {
                        node.setIsEnabled(status);
                        functionNodeMapper.update(node);
                    }
                    totalNodesUpdated += nodes.size();
                }
            }
        }
        logService.logSuccess("admin", "BATCH_EDIT", "批量更新模块状态：ids=" + ids + ", status=" + status + ", 同步更新功能节点数：" + totalNodesUpdated);
    }

    /**
     * 更新模块的业务系统
     */
    @Override
    @Transactional
    public void updateModuleBusinessSystem(String moduleCode, String businessCode) {
        // 获取模块，使用空字符串作为业务系统，查询所有业务系统的模块
        MetadataModule module = moduleMapper.selectByCode(moduleCode, "");
        if (module == null) {
            throw new RuntimeException("模块不存在");
        }

        // 更新模块的业务系统
        module.setBusinessCode(businessCode);
        moduleMapper.update(module);

        // 获取模块关联的表
        List<String> tableCodes = moduleTableMapper.selectTableCodesByModuleCode(moduleCode);
        if (tableCodes != null && !tableCodes.isEmpty()) {
            // 批量更新表的业务系统
            tableService.batchUpdateTableBusinessSystem(tableCodes, businessCode);
        }

        logService.logSuccess("admin", "EDIT", "更新模块业务系统：" + moduleCode + " -> " + businessCode);
    }

    /**
     * 批量更新模块的业务系统
     */
    @Override
    @Transactional
    public void batchUpdateModuleBusinessSystem(List<String> moduleCodes, String businessCode) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            throw new RuntimeException("模块编码列表不能为空");
        }

        for (String moduleCode : moduleCodes) {
            updateModuleBusinessSystem(moduleCode, businessCode);
        }
    }

    /**
     * 获取模块列表
     */
    @Override
    public List<MetadataModule> getModulesByCodes(List<String> moduleCodes) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            return List.of();
        }
        return moduleMapper.selectByCodes(moduleCodes);
    }

    /**
     * 重建模块的功能节点
     */
    @Override
    public void rebuildNodes(String moduleCode) {
        // 获取模块详情
        MetadataModule module = getByCode(moduleCode);
        if (module == null) {
            throw new RuntimeException("模块不存在：" + moduleCode);
        }

        // 获取模块关联的表列表
        List<MetadataTable> tables = tableService.listByModuleCode(moduleCode);
        List<String> tableCodes = tables.stream().map(MetadataTable::getTableCode).toList();

        // 调用现有的创建默认功能节点方法
        createDefaultFunctionNodes(module, tableCodes);
    }

    /**
     * 根据模块类型自动创建默认功能节点
     */
    private void createDefaultFunctionNodes(MetadataModule module, List<String> tableCodes) {
        try {
            // 校验模块对象是否为null
            if (module == null) {
                logService.logError("admin", "ADD", "创建功能节点失败：模块对象不能为空", "模块对象为null");
                return;
            }
            // 记录开始创建节点的日志
            logService.logSuccess("admin", "ADD", "开始创建功能节点，模块：" + module.getModuleCode() + "，关联表数量：" + (tableCodes == null ? 0 : tableCodes.size()));

            if (tableCodes == null || tableCodes.isEmpty()) {
                logService.logError("admin", "ADD", "创建功能节点失败：" + module.getModuleCode(), "关联表列表为空");
                return;
            }

            // 获取模块类型配置
            String moduleTypeCode = module.getModuleType();
            if (moduleTypeCode == null || moduleTypeCode.trim().isEmpty()) {
                logService.logError("admin", "ADD", "创建功能节点失败：" + module.getModuleCode(), "模块类型为空");
                return;
            }

            MetadataModuleType moduleType = moduleTypeService.getByCode(moduleTypeCode);
            if (moduleType == null) {
                logService.logError("admin", "ADD", "创建功能节点失败：" + module.getModuleCode(), "模块类型不存在：" + moduleTypeCode);
                return;
            }

            List<String> defaultNodes = moduleType.getDefaultNodes();
            if (defaultNodes == null || defaultNodes.isEmpty()) {
                logService.logError("admin", "ADD", "创建功能节点失败：" + module.getModuleCode(), "模块类型未配置默认节点：" + moduleTypeCode);
                return;
            }

            // 解析默认节点类型
            String[] nodeTypes = defaultNodes.toArray(new String[0]);
            logService.logSuccess("admin", "ADD", "解析节点类型，模块：" + module.getModuleCode() + "，节点类型数量：" + nodeTypes.length);

            // 节点类型到中文名称的映射
            Map<String, String> nodeTypeNameMap = new HashMap<>();
            nodeTypeNameMap.put("LIST_PAGE", "列表页");
            nodeTypeNameMap.put("FORM_PAGE", "表单页");
            nodeTypeNameMap.put("DETAIL_PAGE", "详情页");
            nodeTypeNameMap.put("PROCESS_PAGE", "流程流转页");
            nodeTypeNameMap.put("REPORT_PAGE", "报表展示页");
            nodeTypeNameMap.put("BATCH_IMPORT_PAGE", "批量导入页");
            nodeTypeNameMap.put("BATCH_EXPORT_PAGE", "批量导出页");

            // 获取表信息（用于生成节点名称）
            String moduleNamePrefix = module.getModuleName();
            if (moduleNamePrefix.endsWith("模块")) {
                moduleNamePrefix = moduleNamePrefix.substring(0, moduleNamePrefix.length() - 2);
            }

            // 为每个关联的表创建功能节点
            int totalCreated = 0;
            for (String tableCode : tableCodes) {
                if (!CodeValidator.isValidCode(tableCode)) {
                    logService.logError("admin", "ADD", "跳过无效表编码：" + tableCode, "编码格式不正确");
                    continue;
                }

                // 获取表信息
                MetadataTable table = tableService.getByCode(tableCode);
                String tableName = table != null ? table.getTableName() : tableCode;
                // 简化表名（去掉"表"字）
                if (tableName.endsWith("表")) {
                    tableName = tableName.substring(0, tableName.length() - 1);
                }

                // 为每个节点类型创建节点
                int sort = 0;
                for (String nodeType : nodeTypes) {
                    nodeType = nodeType.trim();
                    if (nodeType.isEmpty()) {
                        continue;
                    }

                    try {
                        // 生成节点编码：{MODULE_CODE}_{TABLE_CODE}_{NODE_TYPE}
                        String nodeCode = module.getModuleCode() + "_" + tableCode + "_" + nodeType;

                        // 检查节点是否已存在
                        MetadataFunctionNode existing = functionNodeMapper.selectByCode(module.getModuleCode(), nodeCode);
                        if (existing != null) {
                            continue; // 已存在则跳过
                        }

                        // 生成节点名称
                        String nodeTypeName = nodeTypeNameMap.getOrDefault(nodeType, nodeType);
                        String nodeName = tableName + nodeTypeName;

                        // 创建功能节点
                        MetadataFunctionNode node = new MetadataFunctionNode();
                        node.setNodeCode(nodeCode);
                        node.setNodeName(nodeName);
                        node.setModuleCode(module.getModuleCode());
                        node.setNodeType(nodeType);
                        node.setRelatedTableCode(tableCode);
                        node.setBusinessCode(module.getBusinessCode());
                        // 生成默认跳转关系，方便路由生成器和前端集成
                        // 使用模块的routePath作为基础路径，如果模块没有设置，则使用默认规则
                        String modulePath = module.getRoutePath();
                        String pathBase;

                        if (modulePath != null && !modulePath.trim().isEmpty()) {
                            // 如果模块设置了路由路径，使用模块路径作为基础
                            pathBase = modulePath.startsWith("/") ? modulePath.substring(1) : modulePath;
                        } else {
                            // 否则使用默认约定路径：小写表名
                            pathBase = tableCode == null ? "" : tableCode.toLowerCase().replace("_table", "");
                        }

                        if (nodeType.toUpperCase().contains("LIST")) {
                            node.setJumpRelation("/" + pathBase + "/list");
                            // 列表页默认在菜单中显示
                            node.setIsMenuVisible(1);
                        } else if (nodeType != null && nodeType.toUpperCase().contains("FORM")) {
                            node.setJumpRelation("/" + pathBase + "/form/:id?");
                            // 表单页默认不在菜单中显示
                            node.setIsMenuVisible(0);
                        } else if (nodeType != null && nodeType.toUpperCase().contains("DETAIL")) {
                            node.setJumpRelation("/" + pathBase + "/detail/:id?");
                            // 详情页默认不在菜单中显示
                            node.setIsMenuVisible(0);
                        } else if (nodeType != null && nodeType.toUpperCase().contains("REPORT")) {
                            // 报表页默认在菜单中显示
                            node.setIsMenuVisible(1);
                        } else if (nodeType != null && (nodeType.toUpperCase().contains("BATCH_IMPORT") || nodeType.toUpperCase().contains("BATCH_EXPORT"))) {
                            // 批量导入/导出页默认在菜单中显示
                            node.setIsMenuVisible(1);
                        } else {
                            // 其它类型不默认设置跳转关系
                            node.setJumpRelation("");
                            // 其他类型节点默认不在菜单中显示
                            node.setIsMenuVisible(0);
                        }
                        node.setSort(sort++);
                        node.setIsEnabled(1);

                        int result = functionNodeMapper.insert(node);
                        if (result > 0) {
                            totalCreated++;
                            logService.logSuccess("admin", "ADD", "成功创建功能节点：" + nodeCode + "，节点名称：" + nodeName);
                        } else {
                            logService.logError("admin", "ADD", "创建功能节点失败：" + nodeCode, "插入返回结果：" + result);
                        }
                    } catch (Exception e) {
                        // 单个节点创建失败不影响其他节点
                        logService.logError("admin", "ADD", "创建功能节点异常：" + module.getModuleCode() + "_" + tableCode + "_" + nodeType, e.getMessage());
                    }
                }
            }

            // 记录创建结果
            logService.logSuccess("admin", "ADD", "功能节点创建完成，模块：" + module.getModuleCode() + "，成功创建：" + totalCreated + "个节点");
        } catch (Exception e) {
            // 节点创建失败不影响模块创建，只记录日志
            logService.logError("admin", "ADD", "自动创建功能节点失败：" + module.getModuleCode(), e.getMessage() + "，异常类型：" + e.getClass().getName());
        }
    }
}