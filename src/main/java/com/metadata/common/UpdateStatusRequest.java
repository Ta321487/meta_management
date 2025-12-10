package com.metadata.common;

import lombok.Data;

/**
 * 更新状态请求类
 */
@Data
public class UpdateStatusRequest {
    /**
     * 要更新的ID
     */
    private Long id;
    /**
     * 新状态
     */
    private Integer status;
}