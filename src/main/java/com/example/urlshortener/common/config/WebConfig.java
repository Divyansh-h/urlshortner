package com.example.urlshortener.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Apply to all endpoints
                .allowedOrigins("http://localhost:5173", "http://127.0.0.1:5173") // Allow Vite frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Allow standard HTTP methods
                .allowedHeaders("*") // Allow all headers
                .allowCredentials(true); // Allow sending cookies/credentials if needed
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/**"); // Apply rate limiting globally across all API and Redirect endpoints
    }
}