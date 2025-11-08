package com.metadata.common;

import lombok.Data;

/**
 * 分页请求
 */
@Data
public class PageRequest {
    private Integer current = 1;
    private Integer size = 10;

    public Integer getOffset() {
        return (current - 1) * size;
    }
}

