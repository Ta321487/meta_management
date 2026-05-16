package com.metadata.service.impl;

import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataFunctionNodeMapper;
import com.metadata.service.MetadataBusinessRuleService;
import com.metadata.service.MetadataBusinessSystemService;
import com.metadata.service.MetadataFieldService;
import com.metadata.service.MetadataTableRelationService;
import com.metadata.service.MetadataTableService;
import com.metadata.service.exception.CodeGenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 业务系统预览：与 ZIP 生成同源的数据模型 + 内存 Mock API，供元数据平台内试跑前后端逻辑。
 */
@Service
public class CodegenPreviewService {

    private final MetadataTableService tableService;
    private final MetadataBusinessSystemService businessSystemService;
    private final VueCodeGenerator vueCodeGenerator;

    @Autowired
    public CodegenPreviewService(MetadataTableService tableService,
                                 MetadataFieldService fieldService,
                                 MetadataBusinessSystemService businessSystemService,
                                 MetadataFunctionNodeMapper nodeMapper,
                                 MetadataBusinessRuleService businessRuleService,
                                 MetadataTableRelationService relationService) {
        this.tableService = tableService;
        this.businessSystemService = businessSystemService;
        this.vueCodeGenerator = new VueCodeGenerator(tableService, fieldService, businessSystemService,
                nodeMapper, businessRuleService, relationService);
    }

    public Map<String, Object> buildBusinessPreviewSpec(String businessCode, String packageName,
                                                        boolean useInterface, boolean captchaEnabled) throws Exception {
        if (businessCode == null || businessCode.trim().isEmpty()) {
            throw new CodeGenException("BUSINESS_CODE_REQUIRED", "请指定业务系统编码");
        }
        businessCode = businessCode.trim();
        MetadataBusinessSystem bs = businessSystemService.getByCode(businessCode);
        if (bs == null) {
            throw new CodeGenException("BUSINESS_NOT_FOUND", "业务系统不存在: " + businessCode);
        }
        packageName = resolvePackageName(packageName, bs);

        List<MetadataTable> enabledTables = tableService.list(null, businessCode).stream()
                .filter(t -> t.getIsEnabled() != null && t.getIsEnabled() == 1)
                .collect(Collectors.toList());
        if (enabledTables.isEmpty()) {
            throw new CodeGenException("NO_ENABLED_TABLES", "业务系统下没有启用的表");
        }

        List<Map<String, Object>> routes = vueCodeGenerator.collectBusinessRoutes(businessCode);
        List<Map<String, Object>> tables = new ArrayList<>();
        List<Map<String, Object>> apiCatalog = new ArrayList<>();

        for (MetadataTable t : enabledTables) {
            String tableCode = t.getTableCode();
            Map<String, Object> form = vueCodeGenerator.buildFormModel(tableCode, businessCode);
            Map<String, Object> list = vueCodeGenerator.buildListModel(tableCode, businessCode);
            String entityName = (String) form.get("entityName");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tableCode", tableCode);
            row.put("tableName", t.getTableName());
            row.put("entityName", entityName);
            row.put("componentName", form.get("componentName"));
            row.put("form", form);
            row.put("list", list);
            row.put("mockApiBase", "/codegen/preview/mock/" + businessCode + "/" + entityName);
            row.put("zipApiBase", "/api/" + businessCode + "/" + entityName);
            tables.add(row);

            String base = "/api/" + businessCode + "/" + entityName;
            for (String suffix : List.of("/page", "/list", "/add", "/update", "/delete", "/batchDelete")) {
                apiCatalog.add(Map.of(
                        "tableCode", tableCode,
                        "tableName", t.getTableName() != null ? t.getTableName() : tableCode,
                        "method", suffix.equals("/page") || suffix.equals("/list") ? "GET" : "POST",
                        "path", base + suffix,
                        "mockPath", "/codegen/preview/mock/" + businessCode + "/" + entityName + suffix
                ));
            }
        }

        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("businessCode", businessCode);
        spec.put("businessName", bs.getBusinessName() != null ? bs.getBusinessName() : businessCode);
        spec.put("packageName", packageName);
        spec.put("captchaEnabled", captchaEnabled);
        spec.put("useInterface", useInterface);
        spec.put("routes", routes);
        spec.put("tables", tables);
        spec.put("apiCatalog", apiCatalog);
        spec.put("enabledTableCount", enabledTables.size());
        return spec;
    }

    public void resetMockData(String businessCode) {
        CodegenPreviewMockStore.resetBusiness(businessCode);
    }

    private static String resolvePackageName(String packageName, MetadataBusinessSystem bsMeta) {
        if (packageName != null && !packageName.trim().isEmpty()) {
            return packageName.trim();
        }
        if (bsMeta.getPackageName() != null && !bsMeta.getPackageName().trim().isEmpty()) {
            return bsMeta.getPackageName().trim();
        }
        return "com.example";
    }
}
