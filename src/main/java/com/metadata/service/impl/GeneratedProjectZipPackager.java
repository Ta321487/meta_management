package com.metadata.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 将生成结果打成 ZIP：解压后一层项目根目录，含完整 Maven 后端（业务系统下全部启用表）+ 可运行 Vite 前端。
 */
public final class GeneratedProjectZipPackager {

    private GeneratedProjectZipPackager() {
    }

    /**
     * @param codeMap         公共配置（pom、Application、认证、README 等）
     * @param backendExtras   各表后端文件，键为相对项目根路径，如 {@code src/main/java/.../entity/Xxx.java}
     * @param frontendExtras  前端文件，键为相对项目根路径，如 {@code frontend/src/...}
     */
    public static byte[] pack(Map<String, String> codeMap,
                              Map<String, String> backendExtras,
                              Map<String, String> frontendExtras,
                              String packageName,
                              String businessCode) throws IOException {
        if (codeMap == null || codeMap.isEmpty()) {
            return new byte[0];
        }
        String pkgPath = packageName.replace('.', '/');
        int dot = packageName.lastIndexOf('.');
        String artifactId = dot >= 0 ? packageName.substring(dot + 1) : packageName;
        String rootName = (businessCode != null && !businessCode.isEmpty())
                ? sanitizeFolderName(businessCode) : sanitizeFolderName(artifactId);
        String root = rootName + "-generated";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            put(zos, root + "/README.md", codeMap.get("README.md"));
            put(zos, root + "/pom.xml", codeMap.get("pom.xml"));
            put(zos, root + "/database/schema.sql", codeMap.get("create_table.sql"));
            put(zos, root + "/src/main/resources/application.yml", codeMap.get("application.yml"));

            put(zos, root + "/src/main/java/" + pkgPath + "/Application.java", codeMap.get("Application.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/config/MyBatisConfig.java", codeMap.get("MyBatisConfig.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/config/CorsConfig.java", codeMap.get("CorsConfig.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/common/Result.java", codeMap.get("Result.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/common/PageRequest.java", codeMap.get("PageRequest.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/common/PageResult.java", codeMap.get("PageResult.java"));

            put(zos, root + "/src/main/java/" + pkgPath + "/controller/AuthController.java", codeMap.get("AuthController.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/service/AuthService.java", codeMap.get("AuthService.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/service/impl/AuthServiceImpl.java", codeMap.get("AuthServiceImpl.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/service/CaptchaService.java", codeMap.get("CaptchaService.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/common/LoginRequest.java", codeMap.get("LoginRequest.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/interceptor/LoginInterceptor.java", codeMap.get("LoginInterceptor.java"));
            put(zos, root + "/src/main/java/" + pkgPath + "/config/InterceptorConfig.java", codeMap.get("InterceptorConfig.java"));

            if (backendExtras != null) {
                for (Map.Entry<String, String> e : backendExtras.entrySet()) {
                    put(zos, root + "/" + e.getKey().replace('\\', '/'), e.getValue());
                }
            }

            if (frontendExtras != null) {
                for (Map.Entry<String, String> e : frontendExtras.entrySet()) {
                    put(zos, root + "/" + e.getKey().replace('\\', '/'), e.getValue());
                }
            }

            String fe = root + "/frontend";
            put(zos, fe + "/src/utils/request.js", codeMap.get("request.js"));
            put(zos, fe + "/src/router/routes.js", codeMap.get("routes.js"));
            put(zos, fe + "/src/api/auth.js", codeMap.get("auth.js"));
            put(zos, fe + "/src/components/CaptchaInput.vue", codeMap.get("CaptchaInput.vue"));
            put(zos, fe + "/src/views/Login.vue", firstNonEmpty(codeMap.get("Login.vue"), codeMap.get("login.vue")));
            put(zos, fe + "/.env", codeMap.get(".env"));
            put(zos, fe + "/.env.example", codeMap.get(".env.example"));
            put(zos, fe + "/README.md", frontendReadme());
        }
        return baos.toByteArray();
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        return b;
    }

    private static String frontendReadme() {
        return "# 前端工程\n\n"
                + "## 启动\n\n"
                + "```bash\n"
                + "cd frontend\n"
                + "cp .env.example .env   # 首次\n"
                + "npm install\n"
                + "npm run dev\n"
                + "```\n\n"
                + "浏览器访问 http://localhost:5173 。开发服务器已将 `/api` 代理到后端（默认 http://localhost:8080）。\n\n"
                + "## 说明\n\n"
                + "- 侧栏菜单与路由来自元数据「功能节点」配置（`src/router/routes.js`）。\n"
                + "- 各业务表页面在 `src/views/generated/<组件名>/` 下。\n"
                + "- 请先启动 ZIP 根目录下的 Spring Boot 后端，并执行 `database/schema.sql` 初始化全部业务表。\n";
    }

    private static void put(ZipOutputStream zos, String path, String content) throws IOException {
        if (content == null || content.isEmpty()) {
            return;
        }
        ZipEntry e = new ZipEntry(path.replace('\\', '/'));
        zos.putNextEntry(e);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private static String sanitizeFolderName(String name) {
        if (name == null || name.isEmpty()) {
            return "app";
        }
        String s = name.replaceAll("[^a-zA-Z0-9._-]", "-");
        return s.isEmpty() ? "app" : s;
    }
}
