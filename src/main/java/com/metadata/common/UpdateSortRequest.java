package com.metadata.common;

import lombok.Data;

/**
 * 更新排序请求类
 */
@Data
public class UpdateSortRequest {
    /**
     * 要更新的ID
     */
    private Long id;
    /**
     * 新排序值
     */
    private Integer sort;
}