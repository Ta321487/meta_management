package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 业务系统建表 SQL 导出响应类
 * <p>对应「按业务系统生成全部建表 SQL」接口 data。</p>
 */
@Data
@Schema(description = "业务系统下全部建表 SQL")
public class BusinessSystemSqlExportPayload {

    /**
     * 各启用表的建表 SQL 列表
     */
    @Schema(description = "各表建表 SQL 列表")
    private List<TableSqlFile> sqlFiles;
}
