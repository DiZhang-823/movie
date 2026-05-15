package com.example.movie.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.fronted.url}")
    private String frontedUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")   // 只对 /api 开头的接口生效
                .allowedOrigins(frontedUrl)  // 允许前端地址
                .allowedMethods("*")   // GET, POST, PUT, DELETE 都允许
                .allowedHeaders("*");  // 允许所有请求头
    }
}