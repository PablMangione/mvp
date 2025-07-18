package com.acainfo.mvp.mapper;

import com.acainfo.mvp.dto.auth.CurrentUserDto;
import com.acainfo.mvp.dto.auth.LoginResponseDto;
import com.acainfo.mvp.dto.student.StudentRegistrationDto;
import com.acainfo.mvp.dto.student.CreateStudentDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.dto.student.StudentDto;
import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.Student;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Mapper para conversiones entre Student entity y sus DTOs.
 * Maneja la lógica de transformación considerando seguridad con sesiones HTTP.
 */
@Component
public class StudentMapper {

    private final PasswordEncoder passwordEncoder;

    public StudentMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Convierte Student entity a StudentDto (información pública).
     * No incluye password ni información sensible.
     */
    public StudentDto toDto(Student student) {
        if (student == null) {
            return null;
        }

        return StudentDto.builder()
                .name(student.getName())
                .email(student.getEmail())
                .major(student.getMajor())
                .build();
    }

    /**
     * Convierte Student a LoginResponseDto tras autenticación exitosa.
     * No incluye token ya que usamos sesiones HTTP.
     */
    public LoginResponseDto toLoginResponse(Student student) {
        if (student == null) {
            return null;
        }

        return LoginResponseDto.builder()
                .id(student.getId())
                .email(student.getEmail())
                .name(student.getName())
                .role("STUDENT")
                .authenticated(true)
                .build();
    }

    /**
     * Convierte Student a CurrentUserDto para verificación de sesión.
     */
    public CurrentUserDto toCurrentUserDto(Student student) {
        if (student == null) {
            return CurrentUserDto.builder()
                    .authenticated(false)
                    .build();
        }

        return CurrentUserDto.builder()
                .id(student.getId())
                .email(student.getEmail())
                .name(student.getName())
                .role("STUDENT")
                .authenticated(true)
                .build();
    }

    /**
     * Crea nuevo Student desde StudentRegistrationDto.
     * Hashea el password antes de guardar.
     */
    public Student toEntity(StudentRegistrationDto dto) {
        if (dto == null) {
            return null;
        }

        return Student.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .major(dto.getMajor())
                .build();
    }

    /**
     * Crea nuevo Student desde CreateStudentDto (usado por admin).
     * Similar a registro pero en contexto administrativo.
     */
    public Student toEntity(CreateStudentDto dto) {
        if (dto == null) {
            return null;
        }

        return Student.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .major(dto.getMajor())
                .build();
    }

    /**
     * Convierte Enrollment a EnrollmentSummaryDto para vista del alumno.
     * Solo incluye información relevante para el estudiante.
     */
    public EnrollmentSummaryDto toEnrollmentSummary(Enrollment enrollment) {
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
     * Actualiza campos básicos del Student (sin password).
     * Usado para actualizaciones parciales.
     */
    public void updateBasicInfo(Student student, StudentDto dto) {
        if (student == null || dto == null) {
            return;
        }

        if (dto.getName() != null) {
            student.setName(dto.getName());
        }
        if (dto.getMajor() != null) {
            student.setMajor(dto.getMajor());
        }
        // Email no se actualiza por seguridad
    }

    /**
     * Valida si un password sin hashear coincide con el hasheado.
     */
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Hashea un nuevo password.
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
