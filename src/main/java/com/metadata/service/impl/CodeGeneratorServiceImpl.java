package com.metadata.service.impl;

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
    
    // 各个生成器模块
    private SqlGenerator sqlGenerator;
    private JavaCodeGenerator javaCodeGenerator;
    private VueCodeGenerator vueCodeGenerator;
    private ConfigGenerator configGenerator;

    @Autowired
    public CodeGeneratorServiceImpl(MetadataFieldService fieldService,
                                   MetadataTableService tableService,
                                   MetadataBusinessSystemService businessSystemService,
                                   MetadataFunctionNodeMapper nodeMapper,
                                   MetadataBusinessRuleService businessRuleService) {
        this.fieldService = fieldService;
        this.tableService = tableService;
        this.businessSystemService = businessSystemService;
        this.nodeMapper = nodeMapper;
        this.businessRuleService = businessRuleService;
        
        // 初始化各个生成器模块
        this.sqlGenerator = new SqlGenerator(tableService, fieldService, businessSystemService, nodeMapper, businessRuleService);
        this.javaCodeGenerator = new JavaCodeGenerator(tableService, fieldService, businessSystemService, nodeMapper, businessRuleService);
        this.vueCodeGenerator = new VueCodeGenerator(tableService, fieldService, businessSystemService, nodeMapper, businessRuleService);
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
    public String generateService(String tableCode, String packageName, String businessCode) throws Exception {
        return javaCodeGenerator.generateService(tableCode, packageName, businessCode);
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
    public Map<String, String> generateAll(String tableCode, String packageName, String businessCode) throws Exception {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        Map<String, String> codeMap = new HashMap<>();

        // 生成SQL
        codeMap.put("create_table.sql", sqlGenerator.generateCreateTableSQL(tableCode, businessCode));

        // 生成Java代码
        codeMap.put("Entity.java", javaCodeGenerator.generateEntity(tableCode, packageName, businessCode));
        codeMap.put("Controller.java", javaCodeGenerator.generateController(tableCode, packageName, businessCode));
        codeMap.put("Service.java", javaCodeGenerator.generateService(tableCode, packageName, businessCode));
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
        // 生成登录页
        try {
            codeMap.put("Login.vue", vueCodeGenerator.generateLoginPage(businessCode));
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put("Login.vue", "<!-- 生成登录页失败: " + e.getMessage() + " -->");
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
            codeMap.put(".env", vueCodeGenerator.generateEnvFile());
        } catch (Exception e) {
            // 不阻塞主流程，记录但仍返回其他文件
            codeMap.put(".env", "# 生成.env失败: " + e.getMessage());
        }

        // 生成Spring Boot启动类
        codeMap.put("Application.java", configGenerator.generateApplication(packageName));
        // 生成application.yml配置文件
        codeMap.put("application.yml", configGenerator.generateApplicationConfig(packageName));
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

        return codeMap;
    }
    
    /**
     * 生成业务系统下所有表的完整代码包
     */
    @Override
    public Map<String, Map<String, String>> generateAllByBusinessSystem(String businessCode, String packageName) throws Exception {
        Map<String, Map<String, String>> allCodeMap = new HashMap<>();
        
        // 获取业务系统下所有表
        List<MetadataTable> tables = tableService.list(null, businessCode);
        
        // 为每个表生成代码
        for (MetadataTable table : tables) {
            if (table.getIsEnabled() == 1) { // 只处理启用的表
                Map<String, String> codeMap = generateAll(table.getTableCode(), packageName, businessCode);
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