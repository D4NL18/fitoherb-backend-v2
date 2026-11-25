package com.fitoherb.fitoherb_backend_v2.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${local.storage.path}")
    private String storagePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + storagePath + "/");
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:4100",
                        "http://localhost:4200",
                        "http://localhost:4201",
                        "http://localhost:8080",
                        "http://localhost:8000",
                        "http://127.0.0.1:4200",
                        "http://127.0.0.1:8080",
                        "http://127.0.0.1:8000",
                        "https://fitoherb.web.app",
                        "https://fitoherb.firebaseapp.com",
                        "https://fitoherb.com.br",
                        "https://www.fitoherb.com.br",
                        "https://fitoherb-frontend-prod-973619276831.us-central1.run.app",
                        "https://fitoherb-frontend-qa-973619276831.us-central1.run.app",
                        "https://qa.fitoherb.com.br"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}