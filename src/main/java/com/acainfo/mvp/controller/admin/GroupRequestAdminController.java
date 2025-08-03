package com.acainfo.mvp.controller.admin;

import com.acainfo.mvp.dto.grouprequest.*;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.GroupRequestRepository;
import com.acainfo.mvp.service.GroupRequestService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para operaciones administrativas sobre solicitudes de grupos.
 * Gestiona las solicitudes de creación de grupos por parte de estudiantes.
 * Requiere rol de administrador para todas las operaciones.
 *
 * @author Sistema AcaInfo
 * @version 1.0
 * @since 2024
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/group-requests")
@RequiredArgsConstructor
@Tag(name = "Admin - Solicitudes de Grupos", description = "Gestión administrativa de solicitudes de creación de grupos")
@SecurityRequirement(name = "bearerAuth")
public class GroupRequestAdminController {

    private final GroupRequestService groupRequestService;
    private final GroupRequestRepository groupRequestRepository;

    /**
     * Obtiene todas las solicitudes con paginación.
     * Permite ordenar por diferentes campos.
     *
     * @param pageable Configuración de paginación y ordenamiento
     * @return Página de solicitudes
     */
    @GetMapping
    @Operation(
            summary = "Obtener todas las solicitudes",
            description = "Devuelve todas las solicitudes del sistema con paginación. " +
                    "Permite ordenar por: id, status, createdAt."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitudes obtenidas exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<Page<GroupRequestDto>> getAllRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @Parameter(description = "Configuración de paginación") Pageable pageable) {

        log.debug("Admin consultando todas las solicitudes - página: {}", pageable.getPageNumber());

        // TODO: El servicio necesita implementar getAllRequests(Pageable)
        // Por ahora devolvemos una página vacía
        Page<GroupRequestDto> requests = Page.empty(pageable);

        log.info("Retornando {} solicitudes", requests.getTotalElements());
        return ResponseEntity.ok(requests);
    }

    /**
     * Obtiene todas las solicitudes pendientes de revisión.
     * Útil para que el admin vea qué solicitudes necesitan atención.
     *
     * @return Lista de solicitudes pendientes
     */
    @GetMapping("/pending")
    @Operation(
            summary = "Obtener solicitudes pendientes",
            description = "Devuelve todas las solicitudes en estado PENDING que requieren revisión del administrador."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitudes pendientes obtenidas exitosamente"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<GroupRequestDto>> getPendingRequests() {

        log.debug("Admin consultando solicitudes pendientes");

        List<GroupRequestDto> pendingRequests = groupRequestService.getPendingRequests();

        log.info("Retornando {} solicitudes pendientes", pendingRequests.size());
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Obtiene el detalle de una solicitud específica.
     *
     * @param requestId ID de la solicitud
     * @return Detalle de la solicitud
     */
    @GetMapping("/{requestId}")
    @Operation(
            summary = "Obtener detalle de solicitud",
            description = "Devuelve información completa de una solicitud específica."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitud encontrada"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<GroupRequestDto> getRequestDetail(@PathVariable Long requestId) {

        log.debug("Admin consultando detalle de solicitud ID: {}", requestId);

        // TODO: El servicio necesita implementar getRequestById(Long)
        // Por ahora retornamos null

        log.info("Retornando detalle de solicitud ID: {}", requestId);
        return ResponseEntity.ok(null);
    }

    /**
     * Actualiza el estado de una solicitud.
     * Permite aprobar o rechazar solicitudes pendientes.
     *
     * @param requestId ID de la solicitud
     * @param statusDto Nuevo estado y comentarios
     * @return Solicitud actualizada
     */
    @PutMapping("/{requestId}/status")
    @Operation(
            summary = "Actualizar estado de solicitud",
            description = "Cambia el estado de una solicitud pendiente a APPROVED o REJECTED. " +
                    "Solo se pueden actualizar solicitudes en estado PENDING."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "La solicitud no está en estado PENDING"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<GroupRequestDto> updateRequestStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestStatusDto statusDto) {

        log.debug("Admin actualizando estado de solicitud {} a {}", requestId, statusDto.getStatus());

        GroupRequestDto updatedRequest = groupRequestService.updateRequestStatus(requestId, statusDto);

        log.info("Estado de solicitud {} actualizado a {}", requestId, statusDto.getStatus());
        return ResponseEntity.ok(updatedRequest);
    }

    /**
     * Obtiene estadísticas de solicitudes por asignatura.
     * Muestra la demanda de grupos para cada asignatura.
     *
     * @param subjectId ID de la asignatura
     * @return Estadísticas de solicitudes
     */
    @GetMapping("/stats/by-subject/{subjectId}")
    @Operation(
            summary = "Obtener estadísticas por asignatura",
            description = "Devuelve estadísticas de solicitudes para una asignatura específica, " +
                    "incluyendo total, pendientes, aprobadas y rechazadas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estadísticas obtenidas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asignatura no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<GroupRequestStatsDto> getRequestStatsBySubject(@PathVariable Long subjectId) {

        log.debug("Admin consultando estadísticas de solicitudes para asignatura ID: {}", subjectId);

        GroupRequestStatsDto stats = groupRequestService.getRequestStatsBySubject(subjectId);

        log.info("Retornando estadísticas para asignatura {}: {} solicitudes totales",
                subjectId, stats.getTotalRequests());
        return ResponseEntity.ok(stats);
    }

    /**
     * Obtiene solicitudes filtradas por estado.
     *
     * @param status Estado a filtrar (PENDING, APPROVED, REJECTED)
     * @return Lista de solicitudes con el estado especificado
     */
    @GetMapping("/by-status/{status}")
    @Operation(
            summary = "Obtener solicitudes por estado",
            description = "Filtra solicitudes por su estado: PENDING, APPROVED o REJECTED."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitudes obtenidas exitosamente"),
            @ApiResponse(responseCode = "400", description = "Estado inválido"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<GroupRequestDto>> getRequestsByStatus(
            @PathVariable
            @Parameter(description = "Estado de la solicitud", example = "PENDING")
            String status) {

        log.debug("Admin consultando solicitudes con estado: {}", status);

        // Validar que el estado es válido
        RequestStatus requestStatus;
        try {
            requestStatus = RequestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Estado inválido: {}", status);
            return ResponseEntity.badRequest().build();
        }

        // Por ahora usamos getPendingRequests si el estado es PENDING
        List<GroupRequestDto> requests = groupRequestService.getRequestsByStatus(requestStatus);

        log.info("Retornando {} solicitudes con estado {}", requests.size(), status);
        return ResponseEntity.ok(requests);
    }

    /**
     * Obtiene solicitudes por estudiante.
     * Útil para ver el historial de solicitudes de un estudiante específico.
     *
     * @param studentId ID del estudiante
     * @return Lista de solicitudes del estudiante
     */
    @GetMapping("/by-student/{studentId}")
    @Operation(
            summary = "Obtener solicitudes por estudiante",
            description = "Devuelve todas las solicitudes realizadas por un estudiante específico."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solicitudes obtenidas exitosamente"),
            @ApiResponse(responseCode = "404", description = "Estudiante no encontrado"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<List<GroupRequestDto>> getRequestsByStudent(@PathVariable Long studentId) {

        log.debug("Admin consultando solicitudes del estudiante ID: {}", studentId);

        List<GroupRequestDto> requests = groupRequestService.getRequestsByStudent(studentId);

        log.info("Retornando {} solicitudes para el estudiante {}", requests.size(), studentId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Elimina una solicitud.
     * Solo se pueden eliminar solicitudes rechazadas o muy antiguas.
     *
     * @param requestId ID de la solicitud a eliminar
     * @return Confirmación de eliminación
     */
    @DeleteMapping("/{requestId}")
    @Operation(
            summary = "Eliminar solicitud",
            description = "Elimina una solicitud del sistema. " +
                    "Solo se pueden eliminar solicitudes REJECTED o muy antiguas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Solicitud eliminada exitosamente"),
            @ApiResponse(responseCode = "400", description = "La solicitud no se puede eliminar"),
            @ApiResponse(responseCode = "404", description = "Solicitud no encontrada"),
            @ApiResponse(responseCode = "403", description = "Sin permisos de administrador")
    })
    public ResponseEntity<Void> deleteRequest(@PathVariable Long requestId) {

        log.debug("Admin eliminando solicitud ID: {}", requestId);

        groupRequestService.deleteRequest(requestId);

        log.info("Solicitud {} eliminada exitosamente", requestId);
        return ResponseEntity.noContent().build();
    }
}