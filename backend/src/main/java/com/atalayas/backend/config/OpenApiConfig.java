package com.atalayas.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura SpringDoc/OpenAPI 3.
 *
 * Esquemas de seguridad definidos:
 *   - bearerAuth  → JWT en cabecera Authorization: Bearer <token>  (usado por todos los controllers)
 *   - cookieAuth  → Cookie HttpOnly "accessToken"                   (mecanismo real de la app)
 *
 * Ambos esquemas se aplican globalmente; cada endpoint puede declarar el que usa
 * con @SecurityRequirement. Los controllers ya usan "bearerAuth" — ahora está definido.
 *
 * Servidores:
 *   El servidor activo se lee de la variable de entorno SERVER_URL.
 *   En Railway se configura SERVER_URL=https://atalayas-backend-production-4777.up.railway.app
 *   En local no hace falta configurarla (default: http://localhost:8080).
 */
@Configuration
public class OpenApiConfig {

    // Nombres de los esquemas — deben coincidir exactamente con los @SecurityRequirement de los controllers
    private static final String BEARER_SCHEME = "bearerAuth";
    private static final String COOKIE_SCHEME  = "cookieAuth";

    /** URL pública del servidor. Configurable via variable de entorno SERVER_URL. */
    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()

                // ── Metadatos de la API ──────────────────────────────────────
                .info(new Info()
                        .title("Atalayas API")
                        .version("1.0")
                        .description("""
                                API REST de la plataforma de formación y gestión empresarial **EGM Atalayas**.

                                ## Autenticación
                                La app usa **dos mecanismos complementarios** con el mismo JWT:
                                - **Cookie HttpOnly** `accessToken` — el navegador la envía automáticamente \
                                en cada petición al mismo origen. Es el mecanismo principal en producción.
                                - **Bearer token** en cabecera `Authorization: Bearer <token>` — útil para \
                                clientes que no pueden gestionar cookies (apps móviles, Postman, "Try it out" \
                                de Swagger en entornos cross-origin).

                                ## Cómo autenticarse en Swagger
                                1. Ejecuta `POST /api/v1/auth/login` con tus credenciales.
                                2. Copia el campo `accessToken` de la respuesta.
                                3. Pulsa el botón **Authorize 🔒** y pega el token en el campo **bearerAuth**.
                                4. Todas las peticiones subsiguientes incluirán la cabecera `Authorization`.

                                ## Roles
                                | Rol | Descripción |
                                |---|---|
                                | `ROLE_ADMIN` | Superadmin EGM — acceso total |
                                | `ROLE_ADMIN_EMPRESA` | Admin de empresa — scope limitado a su empresa |
                                | `ROLE_EMPLEADO` | Empleado — lectura/consumo dentro de su empresa |

                                ## Errores
                                Todos los errores tienen estructura uniforme:
                                ```json
                                {
                                  "timestamp": "2026-04-02T10:00:00",
                                  "status": 400,
                                  "error": "Bad Request",
                                  "message": "Descripción del error"
                                }
                                ```
                                """)
                        .contact(new Contact()
                                .name("EGM Atalayas — Soporte técnico")
                                .email("soporte@atalayas.com")))

                // ── Servidores ───────────────────────────────────────────────
                .addServersItem(new Server()
                        .url(serverUrl)
                        .description("Servidor activo"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Desarrollo local"))

                // ── Componentes reutilizables ────────────────────────────────
                .components(new Components()

                        // ── Esquemas de seguridad ────────────────────────────
                        .addSecuritySchemes(BEARER_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                JWT obtenido de `POST /api/v1/auth/login` (campo `accessToken`).
                                                Se envía en la cabecera: `Authorization: Bearer <token>`
                                                Expiración: **1 hora**. Renovar con `POST /api/v1/auth/refresh-token`.
                                                """))

                        .addSecuritySchemes(COOKIE_SCHEME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.COOKIE)
                                        .name("accessToken")
                                        .description("""
                                                Cookie HttpOnly `accessToken` enviada automáticamente por el navegador.
                                                Se establece al hacer login. El navegador la gestiona de forma transparente
                                                si el frontend y la API están en el mismo origen.
                                                """))

                        // ── Esquemas de respuesta de error ───────────────────
                        .addSchemas("ErrorResponse",
                                new Schema<>()
                                        .type("object")
                                        .description("Estructura de error estándar de la plataforma")
                                        .addProperty("timestamp", new StringSchema()
                                                .format("date-time")
                                                .example("2026-04-02T10:00:00"))
                                        .addProperty("status", new IntegerSchema()
                                                .example(400))
                                        .addProperty("error", new StringSchema()
                                                .example("Bad Request"))
                                        .addProperty("message", new StringSchema()
                                                .example("Descripción del error")))

                        .addSchemas("ValidationErrorResponse",
                                new Schema<>()
                                        .type("object")
                                        .description("Error de validación de campos — devuelve un mapa campo→mensaje")
                                        .addProperty("timestamp", new StringSchema()
                                                .format("date-time")
                                                .example("2026-04-02T10:00:00"))
                                        .addProperty("status", new IntegerSchema()
                                                .example(400))
                                        .addProperty("error", new StringSchema()
                                                .example("Validación fallida"))
                                        .addProperty("fieldErrors", new Schema<>()
                                                .type("object")
                                                .example("{\"email\": \"no tiene un formato válido\", \"nombre\": \"es obligatorio\"}")))

                        // ── Respuestas de error reutilizables ────────────────
                        .addResponses("400", errorResponse("400 Bad Request",
                                "Regla de negocio violada o validación fallida", "#/components/schemas/ErrorResponse"))
                        .addResponses("401", errorResponse("401 Unauthorized",
                                "Sin sesión activa o token inválido/expirado", "#/components/schemas/ErrorResponse"))
                        .addResponses("403", errorResponse("403 Forbidden",
                                "Sin permisos para este recurso (rol insuficiente o recurso de otra empresa)", "#/components/schemas/ErrorResponse"))
                        .addResponses("404", errorResponse("404 Not Found",
                                "Recurso no encontrado en base de datos", "#/components/schemas/ErrorResponse")))

                // ── Seguridad global (aplica a todos los endpoints salvo los públicos) ──
                .addSecurityItem(new SecurityRequirement()
                        .addList(BEARER_SCHEME)
                        .addList(COOKIE_SCHEME));
    }

    /** Construye una ApiResponse de error referenciando el schema ErrorResponse. */
    private ApiResponse errorResponse(String description, String detail, String schemaRef) {
        return new ApiResponse()
                .description(description + " — " + detail)
                .content(new Content()
                        .addMediaType("application/json",
                                new MediaType().schema(new Schema<>().$ref(schemaRef))));
    }
}

