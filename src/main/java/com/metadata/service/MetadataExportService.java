package com.metadata.service;

import com.alibaba.fastjson2.JSONObject;

/**
 * 元数据导出服务接口
 */
public interface MetadataExportService {

    /**
     * 导出模块元数据
     */
    JSONObject exportModule(String moduleCode);

    /**
     * 导出表元数据
     */
    JSONObject exportTable(String tableCode);

    /**
     * 导出所有元数据
     */
    JSONObject exportAll();
}