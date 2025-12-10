package com.metadata.common;

import lombok.Data;

import java.util.List;

/**
 * 关联模块请求类
 */
@Data
public class AssociateModulesRequest {
    /**
     * 业务系统编码
     */
    private String businessCode;
    /**
     * 模块编码列表
     */
    private List<String> moduleCodes;
}