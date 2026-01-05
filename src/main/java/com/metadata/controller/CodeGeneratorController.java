package com.metadata.controller;

import com.metadata.common.Result;
import com.metadata.service.CodeGeneratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
            @RequestParam(required = false) String businessCode) {
        try {
            String sql = codeGeneratorService.generateCreateTableSQL(tableCode, businessCode);
            return Result.success(sql);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateEntity(tableCode, packageName, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateController(tableCode, packageName, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateServiceInterface(tableCode, packageName, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateServiceImpl(tableCode, packageName, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(defaultValue = "false") boolean useInterface) {
        try {
            String code = codeGeneratorService.generateService(tableCode, packageName, businessCode, useInterface);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateMapper(tableCode, packageName, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateMapperXml(tableCode, packageName, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateVueList(tableCode, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateVueForm(tableCode, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成登录页
     */
    @GetMapping("/vue/login")
    @Operation(summary = "生成登录页")
    public Result<String> generateLoginPage(
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateLoginPage(businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成所有代码
     */
    @GetMapping("/all/{tableCode}")
    @Operation(description = "一次性生成SQL、Controller、Service、Mapper、MapperXML、Vue列表、Vue表单")
    public Result<Map<String, String>> generateAll(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @RequestParam String packageName,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode,
            @Parameter(description = "是否使用接口模式生成Service")
            @RequestParam(defaultValue = "false") boolean useInterface) {
        try {
            Map<String, String> codeMap = codeGeneratorService.generateAll(tableCode, packageName, businessCode, useInterface);
            return Result.success(codeMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成路由配置（单独接口）
     */
    @GetMapping("/routes/{tableCode}")
    @Operation(description = "生成路由配置")
    public Result<String> generateRoutes(
            @Parameter(description = "表编码")
            @PathVariable String tableCode,
            @Parameter(description = "业务系统编码")
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateRoutes(tableCode, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成业务系统下所有表的整合路由配置
     */
    @GetMapping("/routes/integrated/{businessCode}")
    @Operation(description = "生成业务系统下所有表的整合路由配置")
    public Result<String> generateIntegratedRoutes(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode) {
        try {
            String code = codeGeneratorService.generateIntegratedRoutes(businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成业务系统下所有表的完整代码包
     */
    @GetMapping("/allByBusinessSystem/{businessCode}")
    @Operation(description = "生成业务系统下所有表的代码")
    public Result<Map<String, Map<String, String>>> generateAllByBusinessSystem(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode,
            @RequestParam(defaultValue = "com.example") String packageName,
            @Parameter(description = "是否使用接口模式生成Service")
            @RequestParam(defaultValue = "false") boolean useInterface) {
        try {
            Map<String, Map<String, String>> codeMap = codeGeneratorService.generateAllByBusinessSystem(businessCode, packageName, useInterface);
            return Result.success(codeMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成业务系统下所有表的建表SQL
     */
    @GetMapping("/sqlByBusinessSystem/{businessCode}")
    @Operation(description = "生成业务系统下所有表的SQL")
    public Result<Map<String, String>> generateAllSQLByBusinessSystem(
            @Parameter(description = "业务系统编码")
            @PathVariable String businessCode) {
        try {
            Map<String, String> sqlMap = codeGeneratorService.generateAllSQLByBusinessSystem(businessCode);
            return Result.success(sqlMap);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Result统一响应结果类
     */
    @GetMapping("/common/result")
    @Operation(summary = "生成Result统一响应结果类")
    public Result<String> generateResult(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.common") String packageName) {
        try {
            String code = codeGeneratorService.generateResult(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成PageRequest分页请求类
     */
    @GetMapping("/common/pageRequest")
    @Operation(summary = "生成PageRequest分页请求类")
    public Result<String> generatePageRequest(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.common") String packageName) {
        try {
            String code = codeGeneratorService.generatePageRequest(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成PageResult分页结果类
     */
    @GetMapping("/common/pageResult")
    @Operation(summary = "生成PageResult分页结果类")
    public Result<String> generatePageResult(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.common") String packageName) {
        try {
            String code = codeGeneratorService.generatePageResult(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) String businessCode) {
        try {
            String code = codeGeneratorService.generateApi(tableCode, businessCode);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成request.js工具类
     */
    @GetMapping("/common/requestJs")
    @Operation(summary = "生成request.js工具类")
    public Result<String> generateRequestJs() {
        try {
            String code = codeGeneratorService.generateRequestJs();
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成认证API文件
     */
    @GetMapping("/common/auth")
    @Operation(summary = "生成认证API文件")
    public Result<String> generateAuth() {
        try {
            String code = codeGeneratorService.generateAuth();
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成.env环境配置文件
     */
    @GetMapping("/common/env")
    @Operation(summary = "生成.env环境配置文件")
    public Result<String> generateEnvFile() {
        try {
            String code = codeGeneratorService.generateEnvFile();
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成CORS配置类
     */
    @GetMapping("/common/corsConfig")
    @Operation(summary = "生成CORS配置类")
    public Result<String> generateCorsConfig(
            @Parameter(description = "包名")
            @RequestParam(defaultValue = "com.example.config") String packageName) {
        try {
            String code = codeGeneratorService.generateCorsConfig(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成Spring Boot启动类
     */
    @GetMapping("/common/application")
    @Operation(summary = "生成Spring Boot启动类")
    public Result<String> generateApplication(
            @Parameter(description = "包名")
            @RequestParam String packageName) {
        try {
            String code = codeGeneratorService.generateApplication(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 生成application.yml配置文件
     */
    @GetMapping("/common/applicationYml")
    @Operation(summary = "生成application.yml配置文件")
    public Result<String> generateApplicationYml(
            @Parameter(description = "包名")
            @RequestParam String packageName) {
        try {
            String code = codeGeneratorService.generateApplicationConfig(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成MyBatis配置类
     */
    @GetMapping("/common/mybatisConfig")
    @Operation(summary = "生成MyBatis配置类")
    public Result<String> generateMyBatisConfig(
            @Parameter(description = "包名")
            @RequestParam String packageName) {
        try {
            String code = codeGeneratorService.generateMyBatisConfig(packageName);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 生成pom.xml配置文件
     */
    @GetMapping("/common/pomXml")
    @Operation(summary = "生成pom.xml配置文件")
    public Result<String> generatePomXml(
            @Parameter(description = "包名")
            @RequestParam String packageName) {
        try {
            String groupId = packageName;
            String artifactId = packageName.substring(packageName.lastIndexOf(".") + 1);
            String name = artifactId.substring(0, 1).toUpperCase() + artifactId.substring(1);
            String description = name;
            String code = codeGeneratorService.generatePomXml(groupId, artifactId, name, description);
            return Result.success(code);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
}

