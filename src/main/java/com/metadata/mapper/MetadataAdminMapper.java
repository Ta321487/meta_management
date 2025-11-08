package com.metadata.mapper;

import com.metadata.entity.MetadataAdmin;
import org.apache.ibatis.annotations.Param;

/**
 * 管理员Mapper
 */
public interface MetadataAdminMapper {
    MetadataAdmin selectByUsername(String username);
    int updatePassword(@Param("username") String username, @Param("password") String password);
    int updateLastLoginTime(@Param("username") String username);
}

