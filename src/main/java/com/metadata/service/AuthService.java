package com.metadata.service;

import com.metadata.entity.MetadataAdmin;
import com.metadata.mapper.MetadataAdminMapper;
import com.metadata.util.BCryptUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 */
@Service
public class AuthService {

    @Autowired
    private MetadataAdminMapper adminMapper;

    @Autowired
    private OperationLogService logService;

    /**
     * 登录验证
     */
    public MetadataAdmin login(String username, String password) {
        MetadataAdmin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            logService.logError(username, "LOGIN", "登录失败：用户不存在", "用户不存在");
            return null;
        }
        if (!BCryptUtil.matches(password, admin.getPassword())) {
            logService.logError(username, "LOGIN", "登录失败：密码错误", "密码错误");
            return null;
        }
        // 更新最后登录时间
        adminMapper.updateLastLoginTime(username);
        logService.logSuccess(username, "LOGIN", "登录成功");
        return admin;
    }

    /**
     * 修改密码
     */
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        MetadataAdmin admin = adminMapper.selectByUsername(username);
        if (admin == null || !BCryptUtil.matches(oldPassword, admin.getPassword())) {
            logService.logError(username, "CHANGE_PASSWORD", "修改密码失败：原密码错误", "原密码错误");
            return false;
        }
        String encodedPassword = BCryptUtil.encode(newPassword);
        adminMapper.updatePassword(username, encodedPassword);
        logService.logSuccess(username, "CHANGE_PASSWORD", "修改密码成功");
        return true;
    }
}

