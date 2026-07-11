package com.site.toffCo.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class UploadConfig implements WebMvcConfigurer {
    @Value("${app.upload-dir:/app/uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry  registry) {
        registry
                .addResourceHandler("/uploads/**")
                .addResourceLocations(
                        Paths.get(uploadDir).toUri().toString()
                );
    }
}
