package com.metadata.service.impl;

import com.metadata.entity.MetadataBusinessRule;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.MetadataTableRelationService;
import com.metadata.service.MetadataTableService;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.service.exception.CodeGenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vue代码生成模块，负责Vue代码生成相关逻辑
 */
public class VueCodeGenerator {
    
    private MetadataTableService tableService;
    private MetadataFieldService fieldService;
    private MetadataBusinessSystemService businessSystemService;
    private MetadataFunctionNodeMapper nodeMapper;
    private MetadataBusinessRuleService businessRuleService;
    private MetadataTableRelationService relationService;
    private TemplateManager templateManager;
    
    /**
     * 构造方法
     * @param tableService 表服务
     * @param fieldService 字段服务
     * @param businessSystemService 业务系统服务
     * @param nodeMapper 功能节点Mapper
     * @param businessRuleService 业务规则服务
     * @param relationService 表关联关系服务
     */
    public VueCodeGenerator(MetadataTableService tableService,
                           MetadataFieldService fieldService,
                           MetadataBusinessSystemService businessSystemService,
                           MetadataFunctionNodeMapper nodeMapper,
                           MetadataBusinessRuleService businessRuleService,
                           MetadataTableRelationService relationService) {
        this.tableService = tableService;
        this.fieldService = fieldService;
        this.businessSystemService = businessSystemService;
        this.nodeMapper = nodeMapper;
        this.businessRuleService = businessRuleService;
        this.relationService = relationService;
        this.templateManager = TemplateManager.getInstance();
    }
    
    /**
     * 生成Vue列表页面
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return Vue列表页面代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateVueList(String tableCode, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> allFields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(allFields);
        List<MetadataField> fields = allFields.stream()
                // 过滤掉主键字段，列表页通常不显示主键
                .filter(f -> !"primary_key".equals(f.getFormComponent()))
                .collect(Collectors.toList());

        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);

        // 去重：多个外键指向同一主表时只生成一份主表 list 与一次加载
        List<Map<String, Object>> relatedLoadTargets = new ArrayList<>();
        java.util.Set<String> seenRelatedClasses = new java.util.HashSet<>();
        for (Map<String, Object> fm : fieldList) {
            if (Boolean.TRUE.equals(fm.get("isForeignKey"))) {
                String cls = (String) fm.get("relatedTableClassName");
                if (cls != null && seenRelatedClasses.add(cls)) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("relatedTableClassName", cls);
                    row.put("relatedTableCamelCaseName", fm.get("relatedTableCamelCaseName"));
                    relatedLoadTargets.add(row);
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("relatedLoadTargets", relatedLoadTargets);
        data.put("primaryKeyCamelCase", primaryKeyCamelCase);
        data.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));
        boolean hasDetailPage = tableHasDetailPage(tableCode);
        data.put("hasDetailPage", hasDetailPage);
        if (hasDetailPage) {
            data.put("detailRoutePrefix", resolveDetailRoutePrefix(tableCode, businessCode));
        }

        return templateManager.processTemplate("vue_list.vue.ftl", data);
    }
    
    /**
     * 生成Vue表单页面
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return Vue表单页面代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateVueForm(String tableCode, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(fields);
        List<MetadataField> formLayoutFields = fields.stream()
                .filter(CodeGenUtils::fieldParticipatesInForm)
                .collect(Collectors.toList());
        // 获取表关联关系
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(formLayoutFields, relations);
        // 获取表相关的业务规则
        List<Map<String, Object>> businessRules = getTableBusinessRules(tableCode, businessCode);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("primaryKeyCamelCase", primaryKeyCamelCase);
        data.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("businessRules", businessRules);
        data.put("relations", relations);
        data.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));

        return templateManager.processTemplate("vue_form.vue.ftl", data);
    }

    /**
     * 生成 Vue 详情页面（只读，与表单字段同源）
     */
    public String generateVueDetail(String tableCode, String businessCode) throws CodeGenException {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }
        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(fields);
        List<MetadataField> formLayoutFields = fields.stream()
                .filter(CodeGenUtils::fieldParticipatesInForm)
                .collect(Collectors.toList());
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(formLayoutFields, relations);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("primaryKeyCamelCase", primaryKeyCamelCase);
        data.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));
        data.put("formRoutePrefix", resolveFormRoutePrefix(tableCode, businessCode));

        return templateManager.processTemplate("vue_detail.vue.ftl", data);
    }

    public String generateVueReport(String tableCode, String businessCode) throws CodeGenException {
        Map<String, Object> data = buildListPageTemplateData(tableCode, businessCode);
        Map<String, Object> group = findFirstSelectField((List<Map<String, Object>>) data.get("fields"));
        if (group != null) {
            data.put("groupFieldCamelCase", group.get("camelCaseName"));
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMeta = (Map<String, Object>) group.get("field");
            data.put("groupFieldLabel", fieldMeta != null ? fieldMeta.get("label") : "");
        }
        return templateManager.processTemplate("vue_report.vue.ftl", data);
    }

    public String generateVueProcess(String tableCode, String businessCode) throws CodeGenException {
        Map<String, Object> data = buildFormPageTemplateData(tableCode, businessCode);
        Map<String, Object> statusField = findStatusField((List<Map<String, Object>>) data.get("fields"));
        data.put("hasStatusField", statusField != null);
        if (statusField != null) {
            data.put("statusFieldCamelCase", statusField.get("camelCaseName"));
        }
        return templateManager.processTemplate("vue_process.vue.ftl", data);
    }

    public String generateVueImport(String tableCode, String businessCode, boolean batchImport) throws CodeGenException {
        Map<String, Object> data = buildFormPageTemplateData(tableCode, businessCode);
        List<Map<String, Object>> importFields = ((List<Map<String, Object>>) data.get("fields")).stream()
                .filter(f -> !"primary_key".equals(getFieldFormComponent(f)))
                .collect(Collectors.toList());
        data.put("importFields", importFields);
        data.put("importPageTitle", batchImport ? "批量导入" : "数据导入");
        return templateManager.processTemplate("vue_import.vue.ftl", data);
    }

    public String generateVueExport(String tableCode, String businessCode) throws CodeGenException {
        Map<String, Object> data = buildListPageTemplateData(tableCode, businessCode);
        List<Map<String, Object>> exportFields = ((List<Map<String, Object>>) data.get("fields")).stream()
                .filter(f -> !"primary_key".equals(getFieldFormComponent(f)))
                .collect(Collectors.toList());
        data.put("exportFields", exportFields);
        return templateManager.processTemplate("vue_export.vue.ftl", data);
    }

    public boolean tableHasReportPage(String tableCode) {
        return tableHasNodeKind(tableCode, "REPORT");
    }

    public boolean tableHasProcessPage(String tableCode) {
        return tableHasNodeKind(tableCode, "PROCESS");
    }

    public boolean tableHasImportPage(String tableCode) {
        return tableHasNodeKind(tableCode, "IMPORT");
    }

    public boolean tableHasExportPage(String tableCode) {
        return tableHasNodeKind(tableCode, "EXPORT");
    }

    public boolean tableUsesBatchImportPage(String tableCode) {
        if (tableCode == null) {
            return false;
        }
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        if (nodes == null) {
            return false;
        }
        return nodes.stream()
                .filter(n -> n.getIsEnabled() == null || n.getIsEnabled() == 1)
                .anyMatch(n -> "BATCH_IMPORT_PAGE".equalsIgnoreCase(n.getNodeType()));
    }

    private boolean tableHasNodeKind(String tableCode, String kind) {
        if (tableCode == null || kind == null) {
            return false;
        }
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        if (nodes == null) {
            return false;
        }
        return nodes.stream()
                .filter(n -> n.getIsEnabled() == null || n.getIsEnabled() == 1)
                .map(n -> resolveNodePageKind(n.getNodeType()))
                .anyMatch(spec -> spec != null && kind.equals(spec.kind));
    }
    
    /**
     * 生成前端路由配置（routes.js）
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return 前端路由配置代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateRoutes(String tableCode, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        List<Map<String, Object>> routeList = generateTableRoutes(tableCode, businessCode);
        
        Map<String, Object> data = new HashMap<>();
        data.put("routes", routeList);
        data.put("businessCode", businessCode);
        
        return templateManager.processTemplate("routes.js.ftl", data);
    }
    
    /**
     * 生成前端API请求文件
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return 前端API请求文件代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateApi(String tableCode, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }
        
        String className = CodeGenUtils.convertToClassName(table.getTableCode());
        String apiName = className.substring(0, 1).toLowerCase() + className.substring(1);
        
        Map<String, Object> data = new HashMap<>();
        data.put("className", className);
        data.put("apiName", apiName);
        data.put("tableCode", tableCode);
        data.put("businessCode", businessCode);
        
        return templateManager.processTemplate("api.js.ftl", data);
    }
    
    /**
     * 生成前端request.js工具类
     * @return 前端request.js工具类代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateRequestJs() throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        
        return templateManager.processTemplate("request.js.ftl", data);
    }
    
    /**
     * 生成前端认证API文件
     * @return 前端认证API文件代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateAuth(boolean captchaEnabled) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("captchaEnabled", captchaEnabled);
        return templateManager.processTemplate("auth.js.ftl", data);
    }

    public String generateAuth() throws CodeGenException {
        return generateAuth(false);
    }
    
    /**
     * 生成登录页
     * @param businessCode 业务系统编码
     * @return 登录页代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateLoginPage(String businessCode, boolean captchaEnabled) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("captchaEnabled", captchaEnabled);
        return templateManager.processTemplate("login.vue.ftl", data);
    }

    /**
     * 生成验证码输入组件
     */
    public String generateCaptchaInput() throws CodeGenException {
        return templateManager.processTemplate("auth-extension/captcha-input.vue.ftl", new HashMap<>());
    }
    
    /**
     * 生成登录页
     * @param businessCode 业务系统编码
     * @return 登录页代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateLoginPage(String businessCode) throws CodeGenException {
        return generateLoginPage(businessCode, false);
    }
    
    /**
     * 生成前端.env环境配置文件
     * @return 前端.env环境配置文件代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateEnvFile() throws CodeGenException {
        return generateEnvFile(null);
    }

    /**
     * @param appTitle 前端展示标题；为空则用默认
     */
    public String generateEnvFile(String appTitle) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("appTitle", appTitle != null && !appTitle.trim().isEmpty() ? appTitle.trim() : "元数据管理系统");
        return templateManager.processTemplate(".env.ftl", data);
    }

    /**
     * 开箱用环境变量示例（复制为 .env）
     */
    public String generateEnvExampleFile(String appTitle) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("appTitle", appTitle != null && !appTitle.trim().isEmpty() ? appTitle.trim() : "元数据管理系统");
        return templateManager.processTemplate(".env.example.ftl", data);
    }
    
    /**
     * 生成单表的路由配置列表
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return 路由配置列表
     * @throws CodeGenException 代码生成异常
     */
    private List<Map<String, Object>> generateTableRoutes(String tableCode, String businessCode) throws CodeGenException {
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        String componentName = CodeGenUtils.convertToComponentName(table.getTableCode());
        String componentDir = "generated/" + componentName.toLowerCase();

        // 查询与该表相关的功能节点（若数据库中配置了 routePath，则优先使用）
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        
        // 过滤掉禁用的功能节点
        nodes = nodes.stream()
                .filter(node -> node.getIsEnabled() == null || node.getIsEnabled() == 1)
                .collect(Collectors.toList());
        
        List<Map<String, Object>> routeList = new ArrayList<>();
        
        if (!nodes.isEmpty()) {
            // 有配置的功能节点，生成对应的路由
            for (MetadataFunctionNode node : nodes) {
                // 对已配置路由信息或需要默认生成路由的节点生成路由
                NodePageKind pageKind = resolveNodePageKind(node.getNodeType());
                if (node.getRoutePath() != null && !node.getRoutePath().isEmpty()
                        || node.getJumpRelation() != null && !node.getJumpRelation().isEmpty()
                        || pageKind != null) {

                    Map<String, Object> route = new HashMap<>();

                    String pathValue = "";
                    if (node.getRoutePath() != null && !node.getRoutePath().isEmpty()) {
                        pathValue = node.getRoutePath();
                    } else if (node.getJumpRelation() != null && !node.getJumpRelation().isEmpty()) {
                        pathValue = node.getJumpRelation();
                    } else if (pageKind != null) {
                        String basePath = componentName.toLowerCase();
                        pathValue = "/" + businessCode + "/" + basePath + "/" + pageKind.pathSegment
                                + (pageKind.idParam ? "/:id?" : "");
                    } else {
                        continue;
                    }
                    route.put("path", pathValue);

                    String nameValue = node.getNodeCode() != null && !node.getNodeCode().isEmpty()
                            ? node.getNodeCode() : componentName + "List";
                    route.put("name", nameValue);

                    String componentPath;
                    if (node.getComponentPath() != null && !node.getComponentPath().isEmpty()) {
                        if (node.getComponentPath().startsWith("views/")) {
                            componentPath = "@/" + node.getComponentPath();
                        } else {
                            componentPath = "@/views/" + node.getComponentPath();
                        }
                    } else if (pageKind != null) {
                        componentPath = "@/views/" + componentDir + "/" + pageKind.vueFile;
                    } else {
                        componentPath = "@/views/" + componentDir + "/List.vue";
                    }
                    route.put("component", "() => import('" + componentPath + "')");
                    
                    // 生成路由元信息
                    Map<String, Object> meta = new HashMap<>();
                    meta.put("moduleCode", node.getModuleCode() != null ? node.getModuleCode() : "");
                    meta.put("nodeCode", node.getNodeCode() != null ? node.getNodeCode() : "");
                    meta.put("relatedTableCode", node.getRelatedTableCode() != null ? node.getRelatedTableCode() : tableCode);
                    boolean sidebarMenu = CodeGenUtils.isSidebarMenuNodeType(node.getNodeType());
                    int menuVisible = node.getIsMenuVisible() != null ? node.getIsMenuVisible() : (sidebarMenu ? 1 : 0);
                    if (!sidebarMenu) {
                        menuVisible = 0;
                    }
                    meta.put("isMenuVisible", menuVisible);
                    meta.put("nodeType", node.getNodeType() != null ? node.getNodeType() : "");
                    if (menuVisible == 1) {
                        meta.put("title", CodeGenUtils.formatMenuTitle(node.getNodeName(), table.getTableName()));
                    } else {
                        meta.put("title", node.getNodeName() != null ? node.getNodeName() : node.getNodeCode());
                    }
                    meta.put("icon", node.getIcon() != null ? node.getIcon() : "");
                    meta.put("businessCode", businessCode);
                    route.put("meta", meta);
                    
                    routeList.add(route);
                }
            }
        } else {
            // 没有配置功能节点，生成默认的列表页和表单页路由
            // 生成列表页路由
            Map<String, Object> listRoute = new HashMap<>();
            listRoute.put("path", "/" + businessCode + "/" + componentName.toLowerCase() + "/list");
            listRoute.put("name", componentName + "List");
            listRoute.put("component", "() => import('@/views/" + componentDir + "/List.vue')");
            
            Map<String, Object> listMeta = new HashMap<>();
            listMeta.put("relatedTableCode", tableCode);
            listMeta.put("isMenuVisible", 1);
            listMeta.put("nodeType", "LIST_PAGE");
            listMeta.put("title", CodeGenUtils.formatMenuTitle(table.getTableName(), table.getTableName()));
            listMeta.put("businessCode", businessCode);
            listRoute.put("meta", listMeta);
            
            routeList.add(listRoute);
            
            // 生成表单页路由
            Map<String, Object> formRoute = new HashMap<>();
            formRoute.put("path", "/" + businessCode + "/" + componentName.toLowerCase() + "/form/:id?");
            formRoute.put("name", componentName + "Form");
            formRoute.put("component", "() => import('@/views/" + componentDir + "/Form.vue')");
            
            Map<String, Object> formMeta = new HashMap<>();
            formMeta.put("relatedTableCode", tableCode);
            formMeta.put("isMenuVisible", 0);
            formMeta.put("businessCode", businessCode);
            formRoute.put("meta", formMeta);
            
            routeList.add(formRoute);
        }
        
        return routeList;
    }
    
    /**
     * 生成业务系统下所有表的整合路由配置（routes.js）
     * @param businessCode 业务系统编码
     * @return 整合路由配置代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateIntegratedRoutes(String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 获取业务系统下所有启用的表
        List<MetadataTable> tables = tableService.list(null, businessCode);
        List<MetadataTable> enabledTables = tables.stream()
                .filter(table -> table.getIsEnabled() != null && table.getIsEnabled() == 1)
                .collect(Collectors.toList());
        
        if (enabledTables.isEmpty()) {
            throw new CodeGenException("NO_ENABLED_TABLES", "业务系统下没有启用的表: " + businessCode);
        }
        
        // 合并所有表的路由配置
        List<Map<String, Object>> allRoutes = new ArrayList<>();
        for (MetadataTable table : enabledTables) {
            List<Map<String, Object>> tableRoutes = generateTableRoutes(table.getTableCode(), businessCode);
            allRoutes.addAll(tableRoutes);
        }
        
        // 生成完整的routes.js文件
        Map<String, Object> data = new HashMap<>();
        data.put("routes", allRoutes);
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        
        return templateManager.processTemplate("routes.integrated.js.ftl", data);
    }
    
    /**
     * 构建表单预览/生成用数据模型（与 {@code vue_form.vue.ftl} 入参一致，不渲染模板）
     */
    public Map<String, Object> buildFormModel(String tableCode, String businessCode) throws CodeGenException {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }
        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(fields);
        List<MetadataField> formLayoutFields = fields.stream()
                .filter(CodeGenUtils::fieldParticipatesInForm)
                .collect(Collectors.toList());
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(formLayoutFields, relations);
        Map<String, Object> model = new HashMap<>();
        model.put("tableCode", tableCode);
        model.put("tableName", table.getTableName());
        model.put("fields", fieldList);
        model.put("primaryKeyCamelCase", primaryKeyCamelCase);
        model.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        model.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        model.put("businessCode", businessCode);
        model.put("businessRules", getTableBusinessRules(tableCode, businessCode));
        model.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));
        return model;
    }

    /**
     * 构建列表预览/生成用数据模型（与 {@code vue_list.vue.ftl} 入参一致）
     */
    public Map<String, Object> buildListModel(String tableCode, String businessCode) throws CodeGenException {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }
        List<MetadataField> allFields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(allFields);
        List<MetadataField> fields = allFields.stream()
                .filter(f -> !"primary_key".equals(f.getFormComponent()))
                .collect(Collectors.toList());
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);
        Map<String, Object> model = new HashMap<>();
        model.put("tableCode", tableCode);
        model.put("tableName", table.getTableName());
        model.put("fields", fieldList);
        model.put("primaryKeyCamelCase", primaryKeyCamelCase);
        model.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        model.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        model.put("businessCode", businessCode);
        model.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));
        return model;
    }

    private String resolveMenuTitleForTable(String tableCode, String businessCode, MetadataTable table) {
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        for (MetadataFunctionNode node : nodes) {
            if (node.getIsEnabled() != null && node.getIsEnabled() == 0) {
                continue;
            }
            if (!CodeGenUtils.isSidebarMenuNodeType(node.getNodeType())) {
                continue;
            }
            if (node.getIsMenuVisible() != null && node.getIsMenuVisible() == 0) {
                continue;
            }
            return CodeGenUtils.formatMenuTitle(node.getNodeName(), table.getTableName());
        }
        return CodeGenUtils.formatMenuTitle(table.getTableName(), table.getTableName());
    }

    /**
     * 业务系统下全部启用表的整合路由（与 ZIP 内 routes.js 同源）
     */
    public List<Map<String, Object>> collectBusinessRoutes(String businessCode) throws CodeGenException {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        List<MetadataTable> tables = tableService.list(null, businessCode);
        List<Map<String, Object>> allRoutes = new ArrayList<>();
        for (MetadataTable table : tables) {
            if (table.getIsEnabled() != null && table.getIsEnabled() == 1) {
                allRoutes.addAll(generateTableRoutes(table.getTableCode(), businessCode));
            }
        }
        return normalizeMenuRoutes(allRoutes);
    }

    /**
     * 侧栏只保留「xx管理」类列表入口，同一表仅一条菜单项。
     */
    private List<Map<String, Object>> normalizeMenuRoutes(List<Map<String, Object>> routes) {
        java.util.Set<String> menuTables = new java.util.LinkedHashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> route : routes) {
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) route.get("meta");
            if (meta == null) {
                result.add(route);
                continue;
            }
            Object visible = meta.get("isMenuVisible");
            boolean show = visible == null || Integer.valueOf(1).equals(visible) || Boolean.TRUE.equals(visible);
            String tableCode = meta.get("relatedTableCode") != null ? meta.get("relatedTableCode").toString() : "";
            String path = route.get("path") != null ? route.get("path").toString() : "";
            String nodeType = meta.get("nodeType") != null ? meta.get("nodeType").toString() : "";
            boolean listLike = path.contains("/list")
                    || ("LIST_PAGE".equalsIgnoreCase(nodeType) && !path.contains("/form") && !path.contains("/detail"));
            if (show && listLike && !tableCode.isEmpty()) {
                if (menuTables.contains(tableCode)) {
                    meta.put("isMenuVisible", 0);
                } else {
                    menuTables.add(tableCode);
                    meta.put("isMenuVisible", 1);
                    if (meta.get("title") == null || meta.get("title").toString().isEmpty()) {
                        meta.put("title", CodeGenUtils.formatMenuTitle(null, tableCode));
                    }
                }
            }
            result.add(route);
        }
        return result;
    }

    /**
     * 获取表相关的业务规则
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return 表相关的业务规则
     */
    List<Map<String, Object>> getTableBusinessRules(String tableCode, String businessCode) {
        List<Map<String, Object>> tableRules = new ArrayList<>();
        try {
            // 获取表关联的模块编码列表（通过功能节点关联）
            List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode, businessCode);
            Set<String> moduleCodes = nodes.stream()
                .map(MetadataFunctionNode::getModuleCode)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
            
            // 遍历每个关联的模块，获取业务规则
            for (String moduleCode : moduleCodes) {
                // 获取模块的所有业务规则
                List<MetadataBusinessRule> rules = businessRuleService.listByModuleCode(moduleCode, businessCode);
                
                // 筛选出VALIDATION_RULE类型的规则
                for (MetadataBusinessRule rule : rules) {
                    if ("VALIDATION_RULE".equals(rule.getRuleType())) {
                        // 解析规则内容
                        JSONObject ruleContent = JSONObject.parseObject(rule.getRuleContent());
                        if (ruleContent != null) {
                            String ruleType = ruleContent.getString("type");
                            if ("unique".equals(ruleType) || "unique_combo".equals(ruleType)) {
                                // 提取字段列表
                                List<String> ruleFields = new ArrayList<>();
                                if ("unique".equals(ruleType)) {
                                    // 单字段唯一
                                    String field = ruleContent.getString("field");
                                    if (field != null && !field.isEmpty()) {
                                        ruleFields.add(field);
                                    }
                                } else if ("unique_combo".equals(ruleType)) {
                                    // 组合字段唯一
                                    Object fieldsObj = ruleContent.get("fields");
                                    if (fieldsObj instanceof com.alibaba.fastjson2.JSONArray) {
                                        com.alibaba.fastjson2.JSONArray fieldsArray = (com.alibaba.fastjson2.JSONArray) fieldsObj;
                                        for (Object fieldObj : fieldsArray) {
                                            if (fieldObj instanceof String) {
                                                ruleFields.add((String) fieldObj);
                                            }
                                        }
                                    }
                                }
                                
                                // 检查字段是否都存在于当前表中
                                List<MetadataField> tableFields = fieldService.listByTableCode(tableCode);
                                Set<String> tableFieldNames = tableFields.stream()
                                    .map(MetadataField::getFieldName)
                                    .collect(Collectors.toSet());
                                
                                boolean allFieldsExist = true;
                                for (String ruleField : ruleFields) {
                                    if (!tableFieldNames.contains(ruleField)) {
                                        allFieldsExist = false;
                                        break;
                                    }
                                }
                                
                                if (allFieldsExist && !ruleFields.isEmpty()) {
                                    // 将原始字段名转换为包含camelCaseName属性的field对象
                                    List<Map<String, Object>> fieldObjects = new ArrayList<>();
                                    for (String ruleField : ruleFields) {
                                        for (MetadataField tableField : tableFields) {
                                            if (tableField.getFieldName().equals(ruleField)) {
                                                Map<String, Object> fieldMap = new HashMap<>();
                                                fieldMap.put("fieldName", ruleField);
                                                fieldMap.put("camelCaseName", CodeGenUtils.convertToCamelCase(ruleField, false));
                                                fieldObjects.add(fieldMap);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    Map<String, Object> ruleMap = new HashMap<>();
                                    ruleMap.put("ruleCode", rule.getRuleCode());
                                    ruleMap.put("ruleType", ruleType);
                                    ruleMap.put("fields", fieldObjects);
                                    ruleMap.put("message", ruleContent.getString("message"));
                                    ruleMap.put("description", rule.getDescription());
                                    tableRules.add(ruleMap);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取业务规则失败，不影响代码生成
            e.printStackTrace();
        }
        return tableRules;
    }

    /**
     * 表是否配置了详情页功能节点（DETAIL_PAGE 或含 /detail 的路由）
     */
    public boolean tableHasDetailPage(String tableCode) {
        if (tableCode == null || tableCode.isEmpty()) {
            return false;
        }
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        return nodes.stream()
                .filter(n -> n.getIsEnabled() == null || n.getIsEnabled() == 1)
                .anyMatch(this::isDetailNode);
    }

    private boolean isDetailNode(MetadataFunctionNode node) {
        if (node == null) {
            return false;
        }
        String type = node.getNodeType() != null ? node.getNodeType().toUpperCase() : "";
        if ("DETAIL_PAGE".equals(type)) {
            return true;
        }
        String path = firstNonBlank(node.getRoutePath(), node.getJumpRelation());
        return path != null && path.toLowerCase().contains("/detail");
    }

    private boolean isFormNode(MetadataFunctionNode node) {
        if (node == null) {
            return false;
        }
        String type = node.getNodeType() != null ? node.getNodeType().toUpperCase() : "";
        if ("FORM_PAGE".equals(type)) {
            return true;
        }
        String path = firstNonBlank(node.getRoutePath(), node.getJumpRelation());
        return path != null && path.toLowerCase().contains("/form");
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v.trim();
            }
        }
        return null;
    }

    private String normalizeRoutePrefix(String path, String defaultPrefix) {
        if (path == null || path.isEmpty()) {
            return defaultPrefix;
        }
        String normalized = path.replace(":id?", "").replace(":id", "").trim();
        if (!normalized.endsWith("/")) {
            normalized = normalized + "/";
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    private String defaultRoutePrefix(String businessCode, String componentNameLower, String segment) {
        return "/" + businessCode + "/" + componentNameLower + "/" + segment + "/";
    }

    public String resolveDetailRoutePrefix(String tableCode, String businessCode) {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        String componentLower = CodeGenUtils.convertToComponentName(tableCode).toLowerCase();
        String defaultPrefix = defaultRoutePrefix(businessCode, componentLower, "detail");
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        if (nodes == null) {
            return defaultPrefix;
        }
        for (MetadataFunctionNode node : nodes) {
            if (node.getIsEnabled() != null && node.getIsEnabled() == 0) {
                continue;
            }
            if (!isDetailNode(node)) {
                continue;
            }
            String path = firstNonBlank(node.getRoutePath(), node.getJumpRelation());
            if (path != null) {
                return normalizeRoutePrefix(path, defaultPrefix);
            }
        }
        return defaultPrefix;
    }

    public String resolveFormRoutePrefix(String tableCode, String businessCode) {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        String componentLower = CodeGenUtils.convertToComponentName(tableCode).toLowerCase();
        String defaultPrefix = defaultRoutePrefix(businessCode, componentLower, "form");
        List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode);
        if (nodes == null) {
            return defaultPrefix;
        }
        for (MetadataFunctionNode node : nodes) {
            if (node.getIsEnabled() != null && node.getIsEnabled() == 0) {
                continue;
            }
            if (!isFormNode(node)) {
                continue;
            }
            String path = firstNonBlank(node.getRoutePath(), node.getJumpRelation());
            if (path != null) {
                return normalizeRoutePrefix(path, defaultPrefix);
            }
        }
        return defaultPrefix;
    }

    private static final class NodePageKind {
        final String kind;
        final String pathSegment;
        final String vueFile;
        final boolean idParam;

        NodePageKind(String kind, String pathSegment, String vueFile, boolean idParam) {
            this.kind = kind;
            this.pathSegment = pathSegment;
            this.vueFile = vueFile;
            this.idParam = idParam;
        }
    }

    private NodePageKind resolveNodePageKind(String nodeType) {
        if (nodeType == null || nodeType.isEmpty()) {
            return null;
        }
        String t = nodeType.toUpperCase();
        if ("LIST_PAGE".equals(t) || (t.contains("LIST") && !t.contains("BATCH"))) {
            return new NodePageKind("LIST", "list", "List.vue", false);
        }
        if ("FORM_PAGE".equals(t) || (t.contains("FORM") && !t.contains("PLATFORM"))) {
            return new NodePageKind("FORM", "form", "Form.vue", true);
        }
        if ("DETAIL_PAGE".equals(t) || t.contains("DETAIL")) {
            return new NodePageKind("DETAIL", "detail", "Detail.vue", true);
        }
        if ("REPORT_PAGE".equals(t) || t.contains("REPORT")) {
            return new NodePageKind("REPORT", "report", "Report.vue", false);
        }
        if ("PROCESS_PAGE".equals(t) || t.contains("PROCESS")) {
            return new NodePageKind("PROCESS", "process", "Process.vue", false);
        }
        if ("BATCH_IMPORT_PAGE".equals(t)) {
            return new NodePageKind("IMPORT", "batch-import", "Import.vue", false);
        }
        if ("IMPORT_PAGE".equals(t)) {
            return new NodePageKind("IMPORT", "import", "Import.vue", false);
        }
        if ("BATCH_EXPORT_PAGE".equals(t) || t.contains("EXPORT")) {
            return new NodePageKind("EXPORT", "export", "Export.vue", false);
        }
        return null;
    }

    private Map<String, Object> buildListPageTemplateData(String tableCode, String businessCode) throws CodeGenException {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }
        List<MetadataField> allFields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(allFields);
        List<MetadataField> fields = allFields.stream()
                .filter(f -> !"primary_key".equals(f.getFormComponent()))
                .collect(Collectors.toList());
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);
        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("primaryKeyCamelCase", primaryKeyCamelCase);
        data.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));
        return data;
    }

    private Map<String, Object> buildFormPageTemplateData(String tableCode, String businessCode) throws CodeGenException {
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }
        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        String primaryKeyCamelCase = CodeGenUtils.getPrimaryKeyCamelCase(fields);
        List<MetadataField> formLayoutFields = fields.stream()
                .filter(CodeGenUtils::fieldParticipatesInForm)
                .collect(Collectors.toList());
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(formLayoutFields, relations);
        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("primaryKeyCamelCase", primaryKeyCamelCase);
        data.put("componentName", CodeGenUtils.convertToComponentName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("menuTitle", resolveMenuTitleForTable(tableCode, businessCode, table));
        return data;
    }

    private Map<String, Object> findFirstSelectField(List<Map<String, Object>> fieldList) {
        if (fieldList == null) {
            return null;
        }
        for (Map<String, Object> fm : fieldList) {
            if ("select".equals(getFieldFormComponent(fm))) {
                return fm;
            }
        }
        return null;
    }

    private Map<String, Object> findStatusField(List<Map<String, Object>> fieldList) {
        if (fieldList == null) {
            return null;
        }
        for (Map<String, Object> fm : fieldList) {
            @SuppressWarnings("unchecked")
            Map<String, Object> fieldMeta = (Map<String, Object>) fm.get("field");
            if (fieldMeta == null) {
                continue;
            }
            Object fn = fieldMeta.get("fieldName");
            if (fn != null && fn.toString().matches("(?i).*(status|approve|process|flow).*")) {
                return fm;
            }
        }
        return null;
    }

    private String getFieldFormComponent(Map<String, Object> fieldMap) {
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldMeta = (Map<String, Object>) fieldMap.get("field");
        if (fieldMeta == null || fieldMeta.get("formComponent") == null) {
            return "";
        }
        return fieldMeta.get("formComponent").toString();
    }
}