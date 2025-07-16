package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.teacher.*;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades Teacher y sus DTOs.
 */
@Component
public class TeacherMapper {

    /**
     * Convierte una entidad Teacher a TeacherDto básico.
     *
     * @param teacher entidad teacher
     * @return TeacherDto
     */
    public TeacherDto toDto(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        TeacherDto dto = TeacherDto.builder()
                .name(teacher.getName())
                .email(teacher.getEmail())
                .build();

        dto.setId(teacher.getId());
        dto.setCreatedAt(teacher.getCreatedAt());
        dto.setUpdatedAt(teacher.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una entidad Teacher a TeacherDetailDto con estadísticas.
     *
     * @param teacher entidad teacher con relaciones cargadas
     * @return TeacherDetailDto
     */
    public TeacherDetailDto toDetailDto(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        // Calcular estadísticas
        int totalGroups = teacher.getCourseGroups().size();
        int activeGroups = (int) teacher.getCourseGroups().stream()
                .filter(g -> g.getStatus() == CourseGroupStatus.ACTIVE)
                .count();

        TeacherDetailDto dto = TeacherDetailDto.builder()
                .name(teacher.getName())
                .email(teacher.getEmail())
                .totalGroups(totalGroups)
                .activeGroups(activeGroups)
                .build();

        dto.setId(teacher.getId());
        dto.setCreatedAt(teacher.getCreatedAt());
        dto.setUpdatedAt(teacher.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una lista de entidades Teacher a lista de TeacherDto.
     *
     * @param teachers lista de entidades
     * @return lista de DTOs
     */
    public List<TeacherDto> toDtoList(List<Teacher> teachers) {
        return teachers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva entidad Teacher desde CreateTeacherDto.
     *
     * @param createDto DTO de creación
     * @return nueva entidad Teacher
     */
    public Teacher toEntity(CreateTeacherDto createDto) {
        if (createDto == null) {
            return null;
        }

        return Teacher.builder()
                .name(createDto.getName())
                .email(createDto.getEmail())
                .password(createDto.getPassword()) // Se debe encodear en el servicio
                .build();
    }

    /**
     * Actualiza una entidad Teacher con datos de UpdateTeacherDto.
     *
     * @param teacher entidad a actualizar
     * @param updateDto datos de actualización
     */
    public void updateEntityFromDto(Teacher teacher, UpdateTeacherDto updateDto) {
        if (updateDto.getName() != null) {
            teacher.setName(updateDto.getName());
        }
        if (updateDto.getEmail() != null) {
            teacher.setEmail(updateDto.getEmail());
        }
    }

    /**
     * Convierte el horario del profesor a TeacherScheduleDto.
     *
     * @param teacher entidad teacher con grupos y sesiones cargadas
     * @return TeacherScheduleDto
     */
    public TeacherScheduleDto toScheduleDto(Teacher teacher) {
        if (teacher == null) {
            return null;
        }

        List<ScheduleSlotDto> schedule = teacher.getCourseGroups().stream()
                .filter(group -> group.getStatus() == CourseGroupStatus.ACTIVE)
                .flatMap(group -> group.getGroupSessions().stream()
                        .map(session -> toScheduleSlotDto(session, group)))
                .sorted(Comparator.comparing(ScheduleSlotDto::getDayOfWeek)
                        .thenComparing(ScheduleSlotDto::getStartTime))
                .collect(Collectors.toList());

        return TeacherScheduleDto.builder()
                .teacherId(teacher.getId())
                .teacherName(teacher.getName())
                .schedule(schedule)
                .build();
    }

    /**
     * Convierte una GroupSession a ScheduleSlotDto con información del grupo.
     *
     * @param session sesión del grupo
     * @param group grupo al que pertenece la sesión
     * @return ScheduleSlotDto
     */
    private ScheduleSlotDto toScheduleSlotDto(GroupSession session, CourseGroup group) {
        return ScheduleSlotDto.builder()
                .dayOfWeek(session.getDayOfWeek().name())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroom(session.getClassroom())
                .subjectName(group.getSubject().getName())
                .courseGroupId(group.getId())
                .groupType(group.getType().name())
                .enrolledStudents(group.getEnrollments().size())
                .build();
    }

    /**
     * Convierte un grupo a información resumida para el profesor.
     *
     * @param group entidad CourseGroup
     * @return resumen del grupo como mapa
     */
    public TeacherGroupSummaryDto toGroupSummaryDto(CourseGroup group) {
        if (group == null) {
            return null;
        }

        return TeacherGroupSummaryDto.builder()
                .groupId(group.getId())
                .subjectName(group.getSubject().getName())
                .subjectMajor(group.getSubject().getMajor())
                .groupType(group.getType().name())
                .groupStatus(group.getStatus().name())
                .enrolledStudents(group.getEnrollments().size())
                .paidStudents((int) group.getEnrollments().stream()
                        .filter(e -> e.getPaymentStatus() == com.acainfo.mvp.model.enums.PaymentStatus.PAID)
                        .count())
                .sessionCount(group.getGroupSessions().size())
                .build();
    }

    /**
     * Convierte una lista de grupos a lista de resúmenes.
     *
     * @param groups lista de grupos
     * @return lista de resúmenes
     */
    public List<TeacherGroupSummaryDto> toGroupSummaryDtoList(List<CourseGroup> groups) {
        return groups.stream()
                .map(this::toGroupSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * DTO interno para resumen de grupos del profesor
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TeacherGroupSummaryDto {
        private Long groupId;
        private String subjectName;
        private String subjectMajor;
        private String groupType;
        private String groupStatus;
        private int enrolledStudents;
        private int paidStudents;
        private int sessionCount;
    }
}