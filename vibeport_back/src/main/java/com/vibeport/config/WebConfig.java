package com.vibeport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.image-dir:./data/concert_images/}")
    private String imageDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dirPath = ensureTrailingSlash(imageDir);
        registry.addResourceHandler("/concert_images/**", "/vibeport/concert_images/**")
                .addResourceLocations("file:" + dirPath);
    }

    private String ensureTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "./data/concert_images/";
        }
        return value.endsWith("/") || value.endsWith("\\") ? value : value + "/";
    }
}
