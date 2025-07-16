package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.dto.teacher.TeacherDetailDto;
import com.acainfo.mvp.dto.teacher.TeacherScheduleDto;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.StudentMapper;
import com.acainfo.mvp.mapper.TeacherMapper;
import com.acainfo.mvp.mapper.TeacherMapper.TeacherGroupSummaryDto;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.repository.CourseGroupRepository;
import com.acainfo.mvp.repository.EnrollmentRepository;
import com.acainfo.mvp.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar operaciones específicas de profesores.
 * Incluye consulta de horarios, grupos asignados y estudiantes.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;

    /**
     * Obtiene el perfil detallado de un profesor.
     *
     * @param teacherId ID del profesor
     * @return perfil detallado del profesor
     * @throws ResourceNotFoundException si el profesor no existe
     */
    public ApiResponseDto<TeacherDetailDto> getTeacherProfile(Long teacherId) {
        log.info("Obteniendo perfil del profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findByIdWithFullDetails(teacherId)
                .orElseThrow(() -> {
                    log.error("Profesor no encontrado con ID: {}", teacherId);
                    return new ResourceNotFoundException("Profesor no encontrado");
                });

        TeacherDetailDto dto = teacherMapper.toDetailDto(teacher);

        log.info("Perfil obtenido exitosamente. Grupos totales: {}, Grupos activos: {}",
                dto.getTotalGroups(), dto.getActiveGroups());

        return ApiResponseDto.success(dto, "Perfil obtenido exitosamente");
    }

    /**
     * Obtiene el horario semanal del profesor.
     *
     * @param teacherId ID del profesor
     * @return horario semanal estructurado
     * @throws ResourceNotFoundException si el profesor no existe
     */
    public ApiResponseDto<TeacherScheduleDto> getWeeklySchedule(Long teacherId) {
        log.info("Obteniendo horario semanal del profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findByIdWithFullDetails(teacherId)
                .orElseThrow(() -> {
                    log.error("Profesor no encontrado con ID: {}", teacherId);
                    return new ResourceNotFoundException("Profesor no encontrado");
                });

        TeacherScheduleDto scheduleDto = teacherMapper.toScheduleDto(teacher);

        log.info("Horario obtenido exitosamente. Total de sesiones semanales: {}",
                scheduleDto.getSchedule().size());

        return ApiResponseDto.success(scheduleDto,
                String.format("Horario con %d sesiones semanales", scheduleDto.getSchedule().size()));
    }

    /**
     * Obtiene los grupos asignados al profesor.
     *
     * @param teacherId ID del profesor
     * @param onlyActive true para mostrar solo grupos activos
     * @return lista de grupos asignados
     * @throws ResourceNotFoundException si el profesor no existe
     */
    public ApiResponseDto<List<TeacherGroupSummaryDto>> getAssignedGroups(
            Long teacherId,
            boolean onlyActive) {

        log.info("Obteniendo grupos del profesor ID: {}, solo activos: {}", teacherId, onlyActive);

        // Verificar que el profesor existe
        if (!teacherRepository.existsById(teacherId)) {
            log.error("Profesor no encontrado con ID: {}", teacherId);
            throw new ResourceNotFoundException("Profesor no encontrado");
        }

        List<CourseGroup> groups = courseGroupRepository.findByTeacherId(teacherId);

        if (onlyActive) {
            groups = groups.stream()
                    .filter(g -> g.getStatus() == CourseGroupStatus.ACTIVE)
                    .collect(Collectors.toList());
        }

        List<TeacherGroupSummaryDto> groupSummaries = teacherMapper.toGroupSummaryDtoList(groups);

        log.info("Se encontraron {} grupos para el profesor", groupSummaries.size());

        return ApiResponseDto.success(groupSummaries,
                String.format("Se encontraron %d grupos", groupSummaries.size()));
    }

    /**
     * Obtiene los estudiantes de un grupo específico del profesor.
     *
     * @param teacherId ID del profesor
     * @param groupId ID del grupo
     * @param onlyPaid true para mostrar solo estudiantes con pago completado
     * @return lista de estudiantes del grupo
     * @throws ResourceNotFoundException si el profesor o grupo no existen
     * @throws ValidationException si el grupo no pertenece al profesor
     */
    public ApiResponseDto<List<StudentDto>> getStudentsByGroup(
            Long teacherId,
            Long groupId,
            boolean onlyPaid) {

        log.info("Obteniendo estudiantes del grupo {} para profesor {}, solo pagados: {}",
                groupId, teacherId, onlyPaid);

        // Verificar que el profesor existe
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> {
                    log.error("Profesor no encontrado con ID: {}", teacherId);
                    return new ResourceNotFoundException("Profesor no encontrado");
                });

        // Verificar que el grupo existe y pertenece al profesor
        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> {
                    log.error("Grupo no encontrado con ID: {}", groupId);
                    return new ResourceNotFoundException("Grupo no encontrado");
                });

        if (group.getTeacher() == null || !group.getTeacher().getId().equals(teacherId)) {
            log.warn("Intento de acceso no autorizado: profesor {} intentó acceder al grupo {}",
                    teacherId, groupId);
            throw new ValidationException("Este grupo no está asignado a tu perfil");
        }

        // Obtener las inscripciones del grupo
        List<Enrollment> enrollments = enrollmentRepository.findByCourseGroupId(groupId);

        if (onlyPaid) {
            enrollments = enrollments.stream()
                    .filter(e -> e.getPaymentStatus() == PaymentStatus.PAID)
                    .toList();
        }

        // Convertir a DTOs de estudiantes
        List<StudentDto> students = enrollments.stream()
                .map(Enrollment::getStudent)
                .map(studentMapper::toDto)
                .collect(Collectors.toList());

        log.info("Se encontraron {} estudiantes en el grupo {}", students.size(), groupId);

        return ApiResponseDto.success(students,
                String.format("Se encontraron %d estudiantes", students.size()));
    }

    /**
     * Obtiene estadísticas resumidas de todos los grupos del profesor.
     *
     * @param teacherId ID del profesor
     * @return estadísticas agregadas
     * @throws ResourceNotFoundException si el profesor no existe
     */
    public ApiResponseDto<TeacherStatsDto> getTeacherStatistics(Long teacherId) {
        log.info("Obteniendo estadísticas del profesor ID: {}", teacherId);

        Teacher teacher = teacherRepository.findByIdWithFullDetails(teacherId)
                .orElseThrow(() -> {
                    log.error("Profesor no encontrado con ID: {}", teacherId);
                    return new ResourceNotFoundException("Profesor no encontrado");
                });

        // Calcular estadísticas
        List<CourseGroup> allGroups = teacher.getCourseGroups().stream().toList();
        List<CourseGroup> activeGroups = allGroups.stream()
                .filter(g -> g.getStatus() == CourseGroupStatus.ACTIVE)
                .collect(Collectors.toList());

        int totalStudents = allGroups.stream()
                .mapToInt(g -> g.getEnrollments().size())
                .sum();

        int activeStudents = activeGroups.stream()
                .mapToInt(g -> g.getEnrollments().size())
                .sum();

        int paidStudents = allGroups.stream()
                .flatMap(g -> g.getEnrollments().stream())
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PAID)
                .mapToInt(e -> 1)
                .sum();

        int totalSessions = activeGroups.stream()
                .mapToInt(g -> g.getGroupSessions().size())
                .sum();

        TeacherStatsDto stats = TeacherStatsDto.builder()
                .totalGroups(allGroups.size())
                .activeGroups(activeGroups.size())
                .totalStudents(totalStudents)
                .activeStudents(activeStudents)
                .paidStudents(paidStudents)
                .weeklyHours(calculateWeeklyHours(activeGroups))
                .totalSessionsPerWeek(totalSessions)
                .build();

        log.info("Estadísticas calculadas exitosamente para profesor ID: {}", teacherId);

        return ApiResponseDto.success(stats, "Estadísticas obtenidas exitosamente");
    }

    /**
     * Calcula las horas semanales totales del profesor.
     *
     * @param groups grupos activos del profesor
     * @return horas semanales totales
     */
    private double calculateWeeklyHours(List<CourseGroup> groups) {
        return groups.stream()
                .flatMap(g -> g.getGroupSessions().stream())
                .mapToDouble(session -> {
                    // Calcular duración en horas
                    int startMinutes = session.getStartTime().getHour() * 60 + session.getStartTime().getMinute();
                    int endMinutes = session.getEndTime().getHour() * 60 + session.getEndTime().getMinute();
                    return (endMinutes - startMinutes) / 60.0;
                })
                .sum();
    }

    /**
     * Verifica si un profesor puede ser asignado a un nuevo horario.
     *
     * @param teacherId ID del profesor
     * @param dayOfWeek día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @return true si no hay conflictos de horario
     */
    public boolean hasScheduleConflict(Long teacherId, String dayOfWeek,
                                       java.time.LocalTime startTime,
                                       java.time.LocalTime endTime) {

        log.debug("Verificando conflictos de horario para profesor {} en {} de {} a {}",
                teacherId, dayOfWeek, startTime, endTime);

        // Esta lógica puede ser usada por otros servicios
        Teacher teacher = teacherRepository.findByIdWithFullDetails(teacherId)
                .orElse(null);

        if (teacher == null) {
            return false;
        }

        return teacher.getCourseGroups().stream()
                .filter(g -> g.getStatus() == CourseGroupStatus.ACTIVE)
                .flatMap(g -> g.getGroupSessions().stream())
                .anyMatch(session ->
                        session.getDayOfWeek().name().equals(dayOfWeek) &&
                                hasTimeOverlap(session.getStartTime(), session.getEndTime(), startTime, endTime)
                );
    }

    /**
     * Verifica si dos rangos de tiempo se solapan.
     */
    private boolean hasTimeOverlap(java.time.LocalTime start1, java.time.LocalTime end1,
                                   java.time.LocalTime start2, java.time.LocalTime end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }

    /**
     * DTO para estadísticas del profesor
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TeacherStatsDto {
        private int totalGroups;
        private int activeGroups;
        private int totalStudents;
        private int activeStudents;
        private int paidStudents;
        private double weeklyHours;
        private int totalSessionsPerWeek;
    }
}