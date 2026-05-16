package com.metadata.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.metadata.entity.MetadataBusinessRule;
import com.metadata.entity.MetadataField;
import com.metadata.entity.MetadataFunctionNode;
import com.metadata.entity.MetadataTable;
import com.metadata.entity.MetadataTableRelation;
import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.MetadataTableRelationService;
import com.metadata.service.MetadataTableService;
import com.metadata.service.exception.CodeGenException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Java代码生成模块，负责Java代码生成相关逻辑
 */
public class JavaCodeGenerator {
    
    private MetadataTableService tableService;
    private MetadataFieldService fieldService;
    private MetadataBusinessSystemService businessSystemService;
    private MetadataFunctionNodeMapper nodeMapper;
    private MetadataBusinessRuleService businessRuleService;
    private MetadataTableRelationService relationService;
    private TemplateManager templateManager;
    
    /**
     * 构造方法
     * @param tableService 表服务
     * @param fieldService 字段服务
     * @param businessSystemService 业务系统服务
     * @param nodeMapper 功能节点Mapper
     * @param businessRuleService 业务规则服务
     * @param relationService 表关联关系服务
     */
    public JavaCodeGenerator(MetadataTableService tableService,
                            MetadataFieldService fieldService,
                            MetadataBusinessSystemService businessSystemService,
                            MetadataFunctionNodeMapper nodeMapper,
                            MetadataBusinessRuleService businessRuleService,
                            MetadataTableRelationService relationService) {
        this.tableService = tableService;
        this.fieldService = fieldService;
        this.businessSystemService = businessSystemService;
        this.nodeMapper = nodeMapper;
        this.businessRuleService = businessRuleService;
        this.relationService = relationService;
        this.templateManager = TemplateManager.getInstance();
    }
    
    /**
     * 生成Java实体类
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @return Java实体类代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateEntity(String tableCode, String packageName, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 优先使用业务系统的包名
        MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
            packageName = businessSystem.getPackageName();
        }
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        if (fields.isEmpty()) {
            throw new CodeGenException("FIELD_NOT_FOUND", "表没有配置字段: " + tableCode);
        }

        // 获取表关联关系
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("packageName", packageName);
        data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
        data.put("tableName", CodeGenUtils.convertToTableName(table.getTableCode()));
        data.put("hasDate", CodeGenUtils.hasDate(fields));
        data.put("hasDecimal", CodeGenUtils.hasDecimal(fields));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));

        return templateManager.processTemplate("entity.java.ftl", data);
    }
    
    /**
     * 生成Controller
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @return Controller代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateController(String tableCode, String packageName, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 优先使用业务系统的包名
        com.metadata.entity.MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
            packageName = businessSystem.getPackageName();
        }
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        // 获取表关联关系
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("packageName", packageName);
        data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
        data.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));

        return templateManager.processTemplate("controller.java.ftl", data);
    }
    
    /**
     * 生成Service接口
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @return Service接口代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateServiceInterface(String tableCode, String packageName, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 优先使用业务系统的包名
        com.metadata.entity.MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
            packageName = businessSystem.getPackageName();
        }
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("packageName", packageName);
        data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
        data.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));

        return templateManager.processTemplate("service-interface.java.ftl", data);
    }
    
    /**
     * 生成Service实现类
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @return Service实现类代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateServiceImpl(String tableCode, String packageName, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 优先使用业务系统的包名
        com.metadata.entity.MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
            packageName = businessSystem.getPackageName();
        }
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);
        // 获取表相关的业务规则
        List<Map<String, Object>> businessRules = getTableBusinessRules(tableCode, businessCode);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("packageName", packageName);
        data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
        data.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
        data.put("businessRules", businessRules);

        return templateManager.processTemplate("service-impl.java.ftl", data);
    }
    
    /**
     * 生成Service
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @param useInterface 是否使用接口
     * @return Service代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateService(String tableCode, String packageName, String businessCode, boolean useInterface) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        if (useInterface) {
            // 如果使用接口，生成接口+实现类
            StringBuilder serviceCode = new StringBuilder();
            serviceCode.append(generateServiceInterface(tableCode, packageName, businessCode));
            serviceCode.append("\n\n");
            serviceCode.append(generateServiceImpl(tableCode, packageName, businessCode));
            return serviceCode.toString();
        } else {
            // 否则生成传统的Service类
            // 优先使用业务系统的包名
            com.metadata.entity.MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
            if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
                packageName = businessSystem.getPackageName();
            }
            
            MetadataTable table = tableService.getByCode(tableCode);
            if (table == null) {
                throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
            }

            List<MetadataField> fields = fieldService.listByTableCode(tableCode);
            List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
            List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);
            // 获取表相关的业务规则
            List<Map<String, Object>> businessRules = getTableBusinessRules(tableCode, businessCode);

            Map<String, Object> data = new HashMap<>();
            data.put("table", table);
            data.put("fields", fieldList);
            data.put("relations", relations);
            data.put("packageName", packageName);
            data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
            data.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
            data.put("businessCode", businessCode);
            data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));
            data.put("businessRules", businessRules);

            return templateManager.processTemplate("service.java.ftl", data);
        }
    }
    
    /**
     * 生成Mapper接口
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @return Mapper接口代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateMapper(String tableCode, String packageName, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 优先使用业务系统的包名
        com.metadata.entity.MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
            packageName = businessSystem.getPackageName();
        }
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("packageName", packageName);
        data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
        data.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        data.put("tableName", CodeGenUtils.convertToTableName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));

        return templateManager.processTemplate("mapper.java.ftl", data);
    }
    
    /**
     * 生成Mapper XML
     * @param tableCode 表编码
     * @param packageName 包名
     * @param businessCode 业务系统编码
     * @return Mapper XML代码
     * @throws CodeGenException 代码生成异常
     */
    public String generateMapperXml(String tableCode, String packageName, String businessCode) throws CodeGenException {
        // 为businessCode设置默认值，避免null值传递给模板
        businessCode = businessCode == null ? "DEFAULT" : businessCode;
        
        // 优先使用业务系统的包名
        com.metadata.entity.MetadataBusinessSystem businessSystem = businessSystemService.getByCode(businessCode);
        if (businessSystem != null && businessSystem.getPackageName() != null && !businessSystem.getPackageName().isEmpty()) {
            packageName = businessSystem.getPackageName();
        }
        
        MetadataTable table = tableService.getByCode(tableCode);
        if (table == null) {
            throw new CodeGenException("TABLE_NOT_FOUND", "表不存在: " + tableCode);
        }

        List<MetadataField> fields = fieldService.listByTableCode(tableCode);
        List<MetadataTableRelation> relations = relationService.listBySlaveTableCode(tableCode, businessCode);
        List<Map<String, Object>> fieldList = CodeGenUtils.prepareFieldList(fields, relations);

        Map<String, Object> data = new HashMap<>();
        data.put("table", table);
        data.put("fields", fieldList);
        data.put("relations", relations);
        data.put("packageName", packageName);
        data.put("className", CodeGenUtils.convertToClassName(table.getTableCode()));
        data.put("entityName", CodeGenUtils.convertToEntityName(table.getTableCode()));
        data.put("tableName", CodeGenUtils.convertToTableName(table.getTableCode()));
        data.put("businessCode", businessCode);
        data.put("businessName", CodeGenUtils.getBusinessName(businessCode, businessSystemService));

        return templateManager.processTemplate("mapper.xml.ftl", data);
    }
    
    /**
     * 获取表相关的业务规则
     * @param tableCode 表编码
     * @param businessCode 业务系统编码
     * @return 表相关的业务规则
     */
    private List<Map<String, Object>> getTableBusinessRules(String tableCode, String businessCode) {
        List<Map<String, Object>> tableRules = new ArrayList<>();
        try {
            // 获取表关联的模块编码列表（通过功能节点关联）
            List<MetadataFunctionNode> nodes = nodeMapper.selectByRelatedTableCode(tableCode, businessCode);
            Set<String> moduleCodes = nodes.stream()
                .map(MetadataFunctionNode::getModuleCode)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
            
            // 遍历每个关联的模块，获取业务规则
            for (String moduleCode : moduleCodes) {
                // 获取模块的所有业务规则
                List<MetadataBusinessRule> rules = businessRuleService.listByModuleCode(moduleCode, businessCode);
                
                // 筛选出VALIDATION_RULE类型的规则
                for (MetadataBusinessRule rule : rules) {
                    if ("VALIDATION_RULE".equals(rule.getRuleType())) {
                        // 解析规则内容
                        JSONObject ruleContent = JSONObject.parseObject(rule.getRuleContent());
                        if (ruleContent != null) {
                            String ruleType = ruleContent.getString("type");
                            if ("unique".equals(ruleType) || "unique_combo".equals(ruleType)) {
                                // 提取字段列表
                                List<String> ruleFields = new ArrayList<>();
                                if ("unique".equals(ruleType)) {
                                    // 单字段唯一
                                    String field = ruleContent.getString("field");
                                    if (field != null && !field.isEmpty()) {
                                        ruleFields.add(field);
                                    }
                                } else if ("unique_combo".equals(ruleType)) {
                                    // 组合字段唯一
                                    Object fieldsObj = ruleContent.get("fields");
                                    if (fieldsObj instanceof com.alibaba.fastjson2.JSONArray) {
                                        com.alibaba.fastjson2.JSONArray fieldsArray = (com.alibaba.fastjson2.JSONArray) fieldsObj;
                                        for (Object fieldObj : fieldsArray) {
                                            if (fieldObj instanceof String) {
                                                ruleFields.add((String) fieldObj);
                                            }
                                        }
                                    }
                                }
                                
                                // 检查字段是否都存在于当前表中
                                List<MetadataField> tableFields = fieldService.listByTableCode(tableCode);
                                Set<String> tableFieldNames = tableFields.stream()
                                    .map(MetadataField::getFieldName)
                                    .collect(Collectors.toSet());
                                
                                boolean allFieldsExist = true;
                                for (String ruleField : ruleFields) {
                                    if (!tableFieldNames.contains(ruleField)) {
                                        allFieldsExist = false;
                                        break;
                                    }
                                }
                                
                                if (allFieldsExist && !ruleFields.isEmpty()) {
                                    // 将原始字段名转换为包含camelCaseName属性的field对象
                                    List<Map<String, Object>> fieldObjects = new ArrayList<>();
                                    for (String ruleField : ruleFields) {
                                        for (MetadataField tableField : tableFields) {
                                            if (tableField.getFieldName().equals(ruleField)) {
                                                Map<String, Object> fieldMap = new HashMap<>();
                                                fieldMap.put("fieldName", ruleField);
                                                fieldMap.put("camelCaseName", CodeGenUtils.convertToCamelCase(ruleField, false));
                                                fieldObjects.add(fieldMap);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    Map<String, Object> ruleMap = new HashMap<>();
                                    ruleMap.put("ruleCode", rule.getRuleCode());
                                    ruleMap.put("ruleType", ruleType);
                                    ruleMap.put("fields", fieldObjects);
                                    ruleMap.put("message", ruleContent.getString("message"));
                                    ruleMap.put("description", rule.getDescription());
                                    tableRules.add(ruleMap);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 如果获取业务规则失败，不影响代码生成
            e.printStackTrace();
        }
        return tableRules;
    }
    
    /**
     * 生成Result统一响应结果类
     * @param packageName 包名
     * @return Result类代码
     */
    public String generateResult(String packageName) {
        return "package " + packageName + ";\n\n" +
                "import lombok.Data;\n" +
                "import java.io.Serializable;\n\n" +
                "/**\n" +
                " * 统一响应结果类\n" +
                " */\n" +
                "@Data\n" +
                "public class Result<T> implements Serializable {\n" +
                "    private Integer code;\n\n" +
                "    /**\n" +
                "     * 响应消息\n" +
                "     */\n" +
                "    private String message;\n\n" +
                "    /**\n" +
                "     * 响应数据\n" +
                "     */\n" +
                "    private T data;\n\n" +
                "    public Result() {\n" +
                "    }\n\n" +
                "    public Result(Integer code, String message, T data) {\n" +
                "        this.code = code;\n" +
                "        this.message = message;\n" +
                "        this.data = data;\n" +
                "    }\n\n" +
                "    public static <T> Result<T> success() {\n" +
                "        return new Result<>(200, \"操作成功\", null);\n" +
                "    }\n\n" +
                "    public static <T> Result<T> success(T data) {\n" +
                "        return new Result<>(200, \"操作成功\", data);\n" +
                "    }\n\n" +
                "    public static <T> Result<T> error(String message) {\n" +
                "        return new Result<>(500, message, null);\n" +
                "    }\n" +
                "}";
    }
    
    /**
     * 生成PageRequest分页请求类
     * @param packageName 包名
     * @return PageRequest类代码
     */
    public String generatePageRequest(String packageName) {
        return "package " + packageName + ";\n\n" +
                "import java.util.HashMap;\n" +
                "import java.util.Map;\n\n" +
                "/**\n" +
                " * 分页请求类\n" +
                " */\n" +
                "public class PageRequest {\n" +
                "    private Integer current = 1;\n" +
                "    private Integer size = 10;\n" +
                "    private String orderBy;\n" +
                "    private String orderDirection;\n" +
                "    private Map<String, Object> conditions;\n\n" +
                "    /**\n" +
                "     * 无参构造函数，初始化条件映射\n" +
                "     */\n" +
                "    public PageRequest() {\n" +
                "        this.conditions = new HashMap<>();\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取当前页码\n" +
                "     */\n" +
                "    public Integer getCurrent() {\n" +
                "        return current;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置当前页码\n" +
                "     */\n" +
                "    public void setCurrent(Integer current) {\n" +
                "        this.current = current;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取每页大小\n" +
                "     */\n" +
                "    public Integer getSize() {\n" +
                "        return size;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置每页大小\n" +
                "     */\n" +
                "    public void setSize(Integer size) {\n" +
                "        this.size = size;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取排序字段\n" +
                "     */\n" +
                "    public String getOrderBy() {\n" +
                "        return orderBy;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置排序字段\n" +
                "     */\n" +
                "    public void setOrderBy(String orderBy) {\n" +
                "        this.orderBy = orderBy;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取排序方向\n" +
                "     */\n" +
                "    public String getOrderDirection() {\n" +
                "        return orderDirection;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置排序方向\n" +
                "     */\n" +
                "    public void setOrderDirection(String orderDirection) {\n" +
                "        this.orderDirection = orderDirection;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取条件映射\n" +
                "     */\n" +
                "    public Map<String, Object> getConditions() {\n" +
                "        return conditions;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 设置条件映射\n" +
                "     */\n" +
                "    public void setConditions(Map<String, Object> conditions) {\n" +
                "        this.conditions = conditions;\n" +
                "    }\n\n" +
                "    /**\n" +
                "     * 获取偏移量\n" +
                "     */\n" +
                "    public Integer getOffset() {\n" +
                "        return (current - 1) * size;\n" +
                "    }\n" +
                "}";
    }
    
    /**
     * 生成PageResult分页结果类
     * @param packageName 包名
     * @return PageResult类代码
     */
    public String generatePageResult(String packageName) {
        return "package " + packageName + ";\n\n" +
                "import lombok.Data;\n" +
                "import java.util.List;\n\n" +
                "/**\n" +
                " * 分页结果类\n" +
                " */\n" +
                "@Data\n" +
                "public class PageResult<T> {\n" +
                "    private Long total;\n" +
                "    private List<T> records;\n\n" +
                "    public PageResult() {\n" +
                "    }\n\n" +
                "    public PageResult(Long total, List<T> records) {\n" +
                "        this.total = total;\n" +
                "        this.records = records;\n" +
                "    }\n" +
                "}";
    }

    /**
     * 生成认证扩展包（字符型验证码 + Session 登录）
     */
    public Map<String, String> generateAuthExtension(String packageName, boolean captchaEnabled) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("packageName", packageName);
        data.put("captchaEnabled", captchaEnabled);
        Map<String, String> files = new LinkedHashMap<>();
        if (captchaEnabled) {
            files.put("CaptchaService.java", templateManager.processTemplate("auth-extension/captcha-service.java.ftl", data));
        }
        files.put("AuthService.java", templateManager.processTemplate("auth-extension/auth-service.java.ftl", data));
        files.put("AuthServiceImpl.java", templateManager.processTemplate("auth-extension/auth-service-impl.java.ftl", data));
        files.put("AuthController.java", templateManager.processTemplate("auth-extension/auth-controller.java.ftl", data));
        files.put("LoginRequest.java", templateManager.processTemplate("auth-extension/login-request.java.ftl", data));
        files.put("LoginInterceptor.java", templateManager.processTemplate("auth-extension/login-interceptor.java.ftl", data));
        files.put("InterceptorConfig.java", templateManager.processTemplate("auth-extension/interceptor-config.java.ftl", data));
        return files;
    }
}