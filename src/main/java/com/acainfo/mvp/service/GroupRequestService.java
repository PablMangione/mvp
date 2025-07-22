package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.grouprequest.*;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.GroupRequestMapper;
import com.acainfo.mvp.model.GroupRequest;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.enums.RequestStatus;
import com.acainfo.mvp.repository.GroupRequestRepository;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.SubjectRepository;
import com.acainfo.mvp.util.SessionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Servicio para gestión de solicitudes de creación de grupos.
 * Maneja la lógica de negocio para las solicitudes de los estudiantes
 * cuando no existen grupos activos para una asignatura.
 *
 * Iteración 1: Funcionalidad básica de solicitud por parte del alumno.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupRequestService {

    private final GroupRequestRepository groupRequestRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final GroupRequestMapper groupRequestMapper;
    private final SessionUtils sessionUtils;

    /**
     * Crea una nueva solicitud de grupo por parte de un estudiante.
     * El estudiante debe estar autenticado y la asignatura debe existir.
     * No se permite más de una solicitud pendiente por estudiante y asignatura.
     *
     * @param dto Datos de la solicitud
     * @return Respuesta con el resultado de la operación
     */
    @Transactional
    public GroupRequestResponseDto createGroupRequest(CreateGroupRequestDto dto) {
        log.info("Procesando solicitud de grupo para asignatura ID: {}", dto.getSubjectId());

        try {
            // Validar que el usuario sea un estudiante autenticado
            if (!sessionUtils.isStudent()) {
                throw new ValidationException("Solo los estudiantes pueden solicitar la creación de grupos");
            }

            // Obtener el estudiante actual desde la sesión
            Long studentId = sessionUtils.getCurrentUserId();
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Estudiante no encontrado"));

            // Verificar que la asignatura existe
            Subject subject = subjectRepository.findById(dto.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada con ID: " + dto.getSubjectId()));

            // Verificar que el estudiante esté en la carrera de la asignatura
            if (!student.getMajor().equals(subject.getMajor())) {
                throw new ValidationException("La asignatura no pertenece a tu carrera");
            }

            // Verificar que no exista ya una solicitud pendiente
            boolean existePendiente = groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(
                    studentId, dto.getSubjectId(), RequestStatus.PENDING);

            if (existePendiente) {
                throw new ValidationException("Ya tienes una solicitud pendiente para esta asignatura");
            }

            // Crear la nueva solicitud
            GroupRequest groupRequest = groupRequestMapper.toEntity(dto, student, subject);
            groupRequest = groupRequestRepository.save(groupRequest);

            // Contar el total de solicitudes pendientes para la asignatura
            int totalRequests = groupRequestRepository.findByStatus(RequestStatus.PENDING).stream()
                    .filter(req -> req.getSubject().getId().equals(dto.getSubjectId()))
                    .toList()
                    .size();

            log.info("Solicitud de grupo creada exitosamente. ID: {}, Total solicitudes para la asignatura: {}",
                    groupRequest.getId(), totalRequests);

            return groupRequestMapper.toResponseDto(
                    groupRequest,
                    true,
                    "Solicitud creada exitosamente. El administrador revisará tu solicitud.",
                    totalRequests
            );

        } catch (ValidationException | ResourceNotFoundException e) {
            log.error("Error de validación al crear solicitud: {}", e.getMessage());
            return groupRequestMapper.toErrorResponse(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al crear solicitud de grupo", e);
            return groupRequestMapper.toErrorResponse("Error al procesar la solicitud");
        }
    }

    /**
     * Obtiene las solicitudes de grupo del estudiante actual.
     * Solo muestra las solicitudes del estudiante autenticado.
     *
     * @return Lista de solicitudes del estudiante
     */
    public List<GroupRequestDto> getMyGroupRequests() {
        log.debug("Consultando solicitudes del estudiante actual");

        if (!sessionUtils.isStudent()) {
            throw new ValidationException("Solo los estudiantes pueden consultar sus solicitudes");
        }

        Long studentId = sessionUtils.getCurrentUserId();

        // Por ahora, recuperamos todas y filtramos en memoria
        // En futuras iteraciones se puede optimizar con una query específica
        List<GroupRequest> allRequests = groupRequestRepository.findAll();

        List<GroupRequest> myRequests = allRequests.stream()
                .filter(req -> req.getStudent().getId().equals(studentId))
                .toList();

        log.debug("Encontradas {} solicitudes para el estudiante {}", myRequests.size(), studentId);

        return groupRequestMapper.toDtoList(myRequests);
    }

    /**
     * Obtiene todas las solicitudes pendientes (solo para administradores).
     * En la iteración 1, los administradores pueden ver las solicitudes pendientes
     * para decidir si crear nuevos grupos.
     *
     * @return Lista de solicitudes pendientes
     */
    public List<GroupRequestDto> getPendingRequests() {
        log.debug("Consultando solicitudes pendientes");

        if (!sessionUtils.isAdmin()) {
            throw new ValidationException("Solo los administradores pueden ver todas las solicitudes pendientes");
        }

        List<GroupRequest> pendingRequests = groupRequestRepository.findByStatus(RequestStatus.PENDING);

        log.info("Encontradas {} solicitudes pendientes", pendingRequests.size());

        return groupRequestMapper.toDtoList(pendingRequests);
    }

    /**
     * Actualiza el estado de una solicitud (solo para administradores).
     * En la iteración 1, permite aprobar o rechazar solicitudes básicamente.
     *
     * @param requestId ID de la solicitud
     * @param dto Datos de actualización
     * @return DTO con la solicitud actualizada
     */
    @Transactional
    public GroupRequestDto updateRequestStatus(Long requestId, UpdateRequestStatusDto dto) {
        log.info("Actualizando estado de solicitud ID: {} a {}", requestId, dto.getStatus());

        if (!sessionUtils.isAdmin()) {
            throw new ValidationException("Solo los administradores pueden actualizar el estado de las solicitudes");
        }

        GroupRequest groupRequest = groupRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

        // Validar que la solicitud esté pendiente
        if (groupRequest.getStatus() != RequestStatus.PENDING) {
            throw new ValidationException("Solo se pueden actualizar solicitudes pendientes");
        }

        // Actualizar el estado
        groupRequestMapper.updateStatus(groupRequest, dto);
        groupRequest = groupRequestRepository.save(groupRequest);

        log.info("Estado de solicitud actualizado exitosamente");

        return groupRequestMapper.toDto(groupRequest);
    }

    /**
     * Verifica si un estudiante puede solicitar un grupo para una asignatura.
     * Útil para mostrar/ocultar el botón de solicitud en la UI.
     *
     * @param subjectId ID de la asignatura
     * @return true si puede solicitar, false en caso contrario
     */
    public boolean canRequestGroup(Long subjectId) {
        if (!sessionUtils.isStudent()) {
            return false;
        }

        Long studentId = sessionUtils.getCurrentUserId();

        // Verificar si ya tiene una solicitud pendiente
        return !groupRequestRepository.existsByStudentIdAndSubjectIdAndStatus(
                studentId, subjectId, RequestStatus.PENDING);
    }

    /**
     * Cancela una solicitud de grupo del estudiante.
     * Solo el estudiante dueño puede cancelar su propia solicitud.
     * Solo se pueden cancelar solicitudes en estado PENDING.
     *
     * @param requestId ID de la solicitud a cancelar
     * @return Respuesta con el resultado de la operación
     */
    @Transactional
    public ApiResponseDto<Void> cancelGroupRequest(Long requestId) {
        log.info("Procesando cancelación de solicitud ID: {}", requestId);

        try {
            // Validar que el usuario sea un estudiante autenticado
            if (!sessionUtils.isStudent()) {
                throw new ValidationException("Solo los estudiantes pueden cancelar solicitudes");
            }

            // Obtener la solicitud
            GroupRequest groupRequest = groupRequestRepository.findById(requestId)
                    .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada con ID: " + requestId));

            log.info("Está en la base de datos: {}", requestId);

            // Verificar que la solicitud pertenezca al estudiante actual
            Long currentStudentId = sessionUtils.getCurrentUserId();
            if (!groupRequest.getStudent().getId().equals(currentStudentId)) {
                throw new ValidationException("No tienes permisos para cancelar esta solicitud");
            }

            // Verificar que la solicitud esté pendiente
            if (groupRequest.getStatus() != RequestStatus.PENDING) {
                throw new ValidationException("Solo se pueden cancelar solicitudes pendientes");
            }

            // Eliminar la solicitud
            groupRequestRepository.delete(groupRequest);

            log.info("Solicitud de grupo ID: {} cancelada exitosamente", requestId);

            return ApiResponseDto.success(
                    null,
                    "Solicitud cancelada exitosamente"
            );

        } catch (ValidationException | ResourceNotFoundException e) {
            log.error("Error al cancelar solicitud: {}", e.getMessage());
            return ApiResponseDto.error(e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado al cancelar solicitud de grupo", e);
            return ApiResponseDto.error("Error al cancelar la solicitud");
        }
    }

    /**
     * Obtiene estadísticas de solicitudes por asignatura (para administradores).
     * Útil para que el admin vea qué asignaturas tienen más demanda.
     *
     * @param subjectId ID de la asignatura
     * @return Estadísticas básicas de solicitudes
     */
    public GroupRequestStatsDto getRequestStatsBySubject(Long subjectId) {
        log.debug("Obteniendo estadísticas de solicitudes para asignatura ID: {}", subjectId);

        if (!sessionUtils.isAdmin()) {
            throw new ValidationException("Solo los administradores pueden ver estadísticas de solicitudes");
        }

        // Verificar que la asignatura existe
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada"));

        // Obtener todas las solicitudes y filtrar por asignatura
        List<GroupRequest> allRequests = groupRequestRepository.findAll();
        List<GroupRequest> subjectRequests = allRequests.stream()
                .filter(req -> req.getSubject().getId().equals(subjectId))
                .toList();

        // Contar por estado
        long pending = subjectRequests.stream()
                .filter(req -> req.getStatus() == RequestStatus.PENDING)
                .count();

        long approved = subjectRequests.stream()
                .filter(req -> req.getStatus() == RequestStatus.APPROVED)
                .count();

        long rejected = subjectRequests.stream()
                .filter(req -> req.getStatus() == RequestStatus.REJECTED)
                .count();

        return GroupRequestStatsDto.builder()
                .subjectId(subjectId)
                .subjectName(subject.getName())
                .totalRequests(subjectRequests.size())
                .pendingRequests((int) pending)
                .approvedRequests((int) approved)
                .rejectedRequests((int) rejected)
                .build();
    }

    /**
     * Obtiene todas las solicitudes para una asignatura específica.
     * Solo accesible para administradores.
     */
    public List<GroupRequestDto> getRequestsBySubject(Long subjectId) {
        log.debug("Obteniendo solicitudes para asignatura ID: {}", subjectId);

        if (!sessionUtils.isAdmin()) {
            throw new ValidationException("Solo los administradores pueden acceder a esta información");
        }

        // Verificar que la asignatura existe
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Asignatura no encontrada con ID: " + subjectId);
        }

        List<GroupRequest> requests = groupRequestRepository.findBySubjectId(subjectId);
        return groupRequestMapper.toDtoList(requests);
    }

    /**
     * Obtiene todas las solicitudes de un estudiante específico.
     * Solo accesible para administradores.
     */
    public List<GroupRequestDto> getRequestsByStudent(Long studentId) {
        log.debug("Obteniendo solicitudes del estudiante ID: {}", studentId);

        if (!sessionUtils.isAdmin()) {
            throw new ValidationException("Solo administradores pueden acceder a esta información");
        }

        // Verificar que el estudiante existe
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Estudiante no encontrado con ID: " + studentId);
        }

        List<GroupRequest> requests = groupRequestRepository.findByStudentId(studentId);
        return groupRequestMapper.toDtoList(requests);
    }

    /**
     * Busca solicitudes según criterios múltiples.
     * Solo accesible para administradores.
     */
    public List<GroupRequestDto> searchRequests(GroupRequestSearchCriteriaDto criteria) {
        log.debug("Buscando solicitudes con criterios: {}", criteria);

        if (!sessionUtils.isAdmin()) {
            throw new ValidationException("Solo administradores pueden realizar búsquedas");
        }

        // El servicio solo se preocupa por la lógica de negocio
        // La construcción de la consulta está encapsulada en el repositorio
        List<GroupRequest> requests = groupRequestRepository.searchByCriteria(
                criteria.getStatus(),
                criteria.getStudentId(),
                criteria.getSubjectId(),
                criteria.getFromDate(),
                criteria.getToDate()
        );

        return groupRequestMapper.toDtoList(requests);
    }

}
