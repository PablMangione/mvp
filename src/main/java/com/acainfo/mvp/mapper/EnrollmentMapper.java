package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.enrollment.CreateEnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentResponseDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para conversiones entre Enrollment entity y sus DTOs.
 * Maneja la transformación de inscripciones de estudiantes en grupos.
 */
@Component
public class EnrollmentMapper {

    /**
     * Convierte Enrollment entity a EnrollmentDto (vista completa).
     * Incluye información del estudiante, grupo y estado de pago.
     */
    public EnrollmentDto toDto(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }

        return EnrollmentDto.builder()
                .studentId(enrollment.getStudent().getId())
                .studentName(enrollment.getStudent().getName())
                .courseGroupId(enrollment.getCourseGroup().getId())
                .subjectName(enrollment.getCourseGroup().getSubject().getName())
                .teacherName(enrollment.getCourseGroup().getTeacher() != null
                        ? enrollment.getCourseGroup().getTeacher().getName()
                        : "Sin asignar")
                .enrollmentDate(enrollment.getCreatedAt())
                .paymentStatus(enrollment.getPaymentStatus())
                .build();
    }

    /**
     * Convierte Enrollment a EnrollmentSummaryDto (vista resumida para estudiante).
     * Información relevante para el estudiante sobre sus inscripciones.
     */
    public EnrollmentSummaryDto toSummaryDto(Enrollment enrollment) {
        if (enrollment == null) {
            return null;
        }

        return EnrollmentSummaryDto.builder()
                .enrollmentId(enrollment.getId())
                .courseGroupId(enrollment.getCourseGroup().getId())
                .subjectName(enrollment.getCourseGroup().getSubject().getName())
                .teacherName(enrollment.getCourseGroup().getTeacher() != null
                        ? enrollment.getCourseGroup().getTeacher().getName()
                        : "Sin asignar")
                .groupType(enrollment.getCourseGroup().getType().toString())
                .groupStatus(enrollment.getCourseGroup().getStatus().toString())
                .createdAt(enrollment.getCreatedAt())
                .paymentStatus(enrollment.getPaymentStatus())
                .build();
    }

    /**
     * Crea Enrollment desde CreateEnrollmentDto.
     * Requiere Student y CourseGroup ya existentes.
     */
    public Enrollment toEntity(CreateEnrollmentDto dto, Student student, CourseGroup courseGroup) {
        if (dto == null || student == null || courseGroup == null) {
            return null;
        }

        return Enrollment.builder()
                .student(student)
                .courseGroup(courseGroup)
                .paymentStatus(PaymentStatus.PENDING) // Estado inicial siempre pendiente
                .build();
    }

    /**
     * Crea EnrollmentResponseDto para confirmar inscripción exitosa.
     */
    public EnrollmentResponseDto toResponseDto(Enrollment enrollment, boolean success, String message) {
        if (enrollment == null && !success) {
            // Caso de error sin enrollment creado
            return EnrollmentResponseDto.builder()
                    .enrollmentId(null)
                    .success(false)
                    .message(message)
                    .paymentStatus(null)
                    .build();
        }

        return EnrollmentResponseDto.builder()
                .enrollmentId(enrollment != null ? enrollment.getId() : null)
                .success(success)
                .message(message)
                .paymentStatus(enrollment != null ? enrollment.getPaymentStatus() : null)
                .build();
    }

    /**
     * Crea respuesta de error para inscripción fallida.
     */
    public EnrollmentResponseDto toErrorResponse(String errorMessage) {
        return EnrollmentResponseDto.builder()
                .enrollmentId(null)
                .success(false)
                .message(errorMessage)
                .paymentStatus(null)
                .build();
    }

    /**
     * Convierte lista de Enrollments a lista de EnrollmentDtos.
     * Útil para vistas administrativas.
     */
    public List<EnrollmentDto> toDtoList(List<Enrollment> enrollments) {
        if (enrollments == null) {
            return new ArrayList<>();
        }

        return enrollments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Convierte lista de Enrollments a lista de EnrollmentSummaryDtos.
     * Útil para mostrar inscripciones del estudiante.
     */
    public List<EnrollmentSummaryDto> toSummaryDtoList(List<Enrollment> enrollments) {
        if (enrollments == null) {
            return new ArrayList<>();
        }

        return enrollments.stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    /**
     * Actualiza el estado de pago de una inscripción.
     */
    public void updatePaymentStatus(Enrollment enrollment, PaymentStatus newStatus) {
        if (enrollment == null || newStatus == null) {
            return;
        }

        enrollment.setPaymentStatus(newStatus);
    }

    /**
     * Verifica si una inscripción está pagada.
     */
    public boolean isPaid(Enrollment enrollment) {
        return enrollment != null && PaymentStatus.PAID.equals(enrollment.getPaymentStatus());
    }

    /**
     * Genera mensaje descriptivo del estado de pago.
     */
    public String getPaymentStatusMessage(PaymentStatus status) {
        if (status == null) {
            return "Estado desconocido";
        }

        switch (status) {
            case PENDING:
                return "Pago pendiente";
            case PAID:
                return "Pagado";
            case FAILED:
                return "Pago fallido";
            default:
                return status.toString();
        }
    }
}
