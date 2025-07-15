package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Enrollment;
import com.acainfo.mvp.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Buscar por estudiante y grupo (único)
    Optional<Enrollment> findByStudentIdAndCourseGroupId(Long studentId, Long courseGroupId);

    // Buscar con relaciones cargadas
    @EntityGraph(attributePaths = {"student", "courseGroup.subject", "courseGroup.teacher"})
    Optional<Enrollment> findWithDetailsByStudentIdAndCourseGroupId(Long studentId, Long courseGroupId);

    // Verificar si existe
    boolean existsByStudentIdAndCourseGroupId(Long studentId, Long courseGroupId);

    // Inscripciones por estudiante - Optimizado
    @EntityGraph(attributePaths = {"courseGroup.subject", "courseGroup.teacher"})
    List<Enrollment> findByStudentId(Long studentId);

    // Inscripciones por grupo - Optimizado
    @EntityGraph(attributePaths = {"student"})
    List<Enrollment> findByCourseGroupId(Long courseGroupId);

    // Inscripciones por estado de pago
    List<Enrollment> findByPaymentStatus(PaymentStatus paymentStatus);

    // Inscripciones por estado de pago con detalles
    @EntityGraph(attributePaths = {"student", "courseGroup.subject"})
    List<Enrollment> findWithDetailsByPaymentStatus(PaymentStatus paymentStatus);

    // Inscripciones por fecha
    List<Enrollment> findByEnrollmentDateBetween(LocalDateTime start, LocalDateTime end);

    // Inscripciones pendientes de pago por estudiante - Optimizado
    @EntityGraph(attributePaths = {"courseGroup.subject"})
    List<Enrollment> findByStudentIdAndPaymentStatus(Long studentId, PaymentStatus status);

    // Inscripciones activas de un estudiante - Optimizado
    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "WHERE e.student.id = :studentId " +
            "AND cg.status = 'ACTIVE'")
    List<Enrollment> findActiveEnrollmentsByStudent(@Param("studentId") Long studentId);

    // Contar inscripciones por grupo - DTO projection
    @Query("SELECT new map(" +
            "cg.id as groupId, " +
            "cg.subject.name as subjectName, " +
            "COUNT(e) as totalEnrollments, " +
            "COUNT(CASE WHEN e.paymentStatus = 'PAID' THEN 1 END) as paidEnrollments) " +
            "FROM Enrollment e " +
            "JOIN e.courseGroup cg " +
            "GROUP BY cg.id, cg.subject.name")
    List<Object> countEnrollmentsByGroup();

    // Inscripciones con pagos pendientes - Optimizado
    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.student s " +
            "JOIN FETCH e.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "WHERE e.paymentStatus = 'PENDING' " +
            "AND e.enrollmentDate < :cutoffDate")
    List<Enrollment> findOverduePendingPayments(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Total recaudado por grupo - DTO projection
    @Query("SELECT new map(" +
            "cg.id as groupId, " +
            "cg.subject.name as subjectName, " +
            "cg.teacher.name as teacherName, " +
            "COUNT(e) as paidEnrollments, " +
            "SUM(cg.price) as totalRevenue) " +
            "FROM Enrollment e " +
            "JOIN e.courseGroup cg " +
            "LEFT JOIN cg.teacher t " +
            "WHERE e.paymentStatus = 'PAID' " +
            "GROUP BY cg.id, cg.subject.name, cg.teacher.name")
    List<Object> calculateRevenueByGroup();

    // Todas las inscripciones con detalles completos para reportes
    @Query("SELECT e FROM Enrollment e " +
            "JOIN FETCH e.student s " +
            "JOIN FETCH e.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "LEFT JOIN FETCH cg.teacher " +
            "ORDER BY e.enrollmentDate DESC")
    List<Enrollment> findAllWithFullDetails();
}