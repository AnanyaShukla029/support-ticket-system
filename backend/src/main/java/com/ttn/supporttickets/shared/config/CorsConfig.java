package com.ttn.supporttickets.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows the Next.js UI origin to call the REST API.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            @Value("${app.cors.allowed-origins}") String allowedOrigins
    ) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(allowedOrigins.split(","))
                        .allowedMethods("GET", "POST", "PATCH", "OPTIONS")
                        .allowedHeaders("Content-Type", "X-Correlation-Id")
                        .exposedHeaders("Location", "X-Correlation-Id");
            }
        };
    }
}
