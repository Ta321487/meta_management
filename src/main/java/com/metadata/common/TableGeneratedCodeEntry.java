package com.metadata.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 业务系统内单表代码包项
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "业务系统内单表代码包")
public class TableGeneratedCodeEntry {

    /**
     * 表编码
     */
    @Schema(description = "表编码")
    private String tableCode;

    /**
     * 该表生成的完整代码包
     */
    @Schema(description = "该表生成的全部源码")
    private TableGeneratedCodeBundle files;
}
