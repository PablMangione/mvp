package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.common.DeleteResponseDto;
import com.acainfo.mvp.dto.common.PageResponseDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.service.StudentService;
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

import java.util.List;
import java.util.Map;

/**
 * Controlador administrativo para gestión de estudiantes.
 * Todos los endpoints requieren rol ADMIN.
 *
 * @author Academic Info System
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/students")
@RequiredArgsConstructor
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Student Administration", description = "Endpoints administrativos para gestión de estudiantes")
public class StudentAdminController {

    private final StudentService studentService;

    /**
     * Obtiene todos los estudiantes con paginación.
     *
     * @param pageable Parámetros de paginación
     * @return Página de estudiantes
     */
    @GetMapping
    @Operation(
            summary = "Listar estudiantes",
            description = "Obtiene todos los estudiantes del sistema con paginación. " +
                    "Permite filtrar y ordenar los resultados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de estudiantes obtenida exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PageResponseDto.class)
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
    public ResponseEntity<PageResponseDto<StudentDto>> getAllStudents(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        log.debug("Admin consultando lista de estudiantes, página: {}", pageable.getPageNumber());

        Page<StudentDto> page = studentService.getAllStudents(pageable);

        return ResponseEntity.ok(PageResponseDto.from(page));
    }

    /**
     * Obtiene un estudiante específico por ID.
     *
     * @param id ID del estudiante
     * @return Datos del estudiante
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener estudiante por ID",
            description = "Obtiene la información completa de un estudiante específico."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estudiante encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
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
                    description = "Estudiante no encontrado"
            )
    })
    public ResponseEntity<StudentDto> getStudentById(
            @Parameter(description = "ID del estudiante", example = "1")
            @PathVariable Long id) {

        log.debug("Admin consultando estudiante ID: {}", id);

        StudentDto student = studentService.getStudentById(id);

        return ResponseEntity.ok(student);
    }

    /**
     * Crea un nuevo estudiante (Alta de alumno).
     * El email debe ser único. La contraseña debe tener mínimo 8 caracteres.
     *
     * @param createDto Datos del nuevo estudiante
     * @return Estudiante creado
     */
    @PostMapping
    @Operation(
            summary = "Alta de alumno",
            description = "Crea un nuevo estudiante en el sistema. " +
                    "El email debe ser único. La contraseña debe tener mínimo 8 caracteres."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Estudiante creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
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
    public ResponseEntity<StudentDto> createStudent(
            @Valid @RequestBody CreateStudentDto createDto) {

        log.info("Admin creando nuevo estudiante: {}", createDto.getEmail());

        StudentDto created = studentService.createStudent(createDto);

        log.info("Estudiante creado exitosamente con ID: {}", created.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Actualiza un estudiante existente.
     *
     * @param id ID del estudiante a actualizar
     * @param updateDto Datos a actualizar
     * @return Estudiante actualizado
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar estudiante",
            description = "Actualiza los datos de un estudiante existente. " +
                    "Solo actualiza los campos enviados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estudiante actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
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
                    description = "Estudiante no encontrado"
            )
    })
    public ResponseEntity<StudentDto> updateStudent(
            @Parameter(description = "ID del estudiante", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody StudentDto updateDto) {

        log.info("Admin actualizando estudiante ID: {}", id);

        StudentDto updated = studentService.updateStudent(id, updateDto);

        log.info("Estudiante ID: {} actualizado exitosamente", id);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina un estudiante (Baja de alumno).
     * Solo posible si no tiene inscripciones activas.
     *
     * @param id ID del estudiante a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Baja de alumno",
            description = "Elimina un estudiante del sistema. " +
                    "No se puede eliminar si tiene inscripciones activas."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estudiante eliminado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeleteResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No se puede eliminar (tiene inscripciones activas)",
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
                    description = "Estudiante no encontrado"
            )
    })
    public ResponseEntity<DeleteResponseDto> deleteStudent(
            @Parameter(description = "ID del estudiante", example = "1")
            @PathVariable Long id) {

        log.info("Admin eliminando estudiante ID: {}", id);

        studentService.deleteStudent(id);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(id)
                .entityType("Student")
                .success(true)
                .message("Estudiante eliminado exitosamente")
                .build();

        log.info("Estudiante ID: {} eliminado exitosamente", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene estudiantes filtrados por carrera.
     *
     * @param major Nombre de la carrera
     * @return Lista de estudiantes de la carrera
     */
    @GetMapping("/by-major")
    @Operation(
            summary = "Filtrar estudiantes por carrera",
            description = "Obtiene todos los estudiantes de una carrera específica. " +
                    "Útil para reportes y estadísticas por carrera."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de estudiantes obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
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
    public ResponseEntity<List<StudentDto>> getStudentsByMajor(
            @Parameter(description = "Nombre de la carrera", example = "Ingeniería Informática")
            @RequestParam String major) {

        log.debug("Admin consultando estudiantes de la carrera: {}", major);

        List<StudentDto> students = studentService.getStudentsByMajor(major);

        log.info("Retornando {} estudiantes para la carrera: {}", students.size(), major);
        return ResponseEntity.ok(students);
    }

    /**
     * Obtiene estadísticas administrativas de estudiantes.
     * Incluye información agregada útil para el dashboard administrativo.
     *
     * @return Estadísticas de estudiantes
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Obtener estadísticas de estudiantes",
            description = "Obtiene estadísticas agregadas sobre los estudiantes: " +
                    "total por carrera, inscripciones activas, pagos pendientes, etc."
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
    public ResponseEntity<Object> getStudentStats() {
        log.debug("Admin consultando estadísticas de estudiantes");

        var stats = studentService.getAdminStudentStats();

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
            description = "Verifica si un email ya está registrado por otro estudiante. " +
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
    public ResponseEntity<Map<String, Boolean>> checkStudentEmail(
            @Parameter(description = "Email a verificar", example = "estudiante@academia.com")
            @RequestParam String email) {

        log.debug("Verificando disponibilidad del email: {}", email);

        boolean exists = studentService.emailExists(email);

        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Verifica si un estudiante puede ser eliminado.
     * Un estudiante NO puede eliminarse si tiene inscripciones ACTIVAS.
     *
     * @param id ID del estudiante
     * @return true si puede eliminarse, false si tiene restricciones
     */
    @GetMapping("/{id}/can-delete")
    @Operation(
            summary = "Verificar eliminación de estudiante",
            description = "Verifica si un estudiante puede ser eliminado. " +
                    "No se puede eliminar si tiene inscripciones activas."
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
                    description = "Estudiante no encontrado"
            )
    })
    public ResponseEntity<Map<String, Boolean>> canDeleteStudent(
            @Parameter(description = "ID del estudiante", example = "1")
            @PathVariable Long id) {

        log.debug("Verificando si se puede eliminar estudiante ID: {}", id);

        boolean canDelete = studentService.canDeleteStudent(id);

        return ResponseEntity.ok(Map.of("canDelete", canDelete));
    }

    // ========== DTOs INTERNOS PARA DOCUMENTACIÓN ==========

    /**
     * DTO interno para respuestas de error genéricas.
     */
    @Schema(description = "Respuesta de error genérica")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "STUDENT_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "No se puede eliminar el estudiante")
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
        private Map<String, String> fieldErrors;

        @Schema(description = "Timestamp del error", example = "2024-01-15T10:30:00Z")
        private String timestamp;
    }
}