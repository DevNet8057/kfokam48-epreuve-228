package com.kfokam.k48.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Autorise le frontend (origine distincte en développement/Docker) à appeler l'API.
 * Sans ceci, le navigateur bloque les requêtes avant même qu'elles atteignent les contrôleurs.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${FRONTEND_ORIGIN:http://localhost:5173}")
    private String frontendOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("*");
    }
}
