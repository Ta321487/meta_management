package ${packageName}.service.impl;

import ${packageName}.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于配置文件的简易认证（生成子系统默认实现，可按需替换为数据库用户表）
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${r"${app.auth.default-username:admin}"}")
    private String defaultUsername;

    @Value("${r"${app.auth.default-password:admin123}"}")
    private String defaultPassword;

    @Override
    public Map<String, Object> login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        if (defaultUsername.equals(username.trim()) && defaultPassword.equals(password)) {
            Map<String, Object> user = new HashMap<>();
            user.put("username", username.trim());
            return user;
        }
        return null;
    }
}
