package com.metadata.controller;

import com.metadata.common.ChangePasswordRequest;
import com.metadata.common.LoginRequest;
import com.metadata.common.Result;
import com.metadata.common.codes.ApiMessages;
import com.metadata.common.codes.AppErrorCodes;
import com.metadata.exception.BizException;
import com.metadata.entity.MetadataAdmin;
import com.metadata.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证相关API")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录认证，返回用户信息")
    public Result<Map<String, Object>> login(
            @Parameter(description = "登录参数，包含username和password",required = true) @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        MetadataAdmin admin = authService.login(username, password);
        if (admin == null) {
            throw BizException.of(AppErrorCodes.AUTH_LOGIN_FAILED, ApiMessages.LOGIN_FAILED);
        }
        HttpSession session = request.getSession();
        session.setAttribute("admin", admin);
        Map<String, Object> data = new HashMap<>();
        data.put("username", admin.getUsername());
        return Result.success(data);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "用户退出", description = "用户退出登录，清空会话信息")
    public Result<?> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    @Operation(summary = "修改密码", description = "用户修改密码，需要验证原密码")
    public Result<?> changePassword(
            @Parameter(description = "密码参数，包含oldPassword和newPassword") @RequestBody ChangePasswordRequest changePasswordRequest,
            HttpServletRequest request) {
        HttpSession session = request.getSession();
        MetadataAdmin admin = (MetadataAdmin) session.getAttribute("admin");
        if (admin == null) {
            throw BizException.unauthorized(ApiMessages.NOT_LOGGED_IN);
        }
        String oldPassword = changePasswordRequest.getOldPassword();
        String newPassword = changePasswordRequest.getNewPassword();
        boolean success = authService.changePassword(admin.getUsername(), oldPassword, newPassword);
        if (success) {
            return Result.success();
        }
        throw BizException.of(AppErrorCodes.AUTH_PASSWORD_INCORRECT, ApiMessages.WRONG_OLD_PASSWORD);
    }
}

