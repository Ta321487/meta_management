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
     * 模块ID
     */
    private Long id;
    /**
     * 模块编码
     */
    private String moduleCode;
    /**
     * 模块名称
     */
    private String moduleName;
    /**
     * 模块类型
     */
    private String moduleType;
    /**
     * 业务系统编码
     */
    private String businessCode;
    /**
     * 描述
     */
    private String description;
    /**
     * 排序号
     */
    private Integer sort;
    /**
     * 图标
     */
    private String icon;
    /**
     * 路由路径
     */
    private String routePath;
    /**
     * 组件路径
     */
    private String componentPath;
    /**
     * 关联的表编码列表
     */
    private List<String> tableCodes;
    
    /**
     * 将当前对象转换为MetadataModule对象
     * @return MetadataModule对象
     */
    public MetadataModule toMetadataModule() {
        MetadataModule module = new MetadataModule();
        module.setId(this.id);
        module.setModuleCode(this.moduleCode);
        module.setModuleName(this.moduleName);
        module.setModuleType(this.moduleType);
        module.setBusinessCode(this.businessCode);
        module.setDescription(this.description);
        module.setSort(this.sort);
        module.setIcon(this.icon);
        module.setRoutePath(this.routePath);
        module.setComponentPath(this.componentPath);
        return module;
    }
}