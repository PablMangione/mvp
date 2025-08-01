package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.auth.ChangePasswordDto;
import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.coursegroup.CourseGroupDto;
import com.acainfo.mvp.dto.groupsession.GroupSessionDto;
import com.acainfo.mvp.dto.teacher.CreateTeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherDto;
import com.acainfo.mvp.dto.teacher.TeacherScheduleDto;
import com.acainfo.mvp.exception.auth.EmailAlreadyExistsException;
import com.acainfo.mvp.exception.auth.InvalidCredentialsException;
import com.acainfo.mvp.exception.auth.PasswordMismatchException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.mapper.CourseGroupMapper;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.repository.GroupSessionRepository;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import com.acainfo.mvp.util.SessionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de profesores.
 * Maneja operaciones sobre el perfil del profesor, horarios y grupos asignados.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final GroupSessionRepository groupSessionRepository;
    private final TeacherMapper teacherMapper;
    private final CourseGroupMapper courseGroupMapper;
    private final SessionUtils sessionUtils;
    private final CourseGroupService courseGroupService;

    public TeacherService(TeacherRepository teacherRepository,
                          StudentRepository studentRepository,
                          GroupSessionRepository groupSessionRepository,
                          TeacherMapper teacherMapper,
                          CourseGroupMapper courseGroupMapper,
                          SessionUtils sessionUtils,
                          CourseGroupService courseGroupService) {
        this.teacherRepository = teacherRepository;
        this.studentRepository = studentRepository;
        this.groupSessionRepository = groupSessionRepository;
        this.teacherMapper = teacherMapper;
        this.sessionUtils = sessionUtils;
        this.courseGroupService = courseGroupService;
        this.courseGroupMapper = courseGroupMapper;
    }

    // ========== OPERACIONES DE PERFIL DE PROFESOR ==========

    /**
     * Obtiene el perfil del profesor actual.
     * Solo puede acceder a su propio perfil.
     */
    public TeacherDto getMyProfile() {
        Long teacherId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo perfil del profesor ID: {}", teacherId);

        if (!sessionUtils.isTeacher()) {
            throw new ValidationException("Esta operación es solo para profesores");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        return teacherMapper.toDto(teacher);
    }

    /**
     * Obtiene un profesor por ID.
     * Profesores solo pueden ver su propio perfil.
     * Administradores pueden ver cualquier perfil.
     */
    public TeacherDto getTeacherById(Long teacherId) {
        log.debug("Obteniendo profesor con ID: {}", teacherId);

        // Validar acceso
        if (sessionUtils.isTeacher()) {
            validateTeacherAccess(teacherId);
        } else if (!sessionUtils.isAdmin()) {
            throw new ValidationException("No tiene permisos para ver este perfil");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        return teacherMapper.toDto(teacher);
    }

    /**
     * Actualiza el perfil del profesor.
     * Solo puede actualizar su nombre (no email).
     */
    @Transactional
    public TeacherDto updateProfile(Long teacherId, TeacherDto updateDto) {
        validateTeacherAccess(teacherId);
        log.info("Actualizando perfil del profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        // Actualizar solo campos permitidos
        teacherMapper.updateBasicInfo(teacher, updateDto);

        try {
            teacher = teacherRepository.save(teacher);
            log.info("Perfil actualizado exitosamente para profesor: {}", teacher.getEmail());
            return teacherMapper.toDto(teacher);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar perfil", e);
            throw new ValidationException("Error al actualizar el perfil");
        }
    }

    /**
     * Cambia la contraseña del profesor.
     * Requiere la contraseña actual para validación.
     */
    @Transactional
    public ApiResponseDto<Void> changePassword(Long teacherId, ChangePasswordDto changePasswordDto) {
        validateTeacherAccess(teacherId);
        log.info("Cambio de contraseña solicitado para profesor ID: {}", teacherId);

        // Validar que las contraseñas nuevas coincidan
        if (!changePasswordDto.getNewPassword().equals(changePasswordDto.getConfirmPassword())) {
            throw new PasswordMismatchException("Las contraseñas nuevas no coinciden");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        // Verificar contraseña actual
        if (!teacherMapper.passwordMatches(changePasswordDto.getCurrentPassword(),
                teacher.getPassword())) {
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        // Actualizar contraseña
        teacher.setPassword(teacherMapper.encodePassword(changePasswordDto.getNewPassword()));

        try {
            teacherRepository.save(teacher);
            log.info("Contraseña actualizada exitosamente para profesor: {}", teacher.getEmail());
            return ApiResponseDto.success(null, "Contraseña actualizada exitosamente");
        } catch (Exception e) {
            log.error("Error al cambiar contraseña", e);
            throw new ValidationException("Error al cambiar la contraseña");
        }
    }

    // ========== CONSULTAS DE HORARIOS Y GRUPOS ==========

    /**
     * Obtiene el horario semanal completo del profesor.
     * Incluye todas las sesiones de todos sus grupos.
     */
    public TeacherScheduleDto getMySchedule() {
        Long teacherId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo horario del profesor ID: {}", teacherId);

        if (!sessionUtils.isTeacher()) {
            throw new ValidationException("Esta operación es solo para profesores");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        List<GroupSession> sessions = groupSessionRepository.findByTeacherId(teacherId);
        return teacherMapper.toScheduleDto(teacher, sessions);
    }

    /**
     * Obtiene el horario de un día específico.
     * Útil para vista diaria del profesor.
     */
    public List<GroupSessionDto> getMyScheduleByDay(String dayOfWeek) {
        Long teacherId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo horario del {} para profesor ID: {}", dayOfWeek, teacherId);

        if (!sessionUtils.isTeacher()) {
            throw new ValidationException("Esta operación es solo para profesores");
        }

        // Validar día de la semana
        DayOfWeek day;
        try {
            day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Día de la semana inválido: " + dayOfWeek);
        }

        List<GroupSession> allSessions = groupSessionRepository.findByTeacherId(teacherId);

        // Filtrar por día y ordenar por hora
        List<GroupSession> daySessions = allSessions.stream()
                .filter(session -> session.getDayOfWeek().toString().equals(day.toString()))
                .sorted(Comparator.comparing(GroupSession::getStartTime))
                .collect(Collectors.toList());

        return daySessions.stream()
                .map(session -> courseGroupMapper.toSessionDto(session))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene los grupos asignados al profesor.
     * Incluye información básica de cada grupo.
     */
    public List<CourseGroupDto> getMyGroups() {
        Long teacherId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo grupos del profesor ID: {}", teacherId);

        if (!sessionUtils.isTeacher()) {
            throw new ValidationException("Esta operación es solo para profesores");
        }

        return courseGroupService.getTeacherGroups(teacherId);
    }

    /**
     * Obtiene estadísticas del profesor.
     * Incluye número de grupos, estudiantes totales, etc.
     */
    public TeacherStatsDto getMyStats() {
        Long teacherId = sessionUtils.getCurrentUserId();
        log.debug("Obteniendo estadísticas del profesor ID: {}", teacherId);

        if (!sessionUtils.isTeacher()) {
            throw new ValidationException("Esta operación es solo para profesores");
        }

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Profesor no encontrado"));

        // Contar grupos por estado
        long totalGroups = teacher.getCourseGroups().size();
        long activeGroups = teacher.getCourseGroups().stream()
                .filter(g -> "ACTIVE".equals(g.getStatus().name()))
                .count();
        long plannedGroups = teacher.getCourseGroups().stream()
                .filter(g -> "PLANNED".equals(g.getStatus().name()))
                .count();

        // Contar estudiantes totales
        long totalStudents = teacher.getCourseGroups().stream()
                .mapToLong(g -> g.getEnrollments().size())
                .sum();

        // Contar sesiones semanales
        List<GroupSession> sessions = groupSessionRepository.findByTeacherId(teacherId);
        int weeklyHours = sessions.stream()
                .mapToInt(s -> calculateHours(s.getStartTime(), s.getEndTime()))
                .sum();

        // Asignaturas únicas que imparte
        long uniqueSubjects = teacher.getCourseGroups().stream()
                .map(g -> g.getSubject())
                .distinct()
                .count();

        return TeacherStatsDto.builder()
                .teacherId(teacherId)
                .teacherName(teacher.getName())
                .totalGroups((int) totalGroups)
                .activeGroups((int) activeGroups)
                .plannedGroups((int) plannedGroups)
                .totalStudents((int) totalStudents)
                .weeklyHours(weeklyHours)
                .uniqueSubjects((int) uniqueSubjects)
                .build();
    }

    /**
     * Verifica disponibilidad del profesor en un horario específico.
     * Útil para evitar conflictos al asignar nuevos grupos.
     */
    public boolean isAvailable(Long teacherId, String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        log.debug("Verificando disponibilidad del profesor {} el {} de {} a {}",
                teacherId, dayOfWeek, startTime, endTime);

        // Validar que es admin o el mismo profesor
        if (sessionUtils.isTeacher() && !teacherId.equals(sessionUtils.getCurrentUserId())) {
            throw new ValidationException("No puede consultar la disponibilidad de otros profesores");
        }

        DayOfWeek day;
        try {
            day = DayOfWeek.valueOf(dayOfWeek.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Día de la semana inválido: " + dayOfWeek);
        }

        List<GroupSession> sessions = groupSessionRepository.findByTeacherId(teacherId);

        // Verificar conflictos
        return sessions.stream()
                .filter(s -> s.getDayOfWeek().toString().equals(day.toString()))
                .noneMatch(s -> hasTimeOverlap(s.getStartTime(), s.getEndTime(), startTime, endTime));
    }

    // ========== OPERACIONES ADMINISTRATIVAS ==========

    /**
     * Obtiene todos los profesores con paginación.
     * Solo accesible para administradores.
     */
    public List<TeacherDto> getAllTeachers() {
        validateAdminRole();
        log.debug("Obteniendo todos los profesores");

        List<Teacher> teachers = teacherRepository.findAll();
        return teacherMapper.toDtoList(teachers);
    }

    /**
     * Crea un nuevo profesor.
     * Solo accesible para administradores.
     */
    @Transactional
    public TeacherDto createTeacher(CreateTeacherDto createDto) {
        validateAdminRole();
        log.info("Creando nuevo profesor: {}", createDto.getEmail());

        // Verificar si el email ya existe
        if (teacherRepository.existsByEmail(createDto.getEmail()) ||
                studentRepository.existsByEmail(createDto.getEmail())) {
            throw new EmailAlreadyExistsException("El email ya está registrado");
        }

        Teacher teacher = teacherMapper.toEntity(createDto);

        try {
            teacher = teacherRepository.save(teacher);
            log.info("Profesor creado con ID: {}", teacher.getId());
            return teacherMapper.toDto(teacher);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear profesor", e);
            throw new ValidationException("Error al crear el profesor. Verifique los datos");
        }
    }

    /**
     * Actualiza un profesor como administrador.
     * Permite actualizar más campos que el propio profesor.
     */
    @Transactional
    public TeacherDto updateTeacher(Long teacherId, TeacherDto updateDto) {
        validateAdminRole();
        log.info("Admin actualizando profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        // Admin puede actualizar más campos
        teacherMapper.updateBasicInfo(teacher, updateDto);

        try {
            teacher = teacherRepository.save(teacher);
            log.info("Profesor actualizado por admin: {}", teacher.getId());
            return teacherMapper.toDto(teacher);
        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al actualizar profesor", e);
            throw new ValidationException("Error al actualizar el profesor");
        }
    }

    /**
     * Elimina un profesor.
     * Solo posible si no tiene grupos activos asignados.
     */
    @Transactional
    public void deleteTeacher(Long teacherId) {
        validateAdminRole();
        log.info("Eliminando profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        // Verificar si tiene grupos activos
        long activeGroups = teacher.getCourseGroups().stream()
                .filter(g -> "ACTIVE".equals(g.getStatus().name()))
                .count();

        if (activeGroups > 0) {
            throw new ValidationException(
                    "No se puede eliminar el profesor porque tiene grupos activos asignados");
        }

        try {
            teacherRepository.delete(teacher);
            log.info("Profesor eliminado: {}", teacherId);
        } catch (DataIntegrityViolationException e) {
            log.error("Error al eliminar profesor", e);
            throw new ValidationException(
                    "No se puede eliminar el profesor debido a dependencias existentes");
        }
    }

    /**
     * Obtiene profesores disponibles para un horario específico.
     * Útil para asignar profesores a nuevos grupos.
     */
    public List<TeacherDto> getAvailableTeachers(String dayOfWeek, LocalTime startTime, LocalTime endTime) {
        validateAdminRole();
        log.debug("Buscando profesores disponibles el {} de {} a {}", dayOfWeek, startTime, endTime);

        List<Teacher> allTeachers = teacherRepository.findAll();

        // Filtrar profesores disponibles
        List<Teacher> availableTeachers = allTeachers.stream()
                .filter(teacher -> isAvailable(teacher.getId(), dayOfWeek, startTime, endTime))
                .collect(Collectors.toList());

        return availableTeachers.stream()
                .map(teacherMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas generales de profesores.
     * Para dashboard administrativo.
     */
    public AdminTeacherStatsDto getAdminTeacherStats() {
        validateAdminRole();
        log.debug("Obteniendo estadísticas administrativas de profesores");

        List<Teacher> allTeachers = teacherRepository.findAll();

        // Profesores con grupos activos
        long teachersWithActiveGroups = allTeachers.stream()
                .filter(t -> t.getCourseGroups().stream()
                        .anyMatch(g -> "ACTIVE".equals(g.getStatus().name())))
                .count();

        // Total de grupos y estudiantes
        long totalGroups = allTeachers.stream()
                .mapToLong(t -> t.getCourseGroups().size())
                .sum();

        long totalStudents = allTeachers.stream()
                .flatMap(t -> t.getCourseGroups().stream())
                .mapToLong(g -> g.getEnrollments().size())
                .sum();

        // Carga promedio (estudiantes por profesor)
        double avgStudentsPerTeacher = allTeachers.isEmpty() ? 0 :
                (double) totalStudents / allTeachers.size();

        // Profesores sin grupos
        long teachersWithoutGroups = allTeachers.stream()
                .filter(t -> t.getCourseGroups().isEmpty())
                .count();

        return AdminTeacherStatsDto.builder()
                .totalTeachers(allTeachers.size())
                .teachersWithActiveGroups((int) teachersWithActiveGroups)
                .teachersWithoutGroups((int) teachersWithoutGroups)
                .totalGroups((int) totalGroups)
                .totalStudents((int) totalStudents)
                .avgStudentsPerTeacher(avgStudentsPerTeacher)
                .build();
    }

    // ========== MÉTODOS DE VALIDACIÓN Y UTILIDAD PRIVADOS ==========

    /**
     * Valida acceso de profesor a sus propios recursos.
     */
    private void validateTeacherAccess(Long teacherId) {
        if (!sessionUtils.isTeacher()) {
            return; // Puede ser admin
        }

        if (!teacherId.equals(sessionUtils.getCurrentUserId())) {
            log.warn("Profesor {} intentó acceder a datos del profesor {}",
                    sessionUtils.getCurrentUserId(), teacherId);
            throw new ValidationException("No puede acceder a información de otros profesores");
        }
    }

    /**
     * Verifica si un profesor puede ser eliminado.
     * No puede eliminarse si tiene grupos ACTIVOS.
     */
    public boolean canDeleteTeacher(Long teacherId) {
        validateAdminRole();
        log.debug("Verificando si se puede eliminar profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profesor no encontrado con ID: " + teacherId));

        // Verificar si tiene grupos activos
        boolean hasActiveGroups = teacher.getCourseGroups().stream()
                .anyMatch(group -> CourseGroupStatus.ACTIVE.equals(group.getStatus()));

        return !hasActiveGroups;
    }

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
     * Calcula las horas entre dos tiempos.
     */
    private int calculateHours(LocalTime start, LocalTime end) {
        return (int) java.time.Duration.between(start, end).toHours();
    }

    /**
     * Verifica si dos rangos de tiempo se solapan.
     */
    private boolean hasTimeOverlap(LocalTime start1, LocalTime end1,
                                   LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    public boolean emailExists(String email) {
        validateAdminRole();
        log.debug("Verificando si existe el email: {}", email);
        return teacherRepository.existsByEmail(email);
    }
}

/**
 * DTO para estadísticas del profesor.
 */
@lombok.Data
@lombok.Builder
class TeacherStatsDto {
    private Long teacherId;
    private String teacherName;
    private int totalGroups;
    private int activeGroups;
    private int plannedGroups;
    private int totalStudents;
    private int weeklyHours;
    private int uniqueSubjects;
}

/**
 * DTO para estadísticas administrativas.
 */
@lombok.Data
@lombok.Builder
class AdminTeacherStatsDto {
    private int totalTeachers;
    private int teachersWithActiveGroups;
    private int teachersWithoutGroups;
    private int totalGroups;
    private int totalStudents;
    private double avgStudentsPerTeacher;
}