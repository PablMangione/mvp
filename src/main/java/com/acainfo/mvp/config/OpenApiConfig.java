package com.acainfo.mvp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Configuración de OpenAPI/Swagger para la documentación de la API.
 *
 * Configura:
 * - Información general de la API
 * - Esquemas de seguridad (sesiones HTTP)
 * - Respuestas comunes
 * - Ejemplos de uso
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ACAInfo API",
                version = "1.0.0-MVP",
                description = """
            API REST para el sistema de gestión de grupos de estudio ACAInfo.
            
            ## Autenticación
            La API utiliza autenticación basada en sesiones HTTP. Tras un login exitoso,
            se crea una sesión que se mantiene mediante cookies (JSESSIONID).
            
            ## Roles disponibles:
            - **STUDENT**: Estudiantes que pueden inscribirse en grupos
            - **TEACHER**: Profesores que pueden ver sus horarios
            - **ADMIN**: Administradores del sistema (gestión completa)
            
            ## Estado actual: MVP (Iteración 1)
            Esta versión incluye la funcionalidad mínima para:
            - Autenticación básica
            - Gestión de alumnos
            - Consulta de asignaturas y grupos
            - Inscripciones básicas
            """,
                contact = @Contact(
                        name = "Equipo ACAInfo",
                        email = "soporte@acainfo.edu"
                ),
                license = @License(
                        name = "Uso interno",
                        url = "https://acainfo.edu/license"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Servidor de desarrollo"),
                @Server(url = "https://api.acainfo.edu", description = "Servidor de producción")
        }
)
@SecurityScheme(
        name = "sessionAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        description = """
        Autenticación basada en sesiones HTTP.
        La sesión se crea al hacer login y se mantiene mediante cookies.
        No se requiere enviar tokens en headers.
        """
)
public class OpenApiConfig {

    /**
     * Personalización global de OpenAPI.
     * Añade respuestas comunes y ejemplos a todos los endpoints.
     */
    @Bean
    public OpenApiCustomizer customerGlobalHeaderOpenApiCustomizer() {
        return openApi -> {
            // Añadir componentes globales
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }

            // Definir esquemas de error comunes
            openApi.getComponents()
                    .addSchemas("ErrorResponse", createErrorSchema())
                    .addSchemas("ValidationErrorResponse", createValidationErrorSchema())
                    .addSchemas("UnauthorizedResponse", createUnauthorizedSchema());

            // Añadir ejemplos de respuestas
            openApi.getComponents()
                    .addExamples("loginSuccess", createLoginSuccessExample())
                    .addExamples("loginError", createLoginErrorExample())
                    .addExamples("validationError", createValidationErrorExample());
        };
    }

    /**
     * Personalización de respuestas comunes para todos los endpoints.
     */
    @Bean
    public OpenApiCustomizer globalResponseCustomizer() {
        return openApi -> {
            openApi.getPaths().values().forEach(pathItem ->
                    pathItem.readOperations().forEach(operation -> {
                        ApiResponses apiResponses = operation.getResponses();

                        // Añadir respuesta 500 a todos los endpoints
                        apiResponses.addApiResponse("500", new ApiResponse()
                                .description("Error interno del servidor")
                                .content(new Content().addMediaType("application/json",
                                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse"))
                                ))
                        );

                        // Añadir respuesta 401 a endpoints protegidos
                        if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
                            apiResponses.addApiResponse("401", new ApiResponse()
                                    .description("No autenticado o sesión expirada")
                                    .content(new Content().addMediaType("application/json",
                                            new MediaType().schema(new Schema<>().$ref("#/components/schemas/UnauthorizedResponse"))
                                    ))
                            );
                        }
                    })
            );
        };
    }

    private Schema<?> createErrorSchema() {
        return new Schema<>()
                .type("object")
                .required(java.util.List.of("code", "message", "timestamp"))
                .properties(Map.of(
                        "code", new Schema<>().type("string").example("INTERNAL_ERROR"),
                        "message", new Schema<>().type("string").example("Ha ocurrido un error inesperado"),
                        "timestamp", new Schema<>().type("string").format("date-time").example("2024-01-15T10:30:00Z"),
                        "details", new Schema<>().type("string").description("Detalles adicionales del error (opcional)")
                ));
    }

    private Schema<?> createValidationErrorSchema() {
        return new Schema<>()
                .type("object")
                .required(java.util.List.of("code", "message", "fieldErrors", "timestamp"))
                .properties(Map.of(
                        "code", new Schema<>().type("string").example("VALIDATION_ERROR"),
                        "message", new Schema<>().type("string").example("Error de validación en los datos enviados"),
                        "fieldErrors", new Schema<>()
                                .type("object")
                                .additionalProperties(new Schema<>().type("string"))
                                .example(Map.of(
                                        "email", "El email no tiene un formato válido",
                                        "password", "La contraseña debe tener al menos 8 caracteres"
                                )),
                        "timestamp", new Schema<>().type("string").format("date-time").example("2024-01-15T10:30:00Z")
                ));
    }

    private Schema<?> createUnauthorizedSchema() {
        return new Schema<>()
                .type("object")
                .required(java.util.List.of("code", "message", "timestamp"))
                .properties(Map.of(
                        "code", new Schema<>().type("string").example("UNAUTHORIZED"),
                        "message", new Schema<>().type("string").example("No hay sesión activa o ha expirado"),
                        "timestamp", new Schema<>().type("string").format("date-time").example("2024-01-15T10:30:00Z"),
                        "path", new Schema<>().type("string").example("/api/student/enrollments")
                ));
    }

    private Example createLoginSuccessExample() {
        return new Example()
                .summary("Login exitoso de estudiante")
                .description("Respuesta cuando un estudiante se autentica correctamente")
                .value(Map.of(
                        "id", 123,
                        "email", "juan.perez@universidad.edu",
                        "name", "Juan Pérez",
                        "role", "STUDENT",
                        "authenticated", true
                ));
    }

    private Example createLoginErrorExample() {
        return new Example()
                .summary("Error de credenciales")
                .description("Respuesta cuando las credenciales son incorrectas")
                .value(Map.of(
                        "code", "AUTH_001",
                        "message", "Email o contraseña incorrectos",
                        "timestamp", "2024-01-15T10:30:00Z"
                ));
    }

    private Example createValidationErrorExample() {
        return new Example()
                .summary("Error de validación")
                .description("Respuesta cuando los datos enviados no son válidos")
                .value(Map.of(
                        "code", "VALIDATION_ERROR",
                        "message", "Error de validación en los datos enviados",
                        "fieldErrors", Map.of(
                                "email", "El email es requerido",
                                "password", "La contraseña debe tener al menos 8 caracteres",
                                "major", "La carrera es requerida"
                        ),
                        "timestamp", "2024-01-15T10:30:00Z"
                ));
    }
}