package com.acainfo.mvp.controller.admin;

import com.acainfo.mvp.dto.common.DeleteResponseDto;
import com.acainfo.mvp.dto.subject.CreateSubjectDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.dto.subject.UpdateSubjectDto;
import com.acainfo.mvp.service.SubjectService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controlador administrativo para gestión de asignaturas.
 * Todos los endpoints requieren rol ADMIN.
 *
 * @author Academic Info System
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/subjects")
@RequiredArgsConstructor
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Subject Administration", description = "Endpoints administrativos para gestión de asignaturas")
public class SubjectAdminController {

    private final SubjectService subjectService;

    /**
     * Obtiene todas las asignaturas del sistema.
     *
     * @return Lista de todas las asignaturas
     */
    @GetMapping
    @Operation(
            summary = "Listar asignaturas",
            description = "Obtiene todas las asignaturas del sistema ordenadas por nombre."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de asignaturas obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
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
    public ResponseEntity<List<SubjectDto>> getAllSubjects() {
        log.debug("Admin consultando todas las asignaturas");

        List<SubjectDto> subjects = subjectService.getAllSubjects();

        log.info("Retornando {} asignaturas", subjects.size());
        return ResponseEntity.ok(subjects);
    }

    /**
     * Obtiene una asignatura específica por ID.
     *
     * @param id ID de la asignatura
     * @return Datos de la asignatura
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener asignatura por ID",
            description = "Obtiene la información completa de una asignatura específica."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Asignatura encontrada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
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
                    description = "Asignatura no encontrada"
            )
    })
    public ResponseEntity<SubjectDto> getSubjectById(
            @Parameter(description = "ID de la asignatura", example = "1")
            @PathVariable Long id) {

        log.debug("Admin consultando asignatura ID: {}", id);

        SubjectDto subject = subjectService.getSubjectById(id);

        return ResponseEntity.ok(subject);
    }

    /**
     * Crea una nueva asignatura.
     * No puede haber dos asignaturas con el mismo nombre en la misma carrera.
     *
     * @param createDto Datos de la nueva asignatura
     * @return Asignatura creada
     */
    @PostMapping
    @Operation(
            summary = "Crear asignatura",
            description = "Crea una nueva asignatura en el sistema. " +
                    "No puede haber dos asignaturas con el mismo nombre en la misma carrera."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Asignatura creada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o asignatura duplicada",
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
            )
    })
    public ResponseEntity<SubjectDto> createSubject(
            @Valid @RequestBody CreateSubjectDto createDto) {

        log.info("Admin creando nueva asignatura: {} - {}", createDto.getName(), createDto.getMajor());

        SubjectDto created = subjectService.createSubject(createDto);

        log.info("Asignatura creada exitosamente con ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza una asignatura existente.
     * Permite actualización parcial de campos.
     *
     * @param id ID de la asignatura a actualizar
     * @param updateDto Datos a actualizar (parcial)
     * @return Asignatura actualizada
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar asignatura",
            description = "Actualiza los datos de una asignatura existente. " +
                    "Solo se actualizan los campos enviados (actualización parcial)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Asignatura actualizada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o conflicto con asignatura existente",
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
                    description = "Asignatura no encontrada"
            )
    })
    public ResponseEntity<SubjectDto> updateSubject(
            @Parameter(description = "ID de la asignatura", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateSubjectDto updateDto) {

        log.info("Admin actualizando asignatura ID: {}", id);

        SubjectDto updated = subjectService.updateSubject(id, updateDto);

        log.info("Asignatura ID: {} actualizada exitosamente", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina una asignatura.
     * Solo permite eliminar asignaturas sin grupos ni solicitudes asociadas.
     *
     * @param id ID de la asignatura a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar asignatura",
            description = "Elimina una asignatura del sistema. " +
                    "No se puede eliminar si tiene grupos o solicitudes asociadas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Asignatura eliminada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeleteResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No se puede eliminar (tiene grupos o solicitudes asociadas)",
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
                    description = "Asignatura no encontrada"
            )
    })
    public ResponseEntity<DeleteResponseDto> deleteSubject(
            @Parameter(description = "ID de la asignatura", example = "1")
            @PathVariable Long id) {

        log.info("Admin eliminando asignatura ID: {}", id);

        subjectService.deleteSubject(id);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(id)
                .entityType("Subject")
                .success(true)
                .message("Asignatura eliminada exitosamente")
                .build();

        log.info("Asignatura ID: {} eliminada exitosamente", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene estadísticas de una asignatura.
     * Incluye información sobre grupos, inscripciones y solicitudes.
     *
     * @param id ID de la asignatura
     * @return Estadísticas de la asignatura
     */
    @GetMapping("/{id}/stats")
    @Operation(
            summary = "Obtener estadísticas de asignatura",
            description = "Obtiene estadísticas detalladas de una asignatura incluyendo " +
                    "número de grupos por estado, total de inscripciones y solicitudes pendientes."
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
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Asignatura no encontrada"
            )
    })
    public ResponseEntity<Object> getSubjectStats(
            @Parameter(description = "ID de la asignatura", example = "1")
            @PathVariable Long id) {

        log.debug("Admin consultando estadísticas de asignatura ID: {}", id);

        var stats = subjectService.getSubjectStats(id);

        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene asignaturas filtradas por carrera.
     *
     * @param major Nombre de la carrera
     * @return Lista de asignaturas de la carrera
     */
    @GetMapping("/by-major")
    @Operation(
            summary = "Filtrar asignaturas por carrera",
            description = "Obtiene todas las asignaturas de una carrera específica."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de asignaturas obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Carrera no puede estar vacía"
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
    public ResponseEntity<List<SubjectDto>> getSubjectsByMajor(
            @Parameter(description = "Nombre de la carrera", example = "Ingeniería Informática")
            @RequestParam String major) {

        log.debug("Admin consultando asignaturas de la carrera: {}", major);

        List<SubjectDto> subjects = subjectService.getSubjectsByMajor(major);

        log.info("Retornando {} asignaturas para la carrera: {}", subjects.size(), major);
        return ResponseEntity.ok(subjects);
    }

    /**
     * Obtiene asignaturas filtradas por año de curso.
     *
     * @param year Año del curso (1-6)
     * @return Lista de asignaturas del año especificado
     */
    @GetMapping("/by-year")
    @Operation(
            summary = "Filtrar asignaturas por año",
            description = "Obtiene todas las asignaturas de un año específico del curso."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de asignaturas obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Año de curso inválido (debe estar entre 1 y 6)"
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
    public ResponseEntity<List<SubjectDto>> getSubjectsByCourseYear(
            @Parameter(description = "Año del curso", example = "1")
            @RequestParam Integer year) {

        log.debug("Admin consultando asignaturas del año: {}", year);

        List<SubjectDto> subjects = subjectService.getSubjectsByCourseYear(year);

        log.info("Retornando {} asignaturas para el año: {}", subjects.size(), year);
        return ResponseEntity.ok(subjects);
    }

    /**
     * Verifica si una asignatura puede ser eliminada.
     * Una asignatura NO puede eliminarse si tiene grupos o solicitudes asociadas.
     *
     * @param id ID de la asignatura
     * @return true si puede eliminarse, false si tiene restricciones
     */
    @GetMapping("/{id}/can-delete")
    @Operation(
            summary = "Verificar eliminación de asignatura",
            description = "Verifica si una asignatura puede ser eliminada. " +
                    "No se puede eliminar si tiene grupos o solicitudes pendientes."
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
                    description = "Asignatura no encontrada"
            )
    })
    public ResponseEntity<Map<String, Boolean>> canDeleteSubject(
            @Parameter(description = "ID de la asignatura", example = "1")
            @PathVariable Long id) {

        log.debug("Verificando si se puede eliminar asignatura ID: {}", id);

        boolean canDelete = subjectService.canDeleteSubject(id);

        return ResponseEntity.ok(Map.of("canDelete", canDelete));
    }

    // ========== DTOs INTERNOS PARA DOCUMENTACIÓN ==========

    /**
     * DTO interno para respuestas de error.
     */
    @Schema(description = "Respuesta de error")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "SUBJECT_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "Ya existe una asignatura con ese nombre en la carrera")
        private String message;

        @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00Z")
        private String timestamp;
    }
}