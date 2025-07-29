package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.common.DeleteResponseDto;
import com.acainfo.mvp.dto.common.PageResponseDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador administrativo para gestión de profesores.
 * Todos los endpoints requieren rol ADMIN.
 *
 * @author Academic Info System
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/teachers")
@RequiredArgsConstructor
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Teacher Administration", description = "Endpoints administrativos para gestión de profesores")
public class TeacherAdminController {

    private final TeacherService teacherService;

    /**
     * Obtiene todos los profesores sin paginación.
     * Útil para llenar combos o listas pequeñas.
     *
     * @return Lista de todos los profesores
     */
    @GetMapping("/all")
    @Operation(
            summary = "Listar todos los profesores",
            description = "Obtiene todos los profesores del sistema sin paginación. " +
                    "Útil para llenar listas desplegables o combos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de profesores obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            )
    })
    public ResponseEntity<List<TeacherDto>> getAllTeachers() {
        log.debug("Admin consultando lista completa de profesores");

        List<TeacherDto> teachers = teacherService.getAllTeachers();

        return ResponseEntity.ok(teachers);
    }

    /**
     * Obtiene un profesor específico por ID.
     *
     * @param id ID del profesor
     * @return Datos del profesor
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener profesor por ID",
            description = "Obtiene la información completa de un profesor específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profesor encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profesor no encontrado"
            )
    })
    public ResponseEntity<TeacherDto> getTeacherById(
            @Parameter(description = "ID del profesor", example = "1")
            @PathVariable Long id) {

        log.debug("Admin consultando profesor ID: {}", id);

        TeacherDto teacher = teacherService.getTeacherById(id);

        return ResponseEntity.ok(teacher);
    }

    /**
     * Crea un nuevo profesor (Alta de profesor).
     * El email debe ser único. Los profesores NO pueden auto-registrarse.
     *
     * @param createDto Datos del nuevo profesor
     * @return Profesor creado
     */
    @PostMapping
    @Operation(
            summary = "Alta de profesor",
            description = "Crea un nuevo profesor en el sistema. " +
                    "El email debe ser único. Los profesores NO pueden auto-registrarse, " +
                    "solo el administrador puede crearlos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Profesor creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El email ya está registrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<TeacherDto> createTeacher(
            @Valid @RequestBody CreateTeacherDto createDto) {

        log.info("Admin creando nuevo profesor: {}", createDto.getEmail());

        TeacherDto created = teacherService.createTeacher(createDto);

        log.info("Profesor creado exitosamente con ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza un profesor existente.
     *
     * @param id ID del profesor a actualizar
     * @param updateDto Datos a actualizar
     * @return Profesor actualizado
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar profesor",
            description = "Actualiza los datos de un profesor existente. " +
                    "Solo actualiza los campos enviados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profesor actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profesor no encontrado"
            )
    })
    public ResponseEntity<TeacherDto> updateTeacher(
            @Parameter(description = "ID del profesor", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody TeacherDto updateDto) {

        log.info("Admin actualizando profesor ID: {}", id);

        TeacherDto updated = teacherService.updateTeacher(id, updateDto);

        log.info("Profesor ID: {} actualizado exitosamente", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina un profesor (Baja de profesor).
     * Solo posible si no tiene grupos activos asignados.
     *
     * @param id ID del profesor a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Baja de profesor",
            description = "Elimina un profesor del sistema. " +
                    "No se puede eliminar si tiene grupos activos asignados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profesor eliminado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeleteResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No se puede eliminar (tiene grupos activos)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profesor no encontrado"
            )
    })
    public ResponseEntity<DeleteResponseDto> deleteTeacher(
            @Parameter(description = "ID del profesor", example = "1")
            @PathVariable Long id) {

        log.info("Admin eliminando profesor ID: {}", id);

        teacherService.deleteTeacher(id);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(id)
                .entityType("Teacher")
                .success(true)
                .message("Profesor eliminado exitosamente")
                .build();

        log.info("Profesor ID: {} eliminado exitosamente", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si un profesor puede ser eliminado.
     * Un profesor NO puede eliminarse si tiene grupos ACTIVOS asignados.
     *
     * @param id ID del profesor
     * @return true si puede eliminarse, false si tiene restricciones
     */
    @GetMapping("/{id}/can-delete")
    @Operation(
            summary = "Verificar eliminación de profesor",
            description = "Verifica si un profesor puede ser eliminado. " +
                    "No se puede eliminar si tiene grupos activos asignados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Verificación completada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profesor no encontrado"
            )
    })
    public ResponseEntity<Map<String, Boolean>> canDeleteTeacher(
            @Parameter(description = "ID del profesor", example = "1")
            @PathVariable Long id) {

        log.debug("Verificando si se puede eliminar profesor ID: {}", id);

        boolean canDelete = teacherService.canDeleteTeacher(id);

        return ResponseEntity.ok(Map.of("canDelete", canDelete));
    }

    /**
     * Obtiene profesores disponibles para un horario específico.
     * Útil para asignar profesores a nuevos grupos sin conflictos de horario.
     *
     * @param dayOfWeek Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @return Lista de profesores disponibles
     */
    @GetMapping("/available")
    @Operation(
            summary = "Buscar profesores disponibles",
            description = "Obtiene profesores que no tienen conflictos de horario " +
                    "en el día y horas especificados. Útil para asignar profesores a nuevos grupos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de profesores disponibles",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TeacherDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Parámetros inválidos (día o horario)"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            )
    })
    public ResponseEntity<List<TeacherDto>> getAvailableTeachers(
            @Parameter(description = "Día de la semana", example = "MONDAY")
            @RequestParam String dayOfWeek,
            @Parameter(description = "Hora de inicio", example = "10:00")
            @RequestParam LocalTime startTime,
            @Parameter(description = "Hora de fin", example = "12:00")
            @RequestParam LocalTime endTime) {

        log.debug("Buscando profesores disponibles el {} de {} a {}", dayOfWeek, startTime, endTime);

        List<TeacherDto> available = teacherService.getAvailableTeachers(dayOfWeek, startTime, endTime);

        log.info("Encontrados {} profesores disponibles", available.size());
        return ResponseEntity.ok(available);
    }

    /**
     * Obtiene estadísticas administrativas de profesores.
     * Incluye información agregada útil para el dashboard administrativo.
     *
     * @return Estadísticas de profesores
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Obtener estadísticas de profesores",
            description = "Obtiene estadísticas agregadas sobre los profesores: " +
                    "total de profesores, profesores con/sin grupos, promedio de estudiantes, etc."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas obtenidas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Object.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            )
    })
    public ResponseEntity<Object> getTeacherStats() {
        log.debug("Admin consultando estadísticas de profesores");

        var stats = teacherService.getAdminTeacherStats();

        return ResponseEntity.ok(stats);
    }

    /**
     * Verifica si un email ya está registrado.
     * Útil para validación en tiempo real en formularios.
     *
     * @param email Email a verificar
     * @return true si el email existe, false si está disponible
     */
    @GetMapping("/check-email")
    @Operation(
            summary = "Verificar disponibilidad de email",
            description = "Verifica si un email ya está registrado por otro profesor o estudiante. " +
                    "Útil para validación en tiempo real durante el registro."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Verificación completada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Map.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "No tiene permisos de administrador"
            )
    })
    public ResponseEntity<Map<String, Boolean>> checkTeacherEmail(
            @Parameter(description = "Email a verificar", example = "profesor@academia.com")
            @RequestParam String email) {

        log.debug("Verificando disponibilidad del email: {}", email);

        boolean exists = teacherService.emailExists(email);

        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ========== DTOs INTERNOS PARA DOCUMENTACIÓN ==========

    /**
     * DTO interno para respuestas de error.
     */
    @Schema(description = "Respuesta de error")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "TEACHER_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "No se puede eliminar el profesor porque tiene grupos activos")
        private String message;

        @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00Z")
        private String timestamp;
    }
}