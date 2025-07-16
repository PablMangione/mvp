package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.student.StudentDetailDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.model.enums.RequestStatus;
import org.springframework.stereotype.Component;

/**
 * Mapper para convertir entre entidades Student y sus DTOs.
 */
@Component
public class StudentMapper {

    /**
     * Convierte una entidad Student a StudentDto básico.
     *
     * @param student entidad student
     * @return StudentDto
     */
    public StudentDto toDto(Student student) {
        if (student == null) {
            return null;
        }

        StudentDto dto = StudentDto.builder()
                .name(student.getName())
                .email(student.getEmail())
                .major(student.getMajor())
                .build();

        dto.setId(student.getId());
        dto.setCreatedAt(student.getCreatedAt());
        dto.setUpdatedAt(student.getUpdatedAt());

        return dto;
    }

    /**
     * Convierte una entidad Student a StudentDetailDto con estadísticas.
     *
     * @param student entidad student con relaciones cargadas
     * @return StudentDetailDto
     */
    public StudentDetailDto toDetailDto(Student student) {
        if (student == null) {
            return null;
        }

        // Calcular estadísticas
        int activeEnrollments = (int) student.getEnrollments().stream()
                .filter(e -> e.getCourseGroup().getStatus() == CourseGroupStatus.ACTIVE)
                .count();

        int pendingPayments = (int) student.getEnrollments().stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PENDING)
                .count();

        int pendingRequests = (int) student.getGroupRequests().stream()
                .filter(r -> r.getStatus() == RequestStatus.PENDING)
                .count();

        StudentDetailDto dto = StudentDetailDto.builder()
                .name(student.getName())
                .email(student.getEmail())
                .major(student.getMajor())
                .activeEnrollments(activeEnrollments)
                .pendingPayments(pendingPayments)
                .pendingRequests(pendingRequests)
                .build();

        dto.setId(student.getId());
        dto.setCreatedAt(student.getCreatedAt());
        dto.setUpdatedAt(student.getUpdatedAt());

        return dto;
    }

    /**
     * Actualiza una entidad Student con datos de UpdateStudentDto.
     *
     * @param student entidad a actualizar
     * @param updateDto datos de actualización
     */
    public void updateEntityFromDto(Student student, com.acainfo.mvp.dto.student.UpdateStudentDto updateDto) {
        if (updateDto.getName() != null) {
            student.setName(updateDto.getName());
        }
        if (updateDto.getEmail() != null) {
            student.setEmail(updateDto.getEmail());
        }
        if (updateDto.getMajor() != null) {
            student.setMajor(updateDto.getMajor());
        }
    }
}
