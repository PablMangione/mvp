package com.acainfo.mvp.controller;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.groupsession.*;
import com.acainfo.mvp.service.GroupSessionService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para gestión administrativa de sesiones de grupo.
 * Todos los endpoints requieren rol ADMIN.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/sessions")
@RequiredArgsConstructor
@Tag(name = "Admin - sessions", description = "Gestión administrativa de sesiones")
@SecurityRequirement(name = "bearerAuth")
public class SessionGroupAdminController {

    private final GroupSessionService groupSessionService;

    /**
     * Obtiene todas las sesiones con paginación.
     *
     * @param pageable parámetros de paginación
     * @return página de sesiones
     */
    @GetMapping
    public ResponseEntity<Page<GroupSessionDto>> getAllSessions(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin consultando todas las sesiones, página: {}", pageable.getPageNumber());

        Page<GroupSessionDto> sessions = groupSessionService.getAllSessions(pageable);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtiene una sesión por ID con información detallada.
     *
     * @param id ID de la sesión
     * @return sesión detallada
     */
    @GetMapping("/{id}")
    public ResponseEntity<GroupSessionDetailDto> getSessionById(@PathVariable Long id) {
        log.info("Admin consultando sesión ID: {}", id);

        GroupSessionDetailDto session = groupSessionService.getSessionById(id);
        return ResponseEntity.ok(session);
    }

    /**
     * Obtiene todas las sesiones de un grupo específico.
     *
     * @param groupId ID del grupo
     * @return lista de sesiones del grupo
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<GroupSessionDto>> getSessionsByGroup(@PathVariable Long groupId) {
        log.info("Admin consultando sesiones del grupo ID: {}", groupId);

        List<GroupSessionDto> sessions = groupSessionService.getSessionsByGroup(groupId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Obtiene todas las sesiones de un profesor específico.
     *
     * @param teacherId ID del profesor
     * @return lista de sesiones detalladas del profesor
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<GroupSessionDetailDto>> getSessionsByTeacher(@PathVariable Long teacherId) {
        log.info("Admin consultando sesiones del profesor ID: {}", teacherId);

        List<GroupSessionDetailDto> sessions = groupSessionService.getSessionsByTeacher(teacherId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Crea una nueva sesión.
     *
     * @param createDto datos de la nueva sesión
     * @return sesión creada
     */
    @PostMapping
    public ResponseEntity<ApiResponseDto<GroupSessionDto>> createSession(
            @Valid @RequestBody CreateGroupSessionDto createDto) {
        log.info("Admin creando nueva sesión para grupo ID: {}", createDto.getCourseGroupId());

        GroupSessionDto created = groupSessionService.createSession(createDto);

        ApiResponseDto<GroupSessionDto> response = ApiResponseDto.<GroupSessionDto>builder()
                .success(true)
                .message("Sesión creada exitosamente")
                .data(created)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza una sesión existente.
     *
     * @param id ID de la sesión
     * @param updateDto datos a actualizar
     * @return sesión actualizada
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDto<GroupSessionDto>> updateSession(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGroupSessionDto updateDto) {
        log.info("Admin actualizando sesión ID: {}", id);

        GroupSessionDto updated = groupSessionService.updateSession(id, updateDto);

        ApiResponseDto<GroupSessionDto> response = ApiResponseDto.<GroupSessionDto>builder()
                .success(true)
                .message("Sesión actualizada exitosamente")
                .data(updated)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Elimina una sesión.
     *
     * @param id ID de la sesión
     * @return confirmación de eliminación
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSession(@PathVariable Long id) {
        log.info("Admin eliminando sesión ID: {}", id);

        groupSessionService.deleteSession(id);

        ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                .success(true)
                .message("Sesión eliminada exitosamente")
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Crea múltiples sesiones para un grupo (operación batch).
     * Útil para crear horarios semanales completos.
     *
     * @param createDtos lista de sesiones a crear
     * @return lista de sesiones creadas
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponseDto<List<GroupSessionDto>>> createSessionsBatch(
            @Valid @RequestBody List<CreateGroupSessionDto> createDtos) {
        log.info("Admin creando {} sesiones en lote", createDtos.size());

        List<GroupSessionDto> created = createDtos.stream()
                .map(groupSessionService::createSession)
                .collect(java.util.stream.Collectors.toList());

        ApiResponseDto<List<GroupSessionDto>> response = ApiResponseDto.<List<GroupSessionDto>>builder()
                .success(true)
                .message(String.format("%d sesiones creadas exitosamente", created.size()))
                .data(created)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina todas las sesiones de un grupo.
     * Útil para reorganizar completamente el horario de un grupo.
     *
     * @param groupId ID del grupo
     * @return confirmación de eliminación
     */
    @DeleteMapping("/group/{groupId}")
    public ResponseEntity<ApiResponseDto<Void>> deleteSessionsByGroup(@PathVariable Long groupId) {
        log.info("Admin eliminando todas las sesiones del grupo ID: {}", groupId);

        List<GroupSessionDto> sessions = groupSessionService.getSessionsByGroup(groupId);
        sessions.forEach(session -> groupSessionService.deleteSession(session.getId()));

        ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                .success(true)
                .message(String.format("%d sesiones eliminadas del grupo", sessions.size()))
                .build();

        return ResponseEntity.ok(response);
    }
}