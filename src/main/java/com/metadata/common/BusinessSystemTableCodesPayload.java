package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 业务系统全部表代码包响应类
 * <p>对应「按业务系统生成所有表代码」接口 data。</p>
 */
@Data
@Schema(description = "业务系统下全部表的代码包")
public class BusinessSystemTableCodesPayload {

    /**
     * 各启用表的代码包列表
     */
    @Schema(description = "各表代码包列表")
    private List<TableGeneratedCodeEntry> tables;
}
