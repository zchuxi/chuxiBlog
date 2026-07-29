package com.chuxi.config;

import com.chuxi.auth.AdminAuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    public CorsConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String allowedOrigins = System.getenv().getOrDefault("APP_CORS_ALLOWED_ORIGINS", "http://localhost:5173,http://localhost:3000");
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理端接口鉴权
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/api/admin/**");
    }
}
