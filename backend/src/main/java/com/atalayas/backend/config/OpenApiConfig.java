package com.atalayas.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura SpringDoc/OpenAPI.
 * Swagger está protegido por Spring Security: se requiere estar autenticado
 * para acceder a /swagger-ui/** y /v3/api-docs/**.
 */
@Configuration
public class OpenApiConfig {

    private static final String COOKIE_SCHEME = "cookieAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Atalayas API")
                        .version("1.0")
                        .description("API REST — autenticación mediante cookie HttpOnly"))
                // Define el esquema de seguridad por cookie
                .components(new Components()
                        .addSecuritySchemes(COOKIE_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("accessToken")
                                        .description("Cookie HttpOnly con el JWT de acceso")))
                // Aplica el esquema a todos los endpoints de la documentación
                .addSecurityItem(new SecurityRequirement().addList(COOKIE_SCHEME));
    }
}

