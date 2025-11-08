package com.metadata.util;

import java.util.regex.Pattern;

/**
 * 编码校验工具（防止SQL注入）
 */
public class CodeValidator {
    // 编码格式：字母、数字、下划线，长度1-50
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,50}$");

    /**
     * 校验编码合法性
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        return CODE_PATTERN.matcher(code).matches();
    }

    /**
     * 校验多个编码
     */
    public static boolean isValidCodes(String... codes) {
        for (String code : codes) {
            if (!isValidCode(code)) {
                return false;
            }
        }
        return true;
    }
}

