package com.metadata;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 通用元数据管理系统启动类
 */
@SpringBootApplication
@MapperScan("com.metadata.mapper")
public class MetadataSystemApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(MetadataSystemApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MetadataSystemApplication.class, args);
    }
}

