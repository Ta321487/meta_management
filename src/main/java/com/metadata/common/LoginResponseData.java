package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应 data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录成功返回的用户信息")
public class LoginResponseData {

    /**
     * 当前登录用户名
     */
    @Schema(description = "用户名", example = "admin")
    private String username;
}
