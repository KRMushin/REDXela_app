package com.redxela.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Política de acceso entre dominios para el frontend de desarrollo.
 * 
 * Concepto de SO: Control de acceso entre espacios de usuario/orígenes (CORS).
 * Permite que la interfaz Vite (localhost:5173 o el contenedor frontend) consuma
 * la API y el stream SSE sin bloquear las peticiones por política de mismo origen.
 */
@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // Cubre Vite en cualquier puerto local y el servicio de Compose
                        .allowedOriginPatterns(
                                "http://localhost:*",
                                "http://127.0.0.1:*",
                                "http://frontend:*"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Content-Type")
                        .maxAge(3600);
            }
        };
    }
}
