package com.metadata.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置类
 * 使用SpringDoc OpenAPI实现Swagger UI
 */
@Configuration
public class SwaggerConfig {

    /**
     * 配置OpenAPI基本信息
     * @return OpenAPI对象
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("通用元数据管理系统API")
                        .description("通用元数据管理系统的RESTful API文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("元数据管理系统开发团队")
                                .email("metadata@example.com")
                                .url("http://localhost:8080/metadata-system")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                        )
                );
    }

    /**
     * 配置API分组
     * @return GroupedOpenApi对象
     */
    @Bean
    public GroupedOpenApi api() {
        return GroupedOpenApi.builder()
                .group("api")
                .pathsToMatch("/api/**")
                .packagesToScan("com.metadata.controller")
                .build();
    }
}
