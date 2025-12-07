package com.metadata.service;

import com.alibaba.fastjson2.JSONArray;
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
        
        // 提取字段名
        String fieldName = extractFieldNameFromConstraint(constraint);
        logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "提取到字段名: " + fieldName);
        
        // 1. 正则表达式约束：支持 regexp_like 语法，如 regexp_like(`name`,_utf8mb4'^[A-Za-z]+$')
        Pattern regexPattern = Pattern.compile(
            "regexp_like\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*,\\s*(?:_utf8mb4)?'([^']+)'\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regexMatcher = regexPattern.matcher(constraint);
        if (regexMatcher.find()) {
            String pattern = regexMatcher.group(2);
            if (pattern != null) {
                // 生成正则约束JSON
                String json = String.format("{\"pattern\":\"%s\",\"message\":\"\"}", 
                    pattern.replace("\\", "\\\\"));
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为正则约束: " + json);
                return json;
            }
        }
        
        // 2. 支持直接使用 REGEXP 关键字的格式，如 STU_CODE REGEXP '^\\d{10}$'
        Pattern regExpKeywordPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+REGEXP\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        Matcher regExpKeywordMatcher = regExpKeywordPattern.matcher(constraint);
        if (regExpKeywordMatcher.find()) {
            String pattern = regExpKeywordMatcher.group(2);
            if (pattern != null) {
                // 生成正则约束JSON
                String json = String.format("{\"pattern\":\"%s\",\"message\":\"\"}", 
                    pattern.replace("\\", "\\\\"));
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为REGEXP关键字正则约束: " + json);
                return json;
            }
        }
        
        // 3. BETWEEN AND约束：支持 ((`age` between 18 and 60)) 格式，匹配小写的between
        Pattern betweenPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+between\\s+([0-9]+)\\s+and\\s+([0-9]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher betweenMatcher = betweenPattern.matcher(constraint);
        if (betweenMatcher.find()) {
            String min = betweenMatcher.group(2);
            String max = betweenMatcher.group(3);
            // 生成BETWEEN约束JSON
            String json = String.format("{\"min\":\"%s\",\"max\":\"%s\",\"message\":\"\"}",
                min, max);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为BETWEEN约束: " + json);
            return json;
        }
        
        // 4. IN约束：支持 ((`status` in (_gbk'active',_gbk'inactive',_gbk'pending'))) 格式，匹配小写的in
        Pattern inPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+in\\s*\\(([^\\)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher inMatcher = inPattern.matcher(constraint);
        if (inMatcher.find()) {
            String valuesStr = inMatcher.group(2);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "提取IN值列表: " + valuesStr);
            // 解析IN值列表，处理字符集前缀如_gbk'active'
            List<String> valuesList = parseInValues(valuesStr);
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析后的值列表: " + valuesList);
            if (!valuesList.isEmpty()) {
                // 生成IN约束JSON，只包含values字段
                StringBuilder valuesJson = new StringBuilder();
                for (int i = 0; i < valuesList.size(); i++) {
                    if (i > 0) {
                        valuesJson.append(",");
                    }
                    valuesJson.append("\"").append(valuesList.get(i)).append("\"");
                }
                String json = String.format("{\"operator\":\"IN\",\"values\":[%s]}",
                    valuesJson.toString());
                logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为IN约束: " + json);
                return json;
            }
        }
        
        // 5. 数值范围约束：支持 >、<、>=、<=，如 ((`age` >= 18)) 或 ((`salary` < 10000))
        Pattern rangePattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s*([><]=?|<=?|>=?)\\s*([0-9]+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher rangeMatcher = rangePattern.matcher(constraint);
        if (rangeMatcher.find()) {
            String operator = rangeMatcher.group(2);
            String value = rangeMatcher.group(3);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("message", "");
            
            int intValue = Integer.parseInt(value);
            
            // 处理各种运算符
            switch (operator) {
                case ">":
                case ">=" :
                    jsonObj.put("min", intValue);
                    break;
                case "<":
                case "<=" :
                    jsonObj.put("max", intValue);
                    break;
                default:
                    // 未知运算符，记录日志
                    logService.logError("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "未知的数值范围运算符", operator);
                    return null;
            }
            
            String json = jsonObj.toJSONString();
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为数值范围约束: " + json);
            return json;
        }
        
        // 6. 等于/不等于约束：支持 =、!=，如 ((`status` = 'active')) 或 ((`type` != 'admin'))
        Pattern equalPattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s*(!?=)\\s*'?([^']+)'?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher equalMatcher = equalPattern.matcher(constraint);
        if (equalMatcher.find()) {
            String operator = equalMatcher.group(2);
            String value = equalMatcher.group(3);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("message", "");
            
            if ("=".equals(operator)) {
                jsonObj.put("operator", "=");
                jsonObj.put("value", value);
            } else if ("!=".equals(operator)) {
                jsonObj.put("operator", "!");
                jsonObj.put("value", value);
            }
            
            String json = jsonObj.toJSONString();
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为等于/不等于约束: " + json);
            return json;
        }
        
        // 7. LIKE/RLIKE约束：支持 LIKE 'pattern' 或 RLIKE 'pattern'
        Pattern likePattern = Pattern.compile(
            "`?([a-zA-Z0-9_]+)`?\\s+(LIKE|RLIKE)\\s+'([^']+)'",
            Pattern.CASE_INSENSITIVE
        );
        Matcher likeMatcher = likePattern.matcher(constraint);
        if (likeMatcher.find()) {
            String likeOperator = likeMatcher.group(2);
            String pattern = likeMatcher.group(3);
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("message", "");
            
            if ("RLIKE".equalsIgnoreCase(likeOperator)) {
                // RLIKE等价于正则表达式
                jsonObj.put("pattern", pattern);
            } else if ("LIKE".equalsIgnoreCase(likeOperator)) {
                // LIKE转换为正则表达式
                String regexPatternStr = pattern.replace("%", ".*");
                jsonObj.put("pattern", regexPatternStr);
            }
            
            String json = jsonObj.toJSONString();
            logService.logSuccess("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "解析为LIKE/RLIKE约束: " + json);
            return json;
        }
        
        logService.logError("admin", SqlConstants.LOG_MODULE_PARSE_CHECK_CONSTRAINT, "无法解析CHECK约束", constraint);
        return null;
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
        
        // 2. 尝试匹配带括号的字段格式：((`field` between 18 and 60)) 或 ((`field` in (value1, value2)))
        Pattern parenthesisFieldPattern = Pattern.compile(
            "\\(\\s*\\(\\s*`?([a-zA-Z0-9_]+)`?\\s*",
            Pattern.CASE_INSENSITIVE
        );
        Matcher parenthesisFieldMatcher = parenthesisFieldPattern.matcher(trimmedConstraint);
        if (parenthesisFieldMatcher.find()) {
            return parenthesisFieldMatcher.group(1);
        }
        
        // 3. 尝试匹配简单字段格式：`field` between 18 and 60 或 `field` in (value1, value2)
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
     * @return 移除前缀后的值
     */
    private String removeCharsetPrefix(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        // 首先处理带引号的情况：_gbk'active' -> active
        if (value.contains("'") || value.contains("\"")) {
            Pattern valuePattern = Pattern.compile("(?:_\\w+)?(['\\\"'])(.*?)\\1");
            Matcher valueMatcher = valuePattern.matcher(value);
            if (valueMatcher.find()) {
                return valueMatcher.group(2);
            }
        }
        
        // 处理不带引号的情况：_utf8mb4文学 -> 文学
        Pattern charsetPrefixPattern = Pattern.compile("^_\\w+");
        Matcher charsetPrefixMatcher = charsetPrefixPattern.matcher(value);
        if (charsetPrefixMatcher.find()) {
            return charsetPrefixMatcher.replaceFirst("");
        }
        
        return value;
    }
}