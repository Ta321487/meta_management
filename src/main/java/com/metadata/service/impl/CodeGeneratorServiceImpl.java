package com.metadata.service.impl;

import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.*;
import com.metadata.service.exception.CodeGenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代码生成服务实现，采用代理模式，将代码生成逻辑委托给各个专门的生成器模块
 */
@Service
public class CodeGeneratorServiceImpl implements CodeGeneratorService {

    @Autowired
    private MetadataFieldService fieldService;

    @Autowired
    private MetadataTableService tableService;

    @Autowired
    private MetadataFunctionNodeMapper nodeMapper;

    @Autowired
    private MetadataBusinessSystemService businessSystemService;

    @Autowired
    private MetadataBusinessRuleService businessRuleService;

    @Autowired
    private MetadataTableRelationService relationService;

    // 各个生成器模块
    private final SqlGenerator sqlGenerator;
    private final JavaCodeGenerator javaCodeGenerator;
    private final VueCodeGenerator vueCodeGenerator;
    private final ConfigGenerator configGenerator;

    @Autowired
    public CodeGeneratorServiceImpl(MetadataFieldService fieldService,
                                    MetadataTableService tableService,
                                    MetadataBusinessSystemService businessSystemService,
                                    MetadataFunctionNodeMapper nodeMapper,
                                    MetadataBusinessRuleService businessRuleService,
                                    MetadataTableRelationService relationService) {
        this.fieldService = fieldService;
        this.tableService = tableService;
        this.businessSystemService = businessSystemService;
        this.nodeMapper = nodeMapper;
        this.businessRuleService = businessRuleService;
        this.relationService = relationService;

        // 初始化各个生成器模块
        this.sqlGenerator = new SqlGenerator(tableService, fieldService, businessSystemService, nodeMapper, businessRuleService);
        this.javaCodeGenerator = new JavaCodeGenerator(tableService, fieldService, businessSystemService, nodeMapper, businessRuleService, relationService);
        this.vueCodeGenerator = new VueCodeGenerator(tableService, fieldService, businessSystemService, nodeMapper, businessRuleService, relationService);
        this.configGenerator = new ConfigGenerator();
    }

    /**
     * 生成数据库建表SQL
     */
    @Override
    public String generateCreateTableSQL(String tableCode, String businessCode) throws Exception {
        return sqlGenerator.generateCreateTableSQL(tableCode, businessCode);
    }


    /**
     * 生成Java实体类
     */
    @Override
    public String generateEntity(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateEntity(tableCode, packageName, businessCode);
    }

    /**
     * 生成Controller
     */
    @Override
    public String generateController(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateController(tableCode, packageName, businessCode);
    }

    /**
     * 生成Service
     */
    @Override
    public String generateService(String tableCode, String packageName, String businessCode, boolean useInterface) throws Exception {
        return javaCodeGenerator.generateService(tableCode, packageName, businessCode, useInterface);
    }

    /**
     * 生成Service接口
     */
    @Override
    public String generateServiceInterface(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateServiceInterface(tableCode, packageName, businessCode);
    }

    /**
     * 生成Service实现类
     */
    @Override
    public String generateServiceImpl(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateServiceImpl(tableCode, packageName, businessCode);
    }

    /**
     * 生成Mapper接口
     */
    @Override
    public String generateMapper(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateMapper(tableCode, packageName, businessCode);
    }

    /**
     * 生成Mapper XML
     */
    @Override
    public String generateMapperXml(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateMapperXml(tableCode, packageName, businessCode);
    }

    /**
     * 生成Vue列表页面
     */
    @Override
    public String generateVueList(String tableCode, String businessCode) throws Exception {
        return vueCodeGenerator.generateVueList(tableCode, businessCode);
    }

    /**
     * 生成Vue表单页面
     */
    @Override
    public String generateVueForm(String tableCode, String businessCode) throws Exception {
        return vueCodeGenerator.generateVueForm(tableCode, businessCode);
    }

    /**
     * 生成前端路由配置（routes.js）
     */
    @Override
    public String generateRoutes(String tableCode, String businessCode) throws Exception {
        return vueCodeGenerator.generateRoutes(tableCode, businessCode);
    }

    /**
     * 生成前端API请求文件
     */
    public String generateApi(String tableCode, String businessCode) throws Exception {
        return vueCodeGenerator.generateApi(tableCode, businessCode);
    }

    /**
     * 生成前端认证API文件
     */
    @Override
    public String generateAuth() throws Exception {
        return vueCodeGenerator.generateAuth();
    }

    @Override
    public String generateAuth(boolean captchaEnabled) throws Exception {
        return vueCodeGenerator.generateAuth(captchaEnabled);
    }

    @Override
    public String generateLoginPage(String businessCode, boolean captchaEnabled) throws Exception {
        return vueCodeGenerator.generateLoginPage(businessCode, captchaEnabled);
    }

    @Override
    public String generateCaptchaInput() throws Exception {
        return vueCodeGenerator.generateCaptchaInput();
    }

    @Override
    public Map<String, String> generateAuthExtension(String packageName, boolean captchaEnabled) throws Exception {
        return javaCodeGenerator.generateAuthExtension(packageName, captchaEnabled);
    }

    /**
     * 生成前端request.js工具类
     */
    @Override
    public String generateRequestJs() throws Exception {
        return vueCodeGenerator.generateRequestJs();
    }

    /**
     * 生成登录页
     */
    @Override
    public String generateLoginPage(String businessCode) throws Exception {
        return vueCodeGenerator.generateLoginPage(businessCode);
    }

    /**
     * 生成前端.env环境配置文件
     */
    @Override
    public String generateEnvFile() throws Exception {
        return vueCodeGenerator.generateEnvFile();
    }


    /**
     * 生成业务系统下所有表的整合路由配置（routes.js）
     */
    @Override
    public String generateIntegratedRoutes(String businessCode) throws Exception {
        return vueCodeGenerator.generateIntegratedRoutes(businessCode);
    }

    /**
     * 生成完整的代码包（包含所有文件）
     */
    @Override
    public Map<String, String> generateAll(String tableCode, String packageName, String businessCode, boolean useInterface, boolean captchaEnabled) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;

        MetadataTable tableMeta = tableService.getByCode(tableCode);
        MetadataBusinessSystem bsMeta = businessSystemService.getByCode(businessCode);
        String businessNameDisplay = (bsMeta != null && bsMeta.getBusinessName() != null && !bsMeta.getBusinessName().isEmpty())
                ? bsMeta.getBusinessName() : businessCode;
        String tableNameDisplay = (tableMeta != null && tableMeta.getTableName() != null && !tableMeta.getTableName().isEmpty())
                ? tableMeta.getTableName() : tableCode;
        String envAppTitle = businessNameDisplay + " · " + tableNameDisplay;

        Map<String, String> codeMap = new HashMap<>();

        // 生成SQL
        codeMap.put("create_table.sql", sqlGenerator.generateCreateTableSQL(tableCode, businessCode));

        // 生成Java代码
        codeMap.put("Entity.java", javaCodeGenerator.generateEntity(tableCode, packageName, businessCode));
        codeMap.put("Controller.java", javaCodeGenerator.generateController(tableCode, packageName, businessCode));
        codeMap.put("Service.java", javaCodeGenerator.generateService(tableCode, packageName, businessCode, useInterface));
        codeMap.put("ServiceInterface.java", javaCodeGenerator.generateServiceInterface(tableCode, packageName, businessCode));
        codeMap.put("ServiceImpl.java", javaCodeGenerator.generateServiceImpl(tableCode, packageName, businessCode));
        codeMap.put("Mapper.java", javaCodeGenerator.generateMapper(tableCode, packageName, businessCode));
        codeMap.put("Mapper.xml", javaCodeGenerator.generateMapperXml(tableCode, packageName, businessCode));

        // 生成通用类
        String commonPackage = packageName + ".common";
        codeMap.put("Result.java", javaCodeGenerator.generateResult(commonPackage));
        codeMap.put("PageRequest.java", javaCodeGenerator.generatePageRequest(commonPackage));
        codeMap.put("PageResult.java", javaCodeGenerator.generatePageResult(commonPackage));

        // 生成Vue代码
        codeMap.put("List.vue", vueCodeGenerator.generateVueList(tableCode, businessCode));
        codeMap.put("Form.vue", vueCodeGenerator.generateVueForm(tableCode, businessCode));
        if (vueCodeGenerator.tableHasDetailPage(tableCode)) {
            codeMap.put("Detail.vue", vueCodeGenerator.generateVueDetail(tableCode, businessCode));
        }
        if (vueCodeGenerator.tableHasReportPage(tableCode)) {
            codeMap.put("Report.vue", vueCodeGenerator.generateVueReport(tableCode, businessCode));
        }
        if (vueCodeGenerator.tableHasProcessPage(tableCode)) {
            codeMap.put("Process.vue", vueCodeGenerator.generateVueProcess(tableCode, businessCode));
        }
        if (vueCodeGenerator.tableHasImportPage(tableCode)) {
            codeMap.put("Import.vue", vueCodeGenerator.generateVueImport(tableCode, businessCode, vueCodeGenerator.tableUsesBatchImportPage(tableCode)));
        }
        if (vueCodeGenerator.tableHasExportPage(tableCode)) {
            codeMap.put("Export.vue", vueCodeGenerator.generateVueExport(tableCode, businessCode));
        }
        // 生成登录页
        try {
            codeMap.put("Login.vue", vueCodeGenerator.generateLoginPage(businessCode, captchaEnabled));
        } catch (Exception e) {
            codeMap.put("Login.vue", "<!-- 生成登录页失败: " + e.getMessage() + " -->");
        }
        try {
            codeMap.put("auth.js", vueCodeGenerator.generateAuth(captchaEnabled));
        } catch (Exception e) {
            codeMap.put("auth.js", "// 生成 auth.js 失败: " + e.getMessage());
        }
        if (captchaEnabled) {
            try {
                codeMap.put("CaptchaInput.vue", vueCodeGenerator.generateCaptchaInput());
            } catch (Exception e) {
                codeMap.put("CaptchaInput.vue", "<!-- 生成验证码组件失败: " + e.getMessage() + " -->");
            }
        }
        try {
            codeMap.putAll(javaCodeGenerator.generateAuthExtension(packageName, captchaEnabled));
        } catch (Exception e) {
            codeMap.put("AuthController.java", "// 生成认证扩展包失败: " + e.getMessage());
        }
        // 生成路由配置（供客户集成到前端）
        try {
            codeMap.put("routes.js", vueCodeGenerator.generateRoutes(tableCode, businessCode));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("routes.js", "// 生成路由失败: " + e.getMessage());
        }
        // 生成前端API请求文件
        try {
            codeMap.put("api.js", vueCodeGenerator.generateApi(tableCode, businessCode));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("api.js", "// 生成API失败: " + e.getMessage());
        }
        // 生成前端request.js工具类
        try {
            codeMap.put("request.js", vueCodeGenerator.generateRequestJs());
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("request.js", "// 生成request.js失败: " + e.getMessage());
        }
        // 生成前端.env环境配置文件
        try {
            codeMap.put(".env", vueCodeGenerator.generateEnvFile(envAppTitle));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put(".env", "# 生成.env失败: " + e.getMessage());
        }

        // 生成Spring Boot启动类
        codeMap.put("Application.java", configGenerator.generateApplication(packageName));
        // 生成application.yml配置文件
        codeMap.put("application.yml", configGenerator.generateApplicationConfig(packageName, captchaEnabled));
        // 生成MyBatis配置类
        codeMap.put("MyBatisConfig.java", configGenerator.generateMyBatisConfig(packageName));
        // 生成CORS配置类
        try {
            codeMap.put("CorsConfig.java", configGenerator.generateCorsConfig(packageName));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("CorsConfig.java", "// 生成CorsConfig失败: " + e.getMessage());
        }
        // 生成pom.xml配置文件
        String groupId = packageName;
        String artifactId = packageName.substring(packageName.lastIndexOf(".") + 1);
        String name = artifactId.substring(0, 1).toUpperCase() + artifactId.substring(1);
        String description = name;
        codeMap.put("pom.xml", configGenerator.generatePomXml(groupId, artifactId, name, description));

        // 开箱说明：README.md + .env.example（不阻塞主流程）
        try {
            String entityName = CodeGenUtils.convertToEntityName(tableCode);
            String databaseHint = "your_database";
            if (bsMeta != null && bsMeta.getDatabaseName() != null && !bsMeta.getDatabaseName().trim().isEmpty()) {
                databaseHint = bsMeta.getDatabaseName().trim();
            }
            Map<String, Object> readmeData = new LinkedHashMap<>();
            readmeData.put("businessCode", businessCode);
            readmeData.put("businessName", businessNameDisplay);
            readmeData.put("tableCode", tableCode);
            readmeData.put("tableName", tableNameDisplay);
            readmeData.put("packageName", packageName);
            readmeData.put("packagePath", packageName.replace('.', '/'));
            readmeData.put("artifactId", artifactId);
            readmeData.put("entityName", entityName);
            readmeData.put("apiPrefix", "/api/" + businessCode + "/" + entityName);
            readmeData.put("captchaEnabled", captchaEnabled);
            readmeData.put("databaseHint", databaseHint);
            readmeData.put("javaVersion", "17");
            codeMap.put("README.md", configGenerator.generateOutboxReadme(readmeData));
            codeMap.put(".env.example", vueCodeGenerator.generateEnvExampleFile(envAppTitle));
        } catch (Exception e) {
            codeMap.put("README.md", "# 开箱说明生成失败\n\n" + e.getMessage() + "\n");
            codeMap.put(".env.example", "# .env.example 生成失败: " + e.getMessage() + "\n");
        }

        return codeMap;
    }

    @Override
    public byte[] generateProjectZipByBusinessSystem(String businessCode, String packageName, boolean useInterface, boolean captchaEnabled) throws Exception {
        if (businessCode == null || businessCode.trim().isEmpty()) {
            throw new CodeGenException("BUSINESS_CODE_REQUIRED", "请指定业务系统编码");
        }
        businessCode = businessCode.trim();

        MetadataBusinessSystem bsMeta = businessSystemService.getByCode(businessCode);
        if (bsMeta == null) {
            throw new CodeGenException("BUSINESS_NOT_FOUND", "业务系统不存在: " + businessCode);
        }
        packageName = resolvePackageName(packageName, bsMeta);

        List<MetadataTable> tables = tableService.list(null, businessCode);
        List<MetadataTable> enabledTables = tables.stream()
                .filter(t -> t.getIsEnabled() != null && t.getIsEnabled() == 1)
                .collect(Collectors.toList());
        if (enabledTables.isEmpty()) {
            throw new CodeGenException("NO_ENABLED_TABLES", "业务系统下没有启用的表，无法打包项目: " + businessCode);
        }

        String primaryTableCode = enabledTables.get(0).getTableCode();
        String businessNameDisplay = (bsMeta != null && bsMeta.getBusinessName() != null && !bsMeta.getBusinessName().isEmpty())
                ? bsMeta.getBusinessName() : businessCode;
        String envAppTitle = businessNameDisplay + "（" + enabledTables.size() + " 张表）";

        Map<String, String> map = generateAll(primaryTableCode, packageName, businessCode, useInterface, captchaEnabled);

        try {
            map.put("routes.js", vueCodeGenerator.generateIntegratedRoutes(businessCode));
        } catch (Exception e) {
            // 保留 generateAll 中的单表 routes.js
        }
        map.put("Login.vue", vueCodeGenerator.generateLoginPage(businessCode, captchaEnabled));
        map.put(".env", vueCodeGenerator.generateEnvFile(envAppTitle));
        map.put(".env.example", vueCodeGenerator.generateEnvExampleFile(envAppTitle));

        StringBuilder mergedSql = new StringBuilder();
        List<Map<String, String>> tableSummaries = new ArrayList<>();
        for (MetadataTable t : enabledTables) {
            mergedSql.append("-- ").append(t.getTableCode()).append("\n");
            mergedSql.append(sqlGenerator.generateCreateTableSQL(t.getTableCode(), businessCode));
            mergedSql.append("\n\n");
            Map<String, String> row = new LinkedHashMap<>();
            row.put("tableCode", t.getTableCode());
            row.put("tableName", t.getTableName() != null ? t.getTableName() : t.getTableCode());
            row.put("entityName", CodeGenUtils.convertToEntityName(t.getTableCode()));
            row.put("apiPrefix", "/api/" + businessCode + "/" + row.get("entityName"));
            tableSummaries.add(row);
        }
        map.put("create_table.sql", mergedSql.toString());

        Map<String, String> backendExtras = buildBackendTableFiles(enabledTables, packageName, businessCode, useInterface);
        Map<String, String> frontendExtras = buildFrontendTableFiles(enabledTables, businessCode);

        int dot = packageName.lastIndexOf('.');
        String artifactId = dot >= 0 ? packageName.substring(dot + 1) : packageName;
        frontendExtras.putAll(new FrontendScaffoldGenerator().generate(artifactId, businessNameDisplay, "8080"));

        try {
            String databaseHint = "your_database";
            if (bsMeta != null && bsMeta.getDatabaseName() != null && !bsMeta.getDatabaseName().trim().isEmpty()) {
                databaseHint = bsMeta.getDatabaseName().trim();
            }
            Map<String, Object> readmeData = new LinkedHashMap<>();
            readmeData.put("businessCode", businessCode);
            readmeData.put("businessName", businessNameDisplay);
            readmeData.put("tableCode", primaryTableCode);
            readmeData.put("tableName", enabledTables.size() + " 张启用表");
            readmeData.put("packageName", packageName);
            readmeData.put("packagePath", packageName.replace('.', '/'));
            readmeData.put("artifactId", artifactId);
            readmeData.put("entityName", CodeGenUtils.convertToEntityName(primaryTableCode));
            readmeData.put("apiPrefix", "/api/" + businessCode + "/…");
            readmeData.put("captchaEnabled", captchaEnabled);
            readmeData.put("databaseHint", databaseHint);
            readmeData.put("javaVersion", "17");
            readmeData.put("projectZip", true);
            readmeData.put("enabledTableCount", enabledTables.size());
            readmeData.put("enabledTables", tableSummaries);
            map.put("README.md", configGenerator.generateOutboxReadme(readmeData));
        } catch (Exception e) {
            map.put("README.md", "# 开箱说明生成失败\n\n" + e.getMessage() + "\n");
        }

        return GeneratedProjectZipPackager.pack(map, backendExtras, frontendExtras, packageName, businessCode);
    }

    private static String resolvePackageName(String packageName, MetadataBusinessSystem bsMeta) {
        if (packageName != null && !packageName.trim().isEmpty()) {
            return packageName.trim();
        }
        if (bsMeta.getPackageName() != null && !bsMeta.getPackageName().trim().isEmpty()) {
            return bsMeta.getPackageName().trim();
        }
        return "com.example";
    }

    private Map<String, String> buildBackendTableFiles(List<MetadataTable> enabledTables, String packageName,
                                                       String businessCode, boolean useInterface) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        String pkgPath = packageName.replace('.', '/');
        String javaBase = "src/main/java/" + pkgPath;
        String mapperXmlBase = "src/main/resources/mapper";
        for (MetadataTable t : enabledTables) {
            String tableCode = t.getTableCode();
            String className = CodeGenUtils.convertToClassName(tableCode);
            files.put(javaBase + "/entity/" + className + ".java",
                    javaCodeGenerator.generateEntity(tableCode, packageName, businessCode));
            files.put(javaBase + "/controller/" + className + "Controller.java",
                    javaCodeGenerator.generateController(tableCode, packageName, businessCode));
            files.put(javaBase + "/mapper/" + className + "Mapper.java",
                    javaCodeGenerator.generateMapper(tableCode, packageName, businessCode));
            files.put(mapperXmlBase + "/" + className + "Mapper.xml",
                    javaCodeGenerator.generateMapperXml(tableCode, packageName, businessCode));
            if (useInterface) {
                files.put(javaBase + "/service/" + className + "Service.java",
                        javaCodeGenerator.generateServiceInterface(tableCode, packageName, businessCode));
                files.put(javaBase + "/service/impl/" + className + "ServiceImpl.java",
                        javaCodeGenerator.generateServiceImpl(tableCode, packageName, businessCode));
            } else {
                files.put(javaBase + "/service/" + className + "Service.java",
                        javaCodeGenerator.generateService(tableCode, packageName, businessCode, false));
            }
        }
        return files;
    }

    private Map<String, String> buildFrontendTableFiles(List<MetadataTable> enabledTables, String businessCode) throws Exception {
        Map<String, String> files = new LinkedHashMap<>();
        for (MetadataTable t : enabledTables) {
            String leaf = CodeGenUtils.convertToComponentName(t.getTableCode()).toLowerCase();
            String base = "frontend/src/views/generated/" + leaf;
            String tableCode = t.getTableCode();
            files.put(base + "/List.vue", vueCodeGenerator.generateVueList(tableCode, businessCode));
            files.put(base + "/Form.vue", vueCodeGenerator.generateVueForm(tableCode, businessCode));
            if (vueCodeGenerator.tableHasDetailPage(tableCode)) {
                files.put(base + "/Detail.vue", vueCodeGenerator.generateVueDetail(tableCode, businessCode));
            }
            if (vueCodeGenerator.tableHasReportPage(tableCode)) {
                files.put(base + "/Report.vue", vueCodeGenerator.generateVueReport(tableCode, businessCode));
            }
            if (vueCodeGenerator.tableHasProcessPage(tableCode)) {
                files.put(base + "/Process.vue", vueCodeGenerator.generateVueProcess(tableCode, businessCode));
            }
            if (vueCodeGenerator.tableHasImportPage(tableCode)) {
                files.put(base + "/Import.vue", vueCodeGenerator.generateVueImport(tableCode, businessCode, vueCodeGenerator.tableUsesBatchImportPage(tableCode)));
            }
            if (vueCodeGenerator.tableHasExportPage(tableCode)) {
                files.put(base + "/Export.vue", vueCodeGenerator.generateVueExport(tableCode, businessCode));
            }
            files.put(base + "/api.js", vueCodeGenerator.generateApi(tableCode, businessCode));
        }
        return files;
    }

    /**
     * 生成业务系统下所有表的完整代码包
     */
    @Override
    public Map<String, Map<String, String>> generateAllByBusinessSystem(String businessCode, String packageName, boolean useInterface) throws Exception {
        Map<String, Map<String, String>> allCodeMap = new HashMap<>();

        // 获取业务系统下所有表
        List<MetadataTable> tables = tableService.list(null, businessCode);

        // 为每个表生成代码
        for (MetadataTable table : tables) {
            if (table.getIsEnabled() == 1) { // 只处理启用的表
                Map<String, String> codeMap = generateAll(table.getTableCode(), packageName, businessCode, useInterface);
                allCodeMap.put(table.getTableCode(), codeMap);
            }
        }

        return allCodeMap;
    }

    /**
     * 生成业务系统下所有表的建表SQL
     */
    @Override
    public Map<String, String> generateAllSQLByBusinessSystem(String businessCode) throws Exception {
        Map<String, String> sqlMap = new HashMap<>();

        // 获取业务系统下所有表
        List<MetadataTable> tables = tableService.list(null, businessCode);

        // 为每个表生成SQL
        for (MetadataTable table : tables) {
            if (table.getIsEnabled() == 1) { // 只处理启用的表
                String sql = generateCreateTableSQL(table.getTableCode(), businessCode);
                sqlMap.put(table.getTableCode() + ".sql", sql);
            }
        }

        return sqlMap;
    }

    /**
     * 工具方法：转换为表名（下划线）
     */
    @Override
    public String convertToTableName(String code) {
        return CodeGenUtils.convertToTableName(code);
    }

    /**
     * 获取Java类型
     */
    @Override
    public String getJavaType(String fieldType) {
        return CodeGenUtils.getJavaType(fieldType);
    }

    /**
     * 检查字段列表中是否有日期类型
     */
    @Override
    public boolean hasDate(List<MetadataField> fields) {
        return CodeGenUtils.hasDate(fields);
    }

    /**
     * 检查字段列表中是否有Decimal类型
     */
    @Override
    public boolean hasDecimal(List<MetadataField> fields) {
        return CodeGenUtils.hasDecimal(fields);
    }

    /**
     * 生成CHECK约束
     */
    @Override
    public String generateCheckConstraint(MetadataField field) {
        return CodeGenUtils.generateCheckConstraint(field);
    }

    /**
     * 生成添加字段的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableAddColumnSQL(String tableCode, MetadataField field) throws Exception {
        return sqlGenerator.generateAlterTableAddColumnSQL(tableCode, field);
    }

    /**
     * 生成修改字段的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableModifyColumnSQL(String tableCode, MetadataField field) throws Exception {
        return sqlGenerator.generateAlterTableModifyColumnSQL(tableCode, field);
    }

    /**
     * 生成修改字段名称和属性的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableChangeColumnSQL(String tableCode, String oldFieldName, MetadataField field) throws Exception {
        return sqlGenerator.generateAlterTableChangeColumnSQL(tableCode, oldFieldName, field);
    }

    /**
     * 生成删除字段的ALTER TABLE语句
     */
    @Override
    public String generateAlterTableDropColumnSQL(String tableCode, String fieldName) throws Exception {
        return sqlGenerator.generateAlterTableDropColumnSQL(tableCode, fieldName);
    }

    /**
     * 生成Result统一响应结果类
     */
    @Override
    public String generateResult(String packageName) throws Exception {
        return javaCodeGenerator.generateResult(packageName);
    }

    /**
     * 生成PageRequest分页请求类
     */
    @Override
    public String generatePageRequest(String packageName) throws Exception {
        return javaCodeGenerator.generatePageRequest(packageName);
    }

    /**
     * 生成PageResult分页结果类
     */
    @Override
    public String generatePageResult(String packageName) throws Exception {
        return javaCodeGenerator.generatePageResult(packageName);
    }

    /**
     * 生成Spring Boot启动类
     */
    @Override
    public String generateApplication(String packageName) throws Exception {
        return configGenerator.generateApplication(packageName);
    }

    /**
     * 生成application.yml配置文件
     */
    @Override
    public String generateApplicationConfig(String packageName) throws Exception {
        return configGenerator.generateApplicationConfig(packageName);
    }

    @Override
    public String generateApplicationConfig(String packageName, boolean captchaEnabled) throws Exception {
        return configGenerator.generateApplicationConfig(packageName, captchaEnabled);
    }

    /**
     * 生成MyBatis配置类
     */
    @Override
    public String generateMyBatisConfig(String packageName) throws Exception {
        return configGenerator.generateMyBatisConfig(packageName);
    }

    /**
     * 生成CORS配置类
     */
    @Override
    public String generateCorsConfig(String packageName) throws Exception {
        return configGenerator.generateCorsConfig(packageName);
    }

    /**
     * 生成pom.xml配置文件
     */
    @Override
    public String generatePomXml(String groupId, String artifactId, String name, String description) throws Exception {
        return configGenerator.generatePomXml(groupId, artifactId, name, description);
    }
}