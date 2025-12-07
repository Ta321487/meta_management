package com.metadata.service;

import com.metadata.entity.MetadataAdmin;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 登录验证
     */
    MetadataAdmin login(String username, String password);

    /**
     * 修改密码
     */
    boolean changePassword(String username, String oldPassword, String newPassword);
}