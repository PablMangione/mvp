package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.Subject;
import com.acainfo.mvp.model.Teacher;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversiones entre CourseGroup entity y sus DTOs.
 * Maneja la transformación de grupos de curso y sus sesiones.
 */
@Component
public class CourseGroupMapper {

    /**
     * Convierte CourseGroup entity a CourseGroupDto (información básica).
     */
    public CourseGroupDto toDto(CourseGroup courseGroup) {
        if (courseGroup == null) {
            return null;
        }

        CourseGroupDto dto = CourseGroupDto.builder()
                .subjectId(courseGroup.getSubject().getId())
                .subjectName(courseGroup.getSubject().getName())
                .teacherId(courseGroup.getTeacher() != null
                        ? courseGroup.getTeacher().getId()
                        : null)
                .teacherName(courseGroup.getTeacher() != null
                        ? courseGroup.getTeacher().getName()
                        : "Sin asignar")
                .status(courseGroup.getStatus())
                .type(courseGroup.getType())
                .price(courseGroup.getPrice())
                .enrolledStudents(courseGroup.getEnrollments().size())
                .build();

        dto.setId(courseGroup.getId());
        dto.setCreatedAt(courseGroup.getCreatedAt());
        dto.setUpdatedAt(courseGroup.getUpdatedAt());
        return dto;
    }

    /**
     * Convierte CourseGroup a CourseGroupDetailDto con información completa.
     * Incluye horarios, capacidad y estado de inscripción.
     */
    public CourseGroupDetailDto toDetailDto(CourseGroup courseGroup) {
        if (courseGroup == null) {
            return null;
        }

        // Información de la asignatura
        SubjectInfo subjectInfo = SubjectInfo.builder()
                .id(courseGroup.getSubject().getId())
                .name(courseGroup.getSubject().getName())
                .major(courseGroup.getSubject().getMajor())
                .courseYear(courseGroup.getSubject().getCourseYear())
                .build();

        // Información del profesor (si existe)
        TeacherInfo teacherInfo = null;
        if (courseGroup.getTeacher() != null) {
            teacherInfo = TeacherInfo.builder()
                    .id(courseGroup.getTeacher().getId())
                    .name(courseGroup.getTeacher().getName())
                    .email(courseGroup.getTeacher().getEmail())
                    .build();
        }

        // Convertir sesiones
        List<GroupSessionDto> sessions = courseGroup.getGroupSessions().stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());

        // Calcular disponibilidad
        int enrolledCount = courseGroup.getEnrollments().size();
        boolean canEnroll = courseGroup.getStatus() == CourseGroupStatus.ACTIVE
                && enrolledCount < courseGroup.getMaxCapacity();

        String enrollmentMessage = generateEnrollmentMessage(
                courseGroup.getStatus(),
                enrolledCount,
                courseGroup.getMaxCapacity()
        );

        return CourseGroupDetailDto.builder()
                .subject(subjectInfo)
                .teacher(teacherInfo)
                .status(courseGroup.getStatus())
                .type(courseGroup.getType())
                .price(courseGroup.getPrice())
                .enrolledStudents(enrolledCount)
                .maxCapacity(courseGroup.getMaxCapacity())
                .sessions(sessions)
                .canEnroll(canEnroll)
                .enrollmentMessage(enrollmentMessage)
                .build();
    }

    /**
     * Convierte GroupSession a GroupSessionDto.
     */
    public GroupSessionDto toSessionDto(GroupSession session) {
        if (session == null) {
            return null;
        }

        return GroupSessionDto.builder()
                .id(session.getId())
                .dayOfWeek(session.getDayOfWeek().toString())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroom(session.getClassroom())
                .build();
    }

    /**
     * Crea CourseGroup desde CreateCourseGroupDto.
     * Requiere Subject y opcionalmente Teacher ya existentes.
     */
    public CourseGroup toEntity(CreateCourseGroupDto dto, Subject subject, Teacher teacher) {
        if (dto == null || subject == null) {
            return null;
        }

        CourseGroup courseGroup = CourseGroup.builder()
                .subject(subject)
                .teacher(teacher) // Puede ser null
                .type(dto.getType())
                .price(dto.getPrice())
                .status(CourseGroupStatus.PLANNED) // Estado inicial
                .maxCapacity(30) // Valor por defecto
                .build();

        return courseGroup;
    }

    /**
     * Crea GroupSession desde CreateGroupSessionDto.
     */
    public GroupSession toSessionEntity(CreateGroupSessionDto dto, CourseGroup courseGroup) {
        if (dto == null || courseGroup == null) {
            return null;
        }

        return GroupSession.builder()
                .courseGroup(courseGroup)
                .dayOfWeek(com.acainfo.mvp.model.enums.DayOfWeek.valueOf(dto.getDayOfWeek()))
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .classroom(dto.getClassroom())
                .build();
    }

    /**
     * Actualiza el estado del CourseGroup.
     */
    public void updateStatus(CourseGroup courseGroup, UpdateGroupStatusDto dto) {
        if (courseGroup == null || dto == null) {
            return;
        }

        courseGroup.setStatus(dto.getStatus());
    }

    /**
     * Asigna un profesor al CourseGroup.
     */
    public void assignTeacher(CourseGroup courseGroup, Teacher teacher) {
        if (courseGroup == null) {
            return;
        }

        courseGroup.setTeacher(teacher);
    }

    /**
     * Convierte lista de CourseGroups a lista de CourseGroupDtos.
     */
    public List<CourseGroupDto> toDtoList(List<CourseGroup> courseGroups) {
        if (courseGroups == null) {
            return new ArrayList<>();
        }

        return courseGroups.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Genera mensaje descriptivo sobre el estado de inscripción.
     */
    private String generateEnrollmentMessage(CourseGroupStatus status, int enrolled, int maxCapacity) {
        if (status == CourseGroupStatus.PLANNED) {
            return "El grupo aún no está activo";
        }
        if (status == CourseGroupStatus.CLOSED) {
            return "El grupo está cerrado";
        }
        if (enrolled >= maxCapacity) {
            return "El grupo está completo";
        }

        int available = maxCapacity - enrolled;
        return String.format("Plazas disponibles: %d de %d", available, maxCapacity);
    }
}