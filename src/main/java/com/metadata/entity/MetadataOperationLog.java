package com.metadata.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
public class MetadataOperationLog {
    private Long id;
    private String operateUser;
    private String operateType;
    private String operateContent;
    private LocalDateTime operateTime;
    private Integer status;
    private String errorMsg;
}

