package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.subject.SubjectDto;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.CourseGroupMapper;
import com.acainfo.mvp.mapper.SubjectMapper;
import com.acainfo.mvp.model.*;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.DayOfWeek;
import com.acainfo.mvp.repository.*;
import com.acainfo.mvp.util.SessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de grupos de curso.
 * Maneja operaciones de consulta, creación, actualización y gestión de grupos.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class CourseGroupService {

    private final CourseGroupRepository courseGroupRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final GroupSessionRepository groupSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseGroupMapper courseGroupMapper;
    private final SessionUtils sessionUtils;

    public CourseGroupService(CourseGroupRepository courseGroupRepository,
                              SubjectRepository subjectRepository,
                              TeacherRepository teacherRepository,
                              GroupSessionRepository groupSessionRepository,
                              EnrollmentRepository enrollmentRepository,
                              CourseGroupMapper courseGroupMapper,
                              SessionUtils sessionUtils) {
        this.courseGroupRepository = courseGroupRepository;
        this.subjectRepository = subjectRepository;
        this.teacherRepository = teacherRepository;
        this.groupSessionRepository = groupSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseGroupMapper = courseGroupMapper;
        this.sessionUtils = sessionUtils;
    }

    // ========== CONSULTAS PÚBLICAS (ALUMNOS Y PROFESORES) ==========

    /**
     * Obtiene todos los grupos activos del sistema.
     * Útil para mostrar grupos disponibles para inscripción.
     */
    public List<CourseGroupDto> getActiveGroups() {
        log.debug("Obteniendo grupos activos");

        List<CourseGroup> activeGroups = courseGroupRepository.findByStatus(CourseGroupStatus.ACTIVE);
        return courseGroupMapper.toDtoList(activeGroups);
    }

    /**
     * Obtiene grupos por asignatura.
     * Permite a alumnos ver todos los grupos de una asignatura específica.
     */
    public List<CourseGroupDto> getGroupsBySubject(Long subjectId) {
        log.debug("Obteniendo grupos de la asignatura ID: {}", subjectId);

        // Verificar que la asignatura existe
        if (!subjectRepository.existsById(subjectId)) {
            throw new ResourceNotFoundException("Asignatura no encontrada con ID: " + subjectId);
        }

        List<CourseGroup> groups = courseGroupRepository.findBySubjectId(subjectId);
        return courseGroupMapper.toDtoList(groups);
    }

    /**
     * Obtiene información detallada de un grupo.
     * Incluye horarios, capacidad y disponibilidad de inscripción.
     */
    public CourseGroupDetailDto getGroupDetail(Long groupId) {
        log.debug("Obteniendo detalle del grupo ID: {}", groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        return courseGroupMapper.toDetailDto(group);
    }

    /**
     * Obtiene grupos con espacio disponible.
     * Útil para mostrar solo grupos donde es posible inscribirse.
     */
    public List<CourseGroupDto> getAvailableGroups() {
        log.debug("Obteniendo grupos con espacio disponible");

        // Usar la consulta del repositorio que ya considera la capacidad
        List<CourseGroup> availableGroups = courseGroupRepository.findAvailableGroups(30); // Capacidad por defecto
        return courseGroupMapper.toDtoList(availableGroups);
    }

    /**
     * Obtiene grupos sin profesor asignado.
     * Útil para administradores al asignar profesores.
     */
    public List<CourseGroupDto> getGroupsWithoutTeacher() {
        log.debug("Obteniendo grupos sin profesor");

        List<CourseGroup> groups = courseGroupRepository.findByTeacherIsNull();
        return courseGroupMapper.toDtoList(groups);
    }

    /**
     * Verifica si un alumno puede inscribirse en un grupo.
     * Considera estado del grupo, capacidad y si ya está inscrito.
     */
    public boolean canStudentEnroll(Long studentId, Long groupId) {
        log.debug("Verificando si estudiante {} puede inscribirse en grupo {}", studentId, groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        // Verificar estado del grupo
        if (group.getStatus() != CourseGroupStatus.ACTIVE) {
            return false;
        }

        // Verificar capacidad
        if (!group.hasCapacity()) {
            return false;
        }

        // Verificar si ya está inscrito
        return !enrollmentRepository.existsByStudentIdAndCourseGroupId(studentId, groupId);
    }

    /**
     * Obtiene un grupo por su ID.
     * Accesible solo para administradores.
     */
    public CourseGroupDto getGroupById(Long groupId) {
        validateAdminRole();
        log.debug("Obteniendo grupo ID: {}", groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        return courseGroupMapper.toDto(group);
    }

    // ========== OPERACIONES DE PROFESOR ==========

    /**
     * Obtiene los grupos asignados a un profesor.
     * Permite al profesor ver sus grupos y horarios.
     */
    public List<CourseGroupDto> getTeacherGroups(Long teacherId) {
        validateTeacherAccess(teacherId);
        log.debug("Obteniendo grupos del profesor ID: {}", teacherId);

        // Verificar que el profesor existe
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Profesor no encontrado con ID: " + teacherId);
        }

        // Obtener grupos del profesor a través de las sesiones
        List<GroupSession> sessions = groupSessionRepository.findByTeacherId(teacherId);

        // Extraer grupos únicos de las sesiones
        List<CourseGroup> groups = sessions.stream()
                .map(GroupSession::getCourseGroup)
                .distinct()
                .collect(Collectors.toList());

        return courseGroupMapper.toDtoList(groups);
    }

    /**
     * Obtiene el horario semanal de un profesor.
     * Muestra todas las sesiones organizadas por día y hora.
     */
    public List<GroupSessionDto> getTeacherSchedule(Long teacherId) {
        validateTeacherAccess(teacherId);
        log.debug("Obteniendo horario del profesor ID: {}", teacherId);

        List<GroupSession> sessions = groupSessionRepository.findByTeacherId(teacherId);

        return sessions.stream()
                .map(courseGroupMapper::toSessionDto)
                .collect(Collectors.toList());
    }

    // ========== OPERACIONES DE ADMINISTRADOR ==========

    /**
     * Crea un nuevo grupo de curso.
     * Solo accesible para administradores.
     */
    @Transactional
    public CourseGroupDto createGroup(CreateCourseGroupDto createDto) {
        validateAdminRole();
        log.info("Creando nuevo grupo para asignatura ID: {}", createDto.getSubjectId());

        // Obtener la asignatura
        Subject subject = subjectRepository.findById(createDto.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asignatura no encontrada con ID: " + createDto.getSubjectId()));

        // Obtener el profesor si se especifica
        Teacher teacher = null;
        if (createDto.getTeacherId() != null) {
            teacher = teacherRepository.findById(createDto.getTeacherId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Profesor no encontrado con ID: " + createDto.getTeacherId()));
        }

        // Crear el grupo
        CourseGroup group = courseGroupMapper.toEntity(createDto, subject, teacher);

        try {
            group = courseGroupRepository.save(group);
            log.info("Grupo creado con ID: {}", group.getId());

            // Crear las sesiones si se especificaron
            if (createDto.getSessions() != null && !createDto.getSessions().isEmpty()) {
                for (CreateGroupSessionDto sessionDto : createDto.getSessions()) {
                    createGroupSession(group.getId(), sessionDto);
                }
            }

            return courseGroupMapper.toDto(group);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear grupo", e);
            throw new ValidationException("Error al crear el grupo. Verifique los datos ingresados");
        }
    }

    /**
     * Actualiza el estado de un grupo.
     * Permite cambiar entre PLANNED, ACTIVE y CLOSED.
     */
    @Transactional
    public CourseGroupDto updateGroupStatus(Long groupId, UpdateGroupStatusDto statusDto) {
        validateAdminRole();
        log.info("Actualizando estado del grupo ID: {} a {}", groupId, statusDto.getStatus());

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        // Validar transición de estado
        validateStatusTransition(group.getStatus(), statusDto.getStatus());

        // Actualizar estado
        courseGroupMapper.updateStatus(group, statusDto);

        try {
            group = courseGroupRepository.save(group);
            log.info("Estado del grupo {} actualizado a {}", groupId, statusDto.getStatus());
            return courseGroupMapper.toDto(group);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al actualizar estado del grupo", e);
            throw new ValidationException("Error al actualizar el estado del grupo");
        }
    }

    /**
     * Asigna un profesor a un grupo.
     * Solo para grupos que no tienen profesor asignado.
     */
    @Transactional
    public CourseGroupDto assignTeacher(Long groupId, AssignTeacherDto assignDto) {
        validateAdminRole();
        log.info("Asignando profesor {} al grupo {}", assignDto.getTeacherId(), groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        // Verificar si ya tiene profesor
        if (group.getTeacher() != null) {
            throw new ValidationException("El grupo ya tiene un profesor asignado");
        }

        Teacher teacher = teacherRepository.findById(assignDto.getTeacherId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + assignDto.getTeacherId()));

        // Verificar disponibilidad del profesor (si tiene conflictos de horario)
        validateTeacherAvailability(teacher, group);

        // Asignar profesor
        courseGroupMapper.assignTeacher(group, teacher);

        try {
            group = courseGroupRepository.save(group);
            log.info("Profesor asignado exitosamente al grupo {}", groupId);
            return courseGroupMapper.toDto(group);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al asignar profesor", e);
            throw new ValidationException("Error al asignar el profesor al grupo");
        }
    }

    /**
     * Obtiene todos los grupos de un profesor.
     * Solo accesible para administradores.
     */
    public List<CourseGroupDto> getGroupsByTeacher(Long teacherId) {
        validateAdminRole();
        log.debug("Obteniendo grupos del profesor ID: {}", teacherId);

        // Verificar que el profesor existe
        if (!teacherRepository.existsById(teacherId)) {
            throw new ResourceNotFoundException("Profesor no encontrado con ID: " + teacherId);
        }

        List<CourseGroup> groups = courseGroupRepository.findByTeacherId(teacherId);
        return courseGroupMapper.toDtoList(groups);
    }

    /**
     * Crea una sesión (horario) para un grupo.
     * Define día, hora y aula para las clases.
     */
    @Transactional
    public CourseGroupDto createGroupSession(Long groupId, CreateGroupSessionDto sessionDto) {
        validateAdminRole();
        log.info("Creando sesión para grupo ID: {}", groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        // Validar horario
        validateSessionTime(sessionDto);

        // Verificar conflictos de horario
        validateSessionConflicts(group, sessionDto);

        // Crear sesión
        GroupSession session = courseGroupMapper.toSessionEntity(sessionDto, group);

        try {
            session = groupSessionRepository.save(session);
            log.info("Sesión creada con ID: {} para grupo {}", session.getId(), groupId);
            return courseGroupMapper.toDto(group);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al crear sesión", e);
            throw new ValidationException(
                    "Ya existe una sesión en ese día y hora para este grupo");
        }
    }

    /**
     * Elimina un grupo.
     * Solo se pueden eliminar grupos sin inscripciones.
     */
    @Transactional
    public void deleteGroup(Long groupId) {
        validateAdminRole();
        log.info("Eliminando grupo ID: {}", groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        // Verificar que no tenga inscripciones
        if (!group.getEnrollments().isEmpty()) {
            throw new ValidationException(
                    "No se puede eliminar el grupo porque tiene estudiantes inscritos");
        }

        // Verificar que esté en estado PLANNED
        if (group.getStatus() != CourseGroupStatus.PLANNED) {
            throw new ValidationException(
                    "Solo se pueden eliminar grupos en estado PLANNED");
        }

        try {
            courseGroupRepository.delete(group);
            log.info("Grupo eliminado: {}", groupId);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al eliminar grupo", e);
            throw new ValidationException(
                    "No se puede eliminar el grupo debido a dependencias existentes");
        }
    }

    // ========== MÉTODOS DE VALIDACIÓN PRIVADOS ==========

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
     * Valida acceso de profesor a sus propios recursos.
     */
    private void validateTeacherAccess(Long teacherId) {
        if (!sessionUtils.isTeacher()) {
            throw new ValidationException("Acceso denegado: se requiere rol de profesor");
        }

        // Verificar que el profesor accede a sus propios datos
        if (!teacherId.equals(sessionUtils.getCurrentUserId())) {
            log.warn("Profesor {} intentó acceder a datos del profesor {}",
                    sessionUtils.getCurrentUserId(), teacherId);
            throw new ValidationException("No puede acceder a información de otros profesores");
        }
    }

    /**
     * Valida transiciones de estado permitidas.
     */
    private void validateStatusTransition(CourseGroupStatus currentStatus,
                                          CourseGroupStatus newStatus) {
        // PLANNED puede cambiar a ACTIVE o CLOSED
        // ACTIVE puede cambiar a CLOSED
        // CLOSED no puede cambiar

        if (currentStatus == CourseGroupStatus.CLOSED) {
            throw new ValidationException("No se puede cambiar el estado de un grupo cerrado");
        }

        if (currentStatus == CourseGroupStatus.ACTIVE && newStatus == CourseGroupStatus.PLANNED) {
            throw new ValidationException("Un grupo activo no puede volver a estado planificado");
        }
    }

    /**
     * Valida disponibilidad del profesor para el grupo.
     */
    private void validateTeacherAvailability(Teacher teacher, CourseGroup group) {
        // Obtener horario del profesor
        List<GroupSession> teacherSessions = groupSessionRepository.findByTeacherId(teacher.getId());

        // Verificar conflictos con las sesiones del grupo
        for (GroupSession groupSession : group.getGroupSessions()) {
            for (GroupSession teacherSession : teacherSessions) {
                if (hasScheduleConflict(groupSession, teacherSession)) {
                    throw new ValidationException(
                            "El profesor tiene conflicto de horario el " +
                                    groupSession.getDayOfWeek() + " a las " +
                                    groupSession.getStartTime());
                }
            }
        }
    }

    /**
     * Valida que los horarios de una sesión sean correctos.
     */
    private void validateSessionTime(CreateGroupSessionDto sessionDto) {
        LocalTime startTime = sessionDto.getStartTime();
        LocalTime endTime = sessionDto.getEndTime();

        if (!endTime.isAfter(startTime)) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }

        // Validar horario razonable (6:00 - 22:00)
        if (startTime.isBefore(LocalTime.of(6, 0)) || endTime.isAfter(LocalTime.of(22, 0))) {
            throw new ValidationException("El horario debe estar entre las 6:00 y las 22:00");
        }
    }

    /**
     * Valida conflictos de horario al crear una sesión.
     */
    private void validateSessionConflicts(CourseGroup group, CreateGroupSessionDto sessionDto) {
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(sessionDto.getDayOfWeek());
        LocalTime startTime = sessionDto.getStartTime();
        LocalTime endTime = sessionDto.getEndTime();

        // Verificar conflictos con otras sesiones del mismo grupo
        for (GroupSession existingSession : group.getGroupSessions()) {
            if (existingSession.getDayOfWeek() == dayOfWeek) {
                if (hasTimeOverlap(startTime, endTime,
                        existingSession.getStartTime(),
                        existingSession.getEndTime())) {
                    throw new ValidationException(
                            "Ya existe una sesión en ese horario para este grupo");
                }
            }
        }

        // Si el grupo tiene profesor, verificar conflictos con su horario
        if (group.getTeacher() != null) {
            List<GroupSession> teacherSessions = groupSessionRepository
                    .findByTeacherId(group.getTeacher().getId());

            for (GroupSession teacherSession : teacherSessions) {
                if (teacherSession.getDayOfWeek() == dayOfWeek &&
                        !teacherSession.getCourseGroup().getId().equals(group.getId())) {
                    if (hasTimeOverlap(startTime, endTime,
                            teacherSession.getStartTime(),
                            teacherSession.getEndTime())) {
                        throw new ValidationException(
                                "El profesor tiene otro grupo en ese horario");
                    }
                }
            }
        }
    }

    /**
     * Verifica si dos sesiones tienen conflicto de horario.
     */
    private boolean hasScheduleConflict(GroupSession session1, GroupSession session2) {
        // Diferente día = no hay conflicto
        if (session1.getDayOfWeek() != session2.getDayOfWeek()) {
            return false;
        }

        // Mismo día, verificar solapamiento de horarios
        return hasTimeOverlap(session1.getStartTime(), session1.getEndTime(),
                session2.getStartTime(), session2.getEndTime());
    }

    /**
     * Verifica si dos rangos de tiempo se solapan.
     */
    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1,
                                   LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    // ========== MÉTODOS DE ESTADÍSTICAS (ADMIN) ==========

    /**
     * Obtiene estadísticas de un grupo.
     * Información útil para administradores.
     */
    public GroupStatsDto getGroupStats(Long groupId) {
        validateAdminRole();
        log.debug("Obteniendo estadísticas del grupo ID: {}", groupId);

        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        int enrolledStudents = group.getEnrollments().size();
        int availableSpots = group.getMaxCapacity() - enrolledStudents;
        double occupancyRate = (double) enrolledStudents / group.getMaxCapacity() * 100;

        long paidEnrollments = group.getEnrollments().stream()
                .filter(e -> e.getPaymentStatus().name().equals("PAID"))
                .count();

        return GroupStatsDto.builder()
                .groupId(groupId)
                .subjectName(group.getSubject().getName())
                .teacherName(group.getTeacher() != null ? group.getTeacher().getName() : "Sin asignar")
                .status(group.getStatus().toString())
                .enrolledStudents(enrolledStudents)
                .maxCapacity(group.getMaxCapacity())
                .availableSpots(availableSpots)
                .occupancyRate(occupancyRate)
                .totalSessions(group.getGroupSessions().size())
                .paidEnrollments((int) paidEnrollments)
                .pendingPayments(enrolledStudents - (int) paidEnrollments)
                .build();
    }

    public Subject getSubjectById(Long subjectId) {
        validateAdminRole();
        return subjectRepository.getReferenceById(subjectId);

    }
}

/**
 * DTO para estadísticas de grupo (usado internamente).
 */
@lombok.Data
@lombok.Builder
class GroupStatsDto {
    private Long groupId;
    private String subjectName;
    private String teacherName;
    private String status;
    private int enrolledStudents;
    private int maxCapacity;
    private int availableSpots;
    private double occupancyRate;
    private int totalSessions;
    private int paidEnrollments;
    private int pendingPayments;
}
