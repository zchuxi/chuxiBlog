package com.chuxi.config;

import com.chuxi.auth.AdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;

    /** 允许的跨域来源（逗号分隔）。环境变量 APP_CORS_ALLOWED_ORIGINS 经 Spring relaxed binding 映射到同一键，缺省 fail-closed 仅放行本地开发源 */
    @Value("${app.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    public CorsConfig(AdminAuthInterceptor adminAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String allowedOrigins = this.allowedOrigins;
        // 逐项 trim 并过滤空串：配置写成 "http://a.com, http://b.com" 时带空格的原样 pattern 会永远匹配不上
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (origins.isEmpty()) {
            origins = List.of("http://localhost:5173");
        }
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 管理端接口鉴权
        registry.addInterceptor(adminAuthInterceptor).addPathPatterns("/api/admin/**");
    }
}
