package com.metadata.service.impl;

import com.metadata.service.exception.CodeGenException;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置文件生成模块，负责配置文件生成相关逻辑
 */
public class ConfigGenerator {

    private TemplateManager templateManager;

    /**
     * 构造方法
     */
    public ConfigGenerator() {
        this.templateManager = TemplateManager.getInstance();
    }

    /**
     * 生成Spring Boot启动类
     *
     * @param packageName 包名
     * @return Spring Boot启动类代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateApplication(String packageName) throws CodeGenException {
        return "package " + packageName + ";\n\n" +
                "import org.mybatis.spring.annotation.MapperScan;\n" +
                "import org.springframework.boot.SpringApplication;\n" +
                "import org.springframework.boot.autoconfigure.SpringBootApplication;\n\n" +
                "/**\n" +
                " * Spring Boot应用启动类\n" +
                " */\n" +
                "@SpringBootApplication\n" +
                "@MapperScan(\"" + packageName + ".mapper\")\n" +
                "public class Application {\n" +
                "    public static void main(String[] args) {\n" +
                "        SpringApplication.run(Application.class, args);\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成application.yml配置文件
     *
     * @param packageName 包名
     * @return application.yml配置文件代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateApplicationConfig(String packageName) throws CodeGenException {
        return generateApplicationConfig(packageName, false);
    }

    public String generateApplicationConfig(String packageName, boolean captchaEnabled) throws CodeGenException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Spring Boot 应用配置\n");
        sb.append("spring:\n");
        sb.append("  application:\n");
        sb.append("    name: application\n");
        sb.append("  datasource:\n");
        sb.append("    # 数据库连接配置\n");
        sb.append("    url: jdbc:mysql://localhost:3306/your_database?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true\n");
        sb.append("    username: root\n");
        sb.append("    password: your_password\n");
        sb.append("    driver-class-name: com.mysql.cj.jdbc.Driver\n");
        sb.append("\n");
        sb.append("# 服务器配置\n");
        sb.append("server:\n");
        sb.append("  port: 8080\n");
        sb.append("\n");
        sb.append("# 日志配置\n");
        sb.append("logging:\n");
        sb.append("  level:\n");
        sb.append("    root: INFO\n");
        sb.append("    ").append(packageName).append(": DEBUG\n");
        if (captchaEnabled) {
            sb.append("\n");
            sb.append("# 认证扩展包（验证码 + Session 登录）\n");
            sb.append("app:\n");
            sb.append("  auth:\n");
            sb.append("    default-username: admin\n");
            sb.append("    default-password: admin123\n");
            sb.append("  captcha:\n");
            sb.append("    enabled: true\n");
            sb.append("    length: 4\n");
            sb.append("    expire-seconds: 300\n");
        }
        return sb.toString();
    }

    /**
     * 生成MyBatis配置类
     *
     * @param packageName 包名
     * @return MyBatis配置类代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateMyBatisConfig(String packageName) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);

        return templateManager.processTemplate("mybatis-config.java.ftl", data);
    }

    /**
     * 生成CORS配置类
     *
     * @param packageName 包名
     * @return CORS配置类代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateCorsConfig(String packageName) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);

        return templateManager.processTemplate("cors-config.java.ftl", data);
    }

    /**
     * 生成pom.xml配置文件
     *
     * @param groupId     组织ID
     * @param artifactId  项目ID
     * @param name        项目名称
     * @param description 项目描述
     * @return pom.xml配置文件代码
     * @throws CodeGenException 代码生成异常
     */
    public String generatePomXml(String groupId, String artifactId, String name, String description) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("groupId", groupId);
        data.put("artifactId", artifactId);
        data.put("name", name);
        data.put("description", description);

        return templateManager.processTemplate("pom.xml.ftl", data);
    }
}