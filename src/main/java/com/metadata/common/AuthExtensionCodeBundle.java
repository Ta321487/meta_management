package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 认证扩展包响应类
 * <p>验证码 + Session 登录相关后端源码，对应「生成认证扩展包」接口 data。</p>
 */
@Data
@Schema(description = "认证扩展包源码")
public class AuthExtensionCodeBundle {

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
     * 认证控制器（AuthController.java）
     */
    @Schema(description = "认证 Controller")
    private String authControllerJava;

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
}
