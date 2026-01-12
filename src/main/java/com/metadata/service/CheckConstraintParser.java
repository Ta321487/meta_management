package com.metadata.service;

import com.alibaba.fastjson2.JSONObject;
import com.metadata.service.constant.SqlConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CHECK约束解析器
 * 负责解析数据库中的CHECK约束，并转换为元数据系统的校验规则
 */
@Component
public class CheckConstraintParser {
    
    @Autowired
    private OperationLogService logService;
    
    /**
     * 解析CREATE TABLE语句，提取所有CHECK约束
     * @param createTableSql CREATE TABLE语句
     * @return 字段名到CHECK约束的映射
     */
    public Map<String, String> extractCheckConstraints(String createTableSql) {
        Map<String, String> checkConstraints = new HashMap<>();
        
        // 解析CREATE TABLE语句，提取CHECK约束
        // 匹配列级CHECK约束：column_name type CHECK (constraint)
        Pattern columnCheckPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+[^,]+\\s+CHECK\\s*\\(([^\\)]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher columnCheckMatcher = columnCheckPattern.matcher(createTableSql);
        int columnCheckCount = 0;
        while (columnCheckMatcher.find()) {
            String columnName = columnCheckMatcher.group(1);
            String checkClause = columnCheckMatcher.group(2);
            String fullConstraint = "CHECK (" + checkClause + ")";
            checkConstraints.put(columnName.toUpperCase(), fullConstraint);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "提取列级CHECK约束: " + columnName + " -> " + fullConstraint);
            columnCheckCount++;
        }
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "提取到" + columnCheckCount + "个列级CHECK约束");
        
        // 匹配表级CHECK约束：支持两种格式
        // 格式1：CHECK (constraint)
        // 格式2：CONSTRAINT constraint_name CHECK (...)
        // 使用更健壮的正则表达式处理嵌套括号
        String createTableStr = createTableSql;
        int checkStartIndex = createTableStr.indexOf("CHECK");
        int checkCount = 0;
        
        while (checkStartIndex != -1) {
            // 找到CHECK关键字后的第一个左括号
            int openParenIndex = createTableStr.indexOf("(", checkStartIndex);
            if (openParenIndex == -1) {
                break;
            }
            
            // 计算匹配的右括号位置
            int closeParenIndex = findMatchingCloseParen(createTableStr, openParenIndex);
            if (closeParenIndex == -1) {
                break;
            }
            
            // 提取完整的CHECK约束
            String fullCheckConstraint = createTableStr.substring(checkStartIndex, closeParenIndex + 1);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "提取到完整CHECK约束: " + fullCheckConstraint);
            
            // 提取CHECK约束子句（括号内的内容）
            String checkClause = createTableStr.substring(openParenIndex + 1, closeParenIndex);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "提取到CHECK约束子句: " + checkClause);
            
            // 从约束条件中提取字段名
            String columnName = extractFieldNameFromConstraint(checkClause);
            
            if (columnName != null && !columnName.isEmpty()) {
                checkConstraints.put(columnName.toUpperCase(), fullCheckConstraint);
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "提取表级CHECK约束: " + columnName + " -> " + fullCheckConstraint);
                checkCount++;
            } else {
                logService.logError("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "无法从表级约束中提取字段名", checkClause);
            }
            
            // 继续查找下一个CHECK约束
            checkStartIndex = createTableStr.indexOf("CHECK", closeParenIndex);
        }
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_GET_CHECK_CONSTRAINTS, "提取到" + checkCount + "个表级CHECK约束");
        
        return checkConstraints;
    }
    
    /**
     * 解析CHECK约束字符串，转换为JSON格式的校验规则
     * @param checkConstraint CHECK约束字符串
     * @return JSON格式的校验规则
     */
    public String parseCheckConstraint(String checkConstraint) {
        if (checkConstraint == null || checkConstraint.trim().isEmpty()) {
            logService.logError("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "CHECK约束为空", "");
            return null;
        }
        
        String constraint = checkConstraint.trim();
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "开始解析CHECK约束: " + constraint);
        
        JSONObject validationRules = new JSONObject();
        
        // 1. 正则表达式约束：支持 regexp_like 语法
        Pattern regexPattern = Pattern.compile(
                "regexp_like\\s*\\(\\s*`?[a-zA-Z0-9_]+`?\\s*,\\s*(?:_utf8mb4)?'([^']+)'\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regexMatcher = regexPattern.matcher(constraint);
        if (regexMatcher.find()) {
            String pattern = regexMatcher.group(1);
            if (pattern != null) {
                pattern = fixCommonLostBackslashes(pattern);
                validationRules.put("pattern", pattern.replace("\\", "\\\\"));
            }
        }
        
        // 2. 支持直接使用 REGEXP 关键字的格式
        Pattern regExpKeywordPattern = Pattern.compile(
                "`?[a-zA-Z0-9_]+`?\\s+REGEXP\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regExpKeywordMatcher = regExpKeywordPattern.matcher(constraint);
        if (regExpKeywordMatcher.find()) {
            String pattern = regExpKeywordMatcher.group(1);
            if (pattern != null) {
                pattern = fixCommonLostBackslashes(pattern);
                validationRules.put("pattern", pattern.replace("\\", "\\\\"));
            }
        }
        
        // 3. BETWEEN AND约束
        Pattern betweenPattern = Pattern.compile(
                "between\\s+([0-9]+)\\s+and\\s+([0-9]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher betweenMatcher = betweenPattern.matcher(constraint);
        if (betweenMatcher.find()) {
            validationRules.put("min", Integer.parseInt(betweenMatcher.group(1)));
            validationRules.put("max", Integer.parseInt(betweenMatcher.group(2)));
        }
        
        // 4. IN约束
        Pattern inPattern = Pattern.compile(
                "in\\s*\\(([^\\)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher inMatcher = inPattern.matcher(constraint);
        if (inMatcher.find()) {
            String valuesStr = inMatcher.group(1);
            List<String> valuesList = parseInValues(valuesStr);
            if (!valuesList.isEmpty()) {
                List<String> filteredValues = new ArrayList<>();
                for (String value : valuesList) {
                    if (value != null && !value.trim().isEmpty()) {
                        filteredValues.add(value.trim());
                        }
                    }
                if (!filteredValues.isEmpty()) {
                    validationRules.put("operator", "IN");
                    validationRules.put("values", filteredValues);
                }
            }
        }
        
        // 5. 数值范围约束：>、<、>=、<=
        Pattern rangePattern = Pattern.compile(
                "`?[a-zA-Z0-9_]+`?\\s*([><]=?|<=?|>=?)\\s*([0-9]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher rangeMatcher = rangePattern.matcher(constraint);
        while (rangeMatcher.find()) {
            String operator = rangeMatcher.group(1);
            int value = Integer.parseInt(rangeMatcher.group(2));
            switch (operator) {
                case ">":
                case ">=":
                    validationRules.put("min", value);
                    break;
                case "<":
                case "<=":
                    validationRules.put("max", value);
                    break;
            }
        }
        
        // 6. 等于/不等于约束
        Pattern equalPattern = Pattern.compile(
                "`?[a-zA-Z0-9_]+`?\\s*(!?=)\\s*'?([^']+)'?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher equalMatcher = equalPattern.matcher(constraint);
        if (equalMatcher.find()) {
            String operator = equalMatcher.group(1);
            String value = equalMatcher.group(2);
            if ("=".equals(operator)) {
                validationRules.put("operator", "=");
                validationRules.put("value", value);
            } else if ("!=".equals(operator)) {
                validationRules.put("operator", "!");
                validationRules.put("value", value);
            }
        }
        
        // 7. LIKE/RLIKE约束
        Pattern likePattern = Pattern.compile(
                "`?[a-zA-Z0-9_]+`?\\s+(LIKE|RLIKE)\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        Matcher likeMatcher = likePattern.matcher(constraint);
        if (likeMatcher.find()) {
            String likeOperator = likeMatcher.group(2);
            String pattern = likeMatcher.group(3);
            if ("RLIKE".equalsIgnoreCase(likeOperator)) {
                validationRules.put("pattern", pattern);
            } else if ("LIKE".equalsIgnoreCase(likeOperator)) {
                validationRules.put("pattern", pattern.replace("%", ".*"));
            }
        }
        
        // 8. 跨字段比较约束：匹配两个不同字段之间的比较，如 `field2` >= `field1`
        // 匹配模式：`field1` >= `field2` （其中field1和field2是不同的字段）
        Pattern crossFieldPattern = Pattern.compile(
                "`?([a-zA-Z0-9_]+)`?\\s*([><]=?|<=?|>=?|=|!=)\\s*`?([a-zA-Z0-9_]+)`?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher crossFieldMatcher = crossFieldPattern.matcher(constraint);
        if (crossFieldMatcher.find() && validationRules.isEmpty()) {
            String field1 = crossFieldMatcher.group(1); // 第一个字段（比较操作符左侧）
            String operator = crossFieldMatcher.group(2); // 操作符
            String field2 = crossFieldMatcher.group(3); // 第二个字段（比较操作符右侧）
            
            // 如果field1和field2不同，说明是跨字段比较
            if (field1 != null && field2 != null && !field1.equalsIgnoreCase(field2)) {
                validationRules.put("type", "crossField");
                validationRules.put("field1", field1);
                validationRules.put("operator", operator);
                validationRules.put("field2", field2);
                
                // 生成条件描述
                String conditionDesc = field1 + " " + operator + " " + field2;
                validationRules.put("condition", conditionDesc);
                
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, 
                    "解析跨字段比较约束: " + conditionDesc);
            }
        }
        
        if (validationRules.isEmpty()) {
        logService.logError("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "无法解析CHECK约束", constraint);
        return null;
        }

        // 确保message字段存在
        if (!validationRules.containsKey("message")) {
            validationRules.put("message", "");
        }

        String json = validationRules.toJSONString();
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为组合约束: " + json);
        return json;
    }
    
    /**
     * 从CHECK约束中提取字段名
     * @param constraint CHECK约束字符串
     * @return 字段名
     */
    public String extractFieldNameFromConstraint(String constraint) {
        if (constraint == null || constraint.trim().isEmpty()) {
            return "";
        }
        
        String trimmedConstraint = constraint.trim();
        
        // 1. 首先尝试匹配regexp_like函数格式：regexp_like(`field`, '_utf8mb4^[A-Za-z]+$')
        Pattern regexpLikePattern = Pattern.compile(
            "regexp_like\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*,",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regexpLikeMatcher = regexpLikePattern.matcher(trimmedConstraint);
        if (regexpLikeMatcher.find()) {
            return regexpLikeMatcher.group(1);
        }
        
        // 2. 尝试匹配跨字段比较格式：`field1` >= `field2`
        // 对于跨字段比较，返回第一个字段（比较操作符左侧的字段）作为主字段
        // 需要优先匹配跨字段比较，避免被带括号的字段格式规则误匹配
        // 使用更精确的正则表达式，确保匹配到真正的字段比较（避免匹配到 IS NULL 等）
        Pattern crossFieldPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s*([><]=?|<=?|>=?|=|!=)\\s*`?([a-zA-Z0-9_]+)`?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher crossFieldMatcher = crossFieldPattern.matcher(trimmedConstraint);
        // 查找所有匹配，选择field1和field2不同的匹配（真正的跨字段比较）
        while (crossFieldMatcher.find()) {
            String field1 = crossFieldMatcher.group(1);
            String field2 = crossFieldMatcher.group(3);
            
            // 如果field1和field2不同，说明是跨字段比较，返回field1作为主字段
            if (field1 != null && field2 != null && !field1.equalsIgnoreCase(field2)) {
                return field1;
            }
        }
        
        // 3. 尝试匹配带括号的字段格式：((`field` between 18 and 60)) 或 ((`field` in (value1, value2)))
        Pattern parenthesisFieldPattern = Pattern.compile(
            "\\(\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*",
            Pattern.CASE_INSENSITIVE
        );
        Matcher parenthesisFieldMatcher = parenthesisFieldPattern.matcher(trimmedConstraint);
        if (parenthesisFieldMatcher.find()) {
            return parenthesisFieldMatcher.group(1);
        }
        
        // 4. 尝试匹配简单字段格式：`field` between 18 and 60 或 `field` in (value1, value2)
        Pattern simpleFieldPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+(?:between|in|REGEXP|IN|BETWEEN|=|>|<|>=|<=|!=|LIKE|RLIKE)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher simpleFieldMatcher = simpleFieldPattern.matcher(trimmedConstraint);
        if (simpleFieldMatcher.find()) {
            return simpleFieldMatcher.group(1);
        }
        
        logService.logError("admin", SqlConstants.LOG_MODULE_EXTRACT_FIELD_NAME, "无法从约束中提取字段名", constraint);
        return "";
    }
    
    /**
     * 修复从 SHOW CREATE TABLE / CHECK 约束反向解析时，常见的反斜杠丢失问题。
     *
     * 例如内置 number 正则应为：^-?\\d+(\\.\\d+)?$
     * 在 SHOW CREATE TABLE 中可能会变成：^-?d+(.d+)?$
     *
     * 为避免误伤其它复杂正则，这里尽量做“保守修复”：
     * - 仅当 pattern 完全不含反斜杠时，才尝试补全 \\d/\\w/\\s/\\. 等常见转义。
     */
    private String fixCommonLostBackslashes(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return pattern;
        }

        // 兼容：最典型的数字/小数正则（你日志中的 age 就是这个）
        if ("^-?d+(.d+)?$".equals(pattern)) {
            return "^-?\\d+(\\.\\d+)?$";
        }

        // 仅在不包含反斜杠时才尝试修复，避免对本来就正确的表达式重复加工
        if (!pattern.contains("\\")) {
            // 1) 修复 d{m,n} / d{n} -> \\d{...}
            pattern = pattern.replaceAll("(?<!\\\\)d\\{", "\\\\d{");

            // 2) 修复 (19|20)d{2} 这种：紧跟在 ) 后的 d{ -> )\\d{
            pattern = pattern.replaceAll("\\)d\\{", ")\\\\d{");

            // 3) 修复孤立的 d+ / d* / d?（避免把普通单词里的 d 误伤，限定前面是边界或非字母数字下划线）
            pattern = pattern.replaceAll("(^|[^A-Za-z0-9_])d([+*?])", "$1\\\\d$2");

            // 4) 修复典型 IP/URL 片段里的点：d{1,3}. -> \\d{1,3}\\\\.
            // 只针对 "d{...}." 这种明确的形式
            pattern = pattern.replaceAll("d(\\{[0-9,]+\\}).", "\\\\d$1\\\\.");

            // 5) 字符类 [] 内：补全 w/s/W/S（例如 [/w .-] -> [\\w .-]）
            Pattern cls = Pattern.compile("\\[([\\w\\s\\W\\S]*)\\]");
            Matcher cm = cls.matcher(pattern);
            StringBuffer sb = new StringBuffer();
            while (cm.find()) {
                String inner = cm.group(1);
                inner = inner.replaceAll("(^|[^A-Za-z0-9_])w", "$1\\\\w");
                inner = inner.replaceAll("(^|[^A-Za-z0-9_])s", "$1\\\\s");
                inner = inner.replaceAll("(^|[^A-Za-z0-9_])W", "$1\\\\W");
                inner = inner.replaceAll("(^|[^A-Za-z0-9_])S", "$1\\\\S");
                cm.appendReplacement(sb, Matcher.quoteReplacement("[" + inner + "]"));
            }
            cm.appendTail(sb);
            pattern = sb.toString();

            // 6) 非字符类场景下的 \\w/\\s：仅在明显是“转义语义”的位置修复
            // 例如 ^w+$ -> ^\\w+$、(?:/w+) -> (?:/\\w+)
            pattern = pattern.replaceAll("(^|[^A-Za-z0-9_])w([+*?]|\\{|\\)|\\]|\\b|\\B|$)", "$1\\\\w$2");
            pattern = pattern.replaceAll("(^|[^A-Za-z0-9_])s([+*?]|\\{|\\)|\\]|\\b|\\B|$)", "$1\\\\s$2");

            // 7) 点号：修复最常见的小数点形态（\\d.\\d -> \\d\\.\\d）
            pattern = pattern.replaceAll("\\\\d.\\\\d", "\\\\d\\\\.\\\\d");
        }

        return pattern;
    }
    
    /**
     * 查找匹配的右括号位置
     * @param str 输入字符串
     * @param openParenIndex 左括号位置
     * @return 匹配的右括号位置，未找到返回-1
     */
    public int findMatchingCloseParen(String str, int openParenIndex) {
        if (openParenIndex < 0 || openParenIndex >= str.length() || str.charAt(openParenIndex) != '(') {
            return -1;
        }
        
        int parenCount = 1;
        for (int i = openParenIndex + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
                if (parenCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 解析IN约束中的值列表
     * @param valuesStr 值列表字符串
     * @return 解析后的值列表
     */
    public List<String> parseInValues(String valuesStr) {
        List<String> values = new ArrayList<>();
        
        // 去除前后空格
        valuesStr = valuesStr.trim();
        if (valuesStr.isEmpty()) {
            return values;
        }
        
        // 使用状态机解析值列表，处理引号和逗号
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;
        char quoteChar = '\'';
        
        for (char c : valuesStr.toCharArray()) {
            if (c == '\'' || c == '"') {
                // 处理引号
                if (inQuotes) {
                    if (c == quoteChar) {
                        // 引号结束
                        inQuotes = false;
                    } else {
                        // 引号内的其他引号，作为普通字符处理
                        currentValue.append(c);
                    }
                } else {
                    // 引号开始
                    inQuotes = true;
                    quoteChar = c;
                }
            } else if (c == ',' && !inQuotes) {
                // 逗号分隔符，且不在引号内
                String value = currentValue.toString().trim();
                if (!value.isEmpty()) {
                    // 移除字符集前缀，如_utf8mb4文学 -> 文学
                    value = removeCharsetPrefix(value);
                    values.add(value);
                }
                currentValue.setLength(0);
            } else if (!Character.isWhitespace(c) || inQuotes) {
                // 普通字符，或引号内的空格
                currentValue.append(c);
            }
        }
        
        // 处理最后一个值
        String lastValue = currentValue.toString().trim();
        if (!lastValue.isEmpty()) {
            // 移除字符集前缀，如_utf8mb4文学 -> 文学
            lastValue = removeCharsetPrefix(lastValue);
            values.add(lastValue);
        }
        
        return values;
    }
    
    /**
     * 移除字符集前缀，如_utf8mb4文学 -> 文学，_gbk'active' -> active
     * @param value 原始值
     * @return 移除前缀后的值，如果值本身是字符集前缀（如_utf8mb4），返回空字符串
     */
    private String removeCharsetPrefix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String trimmedValue = value.trim();
        
        // 如果值本身就是字符集前缀（如_utf8mb4），返回空字符串
        if (trimmedValue.matches("^_[a-zA-Z0-9]+$")) {
            return "";
        }
        
        // 首先处理带引号的情况：_utf8mb4'1' -> 1, _gbk'active' -> active
        // 匹配格式：_字符集前缀'值' 或 '值'
        Pattern valuePattern = Pattern.compile("(?:_\\w+)?(['\"])(.*?)\\1");
        Matcher valueMatcher = valuePattern.matcher(trimmedValue);
        if (valueMatcher.find()) {
            String extractedValue = valueMatcher.group(2);
            // 如果提取的值不为空，返回它；否则继续处理不带引号的情况
            if (extractedValue != null && !extractedValue.isEmpty()) {
                return extractedValue;
            }
        }
        
        // 处理不带引号的情况：_utf8mb4文学 -> 文学
        // 匹配字符集前缀后跟内容的情况
        Pattern charsetPrefixPattern = Pattern.compile("^_([a-zA-Z0-9]+)(.*)$");
        Matcher charsetPrefixMatcher = charsetPrefixPattern.matcher(trimmedValue);
        if (charsetPrefixMatcher.find()) {
            String remainingValue = charsetPrefixMatcher.group(2);
            // 如果前缀后没有内容，返回空字符串
            if (remainingValue == null || remainingValue.trim().isEmpty()) {
                return "";
            }
            return remainingValue.trim();
        }
        
        return trimmedValue;
    }
}
