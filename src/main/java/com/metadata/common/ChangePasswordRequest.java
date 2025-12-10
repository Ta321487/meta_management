package com.metadata.common;

import lombok.Data;

/**
 * 修改密码请求类
 */
@Data
public class ChangePasswordRequest {
    /**
     * 原密码
     */
    private String oldPassword;
    /**
     * 新密码
     */
    private String newPassword;
}