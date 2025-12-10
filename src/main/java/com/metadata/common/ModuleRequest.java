package com.metadata.common;

import com.metadata.entity.MetadataModule;
import lombok.Data;

import java.util.List;

/**
 * 模块请求类
 */
@Data
public class ModuleRequest {
    /**
     * 模块信息
     */
    private MetadataModule module;
    /**
     * 关联的表编码列表
     */
    private List<String> tableCodes;
}