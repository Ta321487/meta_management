package com.metadata.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成结果转换工具
 * <p>Service 内部仍使用 Map 组装；Controller 对外返回固定结构 DTO，便于 Swagger 文档展示。</p>
 */
public final class GeneratedCodeBundleMapper {

    private GeneratedCodeBundleMapper() {
    }

    /**
     * Map（文件名 → 源码）转为单表代码包 DTO
     */
    public static TableGeneratedCodeBundle fromMap(Map<String, String> m) {
        TableGeneratedCodeBundle b = new TableGeneratedCodeBundle();
        if (m == null) {
            return b;
        }
        b.setCreateTableSql(m.get("create_table.sql"));
        b.setEntityJava(m.get("Entity.java"));
        b.setControllerJava(m.get("Controller.java"));
        b.setServiceJava(m.get("Service.java"));
        b.setServiceInterfaceJava(m.get("ServiceInterface.java"));
        b.setServiceImplJava(m.get("ServiceImpl.java"));
        b.setMapperJava(m.get("Mapper.java"));
        b.setMapperXml(m.get("Mapper.xml"));
        b.setResultJava(m.get("Result.java"));
        b.setPageRequestJava(m.get("PageRequest.java"));
        b.setPageResultJava(m.get("PageResult.java"));
        b.setListVue(m.get("List.vue"));
        b.setFormVue(m.get("Form.vue"));
        b.setLoginVue(m.get("Login.vue"));
        b.setAuthJs(m.get("auth.js"));
        b.setCaptchaInputVue(m.get("CaptchaInput.vue"));
        b.setAuthControllerJava(m.get("AuthController.java"));
        b.setCaptchaServiceJava(m.get("CaptchaService.java"));
        b.setAuthServiceJava(m.get("AuthService.java"));
        b.setAuthServiceImplJava(m.get("AuthServiceImpl.java"));
        b.setLoginRequestJava(m.get("LoginRequest.java"));
        b.setLoginInterceptorJava(m.get("LoginInterceptor.java"));
        b.setInterceptorConfigJava(m.get("InterceptorConfig.java"));
        b.setRoutesJs(m.get("routes.js"));
        b.setApiJs(m.get("api.js"));
        b.setRequestJs(m.get("request.js"));
        b.setEnvFile(m.get(".env"));
        b.setApplicationJava(m.get("Application.java"));
        b.setApplicationYml(m.get("application.yml"));
        b.setMyBatisConfigJava(m.get("MyBatisConfig.java"));
        b.setCorsConfigJava(m.get("CorsConfig.java"));
        b.setPomXml(m.get("pom.xml"));
        return b;
    }

    /**
     * 单表代码包 DTO 转为 Map（文件名 → 源码）
     */
    public static Map<String, String> toMap(TableGeneratedCodeBundle b) {
        Map<String, String> m = new LinkedHashMap<>();
        if (b == null) {
            return m;
        }
        putIfPresent(m, "create_table.sql", b.getCreateTableSql());
        putIfPresent(m, "Entity.java", b.getEntityJava());
        putIfPresent(m, "Controller.java", b.getControllerJava());
        putIfPresent(m, "Service.java", b.getServiceJava());
        putIfPresent(m, "ServiceInterface.java", b.getServiceInterfaceJava());
        putIfPresent(m, "ServiceImpl.java", b.getServiceImplJava());
        putIfPresent(m, "Mapper.java", b.getMapperJava());
        putIfPresent(m, "Mapper.xml", b.getMapperXml());
        putIfPresent(m, "Result.java", b.getResultJava());
        putIfPresent(m, "PageRequest.java", b.getPageRequestJava());
        putIfPresent(m, "PageResult.java", b.getPageResultJava());
        putIfPresent(m, "List.vue", b.getListVue());
        putIfPresent(m, "Form.vue", b.getFormVue());
        putIfPresent(m, "Login.vue", b.getLoginVue());
        putIfPresent(m, "auth.js", b.getAuthJs());
        putIfPresent(m, "CaptchaInput.vue", b.getCaptchaInputVue());
        putIfPresent(m, "AuthController.java", b.getAuthControllerJava());
        putIfPresent(m, "CaptchaService.java", b.getCaptchaServiceJava());
        putIfPresent(m, "AuthService.java", b.getAuthServiceJava());
        putIfPresent(m, "AuthServiceImpl.java", b.getAuthServiceImplJava());
        putIfPresent(m, "LoginRequest.java", b.getLoginRequestJava());
        putIfPresent(m, "LoginInterceptor.java", b.getLoginInterceptorJava());
        putIfPresent(m, "InterceptorConfig.java", b.getInterceptorConfigJava());
        putIfPresent(m, "routes.js", b.getRoutesJs());
        putIfPresent(m, "api.js", b.getApiJs());
        putIfPresent(m, "request.js", b.getRequestJs());
        putIfPresent(m, ".env", b.getEnvFile());
        putIfPresent(m, "Application.java", b.getApplicationJava());
        putIfPresent(m, "application.yml", b.getApplicationYml());
        putIfPresent(m, "MyBatisConfig.java", b.getMyBatisConfigJava());
        putIfPresent(m, "CorsConfig.java", b.getCorsConfigJava());
        putIfPresent(m, "pom.xml", b.getPomXml());
        return m;
    }

    /**
     * 认证扩展包 Map 转为 DTO
     */
    public static AuthExtensionCodeBundle authFromMap(Map<String, String> m) {
        AuthExtensionCodeBundle b = new AuthExtensionCodeBundle();
        if (m == null) {
            return b;
        }
        b.setCaptchaServiceJava(m.get("CaptchaService.java"));
        b.setAuthServiceJava(m.get("AuthService.java"));
        b.setAuthServiceImplJava(m.get("AuthServiceImpl.java"));
        b.setAuthControllerJava(m.get("AuthController.java"));
        b.setLoginRequestJava(m.get("LoginRequest.java"));
        b.setLoginInterceptorJava(m.get("LoginInterceptor.java"));
        b.setInterceptorConfigJava(m.get("InterceptorConfig.java"));
        return b;
    }

    /**
     * 业务系统 SQL Map（表编码.sql → SQL）转为列表结构 DTO
     */
    public static BusinessSystemSqlExportPayload sqlExportFromMap(Map<String, String> sqlMap) {
        BusinessSystemSqlExportPayload payload = new BusinessSystemSqlExportPayload();
        List<TableSqlFile> files = new ArrayList<>();
        if (sqlMap != null) {
            for (Map.Entry<String, String> e : sqlMap.entrySet()) {
                String key = e.getKey();
                String tableCode = key.endsWith(".sql") ? key.substring(0, key.length() - 4) : key;
                files.add(new TableSqlFile(tableCode, e.getValue()));
            }
        }
        payload.setSqlFiles(files);
        return payload;
    }

    /**
     * 业务系统多表代码 Map 转为列表结构 DTO
     */
    public static BusinessSystemTableCodesPayload tableCodesFromMap(Map<String, Map<String, String>> allMap) {
        BusinessSystemTableCodesPayload payload = new BusinessSystemTableCodesPayload();
        List<TableGeneratedCodeEntry> tables = new ArrayList<>();
        if (allMap != null) {
            for (Map.Entry<String, Map<String, String>> e : allMap.entrySet()) {
                tables.add(new TableGeneratedCodeEntry(e.getKey(), fromMap(e.getValue())));
            }
        }
        payload.setTables(tables);
        return payload;
    }

    private static void putIfPresent(Map<String, String> m, String key, String value) {
        if (value != null) {
            m.put(key, value);
        }
    }
}
