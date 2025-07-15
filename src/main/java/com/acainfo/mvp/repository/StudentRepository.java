package com.acainfo.mvp.repository;

import com.acainfo.mvp.model.Student;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    // Búsqueda por email con enrollments cargados
    @EntityGraph(attributePaths = {"enrollments", "enrollments.courseGroup"})
    Optional<Student> findWithEnrollmentsByEmail(String email);

    // Búsqueda simple por email
    Optional<Student> findByEmail(String email);

    // Verificar si existe por email
    boolean existsByEmail(String email);

    // Búsqueda por major con enrollments
    @EntityGraph(attributePaths = {"enrollments"})
    List<Student> findByMajor(String major);

    // Búsqueda por nombre (case insensitive)
    List<Student> findByNameContainingIgnoreCase(String name);

    // Estudiantes con enrollments activos - Optimizado
    @Query("SELECT DISTINCT s FROM Student s " +
            "JOIN FETCH s.enrollments e " +
            "JOIN FETCH e.courseGroup cg " +
            "WHERE cg.status = 'ACTIVE'")
    List<Student> findStudentsWithActiveEnrollments();

    // Estudiantes por asignatura - Optimizado
    @Query("SELECT DISTINCT s FROM Student s " +
            "JOIN FETCH s.enrollments e " +
            "JOIN FETCH e.courseGroup cg " +
            "JOIN FETCH cg.subject " +
            "WHERE cg.subject.id = :subjectId")
    List<Student> findBySubjectId(@Param("subjectId") Long subjectId);

    // Estudiantes con solicitudes pendientes - Optimizado
    @Query("SELECT DISTINCT s FROM Student s " +
            "JOIN FETCH s.groupRequests gr " +
            "JOIN FETCH gr.subject " +
            "WHERE gr.status = 'PENDING'")
    List<Student> findStudentsWithPendingRequests();

    // Obtener estudiante con todas sus relaciones
    @EntityGraph(attributePaths = {
            "enrollments.courseGroup.subject",
            "enrollments.courseGroup.teacher",
            "groupRequests.subject"
    })
    @Query("SELECT s FROM Student s WHERE s.id = :id")
    Optional<Student> findByIdWithFullDetails(@Param("id") Long id);

    // Estudiantes con enrollments y pagos pendientes
    @Query("SELECT DISTINCT s FROM Student s " +
            "JOIN FETCH s.enrollments e " +
            "JOIN FETCH e.courseGroup cg " +
            "WHERE e.paymentStatus = 'PENDING'")
    List<Student> findStudentsWithPendingPayments();
}