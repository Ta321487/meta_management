package com.metadata.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.service.MetadataBusinessSystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 代码生成工具类，包含公共方法和工具函数
 */
public class CodeGenUtils {

    private static final Logger log = LoggerFactory.getLogger(CodeGenUtils.class);

    /**
     * 内置正则表达式映射表，根据type值提供相应的正则表达式
     */
    private static final Map<String, String> BUILT_IN_REGEX_MAP = new HashMap<String, String>() {
        {
            put("email", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
            put("url", "^(https?:\\/\\/)?(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3})(?:[/\\w .-]*)*\\/?$");
            put("number", "^-?\\d+(\\.\\d+)?$");
            put("integer", "^-?\\d+$");
        }
    };

    /**
     * Java关键字集合，用于避免生成的名称与关键字冲突
     */
    private static final Set<String> JAVA_KEYWORDS = new HashSet<String>() {
        {
            add("abstract");
            add("assert");
            add("boolean");
            add("break");
            add("byte");
            add("case");
            add("catch");
            add("char");
            add("class");
            add("const");
            add("continue");
            add("default");
            add("do");
            add("double");
            add("else");
            add("enum");
            add("extends");
            add("final");
            add("finally");
            add("float");
            add("for");
            add("goto");
            add("if");
            add("implements");
            add("import");
            add("instanceof");
            add("int");
            add("interface");
            add("long");
            add("native");
            add("new");
            add("package");
            add("private");
            add("protected");
            add("public");
            add("return");
            add("short");
            add("static");
            add("strictfp");
            add("super");
            add("switch");
            add("synchronized");
            add("this");
            add("throw");
            add("throws");
            add("transient");
            add("try");
            add("void");
            add("volatile");
            add("while");
            add("true");
            add("false");
            add("null");
        }
    };

    /**
     * 获取业务系统名称
     *
     * @param businessCode          业务系统编码
     * @param businessSystemService 业务系统服务
     * @return 业务系统名称
     */
    public static String getBusinessName(String businessCode, MetadataBusinessSystemService businessSystemService) {
        if (businessCode == null || businessCode.trim().isEmpty()) {
            return "默认系统";
        }
        try {
            MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
            return businessSystem != null ? businessSystem.getBusinessName() : businessCode;
        } catch (Exception e) {
            return businessCode;
        }
    }

    /**
     * 主键字段在 Java/Vue 中使用的驼峰属性名（与 Entity 模板一致）；无 primary_key 标记时默认 id。
     */
    public static String getPrimaryKeyCamelCase(List<MetadataField> fields) {
        if (fields == null || fields.isEmpty()) {
            return "id";
        }
        for (MetadataField field : fields) {
            if (field.getIsEnabled() != null && field.getIsEnabled() == 0) {
                continue;
            }
            if (!"primary_key".equals(field.getFormComponent())) {
                continue;
            }
            String camelCaseName = convertToCamelCase(field.getFieldName(), false);
            if (JAVA_KEYWORDS.contains(camelCaseName)) {
                camelCaseName = "_" + camelCaseName;
            }
            return camelCaseName;
        }
        return "id";
    }

    /**
     * 是否参与「新增/编辑」表单布局（列表等仍可展示该列）。
     */
    public static boolean fieldParticipatesInForm(MetadataField f) {
        return f != null && (f.getInForm() == null || f.getInForm() != 0);
    }

    /**
     * 准备字段列表，添加转换后的属性
     *
     * @param fields 字段列表
     * @return 转换后的字段列表
     */
    public static List<Map<String, Object>> prepareFieldList(List<MetadataField> fields) {
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (MetadataField field : fields) {
            // 只处理启用的字段
            if (field.getIsEnabled() != null && field.getIsEnabled() == 0) {
                continue;
            }

            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("field", field);
            fieldMap.put("fieldName", field.getFieldName());
            // 格式化字段类型，确保生成有效的MySQL数据类型
            String formattedFieldType = formatFieldType(field.getFieldType());

            // 处理enum类型，从validateRule中获取enum值
            if ("enum('')".equals(formattedFieldType)) {
                Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
                if (validationRules.containsKey("hasOptions") && (Boolean) validationRules.get("hasOptions")) {
                    Object options = validationRules.get("options");
                    if (options instanceof JSONArray) {
                        JSONArray optionsArray = (JSONArray) options;
                        if (!optionsArray.isEmpty()) {
                            StringBuilder enumValues = new StringBuilder();
                            for (int i = 0; i < optionsArray.size(); i++) {
                                if (i > 0) {
                                    enumValues.append(", ");
                                }
                                Object option = optionsArray.get(i);
                                if (option instanceof String) {
                                    enumValues.append("'").append(option).append("'");
                                } else {
                                    enumValues.append(option);
                                }
                            }
                            formattedFieldType = "enum(" + enumValues.toString() + ")";
                        }
                    }
                }

                // 处理操作符为IN的情况，从values字段获取enum值
                if ("enum('')".equals(formattedFieldType) && validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
                    String operator = (String) validationRules.get("operator");
                    if ("IN".equalsIgnoreCase(operator)) {
                        Object values = validationRules.get("values");
                        if (values instanceof JSONArray) {
                            JSONArray valuesArray = (JSONArray) values;
                            if (!valuesArray.isEmpty()) {
                                StringBuilder enumValues = new StringBuilder();
                                for (int i = 0; i < valuesArray.size(); i++) {
                                    if (i > 0) {
                                        enumValues.append(", ");
                                    }
                                    Object value = valuesArray.get(i);
                                    if (value instanceof String) {
                                        enumValues.append("'").append(value).append("'");
                                    } else {
                                        enumValues.append(value);
                                    }
                                }
                                formattedFieldType = "enum(" + enumValues.toString() + ")";
                            }
                        }
                    }
                }
            }

            fieldMap.put("fieldType", formattedFieldType);
            fieldMap.put("label", field.getLabel());
            fieldMap.put("isRequired", field.getIsRequired());
            fieldMap.put("formComponent", field.getFormComponent());
            fieldMap.put("inForm", field.getInForm());
            fieldMap.put("javaType", getJavaType(field.getFieldType()));

            // 生成字段名并检查是否为关键字
            String camelCaseName = convertToCamelCase(field.getFieldName(), false);
            if (JAVA_KEYWORDS.contains(camelCaseName)) {
                camelCaseName = "_" + camelCaseName;
            }
            fieldMap.put("camelCaseName", camelCaseName);

            // 解析校验规则
            Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
            fieldMap.put("validationRules", validationRules);

            fieldList.add(fieldMap);
        }
        return fieldList;
    }

    /**
     * 格式化字段类型，确保生成有效的MySQL数据类型
     *
     * @param fieldType 字段类型
     * @return 格式化后的字段类型
     */
    public static String formatFieldType(String fieldType) {
        if (fieldType == null || fieldType.trim().isEmpty()) {
            return "varchar(255)";
        }

        String type = fieldType.trim().toLowerCase();

        // 处理常见的无效字段类型
        if (type.equals("var")) {
            return "varchar(255)";
        } else if (type.equals("varchar")) {
            return "varchar(255)";
        } else if (type.equals("char")) {
            return "char(1)";
        } else if (type.equals("int")) {
            return "int(11)";
        } else if (type.equals("bigint")) {
            return "bigint(20)";
        } else if (type.equals("tinyint")) {
            return "tinyint(4)";
        } else if (type.equals("smallint")) {
            return "smallint(6)";
        } else if (type.equals("mediumint")) {
            return "mediumint(9)";
        } else if (type.equals("float")) {
            return "float(10,2)";
        } else if (type.equals("double")) {
            return "double(16,2)";
        } else if (type.equals("decimal")) {
            return "decimal(18,2)";
        } else if (type.equals("date")) {
            return "date";
        } else if (type.equals("time")) {
            return "time";
        } else if (type.equals("datetime")) {
            return "datetime";
        } else if (type.equals("timestamp")) {
            return "timestamp";
        } else if (type.equals("text")) {
            return "text";
        } else if (type.equals("longtext")) {
            return "longtext";
        } else if (type.equals("mediumtext")) {
            return "mediumtext";
        } else if (type.equals("tinytext")) {
            return "tinytext";
        } else if (type.equals("blob")) {
            return "blob";
        } else if (type.equals("longblob")) {
            return "longblob";
        } else if (type.equals("mediumblob")) {
            return "mediumblob";
        } else if (type.equals("tinyblob")) {
            return "tinyblob";
        } else if (type.equals("enum")) {
            return "enum('')";
        } else if (type.equals("set")) {
            return "set('')";
        } else if (type.equals("boolean")) {
            return "tinyint(1)";
        }

        // 如果是已经包含括号的类型，直接返回
        if (type.contains("(")) {
            return fieldType;
        }

        // 默认返回varchar(255)
        return "varchar(255)";
    }

    /**
     * 将元数据字段 {@code validateRule} 解析为供 FreeMarker（Vue 等）使用的 {@code validationRules} Map。
     * <p><b>语义约定（与生成模板一致）</b>：
     * <ul>
     *   <li><b>字符串长度</b>：仅使用 {@code minLength} / {@code maxLength}（可选 {@code lengthMessage}）。</li>
     *   <li><b>数值上下界</b>：{@code min} / {@code max} 表示<strong>数值</strong>范围（可选 {@code rangeMessage}），与字符长度无关。</li>
     *   <li><b>正则</b>：显式 {@code pattern}；或 {@code type} 为内置别名 {@code email}/{@code url}/{@code number}/{@code integer}。
     *       当 {@code type} 为 {@code crossField} 时，不把 {@code type} 当作内置正则（跨字段规则单独解析）。</li>
     *   <li><b>JSON 数组</b>：支持 {@code [{...},{...}]}，按顺序合并为单个对象后再解析（同名键后者覆盖前者）。</li>
     * </ul>
     *
     * @param validateRule 校验规则 JSON 字符串，对象或对象数组
     * @return 供模板读取的标志位与参数；解析失败时返回全 false 的 Map，并打 WARN 日志
     */
    public static Map<String, Object> parseValidationRule(String validateRule) {
        Map<String, Object> rules = newValidationRuleDefaults();
        if (validateRule == null || validateRule.trim().isEmpty()) {
            return rules;
        }

        final JSONObject jsonObject;
        try {
            jsonObject = normalizeValidateRuleToJSONObject(validateRule.trim());
        } catch (Exception e) {
            log.warn("validateRule 无法解析为 JSON，已忽略: {}", abbreviateForLog(validateRule), e);
            return rules;
        }
        if (jsonObject == null || jsonObject.isEmpty()) {
            return rules;
        }

        try {
            fillPatternRules(jsonObject, rules);
            fillOptionsRules(jsonObject, rules);
            fillLengthRules(jsonObject, rules);
            fillNumericRangeRules(jsonObject, rules);
            fillOperatorRules(jsonObject, rules);
            fillCrossFieldRules(jsonObject, rules);
        } catch (Exception e) {
            log.warn("validateRule 解析后处理失败，已忽略: {}", abbreviateForLog(validateRule), e);
            return newValidationRuleDefaults();
        }

        return rules;
    }

    private static Map<String, Object> newValidationRuleDefaults() {
        Map<String, Object> rules = new HashMap<>();
        rules.put("hasPattern", false);
        rules.put("hasOptions", false);
        rules.put("hasLength", false);
        rules.put("hasRange", false);
        rules.put("hasOperator", false);
        rules.put("hasCrossField", false);
        return rules;
    }

    private static String abbreviateForLog(String s) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').replace('\r', ' ').trim();
        return t.length() <= 200 ? t : t.substring(0, 200) + "...";
    }

    /**
     * 将根 JSON 规范为单个 {@link JSONObject}：对象直接返回；数组则按序 {@code putAll} 合并（仅合入 JSON 对象元素）。
     */
    private static JSONObject normalizeValidateRuleToJSONObject(String trimmed) {
        if (trimmed.charAt(0) == '[') {
            JSONArray array = JSON.parseArray(trimmed);
            if (array == null || array.isEmpty()) {
                return null;
            }
            JSONObject merged = new JSONObject();
            for (int i = 0; i < array.size(); i++) {
                Object el = array.get(i);
                if (el instanceof JSONObject) {
                    merged.putAll((JSONObject) el);
                }
            }
            return merged.isEmpty() ? null : merged;
        }
        JSONObject obj = JSON.parseObject(trimmed);
        return obj == null || obj.isEmpty() ? null : obj;
    }

    private static void fillPatternRules(JSONObject jsonObject, Map<String, Object> rules) {
        String pattern = null;
        if (jsonObject.containsKey("pattern")) {
            pattern = jsonObject.getString("pattern");
        } else {
            String type = jsonObject.getString("type");
            if (type != null && !"crossField".equalsIgnoreCase(type) && BUILT_IN_REGEX_MAP.containsKey(type)) {
                pattern = BUILT_IN_REGEX_MAP.get(type);
            }
        }

        if (pattern != null && !pattern.isEmpty()) {
            String message = jsonObject.getString("message");
            rules.put("hasPattern", true);
            rules.put("pattern", pattern);
            rules.put("patternMessage", message != null ? message : "格式不正确");
        } else {
            rules.put("hasPattern", false);
        }
    }

    private static void fillOptionsRules(JSONObject jsonObject, Map<String, Object> rules) {
        if (jsonObject.containsKey("options")) {
            rules.put("hasOptions", true);
            rules.put("options", jsonObject.get("options"));
        } else {
            rules.put("hasOptions", false);
        }
    }

    private static void fillLengthRules(JSONObject jsonObject, Map<String, Object> rules) {
        boolean hasLength = jsonObject.containsKey("minLength") || jsonObject.containsKey("maxLength");
        rules.put("hasLength", hasLength);
        if (!hasLength) {
            return;
        }
        if (jsonObject.containsKey("minLength")) {
            rules.put("minLength", convertToNumber(jsonObject.get("minLength")));
        }
        if (jsonObject.containsKey("maxLength")) {
            rules.put("maxLength", convertToNumber(jsonObject.get("maxLength")));
        }
        String lengthMessage = jsonObject.getString("lengthMessage");
        rules.put("lengthMessage", lengthMessage != null ? lengthMessage : "长度必须在${minLength}到${maxLength}之间");
    }

    private static void fillNumericRangeRules(JSONObject jsonObject, Map<String, Object> rules) {
        boolean hasRange = jsonObject.containsKey("min") || jsonObject.containsKey("max");
        rules.put("hasRange", hasRange);
        if (!hasRange) {
            return;
        }
        if (jsonObject.containsKey("min")) {
            rules.put("min", convertToNumber(jsonObject.get("min")));
        }
        if (jsonObject.containsKey("max")) {
            rules.put("max", convertToNumber(jsonObject.get("max")));
        }
        String rangeMessage = jsonObject.getString("rangeMessage");
        rules.put("rangeMessage", rangeMessage != null ? rangeMessage : "数值必须在${min}到${max}之间");
    }

    private static void fillOperatorRules(JSONObject jsonObject, Map<String, Object> rules) {
        // crossField 规则里的 operator 表示字段间比较符，与 IN/BETWEEN 元数据操作符无关
        if ("crossField".equalsIgnoreCase(jsonObject.getString("type"))) {
            rules.put("hasOperator", false);
            return;
        }
        if (!jsonObject.containsKey("operator")) {
            rules.put("hasOperator", false);
            return;
        }
        String operator = jsonObject.getString("operator");
        rules.put("hasOperator", true);
        rules.put("operator", operator);

        String operatorMessage = jsonObject.getString("operatorMessage");
        rules.put("operatorMessage", operatorMessage != null ? operatorMessage : "值必须在指定范围内");

        if ("IN".equalsIgnoreCase(operator) && jsonObject.containsKey("values")) {
            rules.put("values", jsonObject.get("values"));
        }

        if ("BETWEEN".equalsIgnoreCase(operator)) {
            if (jsonObject.containsKey("min")) {
                rules.put("min", convertToNumber(jsonObject.get("min")));
            }
            if (jsonObject.containsKey("max")) {
                rules.put("max", convertToNumber(jsonObject.get("max")));
            }
        }
    }

    private static void fillCrossFieldRules(JSONObject jsonObject, Map<String, Object> rules) {
        if (!jsonObject.containsKey("type") || !"crossField".equalsIgnoreCase(jsonObject.getString("type"))) {
            rules.put("hasCrossField", false);
            return;
        }
        rules.put("hasCrossField", true);
        if (jsonObject.containsKey("field1")) {
            rules.put("crossField1", jsonObject.getString("field1"));
        }
        if (jsonObject.containsKey("operator")) {
            rules.put("crossFieldOperator", jsonObject.getString("operator"));
        }
        if (jsonObject.containsKey("field2")) {
            rules.put("crossField2", jsonObject.getString("field2"));
        }
        if (jsonObject.containsKey("condition")) {
            rules.put("crossFieldCondition", jsonObject.getString("condition"));
        }
        if (jsonObject.containsKey("message")) {
            String message = jsonObject.getString("message");
            if (message != null && !message.trim().isEmpty()) {
                rules.put("crossFieldMessage", message);
            } else if (jsonObject.containsKey("condition")) {
                rules.put("crossFieldMessage", jsonObject.getString("condition"));
            }
        } else if (jsonObject.containsKey("condition")) {
            rules.put("crossFieldMessage", jsonObject.getString("condition"));
        }
    }

    /**
     * 将对象转换为Number类型，如果是字符串则尝试解析为数值
     *
     * @param value 要转换的值
     * @return 转换后的数值
     */
    public static Number convertToNumber(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return (Number) value;
        }
        if (value instanceof String) {
            String strValue = (String) value;
            try {
                if (strValue.contains(".")) {
                    return Double.parseDouble(strValue);
                } else {
                    return Long.parseLong(strValue);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 工具方法：转换为类名（大驼峰）
     *
     * @param code 编码
     * @return 类名
     */
    public static String convertToClassName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的类名
        String processedCode = code.replace("_TABLE", "");
        String className = convertToCamelCase(processedCode, true);
        // 检查是否为Java关键字，若是则添加前缀
        if (JAVA_KEYWORDS.contains(className.toLowerCase())) {
            return "_" + className;
        }
        return className;
    }

    /**
     * 工具方法：转换为实体名（小驼峰）
     *
     * @param code 编码
     * @return 实体名
     */
    public static String convertToEntityName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的实体名
        String processedCode = code.replace("_TABLE", "");
        String entityName = convertToCamelCase(processedCode, false);
        // 检查是否为Java关键字，若是则添加前缀
        if (JAVA_KEYWORDS.contains(entityName)) {
            return "_" + entityName;
        }
        return entityName;
    }

    /**
     * 工具方法：转换为组件名
     *
     * @param code 编码
     * @return 组件名
     */
    public static String convertToComponentName(String code) {
        // 去掉表编码中的_TABLE后缀，生成更简洁的组件名
        String processedCode = code.replace("_TABLE", "");
        return convertToCamelCase(processedCode, true);
    }

    /**
     * 工具方法：转换为表名（下划线）
     *
     * @param code 编码
     * @return 表名
     */
    public static String convertToTableName(String code) {
        // 将 TABLE_CODE 转换为 table_code
        return code.toLowerCase().replace("_TABLE", "");
    }

    /**
     * 侧栏/页头展示名：规范为「xx管理」，去掉「列表表」「列表」等生成痕迹。
     *
     * @param preferred 优先使用的名称（功能节点名或表中文名）
     * @param tableNameFallback 表中文名兜底
     */
    public static String formatMenuTitle(String preferred, String tableNameFallback) {
        String raw = preferred;
        if (raw == null || raw.trim().isEmpty()) {
            raw = tableNameFallback;
        }
        if (raw == null || raw.trim().isEmpty()) {
            return "数据管理";
        }
        String s = raw.trim();
        String[] stripSuffixes = {"列表表", "列表页", "列表", "表单页", "表单", "详情页", "详情", "明细页", "明细",
                "批量导入页", "批量导入", "管理管理"};
        boolean changed;
        do {
            changed = false;
            for (String suffix : stripSuffixes) {
                if (s.endsWith(suffix) && s.length() > suffix.length()) {
                    s = s.substring(0, s.length() - suffix.length()).trim();
                    changed = true;
                }
            }
            while (s.endsWith("表") && s.length() > 1) {
                s = s.substring(0, s.length() - 1).trim();
                changed = true;
            }
        } while (changed);
        if (s.isEmpty()) {
            s = "数据";
        }
        if (!s.endsWith("管理")) {
            s = s + "管理";
        }
        return s;
    }

    /** 是否宜出现在侧栏菜单（列表/报表/批量导入导出等入口；表单/详情/单条导入不进菜单） */
    public static boolean isSidebarMenuNodeType(String nodeType) {
        if (nodeType == null || nodeType.isEmpty()) {
            return true;
        }
        String t = nodeType.toUpperCase();
        if ("FORM_PAGE".equals(t) || "DETAIL_PAGE".equals(t) || "IMPORT_PAGE".equals(t) || "PROCESS_PAGE".equals(t)) {
            return false;
        }
        if ("LIST_PAGE".equals(t) || "REPORT_PAGE".equals(t) || "BATCH_IMPORT_PAGE".equals(t) || "BATCH_EXPORT_PAGE".equals(t)) {
            return true;
        }
        if (t.contains("BATCH_IMPORT") || t.contains("BATCH_EXPORT") || t.contains("REPORT")) {
            return true;
        }
        if (t.contains("FORM") || t.contains("DETAIL") || t.contains("PROCESS")) {
            return false;
        }
        if (t.contains("IMPORT") && !t.contains("BATCH")) {
            return false;
        }
        return t.contains("LIST");
    }

    /**
     * 工具方法：转换为驼峰命名
     *
     * @param code       编码
     * @param firstUpper 首字母是否大写
     * @return 驼峰命名
     */
    public static String convertToCamelCase(String code, boolean firstUpper) {
        if (code == null || code.isEmpty()) {
            return code;
        }
        String[] parts = code.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0 && !firstUpper) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1));
                }
            }
        }
        String result = sb.toString();
        // 检查是否为Java关键字，若是则添加前缀
        if (JAVA_KEYWORDS.contains(result)) {
            return "_" + result;
        }
        return result;
    }

    /**
     * 获取Java类型
     *
     * @param fieldType 字段类型
     * @return Java类型
     */
    public static String getJavaType(String fieldType) {
        if (fieldType == null) {
            return "String";
        }
        String type = fieldType.toLowerCase();
        if (type.contains("int") && !type.contains("bigint")) {
            return "Integer";
        } else if (type.contains("bigint")) {
            return "Long";
        } else if (type.contains("decimal") || type.contains("numeric") || type.contains("float") || type.contains("double")) {
            return "BigDecimal";
        } else if (type.contains("date") || type.contains("time")) {
            return "LocalDateTime";
        } else if (type.contains("boolean") || (type.contains("tinyint") && type.contains("1"))) {
            return "Boolean";
        } else {
            return "String";
        }
    }

    /**
     * 检查字段列表中是否有日期类型
     *
     * @param fields 字段列表
     * @return 是否包含日期类型
     */
    public static boolean hasDate(List<MetadataField> fields) {
        return fields.stream().anyMatch(f ->
                f.getFieldType() != null &&
                        (f.getFieldType().toLowerCase().contains("date") || f.getFieldType().toLowerCase().contains("time"))
        );
    }

    /**
     * 检查字段列表中是否有Decimal类型
     *
     * @param fields 字段列表
     * @return 是否包含Decimal类型
     */
    public static boolean hasDecimal(List<MetadataField> fields) {
        return fields.stream().anyMatch(f ->
                f.getFieldType() != null &&
                        (f.getFieldType().toLowerCase().contains("decimal") ||
                                f.getFieldType().toLowerCase().contains("numeric") ||
                                f.getFieldType().toLowerCase().contains("float") ||
                                f.getFieldType().toLowerCase().contains("double"))
        );
    }

    /**
     * 将字符串中的 \\uXXXX / \\UXXXXXXXX Unicode 转义还原为真实字符。
     * 仅处理合法十六进制序列；其他内容保持不变。
     */
    private static String unescapeUnicode(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder out = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (next == 'u' && i + 6 <= input.length()) {
                    String hex = input.substring(i + 2, i + 6);
                    if (hex.matches("[0-9a-fA-F]{4}")) {
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 5;
                        continue;
                    }
                } else if (next == 'U' && i + 10 <= input.length()) {
                    String hex = input.substring(i + 2, i + 10);
                    if (hex.matches("[0-9a-fA-F]{8}")) {
                        int codePoint = (int) Long.parseLong(hex, 16);
                        out.append(new String(Character.toChars(codePoint)));
                        i += 9;
                        continue;
                    }
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * 生成CHECK约束
     *
     * @param field 字段信息
     * @return CHECK约束字符串
     */
    public static String generateCheckConstraint(MetadataField field) {
        Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
        List<String> conditions = new ArrayList<>();

        // 处理正则表达式
        if (validationRules.containsKey("hasPattern") && (Boolean) validationRules.get("hasPattern")) {
            String pattern = (String) validationRules.get("pattern");
            // MySQL REGEXP/REGEXP_LIKE 不支持 \\uXXXX 这类Unicode转义，需先将其还原为真实字符
            pattern = unescapeUnicode(pattern);
            conditions.add("`" + field.getFieldName() + "` REGEXP '" + pattern + "'");
        }

        // 处理数值范围
        if (validationRules.containsKey("hasRange") && (Boolean) validationRules.get("hasRange")) {
            Number min = convertToNumber(validationRules.get("min"));
            Number max = convertToNumber(validationRules.get("max"));

            if (min != null && max != null) {
                conditions.add("`" + field.getFieldName() + "` BETWEEN " + min + " AND " + max);
            } else if (min != null) {
                conditions.add("`" + field.getFieldName() + "` >= " + min);
            } else if (max != null) {
                conditions.add("`" + field.getFieldName() + "` <= " + max);
            }
        }

        // 处理枚举值
        if (validationRules.containsKey("hasOptions") && (Boolean) validationRules.get("hasOptions")) {
            Object options = validationRules.get("options");
            if (options instanceof JSONArray) {
                JSONArray optionsArray = (JSONArray) options;
                if (!optionsArray.isEmpty()) {
                    StringBuilder inClause = new StringBuilder();
                    inClause.append("`").append(field.getFieldName()).append("` IN (");
                    for (int i = 0; i < optionsArray.size(); i++) {
                        if (i > 0) {
                            inClause.append(", ");
                        }
                        Object option = optionsArray.get(i);
                        if (option instanceof String) {
                            inClause.append("'").append(option).append("'");
                        } else {
                            inClause.append(option);
                        }
                    }
                    inClause.append(")");
                    conditions.add(inClause.toString());
                }
            }
        }

        // 处理操作符（IN、BETWEEN等）
        if (validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
            String operator = (String) validationRules.get("operator");

            // 处理IN操作符
            if ("IN".equalsIgnoreCase(operator)) {
                Object values = validationRules.get("values");
                if (values instanceof JSONArray) {
                    JSONArray valuesArray = (JSONArray) values;
                    if (!valuesArray.isEmpty()) {
                        StringBuilder inClause = new StringBuilder();
                        inClause.append("`").append(field.getFieldName()).append("` IN (");
                        for (int i = 0; i < valuesArray.size(); i++) {
                            if (i > 0) {
                                inClause.append(", ");
                            }
                            Object value = valuesArray.get(i);
                            if (value instanceof String) {
                                inClause.append("'").append(value).append("'");
                            } else {
                                inClause.append(value);
                            }
                        }
                        inClause.append(")");
                        conditions.add(inClause.toString());
                    }
                }
            }

            // 处理BETWEEN操作符
            else if ("BETWEEN".equalsIgnoreCase(operator)) {
                Number min = convertToNumber(validationRules.get("min"));
                Number max = convertToNumber(validationRules.get("max"));
                if (min != null && max != null) {
                    conditions.add("`" + field.getFieldName() + "` BETWEEN " + min + " AND " + max);
                }
            }
        }

        // 如果有条件，生成单个CHECK约束
        if (!conditions.isEmpty()) {
            return "CHECK (" + String.join(" AND ", conditions) + ")";
        }

        return null;
    }

    /**
     * 判断字段是否为外键
     *
     * @param field     字段信息
     * @param relations 表关联关系列表
     * @return 是否为外键
     */
    public static boolean isForeignKey(MetadataField field, List<MetadataTableRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return false;
        }

        return relations.stream().anyMatch(relation ->
                relation.getSlaveFieldCode().equalsIgnoreCase(field.getFieldName())
        );
    }

    /**
     * 获取字段关联的主表信息
     *
     * @param field     字段信息
     * @param relations 表关联关系列表
     * @return 关联的主表信息
     */
    public static MetadataTableRelation getRelatedTableRelation(MetadataField field, List<MetadataTableRelation> relations) {
        if (relations == null || relations.isEmpty()) {
            return null;
        }

        return relations.stream()
                .filter(relation -> relation.getSlaveFieldCode().equalsIgnoreCase(field.getFieldName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取关联主表名称
     *
     * @param relation 表关联关系
     * @return 关联主表名称
     */
    public static String getRelatedTableName(MetadataTableRelation relation) {
        if (relation == null) {
            return null;
        }
        return relation.getMainTableCode();
    }

    /**
     * 获取关联主表字段名称
     *
     * @param relation 表关联关系
     * @return 关联主表字段名称
     */
    public static String getRelatedTableFieldName(MetadataTableRelation relation) {
        if (relation == null) {
            return null;
        }
        return relation.getMainFieldCode();
    }

    /**
     * 准备字段列表，添加转换后的属性和关联关系信息
     *
     * @param fields    字段列表
     * @param relations 表关联关系列表
     * @return 转换后的字段列表
     */
    public static List<Map<String, Object>> prepareFieldList(List<MetadataField> fields, List<MetadataTableRelation> relations) {
        List<Map<String, Object>> fieldList = new ArrayList<>();
        for (MetadataField field : fields) {
            // 只处理启用的字段
            if (field.getIsEnabled() != null && field.getIsEnabled() == 0) {
                continue;
            }

            Map<String, Object> fieldMap = new HashMap<>();
            fieldMap.put("field", field);
            fieldMap.put("fieldName", field.getFieldName());
            // 格式化字段类型，确保生成有效的MySQL数据类型
            String formattedFieldType = formatFieldType(field.getFieldType());

            // 处理enum类型，从validateRule中获取enum值
            if ("enum('')".equals(formattedFieldType)) {
                Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
                if (validationRules.containsKey("hasOptions") && (Boolean) validationRules.get("hasOptions")) {
                    Object options = validationRules.get("options");
                    if (options instanceof JSONArray) {
                        JSONArray optionsArray = (JSONArray) options;
                        if (!optionsArray.isEmpty()) {
                            StringBuilder enumValues = new StringBuilder();
                            for (int i = 0; i < optionsArray.size(); i++) {
                                if (i > 0) {
                                    enumValues.append(", ");
                                }
                                Object option = optionsArray.get(i);
                                if (option instanceof String) {
                                    enumValues.append("'").append(option).append("'");
                                } else {
                                    enumValues.append(option);
                                }
                            }
                            formattedFieldType = "enum(" + enumValues.toString() + ")";
                        }
                    }
                }

                // 处理操作符为IN的情况，从values字段获取enum值
                if ("enum('')".equals(formattedFieldType) && validationRules.containsKey("hasOperator") && (Boolean) validationRules.get("hasOperator")) {
                    String operator = (String) validationRules.get("operator");
                    if ("IN".equalsIgnoreCase(operator)) {
                        Object values = validationRules.get("values");
                        if (values instanceof JSONArray) {
                            JSONArray valuesArray = (JSONArray) values;
                            if (!valuesArray.isEmpty()) {
                                StringBuilder enumValues = new StringBuilder();
                                for (int i = 0; i < valuesArray.size(); i++) {
                                    if (i > 0) {
                                        enumValues.append(", ");
                                    }
                                    Object value = valuesArray.get(i);
                                    if (value instanceof String) {
                                        enumValues.append("'").append(value).append("'");
                                    } else {
                                        enumValues.append(value);
                                    }
                                }
                                formattedFieldType = "enum(" + enumValues.toString() + ")";
                            }
                        }
                    }
                }
            }

            fieldMap.put("fieldType", formattedFieldType);
            fieldMap.put("label", field.getLabel());
            fieldMap.put("isRequired", field.getIsRequired());
            fieldMap.put("formComponent", field.getFormComponent());
            fieldMap.put("inForm", field.getInForm());
            fieldMap.put("javaType", getJavaType(field.getFieldType()));

            // 生成字段名并检查是否为关键字
            String camelCaseName = convertToCamelCase(field.getFieldName(), false);
            if (JAVA_KEYWORDS.contains(camelCaseName)) {
                camelCaseName = "_" + camelCaseName;
            }
            fieldMap.put("camelCaseName", camelCaseName);

            // 解析校验规则
            Map<String, Object> validationRules = parseValidationRule(field.getValidateRule());
            fieldMap.put("validationRules", validationRules);

            // 处理关联关系
            if (relations != null && !relations.isEmpty()) {
                boolean isForeign = isForeignKey(field, relations);
                fieldMap.put("isForeignKey", isForeign);

                if (isForeign) {
                    MetadataTableRelation relation = getRelatedTableRelation(field, relations);
                    fieldMap.put("relation", relation);
                    fieldMap.put("relatedTableName", getRelatedTableName(relation));
                    fieldMap.put("relatedTableFieldName", getRelatedTableFieldName(relation));
                    fieldMap.put("relatedTableClassName", convertToClassName(getRelatedTableName(relation)));
                    fieldMap.put("relatedTableCamelCaseName", convertToCamelCase(getRelatedTableName(relation), false));
                }
            } else {
                fieldMap.put("isForeignKey", false);
            }

            fieldList.add(fieldMap);
        }
        return fieldList;
    }

}
