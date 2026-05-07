package com.ops.twin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目运行的当前目录，拼上 uploads
        String uploadPath = new File("uploads").getAbsolutePath() + File.separator;
        
        // 核心：把前端请求的 /uploads/** 映射到本地真实的文件夹
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
