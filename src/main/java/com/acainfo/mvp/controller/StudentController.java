package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.enrollment.CreateEnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentResponseDto;
import com.acainfo.mvp.dto.grouprequest.CreateGroupRequestDto;
import com.acainfo.mvp.dto.grouprequest.GroupRequestDto;
import com.acainfo.mvp.dto.grouprequest.GroupRequestResponseDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.service.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones de estudiantes.
 * Maneja el perfil del estudiante, consultas académicas e inscripciones.
 *
 * Todos los endpoints requieren autenticación con rol STUDENT.
 * La autenticación se maneja mediante sesiones HTTP (cookie JSESSIONID).
 */
@Slf4j
@RestController
@RequestMapping("/api/students")
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Students", description = "Endpoints para gestión de estudiantes")
public class StudentController {

    private final StudentService studentService;
    private final SubjectService subjectService;
    private final EnrollmentService enrollmentService;
    private final GroupRequestService groupRequestService;
    private final SessionUtils sessionUtils;

    public StudentController(StudentService studentService,
                             SubjectService subjectService,
                             EnrollmentService enrollmentService,
                             GroupRequestService groupRequestService,
                             SessionUtils sessionUtils) {
        this.studentService = studentService;
        this.subjectService = subjectService;
        this.enrollmentService = enrollmentService;
        this.groupRequestService = groupRequestService;
        this.sessionUtils = sessionUtils;
    }

    // ========== ENDPOINTS DE PERFIL ==========

    /**
     * Obtiene el perfil del estudiante actual.
     * Solo puede acceder a su propio perfil.
     *
     * @return Información del perfil del estudiante
     */
    @GetMapping("/profile")
    @Operation(
            summary = "Obtener mi perfil",
            description = "Devuelve la información del perfil del estudiante autenticado. " +
                    "Solo incluye información básica (sin contraseña)."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil obtenido exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
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
                    description = "No tiene permisos (no es estudiante)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    public ResponseEntity<StudentDto> getMyProfile() {
        log.debug("Solicitud de perfil del estudiante actual");
        StudentDto profile = studentService.getMyProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * Actualiza el perfil del estudiante.
     * Solo puede actualizar nombre y carrera (no email).
     *
     * @param updateDto Datos a actualizar
     * @return Perfil actualizado
     */
    @PutMapping("/profile/update")
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza la información del perfil del estudiante. " +
                    "Solo se pueden actualizar: nombre y carrera. " +
                    "El email no se puede cambiar por seguridad."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Perfil actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
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
    public ResponseEntity<StudentDto> updateProfile(
            @Valid @RequestBody StudentDto updateDto) {

        log.info("Actualizando perfil del estudiante ID: {}", sessionUtils.getCurrentUserId());
        StudentDto updatedProfile = studentService.updateProfile(
                sessionUtils.getCurrentUserId(), updateDto);

        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Cambia la contraseña del estudiante.
     * Requiere la contraseña actual para validación.
     *
     * @param changePasswordDto Datos para cambio de contraseña
     * @return Confirmación del cambio
     */
    @PostMapping("/change-password")
    @Operation(
            summary = "Cambiar contraseña",
            description = "Cambia la contraseña del estudiante. " +
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

        log.info("Cambio de contraseña solicitado para estudiante ID: {}",
                sessionUtils.getCurrentUserId());

        ApiResponseDto<Void> response = studentService.changePassword(
                sessionUtils.getCurrentUserId(), changePasswordDto);

        return ResponseEntity.ok(response);
    }

    // ========== ENDPOINTS DE CONSULTAS ACADÉMICAS ==========

    /**
     * Obtiene las asignaturas de la carrera del estudiante.
     * Filtradas automáticamente por la carrera que cursa.
     *
     * @return Lista de asignaturas de su carrera
     */
    @GetMapping("/subjects")
    @Operation(
            summary = "Obtener asignaturas de mi carrera",
            description = "Devuelve todas las asignaturas disponibles de la carrera " +
                    "del estudiante autenticado, organizadas por año."
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
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<ApiResponseDto<List<SubjectDto>>> getMyMajorSubjects() {
        log.debug("Obteniendo asignaturas de la carrera del estudiante");
        List<SubjectDto> subjects = studentService.getMyMajorSubjects();
        subjects.forEach(subject -> {
            log.debug("++++++++++++++++++++++++++++++++++++++++++++ {}", subject.getId());
        });

        ApiResponseDto<List<SubjectDto>> response = ApiResponseDto.success(
                subjects,
                "Asignaturas obtenidas exitosamente"
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene las asignaturas de un año específico de la carrera.
     *
     * @param year Año del curso (1-6)
     * @return Lista de asignaturas del año especificado
     */
    @GetMapping("/subjects/year/{year}")
    @Operation(
            summary = "Obtener asignaturas por año",
            description = "Devuelve las asignaturas de un año específico (1-6) " +
                    "de la carrera del estudiante."
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
                    description = "Año inválido (debe ser entre 1 y 6)",
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
    public ResponseEntity<List<SubjectDto>> getSubjectsByYear(
            @Parameter(description = "Año del curso (1-6)", example = "2")
            @PathVariable Integer year) {

        log.debug("Obteniendo asignaturas del año {} para el estudiante", year);
        List<SubjectDto> subjects = studentService.getMyMajorSubjectsByYear(year);
        return ResponseEntity.ok(subjects);
    }

    // ========== ENDPOINTS DE INSCRIPCIONES ==========

    /**
     * Obtiene las inscripciones actuales del estudiante.
     * Incluye información del grupo, asignatura y estado de pago.
     *
     * @return Lista de inscripciones del estudiante
     */
    @GetMapping("/enrollments")
    @Operation(
            summary = "Obtener mis inscripciones",
            description = "Devuelve todas las inscripciones del estudiante, " +
                    "incluyendo información del grupo, profesor, horarios y estado de pago."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de inscripciones obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnrollmentSummaryDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<List<EnrollmentSummaryDto>> getMyEnrollments() {
        log.debug("Obteniendo inscripciones del estudiante");
        List<EnrollmentSummaryDto> enrollments = studentService.getMyEnrollments();
        return ResponseEntity.ok(enrollments);
    }

    /**
     * Inscribe al estudiante en un grupo.
     * El grupo debe estar activo y tener espacio disponible.
     *
     * @param subjectId ID de la asignatura
     * @param groupId ID del grupo
     * @return Resultado de la inscripción
     */
    @PostMapping("/subjects/{subjectId}/groups/{groupId}/enroll")
    @Operation(
            summary = "Inscribirse en un grupo",
            description = "Inscribe al estudiante en un grupo específico. " +
                    "Validaciones: " +
                    "- El grupo debe estar ACTIVO " +
                    "- Debe haber espacio disponible " +
                    "- No puede estar ya inscrito " +
                    "- La asignatura debe ser de su carrera"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Inscripción realizada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnrollmentResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validación fallida (grupo cerrado, sin espacio, etc.)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya está inscrito en este grupo",
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
    public ResponseEntity<EnrollmentResponseDto> enrollInGroup(
            @Parameter(description = "ID de la asignatura", example = "123")
            @PathVariable Long subjectId,
            @Parameter(description = "ID del grupo", example = "456")
            @PathVariable Long groupId) {

        log.info("Procesando inscripción del estudiante {} en grupo {} de asignatura {}",
                sessionUtils.getCurrentUserId(), groupId, subjectId);

        // Crear DTO de inscripción
        CreateEnrollmentDto enrollmentDto = CreateEnrollmentDto.builder()
                .studentId(sessionUtils.getCurrentUserId())
                .courseGroupId(groupId)
                .build();

        EnrollmentResponseDto response = enrollmentService.enrollStudent(enrollmentDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    /**
     * Cancela una inscripción.
     * Solo posible si el pago está pendiente y el grupo no ha iniciado.
     *
     * @param enrollmentId ID de la inscripción a cancelar
     * @return Confirmación de cancelación
     */
    @DeleteMapping("/enrollments/{enrollmentId}")
    @Operation(
            summary = "Cancelar inscripción",
            description = "Cancela una inscripción existente. " +
                    "Solo es posible si: " +
                    "- El pago está PENDIENTE " +
                    "- El grupo no está CERRADO"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Inscripción cancelada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = EnrollmentResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No se puede cancelar (pago confirmado o grupo cerrado)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Inscripción no encontrada",
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
    public ResponseEntity<ApiResponseDto> cancelEnrollment(
            @Parameter(description = "ID de la inscripción", example = "123")
            @PathVariable Long enrollmentId) {

        log.info("Cancelando inscripción ID: {} del estudiante {}",
                enrollmentId, sessionUtils.getCurrentUserId());

        ApiResponseDto response = enrollmentService.cancelEnrollment(enrollmentId);

        return ResponseEntity.ok(response);
    }

    // ========== ENDPOINTS DE SOLICITUDES DE GRUPO ==========

    /**
     * Crea una solicitud de nuevo grupo.
     * Para cuando no existen grupos activos de una asignatura.
     *
     * @param requestDto Datos de la solicitud
     * @return Resultado de la solicitud
     */
    @PostMapping("/group-requests/create")
    @Operation(
            summary = "Solicitar creación de grupo",
            description = "Crea una solicitud para que se abra un nuevo grupo " +
                    "de una asignatura que no tiene grupos activos. " +
                    "El administrador revisará la solicitud."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Solicitud creada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupRequestResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solicitud inválida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya tiene una solicitud pendiente para esta asignatura",
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
    public ResponseEntity<GroupRequestResponseDto> createGroupRequest(
            @Valid @RequestBody CreateGroupRequestDto requestDto) {

        log.info("Creando solicitud de grupo para asignatura ID: {}",
                requestDto.getSubjectId());

        GroupRequestResponseDto response = groupRequestService.createGroupRequest(requestDto);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Obtiene las solicitudes de grupo del estudiante.
     *
     * @return Lista de solicitudes del estudiante
     */
    @GetMapping("/group-requests/get")
    @Operation(
            summary = "Obtener mis solicitudes de grupo",
            description = "Devuelve todas las solicitudes de creación de grupo " +
                    "realizadas por el estudiante, con su estado actual."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de solicitudes obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupRequestDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<List<GroupRequestDto>> getMyGroupRequests() {
        log.debug("Obteniendo solicitudes de grupo del estudiante");
        List<GroupRequestDto> requests = groupRequestService.getMyGroupRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * Verifica si puede solicitar un grupo para una asignatura.
     *
     * @param subjectId ID de la asignatura
     * @return true si puede solicitar, false si ya tiene solicitud pendiente
     */
    @GetMapping("/group-requests/can-request/{subjectId}")
    @Operation(
            summary = "Verificar si puede solicitar grupo",
            description = "Verifica si el estudiante puede solicitar la creación " +
                    "de un grupo para una asignatura específica."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Verificación completada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Boolean.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<Boolean> canRequestGroup(
            @Parameter(description = "ID de la asignatura", example = "456")
            @PathVariable Long subjectId) {

        log.debug("Verificando si puede solicitar grupo para asignatura ID: {}", subjectId);
        boolean canRequest = groupRequestService.canRequestGroup(subjectId);
        return ResponseEntity.ok(canRequest);
    }

    // ========== ENDPOINTS DE ESTADÍSTICAS ==========

    /**
     * Obtiene estadísticas del estudiante.
     * Incluye información resumida de inscripciones y progreso.
     *
     * @return Estadísticas del estudiante
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Obtener mis estadísticas",
            description = "Devuelve estadísticas del estudiante incluyendo: " +
                    "número de inscripciones, pagos pendientes, asignaturas cursadas, etc."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estadísticas obtenidas",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentStatsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado"
            )
    })
    public ResponseEntity<Object> getMyStats() {
        log.debug("Obteniendo estadísticas del estudiante");
        var stats = studentService.getMyStats();
        return ResponseEntity.ok(stats);
    }

    // ========== DTOs INTERNOS PARA DOCUMENTACIÓN ==========

    /**
     * DTO interno para respuestas de error genéricas.
     */
    @Schema(description = "Respuesta de error genérica")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "STUDENT_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "No se puede inscribir en este grupo")
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
    @Schema(description = "Estadísticas del estudiante")
    private static class StudentStatsResponse {
        @Schema(description = "ID del estudiante", example = "123")
        private Long studentId;

        @Schema(description = "Nombre del estudiante", example = "Juan Pérez")
        private String studentName;

        @Schema(description = "Carrera", example = "Ingeniería en Sistemas")
        private String major;

        @Schema(description = "Total de inscripciones", example = "5")
        private int totalEnrollments;

        @Schema(description = "Inscripciones activas", example = "3")
        private int activeEnrollments;

        @Schema(description = "Pagos pendientes", example = "2")
        private int pendingPayments;

        @Schema(description = "Total de asignaturas en la carrera", example = "48")
        private int totalSubjectsInMajor;

        @Schema(description = "Asignaturas inscritas", example = "12")
        private int enrolledSubjects;

        @Schema(description = "Asignaturas restantes", example = "36")
        private int remainingSubjects;
    }
}