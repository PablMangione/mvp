package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.enrollment.EnrollmentDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.Enrollment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entre entidades Enrollment y sus DTOs.
 */
@Component
public class EnrollmentMapper {

    /**
     * Convierte una entidad Enrollment a EnrollmentDto.
     *
     * @param enrollment entidad enrollment
     * @return EnrollmentDto
     */
    public EnrollmentDto toDto(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }

        CourseGroup group = enrollment.getCourseGroup();

        EnrollmentDto dto = EnrollmentDto.builder()
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getName())
                .courseGroupId(group.getId())
                .subjectName(group.getSubject().getName())
                .teacherName(group.getTeacher() != null ? group.getTeacher().getName() : "Por asignar")
                .enrollmentDate(enrollment.getCreatedAt())
                .paymentStatus(enrollment.getPaymentStatus())
                .build();

        dto.setId(enrollment.getId());
        dto.setCreatedAt(enrollment.getCreatedAt());
        dto.setUpdatedAt(enrollment.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una entidad Enrollment a EnrollmentSummaryDto para vista de estudiante.
     *
     * @param enrollment entidad enrollment
     * @return EnrollmentSummaryDto
     */
    public EnrollmentSummaryDto toSummaryDto(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }

        CourseGroup group = enrollment.getCourseGroup();

        return EnrollmentSummaryDto.builder()
                .enrollmentId(enrollment.getId())
                .courseGroupId(group.getId())
                .subjectName(group.getSubject().getName())
                .teacherName(group.getTeacher() != null ? group.getTeacher().getName() : "Por asignar")
                .groupType(group.getType().name())
                .groupStatus(group.getStatus().name())
                .createdAt(enrollment.getCreatedAt())
                .paymentStatus(enrollment.getPaymentStatus())
                .build();
    }

    /**
     * Convierte una lista de entidades Enrollment a lista de EnrollmentDto.
     *
     * @param enrollments lista de entidades
     * @return lista de DTOs
     */
    public List<EnrollmentDto> toDtoList(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una lista de entidades Enrollment a lista de EnrollmentSummaryDto.
     *
     * @param enrollments lista de entidades
     * @return lista de DTOs resumen
     */
    public List<EnrollmentSummaryDto> toSummaryDtoList(List<Enrollment> enrollments) {
        return enrollments.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }
}