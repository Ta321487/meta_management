package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 管理员实体
 */
@Data
public class MetadataAdmin {
    private Integer id;
    private String username;
    private String password;
    private LocalDateTime lastLoginTime;
}

