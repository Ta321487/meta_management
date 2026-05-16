package com.metadata.service.impl;

import com.metadata.service.exception.CodeGenException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成可独立运行的 Vue3 + Vite 前端工程骨架（package.json、vite、router 入口、Layout 等）。
 */
public class FrontendScaffoldGenerator {

    private final TemplateManager templateManager = TemplateManager.getInstance();

    public Map<String, String> generate(String artifactId, String appTitle, String serverPort) throws CodeGenException {
        Map<String, Object> data = new HashMap<>();
        data.put("artifactId", artifactId);
        data.put("appTitle", appTitle != null ? appTitle : "业务系统");
        data.put("serverPort", serverPort != null ? serverPort : "8080");

        Map<String, String> files = new HashMap<>();
        files.put("frontend/package.json", process("frontend-project/package.json.ftl", data));
        files.put("frontend/vite.config.js", process("frontend-project/vite.config.js.ftl", data));
        files.put("frontend/index.html", process("frontend-project/index.html.ftl", data));
        files.put("frontend/src/main.js", process("frontend-project/src/main.js.ftl", data));
        files.put("frontend/src/App.vue", process("frontend-project/src/App.vue.ftl", data));
        files.put("frontend/src/router/index.js", process("frontend-project/src/router/index.js.ftl", data));
        files.put("frontend/src/views/Layout.vue", process("frontend-project/src/views/Layout.vue.ftl", data));
        files.put("frontend/src/styles/admin.css", readStaticCss("templates/frontend-project/src/styles/admin.css"));
        return files;
    }

    private static String readStaticCss(String classpath) throws CodeGenException {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpath)) {
            if (in == null) {
                throw new CodeGenException("RESOURCE_NOT_FOUND", "缺少样式文件: " + classpath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (CodeGenException e) {
            throw e;
        } catch (Exception e) {
            throw new CodeGenException("RESOURCE_READ_FAILED", e.getMessage());
        }
    }

    private String process(String templateName, Map<String, Object> data) throws CodeGenException {
        return templateManager.processTemplate(templateName, data);
    }
}
