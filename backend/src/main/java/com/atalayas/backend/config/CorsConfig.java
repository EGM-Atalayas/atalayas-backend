package com.atalayas.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {

    // Cubre cualquier puerto de localhost en desarrollo
    private static final List<String> LOCAL_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:4200",
            "http://localhost:8080",
            "http://localhost:10000",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:4200",
            "http://127.0.0.1:8080",
            "http://127.0.0.1:10000"
    );

    // Orígenes adicionales de producción (ej: URL de Vercel/Render)
    @Value("${CORS_ALLOWED_ORIGINS:}")
    private String corsAllowedOriginsEnv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = new ArrayList<>(LOCAL_ORIGINS);

        // Añadir orígenes de producción desde env var, normalizando el protocolo
        if (!corsAllowedOriginsEnv.isBlank()) {
            Arrays.stream(corsAllowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(s -> (s.startsWith("http://") || s.startsWith("https://")) ? s : "https://" + s)
                    .filter(s -> !origins.contains(s))
                    .forEach(origins::add);
        }

        log.info("CORS — orígenes permitidos: {}", origins);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
