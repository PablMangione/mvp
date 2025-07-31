package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.service.CourseGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones administrativas sobre grupos de curso.
 * Gestiona la creación, actualización y consulta de grupos.
 * Requiere rol de administrador para todas las operaciones.
 *
 * @author Sistema AcaInfo
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
@Tag(name = "Admin - Grupos", description = "Gestión administrativa de grupos de curso")
@SecurityRequirement(name = "bearerAuth")
public class CourseGroupAdminController {

    private final CourseGroupService courseGroupService;

    /**
     * Obtiene todos los grupos con paginación.
     * Permite ordenar por diferentes campos y filtrar por estado.
     *
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de grupos
     */
    @GetMapping
    @Operation(
            summary = "Obtener todos los grupos",
            description = "Devuelve todos los grupos del sistema con paginación. " +
                    "Permite ordenar por: id, status, type, price, createdAt."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupos obtenidos exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<Page<CourseGroupDto>> getAllGroups(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
            @Parameter(description = "Configuración de paginación") Pageable pageable) {

        log.debug("Admin consultando todos los grupos - página: {}", pageable.getPageNumber());

        // TODO: Implementar en el servicio el método getAllGroups(Pageable)
        // Por ahora devolvemos una página vacía
        Page<CourseGroupDto> groups = Page.empty(pageable);

        log.info("Retornando {} grupos", groups.getTotalElements());
        return ResponseEntity.ok(groups);
    }

    /**
     * Crea un nuevo grupo de curso.
     * Puede incluir o no profesor asignado y sesiones iniciales.
     *
     * @param createDto Datos del nuevo grupo
     * @return Grupo creado
     */
    @PostMapping
    @Operation(
            summary = "Crear nuevo grupo",
            description = "Crea un nuevo grupo para una asignatura. " +
                    "Opcionalmente puede incluir profesor y sesiones (horarios)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Grupo creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Asignatura o profesor no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<CourseGroupDto> createGroup(
            @Valid @RequestBody CreateCourseGroupDto createDto) {

        log.debug("Admin creando nuevo grupo para asignatura ID: {}", createDto.getSubjectId());

        CourseGroupDto createdGroup = courseGroupService.createGroup(createDto);

        log.info("Grupo creado con ID: {}", createdGroup.getSubjectId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    /**
     * Obtiene el detalle completo de un grupo.
     * Incluye información de asignatura, profesor, sesiones y capacidad.
     *
     * @param groupId ID del grupo
     * @return Detalle del grupo
     */
    @GetMapping("/{groupId}")
    @Operation(
            summary = "Obtener detalle de grupo",
            description = "Devuelve información completa del grupo incluyendo " +
                    "asignatura, profesor, sesiones y estado de inscripciones."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupo encontrado"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<CourseGroupDetailDto> getGroupDetail(
            @PathVariable Long groupId) {

        log.debug("Admin consultando detalle del grupo ID: {}", groupId);

        CourseGroupDetailDto groupDetail = courseGroupService.getGroupDetail(groupId);

        log.info("Retornando detalle del grupo ID: {}", groupId);
        return ResponseEntity.ok(groupDetail);
    }

    /**
     * Actualiza el estado de un grupo.
     * Permite cambiar entre PLANNED, ACTIVE y CLOSED siguiendo reglas de negocio.
     *
     * @param groupId ID del grupo
     * @param statusDto Nuevo estado y razón del cambio
     * @return Grupo actualizado
     */
    @PutMapping("/{groupId}/status")
    @Operation(
            summary = "Actualizar estado del grupo",
            description = "Cambia el estado del grupo. Transiciones válidas: " +
                    "PLANNED → ACTIVE → CLOSED. No se puede cambiar un grupo CLOSED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Transición de estado inválida"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<CourseGroupDto> updateGroupStatus(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateGroupStatusDto statusDto) {

        log.debug("Admin actualizando estado del grupo {} a {}", groupId, statusDto.getStatus());

        CourseGroupDto updatedGroup = courseGroupService.updateGroupStatus(groupId, statusDto);

        log.info("Estado del grupo {} actualizado a {}", groupId, statusDto.getStatus());
        return ResponseEntity.ok(updatedGroup);
    }

    /**
     * Asigna un profesor a un grupo sin profesor.
     * Verifica disponibilidad de horarios para evitar conflictos.
     *
     * @param groupId ID del grupo
     * @param assignDto Datos del profesor a asignar
     * @return Grupo actualizado con profesor
     */
    @PutMapping("/{groupId}/teacher")
    @Operation(
            summary = "Asignar profesor a grupo",
            description = "Asigna un profesor a un grupo que no tiene profesor. " +
                    "Verifica que no haya conflictos de horario."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profesor asignado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El grupo ya tiene profesor o hay conflicto de horario"),
            @ApiResponse(responseCode = "404", description = "Grupo o profesor no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<CourseGroupDto> assignTeacher(
            @PathVariable Long groupId,
            @Valid @RequestBody AssignTeacherDto assignDto) {

        log.debug("Admin asignando profesor {} al grupo {}", assignDto.getTeacherId(), groupId);

        CourseGroupDto updatedGroup = courseGroupService.assignTeacher(groupId, assignDto);

        log.info("Profesor {} asignado exitosamente al grupo {}", assignDto.getTeacherId(), groupId);
        return ResponseEntity.ok(updatedGroup);
    }

    /**
     * Crea una nueva sesión (horario) para un grupo.
     * Valida que no haya conflictos con otras sesiones del mismo grupo.
     *
     * @param groupId ID del grupo
     * @param sessionDto Datos de la sesión
     * @return Sesión creada
     */
    @PostMapping("/{groupId}/sessions")
    @Operation(
            summary = "Agregar sesión a grupo",
            description = "Agrega un nuevo horario de clase al grupo. " +
                    "Verifica que no haya conflictos con sesiones existentes."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Sesión creada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o conflicto de horario"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<CourseGroupDto> createGroupSession(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateGroupSessionDto sessionDto) {

        log.debug("Admin creando sesión para grupo {}: {} de {} a {}",
                groupId, sessionDto.getDayOfWeek(), sessionDto.getStartTime(), sessionDto.getEndTime());

        CourseGroupDto groupWithAddedSession = courseGroupService.createGroupSession(groupId, sessionDto);

        log.info("Sesión creada exitosamente para grupo {}", groupId);
        return ResponseEntity.ok(groupWithAddedSession);
    }

    /**
     * Elimina un grupo sin inscripciones.
     * Solo se pueden eliminar grupos en estado PLANNED sin estudiantes.
     *
     * @param groupId ID del grupo a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{groupId}")
    @Operation(
            summary = "Eliminar grupo",
            description = "Elimina un grupo del sistema. Solo se pueden eliminar " +
                    "grupos en estado PLANNED que no tengan estudiantes inscritos."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Grupo eliminado exitosamente"),
            @ApiResponse(responseCode = "400", description = "El grupo tiene inscripciones o no está en estado PLANNED"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<Void> deleteGroup(@PathVariable Long groupId) {

        log.debug("Admin eliminando grupo ID: {}", groupId);

        courseGroupService.deleteGroup(groupId);

        log.info("Grupo {} eliminado exitosamente", groupId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene estadísticas de un grupo específico.
     * Incluye información sobre inscripciones, pagos y ocupación.
     *
     * @param groupId ID del grupo
     * @return Estadísticas del grupo
     */
    @GetMapping("/{groupId}/stats")
    @Operation(
            summary = "Obtener estadísticas de grupo",
            description = "Devuelve estadísticas detalladas del grupo incluyendo " +
                    "ocupación, pagos pendientes y sesiones programadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Grupo no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<Object> getGroupStats(@PathVariable Long groupId) {

        log.debug("Admin consultando estadísticas del grupo ID: {}", groupId);

        // El servicio devuelve GroupStatsDto (interno)
        Object stats = courseGroupService.getGroupStats(groupId);

        log.info("Retornando estadísticas del grupo {}", groupId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene grupos sin profesor asignado.
     * Útil para identificar grupos que necesitan profesor.
     *
     * @return Lista de grupos sin profesor
     */
    @GetMapping("/without-teacher")
    @Operation(
            summary = "Obtener grupos sin profesor",
            description = "Devuelve todos los grupos que no tienen profesor asignado."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupos obtenidos exitosamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<CourseGroupDto>> getGroupsWithoutTeacher() {

        log.debug("Admin consultando grupos sin profesor asignado");

        List<CourseGroupDto> groups = courseGroupService.getGroupsWithoutTeacher();

        log.info("Retornando {} grupos sin profesor", groups.size());
        return ResponseEntity.ok(groups);
    }

    /**
     * Obtiene grupos por estado específico.
     * Permite filtrar grupos PLANNED, ACTIVE o CLOSED.
     *
     * @param status Estado a filtrar
     * @return Lista de grupos con el estado especificado
     */
    @GetMapping("/by-status/{status}")
    @Operation(
            summary = "Obtener grupos por estado",
            description = "Filtra grupos por su estado: PLANNED, ACTIVE o CLOSED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupos obtenidos exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<CourseGroupDto>> getGroupsByStatus(
            @PathVariable
            @Parameter(description = "Estado del grupo", example = "ACTIVE")
            String status) {

        log.debug("Admin consultando grupos con estado: {}", status);

        // TODO: Implementar en el servicio el método getGroupsByStatus(CourseGroupStatus)
        // Por ahora solo devolvemos grupos activos
        List<CourseGroupDto> groups = courseGroupService.getActiveGroups();

        log.info("Retornando {} grupos con estado {}", groups.size(), status);
        return ResponseEntity.ok(groups);
    }

    /**
     * Obtiene grupos por asignatura.
     * Útil para ver todos los grupos de una asignatura específica.
     *
     * @param subjectId ID de la asignatura
     * @return Lista de grupos de la asignatura
     */
    @GetMapping("/by-subject/{subjectId}")
    @Operation(
            summary = "Obtener grupos por asignatura",
            description = "Devuelve todos los grupos de una asignatura específica."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Grupos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<CourseGroupDto>> getGroupsBySubject(
            @PathVariable Long subjectId) {

        log.debug("Admin consultando grupos de la asignatura ID: {}", subjectId);

        List<CourseGroupDto> groups = courseGroupService.getGroupsBySubject(subjectId);

        log.info("Retornando {} grupos para la asignatura {}", groups.size(), subjectId);
        return ResponseEntity.ok(groups);
    }
}