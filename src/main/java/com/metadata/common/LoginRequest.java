package com.metadata.common;

import lombok.Data;

/**
 * 登录请求类
 */
@Data
public class LoginRequest {
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
}
