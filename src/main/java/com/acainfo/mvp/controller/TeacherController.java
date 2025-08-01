package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.coursegroup.CourseGroupDto;
import com.acainfo.mvp.dto.groupsession.GroupSessionDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherScheduleDto;
import com.acainfo.mvp.service.TeacherService;
import com.acainfo.mvp.util.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones de profesores.
 * Maneja el perfil del profesor y consulta de horarios.
 *
 * Iteración 1: Funcionalidad básica
 * - Autenticación básica
 * - Consulta básica del horario semanal asignado
 *
 * Todos los endpoints requieren autenticación con rol TEACHER.
 * La autenticación se maneja mediante sesiones HTTP (cookie JSESSIONID).
 */
@Slf4j
@RestController
@RequestMapping("/api/teachers")
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Teachers", description = "Endpoints para gestión de profesores (Iteración 1: Funcionalidad básica)")
public class TeacherController {

    private final TeacherService teacherService;
    private final SessionUtils sessionUtils;

    public TeacherController(TeacherService teacherService, SessionUtils sessionUtils) {
        this.teacherService = teacherService;
        this.sessionUtils = sessionUtils;
    }

    // ========== ENDPOINTS DE PERFIL (BÁSICO) ==========

    /**
     * Obtiene el perfil del profesor actual.
     * Solo puede acceder a su propio perfil.
     *
     * @return Información del perfil del profesor
     */
    @GetMapping("/profile")
    @Operation(
            summary = "Obtener mi perfil",
            description = "Devuelve la información básica del perfil del profesor autenticado. " +
                    "Solo incluye información pública (sin contraseña)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil obtenido exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos (no es profesor)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<TeacherDto> getMyProfile() {
        log.debug("Solicitud de perfil del profesor actual");
        TeacherDto profile = teacherService.getMyProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * Actualiza el perfil del profesor.
     * En la iteración 1, solo puede actualizar su nombre.
     *
     * @param updateDto Datos a actualizar
     * @return Perfil actualizado
     */
    @PutMapping("/profile")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza la información básica del perfil del profesor. " +
                    "En la iteración 1, solo se puede actualizar el nombre. " +
                    "El email no se puede cambiar por seguridad."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ValidationErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos"
            )
    })
    public ResponseEntity<TeacherDto> updateProfile(
            @Valid @RequestBody TeacherDto updateDto) {

        log.info("Actualizando perfil del profesor ID: {}", sessionUtils.getCurrentUserId());
        TeacherDto updatedProfile = teacherService.updateProfile(
                sessionUtils.getCurrentUserId(), updateDto);

        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Cambia la contraseña del profesor.
     * Requiere la contraseña actual para validación.
     *
     * @param changePasswordDto Datos para cambio de contraseña
     * @return Confirmación del cambio
     */
    @PostMapping("/change-password")
    @Operation(
            summary = "Cambiar contraseña",
            description = "Cambia la contraseña del profesor. " +
                    "Requiere la contraseña actual y la nueva contraseña debe cumplir " +
                    "los requisitos de seguridad (mínimo 8 caracteres)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Contraseña cambiada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Contraseña actual incorrecta o nueva contraseña inválida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<ApiResponseDto<Void>> changePassword(
            @Valid @RequestBody ChangePasswordDto changePasswordDto) {

        log.info("Cambio de contraseña solicitado para profesor ID: {}",
                sessionUtils.getCurrentUserId());

        ApiResponseDto<Void> response = teacherService.changePassword(
                sessionUtils.getCurrentUserId(), changePasswordDto);

        return ResponseEntity.ok(response);
    }

    // ========== ENDPOINTS DE HORARIO (FUNCIONALIDAD PRINCIPAL ITERACIÓN 1) ==========

    /**
     * Obtiene el horario semanal completo del profesor.
     * Endpoint principal de la iteración 1 para profesores.
     *
     * @return Horario semanal con todas las sesiones
     */
    @GetMapping("/schedule")
    @Operation(
            summary = "Obtener mi horario semanal",
            description = "Devuelve el horario semanal completo del profesor autenticado. " +
                    "Incluye todas las sesiones de todos los grupos asignados, " +
                    "organizadas por día de la semana. " +
                    "Este es el endpoint principal para profesores en la iteración 1."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Horario obtenido exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherScheduleDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos"
            )
    })
    public ResponseEntity<TeacherScheduleDto> getMySchedule() {
        log.debug("Obteniendo horario semanal del profesor");
        TeacherScheduleDto schedule = teacherService.getMySchedule();
        return ResponseEntity.ok(schedule);
    }

    /**
     * Obtiene el horario de un día específico.
     * Útil para vista diaria del profesor.
     *
     * @param dayOfWeek Día de la semana (MONDAY, TUESDAY, etc.)
     * @return Lista de sesiones del día especificado
     */
    @GetMapping("/schedule/{dayOfWeek}")
    @Operation(
            summary = "Obtener horario por día",
            description = "Devuelve las sesiones del profesor para un día específico de la semana. " +
                    "El día debe especificarse en inglés y mayúsculas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Horario del día obtenido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupSessionDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Día de la semana inválido",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<List<GroupSessionDto>> getScheduleByDay(
            @Parameter(
                    description = "Día de la semana en inglés",
                    example = "MONDAY",
                    schema = @Schema(
                            allowableValues = {"MONDAY", "TUESDAY", "WEDNESDAY",
                                    "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"}
                    )
            )
            @PathVariable String dayOfWeek) {

        log.debug("Obteniendo horario del {} para el profesor", dayOfWeek);
        List<GroupSessionDto> sessions = teacherService.getMyScheduleByDay(dayOfWeek);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtiene los grupos asignados al profesor.
     * Vista resumida de todos los grupos donde imparte clases.
     *
     * @return Lista de grupos del profesor
     */
    @GetMapping("/groups")
    @Operation(
            summary = "Obtener mis grupos",
            description = "Devuelve la lista de grupos donde el profesor imparte clases. " +
                    "Incluye información básica de cada grupo: asignatura, tipo, estado " +
                    "y número de estudiantes inscritos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de grupos obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<List<CourseGroupDto>> getMyGroups() {
        log.debug("Obteniendo grupos del profesor");
        List<CourseGroupDto> groups = teacherService.getMyGroups();
        return ResponseEntity.ok(groups);
    }

    /**
     * Obtiene estadísticas básicas del profesor.
     * Información resumida sobre grupos y estudiantes.
     *
     * @return Estadísticas del profesor
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Obtener mis estadísticas",
            description = "Devuelve estadísticas básicas del profesor: " +
                    "número de grupos, total de estudiantes, horas semanales, etc. " +
                    "Funcionalidad adicional para la iteración 1."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas obtenidas",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherStatsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<Object> getMyStats() {
        log.debug("Obteniendo estadísticas del profesor");
        var stats = teacherService.getMyStats();
        return ResponseEntity.ok(stats);
    }

    // ========== DTOs INTERNOS PARA DOCUMENTACIÓN ==========

    /**
     * DTO interno para respuestas de error genéricas.
     */
    @Schema(description = "Respuesta de error genérica")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "TEACHER_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "No tiene permisos para esta operación")
        private String message;

        @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00Z")
        private String timestamp;
    }

    /**
     * DTO interno para errores de validación.
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

    /**
     * DTO interno para respuesta de estadísticas.
     */
    @Schema(description = "Estadísticas del profesor")
    private static class TeacherStatsResponse {
        @Schema(description = "ID del profesor", example = "456")
        private Long teacherId;

        @Schema(description = "Nombre del profesor", example = "Dr. Juan García")
        private String teacherName;

        @Schema(description = "Total de grupos asignados", example = "5")
        private int totalGroups;

        @Schema(description = "Grupos activos", example = "3")
        private int activeGroups;

        @Schema(description = "Grupos planificados", example = "2")
        private int plannedGroups;

        @Schema(description = "Total de estudiantes", example = "75")
        private int totalStudents;

        @Schema(description = "Horas semanales de clase", example = "20")
        private int weeklyHours;

        @Schema(description = "Número de asignaturas únicas", example = "3")
        private int uniqueSubjects;
    }
}