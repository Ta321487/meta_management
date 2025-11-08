package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.entity.MetadataAdmin;
import com.metadata.service.AuthService;
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
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String username = params.get("username");
        String password = params.get("password");
        MetadataAdmin admin = authService.login(username, password);
        if (admin == null) {
            return Result.error("用户名或密码错误");
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
    public Result<?> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PostMapping("/changePassword")
    public Result<?> changePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        HttpSession session = request.getSession();
        MetadataAdmin admin = (MetadataAdmin) session.getAttribute("admin");
        if (admin == null) {
            return Result.error(401, "未登录");
        }
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        boolean success = authService.changePassword(admin.getUsername(), oldPassword, newPassword);
        if (success) {
            return Result.success();
        } else {
            return Result.error("原密码错误");
        }
    }
}

