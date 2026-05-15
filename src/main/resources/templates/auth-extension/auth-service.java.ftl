package ${packageName}.service;

import java.util.Map;

/**
 * 认证服务
 */
public interface AuthService {

    /**
     * 校验用户名密码，成功返回用户信息
     */
    Map<String, Object> login(String username, String password);
}
