package com.metadata.common;

import lombok.Data;

import java.util.List;

/**
 * 批量删除请求类
 */
@Data
public class BatchDeleteRequest {
    /**
     * 要删除的ID列表
     */
    private List<Long> ids;
}