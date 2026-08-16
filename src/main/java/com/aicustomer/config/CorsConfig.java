package com.aicustomer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 跨域配置，允许前端开发服务器访问后端 API。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 配置允许的跨域来源、方法、请求头和凭证。
     * 注意：这里只覆盖 Web HTTP 请求，CORS 对 WebSocket 的处理在握手拦截器中独立处理。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
