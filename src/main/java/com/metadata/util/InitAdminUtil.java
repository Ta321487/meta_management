package com.metadata.util;

/**
 * 初始化管理员工具类
 * 用于生成BCrypt加密后的密码
 */
public class InitAdminUtil {
    public static void main(String[] args) {
        // 验证现有的哈希值
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String password = "123456";
        
        System.out.println("=========================================");
        System.out.println("验证现有哈希值");
        System.out.println("=========================================");
        System.out.println("BCrypt哈希: " + existingHash);
        System.out.println("测试密码: " + password);
        boolean matches = BCryptUtil.matches(password, existingHash);
        System.out.println("验证结果: " + (matches ? "✓ 匹配" : "✗ 不匹配"));
        System.out.println("=========================================\n");
        
        // 生成新的密码123456的BCrypt加密值
        String encoded = BCryptUtil.encode(password);
        System.out.println("生成新的BCrypt加密值：");
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt加密后: " + encoded);
        System.out.println("\n请将以下SQL更新到数据库：");
        System.out.println("UPDATE metadata_admin SET password = '" + encoded + "' WHERE username = 'admin';");
    }
}

