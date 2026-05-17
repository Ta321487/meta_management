package com.metadata.util;

import java.util.regex.Pattern;

/**
 * 编码/技术标识校验与规范化（防止 SQL 注入；录入时统一处理空白）
 */
public class CodeValidator {

    /** 元数据编码：字母、数字、下划线，长度 1–50 */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_]{1,50}$");

    /** 模块类型编码：大写字母开头 */
    private static final Pattern MODULE_TYPE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u3000]+");

    private CodeValidator() {
    }

    /**
     * 规范化技术标识：trim、空白转下划线、合并连续下划线、去掉首尾下划线
     */
    public static String normalizeIdentifier(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return "";
        }
        s = WHITESPACE.matcher(s).replaceAll("_");
        s = s.replaceAll("_+", "_");
        s = s.replaceAll("^_+|_+$", "");
        return s;
    }

    /**
     * 元数据编码（表/字段/模块等）：规范化后转大写
     */
    public static String normalizeCode(String raw) {
        return normalizeIdentifier(raw).toUpperCase();
    }

    /**
     * 模块类型编码：大写且以字母开头
     */
    public static String normalizeModuleTypeCode(String raw) {
        String s = normalizeCode(raw);
        if (s.isEmpty()) {
            return "";
        }
        if (!Character.isLetter(s.charAt(0))) {
            s = "T_" + s;
        }
        return s;
    }

    /**
     * 校验编码合法性（调用前建议先 normalizeCode）
     */
    public static boolean isValidCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return CODE_PATTERN.matcher(code).matches();
    }

    /**
     * 物理列名等与编码规则相同
     */
    public static boolean isValidIdentifier(String identifier) {
        return isValidCode(identifier);
    }

    public static boolean isValidModuleTypeCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        return MODULE_TYPE_CODE_PATTERN.matcher(code).matches();
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
