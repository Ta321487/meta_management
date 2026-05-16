package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单表完整代码包响应类
 * <p>「生成所有代码」接口 data 的固定结构，各字段为对应文件的源码文本。</p>
 */
@Data
@Schema(description = "单表完整代码包")
public class TableGeneratedCodeBundle {

    /**
     * 建表 SQL（create_table.sql）
     */
    @Schema(description = "建表 SQL")
    private String createTableSql;

    /**
     * 实体类（Entity.java）
     */
    @Schema(description = "实体类源码")
    private String entityJava;

    /**
     * 控制器（Controller.java）
     */
    @Schema(description = "Controller 源码")
    private String controllerJava;

    /**
     * 服务层（Service.java；是否接口由生成参数 useInterface 决定）
     */
    @Schema(description = "Service 源码")
    private String serviceJava;

    /**
     * 服务接口（ServiceInterface.java）
     */
    @Schema(description = "Service 接口源码")
    private String serviceInterfaceJava;

    /**
     * 服务实现类（ServiceImpl.java）
     */
    @Schema(description = "Service 实现类源码")
    private String serviceImplJava;

    /**
     * Mapper 接口（Mapper.java）
     */
    @Schema(description = "Mapper 接口源码")
    private String mapperJava;

    /**
     * MyBatis 映射文件（Mapper.xml）
     */
    @Schema(description = "Mapper XML")
    private String mapperXml;

    /**
     * 统一响应类（Result.java）
     */
    @Schema(description = "Result 类源码")
    private String resultJava;

    /**
     * 分页请求类（PageRequest.java）
     */
    @Schema(description = "PageRequest 类源码")
    private String pageRequestJava;

    /**
     * 分页结果类（PageResult.java）
     */
    @Schema(description = "PageResult 类源码")
    private String pageResultJava;

    /**
     * 列表页组件（List.vue）
     */
    @Schema(description = "列表页 Vue")
    private String listVue;

    /**
     * 表单页组件（Form.vue）
     */
    @Schema(description = "表单页 Vue")
    private String formVue;

    /**
     * 登录页组件（Login.vue）
     */
    @Schema(description = "登录页 Vue")
    private String loginVue;

    /**
     * 认证 API（auth.js）；开启验证码时生成
     */
    @Schema(description = "认证 API（auth.js）")
    private String authJs;

    /**
     * 验证码输入组件（CaptchaInput.vue）
     */
    @Schema(description = "验证码输入组件")
    private String captchaInputVue;

    /**
     * 认证控制器（AuthController.java）
     */
    @Schema(description = "认证 Controller")
    private String authControllerJava;

    /**
     * 验证码服务（CaptchaService.java）
     */
    @Schema(description = "验证码服务")
    private String captchaServiceJava;

    /**
     * 认证服务接口（AuthService.java）
     */
    @Schema(description = "认证 Service 接口")
    private String authServiceJava;

    /**
     * 认证服务实现（AuthServiceImpl.java）
     */
    @Schema(description = "认证 Service 实现")
    private String authServiceImplJava;

    /**
     * 登录请求 DTO（LoginRequest.java）
     */
    @Schema(description = "登录请求 DTO")
    private String loginRequestJava;

    /**
     * 登录拦截器（LoginInterceptor.java）
     */
    @Schema(description = "登录拦截器")
    private String loginInterceptorJava;

    /**
     * 拦截器配置（InterceptorConfig.java）
     */
    @Schema(description = "拦截器配置")
    private String interceptorConfigJava;

    /**
     * 前端路由（routes.js）
     */
    @Schema(description = "前端路由 routes.js")
    private String routesJs;

    /**
     * 前端接口封装（api.js）
     */
    @Schema(description = "前端 API api.js")
    private String apiJs;

    /**
     * 前端请求工具（request.js）
     */
    @Schema(description = "前端 request.js")
    private String requestJs;

    /**
     * 前端环境变量（.env）
     */
    @Schema(description = "前端 .env")
    private String envFile;

    /**
     * Spring Boot 启动类（Application.java）
     */
    @Schema(description = "启动类 Application.java")
    private String applicationJava;

    /**
     * 应用配置（application.yml）
     */
    @Schema(description = "application.yml")
    private String applicationYml;

    /**
     * MyBatis 配置类（MyBatisConfig.java）
     */
    @Schema(description = "MyBatis 配置类")
    private String myBatisConfigJava;

    /**
     * 跨域配置类（CorsConfig.java）
     */
    @Schema(description = "CORS 配置类")
    private String corsConfigJava;

    /**
     * Maven 工程描述（pom.xml）
     */
    @Schema(description = "pom.xml")
    private String pomXml;

    /**
     * 开箱说明（README.md，Markdown）
     */
    @Schema(description = "开箱说明 README.md")
    private String readmeMd;

    /**
     * 前端环境变量示例（.env.example）
     */
    @Schema(description = "前端 .env.example")
    private String envExample;
}
