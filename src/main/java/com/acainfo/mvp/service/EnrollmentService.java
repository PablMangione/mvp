package com.acainfo.mvp.service;

import com.acainfo.mvp.dto.common.ApiResponseDto;
import com.acainfo.mvp.dto.enrollment.CreateEnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentDto;
import com.acainfo.mvp.dto.enrollment.EnrollmentResponseDto;
import com.acainfo.mvp.dto.student.EnrollmentSummaryDto;
import com.acainfo.mvp.exception.student.DuplicateRequestException;
import com.acainfo.mvp.exception.student.GroupFullException;
import com.acainfo.mvp.exception.student.ResourceNotFoundException;
import com.acainfo.mvp.exception.student.ValidationException;
import com.acainfo.mvp.mapper.EnrollmentMapper;
import com.acainfo.mvp.model.CourseGroup;
import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.Student;
import com.acainfo.mvp.model.enums.CourseGroupStatus;
import com.acainfo.mvp.model.enums.PaymentStatus;
import com.acainfo.mvp.repository.CourseGroupRepository;
import com.acainfo.mvp.repository.EnrollmentRepository;
import com.acainfo.mvp.repository.StudentRepository;
import com.acainfo.mvp.util.SessionUtils;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de inscripciones.
 * Maneja el proceso de inscripción de estudiantes en grupos y consultas relacionadas.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseGroupRepository courseGroupRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final SessionUtils sessionUtils;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             StudentRepository studentRepository,
                             CourseGroupRepository courseGroupRepository,
                             EnrollmentMapper enrollmentMapper,
                             SessionUtils sessionUtils) {
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.courseGroupRepository = courseGroupRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.sessionUtils = sessionUtils;
    }

    // ========== OPERACIONES DE ESTUDIANTE ==========

    /**
     * Inscribe a un estudiante en un grupo.
     * Valida que el estudiante pueda inscribirse según las reglas de negocio.
     */
    @Transactional
    public EnrollmentResponseDto enrollStudent(CreateEnrollmentDto createDto) {
        log.info("Procesando inscripción - Estudiante: {}, Grupo: {}",
                createDto.getStudentId(), createDto.getCourseGroupId());

        // Validar que el estudiante existe y coincide con el usuario actual
        Student student = validateAndGetStudent(createDto.getStudentId());

        // Validar que el grupo existe y está disponible
        CourseGroup courseGroup = validateAndGetCourseGroup(createDto.getCourseGroupId());

        // Validar reglas de negocio para la inscripción
        validateEnrollmentRules(student, courseGroup);

        try {
            // Crear la inscripción
            Enrollment enrollment = enrollmentMapper.toEntity(createDto, student, courseGroup);
            enrollment = enrollmentRepository.save(enrollment);

            log.info("Inscripción exitosa - ID: {}, Estudiante: {}, Grupo: {}",
                    enrollment.getId(), student.getEmail(), courseGroup.getId());

            return enrollmentMapper.toResponseDto(enrollment, true,
                    "Inscripción realizada exitosamente. Estado de pago: PENDIENTE");

        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear inscripción", e);
            throw new DuplicateRequestException("Ya existe una inscripción para este estudiante en este grupo");
        }
    }

    /**
     * Obtiene las inscripciones de un estudiante.
     * Solo puede acceder a sus propias inscripciones.
     */
    public List<EnrollmentSummaryDto> getStudentEnrollments(Long studentId) {
        validateStudentAccess(studentId);
        log.debug("Obteniendo inscripciones del estudiante ID: {}", studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(
                studentId, Sort.by("createdAt"));

        return enrollmentMapper.toSummaryDtoList(enrollments);
    }

    /**
     * Obtiene una inscripción específica por ID.
     * Valida que el estudiante tenga acceso a esa inscripción.
     */
    public EnrollmentDto getEnrollmentById(Long enrollmentId) {
        log.debug("Obteniendo inscripción ID: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inscripción no encontrada con ID: " + enrollmentId));

        // Validar acceso
        if (sessionUtils.isStudent()) {
            validateStudentAccess(enrollment.getStudent().getId());
        }

        return enrollmentMapper.toDto(enrollment);
    }

    /**
     * Cancela una inscripción de estudiante.
     * Solo se puede cancelar si el pago está pendiente.
     */
    @Transactional
    public ApiResponseDto cancelEnrollment(Long enrollmentId) {
        log.info("Procesando cancelación de inscripción ID: {}", enrollmentId);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inscripción no encontrada con ID: " + enrollmentId));

        // Validar que el estudiante sea el dueño de la inscripción
        if (sessionUtils.isStudent()) {
            validateStudentAccess(enrollment.getStudent().getId());
        }

        // Validar reglas de cancelación
        validateCancellationRules(enrollment);

        try {
            enrollmentRepository.delete(enrollment);
            log.info("Inscripción cancelada exitosamente - ID: {}", enrollmentId);

            return ApiResponseDto.success(enrollment,"Inscripción cancelada exitosamente");
        } catch (Exception e) {
            log.error("Error al cancelar inscripción", e);
            throw new ValidationException("Error al cancelar la inscripción");
        }
    }

    // ========== OPERACIONES ADMINISTRATIVAS ==========

    /**
     * Obtiene todas las inscripciones con paginación.
     * Solo accesible para administradores.
     */
    public Page<EnrollmentDto> getAllEnrollments(Pageable pageable) {
        validateAdminRole();
        log.debug("Obteniendo todas las inscripciones - página: {}", pageable.getPageNumber());

        Page<Enrollment> enrollments = enrollmentRepository.findAll(pageable);
        return enrollments.map(enrollmentMapper::toDto);
    }

    /**
     * Actualiza el estado de pago de una inscripción.
     * Solo accesible para administradores.
     */
    @Transactional
    public EnrollmentDto updatePaymentStatus(Long enrollmentId, PaymentStatus newStatus) {
        validateAdminRole();
        log.info("Actualizando estado de pago - Inscripción: {}, Nuevo estado: {}",
                enrollmentId, newStatus);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inscripción no encontrada con ID: " + enrollmentId));

        enrollment.setPaymentStatus(newStatus);
        enrollment = enrollmentRepository.save(enrollment);

        log.info("Estado de pago actualizado - Inscripción: {}, Estado: {}",
                enrollmentId, newStatus);

        return enrollmentMapper.toDto(enrollment);
    }

    /**
     * Elimina una inscripción de forma forzada.
     * Solo para casos excepcionales por parte del administrador.
     */
    @Transactional
    public void forceDeleteEnrollment(Long enrollmentId, String reason) {
        validateAdminRole();
        log.warn("Eliminación forzada de inscripción ID: {} - Razón: {}", enrollmentId, reason);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inscripción no encontrada con ID: " + enrollmentId));

        try {
            enrollmentRepository.delete(enrollment);
            log.info("Inscripción eliminada forzosamente - ID: {}, Razón: {}", enrollmentId, reason);
        } catch (Exception e) {
            log.error("Error al eliminar inscripción", e);
            throw new ValidationException("Error al eliminar la inscripción");
        }
    }

    // ========== MÉTODOS DE VALIDACIÓN PRIVADOS ==========

    /**
     * Valida y obtiene el estudiante, verificando que sea el usuario actual.
     */
    private Student validateAndGetStudent(Long studentId) {
        // Si es estudiante, solo puede inscribirse a sí mismo
        if (sessionUtils.isStudent()) {
            if (!studentId.equals(sessionUtils.getCurrentUserId())) {
                throw new ValidationException("Solo puede inscribirse a sí mismo");
            }
        }

        return studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));
    }

    /**
     * Valida y obtiene el grupo de curso.
     */
    private CourseGroup validateAndGetCourseGroup(Long groupId) {
        return courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));
    }

    /**
     * Valida las reglas de negocio para la inscripción.
     */
    private void validateEnrollmentRules(Student student, CourseGroup courseGroup) {
        // Verificar que el grupo esté activo
        if (courseGroup.getStatus() != CourseGroupStatus.ACTIVE) {
            throw new ValidationException("Solo se puede inscribir en grupos activos");
        }

        // Verificar capacidad del grupo usando count en base de datos
        long currentEnrollments = enrollmentRepository.countByCourseGroupId(courseGroup.getId());
        if (currentEnrollments >= courseGroup.getMaxCapacity()) {
            throw new ValidationException("El grupo ha alcanzado su capacidad máxima");
        }

        // Verificar que el estudiante no esté ya inscrito
        if (enrollmentRepository.existsByStudentIdAndCourseGroupId(
                student.getId(), courseGroup.getId())) {
            throw new ValidationException("El estudiante ya está inscrito en este grupo");
        }

        // Verificar que la asignatura corresponda a la carrera del estudiante
        if (!courseGroup.getSubject().getMajor().equals(student.getMajor())) {
            throw new ValidationException(
                    "Solo puede inscribirse en asignaturas de su carrera: " + student.getMajor());
        }
    }

    /**
     * Valida las reglas para cancelar una inscripción.
     */
    private void validateCancellationRules(Enrollment enrollment) {
        // Solo se puede cancelar si el pago está pendiente
        if (enrollment.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ValidationException(
                    "No se puede cancelar una inscripción con pago confirmado");
        }

        // No se puede cancelar si el grupo está cerrado
        if (enrollment.getCourseGroup().getStatus() == CourseGroupStatus.CLOSED) {
            throw new ValidationException(
                    "No se puede cancelar la inscripción de un grupo cerrado");
        }
    }

    /**
     * Valida acceso de estudiante a sus propios recursos.
     */
    private void validateStudentAccess(Long studentId) {
        if (!sessionUtils.isStudent()) {
            return; // Si no es estudiante, puede ser admin
        }

        if (!studentId.equals(sessionUtils.getCurrentUserId())) {
            log.warn("Estudiante {} intentó acceder a datos del estudiante {}",
                    sessionUtils.getCurrentUserId(), studentId);
            throw new ValidationException("No puede acceder a información de otros estudiantes");
        }
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

    // ========== MÉTODOS DE CONSULTA ADICIONALES ==========

    /**
     * Verifica si un estudiante puede inscribirse en un grupo específico.
     * Útil para mostrar botones de inscripción habilitados/deshabilitados.
     */
    public boolean canEnrollInGroup(Long studentId, Long groupId) {
        try {
            Student student = validateAndGetStudent(studentId);
            CourseGroup group = validateAndGetCourseGroup(groupId);
            validateEnrollmentRules(student, group);
            return true;
        } catch (Exception e) {
            log.debug("Estudiante {} no puede inscribirse en grupo {}: {}",
                    studentId, groupId, e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas de inscripciones para un estudiante.
     */
    public EnrollmentStatsDto getStudentEnrollmentStats(Long studentId) {
        validateStudentAccess(studentId);

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Estudiante no encontrado con ID: " + studentId));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(
                studentId, Sort.by("createdAt"));

        long totalEnrollments = enrollments.size();
        long activeEnrollments = enrollments.stream()
                .filter(e -> e.getCourseGroup().getStatus() == CourseGroupStatus.ACTIVE)
                .count();
        long paidEnrollments = enrollments.stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PAID)
                .count();
        long pendingPayments = enrollments.stream()
                .filter(e -> e.getPaymentStatus() == PaymentStatus.PENDING)
                .count();

        return EnrollmentStatsDto.builder()
                .studentId(studentId)
                .totalEnrollments((int) totalEnrollments)
                .activeEnrollments((int) activeEnrollments)
                .paidEnrollments((int) paidEnrollments)
                .pendingPayments((int) pendingPayments)
                .build();
    }

    /**
     * Obtiene inscripciones por grupo.
     * Útil para profesores y administradores.
     */
    public List<EnrollmentDto> getEnrollmentsByGroup(Long groupId) {
        // Validar permisos (profesor del grupo o admin)
        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Grupo no encontrado con ID: " + groupId));

        if (sessionUtils.isTeacher()) {
            if (!group.getTeacher().getId().equals(sessionUtils.getCurrentUserId())) {
                throw new ValidationException("Solo puede ver inscripciones de sus propios grupos");
            }
        } else if (!sessionUtils.isAdmin()) {
            throw new ValidationException("No tiene permisos para ver estas inscripciones");
        }

        return enrollmentRepository.findByCourseGroupId(groupId).stream()
                .map(enrollmentMapper::toDto)
                .collect(Collectors.toList());
    }
}

/**
 * DTO para estadísticas de inscripciones (usado internamente).
 */
@lombok.Data
@lombok.Builder
class EnrollmentStatsDto {
    private Long studentId;
    private int totalEnrollments;
    private int activeEnrollments;
    private int paidEnrollments;
    private int pendingPayments;
}
