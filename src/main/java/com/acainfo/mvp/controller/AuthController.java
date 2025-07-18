package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.*;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación y gestión de sesiones.
 * Maneja login, logout y registro de usuarios.
 *
 * Este controlador usa autenticación basada en sesiones HTTP en lugar de JWT
 * para simplificar la implementación del MVP.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints para autenticación y gestión de sesiones")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Endpoint para login de usuarios (estudiantes y profesores).
     * Crea una sesión HTTP tras autenticación exitosa.
     *
     * @param loginDto Credenciales del usuario
     * @param session Sesión HTTP (inyectada automáticamente por Spring)
     * @return Información del usuario autenticado
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login de usuario",
            description = "Autentica un estudiante o profesor y crea una sesión HTTP. " +
                    "La sesión se mantiene mediante cookies (JSESSIONID)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login exitoso",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales inválidas",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto loginDto,
            @Parameter(hidden = true) HttpSession session) {

        log.info("Login request received for email: {}", loginDto.getEmail());
        LoginResponseDto response = authenticationService.login(loginDto, session);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para registro de nuevos estudiantes.
     * Solo los estudiantes pueden auto-registrarse.
     * Profesores y administradores deben ser creados por un admin.
     *
     * @param registrationDto Datos de registro del estudiante
     * @param session Sesión HTTP
     * @return Información del estudiante registrado y autenticado
     */
    @PostMapping("/register")
    @Operation(
            summary = "Registro de nuevo estudiante",
            description = "Registra un nuevo estudiante en el sistema. " +
                    "Tras el registro exitoso, el estudiante queda automáticamente autenticado. " +
                    "Los profesores NO pueden auto-registrarse."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Estudiante registrado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El email ya está registrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<LoginResponseDto> registerStudent(
            @Valid @RequestBody StudentRegistrationDto registrationDto,
            @Parameter(hidden = true) HttpSession session) {

        log.info("Student registration request for email: {}", registrationDto.getEmail());
        LoginResponseDto response = authenticationService.registerStudent(registrationDto, session);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint para cerrar sesión.
     * Invalida la sesión HTTP actual y limpia el contexto de seguridad.
     *
     * @param session Sesión HTTP actual
     * @return Confirmación de logout
     */
    @PostMapping("/logout")
    @Operation(
            summary = "Cerrar sesión",
            description = "Invalida la sesión actual del usuario y limpia toda la información de autenticación."
    )
    @SecurityRequirement(name = "sessionAuth")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión cerrada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LogoutResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No hay sesión activa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<LogoutResponseDto> logout(
            @Parameter(hidden = true) HttpSession session) {

        log.info("Logout request received");
        LogoutResponseDto response = authenticationService.logout(session);

        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para obtener información del usuario actual.
     * Verifica si hay una sesión activa y devuelve los datos del usuario.
     *
     * @return Información del usuario autenticado o indicación de no autenticado
     */
    @GetMapping("/me")
    @Operation(
            summary = "Obtener usuario actual",
            description = "Devuelve la información del usuario actualmente autenticado. " +
                    "Si no hay sesión activa, devuelve authenticated=false."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Información del usuario obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CurrentUserDto.class)
                    )
            )
    })
    public ResponseEntity<CurrentUserDto> getCurrentUser() {
        log.debug("Current user request received");
        CurrentUserDto currentUser = authenticationService.getCurrentUser();

        return ResponseEntity.ok(currentUser);
    }

    /**
     * Endpoint para verificar el estado de la sesión.
     * Útil para verificar si la sesión sigue activa sin renovarla.
     *
     * @param session Sesión HTTP actual
     * @return Información de la sesión o null si no hay sesión
     */
    @GetMapping("/session")
    @Operation(
            summary = "Verificar estado de sesión",
            description = "Devuelve información sobre la sesión actual, incluyendo tiempo de creación " +
                    "y último acceso. Útil para implementar timeouts en el frontend."
    )
    @SecurityRequirement(name = "sessionAuth")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Información de sesión obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SessionInfoDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No hay sesión activa",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<SessionInfoDto> getSessionInfo(
            @Parameter(hidden = true) HttpSession session) {

        log.debug("Session info request received");
        SessionInfoDto sessionInfo = authenticationService.getSessionInfo(session);

        if (sessionInfo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(sessionInfo);
    }

    /**
     * DTO interno para respuestas de error genéricas.
     * Se usa cuando no hay validaciones específicas.
     */
    @Schema(description = "Respuesta de error genérica")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "AUTH_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "Credenciales inválidas")
        private String message;

        @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00Z")
        private String timestamp;
    }

    /**
     * DTO interno para errores de validación.
     * Incluye detalles de campos con errores.
     */
    @Schema(description = "Respuesta de error de validación")
    private static class ValidationErrorResponse {
        @Schema(description = "Código de error", example = "VALIDATION_ERROR")
        private String code;

        @Schema(description = "Mensaje general", example = "Error de validación en los datos enviados")
        private String message;

        @Schema(description = "Errores por campo")
        private java.util.Map<String, String> fieldErrors;

        @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00Z")
        private String timestamp;
    }
}