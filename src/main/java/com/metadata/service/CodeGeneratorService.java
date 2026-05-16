package com.metadata.service;

import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTable;

import java.util.List;
import java.util.Map;

/**
 * 代码生成服务接口
 */
public interface CodeGeneratorService {

    /**
     * 生成数据库建表SQL
     */
    String generateCreateTableSQL(String tableCode, String businessCode) throws Exception;

    /**
     * 生成数据库建表SQL（默认业务系统）
     */
    default String generateCreateTableSQL(String tableCode) throws Exception {
        return generateCreateTableSQL(tableCode, "DEFAULT");
    }

    /**
     * 生成Java实体类
     */
    String generateEntity(String tableCode, String packageName, String businessCode) throws Exception;

    /**
     * 生成Java实体类（默认业务系统）
     */
    default String generateEntity(String tableCode, String packageName) throws Exception {
        return generateEntity(tableCode, packageName, "DEFAULT");
    }

    /**
     * 生成Controller
     */
    String generateController(String tableCode, String packageName, String businessCode) throws Exception;

    /**
     * 生成Controller（默认业务系统）
     */
    default String generateController(String tableCode, String packageName) throws Exception {
        return generateController(tableCode, packageName, "DEFAULT");
    }

    /**
     * 生成Service
     */
    String generateService(String tableCode, String packageName, String businessCode, boolean useInterface) throws Exception;

    /**
     * 生成Service（默认业务系统）
     */
    default String generateService(String tableCode, String packageName) throws Exception {
        return generateService(tableCode, packageName, "DEFAULT", false);
    }

    /**
     * 生成Service（默认业务系统，指定是否使用接口）
     */
    default String generateService(String tableCode, String packageName, boolean useInterface) throws Exception {
        return generateService(tableCode, packageName, "DEFAULT", useInterface);
    }

    /**
     * 生成Service接口
     */
    String generateServiceInterface(String tableCode, String packageName, String businessCode) throws Exception;

    /**
     * 生成Service接口（默认业务系统）
     */
    default String generateServiceInterface(String tableCode, String packageName) throws Exception {
        return generateServiceInterface(tableCode, packageName, "DEFAULT");
    }

    /**
     * 生成Service实现类
     */
    String generateServiceImpl(String tableCode, String packageName, String businessCode) throws Exception;

    /**
     * 生成Service实现类（默认业务系统）
     */
    default String generateServiceImpl(String tableCode, String packageName) throws Exception {
        return generateServiceImpl(tableCode, packageName, "DEFAULT");
    }

    /**
     * 生成Mapper接口
     */
    String generateMapper(String tableCode, String packageName, String businessCode) throws Exception;

    /**
     * 生成Mapper接口（默认业务系统）
     */
    default String generateMapper(String tableCode, String packageName) throws Exception {
        return generateMapper(tableCode, packageName, "DEFAULT");
    }

    /**
     * 生成Mapper XML
     */
    String generateMapperXml(String tableCode, String packageName, String businessCode) throws Exception;

    /**
     * 生成Mapper XML（默认业务系统）
     */
    default String generateMapperXml(String tableCode, String packageName) throws Exception {
        return generateMapperXml(tableCode, packageName, "DEFAULT");
    }

    /**
     * 生成Vue列表页面
     */
    String generateVueList(String tableCode, String businessCode) throws Exception;

    /**
     * 生成Vue列表页面（默认业务系统）
     */
    default String generateVueList(String tableCode) throws Exception {
        return generateVueList(tableCode, "DEFAULT");
    }

    /**
     * 生成Vue表单页面
     */
    String generateVueForm(String tableCode, String businessCode) throws Exception;

    /**
     * 生成Vue表单页面（默认业务系统）
     */
    default String generateVueForm(String tableCode) throws Exception {
        return generateVueForm(tableCode, "DEFAULT");
    }

    /**
     * 生成前端路由配置（routes.js）
     */
    String generateRoutes(String tableCode, String businessCode) throws Exception;

    /**
     * 生成前端路由配置（routes.js）（默认业务系统）
     */
    default String generateRoutes(String tableCode) throws Exception {
        return generateRoutes(tableCode, "DEFAULT");
    }

    /**
     * 生成业务系统下所有表的整合路由配置（routes.js）
     */
    String generateIntegratedRoutes(String businessCode) throws Exception;

    /**
     * 生成完整的代码包（包含所有文件，可选验证码扩展包）
     */
    Map<String, String> generateAll(String tableCode, String packageName, String businessCode, boolean useInterface, boolean captchaEnabled) throws Exception;

    /**
     * 按业务系统打包完整项目 ZIP（全部启用表的 SQL、后端、可运行前端）。
     *
     * @param businessCode  业务系统编码（必填）
     * @param packageName   Java 根包名；为空时取业务系统配置的 packageName
     */
    byte[] generateProjectZipByBusinessSystem(String businessCode, String packageName, boolean useInterface, boolean captchaEnabled) throws Exception;

    default Map<String, String> generateAll(String tableCode, String packageName, String businessCode, boolean useInterface) throws Exception {
        return generateAll(tableCode, packageName, businessCode, useInterface, false);
    }

    /**
     * 生成完整的代码包（包含所有文件）（默认业务系统）
     */
    default Map<String, String> generateAll(String tableCode, String packageName) throws Exception {
        return generateAll(tableCode, packageName, "DEFAULT", false);
    }

    /**
     * 生成完整的代码包（包含所有文件）（默认业务系统，指定是否使用接口）
     */
    default Map<String, String> generateAll(String tableCode, String packageName, boolean useInterface) throws Exception {
        return generateAll(tableCode, packageName, "DEFAULT", useInterface);
    }

    /**
     * 生成业务系统下所有表的完整代码包
     */
    Map<String, Map<String, String>> generateAllByBusinessSystem(String businessCode, String packageName, boolean useInterface) throws Exception;

    /**
     * 生成业务系统下所有表的完整代码包（默认不使用接口）
     */
    default Map<String, Map<String, String>> generateAllByBusinessSystem(String businessCode, String packageName) throws Exception {
        return generateAllByBusinessSystem(businessCode, packageName, false);
    }

    /**
     * 生成业务系统下所有表的建表SQL
     */
    Map<String, String> generateAllSQLByBusinessSystem(String businessCode) throws Exception;

    /**
     * 工具方法：转换为表名（下划线）
     */
    String convertToTableName(String code);

    /**
     * 获取Java类型
     */
    String getJavaType(String fieldType);

    /**
     * 检查字段列表中是否有日期类型
     */
    boolean hasDate(List<MetadataField> fields);

    /**
     * 检查字段列表中是否有Decimal类型
     */
    boolean hasDecimal(List<MetadataField> fields);

    /**
     * 生成CHECK约束
     */
    String generateCheckConstraint(MetadataField field);

    /**
     * 生成添加字段的ALTER TABLE语句
     */
    String generateAlterTableAddColumnSQL(String tableCode, MetadataField field) throws Exception;

    /**
     * 生成修改字段的ALTER TABLE语句
     */
    String generateAlterTableModifyColumnSQL(String tableCode, MetadataField field) throws Exception;

    /**
     * 生成修改字段名称和属性的ALTER TABLE语句
     */
    String generateAlterTableChangeColumnSQL(String tableCode, String oldFieldName, MetadataField field) throws Exception;

    /**
     * 生成删除字段的ALTER TABLE语句
     */
    String generateAlterTableDropColumnSQL(String tableCode, String fieldName) throws Exception;

    /**
     * 生成Result统一响应结果类
     */
    String generateResult(String packageName) throws Exception;

    /**
     * 生成PageRequest分页请求类
     */
    String generatePageRequest(String packageName) throws Exception;

    /**
     * 生成PageResult分页结果类
     */
    String generatePageResult(String packageName) throws Exception;

    /**
     * 生成Spring Boot启动类
     */
    String generateApplication(String packageName) throws Exception;

    /**
     * 生成application.yml配置文件
     */
    String generateApplicationConfig(String packageName) throws Exception;

    String generateApplicationConfig(String packageName, boolean captchaEnabled) throws Exception;

    /**
     * 生成MyBatis配置类
     */
    String generateMyBatisConfig(String packageName) throws Exception;

    /**
     * 生成CORS配置类
     */
    String generateCorsConfig(String packageName) throws Exception;

    /**
     * 生成前端request.js工具类
     */
    String generateRequestJs() throws Exception;

    /**
     * 生成前端.env环境配置文件
     */
    String generateEnvFile() throws Exception;

    /**
     * 生成登录页
     */
    String generateLoginPage(String businessCode) throws Exception;

    String generateLoginPage(String businessCode, boolean captchaEnabled) throws Exception;

    /**
     * 生成前端API请求文件
     */
    String generateApi(String tableCode, String businessCode) throws Exception;

    /**
     * 生成前端认证API文件
     */
    String generateAuth() throws Exception;

    String generateAuth(boolean captchaEnabled) throws Exception;

    /**
     * 生成验证码输入组件
     */
    String generateCaptchaInput() throws Exception;

    /**
     * 生成认证扩展包（后端 Java 文件）
     */
    Map<String, String> generateAuthExtension(String packageName, boolean captchaEnabled) throws Exception;

    /**
     * 生成pom.xml配置文件
     */
    String generatePomXml(String groupId, String artifactId, String name, String description) throws Exception;
}