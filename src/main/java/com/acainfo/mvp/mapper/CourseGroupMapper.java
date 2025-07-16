package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.coursegroup.*;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.GroupSession;
import com.acainfo.mvp.model.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades CourseGroup y sus DTOs.
 */
@Component
public class CourseGroupMapper {

    /**
     * Convierte una entidad CourseGroup a CourseGroupDto.
     *
     * @param group entidad CourseGroup
     * @return CourseGroupDto
     */
    public CourseGroupDto toDto(CourseGroup group) {
        if (group == null) {
            return null;
        }

        CourseGroupDto dto = CourseGroupDto.builder()
                .subjectId(group.getSubject().getId())
                .subjectName(group.getSubject().getName())
                .teacherId(group.getTeacher() != null ? group.getTeacher().getId() : null)
                .teacherName(group.getTeacher() != null ? group.getTeacher().getName() : "Por asignar")
                .status(group.getStatus())
                .type(group.getType())
                .price(group.getPrice())
                .enrolledStudents(group.getEnrollments().size())
                .build();

        dto.setId(group.getId());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una entidad CourseGroup a CourseGroupDetailDto con información completa.
     *
     * @param group entidad CourseGroup con relaciones cargadas
     * @return CourseGroupDetailDto
     */
    public CourseGroupDetailDto toDetailDto(CourseGroup group) {
        if (group == null) {
            return null;
        }

        // Información del subject
        SubjectInfo subjectInfo = SubjectInfo.builder()
                .id(group.getSubject().getId())
                .name(group.getSubject().getName())
                .major(group.getSubject().getMajor())
                .courseYear(group.getSubject().getCourseYear())
                .build();

        // Información del teacher
        TeacherInfo teacherInfo = null;
        if (group.getTeacher() != null) {
            teacherInfo = TeacherInfo.builder()
                    .id(group.getTeacher().getId())
                    .name(group.getTeacher().getName())
                    .email(group.getTeacher().getEmail())
                    .build();
        }

        // Convertir sesiones
        List<GroupSessionDto> sessions = group.getGroupSessions().stream()
                .map(this::toSessionDto)
                .collect(Collectors.toList());

        // Determinar si se puede inscribir
        boolean canEnroll = group.hasCapacity() &&
                group.getStatus() == com.acainfo.mvp.model.enums.CourseGroupStatus.ACTIVE;

        String enrollmentMessage = "";
        if (!group.getStatus().equals(com.acainfo.mvp.model.enums.CourseGroupStatus.ACTIVE)) {
            enrollmentMessage = "El grupo no está activo";
        } else if (!group.hasCapacity()) {
            enrollmentMessage = "El grupo está lleno";
        } else {
            enrollmentMessage = "Disponible para inscripción";
        }

        CourseGroupDetailDto dto = CourseGroupDetailDto.builder()
                .subject(subjectInfo)
                .teacher(teacherInfo)
                .status(group.getStatus())
                .type(group.getType())
                .price(group.getPrice())
                .enrolledStudents(group.getEnrollments().size())
                .maxCapacity(group.getMaxCapacity())
                .sessions(sessions)
                .canEnroll(canEnroll)
                .enrollmentMessage(enrollmentMessage)
                .build();

        dto.setId(group.getId());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una GroupSession a GroupSessionDto.
     *
     * @param session entidad GroupSession
     * @return GroupSessionDto
     */
    public GroupSessionDto toSessionDto(GroupSession session) {
        if (session == null) {
            return null;
        }

        return GroupSessionDto.builder()
                .id(session.getId())
                .dayOfWeek(session.getDayOfWeek().name())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .classroom(session.getClassroom())
                .build();
    }

    /**
     * Convierte una lista de entidades CourseGroup a lista de CourseGroupDto.
     *
     * @param groups lista de entidades
     * @return lista de DTOs
     */
    public List<CourseGroupDto> toDtoList(List<CourseGroup> groups) {
        return groups.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de ingresos de un grupo.
     *
     * @param group entidad CourseGroup
     * @return mapa con estadísticas
     */
    public GroupRevenueDto toRevenueDto(CourseGroup group) {
        if (group == null) {
            return null;
        }

        long paidEnrollments = group.getEnrollments().stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PAID)
                .count();

        long pendingEnrollments = group.getEnrollments().stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PENDING)
                .count();

        java.math.BigDecimal totalRevenue = group.getPrice()
                .multiply(java.math.BigDecimal.valueOf(paidEnrollments));

        java.math.BigDecimal potentialRevenue = group.getPrice()
                .multiply(java.math.BigDecimal.valueOf(pendingEnrollments));

        return GroupRevenueDto.builder()
                .groupId(group.getId())
                .subjectName(group.getSubject().getName())
                .teacherName(group.getTeacher() != null ? group.getTeacher().getName() : "Por asignar")
                .totalEnrollments(group.getEnrollments().size())
                .paidEnrollments((int) paidEnrollments)
                .pendingEnrollments((int) pendingEnrollments)
                .pricePerStudent(group.getPrice())
                .totalRevenue(totalRevenue)
                .potentialRevenue(potentialRevenue)
                .build();
    }

    /**
     * DTO para información de ingresos del grupo
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroupRevenueDto {
        private Long groupId;
        private String subjectName;
        private String teacherName;
        private int totalEnrollments;
        private int paidEnrollments;
        private int pendingEnrollments;
        private java.math.BigDecimal pricePerStudent;
        private java.math.BigDecimal totalRevenue;
        private java.math.BigDecimal potentialRevenue;
    }
}
