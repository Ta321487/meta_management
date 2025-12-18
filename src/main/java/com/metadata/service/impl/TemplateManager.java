package com.metadata.service.impl;

import com.metadata.service.exception.CodeGenException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.StringWriter;
import java.util.Map;

/**
 * 模板管理类，用于统一管理模板的加载和处理
 */
public class TemplateManager {
    
    private static TemplateManager instance;
    
    private Configuration freemarkerConfig;
    
    /**
     * 构造方法，初始化FreeMarker配置
     */
    private TemplateManager() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_32);
        // 使用 ClassTemplateLoader 从类路径加载模板
        ClassTemplateLoader templateLoader = new ClassTemplateLoader(
            Thread.currentThread().getContextClassLoader(), "/templates");
        freemarkerConfig.setTemplateLoader(templateLoader);
        freemarkerConfig.setDefaultEncoding("UTF-8");
    }
    
    /**
     * 获取单例实例
     * @return TemplateManager实例
     */
    public static synchronized TemplateManager getInstance() {
        if (instance == null) {
            instance = new TemplateManager();
        }
        return instance;
    }
    
    /**
     * 获取模板
     * @param templateName 模板名称
     * @return 模板对象
     * @throws CodeGenException 模板加载异常
     */
    public Template getTemplate(String templateName) throws CodeGenException {
        try {
            return freemarkerConfig.getTemplate(templateName);
        } catch (Exception e) {
            throw new CodeGenException("TEMPLATE_LOAD_ERROR", "模板加载失败: " + templateName, e);
        }
    }
    
    /**
     * 处理模板，生成内容
     * @param templateName 模板名称
     * @param data 模板数据
     * @return 生成的内容
     * @throws CodeGenException 模板处理异常
     */
    public String processTemplate(String templateName, Map<String, Object> data) throws CodeGenException {
        Template template = getTemplate(templateName);
        try (StringWriter writer = new StringWriter()) {
            template.process(data, writer);
            return writer.toString();
        } catch (TemplateException e) {
            throw new CodeGenException("TEMPLATE_PROCESS_ERROR", "模板处理失败: " + templateName, e);
        } catch (Exception e) {
            throw new CodeGenException("TEMPLATE_PROCESS_ERROR", "模板处理失败: " + templateName, e);
        }
    }
    
    /**
     * 获取FreeMarker配置
     * @return FreeMarker配置
     */
    public Configuration getFreemarkerConfig() {
        return freemarkerConfig;
    }
}