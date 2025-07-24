package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.common.DeleteResponseDto;
import com.acainfo.mvp.dto.common.PageResponseDto;
import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.dto.grouprequest.GroupRequestDto;
import com.acainfo.mvp.dto.grouprequest.GroupRequestSearchCriteriaDto;
import com.acainfo.mvp.dto.grouprequest.UpdateRequestStatusDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.subject.CreateSubjectDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.dto.subject.UpdateSubjectDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.enums.RequestStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para operaciones administrativas.
 *
 * Iteración 1: Funcionalidad de gestión básica
 * - Alta/Baja de alumnos y profesores
 * - Creación y eliminación de asignaturas y grupos
 * - Cambiar estado de grupo (planificado → activo → cerrado)
 * - Gestión de solicitudes de grupos
 *
 * Todos los endpoints requieren autenticación con rol ADMIN.
 * La autenticación se maneja mediante sesiones HTTP (cookie JSESSIONID).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "sessionAuth")
@Tag(name = "Administration", description = "Endpoints para administración del sistema (Iteración 1)")
public class AdminController {

    private final StudentService studentService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;
    private final CourseGroupService courseGroupService;
    private final GroupRequestService groupRequestService;
    private final SessionUtils sessionUtils;
    private final SubjectMapper subjectMapper;

    public AdminController(StudentService studentService,
                           TeacherService teacherService,
                           SubjectService subjectService,
                           CourseGroupService courseGroupService,
                           GroupRequestService groupRequestService,
                           SessionUtils sessionUtils, SubjectMapper subjectMapper) {
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.subjectService = subjectService;
        this.courseGroupService = courseGroupService;
        this.groupRequestService = groupRequestService;
        this.sessionUtils = sessionUtils;
        this.subjectMapper = subjectMapper;
    }

    // ========== GESTIÓN DE ALUMNOS ==========

    /**
     * Obtiene todos los estudiantes con paginación.
     *
     * @param pageable Parámetros de paginación
     * @return Página de estudiantes
     */
    @GetMapping("/students")
    @Operation(
            summary = "Listar estudiantes",
            description = "Obtiene todos los estudiantes del sistema con paginación. " +
                    "Permite filtrar y ordenar los resultados."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de estudiantes obtenida",
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
        //¿QUÉ VA AQUÍ?
        Page<StudentDto> page = studentService.getAllStudents(pageable);
        return ResponseEntity.ok(PageResponseDto.from(page));


    }

    /**
     * Crea un nuevo estudiante (Alta de alumno).
     *
     * @param createDto Datos del nuevo estudiante
     * @return Estudiante creado
     */
    @PostMapping("/students")
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
        StudentDto createdStudent = studentService.createStudent(createDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
    }

    /**
     * Elimina un estudiante (Baja de alumno).
     * Solo posible si no tiene inscripciones activas.
     *
     * @param studentId ID del estudiante a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/students/{studentId}")
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
                    responseCode = "404",
                    description = "Estudiante no encontrado"
            )
    })
    public ResponseEntity<DeleteResponseDto> deleteStudent(
            @Parameter(description = "ID del estudiante", example = "123")
            @PathVariable Long studentId) {

        log.info("Admin eliminando estudiante ID: {}", studentId);
        studentService.deleteStudent(studentId);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(studentId)
                .entityType("Student")
                .success(true)
                .message("Estudiante eliminado exitosamente")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si un email ya está registrado en el sistema.
     * Útil para validación en tiempo real en formularios.
     *
     * @param email Email a verificar
     * @return true si el email ya existe, false si está disponible
     */
    @GetMapping("/students/check-email")
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
                            schema = @Schema(implementation = Boolean.class)
                    )
            )
    })
    public ResponseEntity<Map<String, Boolean>> checkStudentEmail(
            @RequestParam String email) {

        log.debug("Verificando disponibilidad del email: {}", email);
        boolean exists = studentService.emailExists(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Obtiene todas las solicitudes de grupo de un estudiante específico.
     * Incluye solicitudes en todos los estados.
     *
     * @param studentId ID del estudiante
     * @return Lista de solicitudes del estudiante
     */
    @GetMapping("/students/{studentId}/group-requests")
    @Operation(
            summary = "Solicitudes por estudiante",
            description = "Obtiene el historial completo de solicitudes de grupo " +
                    "de un estudiante específico."
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
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado")
    })
    public ResponseEntity<List<GroupRequestDto>> getRequestsByStudent(
            @PathVariable Long studentId) {

        log.debug("Consultando solicitudes del estudiante ID: {}", studentId);
        List<GroupRequestDto> requests = groupRequestService.getRequestsByStudent(studentId);
        return ResponseEntity.ok(requests);
    }

    // ========== GESTIÓN DE PROFESORES ==========

    /**
     * Obtiene todos los profesores con paginación.
     *
     * @param pageable Parámetros de paginación
     * @return Página de profesores
     */
    @GetMapping("/teachers")
    @Operation(
            summary = "Listar profesores",
            description = "Obtiene todos los profesores del sistema con paginación."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de profesores obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = Page.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "No tiene permisos")
    })
    public ResponseEntity<PageResponseDto<TeacherDto>> getAllTeachers(
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        log.debug("Admin consultando lista de profesores, página: {}", pageable.getPageNumber());
        Page<TeacherDto> teachers = teacherService.getAllTeachers(pageable);
        return ResponseEntity.ok(PageResponseDto.from(teachers)); // Cambiar aquí también
    }

    /**
     * Crea un nuevo profesor (Alta de profesor).
     *
     * @param createDto Datos del nuevo profesor
     * @return Profesor creado
     */
    @PostMapping("/teachers")
    @Operation(
            summary = "Alta de profesor",
            description = "Crea un nuevo profesor en el sistema. " +
                    "El email debe ser único. Los profesores NO pueden auto-registrarse."
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
                    description = "Datos inválidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El email ya está registrado"
            )
    })
    public ResponseEntity<TeacherDto> createTeacher(
            @Valid @RequestBody CreateTeacherDto createDto) {

        log.info("Admin creando nuevo profesor: {}", createDto.getEmail());
        TeacherDto createdTeacher = teacherService.createTeacher(createDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdTeacher);
    }

    /**
     * Elimina un profesor (Baja de profesor).
     * Solo posible si no tiene grupos activos asignados.
     *
     * @param teacherId ID del profesor a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/teachers/{teacherId}")
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
                    description = "No se puede eliminar (tiene grupos activos)"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profesor no encontrado"
            )
    })
    public ResponseEntity<DeleteResponseDto> deleteTeacher(
            @Parameter(description = "ID del profesor", example = "456")
            @PathVariable Long teacherId) {

        log.info("Admin eliminando profesor ID: {}", teacherId);
        teacherService.deleteTeacher(teacherId);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(teacherId)
                .entityType("Teacher")
                .success(true)
                .message("Profesor eliminado exitosamente")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica si un profesor puede ser eliminado del sistema.
     * Un profesor NO puede eliminarse si tiene grupos ACTIVOS asignados.
     *
     * @param teacherId ID del profesor
     * @return true si puede eliminarse, false si tiene restricciones
     */
    @GetMapping("/teachers/{teacherId}/can-delete")
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
                            schema = @Schema(implementation = Boolean.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado")
    })
    public ResponseEntity<Map<String, Boolean>> canDeleteTeacher(@PathVariable Long teacherId) {
        log.debug("Verificando si se puede eliminar profesor ID: {}", teacherId);
        boolean canDelete = teacherService.canDeleteTeacher(teacherId);
        return ResponseEntity.ok(Map.of("canDelete", canDelete));
    }

    /**
     * Obtiene todos los grupos asignados a un profesor específico.
     * Incluye grupos en todos los estados (PLANNED, ACTIVE, CLOSED).
     *
     * @param teacherId ID del profesor
     * @return Lista de grupos del profesor
     */
    @GetMapping("/teachers/{teacherId}/groups")
    @Operation(
            summary = "Grupos por profesor",
            description = "Obtiene todos los grupos asignados a un profesor específico. " +
                    "Útil para gestionar carga docente y horarios."
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
            @ApiResponse(responseCode = "404", description = "Profesor no encontrado")
    })
    public ResponseEntity<List<CourseGroupDto>> getGroupsByTeacher(
            @PathVariable Long teacherId) {

        log.debug("Consultando grupos del profesor ID: {}", teacherId);
        List<CourseGroupDto> groups = courseGroupService.getGroupsByTeacher(teacherId);
        return ResponseEntity.ok(groups);
    }

    // ========== GESTIÓN DE ASIGNATURAS ==========

    /**
     * Obtiene todas las asignaturas.
     *
     * @return Lista de todas las asignaturas
     */
    @GetMapping("/subjects")
    @Operation(
            summary = "Listar asignaturas",
            description = "Obtiene todas las asignaturas del sistema."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de asignaturas obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SubjectDto.class)
                    )
            )
    })
    public ResponseEntity<List<SubjectDto>> getAllSubjects() {
        log.debug("Admin consultando todas las asignaturas");
        List<SubjectDto> subjects = subjectService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    /**
     * Crea una nueva asignatura.
     *
     * @param createDto Datos de la nueva asignatura
     * @return Asignatura creada
     */
    @PostMapping("/subjects")
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
                    description = "Datos inválidos"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Ya existe una asignatura con ese nombre en la carrera"
            )
    })
    public ResponseEntity<SubjectDto> createSubject(
            @Valid @RequestBody CreateSubjectDto createDto) {

        log.info("Admin creando nueva asignatura: {}", createDto.getName());
        SubjectDto createdSubject = subjectService.createSubject(createDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdSubject);
    }

    /**
     * Actualiza una asignatura existente.
     *
     * @param subjectId ID de la asignatura
     * @param updateDto Datos a actualizar
     * @return Asignatura actualizada
     */
    @PutMapping("/subjects/{subjectId}")
    @Operation(
            summary = "Actualizar asignatura",
            description = "Actualiza los datos de una asignatura existente."
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
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada")
    })
    public ResponseEntity<SubjectDto> updateSubject(
            @PathVariable Long subjectId,
            @Valid @RequestBody UpdateSubjectDto updateDto) {

        log.info("Admin actualizando asignatura ID: {}", subjectId);
        SubjectDto updatedSubject = subjectService.updateSubject(subjectId, updateDto);

        return ResponseEntity.ok(updatedSubject);
    }

    /**
     * Elimina una asignatura.
     * Solo posible si no tiene grupos o solicitudes asociadas.
     *
     * @param subjectId ID de la asignatura
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/subjects/{subjectId}")
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
                    description = "No se puede eliminar (tiene grupos o solicitudes)"
            ),
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada")
    })
    public ResponseEntity<DeleteResponseDto> deleteSubject(
            @PathVariable Long subjectId) {

        log.info("Admin eliminando asignatura ID: {}", subjectId);
        subjectService.deleteSubject(subjectId);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(subjectId)
                .entityType("Subject")
                .success(true)
                .message("Asignatura eliminada exitosamente")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Obtiene todas las solicitudes de grupo para una asignatura específica.
     * Útil para analizar la demanda de una asignatura.
     *
     * @param subjectId ID de la asignatura
     * @return Lista de solicitudes para esa asignatura
     */
    @GetMapping("/subjects/{subjectId}/group-requests")
    @Operation(
            summary = "Solicitudes por asignatura",
            description = "Obtiene todas las solicitudes de creación de grupo para una " +
                    "asignatura específica. Permite analizar la demanda real."
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
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada")
    })
    public ResponseEntity<List<GroupRequestDto>> getRequestsBySubject(
            @PathVariable Long subjectId) {

        log.debug("Admin consultando solicitudes para asignatura ID: {}", subjectId);
        List<GroupRequestDto> requests = groupRequestService.getRequestsBySubject(subjectId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Obtiene todas las solicitudes de grupo para una asignatura específica.
     * Útil para analizar la demanda de una asignatura.
     *
     * @param subjectId ID de la asignatura
     * @return Lista de solicitudes para esa asignatura
     */
    @GetMapping("/subjects/{subjectId}/groups")
    @Operation(
            summary = "Solicitudes por asignatura",
            description = "Obtiene todas las solicitudes de creación de grupo para una " +
                    "asignatura específica. Permite analizar la demanda real."
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
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada")
    })
    public ResponseEntity<List<CourseGroupDto>> getGroupsBySubject(
            @PathVariable Long subjectId) {

        log.debug("Admin consultando grupos de una asignatura: {}", subjectId);
        List<CourseGroupDto> requests = courseGroupService.getGroupsBySubject(subjectId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/subjects/{subjectId}")
    @Operation(
            summary = "Solicitudes por asignatura",
            description = "Obtiene todas las solicitudes de creación de grupo para una " +
                    "asignatura específica. Permite analizar la demanda real."
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
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada")
    })
    public ResponseEntity<SubjectDto> getSubjectById(
            @PathVariable Long subjectId) {
        log.debug("Admin consultando solicitudes para asignatura ID: {}", subjectId);
        SubjectDto requests = subjectMapper.toDto(
                courseGroupService.getSubjectById(subjectId));
        return ResponseEntity.ok(requests);
    }

    /**
     * Verifica si una asignatura puede ser eliminada del sistema.
     * No puede eliminarse si tiene grupos o solicitudes asociadas.
     *
     * @param subjectId ID de la asignatura
     * @return true si puede eliminarse, false si tiene restricciones
     */
    @GetMapping("/subjects/{subjectId}/can-delete")
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
                            schema = @Schema(implementation = Boolean.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada")
    })
    public ResponseEntity<Map<String, Boolean>> canDeleteSubject(@PathVariable Long subjectId) {
        log.debug("Verificando si se puede eliminar asignatura ID: {}", subjectId);
        boolean canDelete = subjectService.canDeleteSubject(subjectId);
        return ResponseEntity.ok(Map.of("canDelete", canDelete));
    }

    // ========== GESTIÓN DE GRUPOS ==========

    /**
     * Crea un nuevo grupo de curso.
     *
     * @param createDto Datos del nuevo grupo
     * @return Grupo creado
     */
    @PostMapping("/groups")
    @Operation(
            summary = "Crear grupo",
            description = "Crea un nuevo grupo de curso. " +
                    "El grupo se crea en estado PLANNED por defecto. " +
                    "Puede asignarse un profesor al momento de creación o después."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Grupo creado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Asignatura o profesor no encontrado")
    })
    public ResponseEntity<CourseGroupDto> createGroup(
            @Valid @RequestBody CreateCourseGroupDto createDto) {

        log.info("Admin creando nuevo grupo para asignatura ID: {}", createDto.getSubjectId());
        CourseGroupDto createdGroup = courseGroupService.createGroup(createDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    /**
     * Obtiene todos los grupos
     *
     *
     * @return Información completa del grupo
     */
    @GetMapping("/groups/all")
    @Operation(
            summary = "Obtener todos los grupos",
            description = "Obtiene toda la información de un grupo específico, " +
                    "incluyendo asignatura, profesor asignado, estado y estudiantes inscritos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Grupo encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<List<CourseGroupDto>> getAllGroups() {
        log.debug("Admin consultando todos los grupos");
        List<CourseGroupDto> groups = courseGroupService.getAllGroupsWithEnrollmentCount();
        return ResponseEntity.ok(groups);
    }

    /**
     * Obtiene la información detallada de un grupo específico.
     * Incluye información de la asignatura, profesor, estado y capacidad.
     *
     * @param groupId ID del grupo
     * @return Información completa del grupo
     */
    @GetMapping("/groups/{groupId}")
    @Operation(
            summary = "Obtener detalle de grupo",
            description = "Obtiene toda la información de un grupo específico, " +
                    "incluyendo asignatura, profesor asignado, estado y estudiantes inscritos."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Grupo encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<CourseGroupDto> getGroupById(@PathVariable Long groupId) {
        log.debug("Admin consultando grupo ID: {}", groupId);
        CourseGroupDto group = courseGroupService.getGroupById(groupId);
        return ResponseEntity.ok(group);
    }

    /**
     * Cambia el estado de un grupo (planificado → activo → cerrado).
     *
     * @param groupId ID del grupo
     * @param statusDto Nuevo estado
     * @return Grupo actualizado
     */
    @PutMapping("/groups/{groupId}/status")
    @Operation(
            summary = "Cambiar estado de grupo",
            description = "Cambia el estado de un grupo. " +
                    "Transiciones permitidas: " +
                    "PLANNED → ACTIVE, PLANNED → CLOSED, ACTIVE → CLOSED. " +
                    "Un grupo CLOSED no puede cambiar de estado."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estado actualizado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Transición de estado no permitida"
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<CourseGroupDto> updateGroupStatus(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupStatusDto statusDto) {

        log.info("Admin cambiando estado del grupo {} a {}", groupId, statusDto.getStatus());
        CourseGroupDto updatedGroup = courseGroupService.updateGroupStatus(groupId, statusDto);

        return ResponseEntity.ok(updatedGroup);
    }

    /**
     * Asigna un profesor a un grupo.
     *
     * @param groupId ID del grupo
     * @param assignDto Datos del profesor a asignar
     * @return Grupo actualizado
     */
    @PutMapping("/groups/{groupId}/teacher")
    @Operation(
            summary = "Asignar profesor a grupo",
            description = "Asigna un profesor a un grupo que no tiene profesor. " +
                    "Se valida disponibilidad de horario del profesor."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Profesor asignado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "El grupo ya tiene profesor o hay conflicto de horario"
            ),
            @ApiResponse(responseCode = "404", description = "Grupo o profesor no encontrado")
    })
    public ResponseEntity<CourseGroupDto> assignTeacher(
            @PathVariable Long groupId,
            @Valid @RequestBody AssignTeacherDto assignDto) {

        log.info("Admin asignando profesor {} al grupo {}", assignDto.getTeacherId(), groupId);
        CourseGroupDto updatedGroup = courseGroupService.assignTeacher(groupId, assignDto);

        return ResponseEntity.ok(updatedGroup);
    }

    /**
     * Crea una sesión (horario) para un grupo.
     *
     * @param groupId ID del grupo
     * @param sessionDto Datos de la sesión
     * @return Sesión creada
     */
    @PostMapping("/groups/{groupId}/sessions")
    @Operation(
            summary = "Crear sesión de grupo",
            description = "Añade una sesión (día, hora, aula) a un grupo. " +
                    "Se validan conflictos de horario."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Sesión creada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupSessionDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Horario inválido o conflicto de horario"
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<CourseGroupDto> createGroupSession(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupSessionDto sessionDto) {

        log.info("Admin creando sesión para grupo ID: {}", groupId);
        CourseGroupDto createdSession = courseGroupService.createGroupSession(groupId, sessionDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdSession);
    }

    /**
     * Crea una sesión (horario) para un grupo.
     *
     * @param groupId ID del grupo
     * @param sessionDto Datos de la sesión
     * @return Sesión creada
     */
    @PutMapping("/groups/{groupId}/sessions")
    @Operation(
            summary = "Editar sesion de un grupo",
            description = "Modifica una sesion (día, hora, aula) de un grupo. " +
                    "Se validan conflictos de horario."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Sesión modificada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupSessionDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Horario inválido o conflicto de horario"
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<CourseGroupDto> editGroupSession(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupSessionDto sessionDto) {

        log.info("Admin editando sesión para grupo ID: {}", groupId);
        CourseGroupDto editedGroupSession = courseGroupService.editGroupSession(groupId, sessionDto);
        return ResponseEntity.ok().body(editedGroupSession);
    }

    /**
     * Crea una sesión (horario) para un grupo.
     *
     * @param groupId ID del grupo
     * @return sesiones del grupo
     */
    @GetMapping("/groups/{groupId}/sessions/")
    @Operation(
            summary = "Devuelve las sesiones del grupo",
            description = "Se valida que solo sean las del grupo con id recibido"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Sesiones consultadas exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupSessionDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No se encuentran las sesiones"
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<List<GroupSessionDto>> getGroupSessions(
            @PathVariable Long groupId) {

        log.info("Admin obteniendo todas las sesiones de un grupo: {}", groupId);
        List<GroupSessionDto> dev = courseGroupService.getGroupSessions(groupId);
        return ResponseEntity.ok().body(dev);
    }

    /**
     * Obtiene la lista de inscritos a un grupo
     *
     * @param groupId ID del grupo
     * @return estudiantes del grupo
     */
    @GetMapping("/groups/{groupId}/students")
    @Operation(
            summary = "Devuelve los inscritos del grupo",
            description = "Se valida que solo sean las del grupo con id recibido"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Estudiantes obtenidos exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StudentDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<List<StudentDto>> getEnrolledStudents(
            @PathVariable Long groupId) {
        log.info("Admin obteniendo todos los estudiantes: {}", groupId);
        List<StudentDto> dev = courseGroupService.getEnrolledStudents(groupId);
        return ResponseEntity.ok().body(dev);
    }

    /**
     * Elimina un grupo.
     * Solo posible si está en estado PLANNED y sin inscripciones.
     *
     * @param groupId ID del grupo
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/groups/{groupId}")
    @Operation(
            summary = "Eliminar grupo",
            description = "Elimina un grupo del sistema. " +
                    "Solo se pueden eliminar grupos en estado PLANNED sin inscripciones."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Grupo eliminado exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = DeleteResponseDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "No se puede eliminar (no está PLANNED o tiene inscripciones)"
            ),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado")
    })
    public ResponseEntity<DeleteResponseDto> deleteGroup(
            @PathVariable Long groupId) {

        log.info("Admin eliminando grupo ID: {}", groupId);
        courseGroupService.deleteGroup(groupId);

        DeleteResponseDto response = DeleteResponseDto.builder()
                .deletedId(groupId)
                .entityType("CourseGroup")
                .success(true)
                .message("Grupo eliminado exitosamente")
                .build();

        return ResponseEntity.ok(response);
    }


    // ========== GESTIÓN DE SOLICITUDES DE GRUPO ==========

    /**
     * Obtiene todas las solicitudes de grupo pendientes.
     *
     * @return Lista de solicitudes pendientes
     */
    @GetMapping("/group-requests/pending")
    @Operation(
            summary = "Ver solicitudes pendientes",
            description = "Obtiene todas las solicitudes de creación de grupo " +
                    "que están pendientes de revisión."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de solicitudes obtenida",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupRequestDto.class)
                    )
            )
    })
    public ResponseEntity<List<GroupRequestDto>> getPendingGroupRequests() {
        log.debug("Admin consultando solicitudes de grupo pendientes");
        List<GroupRequestDto> requests = groupRequestService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    /**
     * Actualiza el estado de una solicitud de grupo.
     *
     * @param requestId ID de la solicitud
     * @param updateDto Nuevo estado (APPROVED/REJECTED)
     * @return Solicitud actualizada
     */
    @PutMapping("/group-requests/{requestId}/status")
    @Operation(
            summary = "Actualizar solicitud de grupo",
            description = "Aprueba o rechaza una solicitud de creación de grupo. " +
                    "Si se aprueba, se puede crear el grupo correspondiente."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Solicitud actualizada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupRequestDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Solo se pueden actualizar solicitudes pendientes"
            ),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada")
    })
    public ResponseEntity<GroupRequestDto> updateGroupRequestStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestStatusDto updateDto) {

        log.info("Admin actualizando solicitud {} a estado {}", requestId, updateDto.getStatus());
        GroupRequestDto updatedRequest = groupRequestService.updateRequestStatus(requestId, updateDto);

        return ResponseEntity.ok(updatedRequest);
    }

    @GetMapping("/weekly-schedule/create-group")
    @Operation(
            summary = "Obtiene Sesiones de un aula y de un profesor",
            description = "Obtiene todas las sesiones de un aula"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Schedule encontrado",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CourseGroupDto.class)
                    )
            ),
            @ApiResponse(responseCode = "404", description = "Schedule no encontrado")
    })
    public ResponseEntity<List<GroupSessionDto>> getSessionsByClassroomAndTeacher(
            @RequestParam Long teacherId,
            @RequestParam String classroom) {
        log.debug("Admin consultando el horario de un aula y profesor: {}", teacherId);
        List<GroupSessionDto> weeklySchedule = courseGroupService.getScheduleByTeacherAndClassroom(teacherId,classroom);
        return ResponseEntity.ok(weeklySchedule);
    }

    /**
     * Busca solicitudes de grupo con filtros avanzados.
     * Permite combinar múltiples criterios de búsqueda.
     *
     * @param status Estado de la solicitud (PENDING, APPROVED, REJECTED)
     * @param studentId ID del estudiante (opcional)
     * @param subjectId ID de la asignatura (opcional)
     * @param fromDate Fecha inicial del rango (opcional)
     * @param toDate Fecha final del rango (opcional)
     * @return Lista de solicitudes que cumplen los criterios
     */
    @GetMapping("/group-requests/search")
    @Operation(
            summary = "Búsqueda avanzada de solicitudes",
            description = "Busca solicitudes de grupo aplicando múltiples filtros. " +
                    "Todos los parámetros son opcionales y se combinan con AND."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Búsqueda completada",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GroupRequestDto.class)
                    )
            )
    })
    public ResponseEntity<List<GroupRequestDto>> searchGroupRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.debug("Búsqueda de solicitudes con filtros - status: {}, studentId: {}, subjectId: {}",
                status, studentId, subjectId);

        GroupRequestSearchCriteriaDto criteria = GroupRequestSearchCriteriaDto.builder()
                .status(status != null ? RequestStatus.valueOf(status) : null)
                .studentId(studentId)
                .subjectId(subjectId)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();

        List<GroupRequestDto> results = groupRequestService.searchRequests(criteria);
        return ResponseEntity.ok(results);
    }

    // ========== DTOs INTERNOS PARA DOCUMENTACIÓN ==========

    /**
     * DTO interno para respuestas de error genéricas.
     */
    @Schema(description = "Respuesta de error genérica")
    private static class ErrorResponse {
        @Schema(description = "Código de error", example = "ADMIN_001")
        private String code;

        @Schema(description = "Mensaje de error", example = "No se puede eliminar el recurso")
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
}