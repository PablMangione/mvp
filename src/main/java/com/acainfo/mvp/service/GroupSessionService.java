package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.groupsession.*;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.GroupSessionMapper;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.repository.CourseGroupRepository;
import com.acainfo.mvp.repository.GroupSessionRepository;
import com.acainfo.mvp.util.SessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de sesiones de grupo.
 * Maneja operaciones CRUD y validaciones de horarios.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class GroupSessionService {

    private final GroupSessionRepository groupSessionRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final GroupSessionMapper groupSessionMapper;
    private final SessionUtils sessionUtils;

    public GroupSessionService(GroupSessionRepository groupSessionRepository,
                               CourseGroupRepository courseGroupRepository,
                               GroupSessionMapper groupSessionMapper,
                               SessionUtils sessionUtils) {
        this.groupSessionRepository = groupSessionRepository;
        this.courseGroupRepository = courseGroupRepository;
        this.groupSessionMapper = groupSessionMapper;
        this.sessionUtils = sessionUtils;
    }

    // ========== OPERACIONES DE CONSULTA ==========

    /**
     * Obtiene todas las sesiones con paginación.
     * Solo accesible para administradores.
     */
    public Page<GroupSessionDto> getAllSessions(Pageable pageable) {
        validateAdminRole();
        log.debug("Obteniendo todas las sesiones, página: {}", pageable.getPageNumber());

        Page<GroupSession> sessions = groupSessionRepository.findAll(pageable);
        return sessions.map(groupSessionMapper::toDto);
    }

    /**
     * Obtiene una sesión por ID con información detallada.
     * Solo accesible para administradores.
     */
    public GroupSessionDetailDto getSessionById(Long id) {
        validateAdminRole();
        log.debug("Obteniendo sesión detallada ID: {}", id);

        GroupSession session = groupSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada con ID: " + id));

        return groupSessionMapper.toDetailDto(session);
    }

    /**
     * Obtiene todas las sesiones de un grupo específico.
     * Accesible para administradores y el profesor del grupo.
     */
    public List<GroupSessionDto> getSessionsByGroup(Long courseGroupId) {
        log.debug("Obteniendo sesiones del grupo ID: {}", courseGroupId);

        // Verificar que el grupo existe
        CourseGroup courseGroup = courseGroupRepository.findById(courseGroupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + courseGroupId));

        // Validar acceso
        if (!sessionUtils.isAdmin()) {
            // Si no es admin, verificar que sea el profesor del grupo
            if (!sessionUtils.isTeacher() ||
                    courseGroup.getTeacher() == null ||
                    !courseGroup.getTeacher().getId().equals(sessionUtils.getCurrentUserId())) {
                throw new ValidationException("No tiene permisos para ver las sesiones de este grupo");
            }
        }

        List<GroupSession> sessions = groupSessionRepository.findByCourseGroupId(courseGroupId);
        return groupSessionMapper.toDtoList(sessions);
    }

    /**
     * Obtiene sesiones por profesor.
     * Usado para mostrar el horario del profesor.
     */
    public List<GroupSessionDetailDto> getSessionsByTeacher(Long teacherId) {
        validateTeacherAccess(teacherId);
        log.debug("Obteniendo sesiones del profesor ID: {}", teacherId);

        List<GroupSession> sessions = groupSessionRepository.findByTeacherId(teacherId);
        return groupSessionMapper.toDetailDtoList(sessions);
    }

    // ========== OPERACIONES DE CREACIÓN ==========

    /**
     * Crea una nueva sesión para un grupo.
     * Solo accesible para administradores.
     */
    @Transactional
    public GroupSessionDto createSession(CreateGroupSessionDto createDto) {
        validateAdminRole();
        log.info("Creando nueva sesión para grupo ID: {}", createDto.getCourseGroupId());

        // Obtener el grupo
        CourseGroup courseGroup = courseGroupRepository.findById(createDto.getCourseGroupId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + createDto.getCourseGroupId()));

        // Validar que el grupo no esté cerrado
        if ("CLOSED".equals(courseGroup.getStatus().name())) {
            throw new ValidationException("No se pueden agregar sesiones a un grupo cerrado");
        }

        // Validar horario
        validateSessionTime(createDto.getStartTime(), createDto.getEndTime());

        // Verificar conflictos de horario para el mismo grupo
        validateNoTimeConflict(createDto, courseGroup.getId(), null);

        // Crear la sesión
        GroupSession session = groupSessionMapper.toEntity(createDto, courseGroup);

        try {
            session = groupSessionRepository.save(session);
            log.info("Sesión creada con ID: {}", session.getId());
            return groupSessionMapper.toDto(session);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear sesión", e);
            throw new ValidationException("Ya existe una sesión con ese horario para el grupo");
        }
    }

    // ========== OPERACIONES DE ACTUALIZACIÓN ==========

    /**
     * Actualiza una sesión existente.
     * Solo accesible para administradores.
     */
    @Transactional
    public GroupSessionDto updateSession(Long id, UpdateGroupSessionDto updateDto) {
        validateAdminRole();
        log.info("Actualizando sesión ID: {}", id);

        GroupSession session = groupSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada con ID: " + id));

        // Validar que el grupo no esté cerrado
        if ("CLOSED".equals(session.getCourseGroup().getStatus().name())) {
            throw new ValidationException("No se pueden modificar sesiones de un grupo cerrado");
        }

        // Validar nuevo horario si se está actualizando
        if (updateDto.getStartTime() != null || updateDto.getEndTime() != null) {
            LocalTime startTime = updateDto.getStartTime() != null ?
                    updateDto.getStartTime() : session.getStartTime();
            LocalTime endTime = updateDto.getEndTime() != null ?
                    updateDto.getEndTime() : session.getEndTime();

            validateSessionTime(startTime, endTime);
        }

        // Validar conflictos si se cambia día u horario
        if (updateDto.getDayOfWeek() != null ||
                updateDto.getStartTime() != null ||
                updateDto.getEndTime() != null) {

            CreateGroupSessionDto tempDto = CreateGroupSessionDto.builder()
                    .courseGroupId(session.getCourseGroup().getId())
                    .dayOfWeek(updateDto.getDayOfWeek() != null ?
                            updateDto.getDayOfWeek() : session.getDayOfWeek().toString())
                    .startTime(updateDto.getStartTime() != null ?
                            updateDto.getStartTime() : session.getStartTime())
                    .endTime(updateDto.getEndTime() != null ?
                            updateDto.getEndTime() : session.getEndTime())
                    .build();

            validateNoTimeConflict(tempDto, session.getCourseGroup().getId(), session.getId());
        }

        // Actualizar la sesión
        groupSessionMapper.updateFromDto(session, updateDto);

        try {
            session = groupSessionRepository.save(session);
            log.info("Sesión actualizada ID: {}", id);
            return groupSessionMapper.toDto(session);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar sesión", e);
            throw new ValidationException("Error al actualizar la sesión. Verifique los datos ingresados");
        }
    }

    // ========== OPERACIONES DE ELIMINACIÓN ==========

    /**
     * Elimina una sesión.
     * Solo accesible para administradores.
     */
    @Transactional
    public void deleteSession(Long id) {
        validateAdminRole();
        log.info("Eliminando sesión ID: {}", id);

        GroupSession session = groupSessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesión no encontrada con ID: " + id));

        // Validar que el grupo no esté cerrado
        if ("CLOSED".equals(session.getCourseGroup().getStatus().name())) {
            throw new ValidationException("No se pueden eliminar sesiones de un grupo cerrado");
        }

        groupSessionRepository.delete(session);
        log.info("Sesión eliminada exitosamente");
    }

    // ========== MÉTODOS DE VALIDACIÓN ==========

    /**
     * Valida que el usuario actual sea administrador.
     */
    private void validateAdminRole() {
        if (!sessionUtils.isAdmin()) {
            log.warn("Intento de acceso a función de admin por usuario: {}",
                    sessionUtils.getCurrentUserEmail());
            throw new ValidationException("No tiene permisos para realizar esta operación");
        }
    }

    /**
     * Valida acceso a información del profesor.
     */
    private void validateTeacherAccess(Long teacherId) {
        if (!sessionUtils.isAdmin() &&
                (!sessionUtils.isTeacher() || !sessionUtils.getCurrentUserId().equals(teacherId))) {
            throw new ValidationException("No tiene permisos para acceder a esta información");
        }
    }

    /**
     * Valida que el horario de la sesión sea correcto.
     */
    private void validateSessionTime(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new ValidationException("Los horarios de inicio y fin son obligatorios");
        }

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }

        // Validar duración mínima (30 minutos)
        if (endTime.minusMinutes(30).isAfter(startTime)) {
            throw new ValidationException("La duración mínima de una sesión es de 30 minutos");
        }

        // Validar duración máxima (4 horas)
        if (endTime.minusHours(4).isAfter(startTime)) {
            throw new ValidationException("La duración máxima de una sesión es de 4 horas");
        }
    }

    /**
     * Valida que no haya conflictos de horario para grupo, aula y profesor.
     */
    private void validateNoTimeConflict(CreateGroupSessionDto dto, Long courseGroupId, Long excludeSessionId) {
        // Obtener el grupo para acceder al profesor
        CourseGroup courseGroup = courseGroupRepository.findById(courseGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        // 1. Validar conflictos del grupo
        validateGroupTimeConflict(dto, courseGroupId, excludeSessionId);

        // 2. Validar conflictos del aula (si se especifica)
        if (dto.getClassroom() != null && !dto.getClassroom().trim().isEmpty()) {
            validateClassroomConflict(dto, excludeSessionId);
        }

        // 3. Validar conflictos del profesor (si el grupo tiene profesor asignado)
        if (courseGroup.getTeacher() != null) {
            validateTeacherConflict(dto, courseGroup.getTeacher().getId(), excludeSessionId);
        }
    }

    /**
     * Valida conflictos de horario para el grupo.
     */
    private void validateGroupTimeConflict(CreateGroupSessionDto dto, Long courseGroupId, Long excludeSessionId) {
        List<GroupSession> existingSessions = groupSessionRepository.findByCourseGroupId(courseGroupId);

        for (GroupSession existing : existingSessions) {
            if (existing.getId().equals(excludeSessionId)) {
                continue;
            }

            if (existing.getDayOfWeek().toString().equals(dto.getDayOfWeek()) &&
                    hasTimeOverlap(dto.getStartTime(), dto.getEndTime(),
                            existing.getStartTime(), existing.getEndTime())) {
                throw new ValidationException(
                        "Existe un conflicto de horario con otra sesión del grupo");
            }
        }
    }

    /**
     * Valida conflictos de horario para el aula.
     */
    private void validateClassroomConflict(CreateGroupSessionDto dto, Long excludeSessionId) {
        // Necesitamos agregar un método en el repositorio para buscar por aula, día y horario
        List<GroupSession> sessionsInClassroom = groupSessionRepository
                .findByClassroomAndDayOfWeek(dto.getClassroom(),
                        com.acainfo.mvp.model.enums.DayOfWeek.valueOf(dto.getDayOfWeek()));

        for (GroupSession existing : sessionsInClassroom) {
            if (existing.getId().equals(excludeSessionId)) {
                continue;
            }

            if (hasTimeOverlap(dto.getStartTime(), dto.getEndTime(),
                    existing.getStartTime(), existing.getEndTime())) {
                throw new ValidationException(String.format(
                        "El aula %s ya está ocupada en ese horario por la asignatura %s",
                        dto.getClassroom(),
                        existing.getCourseGroup().getSubject().getName()));
            }
        }
    }

    /**
     * Valida conflictos de horario para el profesor.
     */
    private void validateTeacherConflict(CreateGroupSessionDto dto, Long teacherId, Long excludeSessionId) {
        List<GroupSession> teacherSessions = groupSessionRepository.findByTeacherId(teacherId);

        for (GroupSession existing : teacherSessions) {
            if (existing.getId().equals(excludeSessionId)) {
                continue;
            }

            if (existing.getDayOfWeek().toString().equals(dto.getDayOfWeek()) &&
                    hasTimeOverlap(dto.getStartTime(), dto.getEndTime(),
                            existing.getStartTime(), existing.getEndTime())) {
                throw new ValidationException(String.format(
                        "El profesor ya tiene clase de %s en ese horario",
                        existing.getCourseGroup().getSubject().getName()));
            }
        }
    }

    /**
     * Verifica si dos rangos de tiempo se solapan.
     */
    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return !(end1.isBefore(start2) || end1.equals(start2) ||
                start1.isAfter(end2) || start1.equals(end2));
    }
}