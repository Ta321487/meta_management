package com.metadata.init;

import com.metadata.entity.MetadataModuleType;
import com.metadata.service.MetadataModuleTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 模块类型初始化器
 * 在应用启动时自动检查并添加默认的模块类型
 */
@Component
public class ModuleTypeInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(ModuleTypeInitializer.class);

    @Autowired
    private MetadataModuleTypeService moduleTypeService;

    @Override
    public void run(String... args) {
        logger.info("开始初始化默认模块类型...");
        
        // 定义默认的模块类型
        List<DefaultModuleType> defaultTypes = Arrays.asList(
            new DefaultModuleType("DATA_MANAGE", "数据管理型", 
                Arrays.asList("LIST_PAGE", "FORM_PAGE", "DETAIL_PAGE"), 
                "支持CRUD操作的数据管理模块"),
            new DefaultModuleType("PROCESS_APPROVE", "流程审批型", 
                Arrays.asList("LIST_PAGE", "FORM_PAGE", "PROCESS_PAGE"), 
                "支持流程审批的模块"),
            new DefaultModuleType("STAT_REPORT", "统计报表型", 
                Arrays.asList("LIST_PAGE", "REPORT_PAGE"), 
                "支持统计报表的模块"),
            new DefaultModuleType("BATCH_OPERATE", "批量操作型", 
                Arrays.asList("LIST_PAGE", "FORM_PAGE", "DETAIL_PAGE", "IMPORT_PAGE"), 
                "支持CRUD和批量导入的模块")
        );
        
        // 检查并添加默认模块类型
        for (DefaultModuleType defaultType : defaultTypes) {
            try {
                // 检查是否已存在
                MetadataModuleType existingType = moduleTypeService.getByCode(defaultType.getTypeCode());
                if (existingType == null) {
                    // 创建新的模块类型
                    MetadataModuleType newType = new MetadataModuleType();
                    newType.setTypeCode(defaultType.getTypeCode());
                    newType.setTypeName(defaultType.getTypeName());
                    
                    // 设置默认节点
                    newType.setDefaultNodes(defaultType.getDefaultNodes());
                    newType.setDescription(defaultType.getDescription());
                    
                    moduleTypeService.add(newType);
                    logger.info("成功添加默认模块类型: {}", defaultType.getTypeName());
                } else {
                    logger.info("模块类型已存在，跳过初始化: {}", defaultType.getTypeName());
                }
            } catch (Exception e) {
                logger.error("初始化模块类型时出错: {}", defaultType.getTypeName(), e);
            }
        }
        
        logger.info("默认模块类型初始化完成");
    }
    
    /**
     * 内部类，用于封装默认模块类型的信息
     */
    private static class DefaultModuleType {
        private String typeCode;
        private String typeName;
        private List<String> defaultNodes;
        private String description;
        
        public DefaultModuleType(String typeCode, String typeName, List<String> defaultNodes, String description) {
            this.typeCode = typeCode;
            this.typeName = typeName;
            this.defaultNodes = defaultNodes;
            this.description = description;
        }
        
        public String getTypeCode() {
            return typeCode;
        }
        
        public String getTypeName() {
            return typeName;
        }
        
        public List<String> getDefaultNodes() {
            return defaultNodes;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
