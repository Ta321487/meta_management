package com.metadata.controller;

import com.metadata.common.AuthExtensionCodeBundle;
import com.metadata.common.BusinessSystemSqlExportPayload;
import com.metadata.common.BusinessSystemTableCodesPayload;
import com.metadata.common.GeneratedCodeBundleMapper;
import com.metadata.common.Result;
import com.metadata.common.TableGeneratedCodeBundle;
import com.metadata.service.CodeGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 代码生成控制器
 */
@RestController
@RequestMapping("/api/codegen")
@Tag(name = "代码生成管理", description = "代码生成相关API")
public class CodeGeneratorController {

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    /**
     * 生成建表SQL
     */
    @GetMapping("/sql/{tableCode}")
    @Operation(summary = "生成SQL语句")
    public Result<String> generateSQL(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String sql = codeGeneratorService.generateCreateTableSQL(tableCode, businessCode);
        return Result.success(sql);

    }

    /**
     * 生成实体类
     */
    @GetMapping("/entity/{tableCode}")
    @Operation(summary = "生成实体类")
    public Result<String> generateEntity(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example.entity") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateEntity(tableCode, packageName, businessCode);
        return Result.success(code);

    }

    /**
     * 生成Controller
     */
    @GetMapping("/controller/{tableCode}")
    @Operation(summary = "生成Controller类")
    public Result<String> generateController(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateController(tableCode, packageName, businessCode);
        return Result.success(code);

    }

    /**
     * 生成Service接口
     */
    @GetMapping("/service-interface/{tableCode}")
    @Operation(summary = "生成Service接口")
    public Result<String> generateServiceInterface(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateServiceInterface(tableCode, packageName, businessCode);
        return Result.success(code);

    }
    
    /**
     * 生成Service实现类
     */
    @GetMapping("/service-impl/{tableCode}")
    @Operation(summary = "生成Service实现类")
    public Result<String> generateServiceImpl(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateServiceImpl(tableCode, packageName, businessCode);
        return Result.success(code);

    }
    
    /**
     * 生成Service
     */
    @GetMapping("/service/{tableCode}")
    @Operation(summary = "生成Service类")
    public Result<String> generateService(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode,
            @Parameter(description = "是否使用接口，true:生成接口+实现类，false:生成传统Service类")
            @RequestParam(defaultValue = "false") boolean useInterface) throws Exception {
        String code = codeGeneratorService.generateService(tableCode, packageName, businessCode, useInterface);
        return Result.success(code);

    }

    /**
     * 生成Mapper接口
     */
    @GetMapping("/mapper/{tableCode}")
    @Operation(summary = "生成Mapper接口")
    public Result<String> generateMapper(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateMapper(tableCode, packageName, businessCode);
        return Result.success(code);

    }

    /**
     * 生成Mapper XML
     */
    @GetMapping("/mapperxml/{tableCode}")
    @Operation(summary = "生成MapperXML")
    public Result<String> generateMapperXml(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateMapperXml(tableCode, packageName, businessCode);
        return Result.success(code);

    }

    /**
     * 生成Vue列表页面
     */
    @GetMapping("/vue/list/{tableCode}")
    @Operation(summary = "生成Vue列表页")
    public Result<String> generateVueList(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateVueList(tableCode, businessCode);
        return Result.success(code);

    }

    /**
     * 生成Vue表单页面
     */
    @GetMapping("/vue/form/{tableCode}")
    @Operation(summary = "生成Vue表单页")
    public Result<String> generateVueForm(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateVueForm(tableCode, businessCode);
        return Result.success(code);

    }
    
    /**
     * 生成登录页
     */
    @GetMapping("/vue/login")
    @Operation(summary = "生成登录页")
    public Result<String> generateLoginPage(
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode,
            @Parameter(description = "是否生成验证码扩展")
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        String code = codeGeneratorService.generateLoginPage(businessCode, captchaEnabled);
        return Result.success(code);

    }

    /**
     * 生成所有代码
     */
    @GetMapping("/all/{tableCode}")
    @Operation(summary = "生成单表全部代码", description = "一次性生成建表 SQL、后端 Java、前端 Vue、路由及配置文件等，返回固定字段结构的代码包")
    public Result<TableGeneratedCodeBundle> generateAll(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode,
            @Parameter(description = "是否使用接口模式生成Service")
            @RequestParam(defaultValue = "false") boolean useInterface,
            @Parameter(description = "是否生成验证码认证扩展包")
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        return Result.success(GeneratedCodeBundleMapper.fromMap(
                codeGeneratorService.generateAll(tableCode, packageName, businessCode, useInterface, captchaEnabled)));

    }

    /**
     * 按业务系统下载完整项目 ZIP（全部启用表 + 可运行前端）
     */
    @GetMapping("/project-zip/business/{businessCode}")
    @Operation(summary = "下载业务系统项目 ZIP", description = "打包该业务系统下全部启用表的 SQL、后端与 Vite 前端，解压后一层项目根目录")
    public ResponseEntity<byte[]> downloadProjectZipByBusiness(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode,
            @Parameter(description = "Java 根包名，为空则使用业务系统配置")
            @RequestParam(required = false) String packageName,
            @RequestParam(defaultValue = "false") boolean useInterface,
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        byte[] body = codeGeneratorService.generateProjectZipByBusinessSystem(
                businessCode, packageName, useInterface, captchaEnabled);
        String safe = businessCode.replaceAll("[^a-zA-Z0-9._-]", "-");
        if (safe.isEmpty()) {
            safe = "app";
        }
        String filename = safe + "-generated.zip";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(body.length)
                .body(body);
    }

    /**
     * 生成路由配置（单独接口）
     */
    @GetMapping("/routes/{tableCode}")
    @Operation(summary = "生成路由配置", description = "生成单表对应的前端 routes.js")
    public Result<String> generateRoutes(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateRoutes(tableCode, businessCode);
        return Result.success(code);

    }
    
    /**
     * 生成业务系统下所有表的整合路由配置
     */
    @GetMapping("/routes/integrated/{businessCode}")
    @Operation(summary = "生成整合路由配置", description = "生成业务系统下所有表的整合 routes.js")
    public Result<String> generateIntegratedRoutes(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode) throws Exception {
        String code = codeGeneratorService.generateIntegratedRoutes(businessCode);
        return Result.success(code);

    }
    
    /**
     * 生成业务系统下所有表的完整代码包
     */
    @GetMapping("/allByBusinessSystem/{businessCode}")
    @Operation(summary = "按业务系统生成全部表代码", description = "为业务系统下所有启用表分别生成完整代码包，按 tables 列表返回")
    public Result<BusinessSystemTableCodesPayload> generateAllByBusinessSystem(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "是否使用接口模式生成Service")
            @RequestParam(defaultValue = "false") boolean useInterface) throws Exception {
        return Result.success(GeneratedCodeBundleMapper.tableCodesFromMap(
                codeGeneratorService.generateAllByBusinessSystem(businessCode, packageName, useInterface)));

    }
    
    /**
     * 生成业务系统下所有表的建表SQL
     */
    @GetMapping("/sqlByBusinessSystem/{businessCode}")
    @Operation(summary = "按业务系统生成全部建表 SQL", description = "为业务系统下所有启用表生成建表语句，按 sqlFiles 列表返回")
    public Result<BusinessSystemSqlExportPayload> generateAllSQLByBusinessSystem(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode) throws Exception {
        return Result.success(GeneratedCodeBundleMapper.sqlExportFromMap(
                codeGeneratorService.generateAllSQLByBusinessSystem(businessCode)));

    }

    /**
     * 生成Result统一响应结果类
     */
    @GetMapping("/common/result")
    @Operation(summary = "生成Result统一响应结果类")
    public Result<String> generateResult(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.common") String packageName) throws Exception {
        String code = codeGeneratorService.generateResult(packageName);
        return Result.success(code);

    }

    /**
     * 生成PageRequest分页请求类
     */
    @GetMapping("/common/pageRequest")
    @Operation(summary = "生成PageRequest分页请求类")
    public Result<String> generatePageRequest(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.common") String packageName) throws Exception {
        String code = codeGeneratorService.generatePageRequest(packageName);
        return Result.success(code);

    }

    /**
     * 生成PageResult分页结果类
     */
    @GetMapping("/common/pageResult")
    @Operation(summary = "生成PageResult分页结果类")
    public Result<String> generatePageResult(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.common") String packageName) throws Exception {
        String code = codeGeneratorService.generatePageResult(packageName);
        return Result.success(code);

    }
    
    /**
     * 生成API请求文件
     */
    @GetMapping("/api/{tableCode}")
    @Operation(summary = "生成API请求文件")
    public Result<String> generateApi(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) throws Exception {
        String code = codeGeneratorService.generateApi(tableCode, businessCode);
        return Result.success(code);

    }
    
    /**
     * 生成request.js工具类
     */
    @GetMapping("/common/requestJs")
    @Operation(summary = "生成request.js工具类")
    public Result<String> generateRequestJs() throws Exception {
        String code = codeGeneratorService.generateRequestJs();
        return Result.success(code);

    }
    
    /**
     * 生成认证API文件
     */
    @GetMapping("/common/auth")
    @Operation(summary = "生成认证API文件")
    public Result<String> generateAuth(
            @Parameter(description = "是否包含验证码 API")
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        String code = codeGeneratorService.generateAuth(captchaEnabled);
        return Result.success(code);

    }

    /**
     * 生成验证码输入组件
     */
    @GetMapping("/vue/captchaInput")
    @Operation(summary = "生成验证码输入组件")
    public Result<String> generateCaptchaInput() throws Exception {
        return Result.success(codeGeneratorService.generateCaptchaInput());
    }

    /**
     * 生成认证扩展包（后端）
     */
    @GetMapping("/auth-extension")
    @Operation(summary = "生成认证扩展包", description = "生成验证码与 Session 登录相关后端源码（CaptchaService、AuthController 等）")
    public Result<AuthExtensionCodeBundle> generateAuthExtension(
            @RequestParam String packageName,
            @RequestParam(defaultValue = "true") boolean captchaEnabled) throws Exception {
        return Result.success(GeneratedCodeBundleMapper.authFromMap(
                codeGeneratorService.generateAuthExtension(packageName, captchaEnabled)));
    }
    
    /**
     * 生成.env环境配置文件
     */
    @GetMapping("/common/env")
    @Operation(summary = "生成.env环境配置文件")
    public Result<String> generateEnvFile() throws Exception {
        String code = codeGeneratorService.generateEnvFile();
        return Result.success(code);

    }
    
    /**
     * 生成CORS配置类
     */
    @GetMapping("/common/corsConfig")
    @Operation(summary = "生成CORS配置类")
    public Result<String> generateCorsConfig(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.config") String packageName) throws Exception {
        String code = codeGeneratorService.generateCorsConfig(packageName);
        return Result.success(code);

    }

    /**
     * 生成Spring Boot启动类
     */
    @GetMapping("/common/application")
    @Operation(summary = "生成Spring Boot启动类")
    public Result<String> generateApplication(
            @Parameter(description = "包名")
            @RequestParam String packageName) throws Exception {
        String code = codeGeneratorService.generateApplication(packageName);
        return Result.success(code);

    }

    /**
     * 生成application.yml配置文件
     */
    @GetMapping("/common/applicationYml")
    @Operation(summary = "生成application.yml配置文件")
    public Result<String> generateApplicationYml(
            @Parameter(description = "包名")
            @RequestParam String packageName,
            @Parameter(description = "是否包含验证码配置")
            @RequestParam(defaultValue = "false") boolean captchaEnabled) throws Exception {
        String code = codeGeneratorService.generateApplicationConfig(packageName, captchaEnabled);
        return Result.success(code);

    }
    
    /**
     * 生成MyBatis配置类
     */
    @GetMapping("/common/mybatisConfig")
    @Operation(summary = "生成MyBatis配置类")
    public Result<String> generateMyBatisConfig(
            @Parameter(description = "包名")
            @RequestParam String packageName) throws Exception {
        String code = codeGeneratorService.generateMyBatisConfig(packageName);
        return Result.success(code);

    }
    
    /**
     * 生成pom.xml配置文件
     */
    @GetMapping("/common/pomXml")
    @Operation(summary = "生成pom.xml配置文件")
    public Result<String> generatePomXml(
            @Parameter(description = "包名")
            @RequestParam String packageName) throws Exception {
        String groupId = packageName;
        String artifactId = packageName.substring(packageName.lastIndexOf(".") + 1);
        String name = artifactId.substring(0, 1).toUpperCase() + artifactId.substring(1);
        String description = name;
        String code = codeGeneratorService.generatePomXml(groupId, artifactId, name, description);
        return Result.success(code);

    }
    
}

