package com.metadata.service.impl;

import com.metadata.entity.MetadataBusinessSystem;
import com.metadata.entity.MetadataTable;
import com.metadata.mapper.MetadataTableMapper;
import com.metadata.service.MetadataBusinessSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 解析物理 DDL 应执行的 MySQL catalog：表级 database_name 优先，否则业务系统默认库。
 */
@Component
public class BusinessCatalogResolver {

    @Autowired
    private MetadataTableMapper tableMapper;

    @Autowired
    private MetadataBusinessSystemService businessSystemService;

    public String resolveCatalog(MetadataTable table) {
        if (table == null) {
            return null;
        }
        if (StringUtils.hasText(table.getDatabaseName())) {
            return table.getDatabaseName().trim();
        }
        String bc = table.getBusinessCode();
        if (!StringUtils.hasText(bc)) {
            return null;
        }
        MetadataBusinessSystem bs = businessSystemService.getByCode(bc);
        if (bs != null && StringUtils.hasText(bs.getDatabaseName())) {
            return bs.getDatabaseName().trim();
        }
        return null;
    }

    /**
     * @param businessCode 可为空；为空时仅用 tableCode 查表（可能命中多条中的第一条，建议传 businessCode）
     */
    public String resolveCatalog(String tableCode, String businessCode) {
        if (!StringUtils.hasText(tableCode)) {
            return null;
        }
        MetadataTable table = StringUtils.hasText(businessCode)
                ? tableMapper.selectByCode(tableCode, businessCode)
                : tableMapper.selectByCode(tableCode);
        return resolveCatalog(table);
    }
}
